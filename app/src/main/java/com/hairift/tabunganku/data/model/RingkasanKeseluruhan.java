package com.hairift.tabunganku.data.model;

/**
 * Model ringkasan menyeluruh seluruh target tabungan untuk kartu kepala Beranda.
 */
public class RingkasanKeseluruhan {

    public final long totalTerkumpul;
    public final long totalTarget;
    public final long sisaKeseluruhan;
    public final int jumlahTargetAktif;
    public final int jumlahTargetTercapai;

    public RingkasanKeseluruhan(
        long totalTerkumpul,
        long totalTarget,
        long sisaKeseluruhan,
        int jumlahTargetAktif,
        int jumlahTargetTercapai
    ) {
        this.totalTerkumpul = totalTerkumpul;
        this.totalTarget = totalTarget;
        this.sisaKeseluruhan = sisaKeseluruhan;
        this.jumlahTargetAktif = jumlahTargetAktif;
        this.jumlahTargetTercapai = jumlahTargetTercapai;
    }
}
