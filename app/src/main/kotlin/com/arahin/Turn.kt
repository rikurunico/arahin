package com.arahin

// Maneuver kinds follow Google's navigation taxonomy, collapsed to what a glance needs:
// S straight/head/continue, L R turn, l r slight/keep/fork/ramp, h j sharp, U u-turn, O roundabout,
// D destination, M merge, E exit/ramp without a side, ? unrecognised text relayed as-is.
// Indonesian phrases below come from real Google Maps (hl=id) step lists.
private class Rule(val dir: Char, val re: Regex)

private val RULES = listOf(
    Rule('O', Regex("""roundabout|rotary|traffic circle|bundaran""")),
    Rule('U', Regex("""u-?turn|putar balik|putar arah""")),
    Rule('h', Regex("""sharp left|tajam ke kiri|kiri tajam""")),
    Rule('j', Regex("""sharp right|tajam ke kanan|kanan tajam""")),
    Rule('l', Regex("""slight left|bear left|keep left|sedikit ke kiri|kiri sedikit|tetap di kiri|serong kiri|terus kiri|lajur kiri|jalur sebelah kiri|jalur kiri|jalan di kiri|(?:ramp|take exit|jalan keluar)[^.]*?\b(?:left|kiri)\b""")),
    Rule('r', Regex("""slight right|bear right|keep right|sedikit ke kanan|kanan sedikit|tetap di kanan|serong kanan|terus kanan|lajur kanan|jalur sebelah kanan|jalur kanan|jalan di kanan|(?:ramp|take exit|jalan keluar)[^.]*?\b(?:right|kanan)\b""")),
    Rule('L', Regex("""\b(?:left|kiri)\b""")),
    Rule('R', Regex("""\b(?:right|kanan)\b""")),
    Rule('D', Regex("""destination|tujuan|arrive|tiba di""")),
    Rule('M', Regex("""merge|bergabung""")),
    Rule('E', Regex("""ambil jalan keluar|take exit|take the exit""")),
    Rule('S', Regex("""\b(?:straight|continue|head|lurus|terus|teruskan|lanjutkan|melanjutkan|mengemudi|ikuti|menuju)\b|tetap di|lajur tengah|ambil arah""")),
)
private val NOTE = Regex("""\([^)]*\)""")
private val DISTANCE = Regex("""(\d+(?:[.,]\d+)?)\s?(km|m|mi|ft)\b""")
private val EXIT = Regex("""\b(\d+)(?:st|nd|rd|th)\b|\bke[- ]?(\d+)\b|\b(first|second|third|fourth|fifth|pertama|kedua|ketiga|keempat|kelima)\b""")
private val ORDINAL = mapOf(
    "first" to 1, "pertama" to 1, "second" to 2, "kedua" to 2, "third" to 3, "ketiga" to 3,
    "fourth" to 4, "keempat" to 4, "fifth" to 5, "kelima" to 5,
)

// label is the short Indonesian instruction shown on the watch; meters is null when no distance was found
data class Turn(val dir: Char, val dist: String?, val meters: Int?, val exit: Int?, val label: String) {
    fun text(): String {
        val glyph = when (dir) {
            'L', 'l', 'h' -> "◀"
            'R', 'r', 'j' -> "▶"
            'U' -> "↩"
            'O' -> "⟳"
            'D' -> "⚑"
            '?' -> "•"
            else -> "▲"
        }
        return listOfNotNull(glyph, label.uppercase(), dist).joinToString(" ")
    }
}

private fun distance(t: String): Pair<String?, Int?> {
    val d = DISTANCE.find(t) ?: return null to null
    val unit = when (d.groupValues[2]) { "km" -> 1000.0; "mi" -> 1609.34; "ft" -> 0.3048; else -> 1.0 }
    return d.value to (d.groupValues[1].replace(',', '.').toDouble() * unit).toInt()
}

private fun labelOf(dir: Char, t: String, exit: Int?) = when (dir) {
    'L' -> "Belok kiri"
    'R' -> "Belok kanan"
    'l' -> "Serong kiri"
    'r' -> "Serong kanan"
    'h' -> "Belok tajam kiri"
    'j' -> "Belok tajam kanan"
    'U' -> "Putar balik"
    'O' -> exit?.let { "Keluar ke-$it" } ?: "Bundaran"
    'D' -> "Tujuan" + when {
        Regex("""\b(?:left|kiri)\b""").containsMatchIn(t) -> " di kiri"
        Regex("""\b(?:right|kanan)\b""").containsMatchIn(t) -> " di kanan"
        else -> ""
    }
    'M' -> "Gabung"
    'E' -> "Ambil jalan keluar"
    else -> "Lurus"
}

// The rule whose match starts first wins, so a street name later in the text ("ke Jl. Bundaran HI") cannot hijack the maneuver.
fun parseTurn(text: String): Turn? {
    val t = text.lowercase().replace(NOTE, " ") // "(di kiri 350 m lagi)" is a landmark note, not the maneuver
    val dir = RULES.mapNotNull { r -> r.re.find(t)?.let { r.dir to it.range.first } }
        .minByOrNull { it.second }?.first ?: return null
    val exit = if (dir == 'O') EXIT.find(t)?.let { e ->
        e.groupValues[1].ifEmpty { e.groupValues[2] }.toIntOrNull() ?: ORDINAL[e.groupValues[3]]
    } else null
    val (dist, meters) = distance(t)
    return Turn(dir, dist, meters, exit, labelOf(dir, t, exit))
}

// Text no rule recognised is still better on the wrist than a blank screen.
fun fallbackTurn(title: String): Turn? {
    val s = title.trim().takeIf { it.isNotEmpty() } ?: return null
    val (dist, meters) = distance(s.lowercase())
    return Turn('?', dist, meters, null, s.take(28))
}
