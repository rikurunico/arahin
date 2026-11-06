package com.arahin

import java.time.LocalTime
import java.util.Locale

// What a Maps notification says about the rest of the trip: arrival label, minutes left, metres left.
data class Trip(val arrival: String, val minutes: Int?, val meters: Int?) {
    companion object {
        val NONE = Trip("", null, null)
    }
}

// Maps' ProgressStyle notification carries the whole route in metres: progress so far out of progressMax.
fun remainingMeters(progress: Int, max: Int): Int? = if (max > 0 && progress in 0..max) max - progress else null

// Minutes from `now` to the arrival time in the notification; an arrival that looks earlier than now is tomorrow's.
fun minutesUntil(arrival: String?, now: LocalTime): Int? {
    val (hour, minute) = clockOf(arrival) ?: return null
    return ((hour * 60 + minute) - (now.hour * 60 + now.minute) + 24 * 60) % (24 * 60)
}

fun distanceLabel(meters: Int?): String = when {
    meters == null -> ""
    meters < 1000 -> "$meters m"
    else -> "%.1f km".format(Locale("id"), meters / 1000.0)
}

fun tripOf(eta: String?, progress: Int, max: Int, now: LocalTime = LocalTime.now()) =
    Trip(arrivalLabel(eta), minutesUntil(eta, now), remainingMeters(progress, max))
