package com.hairift.tabunganku.data.lokal;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.hairift.tabunganku.data.dao.TargetTabunganDao;
import com.hairift.tabunganku.data.dao.TransaksiTabunganDao;
import com.hairift.tabunganku.data.model.TargetTabungan;
import com.hairift.tabunganku.data.model.TransaksiTabungan;

/**
 * Definisi basis data lokal Room SQLite untuk aplikasi TabunganKu.
 */
@Database(
    entities = {TargetTabungan.class, TransaksiTabungan.class},
    version = 1,
    exportSchema = true
)
public abstract class BasisDataTabungan extends RoomDatabase {

    private static volatile BasisDataTabungan instansi;

    public abstract TargetTabunganDao targetTabunganDao();

    public abstract TransaksiTabunganDao transaksiTabunganDao();

    /**
     * Mengembalikan instansi singleton thread-safe dari basis data lokal.
     */
    public static BasisDataTabungan ambilInstansi(Context konteks) {
        if (instansi == null) {
            synchronized (BasisDataTabungan.class) {
                if (instansi == null) {
                    instansi = Room.databaseBuilder(
                            konteks.getApplicationContext(),
                            BasisDataTabungan.class,
                            "tabunganku_lokal.db"
                        )
                        .build();
                }
            }
        }
        return instansi;
    }
}
