package com.arahin

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TurnTest {
    @Test fun leftEnglish() = assertEquals("◀ KIRI 200 m", parseTurn("Turn left onto Main St 200 m")?.text())
    @Test fun rightIndonesian() = assertEquals("▶ KANAN 1,5 km", parseTurn("Belok kanan ke Jl. Sudirman 1,5 km")?.text())
    @Test fun uTurn() = assertEquals("↩ PUTAR BALIK 50 m", parseTurn("Make a U-turn 50 m")?.text())
    @Test fun straight() = assertEquals("▲ LURUS", parseTurn("Continue straight")?.text())
    @Test fun direction() = assertEquals(Turn('R', "116 m", 116), parseTurn("Turn right 116 m"))
    @Test fun kilometers() = assertEquals(1500, parseTurn("Belok kanan 1,5 km")?.meters)
    @Test fun miles() = assertEquals(804, parseTurn("Turn left 0.5 mi")?.meters)
    @Test fun noDistance() = assertNull(parseTurn("Continue straight")?.meters)
    @Test fun unrelated() = assertNull(parseTurn("Rute tercepat tersedia"))
}
