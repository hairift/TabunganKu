package com.hairift.tabunganku;

import com.hairift.tabunganku.util.FormatMataUang;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Pengujian unit untuk pemformatan dan penguraian angka Rupiah.
 */
public class FormatMataUangTest {

    @Test
    public void formatRupiah_nilaiPositifDanNol_sesuaiStandarIndonesia() {
        assertEquals("Rp 0", FormatMataUang.keRupiah(0L));
        assertEquals("Rp 50.000", FormatMataUang.keRupiah(50_000L));
        assertEquals("Rp 150.000.000", FormatMataUang.keRupiah(150_000_000L));
    }

    @Test
    public void formatRupiah_nilaiNegatif_menampilkanTandaMinusDiDepan() {
        assertEquals("-Rp 250.000", FormatMataUang.keRupiah(-250_000L));
    }

    @Test
    public void uraiDariTeks_mengekstrakDigitDenganBenar() {
        assertEquals(0L, FormatMataUang.uraiDariTeks(""));
        assertEquals(0L, FormatMataUang.uraiDariTeks(null));
        assertEquals(1_500_000L, FormatMataUang.uraiDariTeks("Rp 1.500.000"));
        assertEquals(75_000L, FormatMataUang.uraiDariTeks("75.000"));
    }

    @Test
    public void formatRupiahRingkas_menyingkatJutaDanRibu() {
        assertEquals("Rp 500rb", FormatMataUang.keRupiahRingkas(500_000L));
        assertEquals("Rp 2,5jt", FormatMataUang.keRupiahRingkas(2_500_000L));
        assertEquals("Rp 1M", FormatMataUang.keRupiahRingkas(1_000_000_000L));
    }
}
