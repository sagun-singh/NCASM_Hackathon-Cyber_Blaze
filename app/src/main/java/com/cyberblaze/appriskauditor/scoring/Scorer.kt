package com.cyberblaze.appriskauditor.scoring

import com.cyberblaze.appriskauditor.data.AppInfo
import com.cyberblaze.appriskauditor.data.Constants
import com.cyberblaze.appriskauditor.data.RiskResult
import com.cyberblaze.appriskauditor.scoring.RoomCatalog.ALL_SPECIAL_TAGS
import com.cyberblaze.appriskauditor.scoring.RoomCatalog.BACKGROUND_PERMS
import com.cyberblaze.appriskauditor.scoring.RoomCatalog.CAMERA_MIC_PERMS
import com.cyberblaze.appriskauditor.scoring.RoomCatalog.CONTACTS_PERMS
import com.cyberblaze.appriskauditor.scoring.RoomCatalog.LOCATION_BASE_PERMS
import com.cyberblaze.appriskauditor.scoring.RoomCatalog.LOCATION_BG_PERMS
import com.cyberblaze.appriskauditor.scoring.RoomCatalog.NETWORK_PERMS
import com.cyberblaze.appriskauditor.scoring.RoomCatalog.PHONE_IDENTITY_PERMS
import com.cyberblaze.appriskauditor.scoring.RoomCatalog.PERM_TO_ROOM
import com.cyberblaze.appriskauditor.scoring.RoomCatalog.SMS_PERMS
import com.cyberblaze.appriskauditor.scoring.RoomCatalog.STORAGE_PERMS

/**
 * Converts an [AppInfo] (from Person 1's scanner) into a [RiskResult].
 *
 * Pure Kotlin — no Android imports — fully unit-testable on the JVM.
 * Deterministic: same input always produces the same output.
 *
 * Call [scoreApp] AFTER Person 3 has populated [AppInfo.special].
 */
object Scorer {

    // ── Device-admin allowlist ───────────────────────────────────────────────
    /** Apps whose package starts with these prefixes are exempt from the +10 DevAdmin bonus. */
    val DEVICE_ADMIN_ALLOWLIST_PREFIXES: Set<String> = setOf(
        "com.google.android.apps.work",
        "com.microsoft.intune",
        "com.airwatch",
        "com.android.dpc",
        "com.samsung.android.knox"
    )

    /** Trusted installer package names — not considered sideloaded. */
    private val TRUSTED_INSTALLERS: Set<String> = setOf(
        Constants.INSTALLER_PLAY_STORE,
        "com.amazon.venezia",
        "com.sec.android.app.samsungapps"
    )

