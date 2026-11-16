package com.hairift.tabunganku.data.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

/**
 * Entitas target tabungan pengguna beserta foto motivasi dan progres saldo.
 */
@Entity(tableName = "target_tabungan")
public class TargetTabungan {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    public long id;

    @NonNull
    @ColumnInfo(name = "nama_target")
    public String namaTarget = "";

    @NonNull
    @ColumnInfo(name = "kategori")
    public String kategori = "Lainnya";

    @ColumnInfo(name = "nominal_target")
    public long nominalTarget;

    @ColumnInfo(name = "saldo_terkumpul")
    public long saldoTerkumpul;

    @Nullable
    @ColumnInfo(name = "path_foto_motivasi")
    public String pathFotoMotivasi;

    @Nullable
    @ColumnInfo(name = "catatan_motivasi")
    public String catatanMotivasi;

    @ColumnInfo(name = "tanggal_target_epoch")
    public long tanggalTargetEpoch;

    @ColumnInfo(name = "diarsipkan")
    public boolean diarsipkan;

    @ColumnInfo(name = "dibuat_pada_epoch")
    public long dibuatPadaEpoch;

    @ColumnInfo(name = "diperbarui_pada_epoch")
    public long diperbaruiPadaEpoch;

    public TargetTabungan() {
    }

    @Ignore
    public TargetTabungan(
        @NonNull String namaTarget,
        @NonNull String kategori,
        long nominalTarget,
        long saldoTerkumpul,
        @Nullable String pathFotoMotivasi,
        @Nullable String catatanMotivasi,
        long tanggalTargetEpoch,
        long waktuSekarangEpoch
    ) {
        this.namaTarget = namaTarget;
        this.kategori = kategori;
        this.nominalTarget = nominalTarget;
        this.saldoTerkumpul = saldoTerkumpul;
        this.pathFotoMotivasi = pathFotoMotivasi;
        this.catatanMotivasi = catatanMotivasi;
        this.tanggalTargetEpoch = tanggalTargetEpoch;
        this.diarsipkan = false;
        this.dibuatPadaEpoch = waktuSekarangEpoch;
        this.diperbaruiPadaEpoch = waktuSekarangEpoch;
    }

    /**
     * Menentukan apakah target sudah terpenuhi 100% atau lebih.
     */
    public boolean apakahTercapai() {
        return nominalTarget > 0 && saldoTerkumpul >= nominalTarget;
    }
}
