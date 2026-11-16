package com.hairift.tabunganku.data.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import com.hairift.tabunganku.util.KonstantaAplikasi;

/**
 * Entitas mutasi setoran atau penarikan dana yang terkait ke satu TargetTabungan.
 */
@Entity(
    tableName = "transaksi_tabungan",
    foreignKeys = @ForeignKey(
        entity = TargetTabungan.class,
        parentColumns = "id",
        childColumns = "id_target",
        onDelete = ForeignKey.CASCADE
    ),
    indices = {
        @Index(value = {"id_target"}),
        @Index(value = {"tanggal_transaksi_epoch"})
    }
)
public class TransaksiTabungan {

    @PrimaryKey(autoGenerate = true)
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

    @ColumnInfo(name = "dibuat_pada_epoch")
    public long dibuatPadaEpoch;

    public TransaksiTabungan() {
    }

    @Ignore
    public TransaksiTabungan(
        long idTarget,
        @NonNull String jenis,
        long nominal,
        @Nullable String catatan,
        long tanggalTransaksiEpoch,
        long dibuatPadaEpoch
    ) {
        this.idTarget = idTarget;
        this.jenis = jenis;
        this.nominal = nominal;
        this.catatan = catatan;
        this.tanggalTransaksiEpoch = tanggalTransaksiEpoch;
        this.dibuatPadaEpoch = dibuatPadaEpoch;
    }

    public boolean apakahSetoran() {
        return KonstantaAplikasi.JENIS_SETOR.equals(jenis);
    }
}
