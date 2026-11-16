package com.hairift.tabunganku.util;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * Pemformat dan pengurai angka mata uang Rupiah tanpa galat floating-point.
 */
public final class FormatMataUang {

    private static final Locale LOKAL_INDONESIA = new Locale("in", "ID");

    private FormatMataUang() {
    }

    /**
     * Memformat nominal bulat menjadi string Rupiah penuh, contoh "Rp 1.500.000".
     */
    public static String keRupiah(long nominal) {
        long nilaiAbsolut = Math.abs(nominal);
        DecimalFormatSymbols simbol = new DecimalFormatSymbols(LOKAL_INDONESIA);
        simbol.setGroupingSeparator('.');
        DecimalFormat pemformat = new DecimalFormat("#,###", simbol);
        String angkaTerformat = pemformat.format(nilaiAbsolut);
        return nominal < 0 ? "-Rp " + angkaTerformat : "Rp " + angkaTerformat;
    }

    /**
     * Memformat nominal menjadi angka ribuan tanpa awalan "Rp" untuk kolom input.
     */
    public static String keAngkaRibuan(long nominal) {
        if (nominal <= 0) {
            return "";
        }
        DecimalFormatSymbols simbol = new DecimalFormatSymbols(LOKAL_INDONESIA);
        simbol.setGroupingSeparator('.');
        DecimalFormat pemformat = new DecimalFormat("#,###", simbol);
        return pemformat.format(nominal);
    }

    /**
     * Meringkas nominal besar untuk label sumbu grafik agar tidak berdesakan di layar sempit.
     */
    public static String keRupiahRingkas(long nominal) {
        long nilaiAbsolut = Math.abs(nominal);
        String awalan = nominal < 0 ? "-Rp " : "Rp ";
        if (nilaiAbsolut >= 1_000_000_000L) {
            return awalan + formatDesimalRingkas(nilaiAbsolut / 1_000_000_000.0) + "M";
        }
        if (nilaiAbsolut >= 1_000_000L) {
            return awalan + formatDesimalRingkas(nilaiAbsolut / 1_000_000.0) + "jt";
        }
        if (nilaiAbsolut >= 1_000L) {
            return awalan + formatDesimalRingkas(nilaiAbsolut / 1_000.0) + "rb";
        }
        return keRupiah(nominal);
    }

    /**
     * Mengambil hanya digit angka dari teks masukan pengguna menjadi nilai Rupiah bulat.
     */
    public static long uraiDariTeks(String teksMasukan) {
        if (teksMasukan == null || teksMasukan.trim().isEmpty()) {
            return 0L;
        }
        StringBuilder hanyaDigit = new StringBuilder();
        for (int indeks = 0; indeks < teksMasukan.length(); indeks++) {
            char karakter = teksMasukan.charAt(indeks);
            if (Character.isDigit(karakter)) {
                hanyaDigit.append(karakter);
            }
        }
        if (hanyaDigit.length() == 0) {
            return 0L;
        }
        try {
            return Long.parseLong(hanyaDigit.toString());
        } catch (NumberFormatException galat) {
            // Mengatasi angka melebihi batas tipe long saat input sangat panjang.
            return Long.MAX_VALUE;
        }
    }

    private static String formatDesimalRingkas(double nilai) {
        DecimalFormatSymbols simbol = new DecimalFormatSymbols(LOKAL_INDONESIA);
        simbol.setDecimalSeparator(',');
        DecimalFormat pemformat = new DecimalFormat("0.#", simbol);
        return pemformat.format(nilai);
    }
}
