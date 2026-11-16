# Arah Desain TabunganKu (DESIGN.md)

Dokumen ini menjadi pedoman tunggal identitas visual, tipografi, palet warna, tata letak, dan perilaku interaksi aplikasi mobile **TabunganKu**.

## Pembacaan Desain (Design Read)

> Membaca produk ini sebagai: **jurnal tabungan pribadi seluler (mobile personal savings book)** untuk **penabung harian di Indonesia yang mengejar target nyata**, dengan bahasa visual **buku tabungan kertas hangat dan arsip kerajinan (warm archival passbook)**, dial **ENERGY 2 / RHYTHM 2 / MOTION 1**.

- **ENERGY 2 (Seimbang):** Hangat, ramah, dan mantap. Tidak dingin seperti alat pengembang, tidak berisik dengan efek kilau atau gradien neon.
- **RHYTHM 2 (Konsisten dengan variasi fokus):** Satu kartu ringkasan saldo utama yang kuat di atas, diikuti daftar kartu target dengan foto motivasi sebagai jangkar visual, dan formulir/lembar transaksi yang ringkas.
- **MOTION 1 (Fungsional & tenang):** Transisi progres batang tabungan dan perpindahan layar yang halus serta menghormati preferensi pengurangan animasi sistem; tanpa animasi mengambang atau berdenyut tanpa henti.

## Prinsip Inti Antislop

1. **Satu titik fokus per layar:** Di Beranda fokusnya adalah posisi tabungan saat ini; di Detail Target fokusnya adalah foto impian dan progres nominal; di Laporan fokusnya adalah pertanyaan keuangan bulanan dan ekspor berkas rapi.
2. **Satu aksen terukur:** Warna terakota panggang dipakai hemat hanya untuk tombol aksi utama, indikator pencapaian hangat, dan bulan aktif pada grafik.
3. **Motif identitas:** Sudut lengkung buku tabungan (radius `16dp` pada kartu, `12dp` pada input dan tombol utama, `24dp` pada lembar bawah), garis tepi kertas tipis sebagai pembatas elevasi tanpa bayangan mengambang berlebihan, dan angka tabular (`tnum`) agar deret Rupiah sejajar vertikal.

## Palet Warna Terverifikasi WCAG AA

Seluruh pasangan teks dan latar belakang diuji menggunakan rumus rasio kontras WCAG 2.x (`antislop-human/contrast-check.py`). Palet dibatasi pada **2 warna inti (Hijau Pinus Hutan & Kertas Linen Hangat)** serta **1 warna aksen (Terakota Panggang)** beserta warna semantik secukupnya.

### Mode Terang (Kertas Buku Tabungan)

| Token | Nilai Hex | Peran & Alasan | Kontras Terukur |
|---|---|---|---|
| `warna_latar` | `#F7F3EB` | Kertas linen hangat agar nyaman dipandang lama tanpa silau putih polos | dasar |
| `warna_permukaan` | `#FFFDF9` | Kartu dan formulir gading alami agar terpisah lembut dari latar | dasar |
| `warna_permukaan_sekunder` | `#EFE8DA` | Latar chip, kotak ringkasan, dan bilah jalur progres | dasar |
| `warna_garis_batas` | `#8E8577` | Garis tepi input aktif dan batas kartu interaktif | `3.58:1` vs `#FFFDF9` (PASS non-teks >= 3:1) |
| `warna_garis_halus` | `#E3DACB` | Pembatas non-interaktif antar elemen daftar | dekoratif terstruktur |
| `warna_teks_utama` | `#1F2421` | Tinta arang pinus tua untuk judul dan nominal | `15.51:1` vs `#FFFDF9`, `14.24:1` vs `#F7F3EB` |
| `warna_teks_sekunder` | `#505752` | Teks pendukung, tanggal, dan keterangan kategori | `7.31:1` vs `#FFFDF9`, `6.71:1` vs `#F7F3EB` |
| `warna_primer` | `#1E4D3B` | Hijau pinus hutan melambangkan pertumbuhan stabil tanpa kesan neon | `9.48:1` vs `#FFFDF9` |
| `warna_teks_di_primer` | `#FAF7F2` | Teks krem di atas tombol dan tajuk primer | `9.01:1` vs `#1E4D3B` |
| `warna_aksen` | `#A94E2D` | Terakota panggang sebagai aksen tunggal pada aksi utama | `5.42:1` vs `#FFFDF9`, `4.97:1` vs `#F7F3EB` |
| `warna_teks_di_aksen` | `#FFFDF9` | Teks putih gading di atas tombol aksen | `5.42:1` vs `#A94E2D` |
| `warna_setor_latar` / `_teks` | `#E5F0EA` / `#1B4B37` | Penanda setoran masuk (selalu disertai teks dan ikon) | `8.53:1` |
| `warna_tarik_latar` / `_teks` | `#FAEBE4` / `#8D3618` | Penanda penarikan dana (selalu disertai teks dan ikon) | `6.76:1` |
| `warna_galat_latar` / `_teks` | `#FBEAEA` / `#8E2223` | Pesan validasi dan status kesalahan | `7.51:1` |

