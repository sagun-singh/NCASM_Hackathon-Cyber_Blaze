package com.enterprise.riskauditor.contract

/**
 * Shared string constants — import from here; never re-type the strings.
 *
 * All team members MUST import these constants instead of using raw string literals.
 */
object Constants {

    // ── Bands ───────────────────────────────────────────────────────────────
    object Bands {
        const val LOW      = "LOW"
        const val MEDIUM   = "MEDIUM"
        const val HIGH     = "HIGH"
        const val CRITICAL = "CRITICAL"
    }

    // ── Room names ──────────────────────────────────────────────────────────
    object Rooms {
        const val SPECIAL_ACCESS       = "Special access"
        const val SMS                  = "SMS"
        const val CONTACTS_CALL_LOGS   = "Contacts + call logs"
        const val CAMERA_MIC           = "Camera + mic"
        const val LOCATION             = "Location"
        const val PHONE_IDENTITY       = "Phone + identity"
        const val STORAGE_MEDIA        = "Storage + media"
        const val BACKGROUND           = "Background + persistence"
        const val NETWORK              = "Network"
    }

    // ── Special-access tags (used in AppInfo.special) ───────────────────────
    object Tags {
        const val ACCESSIBILITY         = "ACCESSIBILITY"
        const val DEVICE_ADMIN          = "DEVICE_ADMIN"
        const val NOTIFICATION_LISTENER = "NOTIFICATION_LISTENER"
        const val OVERLAY               = "OVERLAY"
        const val INSTALL_APPS          = "INSTALL_APPS"
        const val USAGE_STATS           = "USAGE_STATS"
    }

    // ── Trusted installer package names ─────────────────────────────────────
    /** Apps installed by one of these packages are NOT considered sideloaded. */
    val TRUSTED_INSTALLERS: Set<String> = setOf(
        "com.android.vending",      // Google Play Store
        "com.amazon.venezia",       // Amazon Appstore
        "com.sec.android.app.samsungapps" // Samsung Galaxy Store
    )
}
