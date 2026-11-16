package com.hairift.tabunganku.ui.beranda;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;

import com.google.android.material.chip.Chip;
import com.hairift.tabunganku.R;
import com.hairift.tabunganku.data.model.TargetTabungan;
import com.hairift.tabunganku.data.repositori.RepositoriTabungan;
import com.hairift.tabunganku.databinding.FragmenBerandaBinding;
import com.hairift.tabunganku.ui.target.AktivitasDetailTarget;
import com.hairift.tabunganku.ui.target.AktivitasTambahUbahTarget;
import com.hairift.tabunganku.ui.target.LembarTransaksi;
import com.hairift.tabunganku.util.FormatMataUang;
import com.hairift.tabunganku.util.KalkulatorTarget;
import com.hairift.tabunganku.util.KonstantaAplikasi;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Layar utama TabunganKu berisi ringkasan total dana, penyaring, dan kartu target bermotivasi foto.
 */
public class FragmenBeranda extends Fragment implements AdaptorKartuTarget.PendengarKartuTarget {

    private static final int STATUS_SEMUA = 0;
    private static final int STATUS_BERJALAN = 1;
    private static final int STATUS_TERCAPAI = 2;
    private static final int STATUS_ARSIP = 3;

    private FragmenBerandaBinding ikat;
    private AdaptorKartuTarget adaptorTarget;
    private final List<TargetTabungan> semuaTarget = new ArrayList<>();
    private boolean dataSudahDiterima;
    private int filterStatus = STATUS_SEMUA;
    private String filterKategori = "Semua";
    private String kataKunciCari = "";

    @Nullable
    @Override
    public View onCreateView(
        @NonNull LayoutInflater pengembang,
        @Nullable ViewGroup wadah,
        @Nullable Bundle keadaanTersimpan
    ) {
        ikat = FragmenBerandaBinding.inflate(pengembang, wadah, false);
        return ikat.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View tampilan, @Nullable Bundle keadaanTersimpan) {
        super.onViewCreated(tampilan, keadaanTersimpan);

        int jumlahKolom = getResources().getInteger(R.integer.kolom_target);
        adaptorTarget = new AdaptorKartuTarget(this);
        ikat.daftarTargetRecycler.setLayoutManager(new GridLayoutManager(requireContext(), jumlahKolom));
        ikat.daftarTargetRecycler.setAdapter(adaptorTarget);

        siapkanChipKategori();
        siapkanPendengarFilter();

        ikat.tombolTambahTarget.setOnClickListener(v -> bukaFormTargetBaru());
        ikat.tombolMuatUlangBeranda.setOnClickListener(v -> terapkanSaringanDanTampilkan());

        tampilkanKeadaanMemuat();

        RepositoriTabungan.ambilInstansi(requireContext())
            .pantauSemuaTarget()
            .observe(getViewLifecycleOwner(), daftarBaru -> {
                dataSudahDiterima = true;
                semuaTarget.clear();
                if (daftarBaru != null) {
                    semuaTarget.addAll(daftarBaru);
                }
                perbaruiKartuRingkasan();
                terapkanSaringanDanTampilkan();
            });
    }

    private void siapkanChipKategori() {
        ikat.grupChipKategori.removeAllViews();
        tambahChipKategori("Semua", true);
        for (String kategori : KonstantaAplikasi.DAFTAR_KATEGORI) {
            tambahChipKategori(kategori, false);
        }
    }

    private void tambahChipKategori(String label, boolean terpilihAwal) {
        Chip chip = new Chip(requireContext());
        chip.setId(View.generateViewId());
        chip.setText(label);
        chip.setCheckable(true);
        chip.setChecked(terpilihAwal);
        chip.setMinimumHeight(getResources().getDimensionPixelSize(R.dimen.sentuh_minimum));
        chip.setOnClickListener(v -> {
            filterKategori = label;
            terapkanSaringanDanTampilkan();
        });
        ikat.grupChipKategori.addView(chip);
    }

