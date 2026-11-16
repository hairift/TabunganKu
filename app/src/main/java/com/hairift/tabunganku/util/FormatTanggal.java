package com.hairift.tabunganku.util;

import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

/**
 * Utilitas konversi dan pemformatan tanggal lokal berbahasa Indonesia.
 */
public final class FormatTanggal {

    private static final Locale LOKAL_INDONESIA = new Locale("in", "ID");
    private static final DateTimeFormatter FORMAT_TANGGAL_PENUH =
        DateTimeFormatter.ofPattern("d MMMM yyyy", LOKAL_INDONESIA);
    private static final DateTimeFormatter FORMAT_TANGGAL_RINGKAS =
        DateTimeFormatter.ofPattern("d MMM yyyy", LOKAL_INDONESIA);
    private static final DateTimeFormatter FORMAT_BULAN_TAHUN =
        DateTimeFormatter.ofPattern("MMM yyyy", LOKAL_INDONESIA);
    private static final DateTimeFormatter FORMAT_BULAN_PENDEK =
        DateTimeFormatter.ofPattern("MMM", LOKAL_INDONESIA);
    private static final DateTimeFormatter FORMAT_BERKAS =
        DateTimeFormatter.ofPattern("yyyyMMdd-HHmm", LOKAL_INDONESIA);

    private FormatTanggal() {
    }

    public static String keTanggalPenuh(long epochMilis) {
        if (epochMilis <= 0) {
            return "-";
        }
        LocalDate tanggal = keLocalDate(epochMilis);
        return tanggal.format(FORMAT_TANGGAL_PENUH);
    }

    public static String keTanggalRingkas(long epochMilis) {
        if (epochMilis <= 0) {
            return "-";
        }
        LocalDate tanggal = keLocalDate(epochMilis);
        return tanggal.format(FORMAT_TANGGAL_RINGKAS);
    }

    public static String keBulanTahun(long epochMilis) {
        if (epochMilis <= 0) {
            return "-";
        }
        return keLocalDate(epochMilis).format(FORMAT_BULAN_TAHUN);
    }

    public static String keLabelBulanPendek(YearMonth bulanTahun) {
        return bulanTahun.atDay(1).format(FORMAT_BULAN_PENDEK);
    }

    public static String kePenandaBerkas(long epochMilis) {
        return Instant.ofEpochMilli(epochMilis)
            .atZone(ZoneId.systemDefault())
            .toLocalDateTime()
            .format(FORMAT_BERKAS);
    }

    public static LocalDate keLocalDate(long epochMilis) {
        return Instant.ofEpochMilli(epochMilis)
            .atZone(ZoneId.systemDefault())
            .toLocalDate();
    }

    public static long dariLocalDate(LocalDate tanggal) {
        return tanggal.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    /**
     * Menghitung sisa hari dari hari ini hingga tanggal target; bernilai negatif bila sudah lewat.
     */
    public static long hitungSisaHari(long epochTargetMilis) {
        LocalDate hariIni = LocalDate.now();
        LocalDate tanggalTarget = keLocalDate(epochTargetMilis);
        return ChronoUnit.DAYS.between(hariIni, tanggalTarget);
    }
}
