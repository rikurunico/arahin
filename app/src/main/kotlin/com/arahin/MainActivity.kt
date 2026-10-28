package com.arahin

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

private const val HEALTH = "com.huawei.health"

// Setup checklist: each step says what to allow and why, shows whether it is done,
// and re-checks every time the user comes back from a settings screen.
class MainActivity : Activity() {
    private enum class Kind { REQUIRED, RECOMMENDED, OPTIONAL }

    private class Step(
        val kind: Kind,
        val title: String,
        val body: String,
        val action: String,
        val done: (() -> Boolean)?, // null = cannot be checked from the phone
        val run: () -> Unit,
    )

    private val steps by lazy {
        listOf(
            Step(
                Kind.REQUIRED, "Izinkan notifikasi",
                "Arahin menampilkan belokan sebagai notifikasi, dan notifikasi itulah yang diteruskan ke jam.",
                "Izinkan", ::notificationsAllowed, ::askNotifications,
            ),
            Step(
                Kind.REQUIRED, "Baca petunjuk Google Maps",
                "Aktifkan Arahin di layar Akses notifikasi. Hanya notifikasi Google Maps yang dibaca, tidak ada yang dikirim ke internet.",
                "Buka pengaturan", ::listenerEnabled,
            ) { open(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) },
            Step(
                Kind.RECOMMENDED, "Tetap jalan di latar belakang",
                "Kecualikan Arahin dari penghemat baterai supaya tidak dimatikan saat layar HP mati." +
                    if (isXiaomi()) " Di Xiaomi, aktifkan juga Autostart untuk Arahin." else "",
                "Kecualikan", ::batteryExempt, ::askBattery,
            ),
            Step(
                Kind.REQUIRED, "Teruskan ke jam di Huawei Health",
                if (installed(HEALTH)) "Buka Huawei Health, pilih jammu, masuk ke Notifikasi, lalu aktifkan Arahin."
                else "Huawei Health belum terpasang. Pasang dan sambungkan jammu, lalu aktifkan notifikasi Arahin di sana.",
                "Buka Huawei Health", null,
            ) { packageManager.getLaunchIntentForPackage(HEALTH)?.let(::open) ?: toast("Huawei Health tidak ditemukan") },
            Step(
                Kind.RECOMMENDED, "Coba kirim ke jam",
                "Kirim satu belokan contoh. Jam harus bergetar dan menampilkan \"◀ BELOK KIRI 200 m\".",
                "Kirim tes", null, ::sendTest,
            ),
            Step(
                Kind.OPTIONAL, "Layar navigasi di jam",
                "Untuk app Arahin di jam (panah besar, getar beda kiri dan kanan). Butuh izin Wear Engine dari Huawei. Lewati kalau cukup notifikasi.",
                "Hubungkan", null,
            ) { WatchLink.authorize(this) { toast("Selesai") } },
        )
    }

    private lateinit var list: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        list = findViewById(R.id.steps)
        steps.forEach { s ->
            val row = layoutInflater.inflate(R.layout.step, list, false)
            row.findViewById<TextView>(R.id.title).text = s.title
            row.findViewById<TextView>(R.id.body).text = s.body
            row.findViewById<Button>(R.id.action).apply {
                text = s.action
                setOnClickListener { s.run() }
            }
            list.addView(row)
        }
    }

    override fun onResume() {
        super.onResume()
        render()
    }

    private fun render() {
        val required = steps.filter { it.kind == Kind.REQUIRED && it.done != null }
        val left = required.count { it.done?.invoke() != true }
        findViewById<TextView>(R.id.progress).text =
            if (left == 0) "Izin HP lengkap" else "${required.size - left} dari ${required.size} izin wajib aktif"
        // the first unchecked step is the one to do next
        val next = steps.indexOfFirst { it.done?.invoke() == false }.takeIf { it >= 0 }
            ?: steps.indexOfFirst { it.done == null && it.kind == Kind.REQUIRED }
        steps.forEachIndexed { i, s ->
            val row = list.getChildAt(i)
            val done = s.done?.invoke() == true
            val badge = row.findViewById<TextView>(R.id.badge)
            badge.text = if (done) "✓" else "${i + 1}"
            badge.setTextColor(getColor(if (done) R.color.on_accent else if (s.kind == Kind.OPTIONAL) R.color.muted else R.color.accent))
            badge.setBackgroundResource(if (done) R.drawable.badge_done else if (s.kind == Kind.OPTIONAL) R.drawable.badge_info else R.drawable.badge_todo)
            row.setBackgroundResource(if (i == next) R.drawable.card_current else R.drawable.card)
            row.alpha = if (done) 0.75f else 1f
            row.findViewById<TextView>(R.id.state).apply {
                visibility = View.VISIBLE
                when {
                    done -> { text = "Aktif"; setTextColor(getColor(R.color.ok)) }
                    s.kind == Kind.REQUIRED -> { text = "Wajib"; setTextColor(getColor(R.color.accent)) }
                    s.kind == Kind.RECOMMENDED -> { text = "Dianjurkan"; setTextColor(getColor(R.color.muted)) }
                    else -> { text = "Opsional"; setTextColor(getColor(R.color.muted)) }
                }
            }
            row.findViewById<Button>(R.id.action).apply {
                visibility = if (done) View.GONE else View.VISIBLE
                setBackgroundResource(if (i == next) R.drawable.btn_accent else R.drawable.btn_outline)
                setTextColor(getColor(if (i == next) R.color.on_accent else R.color.ink))
            }
        }
    }

    private fun notificationsAllowed() = getSystemService(NotificationManager::class.java).areNotificationsEnabled()

    private fun askNotifications() {
        val post = Manifest.permission.POST_NOTIFICATIONS
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(post) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(post), 0)
        } else {
            openNotificationSettings()
        }
    }

    // a second refusal is final for the system dialog, so send the user to the app's notification settings
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (!notificationsAllowed() && !shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
            openNotificationSettings()
        }
        render()
    }

    private fun openNotificationSettings() =
        open(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, packageName))

    private fun listenerEnabled(): Boolean {
        val enabled = Settings.Secure.getString(contentResolver, "enabled_notification_listeners").orEmpty()
        return ComponentName(this, MapsRelay::class.java).flattenToString() in enabled.split(':')
    }

    private fun batteryExempt() = getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(packageName)

    @SuppressLint("BatteryLife") // a navigation relay that the OS kills mid-ride is useless
    private fun askBattery() {
        // Xiaomi's Autostart list goes underneath, so it is there when the battery dialog closes
        if (isXiaomi()) {
            open(Intent().setComponent(ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity")))
        }
        open(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:$packageName")))
    }

    private fun sendTest() {
        if (!notificationsAllowed()) return toast("Izinkan notifikasi dulu (langkah 1)")
        notificationTurn("200 m", "", IconMatch('L', null))?.let { TurnNotice.post(this, it) }
        toast("Terkirim. Cek jammu.")
    }

    private fun isXiaomi() = Build.MANUFACTURER.equals("Xiaomi", ignoreCase = true)

    private fun installed(pkg: String) =
        runCatching { packageManager.getPackageInfo(pkg, 0) }.isSuccess

    private fun open(intent: Intent) {
        runCatching { startActivity(intent) }.onFailure { toast("Pengaturan ini tidak tersedia di HP ini") }
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}
