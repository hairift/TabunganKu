package com.hairift.tabunganku;

import com.hairift.tabunganku.data.model.TargetTabungan;
import com.hairift.tabunganku.data.model.TransaksiDenganTarget;
import com.hairift.tabunganku.ekspor.PembuatExcelXlsx;
import com.hairift.tabunganku.util.KonstantaAplikasi;

import org.junit.Test;
import org.w3c.dom.Document;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.xml.parsers.DocumentBuilderFactory;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Pengujian unit untuk struktur OOXML Excel (.xlsx), keabsahan XML, lembar kerja, dan rumus jumlah.
 */
public class PembuatExcelXlsxTest {

    @Test
    public void tulisBukuKerja_menghasilkanArsipXlsxValidDenganDuaLembarDanRumusSum() throws Exception {
        TargetTabungan rumah = new TargetTabungan(
            "DP Rumah Minimalis",
            "Rumah & Properti",
            150_000_000L,
            45_000_000L,
            null,
            "Dekat akses stasiun & sekolah",
            1_800_000_000_000L,
            1_700_000_000_000L
        );
        TargetTabungan darurat = new TargetTabungan(
            "Dana Darurat",
            "Dana Darurat",
            20_000_000L,
            20_000_000L,
            null,
            "Cadangan 6 bulan",
            0L,
            1_700_000_000_000L
        );

        TransaksiDenganTarget tr1 = new TransaksiDenganTarget();
        tr1.id = 1L;
        tr1.idTarget = 1L;
        tr1.jenis = KonstantaAplikasi.JENIS_SETOR;
        tr1.nominal = 45_000_000L;
        tr1.catatan = "Tabungan awal & THR";
        tr1.tanggalTransaksiEpoch = 1_700_000_000_000L;
        tr1.namaTarget = rumah.namaTarget;
        tr1.kategori = rumah.kategori;

        TransaksiDenganTarget tr2 = new TransaksiDenganTarget();
        tr2.id = 2L;
        tr2.idTarget = 2L;
        tr2.jenis = KonstantaAplikasi.JENIS_TARIK;
        tr2.nominal = 1_000_000L;
        tr2.catatan = "Biaya perbaikan mendadak";
        tr2.tanggalTransaksiEpoch = 1_700_100_000_000L;
        tr2.namaTarget = darurat.namaTarget;
        tr2.kategori = darurat.kategori;

        ByteArrayOutputStream keluaran = new ByteArrayOutputStream();
        PembuatExcelXlsx.tulisBukuKerja(
            keluaran,
            "Semua Target",
            Arrays.asList(rumah, darurat),
            Arrays.asList(tr1, tr2),
            1_700_200_000_000L
        );

        Map<String, byte[]> entriZip = new HashMap<>();
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(keluaran.toByteArray()))) {
            ZipEntry entri;
            while ((entri = zis.getNextEntry()) != null) {
                ByteArrayOutputStream penyangga = new ByteArrayOutputStream();
                byte[] blok = new byte[4096];
                int terbaca;
                while ((terbaca = zis.read(blok)) != -1) {
                    penyangga.write(blok, 0, terbaca);
                }
                entriZip.put(entri.getName(), penyangga.toByteArray());
            }
        }

        String[] jalurWajib = {
            "[Content_Types].xml",
            "_rels/.rels",
            "xl/workbook.xml",
            "xl/_rels/workbook.xml.rels",
            "xl/styles.xml",
            "xl/sharedStrings.xml",
            "xl/worksheets/sheet1.xml",
            "xl/worksheets/sheet2.xml"
        };

        DocumentBuilderFactory pabrikXml = DocumentBuilderFactory.newInstance();
        pabrikXml.setNamespaceAware(true);
        for (String jalur : jalurWajib) {
            byte[] dataXml = entriZip.get(jalur);
            assertNotNull("Entri wajib hilang: " + jalur, dataXml);
            Document dokumen = pabrikXml.newDocumentBuilder().parse(new ByteArrayInputStream(dataXml));
            assertNotNull(dokumen.getDocumentElement());
        }

        String workbook = new String(entriZip.get("xl/workbook.xml"), StandardCharsets.UTF_8);
        assertTrue(workbook.contains("Ringkasan Target"));
        assertTrue(workbook.contains("Riwayat Transaksi"));

        String sheet1 = new String(entriZip.get("xl/worksheets/sheet1.xml"), StandardCharsets.UTF_8);
        assertTrue(sheet1.contains("SUM(D8:D9)"));
        assertTrue(sheet1.contains("ySplit=\"7\""));

        String sheet2 = new String(entriZip.get("xl/worksheets/sheet2.xml"), StandardCharsets.UTF_8);
        assertTrue(sheet2.contains("SUMIF("));

        String styles = new String(entriZip.get("xl/styles.xml"), StandardCharsets.UTF_8);
        assertTrue(styles.contains("#,##0"));
    }
}
