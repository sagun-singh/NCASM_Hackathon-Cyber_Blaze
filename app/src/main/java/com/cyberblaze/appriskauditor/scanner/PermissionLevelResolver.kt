package com.cyberblaze.appriskauditor.scanner

import android.content.pm.PackageManager
import android.content.pm.PermissionInfo
import com.cyberblaze.appriskauditor.data.Constants

/**
 * Thin wrapper around [PackageManager.getPermissionInfo] — injectable seam for testing.
 *
 * Production code uses [RealPermissionInfoSource]; unit tests supply a lambda.
 */
internal fun interface PermissionInfoSource {
    @Throws(PackageManager.NameNotFoundException::class)
    fun get(name: String): PermissionInfo
}

/** Live implementation backed by a real [PackageManager]. */
internal class RealPermissionInfoSource(private val pm: PackageManager) : PermissionInfoSource {
    override fun get(name: String): PermissionInfo = pm.getPermissionInfo(name, 0)
}

/**
 * Builds a [PermissionInfo] suitable for host-JVM unit tests without Robolectric.
 *
 * Sets both the deprecated `protectionLevel` field (API < 28) and attempts to set
 * the `protection` / `protectionFlags` fields via reflection (API 28+). The
 * reflection calls are wrapped in `runCatching` so they silently no-op on stub jars
 * that don't expose those fields — the deprecated path is always sufficient for tests.
 */
internal fun fakePermissionInfo(base: Int, protectionFlags: Int): PermissionInfo {
    val info = PermissionInfo()
    @Suppress("DEPRECATION")
    info.protectionLevel = base or protectionFlags
    runCatching {
        PermissionInfo::class.java.getDeclaredField("protection")
            .also { it.isAccessible = true }.setInt(info, base)
    }
    runCatching {
        PermissionInfo::class.java.getDeclaredField("protectionFlags")
            .also { it.isAccessible = true }.setInt(info, protectionFlags)
    }
    return info
}

/**
 * Resolves the protection-level string for a single permission name.
 *
 * Extracted as a top-level function so it can be unit-tested with an injected
 * [PermissionInfoSource] without a real [PackageManager].
 *
 * Rules (spec §Field-by-field rules / level):
 * 1. APPOP flag in protectionFlags → [Constants.LEVEL_SPECIAL]
 * 2. Name in [Constants.SPECIAL_PERMISSION_NAMES] → [Constants.LEVEL_SPECIAL]
 * 3. Base protection DANGEROUS / SIGNATURE → matching string; else "normal"
 * 4. [PackageManager.NameNotFoundException] (custom/unknown perm) → "normal"
 *
 * Results are cached in [cache] — each unique permission name is resolved at most once
 * per scan, not once per app.
 */
internal fun resolvePermLevel(
    name: String,
    source: PermissionInfoSource,
    cache: HashMap<String, String>,
): String {
    cache[name]?.let { return it }

    val level = try {
        val info = source.get(name)

        // Prefer the dedicated `protectionFlags` field (API 28+); fall back to masking
        // the deprecated `protectionLevel` combined field on older devices/stubs.
        val flags: Int = runCatching {
            PermissionInfo::class.java.getDeclaredField("protectionFlags")
                .also { it.isAccessible = true }.getInt(info)
        }.getOrElse {
            @Suppress("DEPRECATION")
            info.protectionLevel and PermissionInfo.PROTECTION_MASK_BASE.inv()
        }

        if ((flags and PermissionInfo.PROTECTION_FLAG_APPOP) != 0
            || name in Constants.SPECIAL_PERMISSION_NAMES
        ) {
            Constants.LEVEL_SPECIAL
        } else {
            // Prefer `protection` (API 28+); fall back to masking protectionLevel.
            val base: Int = runCatching {
                PermissionInfo::class.java.getDeclaredField("protection")
                    .also { it.isAccessible = true }.getInt(info)
            }.getOrElse {
                @Suppress("DEPRECATION")
                info.protectionLevel and PermissionInfo.PROTECTION_MASK_BASE
            }
            when (base) {
                PermissionInfo.PROTECTION_DANGEROUS -> Constants.LEVEL_DANGEROUS
                PermissionInfo.PROTECTION_SIGNATURE -> Constants.LEVEL_SIGNATURE
                else                                -> Constants.LEVEL_NORMAL
            }
        }
    } catch (_: PackageManager.NameNotFoundException) {
        Constants.LEVEL_NORMAL  // custom/unknown permission — default to normal
    }

    cache[name] = level
    return level
}
