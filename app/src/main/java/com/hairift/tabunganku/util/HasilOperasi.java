package com.hairift.tabunganku.util;

/**
 * Pembungkus hasil operasi latar belakang agar keberhasilan dan pesan kesalahan jelas.
 *
 * @param <T> tipe data hasil ketika operasi berhasil
 */
public final class HasilOperasi<T> {

    private final boolean berhasil;
    private final T data;
    private final String pesanGalat;

    private HasilOperasi(boolean berhasil, T data, String pesanGalat) {
        this.berhasil = berhasil;
        this.data = data;
        this.pesanGalat = pesanGalat;
    }

    public static <T> HasilOperasi<T> sukses(T data) {
        return new HasilOperasi<>(true, data, null);
    }

    public static <T> HasilOperasi<T> gagal(String pesanGalat) {
        return new HasilOperasi<>(false, null, pesanGalat);
    }

    public boolean apakahBerhasil() {
        return berhasil;
    }

    public T ambilData() {
        return data;
    }

    public String ambilPesanGalat() {
        return pesanGalat;
    }
}
