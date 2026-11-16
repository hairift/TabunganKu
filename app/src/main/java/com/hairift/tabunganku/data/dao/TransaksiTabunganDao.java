package com.hairift.tabunganku.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import com.hairift.tabunganku.data.model.TransaksiDenganTarget;
import com.hairift.tabunganku.data.model.TransaksiTabungan;

import java.util.List;

/**
 * Akses basis data SQLite untuk entitas TransaksiTabungan dan kueri gabungan riwayat.
 */
@Dao
public interface TransaksiTabunganDao {

    @Query("SELECT * FROM transaksi_tabungan WHERE id_target = :idTarget "
        + "ORDER BY tanggal_transaksi_epoch DESC, id DESC")
    LiveData<List<TransaksiTabungan>> pantauTransaksiTarget(long idTarget);

    @Query("SELECT * FROM transaksi_tabungan WHERE id_target = :idTarget "
        + "ORDER BY tanggal_transaksi_epoch DESC, id DESC")
    List<TransaksiTabungan> ambilTransaksiTargetSinkron(long idTarget);

    @Query("SELECT t.id, t.id_target, t.jenis, t.nominal, t.catatan, t.tanggal_transaksi_epoch, "
        + "g.nama_target, g.kategori "
        + "FROM transaksi_tabungan t INNER JOIN target_tabungan g ON t.id_target = g.id "
        + "ORDER BY t.tanggal_transaksi_epoch DESC, t.id DESC")
    LiveData<List<TransaksiDenganTarget>> pantauSemuaTransaksiDenganTarget();

    @Query("SELECT t.id, t.id_target, t.jenis, t.nominal, t.catatan, t.tanggal_transaksi_epoch, "
        + "g.nama_target, g.kategori "
        + "FROM transaksi_tabungan t INNER JOIN target_tabungan g ON t.id_target = g.id "
        + "ORDER BY t.tanggal_transaksi_epoch ASC, t.id ASC")
    List<TransaksiDenganTarget> ambilSemuaTransaksiDenganTargetSinkron();

    @Query("SELECT * FROM transaksi_tabungan ORDER BY id ASC")
    List<TransaksiTabungan> ambilSemuaTransaksiMentahSinkron();

    @Insert
    long sisipkan(TransaksiTabungan transaksi);

    @Delete
    void hapus(TransaksiTabungan transaksi);

    @Query("DELETE FROM transaksi_tabungan")
    void hapusSemua();
}
