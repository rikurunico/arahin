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

## Kalibrasi parser (cek teks asli Maps di HP-mu)

Pola di `Turn.kt` sudah diuji dengan kalimat nyata dari Google Maps web (bahasa Indonesia dan Inggris), tapi teks di **notifikasi** HP bisa berbeda, dan kalimat "sampai di tujuan" belum pernah terlihat. Untuk memastikannya:

1. Pasang build **debug** (tombol Run di Android Studio), aktifkan Notification access untuk Arahin.
2. `adb logcat -s Arahin:D`, lalu jalankan navigasi nyata di Google Maps sampai tiba di tujuan.
3. Setiap baris berbentuk `raw=[teks asli notifikasi] -> hasil parser`. Baris yang hasilnya `dir=?` atau salah arah adalah pola yang perlu ditambah: kirim baris itu lewat issue atau pull request.

Log hanya dicetak di build debug dan berisi nama jalan, jadi jangan dibagikan sembarangan. Build release tidak mencatat apa pun.

Alternatif tanpa build debug: `adb shell dumpsys notification --noredact | grep -A25 com.google.android.apps.maps` saat navigasi berjalan.

## Batasan

- Format notifikasi Google Maps tidak dijamin stabil. Parser ada di `Turn.kt` (dengan tes di `TurnTest.kt`), dan mengenali kata kunci Indonesia dan Inggris.
- Dua belokan satu arah berturut-turut hanya bergetar sekali.
- Belum diuji di semua jam. Jam harus meneruskan notifikasi app pihak ketiga.

## Lisensi

MIT, lihat [LICENSE](LICENSE).
