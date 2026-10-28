# Arahin

![Layar jam Arahin: semua jenis navigasi](docs/watch-screens.png)

_Pratinjau semua layar jam (render di browser dari HTML/CSS yang sama dengan aplikasi jam, bukan foto jam asli)._

Teruskan petunjuk belok Google Maps ke smartwatch (dibuat untuk Huawei Watch Fit 4), jadi cukup lirik pergelangan tangan saat touring.

Arahin membaca notifikasi navigasi Google Maps, lalu memposting notifikasi singkat seperti `◀ KIRI 200 m` atau `▶ KANAN 1,5 km`. Jam menampilkannya lewat notifikasi mirroring bawaan (Huawei Health, Wear OS, dll.), tanpa app di jam.

Teks notifikasi Maps hanya berisi jarak dan nama jalan (`200 m` · `Jl. Sudirman`), arahnya cuma ada di gambar panah. Jadi Arahin membaca arah dari gambar itu (`ManeuverIcon.kt`, dicocokkan dengan ikon yang diambil dari notifikasi Maps asli), dan memakai teks hanya kalau gambarnya tidak dikenali.

Supaya jam tidak bergetar setiap kali Maps memperbarui jarak (kira-kira tiap 10 m), notifikasi hanya diposting saat belokan baru muncul dalam 500 m, lalu saat jarak melewati 200 m dan 50 m (`AlertGate.kt`). Lurus, gabung jalur, dan jalan keluar tidak bergetar.

## Privasi

- Tidak ada izin `INTERNET`, jadi data tidak bisa keluar dari HP.
- Hanya notifikasi `com.google.android.apps.maps` yang dibaca.
- Tidak ada log, penyimpanan, atau backup. Nama jalan tidak diteruskan.

## Pakai

1. Buka folder ini di Android Studio, lalu jalankan ke HP (Android 8.0+).
2. Buka Arahin, izinkan notifikasi, dan aktifkan Arahin di layar Notification access.
3. Di app jam pendamping (misalnya Huawei Health), aktifkan notifikasi untuk Arahin.
4. Mulai navigasi di Google Maps.

## Layar navigasi di jam (opsional, Wear Engine)

Selain notifikasi, Arahin bisa mengirim arah, jarak, dan ETA ke app kecil di jam yang menampilkan panah besar. Source jam ada di [`watch/`](watch/README.md). Butuh akun developer Huawei, izin Wear Engine untuk app HP (Basic device information), HMS-compatible Huawei Health di HP, dan signing fingerprint di `WatchLink.kt` serta `watch/pages/index/index.js`. Tanpa itu, jalur notifikasi tetap jalan.

## Kalibrasi (cek notifikasi Maps di HP-mu)

Ikon dan teks sudah diuji dengan notifikasi Google Maps asli (Android, Indonesia). Kalau Maps di HP-mu memakai ikon lain:

1. Pasang build **debug** (tombol Run di Android Studio), aktifkan Notification access untuk Arahin.
2. `adb logcat -s Arahin:D`, lalu jalankan navigasi nyata di Google Maps.
3. Setiap baris berbentuk `raw=[jarak | jalan | ETA] icon=<hash>:<hasil> alert=<getar?> -> <JSON untuk jam>`. Ikon yang hasilnya `null` disimpan di `/sdcard/Android/data/com.arahin/files/icons/<hash>.png`: tarik dengan `adb pull`, lalu kirim lewat issue atau pull request.

Log dan ikon hanya dibuat di build debug dan berisi nama jalan, jadi jangan dibagikan sembarangan. Build release tidak mencatat apa pun.

Tanpa HP: emulator dengan Google Play/Google APIs juga bisa. Lokasi digerakkan dengan `adb emu geo fix <lon> <lat>`.

## Batasan

- Ikon dan format notifikasi Google Maps tidak dijamin stabil. Pencocokan ikon ada di `ManeuverIcon.kt` (tes dengan ikon asli di `ManeuverIconTest.kt`), parser teks cadangan di `Turn.kt`.
- Bundaran: jam menampilkan arah keluar (kiri, lurus, kanan), bukan nomor pintu keluar sebenarnya.
- Belum dicoba apakah Huawei Health ikut meredam update notifikasi yang sama. Karena itu Arahin memang hanya memposting di momen getar.
- Belum diuji di semua jam. Jam harus meneruskan notifikasi app pihak ketiga.

## Lisensi

MIT, lihat [LICENSE](LICENSE).
