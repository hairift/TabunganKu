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
 * Grafik cincin proporsional untuk menunjukkan pembagian dana terkumpul antar target.
 */
public class GrafikDonutTargetView extends View {

    public static final class IrisanTarget {
        public final String nama;
        public final long nilai;
        public final int warna;

        public IrisanTarget(String nama, long nilai, int warna) {
            this.nama = nama;
            this.nilai = nilai;
            this.warna = warna;
        }
    }

    private final List<IrisanTarget> daftarIrisan = new ArrayList<>();
    private final Paint kuasBusur = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint kuasJalurKosong = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint kuasTeksJudul = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint kuasTeksTotal = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF kotakLingkar = new RectF();
    private long totalNilai;

    public GrafikDonutTargetView(Context konteks) {
        super(konteks);
        siapkan(konteks);
    }

    public GrafikDonutTargetView(Context konteks, @Nullable AttributeSet atribut) {
        super(konteks, atribut);
        siapkan(konteks);
    }

    public GrafikDonutTargetView(Context konteks, @Nullable AttributeSet atribut, int gaya) {
        super(konteks, atribut, gaya);
        siapkan(konteks);
    }

    private void siapkan(Context konteks) {
        float densitas = getResources().getDisplayMetrics().density;
        float skalaTeks = getResources().getDisplayMetrics().scaledDensity;

        kuasBusur.setStyle(Paint.Style.STROKE);
        kuasBusur.setStrokeCap(Paint.Cap.BUTT);
        kuasBusur.setStrokeWidth(24f * densitas);

        kuasJalurKosong.setStyle(Paint.Style.STROKE);
        kuasJalurKosong.setStrokeWidth(24f * densitas);
        kuasJalurKosong.setColor(ContextCompat.getColor(konteks, R.color.warna_permukaan_sekunder));

        kuasTeksJudul.setColor(ContextCompat.getColor(konteks, R.color.warna_teks_sekunder));
        kuasTeksJudul.setTextSize(11f * skalaTeks);
        kuasTeksJudul.setTextAlign(Paint.Align.CENTER);

        kuasTeksTotal.setColor(ContextCompat.getColor(konteks, R.color.warna_teks_utama));
        kuasTeksTotal.setTextSize(14f * skalaTeks);
        kuasTeksTotal.setTextAlign(Paint.Align.CENTER);

        try {
            kuasTeksJudul.setTypeface(ResourcesCompat.getFont(konteks, R.font.plus_jakarta_medium));
            kuasTeksTotal.setTypeface(ResourcesCompat.getFont(konteks, R.font.plus_jakarta_bold));
        } catch (Exception ignored) {
            // Menjaga kompatibilitas pratinjau layout XML.
        }
    }

    public void aturData(List<IrisanTarget> irisanBaru) {
        daftarIrisan.clear();
        totalNilai = 0L;
        if (irisanBaru != null) {
            for (IrisanTarget item : irisanBaru) {
                if (item.nilai > 0L) {
                    daftarIrisan.add(item);
                    totalNilai += item.nilai;
                }
            }
        }
        setContentDescription("Sebaran total dana terkumpul " + FormatMataUang.keRupiah(totalNilai));
        invalidate();
    }

    @Override
    protected void onDraw(Canvas kanvas) {
        super.onDraw(kanvas);
        float ukuran = Math.min(getWidth(), getHeight());
        float ketebalan = kuasBusur.getStrokeWidth();
        float margin = ketebalan / 2f + 6f * getResources().getDisplayMetrics().density;
        float pusatX = getWidth() / 2f;
        float pusatY = getHeight() / 2f;
        float jari = (ukuran / 2f) - margin;
        if (jari <= 0f) {
            return;
        }

        kotakLingkar.set(pusatX - jari, pusatY - jari, pusatX + jari, pusatY + jari);
        kanvas.drawArc(kotakLingkar, 0f, 360f, false, kuasJalurKosong);

        if (totalNilai > 0L && !daftarIrisan.isEmpty()) {
            float sudutMulai = -90f;
            float celahDerajat = daftarIrisan.size() > 1 ? 2.5f : 0f;
            for (IrisanTarget item : daftarIrisan) {
                float bentang = ((float) item.nilai / (float) totalNilai) * 360f;
                float bentangTerlihat = Math.max(1f, bentang - celahDerajat);
                kuasBusur.setColor(item.warna);
                kanvas.drawArc(kotakLingkar, sudutMulai, bentangTerlihat, false, kuasBusur);
                sudutMulai += bentang;
            }
        }

        kanvas.drawText("Total Dana", pusatX, pusatY - 4f * getResources().getDisplayMetrics().density, kuasTeksJudul);
        String ringkasTotal = FormatMataUang.keRupiahRingkas(totalNilai);
        kanvas.drawText(ringkasTotal, pusatX, pusatY + 16f * getResources().getDisplayMetrics().density, kuasTeksTotal);
    }
}
