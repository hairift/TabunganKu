package com.hairift.tabunganku;

import com.hairift.tabunganku.data.model.TargetTabungan;
import com.hairift.tabunganku.util.KalkulatorTarget;

import org.junit.Test;

import java.time.LocalDate;
import java.time.ZoneId;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Pengujian unit untuk kalkulasi progres, sisa target, dan rekomendasi setoran.
 */
public class KalkulatorTargetTest {

    @Test
    public void hitungPersentase_membatasiRentangNolHinggaSeratus() {
        assertEquals(0, KalkulatorTarget.hitungPersentase(0L, 1_000_000L));
        assertEquals(25, KalkulatorTarget.hitungPersentase(250_000L, 1_000_000L));
        assertEquals(100, KalkulatorTarget.hitungPersentase(1_200_000L, 1_000_000L));
    }

    @Test
    public void hitungSisa_tidakPernahNegatifSaatMelebihiTarget() {
        assertEquals(750_000L, KalkulatorTarget.hitungSisa(250_000L, 1_000_000L));
        assertEquals(0L, KalkulatorTarget.hitungSisa(1_500_000L, 1_000_000L));
    }

    @Test
    public void buatRekomendasiNabung_memberiSaranBulananUntukTenggatPanjang() {
        long masaDepanEpoch = LocalDate.now().plusMonths(6)
            .atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
        TargetTabungan target = new TargetTabungan(
            "DP Rumah Minimalis",
            "Rumah & Properti",
            12_000_000L,
            0L,
            null,
            null,
            masaDepanEpoch,
            System.currentTimeMillis()
        );
        String saran = KalkulatorTarget.buatRekomendasiNabung(target);
        assertTrue(saran.contains("per bulan"));
    }

    @Test
    public void buatRekomendasiNabung_menandaiTargetTercapai() {
        TargetTabungan target = new TargetTabungan(
            "Dana Darurat",
            "Dana Darurat",
            5_000_000L,
            5_000_000L,
            null,
            null,
            0L,
            System.currentTimeMillis()
        );
        assertEquals("Target ini sudah tercapai penuh.", KalkulatorTarget.buatRekomendasiNabung(target));
    }
}
