package com.arahin

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.pm.ApplicationInfo
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

private const val MAPS = "com.google.android.apps.maps"
private const val CHANNEL = "turn"
private const val ID = 1

class MapsRelay : NotificationListenerService() {
    private val nm by lazy { getSystemService(NotificationManager::class.java) }
    private var lastDir: Char? = null

    override fun onCreate() {
        super.onCreate()
        WatchLink.refresh(this)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL, "Belokan", NotificationManager.IMPORTANCE_HIGH).apply {
                enableVibration(true)
            }
        )
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName != MAPS || !sbn.isOngoing) return
        val e = sbn.notification.extras
        val text = listOf(Notification.EXTRA_TITLE, Notification.EXTRA_TEXT, Notification.EXTRA_SUB_TEXT)
            .joinToString(" ") { e.getCharSequence(it)?.toString().orEmpty() }
        val turn = parseTurn(text) ?: fallbackTurn(e.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()) ?: return
        // calibration: only debug builds print the raw Maps text next to what the parser made of it
        if (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0) Log.d("Arahin", "raw=[$text] -> $turn")
        WatchLink.send(this, turn, e.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString())

        // shortcut: two same-direction turns in a row buzz once, upgrade by tracking distance resets
        val quiet = turn.dir == lastDir
        lastDir = turn.dir

        nm.notify(
            ID,
            Notification.Builder(this, CHANNEL)
                .setSmallIcon(android.R.drawable.ic_menu_directions)
                .setContentTitle(turn.text())
                .setCategory(Notification.CATEGORY_NAVIGATION)
                .setOnlyAlertOnce(quiet)
                .setTimeoutAfter(60_000)
                .build()
        )
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        if (sbn.packageName != MAPS || !sbn.isOngoing) return
        lastDir = null
        nm.cancel(ID)
    }
}
