package com.arahin

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TurnTest {
    private fun dir(s: String) = parseTurn(s)?.dir

    @Test fun leftEnglish() = assertEquals("◀ BELOK KIRI 200 m", parseTurn("Turn left onto Main St 200 m")?.text())
    @Test fun rightIndonesian() = assertEquals("▶ BELOK KANAN 1,5 km", parseTurn("Belok kanan ke Jl. Sudirman 1,5 km")?.text())
    @Test fun uTurn() = assertEquals("↩ PUTAR BALIK 50 m", parseTurn("Make a U-turn 50 m")?.text())
    @Test fun straight() = assertEquals("▲ LURUS", parseTurn("Continue straight")?.text())
    @Test fun structured() = assertEquals(Turn('R', "116 m", 116, null, "Belok kanan"), parseTurn("Turn right 116 m"))
    @Test fun kilometers() = assertEquals(1500, parseTurn("Belok kanan 1,5 km")?.meters)
    @Test fun miles() = assertEquals(804, parseTurn("Turn left 0.5 mi")?.meters)
    @Test fun noDistance() = assertNull(parseTurn("Continue straight")?.meters)

    @Test fun slightRight() = assertEquals('r', dir("Slight right onto Jl. Raya"))
    @Test fun keepLeft() = assertEquals('l', dir("Keep left at the fork"))
    @Test fun sharpLeft() = assertEquals('h', dir("Make a sharp left"))
    @Test fun sharpRight() = assertEquals('j', dir("Sharp right onto Main St"))
    @Test fun rampSide() = assertEquals('r', dir("Take the ramp on the right"))
    @Test fun merge() = assertEquals('M', dir("Merge onto Tol Jagorawi"))
    @Test fun depart() = assertEquals('S', dir("Head north on Jl. Merdeka"))
    @Test fun destination() = assertEquals("Tujuan di kiri", parseTurn("Your destination is on the left")?.label)

    @Test fun roundaboutEnglish() = assertEquals(Turn('O', null, null, 2, "Keluar ke-2"), parseTurn("At the roundabout, take the 2nd exit"))
    @Test fun roundaboutIndonesian() = assertEquals(Turn('O', "200 m", 200, 3, "Keluar ke-3"), parseTurn("Di bundaran, ambil jalan keluar ke-3 200 m"))
    @Test fun roundaboutOrdinalWord() = assertEquals(1, parseTurn("Di bundaran, ambil pintu keluar pertama")?.exit)
    @Test fun roundaboutText() = assertEquals("⟳ KELUAR KE-2 150 m", parseTurn("At the roundabout, take the 2nd exit 150 m")?.text())
    @Test fun roundaboutInStreetName() = assertEquals('R', dir("Belok kanan ke Jl. Bundaran HI"))

    @Test fun unrelated() = assertNull(parseTurn("Rute tercepat tersedia"))
    @Test fun fallbackKeepsText() = assertEquals("Rute tercepat tersedia", fallbackTurn("  Rute tercepat tersedia ")?.label)
    @Test fun fallbackBlank() = assertNull(fallbackTurn("   "))
}
