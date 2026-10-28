# Arahin

![Layar jam Arahin: semua jenis navigasi](docs/watch-screens.png)

_Pratinjau semua layar jam (render di browser dari HTML/CSS yang sama dengan aplikasi jam, bukan foto jam asli)._

Teruskan petunjuk belok Google Maps ke smartwatch (dibuat untuk Huawei Watch Fit 4), jadi cukup lirik pergelangan tangan saat touring.

Arahin membaca notifikasi navigasi Google Maps, lalu memposting notifikasi singkat seperti `◀ KIRI 200 m` atau `▶ KANAN 1,5 km`. Jam menampilkannya lewat notifikasi mirroring bawaan (Huawei Health, Wear OS, dll.), tanpa app di jam.

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

## Batasan

- Format notifikasi Google Maps tidak dijamin stabil. Parser ada di `Turn.kt` (dengan tes di `TurnTest.kt`), dan mengenali kata kunci Indonesia dan Inggris.
- Dua belokan satu arah berturut-turut hanya bergetar sekali.
- Belum diuji di semua jam. Jam harus meneruskan notifikasi app pihak ketiga.

## Lisensi

MIT, lihat [LICENSE](LICENSE).
