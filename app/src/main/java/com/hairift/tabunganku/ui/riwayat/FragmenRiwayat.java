package com.hairift.tabunganku.ui.riwayat;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.hairift.tabunganku.R;
import com.hairift.tabunganku.data.model.TransaksiDenganTarget;
import com.hairift.tabunganku.data.repositori.RepositoriTabungan;
import com.hairift.tabunganku.databinding.FragmenRiwayatBinding;
import com.hairift.tabunganku.ui.target.AdaptorRiwayatTransaksi;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Menampilkan seluruh riwayat mutasi setor dan tarik lintas target dengan penyaring jenis dan pencarian.
 */
public class FragmenRiwayat extends Fragment {

    private static final int JENIS_SEMUA = 0;
    private static final int JENIS_SETOR = 1;
    private static final int JENIS_TARIK = 2;

    private FragmenRiwayatBinding ikat;
    private AdaptorRiwayatTransaksi adaptor;
    private final List<TransaksiDenganTarget> semuaTransaksi = new ArrayList<>();
    private boolean dataSudahDiterima;
    private int filterJenis = JENIS_SEMUA;
    private String kataKunci = "";

    @Nullable
    @Override
    public View onCreateView(
        @NonNull LayoutInflater pengembang,
        @Nullable ViewGroup wadah,
        @Nullable Bundle keadaanTersimpan
    ) {
        ikat = FragmenRiwayatBinding.inflate(pengembang, wadah, false);
        return ikat.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View tampilan, @Nullable Bundle keadaanTersimpan) {
        super.onViewCreated(tampilan, keadaanTersimpan);

        adaptor = new AdaptorRiwayatTransaksi(null);
        ikat.daftarRiwayatRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        ikat.daftarRiwayatRecycler.setAdapter(adaptor);

        ikat.grupChipJenisRiwayat.setOnCheckedStateChangeListener((grup, daftarId) -> {
            if (daftarId.isEmpty()) {
                return;
            }
            int id = daftarId.get(0);
            if (id == R.id.chip_riwayat_setor) {
                filterJenis = JENIS_SETOR;
            } else if (id == R.id.chip_riwayat_tarik) {
                filterJenis = JENIS_TARIK;
            } else {
                filterJenis = JENIS_SEMUA;
            }
            perbaruiDaftar();
        });

        ikat.inputCariRiwayat.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                kataKunci = s == null ? "" : s.toString().trim().toLowerCase(Locale.ROOT);
                perbaruiDaftar();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        ikat.tataMemuatRiwayat.setVisibility(View.VISIBLE);
        ikat.tataKosongRiwayat.setVisibility(View.GONE);
        ikat.daftarRiwayatRecycler.setVisibility(View.GONE);

        RepositoriTabungan.ambilInstansi(requireContext())
            .pantauSemuaTransaksiDenganTarget()
            .observe(getViewLifecycleOwner(), daftar -> {
                dataSudahDiterima = true;
                semuaTransaksi.clear();
                if (daftar != null) {
                    semuaTransaksi.addAll(daftar);
                }
                perbaruiDaftar();
            });
    }

    private void perbaruiDaftar() {
        if (!dataSudahDiterima) {
            return;
        }
        ikat.tataMemuatRiwayat.setVisibility(View.GONE);

        if (semuaTransaksi.isEmpty()) {
            ikat.tataKosongRiwayat.setVisibility(View.VISIBLE);
            ikat.daftarRiwayatRecycler.setVisibility(View.GONE);
            ikat.teksKosongRiwayatJudul.setText(R.string.riwayat_kosong_judul);
            ikat.teksKosongRiwayatIsi.setText(R.string.riwayat_kosong_isi);
            return;
        }

        List<TransaksiDenganTarget> tersaring = new ArrayList<>();
        for (TransaksiDenganTarget tr : semuaTransaksi) {
            if (filterJenis == JENIS_SETOR && !tr.apakahSetoran()) {
                continue;
            }
            if (filterJenis == JENIS_TARIK && tr.apakahSetoran()) {
                continue;
            }
            if (!kataKunci.isEmpty()) {
                String nama = tr.namaTarget.toLowerCase(Locale.ROOT);
                String kategori = tr.kategori.toLowerCase(Locale.ROOT);
                String catatan = tr.catatan == null ? "" : tr.catatan.toLowerCase(Locale.ROOT);
                if (!nama.contains(kataKunci) && !kategori.contains(kataKunci) && !catatan.contains(kataKunci)) {
                    continue;
                }
            }
            tersaring.add(tr);
        }

        if (tersaring.isEmpty()) {
            ikat.tataKosongRiwayat.setVisibility(View.VISIBLE);
            ikat.daftarRiwayatRecycler.setVisibility(View.GONE);
            ikat.teksKosongRiwayatJudul.setText(R.string.keadaan_kosong_saring_judul);
            ikat.teksKosongRiwayatIsi.setText("Coba hapus kata kunci atau pilih filter jenis transaksi lain.");
            return;
        }

        ikat.tataKosongRiwayat.setVisibility(View.GONE);
        ikat.daftarRiwayatRecycler.setVisibility(View.VISIBLE);
        adaptor.aturDariTransaksiGabungan(tersaring);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        ikat = null;
    }
}
