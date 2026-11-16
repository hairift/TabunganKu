package com.hairift.tabunganku;

import com.hairift.tabunganku.data.model.TargetTabungan;
import com.hairift.tabunganku.data.model.TransaksiDenganTarget;
import com.hairift.tabunganku.ekspor.PembuatCsv;
import com.hairift.tabunganku.util.KonstantaAplikasi;

import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Pengujian unit untuk ekspor laporan CSV dan pencegahan injeksi rumus.
 */
public class PembuatCsvTest {

    @Test
    public void tulisLaporan_menyertakanBomUtf8DanMelindungiInjeksiRumus() throws Exception {
        TargetTabungan target = new TargetTabungan(
            "=Rumah, \"Impian\"",
            "Rumah & Properti",
            100_000_000L,
            25_000_000L,
            null,
            "Untuk keluarga kecil",
            0L,
            1_700_000_000_000L
        );

        TransaksiDenganTarget tr = new TransaksiDenganTarget();
        tr.id = 1L;
        tr.idTarget = 1L;
        tr.jenis = KonstantaAplikasi.JENIS_SETOR;
        tr.nominal = 25_000_000L;
        tr.catatan = "+Bonus akhir tahun";
        tr.tanggalTransaksiEpoch = 1_700_000_000_000L;
        tr.namaTarget = target.namaTarget;
        tr.kategori = target.kategori;

        ByteArrayOutputStream keluaran = new ByteArrayOutputStream();
        PembuatCsv.tulisLaporan(
            keluaran,
            "Semua Target",
            Collections.singletonList(target),
            Collections.singletonList(tr),
            1_700_000_000_000L
        );

        byte[] hasil = keluaran.toByteArray();
        assertEquals((byte) 0xEF, hasil[0]);
        assertEquals((byte) 0xBB, hasil[1]);
        assertEquals((byte) 0xBF, hasil[2]);

        String isiTeks = new String(hasil, 3, hasil.length - 3, StandardCharsets.UTF_8);
        assertTrue(isiTeks.contains("'=Rumah, \"\"Impian\"\""));
        assertTrue(isiTeks.contains("'+Bonus akhir tahun"));
        assertTrue(isiTeks.contains("RINGKASAN TARGET"));
        assertTrue(isiTeks.contains("RIWAYAT TRANSAKSI"));
        assertTrue(isiTeks.contains("Tanpa tenggat"));
    }
}
