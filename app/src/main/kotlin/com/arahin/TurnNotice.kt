package com.arahin

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context

private const val CHANNEL = "turn"
private const val ID = 1

// The short notification the watch companion mirrors to the wrist: "BELOK KIRI" with "200 m" under it.
// Both lines stay short on purpose, a long line (like an arrival time) turns into scrolling text on the watch.
object TurnNotice {
    private fun nm(ctx: Context) = ctx.getSystemService(NotificationManager::class.java).apply {
        createNotificationChannel(
            NotificationChannel(CHANNEL, "Belokan", NotificationManager.IMPORTANCE_HIGH).apply { enableVibration(true) }
        )
    }

    // the distance fills the line under the title; left empty, Huawei Health shows "You have one new message" there
    fun post(ctx: Context, turn: Turn) = nm(ctx).notify(
        ID,
        Notification.Builder(ctx, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_arahin)
            .setContentTitle(turn.title())
            .setContentText(turn.dist ?: "Arahin")
            // like Maps' own: a private notification may reach the wrist only as "You have one new message"
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setCategory(Notification.CATEGORY_NAVIGATION)
            .setTimeoutAfter(60_000)
            .build()
    )

    fun cancel(ctx: Context) = nm(ctx).cancel(ID)
}
