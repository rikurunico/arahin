package com.arahin

import android.app.Activity
import android.content.Context
import com.huawei.wearengine.HiWear
import com.huawei.wearengine.auth.AuthCallback
import com.huawei.wearengine.auth.Permission
import com.huawei.wearengine.device.Device
import com.huawei.wearengine.p2p.Message
import com.huawei.wearengine.p2p.SendCallback
import org.json.JSONObject

private const val WATCH_PKG = "com.arahin.watch"
private const val WATCH_FINGERPRINT = "REPLACE_WITH_WATCH_SIGNING_FINGERPRINT"

object WatchLink {
    @Volatile private var device: Device? = null
    private var lastBody = ""
    private var lastAt = 0L

    fun authorize(activity: Activity, done: () -> Unit) {
        HiWear.getAuthClient(activity).requestPermission(object : AuthCallback {
            override fun onOk(permissions: Array<Permission>) = done()
            override fun onCancel() = done()
        }, Permission.DEVICE_MANAGER)
    }

    fun refresh(ctx: Context) {
        HiWear.getDeviceClient(ctx).bondedDevices.addOnSuccessListener { devices ->
            device = devices.firstOrNull { it.isConnected }
        }
    }

    // the exact JSON the watch's index.js parses
    fun payload(turn: Turn, eta: String?): String = JSONObject()
        .put("d", turn.dir.toString()).put("m", turn.dist.orEmpty()).put("n", turn.meters ?: -1).put("t", turn.label).put("x", turn.exit ?: 0).put("e", eta.orEmpty())
        .toString()

    fun send(ctx: Context, turn: Turn, eta: String?) {
        val d = device ?: return refresh(ctx)
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
            override fun onSendResult(resultCode: Int) {}
            override fun onSendProgress(progress: Long) {}
        })
    }
}
