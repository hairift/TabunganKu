package com.hairift.tabunganku.ekspor;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;

import androidx.core.content.FileProvider;

import com.hairift.tabunganku.data.model.TargetTabungan;
import com.hairift.tabunganku.data.model.TransaksiDenganTarget;
import com.hairift.tabunganku.util.FormatTanggal;
import com.hairift.tabunganku.util.KonstantaAplikasi;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

/**
 * Menangani pembuatan berkas Excel/CSV ke tembolok FileProvider sekaligus penyimpanan ke folder Download.
 */
public final class PengelolaEkspor {

    public static final String MIME_EXCEL =
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    public static final String MIME_CSV = "text/csv";

    public static final class HasilEkspor {
        public final File berkasLokal;
        public final Uri uriBagikan;
        public final String namaBerkas;
        public final String tipeMime;
        public final String lokasiUnduhanPublik;

        public HasilEkspor(
            File berkasLokal,
            Uri uriBagikan,
            String namaBerkas,
            String tipeMime,
            String lokasiUnduhanPublik
        ) {
            this.berkasLokal = berkasLokal;
            this.uriBagikan = uriBagikan;
            this.namaBerkas = namaBerkas;
            this.tipeMime = tipeMime;
            this.lokasiUnduhanPublik = lokasiUnduhanPublik;
        }
    }

    private PengelolaEkspor() {
    }

    public static HasilEkspor eksporLaporan(
        Context konteks,
        boolean formatExcel,
        String judulCakupan,
        String awalanNama,
        List<TargetTabungan> daftarTarget,
        List<TransaksiDenganTarget> daftarTransaksi
    ) throws IOException {
        long waktuSekarang = System.currentTimeMillis();
        String ekstensi = formatExcel ? ".xlsx" : ".csv";
        String tipeMime = formatExcel ? MIME_EXCEL : MIME_CSV;
        String namaBerkas = awalanNama + "-" + FormatTanggal.kePenandaBerkas(waktuSekarang) + ekstensi;

        File folderSementara = new File(konteks.getCacheDir(), KonstantaAplikasi.FOLDER_EKSPOR);
        if (!folderSementara.exists()) {
            folderSementara.mkdirs();
        }
        File berkasLokal = new File(folderSementara, namaBerkas);
        try (FileOutputStream keluaran = new FileOutputStream(berkasLokal)) {
            if (formatExcel) {
                PembuatExcelXlsx.tulisBukuKerja(
                    keluaran,
                    judulCakupan,
                    daftarTarget,
                    daftarTransaksi,
                    waktuSekarang
                );
            } else {
                PembuatCsv.tulisLaporan(
                    keluaran,
                    judulCakupan,
                    daftarTarget,
                    daftarTransaksi,
                    waktuSekarang
                );
            }
        }

        Uri uriBagikan = FileProvider.getUriForFile(
            konteks,
            konteks.getPackageName() + ".penyedia_berkas",
            berkasLokal
        );
        String lokasiPublik = simpanKeFolderUnduhan(konteks, berkasLokal, namaBerkas, tipeMime);
        return new HasilEkspor(berkasLokal, uriBagikan, namaBerkas, tipeMime, lokasiPublik);
    }

    /**
     * Menyalin berkas hasil ekspor ke URI tujuan yang dipilih pengguna lewat Storage Access Framework.
     */
    public static void salinBerkasKeUri(
        Context konteks,
        File berkasSumber,
        Uri uriTujuan
    ) throws IOException {
        try (FileInputStream masukan = new FileInputStream(berkasSumber);
             OutputStream keluaran = konteks.getContentResolver().openOutputStream(uriTujuan)) {
            if (keluaran == null) {
                throw new IOException("Lokasi penyimpanan tujuan tidak dapat diakses.");
            }
            byte[] penyangga = new byte[8192];
            int terbaca;
            while ((terbaca = masukan.read(penyangga)) != -1) {
                keluaran.write(penyangga, 0, terbaca);
            }
            keluaran.flush();
        }
    }

    private static String simpanKeFolderUnduhan(
        Context konteks,
        File berkasSumber,
        String namaBerkas,
        String tipeMime
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContentResolver penyedia = konteks.getContentResolver();
            ContentValues nilai = new ContentValues();
            nilai.put(MediaStore.Downloads.DISPLAY_NAME, namaBerkas);
            nilai.put(MediaStore.Downloads.MIME_TYPE, tipeMime);
            nilai.put(
                MediaStore.Downloads.RELATIVE_PATH,
                Environment.DIRECTORY_DOWNLOADS + "/" + KonstantaAplikasi.FOLDER_UNDUHAN_PUBLIK
            );
            Uri uriMedia = penyedia.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, nilai);
            if (uriMedia != null) {
                try (FileInputStream masukan = new FileInputStream(berkasSumber);
                     OutputStream keluaran = penyedia.openOutputStream(uriMedia)) {
                    if (keluaran != null) {
                        byte[] penyangga = new byte[8192];
                        int terbaca;
                        while ((terbaca = masukan.read(penyangga)) != -1) {
                            keluaran.write(penyangga, 0, terbaca);
                        }
                        keluaran.flush();
                        return "Downloads/TabunganKu/" + namaBerkas;
                    }
                } catch (IOException galat) {
                    penyedia.delete(uriMedia, null, null);
                }
            }
        }
        return "Penyimpanan aplikasi (" + namaBerkas + "). Gunakan Simpan ke Lokasi Lain atau Bagikan.";
    }
}