### Mode Gelap (Hutan Malam)

| Token | Nilai Hex | Peran & Alasan | Kontras Terukur |
|---|---|---|---|
| `warna_latar` | `#141816` | Arang pinus gelap yang tenang di mata pada malam hari | dasar |
| `warna_permukaan` | `#1D2320` | Permukaan kartu utama dengan garis tepi jelas | dasar |
| `warna_permukaan_sekunder` | `#262E2A` | Latar chip, panel ringkasan, dan jalur progres | dasar |
| `warna_garis_batas` | `#747F76` | Batas komponen interaktif pada mode gelap | `3.84:1` vs `#1D2320` (PASS non-teks >= 3:1) |
| `warna_garis_halus` | `#2E3732` | Pembatas halus antar item | `4.46:1` vs `#6CA88C` |
| `warna_teks_utama` | `#F2EDE4` | Teks alabaster hangat agar nyaman dibaca | `15.37:1` vs `#141816`, `13.71:1` vs `#1D2320` |
| `warna_teks_sekunder` | `#B5BEB7` | Keterangan tanggal, kategori, dan sisa target | `8.39:1` vs `#1D2320`, `7.31:1` vs `#262E2A` |
| `warna_primer` | `#6CA88C` | Hijau lumut lembut untuk tombol dan batang grafik mode gelap | `5.80:1` vs `#1D2320` |
| `warna_teks_di_primer` | `#101714` | Teks gelap pekat di atas primer terang | `6.60:1` vs `#6CA88C` |
| `warna_aksen` | `#DE8361` | Terakota lembut sebagai aksen tunggal mode gelap | `5.73:1` vs `#1D2320` |
| `warna_teks_di_aksen` | `#1A110E` | Teks di atas aksen terakota mode gelap | `6.66:1` vs `#DE8361` |
| `warna_setor_latar` / `_teks` | `#1E3328` / `#8AD1AC` | Penanda setoran masuk | `7.56:1` |
| `warna_tarik_latar` / `_teks` | `#362019` / `#F1A78C` | Penanda penarikan dana | `7.73:1` |

## Tipografi

- **Judul tampilan & tajuk target (`Lora` SemiBold / Bold, SIL OFL 1.1):** Memberikan kehangatan jurnal pribadi pada saldo total dan nama impian pengguna, membedakan TabunganKu dari aplikasi keuangan seragam.
- **Teks antarmuka, label, dan angka nominal (`Plus Jakarta Sans` Regular / Medium / SemiBold / Bold, SIL OFL 1.1):** Dirancang oleh tipografer Indonesia (Tokotype), sangat terbaca di layar ponsel ukuran kecil hingga besar, dipadukan dengan `tnum` (tabular numerals) pada nominal uang.

## Logo TabunganKu

- **Bentuk:** Bingkai buku tabungan berkubah bulat berwarna Hijau Pinus (`#1E4D3B`) dengan atap pelindung Terakota Hangat (`#E08663`) yang menaungi koin/benih krem (`#FAF7F2`) dan lengkung celah tabungan.
- **Makna:** Memadukan tiga unsur inti aplikasi, yakni celengan pribadi, impian nyata seperti rumah, dan pertumbuhan dana yang konsisten tanpa hiasan kilau generik.

## Skala Sudut, Elevasi, dan Sentuhan Seluler

- **Target sentuh minimum:** Seluruh tombol, chip filter, ikon aksi, dan item navigasi minimal `48dp x 48dp` (>= 44px R-03) dengan jarak antar kontrol `>= 8dp`.
- **Elevasi selektif (R-12):** Kartu daftar duduk rata dengan garis tipis `1dp` (`#E3DACB` / `#2E3732`) agar layar tidak terasa melayang; hanya tombol aksi utama mengambang atau dialog yang memiliki elevasi lembut (`4dp` - `6dp`).
- **Keadaan data lengkap (R-27):** Setiap daftar dan laporan menyediakan state *Memuat* (indikator + teks), *Kosong* (penjelasan + satu aksi jelas), dan *Galat* (penyebab + tombol coba ulang).
- **Keterbacaan di atas foto (R-25):** Judul kartu dan angka saldo ditempatkan pada panel padat di bawah foto motivasi atau dilindungi lapis gelap (`scrim`) terverifikasi apabila ada label status di pojok foto.
