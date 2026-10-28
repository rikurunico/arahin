package com.arahin

// Watch companions (Huawei Health, Garmin, Samsung) mirror every update of a notification and buzz each time,
// and Maps updates its own every ~10 m. So the turn notification is only (re)posted at the moments a rider
// needs a buzz, the same moments the watch app buzzes: a new turn within 500 m, then 200 m and 50 m before it.
private val ALERTS = listOf(500, 200, 50)
// straight on, merge, exits and leaving a roundabout need no buzz, like on the watch app
private const val SILENT = "SMEQ"
// distance to the turn going up by more than this means Maps moved on to the next turn
private const val NEXT_TURN_JUMP = 50

class AlertGate {
    private var dir: Char? = null
    private var meters: Int? = null

    fun shouldAlert(turn: Turn): Boolean {
        val m = turn.meters ?: return false // Maps blanks the distance at the turn itself and while rerouting
        val last = meters
        val next = turn.dir != dir || (last != null && m > last + NEXT_TURN_JUMP)
        val crossed = last != null && ALERTS.any { last > it && m <= it }
        dir = turn.dir
        meters = m
        return turn.dir !in SILENT && m <= ALERTS[0] && (next || crossed)
    }

    fun reset() {
        dir = null
        meters = null
    }
}
