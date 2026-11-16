package com.hairift.tabunganku.ui.laporan;

import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.hairift.tabunganku.R;
import com.hairift.tabunganku.data.model.TargetTabungan;
import com.hairift.tabunganku.data.model.TransaksiDenganTarget;
import com.hairift.tabunganku.data.repositori.RepositoriTabungan;
import com.hairift.tabunganku.databinding.FragmenLaporanBinding;
import com.hairift.tabunganku.ekspor.PengelolaEkspor;
import com.hairift.tabunganku.util.FormatMataUang;
import com.hairift.tabunganku.util.FormatTanggal;

import java.io.IOException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Menyajikan analitik arus tabungan bulanan, sebaran dana per target, serta ekspor Excel dan CSV.
 */
public class FragmenLaporan extends Fragment {

    private static final int PERIODE_BULAN_INI = 0;
    private static final int PERIODE_TIGA_BULAN = 1;
    private static final int PERIODE_SEMUA = 2;

    private static final int CAKUPAN_SEMUA = 0;
    private static final int CAKUPAN_AKTIF = 1;
    private static final int CAKUPAN_TERCAPAI = 2;

    private FragmenLaporanBinding ikat;
    private final List<TargetTabungan> semuaTarget = new ArrayList<>();
    private final List<TransaksiDenganTarget> semuaTransaksi = new ArrayList<>();
    private int periodeTerpilih = PERIODE_BULAN_INI;
    private int cakupanEkspor = CAKUPAN_SEMUA;
    private PengelolaEkspor.HasilEkspor hasilEksporTerakhir;

    private final ActivityResultLauncher<String> peluncurSimpanExcel =
        registerForActivityResult(
            new ActivityResultContracts.CreateDocument(PengelolaEkspor.MIME_EXCEL),
            uri -> salinEksporKeLokasiLain(uri)
        );

    private final ActivityResultLauncher<String> peluncurSimpanCsv =
        registerForActivityResult(
            new ActivityResultContracts.CreateDocument(PengelolaEkspor.MIME_CSV),
            uri -> salinEksporKeLokasiLain(uri)
        );

    @Nullable
    @Override
    public View onCreateView(
        @NonNull LayoutInflater pengembang,
        @Nullable ViewGroup wadah,
        @Nullable Bundle keadaanTersimpan
    ) {
        ikat = FragmenLaporanBinding.inflate(pengembang, wadah, false);
        return ikat.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View tampilan, @Nullable Bundle keadaanTersimpan) {
        super.onViewCreated(tampilan, keadaanTersimpan);

        ikat.grupChipPeriode.setOnCheckedStateChangeListener((grup, daftarId) -> {
            if (daftarId.isEmpty()) {
                return;
            }
            int id = daftarId.get(0);
            if (id == R.id.chip_periode_tiga_bulan) {
                periodeTerpilih = PERIODE_TIGA_BULAN;
            } else if (id == R.id.chip_periode_semua) {
                periodeTerpilih = PERIODE_SEMUA;
            } else {
                periodeTerpilih = PERIODE_BULAN_INI;
            }
            perbaruiRingkasanPeriode();
        });

        ikat.grupChipCakupanEkspor.setOnCheckedStateChangeListener((grup, daftarId) -> {
            if (daftarId.isEmpty()) {
                return;
            }
            int id = daftarId.get(0);
            if (id == R.id.chip_cakupan_aktif) {
                cakupanEkspor = CAKUPAN_AKTIF;
            } else if (id == R.id.chip_cakupan_tercapai) {
                cakupanEkspor = CAKUPAN_TERCAPAI;
            } else {
                cakupanEkspor = CAKUPAN_SEMUA;
            }
            perbaruiPratinjauEkspor();
        });

        ikat.tombolUnduhXlsx.setOnClickListener(v -> jalankanEkspor(true));
        ikat.tombolUnduhCsv.setOnClickListener(v -> jalankanEkspor(false));

        RepositoriTabungan repositori = RepositoriTabungan.ambilInstansi(requireContext());
        repositori.pantauSemuaTarget().observe(getViewLifecycleOwner(), daftar -> {
            semuaTarget.clear();
            if (daftar != null) {
                semuaTarget.addAll(daftar);
            }
            perbaruiGrafikSebaran();
            perbaruiPratinjauEkspor();
        });

        repositori.pantauSemuaTransaksiDenganTarget().observe(getViewLifecycleOwner(), daftar -> {
            semuaTransaksi.clear();
            if (daftar != null) {
                semuaTransaksi.addAll(daftar);
            }
            perbaruiRingkasanPeriode();
            perbaruiGrafikBulanan();
            perbaruiPratinjauEkspor();
        });
    }

