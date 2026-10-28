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
    @Test fun roundaboutEnglish() = assertEquals(Turn('O', null, null, 2), parseTurn("At the roundabout, take the 2nd exit"))
    @Test fun roundaboutIndonesian() = assertEquals(Turn('O', "200 m", 200, 3), parseTurn("Di bundaran, ambil jalan keluar ke-3 200 m"))
    @Test fun roundaboutOrdinalWord() = assertEquals(1, parseTurn("Di bundaran, ambil pintu keluar pertama")?.exit)
    @Test fun roundaboutText() = assertEquals("⟳ BUNDARAN KELUAR 2 150 m", parseTurn("At the roundabout, take the 2nd exit 150 m")?.text())
    @Test fun roundaboutInStreetName() = assertEquals('R', parseTurn("Belok kanan ke Jl. Bundaran HI")?.dir)
    @Test fun unrelated() = assertNull(parseTurn("Rute tercepat tersedia"))
}
