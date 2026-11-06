package com.arahin

import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TripTest {
    @Test fun remainingIsMaxMinusProgress() = assertEquals(4457, remainingMeters(118, 4575))
    @Test fun noRouteLengthMeansUnknown() = assertNull(remainingMeters(10, 0))
    @Test fun progressPastTheEndIsIgnored() = assertNull(remainingMeters(5000, 4575))

    @Test fun minutesToArrival() = assertEquals(18, minutesUntil("Arrive 1:23 PM", LocalTime.of(13, 5)))
    @Test fun arrivalPastMidnight() = assertEquals(20, minutesUntil("Tiba 00.10", LocalTime.of(23, 50)))
    @Test fun noTimeMeansUnknown() = assertNull(minutesUntil("Rute baru", LocalTime.of(13, 5)))

    @Test fun shortDistanceInMetres() = assertEquals("850 m", distanceLabel(850))
    @Test fun longDistanceInKilometresWithComma() = assertEquals("4,5 km", distanceLabel(4457))
    @Test fun unknownDistanceIsEmpty() = assertEquals("", distanceLabel(null))

    @Test fun tripFromANotification() =
        assertEquals(Trip("Tiba 13.23", 18, 4457), tripOf("Arrive 1:23 PM", 118, 4575, LocalTime.of(13, 5)))
}
