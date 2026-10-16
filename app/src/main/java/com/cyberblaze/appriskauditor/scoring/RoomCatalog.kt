package com.cyberblaze.appriskauditor.scoring

import com.cyberblaze.appriskauditor.data.Constants

/**
 * Maps every scoreable permission string (and special-access tag) to its room.
 *
 * Permissions that overlap with special-access (SYSTEM_ALERT_WINDOW,
 * BIND_ACCESSIBILITY_SERVICE, etc.) are intentionally absent — scored
 * exclusively via app.special tags to avoid double-counting.
 */
object RoomCatalog {

    // ── Special-access tags ─────────────────────────────────────────────────
    val ALL_SPECIAL_TAGS: Set<String> = setOf(
        Constants.TAG_ACCESSIBILITY,
        Constants.TAG_DEVICE_ADMIN,
        Constants.TAG_NOTIFICATION_LISTENER,
        Constants.TAG_OVERLAY,
        Constants.TAG_INSTALL_APPS,
        Constants.TAG_USAGE_STATS
    )

    // ── Room trigger permission sets ────────────────────────────────────────
    val SMS_PERMS: Set<String> = setOf(
        "android.permission.READ_SMS",
        "android.permission.RECEIVE_SMS",
        "android.permission.SEND_SMS",
        "android.permission.RECEIVE_MMS",
        "android.permission.RECEIVE_WAP_PUSH"
    )

    val CONTACTS_PERMS: Set<String> = setOf(
        "android.permission.READ_CONTACTS",
        "android.permission.WRITE_CONTACTS",
        "android.permission.READ_CALL_LOG",
        "android.permission.WRITE_CALL_LOG",
        "android.permission.PROCESS_OUTGOING_CALLS"
    )

    val CAMERA_MIC_PERMS: Set<String> = setOf(
        "android.permission.CAMERA",
        "android.permission.RECORD_AUDIO"
    )

    val LOCATION_BASE_PERMS: Set<String> = setOf(
        "android.permission.ACCESS_FINE_LOCATION",
        "android.permission.ACCESS_COARSE_LOCATION"
    )
    val LOCATION_BG_PERMS: Set<String> = setOf(
        "android.permission.ACCESS_BACKGROUND_LOCATION"
    )
    val LOCATION_ALL_PERMS: Set<String> = LOCATION_BASE_PERMS + LOCATION_BG_PERMS

    val PHONE_IDENTITY_PERMS: Set<String> = setOf(
        "android.permission.READ_PHONE_STATE",
        "android.permission.READ_PHONE_NUMBERS",
        "android.permission.CALL_PHONE",
        "android.permission.ANSWER_PHONE_CALLS"
    )

    val STORAGE_PERMS: Set<String> = setOf(
        "android.permission.READ_MEDIA_IMAGES",
        "android.permission.READ_MEDIA_VIDEO",
        "android.permission.READ_MEDIA_AUDIO",
        "android.permission.READ_EXTERNAL_STORAGE",
        "android.permission.WRITE_EXTERNAL_STORAGE",
        "android.permission.MANAGE_EXTERNAL_STORAGE"
    )

    val BACKGROUND_PERMS: Set<String> = setOf(
        "android.permission.RECEIVE_BOOT_COMPLETED",
        "android.permission.FOREGROUND_SERVICE",
        "android.permission.IGNORE_BATTERY_OPTIMIZATIONS"
    )

    val NETWORK_PERMS: Set<String> = setOf(
        "android.permission.INTERNET",
        "android.permission.ACCESS_NETWORK_STATE"
    )

    // ── Room name constants (used as keys in RiskResult.rooms) ──────────────
    const val ROOM_SPECIAL     = "Special access"
    const val ROOM_SMS         = "SMS"
    const val ROOM_CONTACTS    = "Contacts + call logs"
    const val ROOM_CAM_MIC     = "Camera + mic"
    const val ROOM_LOCATION    = "Location"
    const val ROOM_PHONE       = "Phone + identity"
    const val ROOM_STORAGE     = "Storage + media"
    const val ROOM_BACKGROUND  = "Background + persistence"
    const val ROOM_NETWORK     = "Network"

    /** Flat reverse-lookup: permission string or special tag → room name. */
    val PERM_TO_ROOM: Map<String, String> by lazy {
        buildMap {
            ALL_SPECIAL_TAGS.forEach     { put(it, ROOM_SPECIAL) }
            SMS_PERMS.forEach            { put(it, ROOM_SMS) }
            CONTACTS_PERMS.forEach       { put(it, ROOM_CONTACTS) }
            CAMERA_MIC_PERMS.forEach     { put(it, ROOM_CAM_MIC) }
            LOCATION_ALL_PERMS.forEach   { put(it, ROOM_LOCATION) }
            PHONE_IDENTITY_PERMS.forEach { put(it, ROOM_PHONE) }
            STORAGE_PERMS.forEach        { put(it, ROOM_STORAGE) }
            BACKGROUND_PERMS.forEach     { put(it, ROOM_BACKGROUND) }
            NETWORK_PERMS.forEach        { put(it, ROOM_NETWORK) }
        }
    }
}
