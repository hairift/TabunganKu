package com.hairift.tabunganku.ekspor;

import com.hairift.tabunganku.data.model.TargetTabungan;
import com.hairift.tabunganku.data.model.TransaksiDenganTarget;
import com.hairift.tabunganku.util.FormatTanggal;
import com.hairift.tabunganku.util.KalkulatorTarget;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Menghasilkan berkas Excel (.xlsx) OOXML berdesain rapi dengan dua lembar kerja,
 * gaya merek TabunganKu, format mata uang Rupiah, freeze header, dan rumus penjumlahan.
 */
public final class PembuatExcelXlsx {

    // Indeks gaya sel yang terdefinisi pada berkas xl/styles.xml.
    private static final int GAYA_JUDUL_BANNER = 1;
    private static final int GAYA_SUBJUDUL = 2;
    private static final int GAYA_LABEL_KPI = 3;
    private static final int GAYA_NILAI_KPI_RUPIAH = 4;
    private static final int GAYA_NILAI_KPI_ANGKA = 5;
    private static final int GAYA_HEADER_TABEL = 6;
    private static final int GAYA_TEKS_GANJIL = 7;
    private static final int GAYA_TEKS_GENAP = 8;
    private static final int GAYA_TENGAH_GANJIL = 9;
    private static final int GAYA_TENGAH_GENAP = 10;
    private static final int GAYA_RUPIAH_GANJIL = 11;
    private static final int GAYA_RUPIAH_GENAP = 12;
    private static final int GAYA_PERSEN_GANJIL = 13;
    private static final int GAYA_PERSEN_GENAP = 14;
    private static final int GAYA_STATUS_HIJAU = 15;
    private static final int GAYA_STATUS_TERAKOTA = 16;
    private static final int GAYA_STATUS_ARSIP = 17;
    private static final int GAYA_TOTAL_TEKS = 18;
    private static final int GAYA_TOTAL_RUPIAH = 19;

    private PembuatExcelXlsx() {
    }

    /**
     * Menulis buku kerja Excel (.xlsx) ke OutputStream yang diberikan.
     */
    public static void tulisBukuKerja(
        OutputStream keluaran,
        String judulCakupan,
        List<TargetTabungan> daftarTarget,
        List<TransaksiDenganTarget> daftarTransaksi,
        long waktuCetakEpoch
    ) throws IOException {
        TabelTeksBersama tabelTeks = new TabelTeksBersama();

        String xmlLembarTarget = susunLembarTarget(
            tabelTeks,
            judulCakupan,
            daftarTarget,
            waktuCetakEpoch
        );
        String xmlLembarTransaksi = susunLembarTransaksi(
            tabelTeks,
            judulCakupan,
            daftarTransaksi,
            waktuCetakEpoch
        );

        try (ZipOutputStream arsip = new ZipOutputStream(keluaran)) {
            tulisEntri(arsip, "[Content_Types].xml", buatContentTypes());
            tulisEntri(arsip, "_rels/.rels", buatRelsAkar());
            tulisEntri(arsip, "xl/workbook.xml", buatWorkbook());
            tulisEntri(arsip, "xl/_rels/workbook.xml.rels", buatWorkbookRels());
            tulisEntri(arsip, "xl/styles.xml", buatStylesXml());
            tulisEntri(arsip, "xl/sharedStrings.xml", tabelTeks.buatXml());
            tulisEntri(arsip, "xl/worksheets/sheet1.xml", xmlLembarTarget);
            tulisEntri(arsip, "xl/worksheets/sheet2.xml", xmlLembarTransaksi);
            arsip.finish();
        }
    }

