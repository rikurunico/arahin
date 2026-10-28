package com.arahin

private val MANEUVER = Regex("""u-?turn|putar balik|\b(left|kiri)\b|\b(right|kanan)\b|\b(straight|continue|lurus|terus)\b""")
private val DISTANCE = Regex("""\d+(?:[.,]\d+)?\s?(?:km|m|mi|ft)\b""")

// dir: L, R, U (u-turn) or S (straight)
data class Turn(val dir: Char, val dist: String?) {
    fun text(): String {
        val label = when (dir) {
            'U' -> "↩ PUTAR BALIK"
            'L' -> "◀ KIRI"
            'R' -> "▶ KANAN"
            else -> "▲ LURUS"
        }
        return listOfNotNull(label, dist).joinToString(" ")
    }
}

// Maps text first match wins, so the maneuver word must precede any street name.
fun parseTurn(text: String): Turn? {
    val t = text.lowercase()
    val m = MANEUVER.find(t)?.value ?: return null
    val dir = when {
        m.startsWith("u") || m.startsWith("putar") -> 'U'
        m == "left" || m == "kiri" -> 'L'
        m == "right" || m == "kanan" -> 'R'
        else -> 'S'
    }
    return Turn(dir, DISTANCE.find(t)?.value)
}
