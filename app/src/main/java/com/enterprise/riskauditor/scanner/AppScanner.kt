package com.enterprise.riskauditor.scanner

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.PackageManager.GET_PERMISSIONS
import android.content.pm.PackageManager.NameNotFoundException
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.Log
import com.enterprise.riskauditor.contract.AppInfo
import com.enterprise.riskauditor.contract.Constants
import com.enterprise.riskauditor.contract.PermInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// resolvePermLevel and PermissionInfoSource are defined in PermissionLevelResolver.kt

/**
 * Person 1 – Scanner
 *
 * Turns raw [PackageManager] data into [List]<[AppInfo]>.
 * No network I/O, no root, public APIs only. Works offline.
 *
 * **Thread safety:** [scanApps] is a blocking call — run it off the main thread.
 * Use [scanAppsSuspend] from coroutine callers (it dispatches to [Dispatchers.Default]).
 */
object AppScanner {

    private const val TAG = "AppScanner"

    // ─────────────────────────────────────────────────────────────────────────
    // Public API
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Synchronously scan every installed app and return a stable list sorted by
     * label (case-insensitive).
     *
     * @param ctx           Any [Context]; the scanner calls [Context.getPackageManager].
     * @param includeSystem When false (default), apps with [android.content.pm.ApplicationInfo.FLAG_SYSTEM]
     *                      set are excluded. Updated system apps (which have both FLAG_SYSTEM and
     *                      FLAG_UPDATED_SYSTEM_APP) are also treated as system and excluded.
     *                      This app's own package is always excluded.
     */
    fun scanApps(ctx: Context, includeSystem: Boolean = false): List<AppInfo> {
        val pm = ctx.packageManager
        val ownPkg = ctx.packageName

        // One pass: fetch all packages with permission data in a single PM call.
        val packages: List<PackageInfo> = getInstalledPackages(pm)

        // Permission-level cache: resolve each permission string at most once.
        // Shared across all apps in this scan for efficiency.
        val levelCache = HashMap<String, String>()

        val result = packages.mapNotNull { pi ->
            val appInfo = pi.applicationInfo ?: return@mapNotNull null
            val pkg = pi.packageName

            // Always skip our own package.
            if (pkg == ownPkg) return@mapNotNull null

            // System-app filter.
            // FLAG_UPDATED_SYSTEM_APP means it was pre-installed but has been updated;
            // it still counts as a system app for filtering purposes.
            val isSystem = (appInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0
            if (!includeSystem && isSystem) return@mapNotNull null

            buildAppInfo(pm, pi, appInfo, pkg, levelCache)
        }

        return result.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.label })
    }

    /**
     * Coroutine-friendly wrapper — suspends on [Dispatchers.Default] so callers on
     * the main dispatcher are never blocked.
     */
    suspend fun scanAppsSuspend(ctx: Context, includeSystem: Boolean = false): List<AppInfo> =
        withContext(Dispatchers.Default) { scanApps(ctx, includeSystem) }

    /**
     * Load the launcher icon for [pkg] without ever throwing.
     *
     * Returns null if the package is not found or the icon cannot be loaded
     * (e.g. race condition between scan and uninstall).
     */
    fun loadAppIcon(ctx: Context, pkg: String): Drawable? {
        return try {
            ctx.packageManager.getApplicationIcon(pkg)
        } catch (e: NameNotFoundException) {
            // Package was uninstalled between scan and icon load — expected edge case.
            logDebug("loadAppIcon: package not found – $pkg")
            null
        } catch (e: Exception) {
            // Catch-all for any other drawable inflation failure.
            logDebug("loadAppIcon: unexpected error for $pkg – ${e.message}")
            null
        }
    }

    /**
     * Returns a list of fake [AppInfo] objects so that Person 2 (scoring) and
     * Person 4 (UI) can work without a real device/PM.
     *
     * Coverage guarantees:
     * - All four protection levels appear (normal, dangerous, signature, special).
     * - Sideloaded (null installer) and Play Store installer both appear.
     * - Declared-but-not-granted permissions appear (FakeCam Pro).
     */
    fun fakeApps(): List<AppInfo> = listOf(

        // ── "Torch Master" ── sideloaded, SMS + contacts, very sus ────────────
        AppInfo(
            pkg         = "com.fake.torch",
            label       = "Torch Master",
            version     = "2.1.0",
            installTime = 1_700_000_000_000L,
            installer   = null,                          // sideloaded
            targetSdk   = 33,
            isSystem    = false,
            perms       = listOf(
                PermInfo("android.permission.READ_SMS",              granted = true,  level = Constants.LEVEL_DANGEROUS, room = Constants.ROOM_MESSAGES),
                PermInfo("android.permission.RECEIVE_SMS",           granted = true,  level = Constants.LEVEL_DANGEROUS, room = Constants.ROOM_MESSAGES),
                PermInfo("android.permission.READ_CONTACTS",         granted = true,  level = Constants.LEVEL_DANGEROUS, room = Constants.ROOM_CONTACTS),
                PermInfo("android.permission.INTERNET",              granted = true,  level = Constants.LEVEL_NORMAL,    room = null),
                PermInfo("android.permission.RECEIVE_BOOT_COMPLETED",granted = true,  level = Constants.LEVEL_NORMAL,    room = null),
            ),
        ),

        // ── "QuickChat" ── Play Store, call log + mic ─────────────────────────
        AppInfo(
            pkg         = "com.fake.quickchat",
            label       = "QuickChat",
            version     = "5.0.3",
            installTime = 1_710_000_000_000L,
            installer   = Constants.INSTALLER_PLAY_STORE,
            targetSdk   = 34,
            isSystem    = false,
            perms       = listOf(
                PermInfo("android.permission.READ_CONTACTS",  granted = true, level = Constants.LEVEL_DANGEROUS, room = Constants.ROOM_CONTACTS),
                PermInfo("android.permission.READ_CALL_LOG",  granted = true, level = Constants.LEVEL_DANGEROUS, room = Constants.ROOM_PHONE),
                PermInfo("android.permission.RECORD_AUDIO",   granted = true, level = Constants.LEVEL_DANGEROUS, room = Constants.ROOM_MICROPHONE),
                PermInfo("android.permission.INTERNET",       granted = true, level = Constants.LEVEL_NORMAL,    room = null),
            ),
        ),

        // ── "Polite Notes" ── Play Store, only INTERNET ───────────────────────
        AppInfo(
            pkg         = "com.fake.politenotes",
            label       = "Polite Notes",
            version     = "1.0.0",
            installTime = 1_715_000_000_000L,
            installer   = Constants.INSTALLER_PLAY_STORE,
            targetSdk   = 34,
            isSystem    = false,
            perms       = listOf(
                PermInfo("android.permission.INTERNET", granted = true, level = Constants.LEVEL_NORMAL, room = null),
            ),
        ),

        // ── "FakeCam Pro" ── Play Store, location declared but NOT granted ─────
        // Also covers SIGNATURE and SPECIAL protection levels via manifest
        AppInfo(
            pkg         = "com.fake.fakecampro",
            label       = "FakeCam Pro",
            version     = "3.4.1",
            installTime = 1_712_000_000_000L,
            installer   = Constants.INSTALLER_PLAY_STORE,
            targetSdk   = 33,
            isSystem    = false,
            perms       = listOf(
                PermInfo("android.permission.CAMERA",                    granted = true,  level = Constants.LEVEL_DANGEROUS, room = Constants.ROOM_CAMERA),
                PermInfo("android.permission.ACCESS_FINE_LOCATION",      granted = false, level = Constants.LEVEL_DANGEROUS, room = Constants.ROOM_LOCATION),
                PermInfo("android.permission.ACCESS_BACKGROUND_LOCATION",granted = false, level = Constants.LEVEL_DANGEROUS, room = Constants.ROOM_LOCATION),
                // signature-level perm example (declared but naturally not granted to non-system)
                PermInfo("android.permission.CHANGE_COMPONENT_ENABLED_STATE", granted = false, level = Constants.LEVEL_SIGNATURE, room = null),
                // special-level perm example
                PermInfo("android.permission.SYSTEM_ALERT_WINDOW",       granted = false, level = Constants.LEVEL_SPECIAL,    room = null),
            ),
        ),
    )

    // ─────────────────────────────────────────────────────────────────────────
    // Internal helpers
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Fetches the full package list with [GET_PERMISSIONS].
     *
     * Uses the API-33+ [PackageManager.getInstalledPackages] overload that accepts a
     * [android.content.pm.PackageManager.PackageInfoFlags] value-class to avoid the
     * deprecated `int`-flag overload lint warning on newer compileSdk targets.
     * Falls back to the classic `int` overload on API < 33.
     */
    @Suppress("DEPRECATION") // deliberate API-version guard below
    private fun getInstalledPackages(pm: PackageManager): List<PackageInfo> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // API 33+: use the PackageInfoFlags overload.
            pm.getInstalledPackages(PackageManager.PackageInfoFlags.of(GET_PERMISSIONS.toLong()))
        } else {
            // API 26–32: classic int overload still works fine.
            pm.getInstalledPackages(GET_PERMISSIONS)
        }
    }

    /**
     * Assembles one [AppInfo] from the raw [PackageInfo].
     *
     * All potentially-throwing PM calls are wrapped; failures log and return
     * a safe default rather than propagating (enterprise-grade resilience).
     */
    private fun buildAppInfo(
        pm: PackageManager,
        pi: PackageInfo,
        appInfo: android.content.pm.ApplicationInfo,
        pkg: String,
        levelCache: HashMap<String, String>,
    ): AppInfo {
        val label   = resolveLabel(pm, appInfo, pkg)
        val version = resolveVersion(pi)
        val installer = resolveInstaller(pm, pkg)
        val perms   = resolvePerms(pm, pi, levelCache)

        logDebug("$label | $pkg | ${perms.size} permissions (${perms.count { it.granted }} granted)")

        return AppInfo(
            pkg         = pkg,
            label       = label,
            version     = version,
            installTime = pi.firstInstallTime,
            installer   = installer,
            targetSdk   = appInfo.targetSdkVersion,
            isSystem    = (appInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0,
            perms       = perms,
            special     = emptySet(),   // Person 3 fills this in the glue layer
        )
    }

    /** loadLabel can return a CharSequence from the resources; fall back to pkg on any failure. */
    private fun resolveLabel(pm: PackageManager, ai: android.content.pm.ApplicationInfo, pkg: String): String {
        return try {
            ai.loadLabel(pm).toString().takeIf { it.isNotBlank() } ?: pkg
        } catch (e: Exception) {
            logDebug("resolveLabel failed for $pkg: ${e.message}")
            pkg
        }
    }

    /**
     * versionName is nullable (and deprecated-ish on API 28+ in favour of longVersionCode).
     * Prefer the human-readable name; fall back to the numeric code.
     */
    @Suppress("DEPRECATION") // versionCode deprecated at API 28; longVersionCode is API 28+
    private fun resolveVersion(pi: PackageInfo): String {
        return if (!pi.versionName.isNullOrBlank()) {
            pi.versionName!!
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            pi.longVersionCode.toString()
        } else {
            pi.versionCode.toString()
        }
    }

    /**
     * Returns the installing package name, or null if the app was sideloaded / unknown.
     *
     * API 30+ uses [PackageManager.getInstallSourceInfo] (the modern path).
     * API 26–29 falls back to the deprecated [PackageManager.getInstallerPackageName].
     * Both paths swallow [NameNotFoundException] gracefully.
     */
    @Suppress("DEPRECATION") // getInstallerPackageName deprecated at API 30; guarded below
    private fun resolveInstaller(pm: PackageManager, pkg: String): String? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                // API 30+: installingPackageName is the package that called
                // PackageInstaller.Session or setInstallerPackageName.
                pm.getInstallSourceInfo(pkg).installingPackageName
            } else {
                pm.getInstallerPackageName(pkg)
            }
        } catch (e: NameNotFoundException) {
            logDebug("resolveInstaller: package not found – $pkg")
            null
        }
    }

    /**
     * Pairs [PackageInfo.requestedPermissions] with [PackageInfo.requestedPermissionsFlags]
     * to produce a list of [PermInfo] records.
     *
     * Apps that declare no permissions return an empty list.
     * [levelCache] is mutated to avoid redundant PM queries across apps.
     */
    private fun resolvePerms(
        pm: PackageManager,
        pi: PackageInfo,
        levelCache: HashMap<String, String>,
    ): List<PermInfo> {
        val names  = pi.requestedPermissions         ?: return emptyList()
        val flags  = pi.requestedPermissionsFlags    ?: return emptyList()

        // Construct the source once per package, not once per permission (perf: avoids
        // allocating a wrapper object for every entry in names[]).
        val source = RealPermissionInfoSource(pm)

        return names.mapIndexed { idx, name ->
            // REQUESTED_PERMISSION_GRANTED is set in the flags array when the permission
            // has been granted by the system (dangerous perms) or is auto-granted (normal/signature).
            val granted = (flags[idx] and PackageInfo.REQUESTED_PERMISSION_GRANTED) != 0
            // Delegate to top-level function in PermissionLevelResolver.kt (injectable seam).
            val level   = resolvePermLevel(name, source, levelCache)
            val room    = Constants.PERMISSION_TO_ROOM[name]
            PermInfo(name = name, granted = granted, level = level, room = room)
        }
    }

    /** Only logs when [BuildConfig.DEBUG] is true, and never logs PII beyond package names. */
    private fun logDebug(msg: String) {
        if (com.enterprise.riskauditor.BuildConfig.DEBUG) {
            Log.d(TAG, msg)
        }
    }
}
