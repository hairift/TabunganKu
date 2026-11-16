package com.hairift.tabunganku.data.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;

import com.hairift.tabunganku.util.KonstantaAplikasi;

/**
 * Proyeksi hasil gabungan transaksi dengan nama dan kategori target induknya.
 */
public class TransaksiDenganTarget {

    @ColumnInfo(name = "id")
    public long id;

    @ColumnInfo(name = "id_target")
    public long idTarget;

    @NonNull
    @ColumnInfo(name = "jenis")
    public String jenis = KonstantaAplikasi.JENIS_SETOR;

    @ColumnInfo(name = "nominal")
    public long nominal;

    @Nullable
    @ColumnInfo(name = "catatan")
    public String catatan;

    @ColumnInfo(name = "tanggal_transaksi_epoch")
    public long tanggalTransaksiEpoch;

    @NonNull
    @ColumnInfo(name = "nama_target")
    public String namaTarget = "";

    @NonNull
    @ColumnInfo(name = "kategori")
    public String kategori = "";

    public boolean apakahSetoran() {
        return KonstantaAplikasi.JENIS_SETOR.equals(jenis);
    }
}