    private static String susunLembarTarget(
        TabelTeksBersama teks,
        String judulCakupan,
        List<TargetTabungan> daftarTarget,
        long waktuCetakEpoch
    ) {
        long totalTarget = 0L;
        long totalTerkumpul = 0L;
        long totalSisa = 0L;
        int jumlahTercapai = 0;
        for (TargetTabungan target : daftarTarget) {
            totalTarget += target.nominalTarget;
            totalTerkumpul += target.saldoTerkumpul;
            totalSisa += KalkulatorTarget.hitungSisa(target.saldoTerkumpul, target.nominalTarget);
            if (target.apakahTercapai()) {
                jumlahTercapai++;
            }
        }

        // Hitung lebar tiap kolom (A..J) secara dinamis supaya teks dan angka tidak terpotong ###.
        int[] lebarMaks = new int[]{6, 22, 18, 18, 18, 18, 11, 16, 13, 28};
        for (TargetTabungan t : daftarTarget) {
            lebarMaks[1] = Math.max(lebarMaks[1], Math.min(36, t.namaTarget.length() + 4));
            lebarMaks[2] = Math.max(lebarMaks[2], Math.min(24, t.kategori.length() + 4));
            if (t.catatanMotivasi != null) {
                lebarMaks[9] = Math.max(lebarMaks[9], Math.min(44, t.catatanMotivasi.length() + 4));
            }
        }

        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>")
            .append("<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">")
            .append("<sheetViews><sheetView tabSelected=\"1\" workbookViewId=\"0\">")
            .append("<pane ySplit=\"7\" topLeftCell=\"A8\" activePane=\"bottomLeft\" state=\"frozen\"/>")
            .append("</sheetView></sheetViews>")
            .append("<sheetFormatPr defaultRowHeight=\"20\"/>");

        xml.append("<cols>");
        for (int i = 0; i < lebarMaks.length; i++) {
            int col = i + 1;
            xml.append("<col min=\"").append(col).append("\" max=\"").append(col)
                .append("\" width=\"").append(lebarMaks[i]).append("\" customWidth=\"1\"/>");
        }
        xml.append("</cols><sheetData>");

        // Baris 1: Banner Judul
        xml.append("<row r=\"1\" ht=\"34\" customHeight=\"1\">");
        selTeks(xml, "A1", teks.indeks("TABUNGANKU - LAPORAN REKAPITULASI TABUNGAN"), GAYA_JUDUL_BANNER);
        for (int c = 2; c <= 10; c++) {
            selKosong(xml, namaKolom(c) + "1", GAYA_JUDUL_BANNER);
        }
        xml.append("</row>");

        // Baris 2: Subjudul metadata cetak
        xml.append("<row r=\"2\" ht=\"22\" customHeight=\"1\">");
        String infoCetak = "Cakupan: " + judulCakupan
            + "   |   Dicetak pada: " + FormatTanggal.keTanggalPenuh(waktuCetakEpoch);
        selTeks(xml, "A2", teks.indeks(infoCetak), GAYA_SUBJUDUL);
        for (int c = 2; c <= 10; c++) {
            selKosong(xml, namaKolom(c) + "2", GAYA_SUBJUDUL);
        }
        xml.append("</row>");

        // Baris 3: Jeda napas
        xml.append("<row r=\"3\" ht=\"10\" customHeight=\"1\"/>");

        // Baris 4: Label ringkasan KPI
        xml.append("<row r=\"4\" ht=\"20\" customHeight=\"1\">");
        selTeks(xml, "B4", teks.indeks("Total Dana Terkumpul"), GAYA_LABEL_KPI);
        selKosong(xml, "C4", GAYA_LABEL_KPI);
        selTeks(xml, "D4", teks.indeks("Total Target"), GAYA_LABEL_KPI);
        selKosong(xml, "E4", GAYA_LABEL_KPI);
        selTeks(xml, "F4", teks.indeks("Sisa yang Perlu Ditabung"), GAYA_LABEL_KPI);
        selKosong(xml, "G4", GAYA_LABEL_KPI);
        selTeks(xml, "H4", teks.indeks("Target Tercapai"), GAYA_LABEL_KPI);
        selKosong(xml, "I4", GAYA_LABEL_KPI);
        xml.append("</row>");

        // Baris 5: Nilai ringkasan KPI
        xml.append("<row r=\"5\" ht=\"26\" customHeight=\"1\">");
        selAngka(xml, "B5", totalTerkumpul, GAYA_NILAI_KPI_RUPIAH);
        selKosong(xml, "C5", GAYA_NILAI_KPI_RUPIAH);
        selAngka(xml, "D5", totalTarget, GAYA_NILAI_KPI_RUPIAH);
        selKosong(xml, "E5", GAYA_NILAI_KPI_RUPIAH);
        selAngka(xml, "F5", totalSisa, GAYA_NILAI_KPI_RUPIAH);
        selKosong(xml, "G5", GAYA_NILAI_KPI_RUPIAH);
        selTeks(
            xml,
            "H5",
            teks.indeks(jumlahTercapai + " dari " + daftarTarget.size()),
            GAYA_NILAI_KPI_ANGKA
        );
        selKosong(xml, "I5", GAYA_NILAI_KPI_ANGKA);
        xml.append("</row>");

        xml.append("<row r=\"6\" ht=\"12\" customHeight=\"1\"/>");

        // Baris 7: Header tabel target
        String[] headerKolom = {
            "No", "Nama Target", "Kategori", "Target Dana", "Saldo Terkumpul",
            "Sisa Target", "Progres", "Tanggal Target", "Status", "Catatan Motivasi"
        };
        xml.append("<row r=\"7\" ht=\"26\" customHeight=\"1\">");
        for (int i = 0; i < headerKolom.length; i++) {
            selTeks(xml, namaKolom(i + 1) + "7", teks.indeks(headerKolom[i]), GAYA_HEADER_TABEL);
        }
        xml.append("</row>");

        // Baris Data Target mulai baris 8
        int barisAwalData = 8;
        int barisSaatIni = barisAwalData;
        for (int i = 0; i < daftarTarget.size(); i++) {
            TargetTabungan item = daftarTarget.get(i);
            boolean genap = (i % 2 == 1);
            int gayaTeks = genap ? GAYA_TEKS_GENAP : GAYA_TEKS_GANJIL;
            int gayaTengah = genap ? GAYA_TENGAH_GENAP : GAYA_TENGAH_GANJIL;
            int gayaRupiah = genap ? GAYA_RUPIAH_GENAP : GAYA_RUPIAH_GANJIL;
            int gayaPersen = genap ? GAYA_PERSEN_GENAP : GAYA_PERSEN_GANJIL;

            long sisa = KalkulatorTarget.hitungSisa(item.saldoTerkumpul, item.nominalTarget);
            double rasio = KalkulatorTarget.hitungRasioDesimal(item.saldoTerkumpul, item.nominalTarget);
            String tanggalTarget = item.tanggalTargetEpoch > 0
                ? FormatTanggal.keTanggalRingkas(item.tanggalTargetEpoch)
                : "-";
            String status = item.diarsipkan
                ? "Diarsipkan"
                : (item.apakahTercapai() ? "Tercapai" : "Berjalan");
            int gayaStatus = item.diarsipkan
                ? GAYA_STATUS_ARSIP
                : (item.apakahTercapai() ? GAYA_STATUS_HIJAU : GAYA_STATUS_TERAKOTA);

            xml.append("<row r=\"").append(barisSaatIni).append("\" ht=\"24\" customHeight=\"1\">");
            selAngka(xml, "A" + barisSaatIni, i + 1, gayaTengah);
            selTeks(xml, "B" + barisSaatIni, teks.indeks(item.namaTarget), gayaTeks);
            selTeks(xml, "C" + barisSaatIni, teks.indeks(item.kategori), gayaTengah);
            selAngka(xml, "D" + barisSaatIni, item.nominalTarget, gayaRupiah);
            selAngka(xml, "E" + barisSaatIni, item.saldoTerkumpul, gayaRupiah);
            selAngka(xml, "F" + barisSaatIni, sisa, gayaRupiah);
            selDesimal(xml, "G" + barisSaatIni, rasio, gayaPersen);
            selTeks(xml, "H" + barisSaatIni, teks.indeks(tanggalTarget), gayaTengah);
            selTeks(xml, "I" + barisSaatIni, teks.indeks(status), gayaStatus);
            selTeks(
                xml,
                "J" + barisSaatIni,
                teks.indeks(item.catatanMotivasi != null ? item.catatanMotivasi : "-"),
                gayaTeks
            );
            xml.append("</row>");
            barisSaatIni++;
        }

        // Baris Total dengan rumus Excel SUM asli apabila ada minimal satu target
        int barisAkhirData = barisSaatIni - 1;
        xml.append("<row r=\"").append(barisSaatIni).append("\" ht=\"26\" customHeight=\"1\">");
        selKosong(xml, "A" + barisSaatIni, GAYA_TOTAL_TEKS);
        selTeks(xml, "B" + barisSaatIni, teks.indeks("TOTAL KESELURUHAN"), GAYA_TOTAL_TEKS);
        selKosong(xml, "C" + barisSaatIni, GAYA_TOTAL_TEKS);
        if (barisAkhirData >= barisAwalData) {
            selRumus(
                xml,
                "D" + barisSaatIni,
                "SUM(D" + barisAwalData + ":D" + barisAkhirData + ")",
                totalTarget,
                GAYA_TOTAL_RUPIAH
            );
            selRumus(
                xml,
                "E" + barisSaatIni,
                "SUM(E" + barisAwalData + ":E" + barisAkhirData + ")",
                totalTerkumpul,
                GAYA_TOTAL_RUPIAH
            );
            selRumus(
                xml,
                "F" + barisSaatIni,
                "SUM(F" + barisAwalData + ":F" + barisAkhirData + ")",
                totalSisa,
                GAYA_TOTAL_RUPIAH
            );
        } else {
            selAngka(xml, "D" + barisSaatIni, 0L, GAYA_TOTAL_RUPIAH);
            selAngka(xml, "E" + barisSaatIni, 0L, GAYA_TOTAL_RUPIAH);
            selAngka(xml, "F" + barisSaatIni, 0L, GAYA_TOTAL_RUPIAH);
        }
        selKosong(xml, "G" + barisSaatIni, GAYA_TOTAL_TEKS);
        selKosong(xml, "H" + barisSaatIni, GAYA_TOTAL_TEKS);
        selKosong(xml, "I" + barisSaatIni, GAYA_TOTAL_TEKS);
        selKosong(xml, "J" + barisSaatIni, GAYA_TOTAL_TEKS);
        xml.append("</row>");

        xml.append("</sheetData>");
        xml.append("<mergeCells count=\"10\">")
            .append("<mergeCell ref=\"A1:J1\"/>")
            .append("<mergeCell ref=\"A2:J2\"/>")
            .append("<mergeCell ref=\"B4:C4\"/>")
            .append("<mergeCell ref=\"D4:E4\"/>")
            .append("<mergeCell ref=\"F4:G4\"/>")
            .append("<mergeCell ref=\"H4:I4\"/>")
            .append("<mergeCell ref=\"B5:C5\"/>")
            .append("<mergeCell ref=\"D5:E5\"/>")
            .append("<mergeCell ref=\"F5:G5\"/>")
            .append("<mergeCell ref=\"H5:I5\"/>")
            .append("</mergeCells>");
        xml.append("<pageSetup orientation=\"landscape\" paperSize=\"9\"/>");
        xml.append("</worksheet>");
        return xml.toString();
    }

    private static String susunLembarTransaksi(
        TabelTeksBersama teks,
        String judulCakupan,
        List<TransaksiDenganTarget> daftarTransaksi,
        long waktuCetakEpoch
    ) {
        long totalSetor = 0L;
        long totalTarik = 0L;
        for (TransaksiDenganTarget tr : daftarTransaksi) {
            if (tr.apakahSetoran()) {
                totalSetor += tr.nominal;
            } else {
                totalTarik += tr.nominal;
            }
        }

        int[] lebarMaks = new int[]{6, 16, 24, 18, 12, 20, 34};
        for (TransaksiDenganTarget tr : daftarTransaksi) {
            lebarMaks[2] = Math.max(lebarMaks[2], Math.min(36, tr.namaTarget.length() + 4));
            if (tr.catatan != null) {
                lebarMaks[6] = Math.max(lebarMaks[6], Math.min(46, tr.catatan.length() + 4));
            }
        }

        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>")
            .append("<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">")
            .append("<sheetViews><sheetView workbookViewId=\"0\">")
            .append("<pane ySplit=\"4\" topLeftCell=\"A5\" activePane=\"bottomLeft\" state=\"frozen\"/>")
            .append("</sheetView></sheetViews>")
            .append("<sheetFormatPr defaultRowHeight=\"20\"/>");

        xml.append("<cols>");
        for (int i = 0; i < lebarMaks.length; i++) {
            int col = i + 1;
            xml.append("<col min=\"").append(col).append("\" max=\"").append(col)
                .append("\" width=\"").append(lebarMaks[i]).append("\" customWidth=\"1\"/>");
        }
        xml.append("</cols><sheetData>");

        xml.append("<row r=\"1\" ht=\"34\" customHeight=\"1\">");
        selTeks(xml, "A1", teks.indeks("TABUNGANKU - RIWAYAT MUTASI SETOR DAN TARIK"), GAYA_JUDUL_BANNER);
        for (int c = 2; c <= 7; c++) {
            selKosong(xml, namaKolom(c) + "1", GAYA_JUDUL_BANNER);
        }
        xml.append("</row>");

        xml.append("<row r=\"2\" ht=\"22\" customHeight=\"1\">");
        String info = "Cakupan: " + judulCakupan
            + "   |   Total transaksi: " + daftarTransaksi.size()
            + "   |   Dicetak: " + FormatTanggal.keTanggalPenuh(waktuCetakEpoch);
        selTeks(xml, "A2", teks.indeks(info), GAYA_SUBJUDUL);
        for (int c = 2; c <= 7; c++) {
            selKosong(xml, namaKolom(c) + "2", GAYA_SUBJUDUL);
        }
        xml.append("</row>");

        xml.append("<row r=\"3\" ht=\"10\" customHeight=\"1\"/>");

        String[] headerKolom = {
            "No", "Tanggal", "Nama Target", "Kategori", "Jenis", "Nominal (Rp)", "Catatan"
        };
        xml.append("<row r=\"4\" ht=\"26\" customHeight=\"1\">");
        for (int i = 0; i < headerKolom.length; i++) {
            selTeks(xml, namaKolom(i + 1) + "4", teks.indeks(headerKolom[i]), GAYA_HEADER_TABEL);
        }
        xml.append("</row>");

        int barisAwalData = 5;
        int barisSaatIni = barisAwalData;
        for (int i = 0; i < daftarTransaksi.size(); i++) {
            TransaksiDenganTarget tr = daftarTransaksi.get(i);
            boolean genap = (i % 2 == 1);
            int gayaTeks = genap ? GAYA_TEKS_GENAP : GAYA_TEKS_GANJIL;
            int gayaTengah = genap ? GAYA_TENGAH_GENAP : GAYA_TENGAH_GANJIL;
            int gayaRupiah = genap ? GAYA_RUPIAH_GENAP : GAYA_RUPIAH_GANJIL;
            int gayaJenis = tr.apakahSetoran() ? GAYA_STATUS_HIJAU : GAYA_STATUS_TERAKOTA;

            xml.append("<row r=\"").append(barisSaatIni).append("\" ht=\"23\" customHeight=\"1\">");
            selAngka(xml, "A" + barisSaatIni, i + 1, gayaTengah);
            selTeks(xml, "B" + barisSaatIni, teks.indeks(FormatTanggal.keTanggalRingkas(tr.tanggalTransaksiEpoch)), gayaTengah);
            selTeks(xml, "C" + barisSaatIni, teks.indeks(tr.namaTarget), gayaTeks);
            selTeks(xml, "D" + barisSaatIni, teks.indeks(tr.kategori), gayaTengah);
            selTeks(xml, "E" + barisSaatIni, teks.indeks(tr.apakahSetoran() ? "Setor" : "Tarik"), gayaJenis);
            selAngka(xml, "F" + barisSaatIni, tr.nominal, gayaRupiah);
            selTeks(xml, "G" + barisSaatIni, teks.indeks(tr.catatan != null ? tr.catatan : "-"), gayaTeks);
            xml.append("</row>");
            barisSaatIni++;
        }

        int barisAkhirData = barisSaatIni - 1;
        int barisTotalSetor = barisSaatIni + 1;
        int barisTotalTarik = barisSaatIni + 2;
        int barisArusBersih = barisSaatIni + 3;

        xml.append("<row r=\"").append(barisTotalSetor).append("\" ht=\"24\" customHeight=\"1\">");
        selTeks(xml, "E" + barisTotalSetor, teks.indeks("Total Setor"), GAYA_STATUS_HIJAU);
        if (barisAkhirData >= barisAwalData) {
            selRumus(
                xml,
                "F" + barisTotalSetor,
                "SUMIF(E" + barisAwalData + ":E" + barisAkhirData + ",\"Setor\",F" + barisAwalData + ":F" + barisAkhirData + ")",
                totalSetor,
                GAYA_TOTAL_RUPIAH
            );
        } else {
            selAngka(xml, "F" + barisTotalSetor, 0L, GAYA_TOTAL_RUPIAH);
        }
        xml.append("</row>");

        xml.append("<row r=\"").append(barisTotalTarik).append("\" ht=\"24\" customHeight=\"1\">");
        selTeks(xml, "E" + barisTotalTarik, teks.indeks("Total Tarik"), GAYA_STATUS_TERAKOTA);
        if (barisAkhirData >= barisAwalData) {
            selRumus(
                xml,
                "F" + barisTotalTarik,
                "SUMIF(E" + barisAwalData + ":E" + barisAkhirData + ",\"Tarik\",F" + barisAwalData + ":F" + barisAkhirData + ")",
                totalTarik,
                GAYA_TOTAL_RUPIAH
            );
        } else {
            selAngka(xml, "F" + barisTotalTarik, 0L, GAYA_TOTAL_RUPIAH);
        }
        xml.append("</row>");

        xml.append("<row r=\"").append(barisArusBersih).append("\" ht=\"26\" customHeight=\"1\">");
        selTeks(xml, "E" + barisArusBersih, teks.indeks("Arus Bersih"), GAYA_TOTAL_TEKS);
        selRumus(
            xml,
            "F" + barisArusBersih,
            "F" + barisTotalSetor + "-F" + barisTotalTarik,
            totalSetor - totalTarik,
            GAYA_TOTAL_RUPIAH
        );
        xml.append("</row>");

        xml.append("</sheetData>");
        xml.append("<mergeCells count=\"2\">")
            .append("<mergeCell ref=\"A1:G1\"/>")
            .append("<mergeCell ref=\"A2:G2\"/>")
            .append("</mergeCells>");
        xml.append("<pageSetup orientation=\"landscape\" paperSize=\"9\"/>");
        xml.append("</worksheet>");
        return xml.toString();
    }

    private static void selTeks(StringBuilder xml, String referensi, int indeksTeks, int gaya) {
        xml.append("<c r=\"").append(referensi).append("\" t=\"s\" s=\"").append(gaya)
            .append("\"><v>").append(indeksTeks).append("</v></c>");
    }

    private static void selKosong(StringBuilder xml, String referensi, int gaya) {
        xml.append("<c r=\"").append(referensi).append("\" s=\"").append(gaya).append("\"/>");
    }

    private static void selAngka(StringBuilder xml, String referensi, long nilai, int gaya) {
        xml.append("<c r=\"").append(referensi).append("\" s=\"").append(gaya)
            .append("\"><v>").append(nilai).append("</v></c>");
    }

    private static void selDesimal(StringBuilder xml, String referensi, double nilai, int gaya) {
        String teksAngka = String.format(Locale.US, "%.4f", nilai);
        xml.append("<c r=\"").append(referensi).append("\" s=\"").append(gaya)
            .append("\"><v>").append(teksAngka).append("</v></c>");
    }

    private static void selRumus(
        StringBuilder xml,
        String referensi,
        String rumus,
        long nilaiTersimpan,
        int gaya
    ) {
        xml.append("<c r=\"").append(referensi).append("\" s=\"").append(gaya)
            .append("\"><f>").append(lolosXml(rumus)).append("</f><v>")
            .append(nilaiTersimpan).append("</v></c>");
    }

    private static String namaKolom(int nomorSatuBerbasis) {
        StringBuilder nama = new StringBuilder();
        int n = nomorSatuBerbasis;
        while (n > 0) {
            int sisa = (n - 1) % 26;
            nama.insert(0, (char) ('A' + sisa));
            n = (n - 1) / 26;
        }
        return nama.toString();
    }

    private static void tulisEntri(ZipOutputStream arsip, String jalur, String konten) throws IOException {
        arsip.putNextEntry(new ZipEntry(jalur));
        arsip.write(konten.getBytes(StandardCharsets.UTF_8));
        arsip.closeEntry();
    }

    private static String buatContentTypes() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
            + "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">"
            + "<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>"
            + "<Default Extension=\"xml\" ContentType=\"application/xml\"/>"
            + "<Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/>"
            + "<Override PartName=\"/xl/worksheets/sheet1.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>"
            + "<Override PartName=\"/xl/worksheets/sheet2.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>"
            + "<Override PartName=\"/xl/styles.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml\"/>"
            + "<Override PartName=\"/xl/sharedStrings.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sharedStrings+xml\"/>"
            + "</Types>";
    }

    private static String buatRelsAkar() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
            + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"
            + "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.xml\"/>"
            + "</Relationships>";
    }

