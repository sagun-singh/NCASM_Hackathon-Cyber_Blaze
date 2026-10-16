package com.cyberblaze.appriskauditor.scoring

/**
 * Associates each room with a display persona name and a short tagline.
 *
 * Persona is chosen by the room that scored the most raw points.
 * Tie-break order: Special access > SMS > Contacts > Camera+mic > Location >
 * Phone+identity > Storage > Background > Network.
 * Combo bonuses do NOT count toward room selection.
 */
object PersonaCatalog {

    private data class PersonaDef(val name: String, val tagline: String)

    private val ROOM_TO_PERSONA: Map<String, PersonaDef> = mapOf(
        RoomCatalog.ROOM_SPECIAL     to PersonaDef("The Puppet Master",  "I can press your buttons and watch your screen."),
        RoomCatalog.ROOM_SMS         to PersonaDef("The Letter Opener",  "I read your messages. Shh."),
        RoomCatalog.ROOM_CONTACTS    to PersonaDef("The Gossip",         "I know everybody you know."),
        RoomCatalog.ROOM_CAM_MIC     to PersonaDef("The Eavesdropper",   "Did you just say pizza?"),
        RoomCatalog.ROOM_LOCATION    to PersonaDef("The Shadow",         "I know where you slept last night."),
        RoomCatalog.ROOM_PHONE       to PersonaDef("The Nosy Neighbor",  "I know exactly which phone you are."),
        RoomCatalog.ROOM_STORAGE     to PersonaDef("The Album Browser",  "Nice vacation photos."),
        RoomCatalog.ROOM_BACKGROUND  to PersonaDef("The Clingy Guest",   "You can restart. I will be back."),
        RoomCatalog.ROOM_NETWORK     to PersonaDef("The Polite Guest",   "I only took what I need.")
    )

    private val TIE_BREAK_ORDER: List<String> = listOf(
        RoomCatalog.ROOM_SPECIAL,
        RoomCatalog.ROOM_SMS,
        RoomCatalog.ROOM_CONTACTS,
        RoomCatalog.ROOM_CAM_MIC,
        RoomCatalog.ROOM_LOCATION,
        RoomCatalog.ROOM_PHONE,
        RoomCatalog.ROOM_STORAGE,
        RoomCatalog.ROOM_BACKGROUND,
        RoomCatalog.ROOM_NETWORK
    )

    /** Returns the tagline for a given persona name. Empty string if unknown. */
    fun tagline(persona: String): String =
        ROOM_TO_PERSONA.values.firstOrNull { it.name == persona }?.tagline ?: ""

    /**
     * Selects the persona whose room scored the most raw points.
     * Returns Pair(personaName, tagline).
     */
    fun selectPersona(roomPoints: Map<String, Double>): Pair<String, String> {
        val maxPts = roomPoints.values.maxOrNull() ?: 0.0
        if (maxPts <= 0.0) return politeGuest()
        val topRoom = TIE_BREAK_ORDER
            .firstOrNull { (roomPoints[it] ?: 0.0) >= maxPts }
            ?: roomPoints.maxByOrNull { it.value }?.key
            ?: return politeGuest()
        val def = ROOM_TO_PERSONA[topRoom] ?: return politeGuest()
        return def.name to def.tagline
    }

    private fun politeGuest(): Pair<String, String> {
        val def = ROOM_TO_PERSONA.getValue(RoomCatalog.ROOM_NETWORK)
        return def.name to def.tagline
    }
}
