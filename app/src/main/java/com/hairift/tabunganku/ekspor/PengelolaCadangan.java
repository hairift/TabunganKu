package com.hairift.tabunganku.ekspor;

import android.content.Context;
import android.net.Uri;

import com.hairift.tabunganku.data.dao.TargetTabunganDao;
import com.hairift.tabunganku.data.dao.TransaksiTabunganDao;
import com.hairift.tabunganku.data.lokal.BasisDataTabungan;
import com.hairift.tabunganku.data.lokal.PenyimpanFoto;
import com.hairift.tabunganku.data.model.TargetTabungan;
import com.hairift.tabunganku.data.model.TransaksiTabungan;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Mencadangkan dan memulihkan seluruh basis data target, transaksi, serta foto motivasi dalam arsip ZIP.
 */
public final class PengelolaCadangan {

    private static final String NAMA_JSON_CADANGAN = "cadangan_tabunganku.json";
    private static final String AWALAN_FOLDER_FOTO = "foto/";

    private PengelolaCadangan() {
    }

    /**
     * Mengekspor seluruh data basis data dan berkas foto ke URI ZIP tujuan.
     */
    public static int buatCadanganKeUri(Context konteks, Uri uriTujuan)
        throws IOException, JSONException {
        BasisDataTabungan basisData = BasisDataTabungan.ambilInstansi(konteks);
        List<TargetTabungan> daftarTarget = basisData.targetTabunganDao().ambilSemuaTargetSinkron();
        List<TransaksiTabungan> daftarTransaksi =
            basisData.transaksiTabunganDao().ambilSemuaTransaksiMentahSinkron();

        JSONObject akar = new JSONObject();
        akar.put("versi_skema", 1);
        akar.put("dibuat_pada_epoch", System.currentTimeMillis());

        JSONArray arrayTarget = new JSONArray();
        Map<String, File> fotoUntukDisalin = new HashMap<>();

        for (TargetTabungan t : daftarTarget) {
            JSONObject obj = new JSONObject();
            obj.put("id_lama", t.id);
            obj.put("nama_target", t.namaTarget);
            obj.put("kategori", t.kategori);
            obj.put("nominal_target", t.nominalTarget);
            obj.put("saldo_terkumpul", t.saldoTerkumpul);
            obj.put("catatan_motivasi", t.catatanMotivasi != null ? t.catatanMotivasi : "");
            obj.put("tanggal_target_epoch", t.tanggalTargetEpoch);
            obj.put("diarsipkan", t.diarsipkan);
            obj.put("dibuat_pada_epoch", t.dibuatPadaEpoch);
            obj.put("diperbarui_pada_epoch", t.diperbaruiPadaEpoch);

            String namaFotoDiZip = "";
            if (t.pathFotoMotivasi != null && !t.pathFotoMotivasi.isEmpty()) {
                File berkasFoto = new File(t.pathFotoMotivasi);
                if (berkasFoto.exists()) {
                    namaFotoDiZip = berkasFoto.getName();
                    fotoUntukDisalin.put(namaFotoDiZip, berkasFoto);
                }
            }
            obj.put("nama_foto", namaFotoDiZip);
            arrayTarget.put(obj);
        }
        akar.put("target", arrayTarget);

        JSONArray arrayTransaksi = new JSONArray();
        for (TransaksiTabungan tr : daftarTransaksi) {
            JSONObject obj = new JSONObject();
            obj.put("id_target_lama", tr.idTarget);
            obj.put("jenis", tr.jenis);
            obj.put("nominal", tr.nominal);
            obj.put("catatan", tr.catatan != null ? tr.catatan : "");
            obj.put("tanggal_transaksi_epoch", tr.tanggalTransaksiEpoch);
            obj.put("dibuat_pada_epoch", tr.dibuatPadaEpoch);
            arrayTransaksi.put(obj);
        }
        akar.put("transaksi", arrayTransaksi);

        try (OutputStream keluaran = konteks.getContentResolver().openOutputStream(uriTujuan)) {
            if (keluaran == null) {
                throw new IOException("Berkas tujuan cadangan tidak dapat dibuka.");
            }
            try (ZipOutputStream arsip = new ZipOutputStream(keluaran)) {
                arsip.putNextEntry(new ZipEntry(NAMA_JSON_CADANGAN));
                arsip.write(akar.toString(2).getBytes(StandardCharsets.UTF_8));
                arsip.closeEntry();

                for (Map.Entry<String, File> entri : fotoUntukDisalin.entrySet()) {
                    arsip.putNextEntry(new ZipEntry(AWALAN_FOLDER_FOTO + entri.getKey()));
                    salinStream(new FileInputStream(entri.getValue()), arsip, true);
                    arsip.closeEntry();
                }
                arsip.finish();
            }
        }
        return daftarTarget.size();
    }

