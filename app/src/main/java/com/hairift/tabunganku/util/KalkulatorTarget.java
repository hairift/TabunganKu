package com.hairift.tabunganku.util;

import com.hairift.tabunganku.data.model.TargetTabungan;

/**
 * Perhitungan logika bisnis target tabungan yang terpisah dari lapisan tampilan.
 */
public final class KalkulatorTarget {

    private KalkulatorTarget() {
    }

    /**
     * Menghitung persentase pencapaian 0..100 untuk bilah progres visual.
     */
    public static int hitungPersentase(long saldoTerkumpul, long nominalTarget) {
        if (nominalTarget <= 0 || saldoTerkumpul <= 0) {
            return 0;
        }
        long persen = (saldoTerkumpul * 100L) / nominalTarget;
        return (int) Math.min(100L, persen);
    }

    /**
     * Menghitung persentase asli tanpa batas 100 untuk kebutuhan pelaporan Excel.
     */
    public static double hitungRasioDesimal(long saldoTerkumpul, long nominalTarget) {
        if (nominalTarget <= 0 || saldoTerkumpul <= 0) {
            return 0.0;
        }
        return (double) saldoTerkumpul / (double) nominalTarget;
    }

    /**
     * Menghitung sisa dana yang masih diperlukan agar target terpenuhi; nol bila sudah tercapai.
     */
    public static long hitungSisa(long saldoTerkumpul, long nominalTarget) {
        long sisa = nominalTarget - saldoTerkumpul;
        return Math.max(0L, sisa);
    }

    /**
     * Menyusun teks rekomendasi setoran otomatis berdasarkan tanggal target dan sisa dana.
     */
    public static String buatRekomendasiNabung(TargetTabungan target) {
        if (target == null) {
            return "";
        }
        if (target.apakahTercapai()) {
            return "Target ini sudah tercapai penuh.";
        }
        long sisa = hitungSisa(target.saldoTerkumpul, target.nominalTarget);
        if (target.tanggalTargetEpoch <= 0) {
            return "Atur tanggal target untuk melihat rekomendasi setoran rutin.";
        }
        long sisaHari = FormatTanggal.hitungSisaHari(target.tanggalTargetEpoch);
        if (sisaHari <= 0) {
            return "Tenggat target telah tiba. Masih kurang " + FormatMataUang.keRupiah(sisa) + ".";
        }
        if (sisaHari <= 14) {
            long perHari = Math.max(1L, (sisa + sisaHari - 1L) / sisaHari);
            return "Sekitar " + FormatMataUang.keRupiah(perHari) + " per hari (" + sisaHari + " hari lagi).";
        }
        if (sisaHari <= 90) {
            long jumlahMinggu = Math.max(1L, (sisaHari + 6L) / 7L);
            long perMinggu = (sisa + jumlahMinggu - 1L) / jumlahMinggu;
            return "Sekitar " + FormatMataUang.keRupiah(perMinggu) + " per minggu (" + jumlahMinggu + " minggu lagi).";
        }
        long jumlahBulan = Math.max(1L, (sisaHari + 29L) / 30L);
        long perBulan = (sisa + jumlahBulan - 1L) / jumlahBulan;
        return "Sekitar " + FormatMataUang.keRupiah(perBulan) + " per bulan (" + jumlahBulan + " bulan lagi).";
    }
}
