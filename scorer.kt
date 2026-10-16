package com.enterprise.riskauditor.scoring

import com.enterprise.riskauditor.contract.AppInfo
import com.enterprise.riskauditor.contract.RiskResult
import com.enterprise.riskauditor.contract.Constants.Bands
import com.enterprise.riskauditor.contract.Constants.Rooms
import com.enterprise.riskauditor.contract.Constants.Tags
import com.enterprise.riskauditor.contract.Constants.TRUSTED_INSTALLERS
import com.enterprise.riskauditor.scoring.RoomCatalog.ALL_SPECIAL_TAGS
import com.enterprise.riskauditor.scoring.RoomCatalog.BACKGROUND_PERMS
import com.enterprise.riskauditor.scoring.RoomCatalog.CAMERA_MIC_PERMS
import com.enterprise.riskauditor.scoring.RoomCatalog.CONTACTS_PERMS
import com.enterprise.riskauditor.scoring.RoomCatalog.LOCATION_ALL_PERMS
import com.enterprise.riskauditor.scoring.RoomCatalog.LOCATION_BASE_PERMS
import com.enterprise.riskauditor.scoring.RoomCatalog.LOCATION_BG_PERMS
import com.enterprise.riskauditor.scoring.RoomCatalog.NETWORK_PERMS
import com.enterprise.riskauditor.scoring.RoomCatalog.PHONE_IDENTITY_PERMS
import com.enterprise.riskauditor.scoring.RoomCatalog.PERM_TO_ROOM
import com.enterprise.riskauditor.scoring.RoomCatalog.SMS_PERMS
import com.enterprise.riskauditor.scoring.RoomCatalog.STORAGE_PERMS

/**
 * Scores a single [AppInfo] and returns a fully-populated [RiskResult].
 *
 * ## Scoring overview
 * 1. Room points — each room contributes a fixed number of points.
 *    - If at least one trigger permission in a room is **granted**, full points.
 *    - If triggers are only **declared-not-granted**, 25 % of full points.
 * 2. Special-access room — 15 pts per tag (capped at 40); tags always count in full.
 * 3. Location split — base (fine/coarse) 8 pts + background +4 pts; 25 % rule
 *    applied to each part separately.
 * 4. Combination bonuses — fired on granted permissions and detected tags.
 * 5. Score rounded once at the end, capped at 100.
 *
 * ## Thread safety
 * Pure function; no shared mutable state; safe to call from any thread.
 */
object Scorer {

    // ── Device-admin allowlist ───────────────────────────────────────────────
    /**
     * Package prefixes for known legitimate device-management apps.
     * The +10 device-admin combo bonus does NOT fire for apps whose package
     * starts with any of these prefixes.
     *
     * Configurable: add entries here to extend the allowlist without touching
     * scoring logic.
     */
    val DEVICE_ADMIN_ALLOWLIST_PREFIXES: Set<String> = setOf(
        "com.google.android.apps.work",
        "com.microsoft.intune",
        "com.airwatch",
        "com.android.dpc",
        "com.samsung.android.knox"
    )

    // ── Context-mismatch keyword sets ────────────────────────────────────────
    /** App-type keywords that indicate an innocuous utility. */
    private val INNOCUOUS_APP_KEYWORDS: Set<String> = setOf(
        "torch", "flashlight", "calculator", "wallpaper", "clock"
    )

    /** Permissions that are "sensitive data" for the context-mismatch bonus. */
    private val CONTEXT_MISMATCH_PERMS: Map<Set<String>, String> = mapOf(
        SMS_PERMS      to "SMS",
        CONTACTS_PERMS to "contacts"
        // call-log is a subset of CONTACTS_PERMS; covered by that entry
    )

    // ── Main entry point ─────────────────────────────────────────────────────

