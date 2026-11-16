package com.hairift.tabunganku.ekspor;

import com.hairift.tabunganku.data.model.TargetTabungan;
import com.hairift.tabunganku.data.model.TransaksiDenganTarget;
import com.hairift.tabunganku.util.FormatTanggal;
import com.hairift.tabunganku.util.KalkulatorTarget;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Menghasilkan berkas laporan CSV berstandar RFC 4180 dengan UTF-8 BOM agar terbuka rapi di Excel.
 */
public final class PembuatCsv {

    private static final byte[] UTF8_BOM = new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};

    private PembuatCsv() {
    }

    /**
     * Menulis laporan ringkasan target dan riwayat transaksi ke OutputStream dalam format CSV.
     */
    public static void tulisLaporan(
        OutputStream keluaran,
        String judulCakupan,
        List<TargetTabungan> daftarTarget,
        List<TransaksiDenganTarget> daftarTransaksi,
        long waktuCetakEpoch
    ) throws IOException {
        keluaran.write(UTF8_BOM);

        StringBuilder isi = new StringBuilder();
        tulisBaris(isi, "TabunganKu", "Laporan Rekapitulasi Tabungan");
        tulisBaris(isi, "Cakupan", judulCakupan);
        tulisBaris(isi, "Dicetak pada", FormatTanggal.keTanggalPenuh(waktuCetakEpoch));
        isi.append("\r\n");

        long totalTarget = 0L;
        long totalTerkumpul = 0L;
        long totalSisa = 0L;
        for (TargetTabungan target : daftarTarget) {
            totalTarget += target.nominalTarget;
            totalTerkumpul += target.saldoTerkumpul;
            totalSisa += KalkulatorTarget.hitungSisa(target.saldoTerkumpul, target.nominalTarget);
        }

        tulisBaris(isi, "RINGKASAN TARGET");
        tulisBaris(
            isi,
            "No",
            "Nama Target",
            "Kategori",
            "Target (Rp)",
            "Terkumpul (Rp)",
            "Sisa (Rp)",
            "Progres (%)",
            "Tanggal Target",
            "Status",
            "Catatan Motivasi"
        );

        for (int i = 0; i < daftarTarget.size(); i++) {
            TargetTabungan item = daftarTarget.get(i);
            long sisa = KalkulatorTarget.hitungSisa(item.saldoTerkumpul, item.nominalTarget);
            int persen = KalkulatorTarget.hitungPersentase(item.saldoTerkumpul, item.nominalTarget);
            String status = item.diarsipkan
                ? "Diarsipkan"
                : (item.apakahTercapai() ? "Tercapai" : "Berjalan");
            String tanggalTarget = item.tanggalTargetEpoch > 0
                ? FormatTanggal.keTanggalRingkas(item.tanggalTargetEpoch)
                : "Tanpa tenggat";

            tulisBaris(
                isi,
                String.valueOf(i + 1),
                item.namaTarget,
                item.kategori,
                String.valueOf(item.nominalTarget),
                String.valueOf(item.saldoTerkumpul),
                String.valueOf(sisa),
                persen + "%",
                tanggalTarget,
                status,
                item.catatanMotivasi != null ? item.catatanMotivasi : ""
            );
        }

        tulisBaris(
            isi,
            "",
            "TOTAL",
            "",
            String.valueOf(totalTarget),
            String.valueOf(totalTerkumpul),
            String.valueOf(totalSisa),
            "",
            "",
            "",
            ""
        );

        isi.append("\r\n");
        tulisBaris(isi, "RIWAYAT TRANSAKSI");
        tulisBaris(isi, "No", "Tanggal", "Nama Target", "Kategori", "Jenis", "Nominal (Rp)", "Catatan");

        for (int i = 0; i < daftarTransaksi.size(); i++) {
            TransaksiDenganTarget transaksi = daftarTransaksi.get(i);
            tulisBaris(
                isi,
                String.valueOf(i + 1),
                FormatTanggal.keTanggalRingkas(transaksi.tanggalTransaksiEpoch),
                transaksi.namaTarget,
                transaksi.kategori,
                transaksi.apakahSetoran() ? "Setor" : "Tarik",
                String.valueOf(transaksi.nominal),
                transaksi.catatan != null ? transaksi.catatan : ""
            );
        }

        keluaran.write(isi.toString().getBytes(StandardCharsets.UTF_8));
        keluaran.flush();
    }

    private static void tulisBaris(StringBuilder penyusun, String... sel) {
        for (int i = 0; i < sel.length; i++) {
            if (i > 0) {
                penyusun.append(',');
            }
            penyusun.append(amanCsv(sel[i]));
        }
        penyusun.append("\r\n");
    }

    /**
     * Mengamankan nilai sel sesuai RFC 4180 sekaligus mencegah injeksi rumus di Excel.
     */
    private static String amanCsv(String nilaiAsli) {
        if (nilaiAsli == null) {
            return "\"\"";
        }
        String bersih = nilaiAsli;
        if (!bersih.isEmpty()) {
            char awal = bersih.charAt(0);
            if (awal == '=' || awal == '+' || awal == '-' || awal == '@') {
                bersih = "'" + bersih;
            }
        }
        String keluarKutipan = bersih.replace("\"", "\"\"");
        return "\"" + keluarKutipan + "\"";
    }
}
