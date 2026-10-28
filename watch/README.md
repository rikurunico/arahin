# Arahin watch app (Huawei Lite Wearable, JS)

Layar navigasi di jam: instruksi, panah besar, jarak, dan ETA. Data datang dari app HP lewat Wear Engine.

Belum diuji di jam asli. Source ini dimasukkan ke project DevEco Studio buatanmu sendiri, karena `config.json`, signing, dan `wearengine.js` dibuat/diunduh di sana.

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

JSON kecil: `{"d":"R","m":"116 m","n":116,"e":"4.8 km · 10:50 · 12 min"}`. `d` = `S` lurus, `L` kiri, `R` kanan, `U` putar balik; `n` = jarak dalam meter (-1 kalau tidak ada).

Getar: kiri 1x pendek, kanan 2x pendek, putar balik 1x panjang, lurus senyap. Getar saat melewati 500 m, 200 m, dan 50 m menuju belokan, dan saat arah berganti kalau jaraknya sudah ≤ 500 m (belokan yang baru muncul dari jauh tidak bergetar). Bar di bawah jarak terisi mulai 300 m sebelum belokan. `TRACK` di `index.js` harus sama dengan lebar `.track` di `index.css`.