    private static String buatWorkbook() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
            + "<workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" "
            + "xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\">"
            + "<sheets>"
            + "<sheet name=\"Ringkasan Target\" sheetId=\"1\" r:id=\"rId1\"/>"
            + "<sheet name=\"Riwayat Transaksi\" sheetId=\"2\" r:id=\"rId2\"/>"
            + "</sheets>"
            + "</workbook>";
    }

    private static String buatWorkbookRels() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
            + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"
            + "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet1.xml\"/>"
            + "<Relationship Id=\"rId2\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet2.xml\"/>"
            + "<Relationship Id=\"rId3\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles\" Target=\"styles.xml\"/>"
            + "<Relationship Id=\"rId4\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/sharedStrings\" Target=\"sharedStrings.xml\"/>"
            + "</Relationships>";
    }

    private static String buatStylesXml() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
            + "<styleSheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">"
            + "<numFmts count=\"1\">"
            + "<numFmt numFmtId=\"164\" formatCode=\"&quot;Rp &quot;#,##0\"/>"
            + "</numFmts>"
            // Fonts: 0=Normal, 1=Banner Cream Bold 14, 2=Header Cream Bold 11, 3=Sekunder 10, 4=Bold Hijau, 5=Hijau Status, 6=Terakota Status, 7=Bold Hitam 11
            + "<fonts count=\"8\">"
            + "<font><sz val=\"10\"/><color rgb=\"FF1F2421\"/><name val=\"Calibri\"/></font>"
            + "<font><b/><sz val=\"14\"/><color rgb=\"FFFAF7F2\"/><name val=\"Calibri\"/></font>"
            + "<font><b/><sz val=\"10\"/><color rgb=\"FFFAF7F2\"/><name val=\"Calibri\"/></font>"
            + "<font><sz val=\"10\"/><color rgb=\"FF505752\"/><name val=\"Calibri\"/></font>"
            + "<font><b/><sz val=\"10\"/><color rgb=\"FF1E4D3B\"/><name val=\"Calibri\"/></font>"
            + "<font><b/><sz val=\"10\"/><color rgb=\"FF1B4B37\"/><name val=\"Calibri\"/></font>"
            + "<font><b/><sz val=\"10\"/><color rgb=\"FF8D3618\"/><name val=\"Calibri\"/></font>"
            + "<font><b/><sz val=\"11\"/><color rgb=\"FF1F2421\"/><name val=\"Calibri\"/></font>"
            + "</fonts>"
            // Fills: 0=none, 1=gray125, 2=Hijau Pinus, 3=Pasir Hangat, 4=Gading, 5=Linen, 6=Setor Hijau Muda, 7=Tarik Terakota Muda, 8=Hijau Total Lembut
            + "<fills count=\"9\">"
            + "<fill><patternFill patternType=\"none\"/></fill>"
            + "<fill><patternFill patternType=\"gray125\"/></fill>"
            + "<fill><patternFill patternType=\"solid\"><fgColor rgb=\"FF1E4D3B\"/></patternFill></fill>"
            + "<fill><patternFill patternType=\"solid\"><fgColor rgb=\"FFEFE8DA\"/></patternFill></fill>"
            + "<fill><patternFill patternType=\"solid\"><fgColor rgb=\"FFFFFDF9\"/></patternFill></fill>"
            + "<fill><patternFill patternType=\"solid\"><fgColor rgb=\"FFF7F3EB\"/></patternFill></fill>"
            + "<fill><patternFill patternType=\"solid\"><fgColor rgb=\"FFE5F0EA\"/></patternFill></fill>"
            + "<fill><patternFill patternType=\"solid\"><fgColor rgb=\"FFFAEBE4\"/></patternFill></fill>"
            + "<fill><patternFill patternType=\"solid\"><fgColor rgb=\"FFDCE9E2\"/></patternFill></fill>"
            + "</fills>"
            // Borders: 0=none, 1=Tipis sel tabel, 2=Total atas tipis bawah ganda, 3=KPI kotak halus
            + "<borders count=\"4\">"
            + "<border><left/><right/><top/><bottom/><diagonal/></border>"
            + "<border><left style=\"thin\"><color rgb=\"FFD5CCB9\"/></left><right style=\"thin\"><color rgb=\"FFD5CCB9\"/></right><top style=\"thin\"><color rgb=\"FFD5CCB9\"/></top><bottom style=\"thin\"><color rgb=\"FFD5CCB9\"/></bottom><diagonal/></border>"
            + "<border><left style=\"thin\"><color rgb=\"FF1E4D3B\"/></left><right style=\"thin\"><color rgb=\"FF1E4D3B\"/></right><top style=\"thin\"><color rgb=\"FF1E4D3B\"/></top><bottom style=\"double\"><color rgb=\"FF1E4D3B\"/></bottom><diagonal/></border>"
            + "<border><left style=\"thin\"><color rgb=\"FFC5BAAA\"/></left><right style=\"thin\"><color rgb=\"FFC5BAAA\"/></right><top style=\"thin\"><color rgb=\"FFC5BAAA\"/></top><bottom style=\"thin\"><color rgb=\"FFC5BAAA\"/></bottom><diagonal/></border>"
            + "</borders>"
            + "<cellStyleXfs count=\"1\"><xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\"/></cellStyleXfs>"
            // CellXfs indeks 0..19 cocok dengan konstanta GAYA_* di atas
            + "<cellXfs count=\"20\">"
            + "<xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\" xfId=\"0\"/>"
            // 1: GAYA_JUDUL_BANNER
            + "<xf numFmtId=\"0\" fontId=\"1\" fillId=\"2\" borderId=\"0\" xfId=\"0\" applyFont=\"1\" applyFill=\"1\" applyAlignment=\"1\"><alignment horizontal=\"left\" vertical=\"center\" indent=\"1\"/></xf>"
            // 2: GAYA_SUBJUDUL
            + "<xf numFmtId=\"0\" fontId=\"0\" fillId=\"3\" borderId=\"0\" xfId=\"0\" applyFont=\"1\" applyFill=\"1\" applyAlignment=\"1\"><alignment horizontal=\"left\" vertical=\"center\" indent=\"1\"/></xf>"
            // 3: GAYA_LABEL_KPI
            + "<xf numFmtId=\"0\" fontId=\"3\" fillId=\"3\" borderId=\"3\" xfId=\"0\" applyFont=\"1\" applyFill=\"1\" applyBorder=\"1\" applyAlignment=\"1\"><alignment horizontal=\"center\" vertical=\"center\"/></xf>"
            // 4: GAYA_NILAI_KPI_RUPIAH
            + "<xf numFmtId=\"164\" fontId=\"7\" fillId=\"4\" borderId=\"3\" xfId=\"0\" applyNumberFormat=\"1\" applyFont=\"1\" applyFill=\"1\" applyBorder=\"1\" applyAlignment=\"1\"><alignment horizontal=\"center\" vertical=\"center\"/></xf>"
            // 5: GAYA_NILAI_KPI_ANGKA
            + "<xf numFmtId=\"0\" fontId=\"7\" fillId=\"4\" borderId=\"3\" xfId=\"0\" applyFont=\"1\" applyFill=\"1\" applyBorder=\"1\" applyAlignment=\"1\"><alignment horizontal=\"center\" vertical=\"center\"/></xf>"
            // 6: GAYA_HEADER_TABEL
            + "<xf numFmtId=\"0\" fontId=\"2\" fillId=\"2\" borderId=\"1\" xfId=\"0\" applyFont=\"1\" applyFill=\"1\" applyBorder=\"1\" applyAlignment=\"1\"><alignment horizontal=\"center\" vertical=\"center\" wrapText=\"1\"/></xf>"
            // 7: GAYA_TEKS_GANJIL
            + "<xf numFmtId=\"0\" fontId=\"0\" fillId=\"4\" borderId=\"1\" xfId=\"0\" applyFont=\"1\" applyFill=\"1\" applyBorder=\"1\" applyAlignment=\"1\"><alignment horizontal=\"left\" vertical=\"center\" wrapText=\"1\"/></xf>"
            // 8: GAYA_TEKS_GENAP
            + "<xf numFmtId=\"0\" fontId=\"0\" fillId=\"5\" borderId=\"1\" xfId=\"0\" applyFont=\"1\" applyFill=\"1\" applyBorder=\"1\" applyAlignment=\"1\"><alignment horizontal=\"left\" vertical=\"center\" wrapText=\"1\"/></xf>"
            // 9: GAYA_TENGAH_GANJIL
            + "<xf numFmtId=\"0\" fontId=\"0\" fillId=\"4\" borderId=\"1\" xfId=\"0\" applyFont=\"1\" applyFill=\"1\" applyBorder=\"1\" applyAlignment=\"1\"><alignment horizontal=\"center\" vertical=\"center\"/></xf>"
            // 10: GAYA_TENGAH_GENAP
            + "<xf numFmtId=\"0\" fontId=\"0\" fillId=\"5\" borderId=\"1\" xfId=\"0\" applyFont=\"1\" applyFill=\"1\" applyBorder=\"1\" applyAlignment=\"1\"><alignment horizontal=\"center\" vertical=\"center\"/></xf>"
            // 11: GAYA_RUPIAH_GANJIL
            + "<xf numFmtId=\"164\" fontId=\"0\" fillId=\"4\" borderId=\"1\" xfId=\"0\" applyNumberFormat=\"1\" applyFont=\"1\" applyFill=\"1\" applyBorder=\"1\" applyAlignment=\"1\"><alignment horizontal=\"right\" vertical=\"center\"/></xf>"
            // 12: GAYA_RUPIAH_GENAP
            + "<xf numFmtId=\"164\" fontId=\"0\" fillId=\"5\" borderId=\"1\" xfId=\"0\" applyNumberFormat=\"1\" applyFont=\"1\" applyFill=\"1\" applyBorder=\"1\" applyAlignment=\"1\"><alignment horizontal=\"right\" vertical=\"center\"/></xf>"
            // 13: GAYA_PERSEN_GANJIL
            + "<xf numFmtId=\"10\" fontId=\"0\" fillId=\"4\" borderId=\"1\" xfId=\"0\" applyNumberFormat=\"1\" applyFont=\"1\" applyFill=\"1\" applyBorder=\"1\" applyAlignment=\"1\"><alignment horizontal=\"center\" vertical=\"center\"/></xf>"
            // 14: GAYA_PERSEN_GENAP
            + "<xf numFmtId=\"10\" fontId=\"0\" fillId=\"5\" borderId=\"1\" xfId=\"0\" applyNumberFormat=\"1\" applyFont=\"1\" applyFill=\"1\" applyBorder=\"1\" applyAlignment=\"1\"><alignment horizontal=\"center\" vertical=\"center\"/></xf>"
            // 15: GAYA_STATUS_HIJAU
            + "<xf numFmtId=\"0\" fontId=\"5\" fillId=\"6\" borderId=\"1\" xfId=\"0\" applyFont=\"1\" applyFill=\"1\" applyBorder=\"1\" applyAlignment=\"1\"><alignment horizontal=\"center\" vertical=\"center\"/></xf>"
            // 16: GAYA_STATUS_TERAKOTA
            + "<xf numFmtId=\"0\" fontId=\"6\" fillId=\"7\" borderId=\"1\" xfId=\"0\" applyFont=\"1\" applyFill=\"1\" applyBorder=\"1\" applyAlignment=\"1\"><alignment horizontal=\"center\" vertical=\"center\"/></xf>"
            // 17: GAYA_STATUS_ARSIP
            + "<xf numFmtId=\"0\" fontId=\"3\" fillId=\"3\" borderId=\"1\" xfId=\"0\" applyFont=\"1\" applyFill=\"1\" applyBorder=\"1\" applyAlignment=\"1\"><alignment horizontal=\"center\" vertical=\"center\"/></xf>"
            // 18: GAYA_TOTAL_TEKS
            + "<xf numFmtId=\"0\" fontId=\"4\" fillId=\"8\" borderId=\"2\" xfId=\"0\" applyFont=\"1\" applyFill=\"1\" applyBorder=\"1\" applyAlignment=\"1\"><alignment horizontal=\"left\" vertical=\"center\"/></xf>"
            // 19: GAYA_TOTAL_RUPIAH
            + "<xf numFmtId=\"164\" fontId=\"4\" fillId=\"8\" borderId=\"2\" xfId=\"0\" applyNumberFormat=\"1\" applyFont=\"1\" applyFill=\"1\" applyBorder=\"1\" applyAlignment=\"1\"><alignment horizontal=\"right\" vertical=\"center\"/></xf>"
            + "</cellXfs>"
            + "<cellStyles count=\"1\"><cellStyle name=\"Normal\" xfId=\"0\" builtinId=\"0\"/></cellStyles>"
            + "</styleSheet>";
    }

    private static String lolosXml(String teksAsli) {
        if (teksAsli == null) {
            return "";
        }
        StringBuilder hasil = new StringBuilder(teksAsli.length());
        for (int i = 0; i < teksAsli.length(); i++) {
            char c = teksAsli.charAt(i);
            // Buang karakter kontrol XML ilegal agar berkas selalu valid dibuka Excel.
            if (c < 0x20 && c != '\t' && c != '\n' && c != '\r') {
                continue;
            }
            switch (c) {
                case '&':
                    hasil.append("&amp;");
                    break;
                case '<':
                    hasil.append("&lt;");
                    break;
                case '>':
                    hasil.append("&gt;");
                    break;
                case '"':
                    hasil.append("&quot;");
                    break;
                case '\'':
                    hasil.append("&apos;");
                    break;
                default:
                    hasil.append(c);
                    break;
            }
        }
        return hasil.toString();
    }

    private static final class TabelTeksBersama {
        private final Map<String, Integer> petaIndeks = new LinkedHashMap<>();
        private final List<String> daftarTeks = new ArrayList<>();
        private int totalPemakaian;

        int indeks(String teks) {
            String nilaiAman = teks == null ? "" : teks;
            totalPemakaian++;
            Integer ada = petaIndeks.get(nilaiAman);
            if (ada != null) {
                return ada;
            }
            int baru = daftarTeks.size();
            petaIndeks.put(nilaiAman, baru);
            daftarTeks.add(nilaiAman);
            return baru;
        }

        String buatXml() {
            StringBuilder xml = new StringBuilder();
            xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>")
                .append("<sst xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" ")
                .append("count=\"").append(totalPemakaian).append("\" ")
                .append("uniqueCount=\"").append(daftarTeks.size()).append("\">");
            for (String item : daftarTeks) {
                xml.append("<si><t xml:space=\"preserve\">")
                    .append(lolosXml(item))
                    .append("</t></si>");
            }
            xml.append("</sst>");
            return xml.toString();
        }
    }
}