    /** App-type keywords for context-mismatch bonus. */
    private val INNOCUOUS_KEYWORDS: Set<String> =
        setOf("torch", "flashlight", "calculator", "wallpaper", "clock")

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Score a single app.
     *
     * @param app AppInfo with [AppInfo.special] already populated by Person 3.
     * @return fully populated [RiskResult].
     */
    fun scoreApp(app: AppInfo): RiskResult {
        // Derive granted and declared sets from the PermInfo list
        val granted  = app.perms.filter { it.granted }.map { it.name }.toSet()
        val declared = app.perms.map { it.name }.toSet()   // declared includes granted
        val special  = app.special
        val sideloaded = isSideloaded(app)

        val contributions = mutableListOf<Contribution>()
        val roomPoints    = mutableMapOf<String, Double>()  // room → raw pts (combos excluded)

        // ── 1. Special access (15/tag, capped at 40) ───────────────────────
        val presentTags = special.intersect(ALL_SPECIAL_TAGS)
        if (presentTags.isNotEmpty()) {
            val pts = (presentTags.size * 15.0).coerceAtMost(40.0)
            val labels = presentTags.joinToString(", ") { tagLabel(it) }
            contributions += Contribution(
                "Has special system access: $labels — giving it deep control over your device.",
                pts, RoomCatalog.ROOM_SPECIAL
            )
            roomPoints[RoomCatalog.ROOM_SPECIAL] = pts
        }

        // ── 2. SMS (15 pts) ─────────────────────────────────────────────────
        addRoom(contributions, roomPoints, RoomCatalog.ROOM_SMS, 15.0, SMS_PERMS, granted, declared,
            "Can read your SMS, which can include bank codes and one-time passwords.",
            "Wants to read your SMS (not yet granted).")

        // ── 3. Contacts + call logs (10 pts) ───────────────────────────────
        addRoom(contributions, roomPoints, RoomCatalog.ROOM_CONTACTS, 10.0, CONTACTS_PERMS, granted, declared,
            "Can access your contacts and call history.",
            "Wants access to your contacts and call history (not yet granted).")

        // ── 4. Camera + mic (10 pts) ────────────────────────────────────────
        addRoom(contributions, roomPoints, RoomCatalog.ROOM_CAM_MIC, 10.0, CAMERA_MIC_PERMS, granted, declared,
            "Can use your camera or microphone to capture audio and video.",
            "Wants to use your camera or microphone (not yet granted).")

        // ── 5. Location — split scoring ─────────────────────────────────────
        run {
            val baseG = granted.intersect(LOCATION_BASE_PERMS).isNotEmpty()
            val baseD = declared.intersect(LOCATION_BASE_PERMS).isNotEmpty()
            val bgG   = granted.intersect(LOCATION_BG_PERMS).isNotEmpty()
            val bgD   = declared.intersect(LOCATION_BG_PERMS).isNotEmpty()
            var locPts = 0.0
            if (baseG) {
                locPts += 8.0
                contributions += Contribution("Can see your precise or approximate location.", 8.0, RoomCatalog.ROOM_LOCATION)
            } else if (baseD) {
                locPts += 2.0  // 25% of 8
                contributions += Contribution("Wants your location (not yet granted).", 2.0, RoomCatalog.ROOM_LOCATION)
            }
            if (bgG) {
                locPts += 4.0
                contributions += Contribution("Can track your location even when the app is closed.", 4.0, RoomCatalog.ROOM_LOCATION)
            } else if (bgD) {
                locPts += 1.0  // 25% of 4
                contributions += Contribution("Wants background location tracking (not yet granted).", 1.0, RoomCatalog.ROOM_LOCATION)
            }
            if (locPts > 0) roomPoints[RoomCatalog.ROOM_LOCATION] = locPts
        }

        // ── 6. Phone + identity (5 pts) ─────────────────────────────────────
        addRoom(contributions, roomPoints, RoomCatalog.ROOM_PHONE, 5.0, PHONE_IDENTITY_PERMS, granted, declared,
            "Can read your phone number and device identifiers.",
            "Wants to read your phone number (not yet granted).")

        // ── 7. Storage + media (5 pts) ──────────────────────────────────────
        addRoom(contributions, roomPoints, RoomCatalog.ROOM_STORAGE, 5.0, STORAGE_PERMS, granted, declared,
            "Can read or write files on your device storage.",
            "Wants access to your device storage (not yet granted).")

        // ── 8. Background + persistence (5 pts) ────────────────────────────
        addRoom(contributions, roomPoints, RoomCatalog.ROOM_BACKGROUND, 5.0, BACKGROUND_PERMS, granted, declared,
            "Starts itself every time the phone restarts.",
            "Wants to run in the background at startup (not yet granted).")

        // ── 9. Network (2 pts) ──────────────────────────────────────────────
        addRoom(contributions, roomPoints, RoomCatalog.ROOM_NETWORK, 2.0, NETWORK_PERMS, granted, declared,
            "Can connect to the internet.",
            "Wants internet access (not yet granted).")

        // ── Combination bonuses ─────────────────────────────────────────────
        // +20: SMS exfil combo
        val hasSmsGrant = granted.intersect(setOf("android.permission.READ_SMS", "android.permission.RECEIVE_SMS")).isNotEmpty()
        val hasInternet = "android.permission.INTERNET" in granted
        val hasBoot     = "android.permission.RECEIVE_BOOT_COMPLETED" in granted
        if (hasSmsGrant && hasInternet && hasBoot && sideloaded) {
            contributions += Contribution(
                "Dangerous combo (+20): reads your SMS, connects to the internet, " +
                "auto-starts on reboot, and was sideloaded — classic spyware pattern.",
                20.0, null
            )
        }

        // +15: Accessibility + Overlay together
        if (Constants.TAG_ACCESSIBILITY in special && Constants.TAG_OVERLAY in special) {
            contributions += Contribution(
                "Can watch your screen and tap for you (Accessibility) while drawing " +
                "invisible overlays on top of other apps — a perfect click-jacking setup (+15).",
                15.0, null
            )
        }

        // +10: Device admin on non-allowlisted app
        if (Constants.TAG_DEVICE_ADMIN in special && !isAllowlistedAdmin(app.pkg)) {
            contributions += Contribution(
                "Has device-administrator privileges — can lock or wipe your phone, " +
                "and it is not a recognised management tool (+10).",
                10.0, null
            )
        }

        // +10: Sideloaded
        if (sideloaded) {
            contributions += Contribution(
                "Was installed from outside the Play Store — no Google safety review.",
                10.0, null
            )
        }

        // +15: Context mismatch
        contextMismatch(app, granted)?.let { contributions += it }

        // ── Final score ─────────────────────────────────────────────────────
        val raw   = contributions.sumOf { it.points }
        val score = raw.toInt().coerceIn(0, 100)
        val band  = toBand(score)

        val reasons = buildReasons(contributions, band)
        val (persona, _) = PersonaCatalog.selectPersona(roomPoints)
        val rooms = buildRoomsMap(app)

        return RiskResult(
            score   = score,
            band    = band,
            reasons = reasons,
            rooms   = rooms,
            persona = persona
        )
    }

