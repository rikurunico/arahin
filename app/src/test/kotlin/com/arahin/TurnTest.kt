package com.arahin

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TurnTest {
    @Test fun leftEnglish() = assertEquals("◀ KIRI 200 m", parseTurn("Turn left onto Main St 200 m"))
    @Test fun rightIndonesian() = assertEquals("▶ KANAN 1,5 km", parseTurn("Belok kanan ke Jl. Sudirman 1,5 km"))
    @Test fun uTurn() = assertEquals("↩ PUTAR BALIK 50 m", parseTurn("Make a U-turn 50 m"))
    @Test fun straight() = assertEquals("▲ LURUS", parseTurn("Continue straight"))
    @Test fun unrelated() = assertNull(parseTurn("Rute tercepat tersedia"))
}
