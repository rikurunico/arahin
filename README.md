# Arahin

Petunjuk belok Google Maps langsung di smartwatch, jadi saat touring cukup lirik pergelangan tangan. Dibuat dan diuji dengan **Huawei Watch Fit 4**.

**[⬇ Unduh APK](https://github.com/rikurunico/arahin/releases/latest)** · **[▶ Demo interaktif](https://rikurunico.github.io/arahin/)**

<p>
  <img src="docs/setup.png" alt="Layar setup Arahin: checklist izin bernomor" width="270">
  &nbsp;
  <img src="docs/watch-screens.png" alt="Layar app jam Arahin untuk semua jenis manuver" width="520">
</p>

_Kiri: layar setup di HP. Kanan: pratinjau app jam (Wear Engine), dirender dari HTML/CSS yang sama dengan app jamnya._

## Cara kerja

1. Arahin membaca notifikasi navigasi Google Maps di HP.
2. Teks notifikasi Maps hanya berisi jarak dan nama jalan, sedangkan arahnya cuma ada di **gambar panah**. Arahin mengenali gambar itu (`ManeuverIcon.kt`) dengan mencocokkannya ke ikon yang diambil dari notifikasi Maps asli, dan memakai teks sebagai cadangan.
3. Arahin memposting notifikasi singkat seperti `◀ BELOK KIRI 200 m`, dengan sisa waktu dan jam tiba di baris kedua.
4. Huawei Health (atau app jam pendamping lain) meneruskannya ke jam, dan jam bergetar.

Maps memperbarui jaraknya kira-kira tiap 10 m. Supaya jam tidak bergetar terus, Arahin hanya memposting saat belokan baru muncul dalam **500 m**, lalu saat jarak melewati **200 m** dan **50 m** (`AlertGate.kt`). Lurus, gabung jalur, dan jalan keluar tidak bergetar.

Sudah diuji dari ujung ke ujung: Google Maps di POCO F6 (Android 16) → Arahin → Huawei Health → Watch Fit 4.

## Pakai

1. Unduh `arahin.apk` dari [Releases](https://github.com/rikurunico/arahin/releases/latest) lalu pasang (Android 8.0+, izinkan pemasangan dari sumber ini).
2. Buka Arahin. Layar pertamanya berupa checklist yang memberi tahu apa saja yang perlu diizinkan, lengkap dengan statusnya. Langkah yang harus dikerjakan berikutnya diberi bingkai kuning, dan langkah yang selesai berubah jadi centang hijau **Aktif**:

   | # | Langkah | Isi di aplikasi | |
   |---|---|---|---|
   | 1 | **Izinkan notifikasi** | Arahin menampilkan belokan sebagai notifikasi, dan notifikasi itulah yang diteruskan ke jam. | Wajib |
   | 2 | **Baca petunjuk Google Maps** | Aktifkan Arahin di layar Akses notifikasi. Hanya notifikasi Google Maps yang dibaca, tidak ada yang dikirim ke internet. | Wajib |
   | 3 | **Tetap jalan di latar belakang** | Kecualikan Arahin dari penghemat baterai supaya tidak dimatikan saat layar HP mati. Di Xiaomi, aktifkan juga Autostart untuk Arahin. | Dianjurkan |
   | 4 | **Teruskan ke jam di Huawei Health** | Buka Huawei Health, pilih jammu, masuk ke Notifikasi, lalu aktifkan Arahin. | Wajib |
   | 5 | **Coba kirim ke jam** | Kirim belokan contoh acak. Arah, jarak, dan waktu tiba berbeda setiap kali. Jam harus bergetar dan menampilkannya. | Dianjurkan |
   | 6 | **Layar navigasi di jam** | Untuk app Arahin di jam (panah besar, getar beda kiri dan kanan). Butuh izin Wear Engine dari Huawei. Lewati kalau cukup notifikasi. | Opsional |

3. Mulai navigasi di Google Maps. Jam bergetar 500, 200, dan 50 m sebelum belokan.

## Privasi

- Tidak ada izin `INTERNET`, jadi data tidak bisa keluar dari HP.
- Hanya notifikasi `com.google.android.apps.maps` yang dibaca.
- Nama jalan tidak diteruskan ke jam. Build release tidak mencatat log apa pun.

## Layar navigasi di jam (opsional, Wear Engine)

Selain notifikasi, Arahin bisa mengirim arah, jarak, dan ETA ke app kecil di jam yang menampilkan panah besar dengan pola getar berbeda untuk kiri dan kanan. Source-nya ada di [`watch/`](watch/README.md).

App ini butuh akun developer Huawei yang sudah terverifikasi, izin **Wear Engine** (Basic device information) yang disetujui Huawei, dan fingerprint signing di `WatchLink.kt` serta `watch/pages/index/index.js`. SHA-256 sertifikat APK release:

```
28:DA:EB:EF:9C:CB:F3:12:5D:20:9F:44:77:F6:5B:D2:FE:B4:50:65:66:73:79:C5:08:01:F4:15:DF:D0:37:D9
```

Tanpa itu semua, jalur notifikasi tetap jalan.

## Build

Buka folder ini di Android Studio, atau dengan Gradle 8.9+ dan JDK 17–21:

```
gradle :app:testDebugUnitTest :app:assembleDebug
```

APK release ditandatangani hanya kalau `local.properties` berisi `arahin.keystore`, `arahin.keystore.password`, dan `arahin.key.alias`. Tanpa itu, APK release dibuat tanpa tanda tangan.

## Kalibrasi dan tes (build debug)

Ikon dan teks sudah diuji dengan notifikasi Google Maps asli di emulator dan di HP sungguhan. Kalau Maps di HP-mu memakai ikon lain:

1. Pasang build **debug** dan aktifkan Akses notifikasi untuk Arahin.
2. Jalankan `adb logcat -s Arahin:D`, lalu navigasi di Google Maps.
3. Setiap baris berbentuk `raw=[judul | teks | ETA] icon=<hash>:<hasil> alert=<getar?> -> <JSON untuk jam>`. Ikon yang hasilnya `null` disimpan di `/sdcard/Android/data/com.arahin/files/icons/<hash>.png`. Tarik dengan `adb pull`, lalu kirim lewat issue atau pull request.

Log dan ikon berisi nama jalan, jadi jangan dibagikan sembarangan.

Alat tes tanpa harus berkendara (hanya ada di build debug):

- **Belokan palsu:** `adb shell am broadcast -n com.arahin/.DemoReceiver --es d L --ei m 200` memposting notifikasi belokan seperti dari Maps. `d` = jenis manuver seperti di [`watch/`](watch/README.md), `m` = meter, `--ei x 2` = pintu keluar bundaran.
- **Perjalanan palsu di HP asli:** jalankan `adb shell appops set com.arahin android:mock_location allow`, lalu kirim lokasi tiap detik dengan `adb shell am broadcast -n com.arahin/.MockGps --ef lat <lat> --ef lon <lon> --ef bearing <derajat>` sambil navigasi Google Maps berjalan. Matikan dengan `--ez stop true` dan `appops set ... default`.
- **Emulator:** image Google APIs dengan Google Maps. Lokasi digerakkan dengan `adb emu geo fix <lon> <lat>`.

## Batasan

- Ikon dan format notifikasi Google Maps tidak dijamin stabil, dan berbeda antar versi. Contohnya, Android 16 memakai notifikasi "Live Update" dengan jarak dan instruksi di judul. Pencocokan ikon diuji dengan ikon asli di `ManeuverIconTest.kt`.
- Bundaran: jam menampilkan arah keluar (kiri, lurus, kanan), bukan nomor pintu keluar sebenarnya.
- Lewat notifikasi, pola getarnya pola bawaan jam, tidak dibedakan kiri dan kanan.
- Jam harus bisa meneruskan notifikasi app pihak ketiga.

## Lisensi

MIT, lihat [LICENSE](LICENSE).