    /**
     * Plain-text action recommendation.
     * CRITICAL → uninstall. HIGH → name the worst room. MEDIUM → review. LOW → nothing.
     */
    @Suppress("UNUSED_PARAMETER")
    fun recommendedFix(app: AppInfo, result: RiskResult): String = when (result.band) {
        Constants.BAND_CRITICAL -> "Uninstall now"
        Constants.BAND_HIGH -> {
            val topRoom = result.rooms.keys.maxByOrNull { roomRank(it) } ?: "permissions"
            "Turn off $topRoom access"
        }
        Constants.BAND_MEDIUM -> "Review its permissions"
        else                  -> "No action needed"
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    private data class Contribution(val reason: String, val points: Double, val room: String?)

    private fun addRoom(
        contributions: MutableList<Contribution>,
        roomPoints: MutableMap<String, Double>,
        room: String,
        fullPts: Double,
        triggers: Set<String>,
        granted: Set<String>,
        declared: Set<String>,
        fullReason: String,
        partialReason: String
    ) {
        when {
            granted.intersect(triggers).isNotEmpty() -> {
                contributions += Contribution(fullReason, fullPts, room)
                roomPoints[room] = fullPts
            }
            declared.intersect(triggers).isNotEmpty() -> {
                val pts = fullPts * 0.25
                contributions += Contribution(partialReason, pts, room)
                roomPoints[room] = pts
            }
        }
    }

    private fun isSideloaded(app: AppInfo): Boolean {
        if (app.isSystem) return false
        val installer = app.installer ?: return true
        return installer !in TRUSTED_INSTALLERS
    }

    private fun isAllowlistedAdmin(pkg: String): Boolean =
        DEVICE_ADMIN_ALLOWLIST_PREFIXES.any { pkg.startsWith(it) }

    private fun contextMismatch(app: AppInfo, granted: Set<String>): Contribution? {
        val kw = INNOCUOUS_KEYWORDS.firstOrNull {
            app.label.lowercase().contains(it) || app.pkg.lowercase().contains(it)
        } ?: return null
        val dataType = when {
            granted.intersect(SMS_PERMS).isNotEmpty()      -> "SMS"
            granted.intersect(CONTACTS_PERMS).isNotEmpty() -> "contacts"
            else -> return null
        }
        val appWord = kw.replaceFirstChar { it.titlecase() }
        return Contribution(
            "A $appWord app asking for your $dataType is like a pizza guy asking for " +
            "your house keys — there is no good reason for it (+15).",
            15.0, null
        )
    }

    private fun buildReasons(contributions: List<Contribution>, band: String): List<String> {
        val sorted = contributions.sortedByDescending { it.points }
        if (band == Constants.BAND_LOW && sorted.isEmpty())
            return listOf("No significant risks detected — this app appears safe.")
        return sorted.map { it.reason }.distinct().take(5)
    }

    private fun buildRoomsMap(app: AppInfo): Map<String, List<String>> {
        val all = (app.perms.map { it.name } + app.special).toSet()
        val map = mutableMapOf<String, MutableList<String>>()
        for (item in all) {
            val room = PERM_TO_ROOM[item] ?: continue
            map.getOrPut(room) { mutableListOf() }.add(item)
        }
        return map.mapValues { (_, v) -> v.distinct().sorted() }
            .filterValues { it.isNotEmpty() }
    }

    private fun toBand(score: Int): String = when {
        score >= 75 -> Constants.BAND_CRITICAL
        score >= 50 -> Constants.BAND_HIGH
        score >= 25 -> Constants.BAND_MEDIUM
        else        -> Constants.BAND_LOW
    }

    private fun roomRank(room: String): Int = when (room) {
        RoomCatalog.ROOM_SPECIAL    -> 9
        RoomCatalog.ROOM_SMS        -> 8
        RoomCatalog.ROOM_CONTACTS   -> 7
        RoomCatalog.ROOM_CAM_MIC    -> 6
        RoomCatalog.ROOM_LOCATION   -> 5
        RoomCatalog.ROOM_PHONE      -> 4
        RoomCatalog.ROOM_STORAGE    -> 3
        RoomCatalog.ROOM_BACKGROUND -> 2
        RoomCatalog.ROOM_NETWORK    -> 1
        else                        -> 0
    }

    private fun tagLabel(tag: String): String = when (tag) {
        Constants.TAG_ACCESSIBILITY         -> "Accessibility"
        Constants.TAG_DEVICE_ADMIN          -> "Device Admin"
        Constants.TAG_NOTIFICATION_LISTENER -> "Notification Listener"
        Constants.TAG_OVERLAY               -> "Overlay"
        Constants.TAG_INSTALL_APPS          -> "Install Apps"
        Constants.TAG_USAGE_STATS           -> "Usage Stats"
        else                                -> tag
    }
}
