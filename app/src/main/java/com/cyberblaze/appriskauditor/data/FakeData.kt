package com.cyberblaze.appriskauditor.data

object FakeData {
    val sampleApps = listOf(
        AppInfo(
            label = "Torch Master",
            pkg = "com.fake.torch",
            version = "1.2.3",
            installTime = System.currentTimeMillis() - 86400000L * 30,
            installer = "com.android.vending",
            targetSdk = 33,
            isSystem = false,
            perms = listOf(
                PermInfo("android.permission.READ_SMS", true, "dangerous"),
                PermInfo("android.permission.RECEIVE_BOOT_COMPLETED", true, "normal"),
                PermInfo("android.permission.INTERNET", true, "normal")
            ),
            special = setOf("ACCESSIBILITY")
        ),
        AppInfo(
            label = "QuickChat",
            pkg = "com.fake.quickchat",
            version = "4.0.1",
            installTime = System.currentTimeMillis() - 86400000L * 10,
            installer = "com.android.vending",
            targetSdk = 34,
            isSystem = false,
            perms = listOf(
                PermInfo("android.permission.READ_CONTACTS", true, "dangerous"),
                PermInfo("android.permission.READ_CALL_LOG", true, "dangerous"),
                PermInfo("android.permission.RECORD_AUDIO", true, "dangerous"),
                PermInfo("android.permission.INTERNET", true, "normal")
            )
        ),
        AppInfo(
            label = "Calculator Pro",
            pkg = "com.fake.calc",
            version = "1.0",
            installTime = System.currentTimeMillis() - 86400000L * 2,
            installer = null,
            targetSdk = 30,
            isSystem = false,
            perms = listOf(
                PermInfo("android.permission.READ_SMS", true, "dangerous"),
                PermInfo("android.permission.SEND_SMS", true, "dangerous"),
                PermInfo("android.permission.RECEIVE_BOOT_COMPLETED", true, "normal"),
                PermInfo("android.permission.INTERNET", true, "normal")
            ),
            special = setOf("OVERLAY", "INSTALL_PKGS")
        )
    )

    val sampleResults: Map<AppInfo, RiskResult> = mapOf(
        sampleApps[0] to RiskResult(
            score = 86,
            band = "CRITICAL",
            reasons = listOf(
                "A torch app can read your SMS",
                "It starts itself after reboot",
                "Installed from outside Play Store"
            ),
            rooms = mapOf(
                "SMS" to listOf("READ_SMS"),
                "Background" to listOf("RECEIVE_BOOT_COMPLETED"),
                "Network" to listOf("INTERNET")
            ),
            persona = "The Letter Opener"
        ),
        sampleApps[1] to RiskResult(
            score = 58,
            band = "HIGH",
            reasons = listOf(
                "Reads contacts and call log",
                "Can use the microphone",
                "Has internet to send it away"
            ),
            rooms = mapOf(
                "Contacts" to listOf("READ_CONTACTS", "READ_CALL_LOG"),
                "Camera + mic" to listOf("RECORD_AUDIO"),
                "Network" to listOf("INTERNET")
            ),
            persona = "The Gossip"
        ),
        sampleApps[2] to RiskResult(
            score = 92,
            band = "CRITICAL",
            reasons = listOf(
                "A calculator asking for SMS is like a pizza guy asking for your house keys",
                "Can draw fake screens on top of other apps",
                "Can install other apps silently"
            ),
            rooms = mapOf(
                "SMS" to listOf("READ_SMS", "SEND_SMS"),
                "Background" to listOf("RECEIVE_BOOT_COMPLETED"),
                "Network" to listOf("INTERNET")
            ),
            persona = "The Puppet Master"
        )
    )
}
