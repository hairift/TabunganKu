package com.hairift.tabunganku.data.lokal;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.LruCache;
import android.widget.ImageView;

import androidx.exifinterface.media.ExifInterface;

import com.hairift.tabunganku.R;
import com.hairift.tabunganku.util.KonstantaAplikasi;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Mengelola salinan foto motivasi di penyimpanan internal beserta pemuatan bitmap yang aman memori.
 */
public final class PenyimpanFoto {

    private static final int DIMENSI_MAKSIMAL_SIMPAN = 1280;
    private static final int KUALITAS_JPEG = 88;
    private static final ExecutorService PELAKSANA_GAMBAR = Executors.newFixedThreadPool(2);
    private static final Handler HANDLER_UTAMA = new Handler(Looper.getMainLooper());
    private static final LruCache<String, Bitmap> TEMBOLOK_BITMAP = buatTembolokBitmap();

    private PenyimpanFoto() {
    }

    private static LruCache<String, Bitmap> buatTembolokBitmap() {
        int memoriMaksimalKb = (int) (Runtime.getRuntime().maxMemory() / 1024L);
        int ukuranTembolokKb = Math.max(4096, memoriMaksimalKb / 8);
        return new LruCache<String, Bitmap>(ukuranTembolokKb) {
            @Override
            protected int sizeOf(String kunci, Bitmap nilai) {
                return nilai.getByteCount() / 1024;
            }
        };
    }

    /**
     * Menyalin foto pilihan galeri ke penyimpanan internal agar tetap tersedia walaupun URI asal berubah.
     */
    public static String salinDariUri(Context konteks, Uri uriSumber) throws IOException {
        ContentResolver penyediaKonten = konteks.getContentResolver();
        int sudutRotasi = bacaSudutRotasiExif(penyediaKonten, uriSumber);

        BitmapFactory.Options opsiBatas = new BitmapFactory.Options();
        opsiBatas.inJustDecodeBounds = true;
        try (InputStream aliranBatas = penyediaKonten.openInputStream(uriSumber)) {
            if (aliranBatas == null) {
                throw new IOException("Berkas gambar tidak dapat dibuka.");
            }
            BitmapFactory.decodeStream(aliranBatas, null, opsiBatas);
        }

        if (opsiBatas.outWidth <= 0 || opsiBatas.outHeight <= 0) {
            throw new IOException("Format gambar tidak dikenali.");
        }

        BitmapFactory.Options opsiMuat = new BitmapFactory.Options();
        opsiMuat.inSampleSize = hitungFaktorSkala(
            opsiBatas.outWidth,
            opsiBatas.outHeight,
            DIMENSI_MAKSIMAL_SIMPAN
        );

        Bitmap gambarMentah;
        try (InputStream aliranGambar = penyediaKonten.openInputStream(uriSumber)) {
            if (aliranGambar == null) {
                throw new IOException("Aliran gambar terputus.");
            }
            gambarMentah = BitmapFactory.decodeStream(aliranGambar, null, opsiMuat);
        }

        if (gambarMentah == null) {
            throw new IOException("Gambar gagal diurai.");
        }

        Bitmap gambarTerarah = putarJikaPerlu(gambarMentah, sudutRotasi);
        Bitmap gambarAkhir = perkecilProporsional(gambarTerarah, DIMENSI_MAKSIMAL_SIMPAN);

        File direktoriFoto = ambilDirektoriFoto(konteks);
        String namaBerkas = "foto_" + UUID.randomUUID().toString().replace("-", "") + ".jpg";
        File berkasTujuan = new File(direktoriFoto, namaBerkas);
        try (FileOutputStream keluaran = new FileOutputStream(berkasTujuan)) {
            gambarAkhir.compress(Bitmap.CompressFormat.JPEG, KUALITAS_JPEG, keluaran);
        }

        if (gambarAkhir != gambarTerarah) {
            gambarAkhir.recycle();
        }
        if (gambarTerarah != gambarMentah) {
            gambarTerarah.recycle();
        }
        gambarMentah.recycle();

        return berkasTujuan.getAbsolutePath();
    }

