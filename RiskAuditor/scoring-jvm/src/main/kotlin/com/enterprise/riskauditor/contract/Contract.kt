package com.enterprise.riskauditor.contract

/**
 * Shared read-only contract types used by all team members.
 * Person 3 fills [special]; Person 1 provides the rest via scanApps().
 */

/**
 * All metadata about a single installed application.
 *
 * @property pkg Android package name, e.g. "com.example.app"
 * @property label Human-readable app name
 * @property versionName Version string from PackageInfo
 * @property versionCode Version code from PackageInfo
 * @property targetSdk Target SDK integer from ApplicationInfo
 * @property installTimeMs Unix epoch milliseconds of first install
 * @property installerPackage Package name of the installer, or null if unknown/sideloaded
 * @property isSystem True if the app is a system app (FLAG_SYSTEM)
 * @property declaredPerms All android.permission.* strings declared in the manifest
 * @property grantedPerms Subset of declaredPerms that are currently GRANTED
 * @property special Mutable; Person 3 fills this with special-access tags before scoring
 */
data class AppInfo(
    val pkg: String,
    val label: String,
    val versionName: String,
    val versionCode: Long,
    val targetSdk: Int,
    val installTimeMs: Long,
    val installerPackage: String?,
    val isSystem: Boolean,
    val declaredPerms: Set<String>,
    val grantedPerms: Set<String>,
    var special: Set<String> = emptySet()
)

/**
 * The output of scoreApp() for a single app.
 *
 * @property score Risk score 0-100
 * @property band One of the Bands.* constants
 * @property persona Display name string from PersonaCatalog
 * @property tagline Short persona tagline from PersonaCatalog
 * @property reasons Ordered list of plain-English reason sentences (descending by points)
 * @property rooms Map of room-name -> list of permission/tag strings present in that room
 */
data class RiskResult(
    val score: Int,
    val band: String,
    val persona: String,
    val tagline: String,
    val reasons: List<String>,
    val rooms: Map<String, List<String>>
)
