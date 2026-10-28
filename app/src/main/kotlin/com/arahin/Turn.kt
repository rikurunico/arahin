package com.arahin

private val MANEUVER = Regex("""u-?turn|putar balik|\b(left|kiri)\b|\b(right|kanan)\b|\b(straight|continue|lurus|terus)\b""")
private val DISTANCE = Regex("""\d+(?:[.,]\d+)?\s?(?:km|m|mi|ft)\b""")

// Maps text first match wins, so the maneuver word must precede any street name.
fun parseTurn(text: String): String? {
    val t = text.lowercase()
    val m = MANEUVER.find(t)?.value ?: return null
    val label = when {
        m.startsWith("u") || m.startsWith("putar") -> "↩ PUTAR BALIK"
        m == "left" || m == "kiri" -> "◀ KIRI"
        m == "right" || m == "kanan" -> "▶ KANAN"
        else -> "▲ LURUS"
    }
    return listOfNotNull(label, DISTANCE.find(t)?.value).joinToString(" ")
}
