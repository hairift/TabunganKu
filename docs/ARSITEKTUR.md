# Arsitektur Teknis TabunganKu

Dokumen ini menjelaskan struktur proyek, pemisahan tanggung jawab kode, skema basis data lokal, serta cara pemeliharaan aplikasi TabunganKu berbasis Android Java Native.

## Ringkasan Tumpukan Teknologi

- **Bahasa Pemrograman:** Java 17 (kompatibel dengan JDK 17 dan JDK 21)
- **Antarmuka:** Android Views + ViewBinding + Material Components 3 (`1.12.0`)
- **Basis Data Lokal:** SQLite melalui AndroidX Room (`2.6.1`) dengan skema ekspor JSON otomatis di `app/schemas/`
- **Ekspor Berkas:** Pembangun OOXML Excel `.xlsx` murni Java (`ZipOutputStream` + XML terstruktur) dan generator CSV RFC 4180 ber-BOM UTF-8 tanpa ketergantungan pustaka eksternal berbayar atau berat
- **Pengelolaan Gambar:** Pemilih foto sistem Android (`PickVisualMedia`) disertai koreksi rotasi EXIF, penskalaan dimensi maksimal 1280px, penyimpanan lokal privat (`filesDir/foto_target`), dan tembolok memori `LruCache`

## Struktur Paket Kode Sumber

```
com.hairift.tabunganku
├── AplikasiTabunganKu.java            # Menerapkan preferensi tema sejak awal proses
├── data
│   ├── dao
│   │   ├── TargetTabunganDao.java     # Kueri Room untuk tabel target_tabungan
│   │   └── TransaksiTabunganDao.java  # Kueri mutasi dan gabungan target
│   ├── lokal
│   │   ├── BasisDataTabungan.java     # Singleton RoomDatabase SQLite lokal
│   │   ├── PengelolaPreferensi.java   # Pengaturan tema terang/gelap dan pengingat
│   │   └── PenyimpanFoto.java         # Salinan foto aman memori dan rotasi EXIF
│   ├── model
│   │   ├── RingkasanKeseluruhan.java
│   │   ├── TargetTabungan.java
│   │   ├── TransaksiDenganTarget.java
│   │   └── TransaksiTabungan.java
│   └── repositori
│       └── RepositoriTabungan.java    # Transaksi atomik saldo target & mutasi
├── ekspor
│   ├── PembuatCsv.java                # Generator CSV ber-BOM UTF-8 + anti injeksi rumus
│   ├── PembuatExcelXlsx.java          # Generator OOXML .xlsx 2 lembar + rumus SUM/SUMIF
│   ├── PengelolaCadangan.java         # Arsip cadangan dan pemulihan ZIP + foto
│   └── PengelolaEkspor.java           # Penyimpan ke Downloads, SAF, dan FileProvider
├── ui
│   ├── beranda
│   │   ├── AdaptorKartuTarget.java
│   │   └── FragmenBeranda.java
│   ├── laporan
│   │   ├── FragmenLaporan.java
│   │   ├── GrafikBatangBulananView.java
│   │   └── GrafikDonutTargetView.java
│   ├── pengaturan
│   │   └── FragmenPengaturan.java
│   ├── riwayat
│   │   └── FragmenRiwayat.java
│   ├── target
│   │   ├── AdaptorRiwayatTransaksi.java
│   │   ├── AktivitasDetailTarget.java
│   │   ├── AktivitasPratinjauFoto.java
│   │   ├── AktivitasTambahUbahTarget.java
│   │   └── LembarTransaksi.java
│   └── utama
│       └── AktivitasUtama.java
└── util
    ├── FormatMataUang.java
    ├── FormatTanggal.java
    ├── HasilOperasi.java
    ├── KalkulatorTarget.java
    ├── KonstantaAplikasi.java
    ├── PemformatInputRupiah.java
    ├── PenerimaPengingat.java
    └── PengelolaPengingat.java
```

## Prinsip Konsistensi Data

Seluruh perubahan saldo berjalan melalui `RepositoriTabungan` di dalam transaksi Room (`runInTransaction`):

1. **Pembuatan Target dengan Saldo Awal:** Bila pengguna mengisi saldo awal di atas nol, sistem menyisipkan baris `TargetTabungan` sekaligus satu baris `TransaksiTabungan` bertipe `SETOR` berlabel `Saldo awal target`.
2. **Setor dan Tarik Dana:** Penarikan divalidasi tidak boleh melebihi `saldo_terkumpul` saat itu. Nilai mutasi dan pembaruan saldo ditulis serentak dalam satu transaksi atomik.
3. **Penghapusan Transaksi:** Menghapus satu catatan setoran memeriksa terlebih dahulu agar saldo target sesudah penghapusan tidak menjadi minus. Bila valid, saldo otomatis disesuaikan ulang.

## Rancangan Berkas Excel (.xlsx)

`PembuatExcelXlsx` menghasilkan arsip `.xlsx` valid secara langsung dalam kode Java tanpa ketergantungan pustaka pihak ketiga berukuran besar, dengan dua lembar kerja:

1. **`Ringkasan Target`:**
   - Banner identitas `TabunganKu` dan informasi waktu pencetakan.
   - Baris KPI ringkas (Total Dana Terkumpul, Total Target, Sisa, Jumlah Tercapai).
   - Header terkunci (*freeze pane* pada baris 7) supaya tetap terlihat saat menggulir.
   - Format angka Rupiah asli Excel (`"Rp "#,##0`), persentase (`0.0%`), pewarnaan status jelas (`Tercapai`, `Berjalan`, `Diarsipkan`), serta rumus Excel asli `=SUM(...)` pada baris total.
2. **`Riwayat Transaksi`:**
   - Mutasi kronologis seluruh setoran dan penarikan dengan warna pembeda label, header terkunci, serta baris rekap `=SUMIF(...)` untuk Total Setor, Total Tarik, dan Arus Bersih.