    private void perbaruiRingkasanPeriode() {
        long batasAwalEpoch = hitungBatasAwalPeriode();
        long totalSetor = 0L;
        long totalTarik = 0L;

        for (TransaksiDenganTarget tr : semuaTransaksi) {
            if (tr.tanggalTransaksiEpoch < batasAwalEpoch) {
                continue;
            }
            if (tr.apakahSetoran()) {
                totalSetor += tr.nominal;
            } else {
                totalTarik += tr.nominal;
            }
        }

        long arusBersih = totalSetor - totalTarik;
        ikat.teksTotalSetorLaporan.setText(FormatMataUang.keRupiah(totalSetor));
        ikat.teksTotalTarikLaporan.setText(FormatMataUang.keRupiah(totalTarik));
        ikat.teksArusBersihLaporan.setText(FormatMataUang.keRupiah(arusBersih));
    }

    private long hitungBatasAwalPeriode() {
        if (periodeTerpilih == PERIODE_SEMUA) {
            return 0L;
        }
        LocalDate hariIni = LocalDate.now();
        LocalDate awal = periodeTerpilih == PERIODE_BULAN_INI
            ? hariIni.withDayOfMonth(1)
            : hariIni.minusMonths(2).withDayOfMonth(1);
        return FormatTanggal.dariLocalDate(awal);
    }

    private void perbaruiGrafikBulanan() {
        YearMonth bulanSekarang = YearMonth.now();
        List<YearMonth> daftarBulan = new ArrayList<>();
        Map<YearMonth, Long> totalPerBulan = new HashMap<>();

        for (int mundur = 5; mundur >= 0; mundur--) {
            YearMonth ym = bulanSekarang.minusMonths(mundur);
            daftarBulan.add(ym);
            totalPerBulan.put(ym, 0L);
        }

        boolean adaSetoranEnamBulan = false;
        for (TransaksiDenganTarget tr : semuaTransaksi) {
            if (!tr.apakahSetoran()) {
                continue;
            }
            LocalDate tgl = FormatTanggal.keLocalDate(tr.tanggalTransaksiEpoch);
            YearMonth ym = YearMonth.from(tgl);
            if (totalPerBulan.containsKey(ym)) {
                totalPerBulan.put(ym, totalPerBulan.get(ym) + tr.nominal);
                adaSetoranEnamBulan = true;
            }
        }

        if (!adaSetoranEnamBulan) {
            ikat.teksKosongGrafikBulanan.setVisibility(View.VISIBLE);
            ikat.grafikBatangBulanan.setVisibility(View.GONE);
            return;
        }

        ikat.teksKosongGrafikBulanan.setVisibility(View.GONE);
        ikat.grafikBatangBulanan.setVisibility(View.VISIBLE);

        List<GrafikBatangBulananView.EntriBulan> entri = new ArrayList<>();
        for (YearMonth ym : daftarBulan) {
            entri.add(new GrafikBatangBulananView.EntriBulan(
                FormatTanggal.keLabelBulanPendek(ym),
                totalPerBulan.get(ym)
            ));
        }
        ikat.grafikBatangBulanan.aturData(entri);
    }

    private void perbaruiGrafikSebaran() {
        int[] paletGrafik = new int[]{
            ContextCompat.getColor(requireContext(), R.color.warna_grafik_1),
            ContextCompat.getColor(requireContext(), R.color.warna_grafik_2),
            ContextCompat.getColor(requireContext(), R.color.warna_grafik_3),
            ContextCompat.getColor(requireContext(), R.color.warna_grafik_4),
            ContextCompat.getColor(requireContext(), R.color.warna_grafik_5),
            ContextCompat.getColor(requireContext(), R.color.warna_grafik_6)
        };

        List<TargetTabungan> targetBersaldo = new ArrayList<>();
        long totalSaldo = 0L;
        for (TargetTabungan t : semuaTarget) {
            if (!t.diarsipkan && t.saldoTerkumpul > 0L) {
                targetBersaldo.add(t);
                totalSaldo += t.saldoTerkumpul;
            }
        }

        if (targetBersaldo.isEmpty() || totalSaldo <= 0L) {
            ikat.teksKosongSebaran.setVisibility(View.VISIBLE);
            ikat.wadahKontenSebaran.setVisibility(View.GONE);
            return;
        }

        ikat.teksKosongSebaran.setVisibility(View.GONE);
        ikat.wadahKontenSebaran.setVisibility(View.VISIBLE);

        List<GrafikDonutTargetView.IrisanTarget> daftarIrisan = new ArrayList<>();
        ikat.wadahLegendaSebaran.removeAllViews();

        for (int i = 0; i < targetBersaldo.size(); i++) {
            TargetTabungan t = targetBersaldo.get(i);
            int warna = paletGrafik[i % paletGrafik.length];
            daftarIrisan.add(new GrafikDonutTargetView.IrisanTarget(t.namaTarget, t.saldoTerkumpul, warna));
            tambahBarisLegenda(t.namaTarget, t.saldoTerkumpul, totalSaldo, warna);
        }
        ikat.grafikDonutTarget.aturData(daftarIrisan);
    }