    private void siapkanPendengarFilter() {
        ikat.grupChipStatus.setOnCheckedStateChangeListener((grup, daftarId) -> {
            if (daftarId.isEmpty()) {
                return;
            }
            int id = daftarId.get(0);
            if (id == R.id.chip_status_berjalan) {
                filterStatus = STATUS_BERJALAN;
            } else if (id == R.id.chip_status_tercapai) {
                filterStatus = STATUS_TERCAPAI;
            } else if (id == R.id.chip_status_arsip) {
                filterStatus = STATUS_ARSIP;
            } else {
                filterStatus = STATUS_SEMUA;
            }
            terapkanSaringanDanTampilkan();
        });

        ikat.inputCariTarget.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                kataKunciCari = s == null ? "" : s.toString().trim().toLowerCase(Locale.ROOT);
                terapkanSaringanDanTampilkan();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private void perbaruiKartuRingkasan() {
        long totalTerkumpul = 0L;
        long totalTarget = 0L;
        int jumlahAktif = 0;
        int jumlahTercapai = 0;

        for (TargetTabungan t : semuaTarget) {
            if (t.diarsipkan) {
                continue;
            }
            jumlahAktif++;
            totalTerkumpul += t.saldoTerkumpul;
            totalTarget += t.nominalTarget;
            if (t.apakahTercapai()) {
                jumlahTercapai++;
            }
        }

        int persen = KalkulatorTarget.hitungPersentase(totalTerkumpul, totalTarget);
        ikat.teksTotalTerkumpulHero.setText(FormatMataUang.keRupiah(totalTerkumpul));
        ikat.teksDariTargetHero.setText(
            getString(R.string.beranda_label_dari_target, FormatMataUang.keRupiah(totalTarget))
        );
        ikat.progresHero.setProgress(persen);
        ikat.teksJumlahTargetHero.setText(
            getString(R.string.beranda_label_jumlah_target, jumlahAktif, jumlahTercapai)
        );
        ikat.teksPersenHero.setText(persen + "%");
    }

    private void terapkanSaringanDanTampilkan() {
        if (!dataSudahDiterima) {
            tampilkanKeadaanMemuat();
            return;
        }

        if (semuaTarget.isEmpty()) {
            tampilkanKeadaanKosongAwal();
            return;
        }

        List<TargetTabungan> hasilSaring = new ArrayList<>();
        for (TargetTabungan t : semuaTarget) {
            if (!cocokStatus(t) || !cocokKategori(t) || !cocokKataKunci(t)) {
                continue;
            }
            hasilSaring.add(t);
        }

        if (hasilSaring.isEmpty()) {
            tampilkanKeadaanKosongSaring();
            return;
        }

        ikat.kelompokCariDanSaring.setVisibility(View.VISIBLE);
        ikat.tombolTambahTarget.show();
        ikat.tataMemuatBeranda.setVisibility(View.GONE);
        ikat.tataKosongBeranda.setVisibility(View.GONE);
        ikat.tataGalatBeranda.setVisibility(View.GONE);
        ikat.daftarTargetRecycler.setVisibility(View.VISIBLE);
        adaptorTarget.submitList(hasilSaring);
    }

    private boolean cocokStatus(TargetTabungan t) {
        switch (filterStatus) {
            case STATUS_BERJALAN:
                return !t.diarsipkan && !t.apakahTercapai();
            case STATUS_TERCAPAI:
                return !t.diarsipkan && t.apakahTercapai();
            case STATUS_ARSIP:
                return t.diarsipkan;
            default:
                return !t.diarsipkan;
        }
    }

    private boolean cocokKategori(TargetTabungan t) {
        return "Semua".equals(filterKategori) || filterKategori.equalsIgnoreCase(t.kategori);
    }

    private boolean cocokKataKunci(TargetTabungan t) {
        if (kataKunciCari.isEmpty()) {
            return true;
        }
        String nama = t.namaTarget.toLowerCase(Locale.ROOT);
        String catatan = t.catatanMotivasi == null ? "" : t.catatanMotivasi.toLowerCase(Locale.ROOT);
        String kategori = t.kategori.toLowerCase(Locale.ROOT);
        return nama.contains(kataKunciCari)
            || catatan.contains(kataKunciCari)
            || kategori.contains(kataKunciCari);
    }

    private void tampilkanKeadaanMemuat() {
        ikat.kelompokCariDanSaring.setVisibility(View.GONE);
        ikat.tombolTambahTarget.hide();
        ikat.tataMemuatBeranda.setVisibility(View.VISIBLE);
        ikat.tataKosongBeranda.setVisibility(View.GONE);
        ikat.tataGalatBeranda.setVisibility(View.GONE);
        ikat.daftarTargetRecycler.setVisibility(View.GONE);
    }

    private void tampilkanKeadaanKosongAwal() {
        ikat.kelompokCariDanSaring.setVisibility(View.GONE);
        ikat.tombolTambahTarget.hide();
        ikat.tataMemuatBeranda.setVisibility(View.GONE);
        ikat.tataGalatBeranda.setVisibility(View.GONE);
        ikat.daftarTargetRecycler.setVisibility(View.GONE);
        ikat.tataKosongBeranda.setVisibility(View.VISIBLE);
        ikat.teksKosongJudul.setText(R.string.keadaan_kosong_judul);
        ikat.teksKosongIsi.setText(R.string.keadaan_kosong_isi);
        ikat.tombolAksiKosong.setText(R.string.keadaan_kosong_aksi);
        ikat.tombolAksiKosong.setOnClickListener(v -> bukaFormTargetBaru());
    }

    private void tampilkanKeadaanKosongSaring() {
        ikat.kelompokCariDanSaring.setVisibility(View.VISIBLE);
        ikat.tombolTambahTarget.hide();
        ikat.tataMemuatBeranda.setVisibility(View.GONE);
        ikat.tataGalatBeranda.setVisibility(View.GONE);
        ikat.daftarTargetRecycler.setVisibility(View.GONE);
        ikat.tataKosongBeranda.setVisibility(View.VISIBLE);
        ikat.teksKosongJudul.setText(R.string.keadaan_kosong_saring_judul);
        ikat.teksKosongIsi.setText(R.string.keadaan_kosong_saring_isi);
        ikat.tombolAksiKosong.setText(R.string.keadaan_kosong_saring_aksi);
        ikat.tombolAksiKosong.setOnClickListener(v -> aturUlangFilter());
    }

    private void aturUlangFilter() {
        filterStatus = STATUS_SEMUA;
        filterKategori = "Semua";
        kataKunciCari = "";
        ikat.inputCariTarget.setText("");
        ikat.chipStatusSemua.setChecked(true);
        if (ikat.grupChipKategori.getChildCount() > 0) {
            View anakPertama = ikat.grupChipKategori.getChildAt(0);
            if (anakPertama instanceof Chip) {
                ((Chip) anakPertama).setChecked(true);
            }
        }
        terapkanSaringanDanTampilkan();
    }

    private void bukaFormTargetBaru() {
        startActivity(new Intent(requireContext(), AktivitasTambahUbahTarget.class));
    }

    @Override
    public void saatKartuDiketuk(TargetTabungan target) {
        Intent niat = new Intent(requireContext(), AktivitasDetailTarget.class);
        niat.putExtra(KonstantaAplikasi.EKSTRA_ID_TARGET, target.id);
        startActivity(niat);
    }

    @Override
    public void saatSetorCepatDiketuk(TargetTabungan target) {
        LembarTransaksi.buatInstansi(target, KonstantaAplikasi.JENIS_SETOR)
            .show(getChildFragmentManager(), "LEMBAR_SETOR_CEPAT");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        ikat = null;
    }
}
