package com.hairift.tabunganku.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.hairift.tabunganku.data.model.TargetTabungan;

import java.util.List;

/**
 * Akses basis data SQLite untuk entitas TargetTabungan.
 */
@Dao
public interface TargetTabunganDao {

    @Query("SELECT * FROM target_tabungan ORDER BY diarsipkan ASC, diperbarui_pada_epoch DESC, id DESC")
    LiveData<List<TargetTabungan>> pantauSemuaTarget();

    @Query("SELECT * FROM target_tabungan ORDER BY diarsipkan ASC, id ASC")
    List<TargetTabungan> ambilSemuaTargetSinkron();

    @Query("SELECT * FROM target_tabungan WHERE id = :idTarget LIMIT 1")
    LiveData<TargetTabungan> pantauTargetBerdasarkanId(long idTarget);

    @Query("SELECT * FROM target_tabungan WHERE id = :idTarget LIMIT 1")
    TargetTabungan ambilTargetBerdasarkanIdSinkron(long idTarget);

    @Insert
    long sisipkan(TargetTabungan target);

    @Update
    void perbarui(TargetTabungan target);

    @Delete
    void hapus(TargetTabungan target);

    @Query("DELETE FROM target_tabungan")
    void hapusSemua();
}
