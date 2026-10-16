package com.cyberblaze.appriskauditor.data

/**
 * Core data contract. READ-ONLY for all team members.
 * Person 1 added: `room` field to [PermInfo] for 9-room categorisation.
 */

data class PermInfo(
    val name: String,
    val granted: Boolean,
    val level: String,
    val room: String? = null,   // one of Constants.ROOM_* or null for uncategorised
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
    var special: Set<String> = emptySet(),  // mutable: Person 3 fills this before scoring
)

data class RiskResult(
    val score: Int,
    val band: String,
    val reasons: List<String>,
    val rooms: Map<String, List<String>>,
    val persona: String,
)
