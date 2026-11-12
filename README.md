# JanSipBill — GitHub Actions + Bluetooth Thermal Printer

## Fitur printer
- Native Android Bluetooth Classic SPP.
- Daftar perangkat Bluetooth yang sudah dipair dengan Android.
- Hubungkan / putuskan koneksi printer.
- Cetak tes, tagihan, dan bukti pembayaran ke printer ESC/POS 58 mm.
- Target perangkat: RPP02N yang mendukung Bluetooth Classic SPP.

## Penting sebelum menggunakan printer
1. Nyalakan RPP02N.
2. Pair dari Settings > Bluetooth Android terlebih dahulu.
3. Instal APK JanSipBill hasil GitHub Actions.
4. Buka JanSipBill > Dashboard > Printer Bluetooth.
5. Pilih RPP02N > Hubungkan > Cetak Tes.

Printer harus mendukung Bluetooth Classic SPP / serial port profile. Jika unit RPP02N Anda menggunakan BLE-only, modul ini tidak cocok dan perlu plugin BLE khusus.

## Firebase
Isi `firebaseConfig` di `www/index.html` sebelum upload. Aktifkan Firebase Authentication Email/Password dan siapkan Firestore Rules yang aman.

## Upload ke GitHub
Buat repository baru `jansipbill`, lalu upload semua isi folder proyek ini ke root repository, termasuk folder tersembunyi `.github`.

Struktur:
```text
.github/workflows/build-apk.yml
plugins/jansipbill-bluetooth-classic/
www/index.html
package.json
capacitor.config.json
README.md
```

## Build APK
1. Buka repository > Actions.
2. Pilih `Build JanSipBill APK`.
3. Klik `Run workflow`.
4. Tunggu Success.
5. Unduh artifact `JanSipBill-debug-apk`, ekstrak, lalu instal `app-debug.apk`.

## Catatan teknis
- Build pertama meng-generate folder Android pada runner GitHub, lalu Capacitor mendaftarkan plugin lokal.
- Printer harus dipair lebih dulu dari pengaturan Android; aplikasi membaca daftar perangkat yang sudah dipasangkan.
- Android 12+ akan meminta izin Bluetooth Connect ketika aplikasi mengakses perangkat.
- Mode aplikasi online tetap memerlukan internet untuk Firebase dan library XLSX CDN.
- APK yang dibuat adalah debug APK untuk pengujian. Untuk distribusi luas, buat signed release APK dan simpan keystore dengan aman.
- Pastikan menguji pada unit printer yang sebenarnya. Nama RPP02N saja tidak menjamin semua revisi perangkat menggunakan profil Bluetooth yang sama.
