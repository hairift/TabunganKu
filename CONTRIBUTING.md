# Panduan Kontribusi TabunganKu

Terima kasih telah tertarik mempelajari dan ikut mengembangkan **TabunganKu**. Sebelum mengirim perubahan, harap perhatikan aturan kerja proyek berikut.

## Kaidah Kode dan Penamaan

1. **Bahasa Indonesia Konsisten:** Nama kelas, metode, variabel, parameter, sumber daya XML, komentar kode, dan dokumentasi wajib menggunakan Bahasa Indonesia yang jelas dan alami (kecuali kata kunci bahasa Java serta metode turunan kerangka kerja Android).
2. **Kode Bersih (Clean Code):**
   - Pecah logika besar menjadi metode kecil dengan satu tanggung jawab jelas.
   - Hindari duplikasi kode; manfaatkan utilitas dan komponen bersama.
   - Tulis komentar hanya untuk menjelaskan alasan atau batasan non-sepele (*mengapa*), bukan mengulang isi baris kode (*apa*).
3. **Standar Antarmuka:**
   - Ikuti palet warna dan skala sudut pada [DESIGN.md](DESIGN.md).
   - Pastikan setiap pasangan teks dan latar belakang memenuhi standar kontras WCAG AA (minimal 4.5:1 untuk teks normal dan 3.0:1 untuk batas komponen non-teks).
   - Pertahankan ukuran area sentuh minimal `48dp x 48dp` pada setiap elemen interaktif.

## Pengujian Lokal Sebelum Mengirim Pull Request

Jalankan pengujian unit dan kompilasi proyek:

```bash
./gradlew testDebugUnitTest assembleDebug
```

## Ketentuan Lisensi

Seluruh kontribusi tunduk pada lisensi non-komersial terbuka proyek di berkas [LICENSE](LICENSE). Kontributor dan pengguna diizinkan memakai, memodifikasi, dan membagikan secara gratis, tetapi dilarang memperjualbelikan aplikasi maupun turunannya.
