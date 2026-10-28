package com.arahin

import org.junit.Assert.assertEquals
import org.junit.Test

class AlertGateTest {
    private fun turn(dir: Char, m: Int?) = Turn(dir, m?.let { "$it m" }, m, null, "")

    // which distances of a drive would buzz the watch
    private fun alerts(vararg steps: Pair<Char, Int?>): List<String> {
        val gate = AlertGate()
        return steps.filter { (d, m) -> gate.shouldAlert(turn(d, m)) }.map { (d, m) -> "$d$m" }
    }

    // Maps updates every ~10 m; the wrist should get three buzzes, not thirty
    @Test fun threeBuzzesPerTurn() = assertEquals(
        listOf("L500", "L200", "L50"),
        alerts(*(800 downTo 0 step 10).map { 'L' to it }.toTypedArray()),
    )

    @Test fun turnFirstSeenClose() = assertEquals(listOf("R240", "R190", "R50"), alerts('R' to 240, 'R' to 190, 'R' to 150, 'R' to 60, 'R' to 50))

    @Test fun straightIsSilent() = assertEquals(emptyList<String>(), alerts('S' to 600, 'S' to 400, 'S' to 100, 'S' to 0))

    // two left turns in a row: the distance jumping back up is the second turn
    @Test fun sameDirectionTwice() = assertEquals(listOf("L250", "L200", "L50", "L300", "L200"), alerts('L' to 250, 'L' to 200, 'L' to 50, 'L' to 300, 'L' to 200))

    // Maps blanks the title at the turn and while rerouting
    @Test fun blankDistanceIgnored() = assertEquals(listOf("R100", "R50"), alerts('R' to 100, 'R' to null, 'R' to 50))

    @Test fun gpsJitterIsNotANewTurn() = assertEquals(listOf("L180"), alerts('L' to 180, 'L' to 190, 'L' to 170, 'L' to 160))

    // seen on a real phone: the distance wobbling around a threshold buzzed twice
    @Test fun wobbleAroundThreshold() = assertEquals(listOf("L90", "L50"), alerts('L' to 90, 'L' to 70, 'L' to 80, 'L' to 60, 'L' to 50, 'L' to 60, 'L' to 50, 'L' to 60))
}