    /**
     * Memuat bitmap dari penyimpanan lokal secara asinkron ke ImageView dengan penanda tag anti-tertukar.
     */
    public static void muatKeImageView(
        ImageView targetTampilan,
        String jalurBerkas,
        int lebarTargetPx
    ) {
        if (jalurBerkas == null || jalurBerkas.trim().isEmpty()) {
            targetTampilan.setTag(null);
            targetTampilan.setImageResource(R.drawable.ic_foto_kosong);
            return;
        }

        File berkas = new File(jalurBerkas);
        if (!berkas.exists()) {
            targetTampilan.setTag(null);
            targetTampilan.setImageResource(R.drawable.ic_foto_kosong);
            return;
        }

        String kunciTembolok = jalurBerkas + "#" + lebarTargetPx;
        Bitmap dariTembolok = TEMBOLOK_BITMAP.get(kunciTembolok);
        if (dariTembolok != null && !dariTembolok.isRecycled()) {
            targetTampilan.setTag(kunciTembolok);
            targetTampilan.setImageBitmap(dariTembolok);
            return;
        }

        targetTampilan.setTag(kunciTembolok);
        targetTampilan.setImageResource(R.drawable.ic_foto_kosong);

        PELAKSANA_GAMBAR.execute(() -> {
            Bitmap hasil = muatBitmapTerskala(jalurBerkas, Math.max(400, lebarTargetPx));
            if (hasil != null) {
                TEMBOLOK_BITMAP.put(kunciTembolok, hasil);
            }
            HANDLER_UTAMA.post(() -> {
                Object tagSaatIni = targetTampilan.getTag();
                if (kunciTembolok.equals(tagSaatIni) && hasil != null) {
                    targetTampilan.setImageBitmap(hasil);
                }
            });
        });
    }

    public static void hapusBerkasFoto(String jalurBerkas) {
        if (jalurBerkas == null || jalurBerkas.trim().isEmpty()) {
            return;
        }
        File berkas = new File(jalurBerkas);
        if (berkas.exists() && !berkas.delete()) {
            // Penghapusan foto yatim bersifat best-effort agar tidak menggagalkan operasi utama.
            berkas.deleteOnExit();
        }
    }

    public static File ambilDirektoriFoto(Context konteks) {
        File direktori = new File(konteks.getFilesDir(), KonstantaAplikasi.FOLDER_FOTO);
        if (!direktori.exists()) {
            direktori.mkdirs();
        }
        return direktori;
    }

    private static Bitmap muatBitmapTerskala(String jalurBerkas, int dimensiTarget) {
        BitmapFactory.Options opsiBatas = new BitmapFactory.Options();
        opsiBatas.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(jalurBerkas, opsiBatas);
        if (opsiBatas.outWidth <= 0 || opsiBatas.outHeight <= 0) {
            return null;
        }
        BitmapFactory.Options opsiMuat = new BitmapFactory.Options();
        opsiMuat.inSampleSize = hitungFaktorSkala(opsiBatas.outWidth, opsiBatas.outHeight, dimensiTarget);
        return BitmapFactory.decodeFile(jalurBerkas, opsiMuat);
    }

    private static int bacaSudutRotasiExif(ContentResolver penyedia, Uri uriSumber) {
        try (InputStream aliran = penyedia.openInputStream(uriSumber)) {
            if (aliran == null) {
                return 0;
            }
            ExifInterface exif = new ExifInterface(aliran);
            int orientasi = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            );
            switch (orientasi) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    return 90;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    return 180;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    return 270;
                default:
                    return 0;
            }
        } catch (IOException galat) {
            return 0;
        }
    }

    private static Bitmap putarJikaPerlu(Bitmap sumber, int sudut) {
        if (sudut == 0) {
            return sumber;
        }
        Matrix matriks = new Matrix();
        matriks.postRotate(sudut);
        return Bitmap.createBitmap(sumber, 0, 0, sumber.getWidth(), sumber.getHeight(), matriks, true);
    }

    private static Bitmap perkecilProporsional(Bitmap sumber, int batasMaks) {
        int sisiTerpanjang = Math.max(sumber.getWidth(), sumber.getHeight());
        if (sisiTerpanjang <= batasMaks) {
            return sumber;
        }
        float rasio = (float) batasMaks / (float) sisiTerpanjang;
        int lebarBaru = Math.max(1, Math.round(sumber.getWidth() * rasio));
        int tinggiBaru = Math.max(1, Math.round(sumber.getHeight() * rasio));
        return Bitmap.createScaledBitmap(sumber, lebarBaru, tinggiBaru, true);
    }

    private static int hitungFaktorSkala(int lebarAsli, int tinggiAsli, int batasMaks) {
        int faktor = 1;
        while ((lebarAsli / faktor) > batasMaks * 2 || (tinggiAsli / faktor) > batasMaks * 2) {
            faktor *= 2;
        }
        return faktor;
    }
}
