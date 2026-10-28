# Arahin watch app (Huawei Lite Wearable, JS)

Layar navigasi di jam: instruksi, panah besar, jarak, dan ETA, dikirim dari app HP lewat Wear Engine.

Belum dicoba di jam asli. Masukkan source ini ke project DevEco Studio kamu sendiri, karena `config.json`, signing, dan `wearengine.js` dibuat atau diunduh di sana.

## Pasang

1. DevEco Studio: buat project **Lite Wearable** (JS), bundle name `com.arahin.watch`, Compile SDK 10.
2. Unduh `wearengine.js` ([Wear Engine SDK, Lite Wearable](https://developer.huawei.com/consumer/en/doc/connectivity-guides/integrating-fitnesstwatch-sdk-0000001052859174)) ke `entry/src/main/js/default/pages/`.
3. Salin isi folder `pages/index/` di sini ke `entry/src/main/js/default/pages/index/`, lalu salin folder `common/images/` (empat `arrow-*.png`) ke `entry/src/main/js/default/common/`.
4. Di `config.json` modul, tambahkan (fingerprint = SHA-256 sertifikat signing app HP):
   ```json
   "metaData": { "customizeData": [{ "name": "supportLists", "value": "com.arahin:<PHONE_FINGERPRINT>", "extra": "" }] }
   ```
5. Isi `PHONE_FINGERPRINT` di `index.js` dengan nilai yang sama.
6. Daftarkan jam sebagai debug device di AppGallery Connect, tanda tangani, lalu pasang lewat Huawei Health + DevEco Assistant.

## Format pesan dari HP

JSON kecil: `{"d":"R","t":"Belok kanan","m":"116 m","n":116,"x":0,"e":"4.8 km · 10:50 · 12 min"}`.

- `d` = jenis manuver: `S` lurus/lanjut/awal rute, `L` `R` belok, `l` `r` serong/tetap di sisi/percabangan/ramp, `h` `j` belok tajam, `U` putar balik, `O` bundaran, `D` tujuan, `M` gabung jalur, `E` ambil jalan keluar (sisi tidak diketahui), `Q` keluar dari bundaran, `?` teks yang tidak dikenali (ditampilkan apa adanya).
- `t` = instruksi singkat berbahasa Indonesia yang ditampilkan di jam, `m` = jarak, `n` = jarak dalam meter (-1 kalau tidak ada), `x` = nomor pintu keluar bundaran (0 kalau tidak ada), `e` = ETA.
- Ikon: `common/images/arrow-{s,l,r,sl,sr,hl,hr,u,o,o1,o2,o3,d}.png`. Ikon bundaran `o1` `o2` `o3` menggambar bundaran 4 lengan dari bawah: pintu yang harus diambil terang dengan panah, pintu lain redup. Cuma skema, bukan bentuk bundaran aslinya. `DRIVE_LEFT` di `index.js` (default `true`, lalu lintas kiri) menentukan pintu 1 di kiri atau kanan.

Getar: kiri (termasuk serong dan tajam) 1x pendek, kanan 2x pendek, putar balik 1x panjang, bundaran 1x panjang + 1x pendek, tujuan 2x panjang, lurus, gabung, dan jalan keluar senyap. Getar saat melewati 500 m, 200 m, dan 50 m menuju belokan, dan saat arah berganti kalau jaraknya sudah ≤ 500 m (belokan yang baru muncul dari jauh tidak bergetar). Bar di bawah jarak terisi mulai 300 m sebelum belokan. `TRACK` di `index.js` harus sama dengan lebar `.track` di `index.css`.
