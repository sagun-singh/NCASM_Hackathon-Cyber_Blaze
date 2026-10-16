package com.enterprise.riskauditor.scanner

import android.content.pm.PackageManager
import android.content.pm.PermissionInfo
import com.enterprise.riskauditor.contract.Constants

/**
 * Thin wrapper around [PackageManager.getPermissionInfo] to make [AppScanner]
 * testable without Robolectric.
 *
 * Production code uses [RealPermissionInfoSource]; unit tests inject a fake.
 */
internal fun interface PermissionInfoSource {
    /**
     * Returns the [PermissionInfo] for [name], or throws [PackageManager.NameNotFoundException]
     * if the permission is not known to the system (matches PM's own contract exactly).
     */
    @Throws(PackageManager.NameNotFoundException::class)
    fun get(name: String): PermissionInfo
}

/** Live implementation backed by a real [PackageManager]. */
internal class RealPermissionInfoSource(private val pm: PackageManager) : PermissionInfoSource {
    override fun get(name: String): PermissionInfo = pm.getPermissionInfo(name, 0)
}

/**
 * Fake [PermissionInfo] helper for unit tests — avoids reflection on Android stub jars.
 *
 * @param base           One of [PermissionInfo.PROTECTION_NORMAL], [PROTECTION_DANGEROUS],
 *                       [PROTECTION_SIGNATURE].
 * @param protectionFlags Bitmask of PROTECTION_FLAG_* values (e.g. [PermissionInfo.PROTECTION_FLAG_APPOP]).
 */
internal fun fakePermissionInfo(base: Int, protectionFlags: Int): PermissionInfo {
    // PermissionInfo is a concrete class in android-stubs; its fields are settable directly.
    val info = PermissionInfo()
    @Suppress("DEPRECATION")
    info.protectionLevel = base or protectionFlags  // API < 28 path
    // Attempt to set the API-28+ fields via reflection; silently ignore on stub jars
    // that don't expose them (the deprecated path is always available and sufficient
    // for host-JVM tests since we only guard on SDK_INT at runtime, not compile time).
    runCatching {
        val pField = PermissionInfo::class.java.getDeclaredField("protection")
        pField.isAccessible = true
        pField.setInt(info, base)
    }
    runCatching {
        val fField = PermissionInfo::class.java.getDeclaredField("protectionFlags")
        fField.isAccessible = true
        fField.setInt(info, protectionFlags)
    }
    return info
}

/**
 * Pure-logic resolver for permission protection levels.
 *
 * Extracted from [AppScanner] so it can be unit-tested with an injected
 * [PermissionInfoSource] (no PackageManager needed in tests).
 *
 * Rules (from spec):
 * 1. APPOP flag  → "special"
 * 2. Name in [Constants.SPECIAL_PERMISSION_NAMES] → "special"
 * 3. Base level: DANGEROUS → "dangerous", SIGNATURE → "signature", else → "normal"
 * 4. [PackageManager.NameNotFoundException] → "normal" (custom/unknown permission)
 *
 * Results are cached in [cache] (one resolution per unique permission name per scan).
 */
internal fun resolvePermLevel(
    name: String,
    source: PermissionInfoSource,
    cache: HashMap<String, String>,
): String {
    cache[name]?.let { return it }

    val level = try {
        val info = source.get(name)

        // Read the flags; prefer the `protectionFlags` field (API 28+) but fall back
        // to masking the deprecated `protectionLevel` field on older stubs/devices.
        val flags: Int = runCatching {
            val f = PermissionInfo::class.java.getDeclaredField("protectionFlags")
            f.isAccessible = true
            f.getInt(info)
        }.getOrElse {
            @Suppress("DEPRECATION")
            // Mask out base-protection bits to isolate flags.
            info.protectionLevel and PermissionInfo.PROTECTION_MASK_BASE.inv()
        }

        val isAppOp = (flags and PermissionInfo.PROTECTION_FLAG_APPOP) != 0
        if (isAppOp || name in Constants.SPECIAL_PERMISSION_NAMES) {
            Constants.LEVEL_SPECIAL
        } else {
            // Read the base-protection value; prefer `protection` (API 28+).
            val base: Int = runCatching {
                val f = PermissionInfo::class.java.getDeclaredField("protection")
                f.isAccessible = true
                f.getInt(info)
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
        // Unknown / custom permission (defined by another app that was uninstalled).
        Constants.LEVEL_NORMAL
    }

    cache[name] = level
    return level
}
