package com.arahin

import android.app.Notification
import android.content.pm.ApplicationInfo
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import java.io.File

private const val MAPS = "com.google.android.apps.maps"

class MapsRelay : NotificationListenerService() {
    private val gate = AlertGate()

    override fun onCreate() {
        super.onCreate()
        WatchLink.refresh(this)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName != MAPS || !sbn.isOngoing) return
        val e = sbn.notification.extras
        val title = e.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val text = e.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()
        val eta = e.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()
        val bmp = (sbn.notification.getLargeIcon()?.loadDrawable(this) as? BitmapDrawable)?.bitmap
        val icon = bmp?.let { matchIcon(signatureOf(it)) }
        val turn = notificationTurn(title, text, icon) ?: return
        val alert = gate.shouldAlert(turn)
        // calibration: only debug builds print the raw Maps text next to the JSON the watch would receive
        if (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0) {
            Log.d("Arahin", "raw=[$title | $text | $eta] icon=${bmp?.let(::dumpIcon)}:$icon alert=$alert -> ${WatchLink.payload(turn, eta)}")
        }
        WatchLink.send(this, turn, eta)

        if (!alert) return

        TurnNotice.post(this, turn)
    }

    private fun signatureOf(bmp: Bitmap): IntArray {
        val px = IntArray(bmp.width * bmp.height).also { bmp.getPixels(it, 0, bmp.width, 0, 0, bmp.width, bmp.height) }
        return iconSignature(IntArray(px.size) { px[it] ushr 24 }, bmp.width, bmp.height)
    }

    // calibration: save each distinct maneuver icon so it can be pulled with adb and labelled
    private fun dumpIcon(bmp: Bitmap): String {
        val px = IntArray(bmp.width * bmp.height).also { bmp.getPixels(it, 0, bmp.width, 0, 0, bmp.width, bmp.height) }
        val name = "%08x".format(px.contentHashCode())
        val f = File(getExternalFilesDir("icons"), "$name.png")
        if (!f.exists()) f.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        return name
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        if (sbn.packageName != MAPS || !sbn.isOngoing) return
        gate.reset()
        TurnNotice.cancel(this)
    }
}
