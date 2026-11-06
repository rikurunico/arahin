package com.arahin

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Handler
import android.os.Looper
import com.huawei.wearengine.HiWear
import com.huawei.wearengine.WearEngineException
import com.huawei.wearengine.auth.AuthCallback
import com.huawei.wearengine.auth.Permission
import com.huawei.wearengine.common.WearEngineErrorCode
import com.huawei.wearengine.device.Device
import com.huawei.wearengine.p2p.Message
import com.huawei.wearengine.p2p.SendCallback
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val WATCH_PKG = "com.arahin.watch"
// Lite Wearable format: <bundle>_<base64 of the 65-byte EC public key in the last certificate of the watch .cer>
private const val WATCH_FINGERPRINT = "com.arahin.watch_BC0UOQc+K43wZsl8kgMn63fetk/E8sAXYOHyZKXAw0esirI2DuiCw7pPTRlzK3Y3P2yxEe8TuxTK9F6PrT+e1nc="
private const val RETRY_MS = 5_000L
private const val SEND_BACKOFF_MS = 10_000L
private const val KEEPALIVE_MS = 5_000L
private const val LAUNCH_MS = 1_500L // time a watch app needs to start after a ping

object WatchLink {
    @Volatile var state: LinkState = LinkState.Unknown
        private set

    // called on whatever thread the change happened on
    @Volatile var onChange: (() -> Unit)? = null

    @Volatile private var device: Device? = null

    // version code of the watch app that is installed, -1 when there is none, null until asked
    @Volatile var watchVersion: Int? = null
        private set
    private var lastBody = ""
    private var lastAt = 0L
    private var retryAt = 0L
    private val handler = Handler(Looper.getMainLooper())
    @Volatile private var latest: Pair<Turn, Trip>? = null // what the watch should be showing right now
    private var lookedUpAt = 0L

    fun authorize(activity: Activity) {
        HiWear.getAuthClient(activity).requestPermission(object : AuthCallback {
            override fun onOk(permissions: Array<Permission>) = refresh(activity)
            override fun onCancel() = update(LinkState.NoPermission)
        }, Permission.DEVICE_MANAGER)
    }

    fun refresh(ctx: Context) {
        lookedUpAt = System.currentTimeMillis()
        HiWear.getAuthClient(ctx).checkPermission(Permission.DEVICE_MANAGER)
            .addOnSuccessListener { granted -> if (granted) findWatch(ctx) else update(LinkState.NoPermission) }
            .addOnFailureListener { fail(it, "cek izin") }
    }

    private fun findWatch(ctx: Context) {
        HiWear.getDeviceClient(ctx).bondedDevices
            .addOnSuccessListener { devices ->
                val watch = devices.firstOrNull { it.isConnected }
                device = watch
                if (watch != null) askWatchVersion(ctx, watch)
                val current = state
                update(when {
                    watch == null -> LinkState.NoWatch
                    current is LinkState.Sent -> current // keep "sent" until something changes
                    else -> LinkState.Ready(watch.name)
                })
            }
            .addOnFailureListener { fail(it, "cari jam") }
    }

    private fun askWatchVersion(ctx: Context, watch: Device) {
        p2p(ctx).getAppVersion(watch, WATCH_PKG).addOnSuccessListener { v ->
            if (v != watchVersion) {
                watchVersion = v
                onChange?.invoke()
            }
        }
    }

    private fun fail(e: Exception, at: String) {
        device = null
        update(LinkState.Failed((e as? WearEngineException)?.errorCode ?: WearEngineErrorCode.ERROR_CODE_GENERIC, at))
    }

    private fun update(next: LinkState) {
        if (next == state) return
        state = next
        onChange?.invoke()
    }

    // the exact JSON the watch's index.js parses
    fun payload(turn: Turn, trip: Trip, battery: Int = -1, clock: String = ""): String = JSONObject()
        .put("d", turn.dir.toString()).put("m", turn.dist.orEmpty()).put("n", turn.meters ?: -1).put("t", turn.label).put("x", turn.exit ?: 0).put("e", trip.arrival).put("u", trip.minutes?.let { "$it mnt" }.orEmpty()).put("r", distanceLabel(trip.meters)).put("b", battery).put("c", clock)
        .toString()

    // manual = a person pressed "Kirim tes": try now even if a recent send failed
    fun send(ctx: Context, turn: Turn, trip: Trip, manual: Boolean = false) {
        latest = turn to trip // even when this call is throttled, so a watch app opened later gets the newest turn
        val d = device ?: return retryLookup(ctx)
        val body = payload(turn, trip, batteryPercent(ctx), SimpleDateFormat("HH:mm", Locale.US).format(Date()))
        // Maps re-posts every second; skip repeats and cap the Bluetooth traffic at 1 message/s
        val now = System.currentTimeMillis()
        // an unchanged turn is still re-sent every few seconds: a watch app that was just opened, or woke up empty, fills
        // itself without waiting for the car to move
        if ((body == lastBody && now - lastAt < KEEPALIVE_MS) || now - lastAt < 1000 || (now < retryAt && !manual)) return
        lastBody = body
        lastAt = now
        p2p(ctx).send(d, Message.Builder().setPayload(body.toByteArray()).build(), object : SendCallback {
            override fun onSendResult(resultCode: Int) {
                if (resultCode == WearEngineErrorCode.ERROR_CODE_COMM_SUCCESS) return update(LinkState.Sent(d.name))
                // retry later, e.g. once the watch app is opened, but not on every Maps update: a failing
                // Bluetooth send each second all ride would drain both batteries
                lastBody = ""
                retryAt = System.currentTimeMillis() + SEND_BACKOFF_MS
                if (resultCode == WearEngineErrorCode.ERROR_CODE_DEVICE_IS_NOT_CONNECTED) device = null
                update(LinkState.Failed(resultCode, "kirim"))
            }

            override fun onSendProgress(progress: Long) {}
        })
    }

    // Brings the watch app to the screen, like tapping the turn notification would: a ping starts the app when it
    // is not running. Meant for the moments a turn notification is posted, not for every Maps update.
    // Once the app has had time to start, the newest turn goes over so it is not left on "Menunggu rute".
    fun open(ctx: Context) {
        val d = device ?: return
        val app = ctx.applicationContext
        p2p(ctx).ping(d) { code ->
            if (code == WearEngineErrorCode.ERROR_CODE_P2P_WATCH_APP_NOT_EXIT) return@ping update(LinkState.Failed(code, "buka app"))
            handler.postDelayed({ latest?.let { (turn, trip) -> send(app, turn, trip, manual = true) } }, LAUNCH_MS)
        }
    }

    // phone battery for the watch screen; -1 when the system does not say. The sticky broadcast needs no permission.
    private fun batteryPercent(ctx: Context): Int {
        val i = ctx.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED)) ?: return -1
        val scale = i.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        return if (scale > 0) i.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) * 100 / scale else -1
    }

    private fun p2p(ctx: Context) = HiWear.getP2pClient(ctx).setPeerPkgName(WATCH_PKG).setPeerFingerPrint(WATCH_FINGERPRINT)

    // Maps posts every second; do not look the watch up that often while it is missing
    private fun retryLookup(ctx: Context) {
        if (System.currentTimeMillis() - lookedUpAt >= RETRY_MS) refresh(ctx)
    }
}
