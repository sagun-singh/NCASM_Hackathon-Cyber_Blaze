package com.enterprise.riskauditor.scoring

import com.enterprise.riskauditor.contract.Constants.Rooms
import com.enterprise.riskauditor.contract.Constants.Tags

/**
 * RoomCatalog maps every scoreable permission string (and special-access tag) to
 * the room it belongs to.
 *
 * ## Design decisions
 * - Special-access tags live in their own set; the room entry for SPECIAL_ACCESS
 *   holds [ALL_SPECIAL_TAGS] so the scorer can iterate them uniformly.
 * - Permissions that appear in multiple rooms would create double-counting; none
 *   do here, so there is a 1-to-1 mapping for regular permissions.
 * - The double-scoring exclusion (spec: "do NOT also score SYSTEM_ALERT_WINDOW …
 *   as separate permissions") is enforced by simply NOT including those strings
 *   in any non-special room.
 */
object RoomCatalog {

    // ── Special-access tags ─────────────────────────────────────────────────
    /** All tags that can appear in AppInfo.special. */
    val ALL_SPECIAL_TAGS: Set<String> = setOf(
        Tags.ACCESSIBILITY,
        Tags.DEVICE_ADMIN,
        Tags.NOTIFICATION_LISTENER,
        Tags.OVERLAY,
        Tags.INSTALL_APPS,
        Tags.USAGE_STATS
    )

    // ── Room → trigger permission lists ─────────────────────────────────────
    // NOTE: permissions excluded to avoid double-counting with special-access:
    //   SYSTEM_ALERT_WINDOW, REQUEST_INSTALL_PACKAGES, PACKAGE_USAGE_STATS,
    //   BIND_ACCESSIBILITY_SERVICE, BIND_DEVICE_ADMIN, BIND_NOTIFICATION_LISTENER_SERVICE

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

    /**
     * Location is split into two tiers for scoring:
     * - [LOCATION_BASE_PERMS]: fine or coarse → 8 pts base
     * - [LOCATION_BG_PERMS]:   background      → +4 pts bonus (on top of base)
     */
    val LOCATION_BASE_PERMS: Set<String> = setOf(
        "android.permission.ACCESS_FINE_LOCATION",
        "android.permission.ACCESS_COARSE_LOCATION"
    )
    val LOCATION_BG_PERMS: Set<String> = setOf(
        "android.permission.ACCESS_BACKGROUND_LOCATION"
    )
    /** Union used for room-membership listing. */
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

    // ── Reverse lookup: permission string → room name ────────────────────────
    /**
     * Flat map of every individual permission (and special tag) to its room.
     * Used by the scorer to build [RiskResult.rooms] without scanning every set.
     */
    val PERM_TO_ROOM: Map<String, String> by lazy {
        buildMap {
            ALL_SPECIAL_TAGS.forEach   { put(it, Rooms.SPECIAL_ACCESS) }
            SMS_PERMS.forEach          { put(it, Rooms.SMS) }
            CONTACTS_PERMS.forEach     { put(it, Rooms.CONTACTS_CALL_LOGS) }
            CAMERA_MIC_PERMS.forEach   { put(it, Rooms.CAMERA_MIC) }
            LOCATION_ALL_PERMS.forEach { put(it, Rooms.LOCATION) }
            PHONE_IDENTITY_PERMS.forEach { put(it, Rooms.PHONE_IDENTITY) }
            STORAGE_PERMS.forEach      { put(it, Rooms.STORAGE_MEDIA) }
            BACKGROUND_PERMS.forEach   { put(it, Rooms.BACKGROUND) }
            NETWORK_PERMS.forEach      { put(it, Rooms.NETWORK) }
        }
    }
}
