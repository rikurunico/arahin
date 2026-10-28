package com.arahin

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context

private const val CHANNEL = "turn"
private const val ID = 1

// The short notification the watch companion mirrors to the wrist, e.g. "◀ BELOK KIRI 200 m".
object TurnNotice {
    private fun nm(ctx: Context) = ctx.getSystemService(NotificationManager::class.java).apply {
        createNotificationChannel(
            NotificationChannel(CHANNEL, "Belokan", NotificationManager.IMPORTANCE_HIGH).apply { enableVibration(true) }
        )
    }

    // detail fills the line under the title; left empty, Huawei Health shows "You have one new message" there
    fun post(ctx: Context, turn: Turn, detail: String?) = nm(ctx).notify(
        ID,
        Notification.Builder(ctx, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_menu_directions)
            .setContentTitle(turn.text())
            .setContentText(detail?.takeIf { it.isNotBlank() } ?: turn.label)
            .setCategory(Notification.CATEGORY_NAVIGATION)
            .setTimeoutAfter(60_000)
            .build()
    )

    fun cancel(ctx: Context) = nm(ctx).cancel(ID)
}
