package com.enterprise.riskauditor.contract

/**
 * Shared data contract for the On-Device App Risk Auditor.
 * READ-ONLY for all team members. Import types from here; never redefine them.
 */

// ── Per-permission record ────────────────────────────────────────────────────

/**
 * One permission entry on an [AppInfo].
 *
 * @param name      Fully-qualified permission name, e.g. "android.permission.CAMERA".
 * @param granted   True when the system has granted this permission at scan time.
 * @param level     One of [Constants.LEVEL_NORMAL], [Constants.LEVEL_DANGEROUS],
 *                  [Constants.LEVEL_SIGNATURE], or [Constants.LEVEL_SPECIAL].
 * @param room      One of the nine [Constants.ROOM_*] strings, or null for uncategorised.
 */
data class PermInfo(
    val name: String,
    val granted: Boolean,
    val level: String,
    val room: String?,
)

// ── Per-app record ────────────────────────────────────────────────────────────

/**
 * Full audit snapshot of one installed application.
 *
 * All fields are populated by Person 1 (scanner) except [special], which is
 * populated by Person 3 (special access) before any scoring takes place.
 *
 * @param pkg         Package name.
 * @param label       Human-readable app label; falls back to [pkg].
 * @param version     versionName, or longVersionCode.toString() if name is null.
 * @param installTime firstInstallTime in milliseconds since epoch.
 * @param installer   Installing package (e.g. "com.android.vending") or null if
 *                    sideloaded / unknown.
 * @param targetSdk   applicationInfo.targetSdkVersion.
 * @param isSystem    True when FLAG_SYSTEM is set (including updated system apps).
 * @param perms       All declared permissions with grant state and protection level.
 * @param special     Tags from [Constants.TAG_*] indicating special-access grants.
 *                    Person 1 leaves this empty; Person 3 fills it before scoring.
 */
data class AppInfo(
    val pkg: String,
    val label: String,
    val version: String,
    val installTime: Long,
    val installer: String?,
    val targetSdk: Int,
    val isSystem: Boolean,
    val perms: List<PermInfo>,
    var special: Set<String> = emptySet(),   // mutable so glue code can assign P3's result
)

// ── Scoring output ────────────────────────────────────────────────────────────

/**
 * Result produced by Person 2 (scoring) for one [AppInfo].
 *
 * @param score    0–100 integer risk score.
 * @param band     One of [Constants.BAND_LOW/MEDIUM/HIGH/CRITICAL].
 * @param reasons  Plain-English reasons; at least 3 for HIGH/CRITICAL.
 * @param persona  Spy-card persona string (e.g. "The Letter Opener").
 */
data class RiskResult(
    val score: Int,
    val band: String,
    val reasons: List<String>,
    val persona: String,
)