    /**
     * Convert an [AppInfo] into a [RiskResult].
     *
     * Deterministic: same input always produces the same output.
     * No Android imports: fully unit-testable on the JVM.
     *
     * @param app AppInfo with [AppInfo.special] already populated by Person 3
     * @return fully scored [RiskResult]
     */
    fun scoreApp(app: AppInfo): RiskResult {
        val granted   = app.grantedPerms
        val declared  = app.declaredPerms
        val special   = app.special
        val sideloaded = isSideloaded(app)

        // Accumulate (reason-text, raw-points, room) triples
        val contributions = mutableListOf<Contribution>()

        // Track raw room points (for persona selection — combos excluded)
        val roomPoints = mutableMapOf<String, Double>()

        // ── 1. Special-access room ──────────────────────────────────────────
        val presentTags = special.intersect(ALL_SPECIAL_TAGS)
        if (presentTags.isNotEmpty()) {
            // 15 pts per tag, room total capped at 40; tags always full
            val rawTagPoints = presentTags.size * 15.0
            val cappedPoints = rawTagPoints.coerceAtMost(40.0)
            val tagLabels = presentTags.joinToString(", ") { tagLabel(it) }
            contributions += Contribution(
                reason = "Has special system access: $tagLabels — " +
                        "giving it deep control over your device.",
                points = cappedPoints,
                room   = Rooms.SPECIAL_ACCESS
            )
            roomPoints[Rooms.SPECIAL_ACCESS] = cappedPoints
        }

        // ── 2. SMS room (15 pts) ────────────────────────────────────────────
        addRoomContribution(
            contributions, roomPoints,
            room = Rooms.SMS,
            fullPoints = 15.0,
            triggerPerms = SMS_PERMS,
            granted = granted, declared = declared,
            fullReason   = "Can read your SMS, which can include bank codes and one-time passwords.",
            partialReason= "Wants to read your SMS (not yet granted)."
        )

        // ── 3. Contacts + call logs room (10 pts) ──────────────────────────
        addRoomContribution(
            contributions, roomPoints,
            room = Rooms.CONTACTS_CALL_LOGS,
            fullPoints = 10.0,
            triggerPerms = CONTACTS_PERMS,
            granted = granted, declared = declared,
            fullReason   = "Can access your contacts and call history.",
            partialReason= "Wants access to your contacts and call history (not yet granted)."
        )

        // ── 4. Camera + mic room (10 pts) ───────────────────────────────────
        addRoomContribution(
            contributions, roomPoints,
            room = Rooms.CAMERA_MIC,
            fullPoints = 10.0,
            triggerPerms = CAMERA_MIC_PERMS,
            granted = granted, declared = declared,
            fullReason   = "Can use your camera or microphone to capture audio and video.",
            partialReason= "Wants to use your camera or microphone (not yet granted)."
        )

        // ── 5. Location room (split: base 8 pts + bg +4 pts) ───────────────
        run {
            val baseGranted   = granted.intersect(LOCATION_BASE_PERMS).isNotEmpty()
            val baseDeclared  = declared.intersect(LOCATION_BASE_PERMS).isNotEmpty()
            val bgGranted     = granted.intersect(LOCATION_BG_PERMS).isNotEmpty()
            val bgDeclared    = declared.intersect(LOCATION_BG_PERMS).isNotEmpty()

            var locationPts = 0.0

            if (baseGranted) {
                locationPts += 8.0
                contributions += Contribution(
                    reason = "Can see your precise or approximate location.",
                    points = 8.0, room = Rooms.LOCATION
                )
            } else if (baseDeclared) {
                locationPts += 2.0   // 25 % of 8
                contributions += Contribution(
                    reason = "Wants your location (not yet granted).",
                    points = 2.0, room = Rooms.LOCATION
                )
            }

            if (bgGranted) {
                locationPts += 4.0
                contributions += Contribution(
                    reason = "Can track your location even when the app is closed.",
                    points = 4.0, room = Rooms.LOCATION
                )
            } else if (bgDeclared) {
                locationPts += 1.0   // 25 % of 4
                contributions += Contribution(
                    reason = "Wants background location tracking (not yet granted).",
                    points = 1.0, room = Rooms.LOCATION
                )
            }

            if (locationPts > 0) roomPoints[Rooms.LOCATION] = locationPts
        }

        // ── 6. Phone + identity room (5 pts) ───────────────────────────────
        addRoomContribution(
            contributions, roomPoints,
            room = Rooms.PHONE_IDENTITY,
            fullPoints = 5.0,
            triggerPerms = PHONE_IDENTITY_PERMS,
            granted = granted, declared = declared,
            fullReason   = "Can read your phone number and device identifiers.",
            partialReason= "Wants to read your phone number and device identifiers (not yet granted)."
        )

        // ── 7. Storage + media room (5 pts) ────────────────────────────────
        addRoomContribution(
            contributions, roomPoints,
            room = Rooms.STORAGE_MEDIA,
            fullPoints = 5.0,
            triggerPerms = STORAGE_PERMS,
            granted = granted, declared = declared,
            fullReason   = "Can read or write files on your device storage.",
            partialReason= "Wants access to your device storage (not yet granted)."
        )

        // ── 8. Background + persistence room (5 pts) ───────────────────────
        addRoomContribution(
            contributions, roomPoints,
            room = Rooms.BACKGROUND,
            fullPoints = 5.0,
            triggerPerms = BACKGROUND_PERMS,
            granted = granted, declared = declared,
            fullReason   = "Starts itself every time the phone restarts.",
            partialReason= "Wants to run in the background at startup (not yet granted)."
        )

        // ── 9. Network room (2 pts) ─────────────────────────────────────────
        addRoomContribution(
            contributions, roomPoints,
            room = Rooms.NETWORK,
            fullPoints = 2.0,
            triggerPerms = NETWORK_PERMS,
            granted = granted, declared = declared,
            fullReason   = "Can connect to the internet.",
            partialReason= "Wants internet access (not yet granted)."
        )

        // ── 10. Combination bonuses ─────────────────────────────────────────
        // +20: SMS exfil combo
        val hasSmsGrant  = granted.intersect(setOf(
            "android.permission.READ_SMS", "android.permission.RECEIVE_SMS")).isNotEmpty()
        val hasInternet  = "android.permission.INTERNET" in granted
        val hasBoot      = "android.permission.RECEIVE_BOOT_COMPLETED" in granted

        if (hasSmsGrant && hasInternet && hasBoot && sideloaded) {
            contributions += Contribution(
                reason = "Dangerous combo (+20): reads your SMS, connects to the internet, " +
                        "auto-starts on reboot, and was sideloaded — classic spyware pattern.",
                points = 20.0, room = null
            )
        }

        // +15: Accessibility + Overlay tags
        if (Tags.ACCESSIBILITY in special && Tags.OVERLAY in special) {
            contributions += Contribution(
                reason = "Can watch your screen and tap for you (Accessibility) while drawing " +
                        "invisible overlays on top of other apps — a perfect click-jacking setup (+15).",
                points = 15.0, room = null
            )
        }

        // +10: Device admin on non-allowlisted app
        if (Tags.DEVICE_ADMIN in special && !isAllowlistedDeviceAdmin(app.pkg)) {
            contributions += Contribution(
                reason = "Has device-administrator privileges — can lock or wipe your phone, " +
                        "and it is not a recognised management tool (+10).",
                points = 10.0, room = null
            )
        }

        // +10: Sideloaded
        if (sideloaded) {
            contributions += Contribution(
                reason = "Was installed from outside the Play Store — no Google safety review.",
                points = 10.0, room = null
            )
        }

        // +15: Context mismatch
        contextMismatchBonus(app, granted)?.let { contributions += it }

        // ── Compute total score ─────────────────────────────────────────────
        val rawScore = contributions.sumOf { it.points }
        val score    = rawScore.toInt().coerceAtMost(100).coerceAtLeast(0)

        // ── Determine band ─────────────────────────────────────────────────
        val band = scoreToBand(score)

        // ── Build reasons list ──────────────────────────────────────────────
        val reasons = buildReasons(contributions, band)

        // ── Select persona ──────────────────────────────────────────────────
        val (persona, tagline) = PersonaCatalog.selectPersona(roomPoints)

        // ── Build rooms map ─────────────────────────────────────────────────
        val rooms = buildRoomsMap(app)

        return RiskResult(
            score    = score,
            band     = band,
            persona  = persona,
            tagline  = tagline,
            reasons  = reasons,
            rooms    = rooms
        )
    }