    /**
     * Memulihkan seluruh target, transaksi, dan foto dari berkas arsip cadangan ZIP.
     */
    public static int pulihkanDariUri(Context konteks, Uri uriSumber)
        throws IOException, JSONException {
        String teksJson = null;
        File direktoriFoto = PenyimpanFoto.ambilDirektoriFoto(konteks);
        Map<String, String> petaFotoPulih = new HashMap<>();

        try (InputStream masukan = konteks.getContentResolver().openInputStream(uriSumber)) {
            if (masukan == null) {
                throw new IOException("Berkas cadangan tidak dapat dibaca.");
            }
            try (ZipInputStream arsip = new ZipInputStream(masukan)) {
                ZipEntry entri;
                while ((entri = arsip.getNextEntry()) != null) {
                    if (entri.isDirectory()) {
                        arsip.closeEntry();
                        continue;
                    }
                    String namaEntri = entri.getName();
                    if (NAMA_JSON_CADANGAN.equals(namaEntri)) {
                        teksJson = bacaSeluruhTeks(arsip);
                    } else if (namaEntri.startsWith(AWALAN_FOLDER_FOTO)) {
                        String namaDasar = new File(namaEntri).getName();
                        if (!namaDasar.isEmpty()) {
                            String namaBaru = "foto_pulih_"
                                + UUID.randomUUID().toString().replace("-", "") + ".jpg";
                            File berkasTujuan = new File(direktoriFoto, namaBaru);
                            try (FileOutputStream keluaranFoto = new FileOutputStream(berkasTujuan)) {
                                salinStream(arsip, keluaranFoto, false);
                            }
                            petaFotoPulih.put(namaDasar, berkasTujuan.getAbsolutePath());
                        }
                    }
                    arsip.closeEntry();
                }
            }
        }

        if (teksJson == null) {
            throw new IOException("Berkas cadangan tidak berisi data TabunganKu yang valid.");
        }

        JSONObject akar = new JSONObject(teksJson);
        JSONArray arrayTarget = akar.getJSONArray("target");
        JSONArray arrayTransaksi = akar.getJSONArray("transaksi");

        BasisDataTabungan basisData = BasisDataTabungan.ambilInstansi(konteks);
        TargetTabunganDao targetDao = basisData.targetTabunganDao();
        TransaksiTabunganDao transaksiDao = basisData.transaksiTabunganDao();

        basisData.runInTransaction(() -> {
            transaksiDao.hapusSemua();
            targetDao.hapusSemua();

            Map<Long, Long> petaIdBaru = new HashMap<>();
            for (int i = 0; i < arrayTarget.length(); i++) {
                JSONObject obj = arrayTarget.getJSONObject(i);
                long idLama = obj.getLong("id_lama");
                String namaFoto = obj.optString("nama_foto", "");
                String jalurFotoPulih = namaFoto.isEmpty() ? null : petaFotoPulih.get(namaFoto);
                String catatan = obj.optString("catatan_motivasi", "");

                TargetTabungan baru = new TargetTabungan(
                    obj.getString("nama_target"),
                    obj.getString("kategori"),
                    obj.getLong("nominal_target"),
                    obj.getLong("saldo_terkumpul"),
                    jalurFotoPulih,
                    catatan.isEmpty() ? null : catatan,
                    obj.optLong("tanggal_target_epoch", 0L),
                    obj.optLong("dibuat_pada_epoch", System.currentTimeMillis())
                );
                baru.diarsipkan = obj.optBoolean("diarsipkan", false);
                baru.diperbaruiPadaEpoch = obj.optLong("diperbarui_pada_epoch", baru.dibuatPadaEpoch);

                long idBaru = targetDao.sisipkan(baru);
                petaIdBaru.put(idLama, idBaru);
            }

            for (int i = 0; i < arrayTransaksi.length(); i++) {
                JSONObject obj = arrayTransaksi.getJSONObject(i);
                long idTargetLama = obj.getLong("id_target_lama");
                Long idTargetBaru = petaIdBaru.get(idTargetLama);
                if (idTargetBaru == null) {
                    continue;
                }
                String catatan = obj.optString("catatan", "");
                TransaksiTabungan tr = new TransaksiTabungan(
                    idTargetBaru,
                    obj.getString("jenis"),
                    obj.getLong("nominal"),
                    catatan.isEmpty() ? null : catatan,
                    obj.getLong("tanggal_transaksi_epoch"),
                    obj.optLong("dibuat_pada_epoch", System.currentTimeMillis())
                );
                transaksiDao.sisipkan(tr);
            }
            return null;
        });

        return arrayTarget.length();
    }

    private static void salinStream(InputStream masukan, OutputStream keluaran, boolean tutupMasukan)
        throws IOException {
        byte[] penyangga = new byte[8192];
        int terbaca;
        while ((terbaca = masukan.read(penyangga)) != -1) {
            keluaran.write(penyangga, 0, terbaca);
        }
        if (tutupMasukan) {
            masukan.close();
        }
    }

    private static String bacaSeluruhTeks(InputStream masukan) throws IOException {
        ByteArrayOutputStream penyangga = new ByteArrayOutputStream();
        byte[] blok = new byte[4096];
        int terbaca;
        while ((terbaca = masukan.read(blok)) != -1) {
            penyangga.write(blok, 0, terbaca);
        }
        return penyangga.toString(StandardCharsets.UTF_8.name());
    }
}