    private void tambahBarisLegenda(String nama, long saldo, long total, int warna) {
        LinearLayout baris = new LinearLayout(requireContext());
        baris.setOrientation(LinearLayout.HORIZONTAL);
        baris.setGravity(Gravity.CENTER_VERTICAL);
        baris.setPadding(0, dp(4), 0, dp(4));

        View kotakWarna = new View(requireContext());
        LinearLayout.LayoutParams paramKotak = new LinearLayout.LayoutParams(dp(12), dp(12));
        paramKotak.setMarginEnd(dp(10));
        kotakWarna.setLayoutParams(paramKotak);
        GradientDrawable bentuk = new GradientDrawable();
        bentuk.setShape(GradientDrawable.OVAL);
        bentuk.setColor(warna);
        kotakWarna.setBackground(bentuk);

        TextView teksNama = new TextView(requireContext());
        LinearLayout.LayoutParams paramNama = new LinearLayout.LayoutParams(
            0,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            1f
        );
        teksNama.setLayoutParams(paramNama);
        teksNama.setText(nama);
        teksNama.setTextColor(ContextCompat.getColor(requireContext(), R.color.warna_teks_utama));
        teksNama.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f);

        double persen = ((double) saldo * 100.0) / (double) total;
        TextView teksNilai = new TextView(requireContext());
        teksNilai.setText(String.format(
            new Locale("in", "ID"),
            "%s (%.1f%%)",
            FormatMataUang.keRupiah(saldo),
            persen
        ));
        teksNilai.setTextColor(ContextCompat.getColor(requireContext(), R.color.warna_teks_sekunder));
        teksNilai.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f);

        baris.addView(kotakWarna);
        baris.addView(teksNama);
        baris.addView(teksNilai);
        ikat.wadahLegendaSebaran.addView(baris);
    }

    private void perbaruiPratinjauEkspor() {
        List<TargetTabungan> targetSaring = ambilTargetSesuaiCakupan();
        List<TransaksiDenganTarget> transaksiSaring = ambilTransaksiUntukTarget(targetSaring);
        ikat.teksPratinjauEkspor.setText(
            "Siap mengekspor " + targetSaring.size() + " target dan "
                + transaksiSaring.size() + " catatan transaksi."
        );
    }

    private List<TargetTabungan> ambilTargetSesuaiCakupan() {
        List<TargetTabungan> hasil = new ArrayList<>();
        for (TargetTabungan t : semuaTarget) {
            if (cakupanEkspor == CAKUPAN_AKTIF && (t.diarsipkan || t.apakahTercapai())) {
                continue;
            }
            if (cakupanEkspor == CAKUPAN_TERCAPAI && !t.apakahTercapai()) {
                continue;
            }
            hasil.add(t);
        }
        return hasil;
    }

    private List<TransaksiDenganTarget> ambilTransaksiUntukTarget(List<TargetTabungan> targetSaring) {
        Set<Long> idDiizinkan = new HashSet<>();
        for (TargetTabungan t : targetSaring) {
            idDiizinkan.add(t.id);
        }
        List<TransaksiDenganTarget> hasil = new ArrayList<>();
        for (int i = semuaTransaksi.size() - 1; i >= 0; i--) {
            TransaksiDenganTarget tr = semuaTransaksi.get(i);
            if (idDiizinkan.contains(tr.idTarget)) {
                hasil.add(tr);
            }
        }
        return hasil;
    }

    private void jalankanEkspor(boolean formatExcel) {
        List<TargetTabungan> targetSaring = ambilTargetSesuaiCakupan();
        if (targetSaring.isEmpty()) {
            Snackbar.make(
                ikat.getRoot(),
                "Tidak ada data target pada cakupan yang dipilih untuk diekspor.",
                Snackbar.LENGTH_SHORT
            ).show();
            return;
        }
        List<TransaksiDenganTarget> transaksiSaring = ambilTransaksiUntukTarget(targetSaring);

        String judulCakupan;
        if (cakupanEkspor == CAKUPAN_AKTIF) {
            judulCakupan = getString(R.string.laporan_cakupan_aktif);
        } else if (cakupanEkspor == CAKUPAN_TERCAPAI) {
            judulCakupan = getString(R.string.laporan_cakupan_tercapai);
        } else {
            judulCakupan = getString(R.string.laporan_cakupan_semua);
        }

        ikat.wadahMemuatEkspor.setVisibility(View.VISIBLE);
        ikat.tombolUnduhXlsx.setEnabled(false);
        ikat.tombolUnduhCsv.setEnabled(false);

        RepositoriTabungan.ambilInstansi(requireContext()).ambilPelaksanaIo().execute(() -> {
            try {
                PengelolaEkspor.HasilEkspor hasil = PengelolaEkspor.eksporLaporan(
                    requireContext(),
                    formatExcel,
                    judulCakupan,
                    "Laporan-TabunganKu",
                    targetSaring,
                    transaksiSaring
                );
                if (!isAdded()) {
                    return;
                }
                requireActivity().runOnUiThread(() -> {
                    ikat.wadahMemuatEkspor.setVisibility(View.GONE);
                    ikat.tombolUnduhXlsx.setEnabled(true);
                    ikat.tombolUnduhCsv.setEnabled(true);
                    tampilkanDialogHasilEkspor(hasil, formatExcel);
                });
            } catch (IOException galat) {
                if (!isAdded()) {
                    return;
                }
                requireActivity().runOnUiThread(() -> {
                    ikat.wadahMemuatEkspor.setVisibility(View.GONE);
                    ikat.tombolUnduhXlsx.setEnabled(true);
                    ikat.tombolUnduhCsv.setEnabled(true);
                    Snackbar.make(
                        ikat.getRoot(),
                        "Gagal membuat berkas laporan: " + galat.getMessage(),
                        Snackbar.LENGTH_LONG
                    ).show();
                });
            }
        });
    }

    private void tampilkanDialogHasilEkspor(
        PengelolaEkspor.HasilEkspor hasil,
        boolean formatExcel
    ) {
        this.hasilEksporTerakhir = hasil;
        new MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.laporan_dialog_berhasil_judul)
            .setMessage("Berkas laporan telah dibuat dan disimpan di:\n" + hasil.lokasiUnduhanPublik)
            .setPositiveButton(R.string.laporan_dialog_buka, (d, w) -> bukaBerkas(hasil))
            .setNeutralButton(R.string.laporan_dialog_simpan_lain, (d, w) -> {
                if (formatExcel) {
                    peluncurSimpanExcel.launch(hasil.namaBerkas);
                } else {
                    peluncurSimpanCsv.launch(hasil.namaBerkas);
                }
            })
            .setNegativeButton(R.string.laporan_dialog_bagikan, (d, w) -> bagikanBerkas(hasil))
            .show();
    }

    private void bukaBerkas(PengelolaEkspor.HasilEkspor hasil) {
        Intent niat = new Intent(Intent.ACTION_VIEW);
        niat.setDataAndType(hasil.uriBagikan, hasil.tipeMime);
        niat.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            startActivity(Intent.createChooser(niat, "Buka laporan dengan aplikasi"));
        } catch (Exception galat) {
            bagikanBerkas(hasil);
        }
    }

    private void bagikanBerkas(PengelolaEkspor.HasilEkspor hasil) {
        Intent niat = new Intent(Intent.ACTION_SEND);
        niat.setType(hasil.tipeMime);
        niat.putExtra(Intent.EXTRA_STREAM, hasil.uriBagikan);
        niat.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(niat, "Bagikan laporan TabunganKu"));
    }

    private void salinEksporKeLokasiLain(android.net.Uri uriTujuan) {
        if (uriTujuan == null || hasilEksporTerakhir == null) {
            return;
        }
        RepositoriTabungan.ambilInstansi(requireContext()).ambilPelaksanaIo().execute(() -> {
            try {
                PengelolaEkspor.salinBerkasKeUri(
                    requireContext(),
                    hasilEksporTerakhir.berkasLokal,
                    uriTujuan
                );
                if (!isAdded()) {
                    return;
                }
                requireActivity().runOnUiThread(() -> Snackbar.make(
                    ikat.getRoot(),
                    "Berkas laporan berhasil disimpan ke lokasi pilihanmu.",
                    Snackbar.LENGTH_LONG
                ).show());
            } catch (IOException galat) {
                if (!isAdded()) {
                    return;
                }
                requireActivity().runOnUiThread(() -> Snackbar.make(
                    ikat.getRoot(),
                    "Gagal menyimpan berkas: " + galat.getMessage(),
                    Snackbar.LENGTH_LONG
                ).show());
            }
        });
    }

    private int dp(int nilaiDp) {
        return Math.round(nilaiDp * getResources().getDisplayMetrics().density);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        ikat = null;
    }
}
