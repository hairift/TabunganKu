package com.hairift.tabunganku.ui.laporan;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;

import com.hairift.tabunganku.R;
import com.hairift.tabunganku.util.FormatMataUang;

import java.util.ArrayList;
import java.util.List;

/**
 * Grafik batang kustom untuk menampilkan total setoran bersih tiap bulan selama 6 bulan terakhir.
 */
public class GrafikBatangBulananView extends View {

    public static final class EntriBulan {
        public final String label;
        public final long nilai;

        public EntriBulan(String label, long nilai) {
            this.label = label;
            this.nilai = nilai;
        }
    }

    private final List<EntriBulan> daftarData = new ArrayList<>();
    private final Paint kuasBatangPrimer = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint kuasBatangAksen = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint kuasGaris = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint kuasTeksLabel = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint kuasTeksNilai = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF kotakBatang = new RectF();

    public GrafikBatangBulananView(Context konteks) {
        super(konteks);
        siapkan(konteks);
    }

    public GrafikBatangBulananView(Context konteks, @Nullable AttributeSet atribut) {
        super(konteks, atribut);
        siapkan(konteks);
    }

    public GrafikBatangBulananView(Context konteks, @Nullable AttributeSet atribut, int gaya) {
        super(konteks, atribut, gaya);
        siapkan(konteks);
    }

    private void siapkan(Context konteks) {
        float densitas = getResources().getDisplayMetrics().density;
        float skalaTeks = getResources().getDisplayMetrics().scaledDensity;

        kuasBatangPrimer.setColor(ContextCompat.getColor(konteks, R.color.warna_primer));
        kuasBatangAksen.setColor(ContextCompat.getColor(konteks, R.color.warna_aksen));

        kuasGaris.setColor(ContextCompat.getColor(konteks, R.color.warna_garis_batas));
        kuasGaris.setStrokeWidth(1.2f * densitas);

        kuasTeksLabel.setColor(ContextCompat.getColor(konteks, R.color.warna_teks_sekunder));
        kuasTeksLabel.setTextSize(11f * skalaTeks);
        kuasTeksLabel.setTextAlign(Paint.Align.CENTER);

        kuasTeksNilai.setColor(ContextCompat.getColor(konteks, R.color.warna_teks_utama));
        kuasTeksNilai.setTextSize(10f * skalaTeks);
        kuasTeksNilai.setTextAlign(Paint.Align.CENTER);

        try {
            kuasTeksLabel.setTypeface(ResourcesCompat.getFont(konteks, R.font.plus_jakarta_medium));
            kuasTeksNilai.setTypeface(ResourcesCompat.getFont(konteks, R.font.plus_jakarta_semibold));
        } catch (Exception ignored) {
            // Memastikan pratinjau layout editor tetap berjalan walaupun font kustom belum dimuat.
        }
    }

    public void aturData(List<EntriBulan> dataBaru) {
        daftarData.clear();
        if (dataBaru != null) {
            daftarData.addAll(dataBaru);
        }
        StringBuilder ringkasan = new StringBuilder("Grafik batang setoran bulanan: ");
        for (EntriBulan item : daftarData) {
            ringkasan.append(item.label).append(" ")
                .append(FormatMataUang.keRupiah(item.nilai)).append(". ");
        }
        setContentDescription(ringkasan.toString());
        invalidate();
    }

    @Override
    protected void onDraw(Canvas kanvas) {
        super.onDraw(kanvas);
        if (daftarData.isEmpty()) {
            return;
        }

        float densitas = getResources().getDisplayMetrics().density;
        float kiri = getPaddingLeft() + 8f * densitas;
        float kanan = getWidth() - getPaddingRight() - 8f * densitas;
        float atas = getPaddingTop() + 24f * densitas;
        float bawahGaris = getHeight() - getPaddingBottom() - 26f * densitas;
        float tinggiArea = Math.max(24f * densitas, bawahGaris - atas);

        long nilaiMaks = 1L;
        for (EntriBulan item : daftarData) {
            if (item.nilai > nilaiMaks) {
                nilaiMaks = item.nilai;
            }
        }

        // Garis dasar datar sebagai tumpuan batang grafik.
        kanvas.drawLine(kiri, bawahGaris, kanan, bawahGaris, kuasGaris);

        int jumlah = daftarData.size();
        float lebarSlot = (kanan - kiri) / jumlah;
        float lebarBatang = Math.min(34f * densitas, lebarSlot * 0.56f);
        float radiusSudut = 7f * densitas;

        for (int i = 0; i < jumlah; i++) {
            EntriBulan item = daftarData.get(i);
            float pusatX = kiri + (i * lebarSlot) + (lebarSlot / 2f);
            float rasio = Math.max(0f, (float) item.nilai / (float) nilaiMaks);
            float tinggiBatang = item.nilai > 0 ? Math.max(8f * densitas, tinggiArea * rasio) : 3f * densitas;

            float x1 = pusatX - (lebarBatang / 2f);
            float x2 = pusatX + (lebarBatang / 2f);
            float y1 = bawahGaris - tinggiBatang;

            kotakBatang.set(x1, y1, x2, bawahGaris);
            // Sorot bulan terbaru dengan aksen terakota, bulan lainnya dengan hijau pinus.
            Paint kuasAktif = (i == jumlah - 1) ? kuasBatangAksen : kuasBatangPrimer;
            kanvas.drawRoundRect(kotakBatang, radiusSudut, radiusSudut, kuasAktif);

            if (item.nilai > 0) {
                String labelNilai = FormatMataUang.keRupiahRingkas(item.nilai).replace("Rp ", "");
                kanvas.drawText(labelNilai, pusatX, y1 - 6f * densitas, kuasTeksNilai);
            }
            kanvas.drawText(item.label, pusatX, bawahGaris + 18f * densitas, kuasTeksLabel);
        }
    }
}
