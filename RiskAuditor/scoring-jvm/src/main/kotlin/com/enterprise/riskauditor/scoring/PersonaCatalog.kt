package com.enterprise.riskauditor.scoring

import com.enterprise.riskauditor.contract.Constants.Rooms

/**
 * Associates each room with a display persona name and a short, punchy tagline.
 *
 * Persona selection rule (from spec):
 *   Pick the room that contributed the MOST raw points.
 *   Tie-break order: Special access > SMS > Contacts > Camera+mic > Location >
 *   Phone+identity > Storage > Background > Network.
 *   Combo bonuses do NOT count toward room choice.
 */
object PersonaCatalog {

    // ── Persona definitions ──────────────────────────────────────────────────

    private data class PersonaDef(val name: String, val tagline: String)

    private val ROOM_TO_PERSONA: Map<String, PersonaDef> = mapOf(
        Rooms.SPECIAL_ACCESS     to PersonaDef("The Puppet Master",   "I can press your buttons and watch your screen."),
        Rooms.SMS                to PersonaDef("The Letter Opener",   "I read your messages. Shh."),
        Rooms.CONTACTS_CALL_LOGS to PersonaDef("The Gossip",          "I know everybody you know."),
        Rooms.CAMERA_MIC         to PersonaDef("The Eavesdropper",    "Did you just say pizza?"),
        Rooms.LOCATION           to PersonaDef("The Shadow",          "I know where you slept last night."),
        Rooms.PHONE_IDENTITY     to PersonaDef("The Nosy Neighbor",   "I know exactly which phone you are."),
        Rooms.STORAGE_MEDIA      to PersonaDef("The Album Browser",   "Nice vacation photos."),
        Rooms.BACKGROUND         to PersonaDef("The Clingy Guest",    "You can restart. I will be back."),
        Rooms.NETWORK            to PersonaDef("The Polite Guest",    "I only took what I need.")
    )

    // Tie-break priority (lower index = higher priority)
    private val TIE_BREAK_ORDER: List<String> = listOf(
        Rooms.SPECIAL_ACCESS,
        Rooms.SMS,
        Rooms.CONTACTS_CALL_LOGS,
        Rooms.CAMERA_MIC,
        Rooms.LOCATION,
        Rooms.PHONE_IDENTITY,
        Rooms.STORAGE_MEDIA,
        Rooms.BACKGROUND,
        Rooms.NETWORK
    )

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Returns the persona name string for [persona].
     * Safe: returns an empty string for unknown inputs (shouldn't happen in practice).
     */
    fun tagline(persona: String): String =
        ROOM_TO_PERSONA.values.firstOrNull { it.name == persona }?.tagline ?: ""

    /**
     * Selects the persona whose room scored the most raw points.
     *
     * @param roomPoints Map of room-name -> raw points contributed by that room
     *                   (combo bonuses excluded, as per spec)
     * @return Pair of (persona name, tagline)
     */
    fun selectPersona(roomPoints: Map<String, Double>): Pair<String, String> {
        if (roomPoints.isEmpty()) return politeGuest()

        val maxPoints = roomPoints.values.maxOrNull() ?: 0.0
        if (maxPoints <= 0.0) return politeGuest()

        // Among rooms with the max score, use tie-break order
        val topRoom = TIE_BREAK_ORDER
            .filter { (roomPoints[it] ?: 0.0) >= maxPoints }
            .firstOrNull()
            ?: roomPoints.maxByOrNull { it.value }?.key
            ?: return politeGuest()

        val def = ROOM_TO_PERSONA[topRoom] ?: return politeGuest()
        return def.name to def.tagline
    }

    private fun politeGuest(): Pair<String, String> {
        val def = ROOM_TO_PERSONA.getValue(Rooms.NETWORK)
        return def.name to def.tagline
    }
}
