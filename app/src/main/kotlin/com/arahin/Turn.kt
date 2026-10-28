package com.arahin

private val MANEUVER = Regex("""u-?turn|putar balik|\b(left|kiri)\b|\b(right|kanan)\b|\b(straight|continue|lurus|terus)\b""")
private val DISTANCE = Regex("""(\d+(?:[.,]\d+)?)\s?(km|m|mi|ft)\b""")

// dir: L, R, U (u-turn) or S (straight); meters is null when no distance was found
data class Turn(val dir: Char, val dist: String?, val meters: Int?) {
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
    val d = DISTANCE.find(t)
    val meters = d?.let {
        val unit = when (it.groupValues[2]) { "km" -> 1000.0; "mi" -> 1609.34; "ft" -> 0.3048; else -> 1.0 }
        (it.groupValues[1].replace(',', '.').toDouble() * unit).toInt()
    }
    return Turn(dir, d?.value, meters)
}
