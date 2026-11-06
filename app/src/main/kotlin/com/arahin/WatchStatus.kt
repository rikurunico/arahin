package com.arahin

import com.huawei.wearengine.common.WearEngineErrorCode as Code

// What the phone knows about its Wear Engine link to the watch, shown in the setup checklist
sealed interface LinkState {
    data object Unknown : LinkState
    data object NoPermission : LinkState
    data object NoWatch : LinkState
    data class Ready(val watch: String) : LinkState
    data class Sent(val watch: String) : LinkState
    data class Failed(val code: Int, val at: String) : LinkState // at = which Wear Engine call failed
}

val LinkState.linked get() = this is LinkState.Ready || this is LinkState.Sent

fun LinkState.label(): String = when (this) {
    LinkState.Unknown -> "Belum dicek."
    LinkState.NoPermission -> "Izin Wear Engine belum diberikan. Tekan Hubungkan."
    LinkState.NoWatch -> "Tidak ada jam yang tersambung. Cek Huawei Health."
    is LinkState.Ready -> "Terhubung ke $watch. Buka app Arahin di jam, lalu mulai navigasi."
    is LinkState.Sent -> "Terkirim ke $watch."
    is LinkState.Failed -> "${failureText(code)} (kode $code, $at)."
}

internal fun failureText(code: Int) = when (code) {
    Code.ERROR_CODE_UNINSTALL_HEALTH -> "Huawei Health belum terpasang"
    Code.ERROR_CODE_HEALTH_LOGOUT -> "Huawei Health belum login"
    Code.ERROR_CODE_UNBOUND_DEVICE -> "Belum ada jam yang dipasangkan"
    Code.ERROR_CODE_USER_UNAUTHORIZED_IN_HEALTH, Code.ERROR_CODE_USER_UNAUTHORIZED_IN_WEAR_ENGINE ->
        "Izin Wear Engine belum diberikan, tekan Hubungkan"
    Code.ERROR_CODE_SCOPE_UNAUTHORIZED -> "Huawei belum menyetujui izin Wear Engine untuk app ini"
    Code.ERROR_CODE_DEVICE_VERSION_NOT_SUPPORT -> "Versi jam belum mendukung"
    Code.ERROR_CODE_HEALTH_VERSION_IS_LOW -> "Huawei Health perlu diperbarui"
    Code.ERROR_CODE_HEALTH_NOT_AUTO_START -> "Huawei Health belum boleh autostart"
    Code.ERROR_CODE_DEVICE_IS_NOT_CONNECTED -> "Jam tidak tersambung"
    Code.ERROR_CODE_P2P_WATCH_APP_NOT_EXIT -> "App Arahin belum terpasang di jam"
    Code.ERROR_CODE_P2P_WATCH_APP_NOT_RUNNING -> "App Arahin di jam belum dibuka"
    Code.ERROR_CODE_COMM_FAIL -> "Pesan gagal terkirim. Nyalakan layar jam dan buka app Arahin di jam"
    else -> "Gagal"
}
