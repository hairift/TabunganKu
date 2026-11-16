package com.hairift.tabunganku;

import android.app.Application;

import com.hairift.tabunganku.data.lokal.PengelolaPreferensi;

/**
 * Titik masuk aplikasi yang menerapkan preferensi tema sejak proses aplikasi dimulai.
 */
public class AplikasiTabunganKu extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        PengelolaPreferensi preferensi = new PengelolaPreferensi(this);
        PengelolaPreferensi.terapkanTema(preferensi.ambilPilihanTema());
    }
}
