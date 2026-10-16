package com.cyberblaze.appriskauditor.scanner

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.PackageManager.GET_PERMISSIONS
import android.content.pm.PackageManager.NameNotFoundException
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.Log
import com.cyberblaze.appriskauditor.data.AppInfo
import com.cyberblaze.appriskauditor.data.Constants
import com.cyberblaze.appriskauditor.data.PermInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Person 1 – Scanner
 *
 * Turns raw [PackageManager] data into [List]<[AppInfo]>.
 * No network I/O, no root, public APIs only. Works fully offline.
 *
 * **Thread safety:** [scanApps] is blocking — always call it off the main thread.
 * Use [scanAppsSuspend] from coroutine callers (dispatches to [Dispatchers.Default]).
 */
object AppScanner {

    private const val TAG = "AppScanner"

    // ─────────────────────────────────────────────────────────────────────────
    // Public API
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Synchronously scans every installed app and returns a stable list sorted by
     * label (case-insensitive).
     *
     * @param ctx           Any [Context].
     * @param includeSystem When false (default), apps with FLAG_SYSTEM are excluded.
     *                      Updated system apps are also excluded. This app's own package
     *                      is always excluded regardless of this flag.
     */
    fun scanApps(ctx: Context, includeSystem: Boolean = false): List<AppInfo> {
        val pm     = ctx.packageManager
        val ownPkg = ctx.packageName

        val packages: List<PackageInfo> = getInstalledPackages(pm)

        // One cache per scan — each unique permission string is resolved at most once.
        val levelCache = HashMap<String, String>()

        val result = packages.mapNotNull { pi ->
            val appInfo = pi.applicationInfo ?: return@mapNotNull null
            val pkg     = pi.packageName

            if (pkg == ownPkg) return@mapNotNull null  // always skip self

            // FLAG_SYSTEM covers both pre-installed and updated system apps.
            val isSystem = (appInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0
            if (!includeSystem && isSystem) return@mapNotNull null

            buildAppInfo(pm, pi, appInfo, pkg, levelCache)
        }

        return result.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.label })
    }

    /**
     * Coroutine-friendly wrapper that suspends on [Dispatchers.Default].
     * Glue (Person 4) should prefer this over [scanApps].
     */
    suspend fun scanAppsSuspend(ctx: Context, includeSystem: Boolean = false): List<AppInfo> =
        withContext(Dispatchers.Default) { scanApps(ctx, includeSystem) }

    /**
     * Loads the launcher icon for [pkg] without ever throwing.
     *
     * Returns null when the package is not found (race between scan and uninstall)
     * or when drawable inflation fails for any other reason.
     */
    fun loadAppIcon(ctx: Context, pkg: String): Drawable? {
        return try {
            ctx.packageManager.getApplicationIcon(pkg)
        } catch (e: NameNotFoundException) {
            logDebug("loadAppIcon: package not found – $pkg")
            null
        } catch (e: Exception) {
            logDebug("loadAppIcon: error for $pkg – ${e.message}")
            null
        }
    }

    /**
     * Returns fake [AppInfo] objects for Person 2 (scoring) and Person 4 (UI)
     * to use immediately without a real device.
     *
     * Coverage:
     * - All four protection levels (normal, dangerous, signature, special)
     * - Sideloaded (null installer) and Play Store installer
     * - Declared-but-not-granted permissions
     */
    fun fakeApps(): List<AppInfo> = listOf(

        // Torch Master — sideloaded, SMS + contacts = very suspicious
        AppInfo(
            label       = "Torch Master",
            pkg         = "com.fake.torch",
            version     = "2.1.0",
            installTime = 1_700_000_000_000L,
            installer   = null,                         // sideloaded
            targetSdk   = 33,
            isSystem    = false,
            perms       = listOf(
                PermInfo("android.permission.READ_SMS",               granted = true,  level = Constants.LEVEL_DANGEROUS),
                PermInfo("android.permission.RECEIVE_SMS",            granted = true,  level = Constants.LEVEL_DANGEROUS),
                PermInfo("android.permission.READ_CONTACTS",          granted = true,  level = Constants.LEVEL_DANGEROUS),
                PermInfo("android.permission.INTERNET",               granted = true,  level = Constants.LEVEL_NORMAL),
                PermInfo("android.permission.RECEIVE_BOOT_COMPLETED", granted = true,  level = Constants.LEVEL_NORMAL),
            ),
        ),

        // QuickChat — Play Store, call log + mic
        AppInfo(
            label       = "QuickChat",
            pkg         = "com.fake.quickchat",
            version     = "5.0.3",
            installTime = 1_710_000_000_000L,
            installer   = Constants.INSTALLER_PLAY_STORE,
            targetSdk   = 34,
            isSystem    = false,
            perms       = listOf(
                PermInfo("android.permission.READ_CONTACTS",  granted = true, level = Constants.LEVEL_DANGEROUS),
                PermInfo("android.permission.READ_CALL_LOG",  granted = true, level = Constants.LEVEL_DANGEROUS),
                PermInfo("android.permission.RECORD_AUDIO",   granted = true, level = Constants.LEVEL_DANGEROUS),
                PermInfo("android.permission.INTERNET",       granted = true, level = Constants.LEVEL_NORMAL),
            ),
        ),

        // Polite Notes — Play Store, only INTERNET
        AppInfo(
            label       = "Polite Notes",
            pkg         = "com.fake.politenotes",
            version     = "1.0.0",
            installTime = 1_715_000_000_000L,
            installer   = Constants.INSTALLER_PLAY_STORE,
            targetSdk   = 34,
            isSystem    = false,
            perms       = listOf(
                PermInfo("android.permission.INTERNET", granted = true, level = Constants.LEVEL_NORMAL),
            ),
        ),

        // FakeCam Pro — location declared but NOT granted; covers signature + special levels
        AppInfo(
            label       = "FakeCam Pro",
            pkg         = "com.fake.fakecampro",
            version     = "3.4.1",
            installTime = 1_712_000_000_000L,
            installer   = Constants.INSTALLER_PLAY_STORE,
            targetSdk   = 33,
            isSystem    = false,
            perms       = listOf(
                PermInfo("android.permission.CAMERA",                     granted = true,  level = Constants.LEVEL_DANGEROUS),
                PermInfo("android.permission.ACCESS_FINE_LOCATION",       granted = false, level = Constants.LEVEL_DANGEROUS),
                PermInfo("android.permission.ACCESS_BACKGROUND_LOCATION", granted = false, level = Constants.LEVEL_DANGEROUS),
                PermInfo("android.permission.CHANGE_COMPONENT_ENABLED_STATE", granted = false, level = Constants.LEVEL_SIGNATURE),
                PermInfo("android.permission.SYSTEM_ALERT_WINDOW",        granted = false, level = Constants.LEVEL_SPECIAL),
            ),
        ),
    )

    // ─────────────────────────────────────────────────────────────────────────
    // Internal helpers
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Fetches the full package list with [GET_PERMISSIONS].
     *
     * Uses [PackageManager.PackageInfoFlags] on API 33+ to avoid the deprecated
     * int-flag overload lint warning; falls back to the classic int overload on API ≤ 32.
     */
    @Suppress("DEPRECATION")
    private fun getInstalledPackages(pm: PackageManager): List<PackageInfo> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getInstalledPackages(PackageManager.PackageInfoFlags.of(GET_PERMISSIONS.toLong()))
        } else {
            pm.getInstalledPackages(GET_PERMISSIONS)
        }

    /** Assembles one [AppInfo]; all throwing PM calls are caught and logged. */
    private fun buildAppInfo(
        pm: PackageManager,
        pi: PackageInfo,
        ai: android.content.pm.ApplicationInfo,
        pkg: String,
        levelCache: HashMap<String, String>,
    ): AppInfo {
        val label     = resolveLabel(pm, ai, pkg)
        val version   = resolveVersion(pi)
        val installer = resolveInstaller(pm, pkg)
        val perms     = resolvePerms(pm, pi, levelCache)

        logDebug("$label | $pkg | ${perms.size} permissions (${perms.count { it.granted }} granted)")

        return AppInfo(
            label       = label,
            pkg         = pkg,
            version     = version,
            installTime = pi.firstInstallTime,
            installer   = installer,
            targetSdk   = ai.targetSdkVersion,
            isSystem    = (ai.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0,
            perms       = perms,
            special     = emptySet(),  // Person 3 fills this in the glue layer
        )
    }

    private fun resolveLabel(pm: PackageManager, ai: android.content.pm.ApplicationInfo, pkg: String): String =
        try { ai.loadLabel(pm).toString().takeIf { it.isNotBlank() } ?: pkg }
        catch (e: Exception) { logDebug("resolveLabel failed for $pkg: ${e.message}"); pkg }

    @Suppress("DEPRECATION") // versionCode deprecated at API 28; longVersionCode available from API 28
    private fun resolveVersion(pi: PackageInfo): String =
        when {
            !pi.versionName.isNullOrBlank()                  -> pi.versionName!!
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.P  -> pi.longVersionCode.toString()
            else                                             -> pi.versionCode.toString()
        }

    /**
     * Returns the installer package name, or null if sideloaded / unknown.
     *
     * API 30+: [PackageManager.getInstallSourceInfo].installingPackageName  
     * API 26–29: deprecated [PackageManager.getInstallerPackageName]
     */
    @Suppress("DEPRECATION")
    private fun resolveInstaller(pm: PackageManager, pkg: String): String? =
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                pm.getInstallSourceInfo(pkg).installingPackageName
            } else {
                pm.getInstallerPackageName(pkg)
            }
        } catch (e: NameNotFoundException) {
            logDebug("resolveInstaller: not found – $pkg"); null
        }

    /**
     * Pairs [PackageInfo.requestedPermissions] with [PackageInfo.requestedPermissionsFlags].
     * Apps with no declared permissions return an empty list.
     */
    private fun resolvePerms(
        pm: PackageManager,
        pi: PackageInfo,
        levelCache: HashMap<String, String>,
    ): List<PermInfo> {
        val names = pi.requestedPermissions      ?: return emptyList()
        val flags = pi.requestedPermissionsFlags ?: return emptyList()
        val src   = RealPermissionInfoSource(pm)   // one wrapper per package, not per permission

        return names.mapIndexed { i, name ->
            val granted = (flags[i] and PackageInfo.REQUESTED_PERMISSION_GRANTED) != 0
            val level   = resolvePermLevel(name, src, levelCache)
            PermInfo(name = name, granted = granted, level = level)
        }
    }

    private fun logDebug(msg: String) {
        if (com.cyberblaze.appriskauditor.BuildConfig.DEBUG) Log.d(TAG, msg)
    }
}
