package com.enterprise.riskauditor.contract

/**
 * Shared constants for the On-Device App Risk Auditor.
 * READ-ONLY: all team members import from here, never re-type these strings.
 */
object Constants {

    // ── Risk band names ──────────────────────────────────────────────────────
    const val BAND_LOW      = "LOW"
    const val BAND_MEDIUM   = "MEDIUM"
    const val BAND_HIGH     = "HIGH"
    const val BAND_CRITICAL = "CRITICAL"

    // ── Permission-category "room" names ─────────────────────────────────────
    const val ROOM_LOCATION       = "Location"
    const val ROOM_MICROPHONE     = "Microphone"
    const val ROOM_CAMERA         = "Camera"
    const val ROOM_CONTACTS       = "Contacts"
    const val ROOM_STORAGE        = "Storage"
    const val ROOM_PHONE          = "Phone"
    const val ROOM_MESSAGES       = "Messages"
    const val ROOM_SENSORS        = "Sensors"
    const val ROOM_OTHER          = "Other"

    // ── Special-access tags ──────────────────────────────────────────────────
    const val TAG_ACCESSIBILITY          = "accessibility"
    const val TAG_DEVICE_ADMIN           = "device_admin"
    const val TAG_NOTIFICATION_LISTENER  = "notification_listener"
    const val TAG_OVERLAY                = "overlay"
    const val TAG_INSTALL_APPS           = "install_apps"
    const val TAG_USAGE_STATS            = "usage_stats"

    // ── Installer identifiers ────────────────────────────────────────────────
    const val INSTALLER_PLAY_STORE = "com.android.vending"

    // ── Permission protection-level strings ──────────────────────────────────
    const val LEVEL_NORMAL     = "normal"
    const val LEVEL_DANGEROUS  = "dangerous"
    const val LEVEL_SIGNATURE  = "signature"
    const val LEVEL_SPECIAL    = "special"

    // ── "Special" permissions whose protection flags include APPOP or whose
    //    strings mark them as special-access gates. ───────────────────────────
    val SPECIAL_PERMISSION_NAMES: Set<String> = setOf(
        "android.permission.SYSTEM_ALERT_WINDOW",
        "android.permission.WRITE_SETTINGS",
        "android.permission.MANAGE_EXTERNAL_STORAGE",
        "android.permission.REQUEST_INSTALL_PACKAGES",
        "android.permission.PACKAGE_USAGE_STATS",
        "android.permission.SCHEDULE_EXACT_ALARM",
        "android.permission.BIND_ACCESSIBILITY_SERVICE",
        "android.permission.BIND_DEVICE_ADMIN",
        "android.permission.BIND_NOTIFICATION_LISTENER_SERVICE",
    )

    // ── Permission → Room mapping ─────────────────────────────────────────────
    val PERMISSION_TO_ROOM: Map<String, String> = mapOf(
        "android.permission.ACCESS_FINE_LOCATION"             to ROOM_LOCATION,
        "android.permission.ACCESS_COARSE_LOCATION"           to ROOM_LOCATION,
        "android.permission.ACCESS_BACKGROUND_LOCATION"       to ROOM_LOCATION,
        "android.permission.RECORD_AUDIO"                     to ROOM_MICROPHONE,
        "android.permission.CAMERA"                           to ROOM_CAMERA,
        "android.permission.READ_CONTACTS"                    to ROOM_CONTACTS,
        "android.permission.WRITE_CONTACTS"                   to ROOM_CONTACTS,
        "android.permission.GET_ACCOUNTS"                     to ROOM_CONTACTS,
        "android.permission.READ_EXTERNAL_STORAGE"            to ROOM_STORAGE,
        "android.permission.WRITE_EXTERNAL_STORAGE"           to ROOM_STORAGE,
        "android.permission.MANAGE_EXTERNAL_STORAGE"          to ROOM_STORAGE,
        "android.permission.READ_CALL_LOG"                    to ROOM_PHONE,
        "android.permission.WRITE_CALL_LOG"                   to ROOM_PHONE,
        "android.permission.CALL_PHONE"                       to ROOM_PHONE,
        "android.permission.PROCESS_OUTGOING_CALLS"           to ROOM_PHONE,
        "android.permission.READ_PHONE_STATE"                 to ROOM_PHONE,
        "android.permission.READ_SMS"                         to ROOM_MESSAGES,
        "android.permission.SEND_SMS"                         to ROOM_MESSAGES,
        "android.permission.RECEIVE_SMS"                      to ROOM_MESSAGES,
        "android.permission.RECEIVE_MMS"                      to ROOM_MESSAGES,
        "android.permission.BODY_SENSORS"                     to ROOM_SENSORS,
        "android.permission.ACTIVITY_RECOGNITION"             to ROOM_SENSORS,
    )
}