    // ── recommendedFix ───────────────────────────────────────────────────────

    /**
     * Returns a plain-text action recommendation based on the risk band.
     *
     * CRITICAL → uninstall immediately.
     * HIGH     → names the single biggest room for targeted action.
     * MEDIUM   → prompt a permission review.
     * LOW      → no action needed.
     */
    @Suppress("UNUSED_PARAMETER")
    fun recommendedFix(app: AppInfo, result: RiskResult): String = when (result.band) {
        Bands.CRITICAL -> "Uninstall now"
        Bands.HIGH -> {
            // Find the room with the highest points contribution in rooms map
            val topRoom = result.rooms.keys
                .maxByOrNull { roomRank(it) } ?: "permissions"
            "Turn off $topRoom access"
        }
        Bands.MEDIUM   -> "Review its permissions"
        else           -> "No action needed"
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    /**
     * An intermediate accumulation unit: a reason sentence, the raw points it
     * contributed, and (optionally) the room it came from.
     * [room] is null for combination bonuses.
     */
    private data class Contribution(
        val reason: String,
        val points: Double,
        val room: String?
    )

    /**
     * Adds a room contribution for a simple "any trigger = full points" room.
     * Applies the 25 % declared-not-granted rule automatically.
     */
    private fun addRoomContribution(
        contributions: MutableList<Contribution>,
        roomPoints: MutableMap<String, Double>,
        room: String,
        fullPoints: Double,
        triggerPerms: Set<String>,
        granted: Set<String>,
        declared: Set<String>,
        fullReason: String,
        partialReason: String
    ) {
        val hasGranted  = granted.intersect(triggerPerms).isNotEmpty()
        val hasDeclared = declared.intersect(triggerPerms).isNotEmpty()

        when {
            hasGranted -> {
                contributions += Contribution(reason = fullReason,  points = fullPoints,        room = room)
                roomPoints[room] = fullPoints
            }
            hasDeclared -> {
                val pts = fullPoints * 0.25
                contributions += Contribution(reason = partialReason, points = pts, room = room)
                roomPoints[room] = pts
            }
            // else: room does not apply at all
        }
    }

    /**
     * Returns true when the app was not installed through a trusted store channel.
     * System apps are never considered sideloaded (they ship with the firmware).
     */
    private fun isSideloaded(app: AppInfo): Boolean {
        if (app.isSystem) return false
        val installer = app.installerPackage ?: return true   // null → unknown source
        return installer !in TRUSTED_INSTALLERS
    }

    /**
     * Returns true if [pkg] starts with any of the device-admin allowlist prefixes.
     * Package must be lowercased; Android package names are case-sensitive but
     * conventionally lowercase.
     */
    private fun isAllowlistedDeviceAdmin(pkg: String): Boolean =
        DEVICE_ADMIN_ALLOWLIST_PREFIXES.any { pkg.startsWith(it) }

    /**
     * Context-mismatch bonus (+15): the app name or package looks like a harmless
     * utility (torch, calculator, etc.) but it holds SMS or contacts permissions.
     *
     * Reason text adapts the utility type and data type to what actually matched,
     * making the explanation concrete for the end user.
     */
    private fun contextMismatchBonus(app: AppInfo, granted: Set<String>): Contribution? {
        val needle = INNOCUOUS_APP_KEYWORDS.firstOrNull { kw ->
            app.label.lowercase().contains(kw) || app.pkg.lowercase().contains(kw)
        } ?: return null

        // Determine which sensitive data type is actually granted
        val dataType = when {
            granted.intersect(SMS_PERMS).isNotEmpty()      -> "SMS"
            granted.intersect(CONTACTS_PERMS).isNotEmpty() -> "contacts or call history"
            else -> return null   // no sensitive data grant → bonus does not fire
        }

        // Adapt the pizza/house-keys metaphor to the specific mismatch
        val appTypeWord = needle.replaceFirstChar { it.titlecase() }
        return Contribution(
            reason = "A $appTypeWord app asking for your $dataType is like a pizza guy asking for " +
                    "your house keys — there is no good reason for it (+15).",
            points = 15.0,
            room   = null
        )
    }

    /**
     * Selects and caps the reasons list according to spec:
     * - Sort by points descending.
     * - Return top 5.
     * - HIGH/CRITICAL must have at least 3 reasons (add extra per-room sentences if needed).
     * - LOW with nothing scary gets one reassuring sentence.
     */
    private fun buildReasons(contributions: List<Contribution>, band: String): List<String> {
        val sorted = contributions.sortedByDescending { it.points }
        val top5   = sorted.take(5)

        if (band == Bands.LOW && top5.isEmpty()) {
            return listOf("No significant risks detected — this app appears safe.")
        }

        // For HIGH/CRITICAL, ensure at least 3 reasons
        if ((band == Bands.HIGH || band == Bands.CRITICAL) && top5.size < 3) {
            // All room contributions are already in the list; take what we have
            // (this can only happen if exactly 1-2 rules fired and score is still HIGH,
            //  which is nearly impossible given the rubric, but we guard it defensively)
            val allSorted = contributions.sortedByDescending { it.points }
            return allSorted.map { it.reason }.distinct().take(5)
        }

        return top5.map { it.reason }
    }

    /**
     * Builds the rooms map: room name → list of permission/tag strings present
     * in that room (both declared and granted; special tags from app.special).
     * Empty rooms are omitted.
     */
    private fun buildRoomsMap(app: AppInfo): Map<String, List<String>> {
        val all = (app.declaredPerms + app.grantedPerms + app.special).toSet()
        val map = mutableMapOf<String, MutableList<String>>()
        for (item in all) {
            val room = PERM_TO_ROOM[item] ?: continue
            map.getOrPut(room) { mutableListOf() }.add(item)
        }
        return map.mapValues { (_, v) -> v.distinct().sorted() }
            .filterValues { it.isNotEmpty() }
    }

    /** Maps a 0-100 score to a band constant. */
    private fun scoreToBand(score: Int): String = when {
        score >= 75 -> Bands.CRITICAL
        score >= 50 -> Bands.HIGH
        score >= 25 -> Bands.MEDIUM
        else        -> Bands.LOW
    }

    /**
     * Returns a rank value for room-based HIGH recommendation.
     * Higher rank = more dangerous = shown in the fix string.
     * Uses the same tie-break order as PersonaCatalog.
     */
    private fun roomRank(room: String): Int = when (room) {
        Rooms.SPECIAL_ACCESS     -> 9
        Rooms.SMS                -> 8
        Rooms.CONTACTS_CALL_LOGS -> 7
        Rooms.CAMERA_MIC         -> 6
        Rooms.LOCATION           -> 5
        Rooms.PHONE_IDENTITY     -> 4
        Rooms.STORAGE_MEDIA      -> 3
        Rooms.BACKGROUND         -> 2
        Rooms.NETWORK            -> 1
        else                     -> 0
    }

    /** Human-readable label for a special-access tag used in reason sentences. */
    private fun tagLabel(tag: String): String = when (tag) {
        Tags.ACCESSIBILITY         -> "Accessibility"
        Tags.DEVICE_ADMIN          -> "Device Admin"
        Tags.NOTIFICATION_LISTENER -> "Notification Listener"
        Tags.OVERLAY               -> "Overlay"
        Tags.INSTALL_APPS          -> "Install Apps"
        Tags.USAGE_STATS           -> "Usage Stats"
        else                       -> tag
    }
}
