package com.cyberblaze.appriskauditor.data

data class PermInfo(
    val name: String,
    val granted: Boolean,
    val level: String
)

data class AppInfo(
    val label: String,
    val pkg: String,
    val version: String,
    val installTime: Long,
    val installer: String?,
    val targetSdk: Int,
    val isSystem: Boolean,
    val perms: List<PermInfo>,
    val special: Set<String> = emptySet()
)

data class RiskResult(
    val score: Int,
    val band: String,
    val reasons: List<String>,
    val rooms: Map<String, List<String>>,
    val persona: String
)
