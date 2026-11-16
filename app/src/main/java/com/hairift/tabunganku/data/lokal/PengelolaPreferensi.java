package com.hairift.tabunganku.data.lokal;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;

import com.hairift.tabunganku.util.KonstantaAplikasi;

/**
 * Penyimpanan preferensi antarmuka pengguna seperti tema dan jadwal pengingat lokal.
 */
public class PengelolaPreferensi {

    private static final String NAMA_PREFERENSI = "preferensi_tabunganku";
    private static final String KUNCI_TEMA = "kunci_tema";
    private static final String KUNCI_PENGINGAT_AKTIF = "kunci_pengingat_aktif";

    private final SharedPreferences preferensi;

    public PengelolaPreferensi(Context konteks) {
        this.preferensi = konteks.getApplicationContext()
            .getSharedPreferences(NAMA_PREFERENSI, Context.MODE_PRIVATE);
    }

    public int ambilPilihanTema() {
        return preferensi.getInt(KUNCI_TEMA, KonstantaAplikasi.TEMA_SISTEM);
    }

    public void simpanPilihanTema(int kodeTema) {
        preferensi.edit().putInt(KUNCI_TEMA, kodeTema).apply();
        terapkanTema(kodeTema);
    }

    public boolean apakahPengingatAktif() {
        return preferensi.getBoolean(KUNCI_PENGINGAT_AKTIF, false);
    }

    public void simpanPengingatAktif(boolean aktif) {
        preferensi.edit().putBoolean(KUNCI_PENGINGAT_AKTIF, aktif).apply();
    }

    /**
     * Menerapkan mode terang, gelap, atau mengikuti sistem ke AppCompatDelegate.
     */
    public static void terapkanTema(int kodeTema) {
        switch (kodeTema) {
            case KonstantaAplikasi.TEMA_TERANG:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case KonstantaAplikasi.TEMA_GELAP:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
            default:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                break;
        }
    }
}
