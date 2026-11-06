# Arahin watch app (Huawei Lite Wearable, JS)

Layar navigasi di jam: instruksi, panah besar, jarak, dan ETA, dikirim dari app HP lewat Wear Engine.

Sudah berhasil dipasang di Huawei Watch Fit 4 (HarmonyOS 6.1.0) dengan DevEco Studio 6.1.1, template Lite Wearable (JS), dan `wearengine.js` 5.0.2.306. Masukkan source ini ke project DevEco Studio kamu sendiri, karena `config.json`, signing, dan `wearengine.js` dibuat atau diunduh di sana.

## Pasang

1. DevEco Studio: buat project **Lite Wearable** (JS, template Empty), bundle name `com.arahin.watch`. Lalu di `build-profile.json5` set `"compatibleSdkVersion": "5.1.0(18)"`. Ini yang paling gampang terlewat, lihat [Catatan pengerjaan](#catatan-pengerjaan).
2. Unduh `wearengine.js` ([Wear Engine SDK, Lite Wearable](https://developer.huawei.com/consumer/en/doc/connectivity-guides/integrating-fitnesstwatch-sdk-0000001052859174)) ke `entry/src/main/js/MainAbility/pages/`.
3. Salin isi folder `pages/index/` di sini ke `entry/src/main/js/MainAbility/pages/index/`, lalu salin folder `common/images/` (empat `arrow-*.png`) ke `entry/src/main/js/MainAbility/common/`.
   Ikon app jam (`media/icon.png` 104×104 dan `media/icon_small.png` 92×92, lingkaran dengan sudut transparan) disalin ke `entry/src/main/resources/base/media/`.
4. Di `config.json` modul, tambahkan (fingerprint = SHA-256 sertifikat signing app HP):
   ```json
   "metaData": { "customizeData": [{ "name": "supportLists", "value": "com.arahin:<PHONE_FINGERPRINT>", "extra": "" }] }
   ```
5. Isi `PHONE_FINGERPRINT` di `index.js` dengan nilai yang sama.
6. Siapkan signing:
   - **Build → Generate Key and CSR** di DevEco, hasilnya `.p12` dan `.csr`.
   - Ambil UDID jam dari app **DevEco Assistant** di HP (tab Lite Wearable, jam harus tersambung lewat Huawei Health).
   - Di AppGallery Connect → *Certificates, app IDs, and profiles*: tambah jam di **Devices** (tipe Sports watch), upload `.csr` sebagai sertifikat **Debug**, buat **App ID** `com.arahin.watch`, lalu buat **Profile** tipe Debug yang berisi app, sertifikat, dan jam itu. Unduh `.cer` dan `.p7b`.
   - Di DevEco: **File → Project Structure → Signing Configs**, matikan "Automatically generate signature", isi `.p12`, password, alias, `.p7b`, dan `.cer`.
7. Isi `WATCH_FINGERPRINT` di `WatchLink.kt` dengan `<bundle name>_<base64 kunci publik>`, dari sertifikat **terakhir** di `.cer` (milik kamu; dua sebelumnya root dan CA Huawei). Kunci publik yang dimaksud adalah titik EC mentah 65 byte (berawalan `0x04`), bukan SHA-256. Di PowerShell: ambil blok `BEGIN/END CERTIFICATE` terakhir, buat `X509Certificate2`, lalu `[Convert]::ToBase64String($cert.PublicKey.EncodedKeyValue.RawData)`.
8. **Build → Build Hap(s)**, salin `entry-default-signed.hap` ke folder `/sdcard/haps/` di HP, lalu di DevEco Assistant buka **Apps**, pilih file itu, dan **Install**.

## Catatan pengerjaan

Hal-hal yang makan waktu waktu pertama kali, supaya tidak diulang:

- **Error `40. Invalid configuration file format` saat Install: turunkan `compatibleSdkVersion`.** Pesannya menyesatkan, isi `config.json` tidak salah. DevEco 6.1.1 default-nya mengisi `compatible` dengan versi SDK-nya sendiri (`6.1.1(24)`), lebih tinggi dari API jam, dan jam menolak HAP itu. `6.1.1(24)` dan `6.1.0(23)` gagal, `5.1.0(18)` berhasil. `targetSdkVersion` dibiarkan `6.1.1(24)` karena itu SDK yang terpasang. Nilai 5.1.0(18) kami ambil dari proyek Lite Wearable lain yang terbukti jalan di Watch GT 6 ([pomodoro-odak](https://github.com/ahmetkurtbm/pomodoro-odak)). Batas persis API Watch Fit 4 belum kami cari tahu, jadi nilai di antara 18 dan 22 belum dicoba.
- **Ikon wajib ada dengan ukuran persis:** `icon.png` 104×104 dan `icon_small.png` 92×92 di `entry/src/main/resources/base/media/`. Kalau tidak, error 40 yang sama muncul dari sebab lain.
- **HAP harus ada di `/sdcard/haps/`.** Halaman Apps di DevEco Assistant hanya membaca folder itu. Pakai file `entry-default-signed.hap`, bukan `unsigned`.
- **Jangan pakai regex di JS jam.** Mesin JS Lite Wearable tidak mendukungnya dan install gagal dengan error 34 (kata proyek referensi di atas). `wearengine.js` dan `index.js` sudah dicek, tidak ada.
- **Dua warning build yang aman:** `font-weight is not a standard attribute` di `index.css` (teks jadi tidak tebal) dan `can not sign bin with codesign` (hanya muncul kalau build dari terminal).
- **Build dari terminal:** `DEVECO_SDK_HOME` harus menunjuk ke folder `sdk` di instalasi DevEco, lalu jalankan `node hvigorw.js --mode module -p product=default assembleHap` dari `tools/hvigor/bin`. Setelah mengubah `build-profile.json5` di IDE, jalankan sync dulu sebelum build, kalau tidak nilai lama yang terpakai.
- **Dua format fingerprint, tergantung arah.** Di jam (`PHONE_FINGERPRINT`, `supportLists`) pakai SHA-256 sertifikat release app HP, tanpa titik dua, dan pasang APK **release** di HP (bukan debug). Di HP (`WATCH_FINGERPRINT`) pakai `<bundle>_<base64 kunci publik>` seperti langkah 7. `.cer` dari AppGallery Connect berisi tiga sertifikat; yang dipakai yang ketiga.
- **Sisi HP juga perlu didaftarkan.** Kode 12 saat cek izin berhenti setelah (1) SHA-256 sertifikat release ditambahkan di AppGallery Connect, di app HP (`com.arahin`), dan (2) manifest HP memuat `<meta-data android:name="com.huawei.hms.client.appid" android:value="appid=<App ID>"/>`.
- **Kode 206 saat kirim: pastikan layar jam menyala dan app Arahin terbuka di jam.** Itu penyebab 206 yang kami alami. Kami sempat mengganti format `WATCH_FINGERPRINT` ke `<bundle>_<base64 kunci publik>` karena sebuah artikel menyebut format yang salah menyebabkan 206, jadi belum jelas apakah format lama (SHA-256) juga akan ditolak. Format baru terbukti jalan.
- **Terbukti di Watch Fit 4:** build, signing, instal, dan pesan dari HP ke jam lewat tombol Kirim tes. Navigasi Google Maps sungguhan di jalan belum diuji.

## Format pesan dari HP

JSON kecil: `{"d":"R","t":"Belok kanan","m":"116 m","n":116,"x":0,"e":"4.8 km · 10:50 · 12 min"}`.

- `d` = jenis manuver: `S` lurus/lanjut/awal rute, `L` `R` belok, `l` `r` serong/tetap di sisi/percabangan/ramp, `h` `j` belok tajam, `U` putar balik, `O` bundaran, `D` tujuan, `M` gabung jalur, `E` ambil jalan keluar (sisi tidak diketahui), `Q` keluar dari bundaran, `?` teks yang tidak dikenali (ditampilkan apa adanya).
- `t` = instruksi singkat berbahasa Indonesia yang ditampilkan di jam, `m` = jarak, `n` = jarak dalam meter (-1 kalau tidak ada), `x` = nomor pintu keluar bundaran (0 kalau tidak ada), `e` = ETA.
- Ikon: `common/images/arrow-{s,l,r,sl,sr,hl,hr,u,o,o1,o2,o3,d}.png`. Ikon bundaran `o1` `o2` `o3` menggambar bundaran 4 lengan dari bawah: pintu yang harus diambil terang dengan panah, pintu lain redup. Cuma skema, bukan bentuk bundaran aslinya. `DRIVE_LEFT` di `index.js` (default `true`, lalu lintas kiri) menentukan pintu 1 di kiri atau kanan.

Getar: kiri (termasuk serong dan tajam) 1x pendek, kanan 2x pendek, putar balik 1x panjang, bundaran 1x panjang + 1x pendek, tujuan 2x panjang, lurus, gabung, dan jalan keluar senyap. Getar saat melewati 500 m, 200 m, dan 50 m menuju belokan, dan saat arah berganti kalau jaraknya sudah ≤ 500 m (belokan yang baru muncul dari jauh tidak bergetar). Bar di bawah jarak terisi mulai 300 m sebelum belokan. Angka jarak berwarna putih, kuning di bawah 200 m, dan oranye di bawah 50 m. Instruksi lebih dari 16 huruf memakai font lebih kecil, dan pil ETA hilang kalau tidak ada ETA. Semua isi layar ditata di dalam lingkaran yang muat di layar (panah di dalam cakram bulat), supaya jam bulat seperti Watch GT tidak memotong sudutnya. `TRACK` di `index.js` harus sama dengan lebar `.track` di `index.css`.
