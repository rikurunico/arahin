package com.arahin

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

// adb shell am broadcast -n com.arahin/.DemoReceiver --es d L --ei m 200 [--ei x 2] [--es e "12 min · 1.4 km · 18.07 ETA"]
// posts the same notification a real Maps turn would, so the watch side can be checked at home
class DemoReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        val dir = intent.getStringExtra("d")?.firstOrNull() ?: 'L'
        val m = intent.getIntExtra("m", 200)
        val exit = intent.getIntExtra("x", 0).takeIf { it > 0 }
        val turn = notificationTurn("$m m", "", IconMatch(dir, exit)) ?: return
        TurnNotice.post(ctx, turn, intent.getStringExtra("e"))
    }
}
