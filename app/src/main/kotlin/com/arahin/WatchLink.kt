package com.arahin

import android.app.Activity
import android.content.Context
import com.huawei.wearengine.HiWear
import com.huawei.wearengine.WearEngineException
import com.huawei.wearengine.auth.AuthCallback
import com.huawei.wearengine.auth.Permission
import com.huawei.wearengine.common.WearEngineErrorCode
import com.huawei.wearengine.device.Device
import com.huawei.wearengine.p2p.Message
import com.huawei.wearengine.p2p.SendCallback
import org.json.JSONObject

private const val WATCH_PKG = "com.arahin.watch"
private const val WATCH_FINGERPRINT = "0AA3F3346209632D66BD054693AD929C4830A470B94701DFEA0E3B711035EB1D"
private const val RETRY_MS = 5_000L

object WatchLink {
    @Volatile var state: LinkState = LinkState.Unknown
        private set

    // called on whatever thread the change happened on
    @Volatile var onChange: (() -> Unit)? = null

    @Volatile private var device: Device? = null
    private var lastBody = ""
    private var lastAt = 0L
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
                val current = state
                update(when {
                    watch == null -> LinkState.NoWatch
                    current is LinkState.Sent -> current // keep "sent" until something changes
                    else -> LinkState.Ready(watch.name)
                })
            }
            .addOnFailureListener { fail(it, "cari jam") }
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
    fun payload(turn: Turn, eta: String?): String = JSONObject()
        .put("d", turn.dir.toString()).put("m", turn.dist.orEmpty()).put("n", turn.meters ?: -1).put("t", turn.label).put("x", turn.exit ?: 0).put("e", eta.orEmpty())
        .toString()

    fun send(ctx: Context, turn: Turn, eta: String?) {
        val d = device ?: return retryLookup(ctx)
        val body = payload(turn, eta)
        // Maps re-posts every second; skip repeats and cap the Bluetooth traffic at 1 message/s
        val now = System.currentTimeMillis()
        if (body == lastBody || now - lastAt < 1000) return
        lastBody = body
        lastAt = now
        val p2p = HiWear.getP2pClient(ctx)
        p2p.setPeerPkgName(WATCH_PKG)
        p2p.setPeerFingerPrint(WATCH_FINGERPRINT)
        p2p.send(d, Message.Builder().setPayload(body.toByteArray()).build(), object : SendCallback {
            override fun onSendResult(resultCode: Int) {
                if (resultCode == WearEngineErrorCode.ERROR_CODE_COMM_SUCCESS) return update(LinkState.Sent(d.name))
                lastBody = "" // let the next Maps update retry, e.g. once the watch app is opened
                if (resultCode == WearEngineErrorCode.ERROR_CODE_DEVICE_IS_NOT_CONNECTED) device = null
                update(LinkState.Failed(resultCode, "kirim"))
            }

            override fun onSendProgress(progress: Long) {}
        })
    }

    // Maps posts every second; do not look the watch up that often while it is missing
    private fun retryLookup(ctx: Context) {
        if (System.currentTimeMillis() - lookedUpAt >= RETRY_MS) refresh(ctx)
    }
}
