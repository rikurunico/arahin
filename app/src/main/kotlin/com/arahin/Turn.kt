package com.arahin

private val MANEUVER = Regex("""roundabout|rotary|traffic circle|bundaran|u-?turn|putar balik|\b(left|kiri)\b|\b(right|kanan)\b|\b(straight|continue|lurus|terus)\b""")
private val DISTANCE = Regex("""(\d+(?:[.,]\d+)?)\s?(km|m|mi|ft)\b""")
private val EXIT = Regex("""\b(\d+)(?:st|nd|rd|th)\b|\bke[- ]?(\d+)\b|\b(first|second|third|fourth|fifth|pertama|kedua|ketiga|keempat|kelima)\b""")
private val ORDINAL = mapOf(
    "first" to 1, "pertama" to 1, "second" to 2, "kedua" to 2, "third" to 3, "ketiga" to 3,
    "fourth" to 4, "keempat" to 4, "fifth" to 5, "kelima" to 5,
)

// dir: L, R, U (u-turn), S (straight) or O (roundabout, see exit); meters is null when no distance was found
data class Turn(val dir: Char, val dist: String?, val meters: Int?, val exit: Int? = null) {
    fun text(): String {
        val label = when (dir) {
            'U' -> "↩ PUTAR BALIK"
            'L' -> "◀ KIRI"
            'R' -> "▶ KANAN"
            'O' -> "⟳ BUNDARAN" + (exit?.let { " KELUAR $it" } ?: "")
            else -> "▲ LURUS"
        }
        return listOfNotNull(label, dist).joinToString(" ")
    }
}

// Maps text first match wins, so the maneuver word must precede any street name ("ke Jl. Bundaran HI" stays a plain turn).
fun parseTurn(text: String): Turn? {
    val t = text.lowercase()
    val m = MANEUVER.find(t)?.value ?: return null
    val dir = when {
        m in setOf("roundabout", "rotary", "traffic circle", "bundaran") -> 'O'
        m.startsWith("u") || m.startsWith("putar") -> 'U'
        m == "left" || m == "kiri" -> 'L'
        m == "right" || m == "kanan" -> 'R'
        else -> 'S'
    }
    val exit = if (dir == 'O') EXIT.find(t)?.let { e ->
        e.groupValues[1].ifEmpty { e.groupValues[2] }.toIntOrNull() ?: ORDINAL[e.groupValues[3]]
    } else null
    val d = DISTANCE.find(t)
    val meters = d?.let {
        val unit = when (it.groupValues[2]) { "km" -> 1000.0; "mi" -> 1609.34; "ft" -> 0.3048; else -> 1.0 }
        (it.groupValues[1].replace(',', '.').toDouble() * unit).toInt()
    }
    return Turn(dir, d?.value, meters, exit)
}
