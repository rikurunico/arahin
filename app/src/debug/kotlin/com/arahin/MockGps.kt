package com.arahin

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationManager
import android.location.provider.ProviderProperties
import android.os.Build
import android.os.SystemClock

// adb shell am broadcast -n com.arahin/.MockGps --ef lat -6.19 --ef lon 106.82 [--ef bearing 90 --ef speed 12]
// moves the phone's location so Google Maps navigates a simulated drive and the whole chain, watch included,
// can be tested at home. Needs Arahin picked under Developer options > Select mock location app.
class MockGps : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        val lm = ctx.getSystemService(LocationManager::class.java)
        val providers = buildList {
            add(LocationManager.GPS_PROVIDER)
            add(LocationManager.NETWORK_PROVIDER)
            if (Build.VERSION.SDK_INT >= 31) add(LocationManager.FUSED_PROVIDER)
        }
        if (intent.getBooleanExtra("stop", false)) {
            providers.forEach { runCatching { lm.removeTestProvider(it) } }
            return
        }
        providers.forEach { p ->
            runCatching {
                if (Build.VERSION.SDK_INT >= 31) {
                    lm.addTestProvider(p, false, false, false, false, true, true, true, ProviderProperties.POWER_USAGE_LOW, ProviderProperties.ACCURACY_FINE)
                } else {
                    @Suppress("DEPRECATION")
                    lm.addTestProvider(p, false, false, false, false, true, true, true, 1, 1)
                }
                lm.setTestProviderEnabled(p, true)
            }
            lm.setTestProviderLocation(p, Location(p).apply {
                latitude = intent.getFloatExtra("lat", 0f).toDouble()
                longitude = intent.getFloatExtra("lon", 0f).toDouble()
                bearing = intent.getFloatExtra("bearing", 0f)
                speed = intent.getFloatExtra("speed", 0f)
                accuracy = 4f
                time = System.currentTimeMillis()
                elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()
            })
        }
    }
}
