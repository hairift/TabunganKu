package com.hairift.tabunganku.ui.target;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.widget.ImageViewCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.hairift.tabunganku.R;
import com.hairift.tabunganku.data.model.TransaksiDenganTarget;
import com.hairift.tabunganku.data.model.TransaksiTabungan;
import com.hairift.tabunganku.databinding.ItemTransaksiBinding;
import com.hairift.tabunganku.util.FormatMataUang;
import com.hairift.tabunganku.util.FormatTanggal;

import java.util.ArrayList;
import java.util.List;

/**
 * Adaptor serbaguna untuk menampilkan baris riwayat setor dan tarik pada Detail Target maupun Tab Riwayat.
 */
public class AdaptorRiwayatTransaksi
    extends RecyclerView.Adapter<AdaptorRiwayatTransaksi.PemegangTransaksi> {

    public interface PendengarHapusTransaksi {
        void saatHapusDiketuk(TransaksiTabungan transaksi);
    }

    public static final class BarisRiwayat {
        public final long id;
        public final long idTarget;
        public final String judulUtama;
        public final String subjudul;
        public final boolean apakahSetor;
        public final long nominal;
        @Nullable
        public final TransaksiTabungan entitasAsli;

        public BarisRiwayat(
            long id,
            long idTarget,
            String judulUtama,
            String subjudul,
            boolean apakahSetor,
            long nominal,
            @Nullable TransaksiTabungan entitasAsli
        ) {
            this.id = id;
            this.idTarget = idTarget;
            this.judulUtama = judulUtama;
            this.subjudul = subjudul;
            this.apakahSetor = apakahSetor;
            this.nominal = nominal;
            this.entitasAsli = entitasAsli;
        }
    }

    private final List<BarisRiwayat> daftarBaris = new ArrayList<>();
    @Nullable
    private final PendengarHapusTransaksi pendengarHapus;

    public AdaptorRiwayatTransaksi(@Nullable PendengarHapusTransaksi pendengarHapus) {
        this.pendengarHapus = pendengarHapus;
    }

    public void aturDariTransaksiTarget(List<TransaksiTabungan> daftar) {
        daftarBaris.clear();
        if (daftar != null) {
            for (TransaksiTabungan tr : daftar) {
                String judul = (tr.catatan != null && !tr.catatan.trim().isEmpty())
                    ? tr.catatan
                    : (tr.apakahSetoran() ? "Setoran tabungan" : "Penarikan dana");
                String tanggal = FormatTanggal.keTanggalPenuh(tr.tanggalTransaksiEpoch);
                daftarBaris.add(new BarisRiwayat(
                    tr.id,
                    tr.idTarget,
                    judul,
                    tanggal,
                    tr.apakahSetoran(),
                    tr.nominal,
                    tr
                ));
            }
        }
        notifyDataSetChanged();
    }

    public void aturDariTransaksiGabungan(List<TransaksiDenganTarget> daftar) {
        daftarBaris.clear();
        if (daftar != null) {
            for (TransaksiDenganTarget tr : daftar) {
                String tanggal = FormatTanggal.keTanggalRingkas(tr.tanggalTransaksiEpoch);
                String subjudul = (tr.catatan != null && !tr.catatan.trim().isEmpty())
                    ? tanggal + " · " + tr.catatan
                    : tanggal + " · " + tr.kategori;
                daftarBaris.add(new BarisRiwayat(
                    tr.id,
                    tr.idTarget,
                    tr.namaTarget,
                    subjudul,
                    tr.apakahSetoran(),
                    tr.nominal,
                    null
                ));
            }
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public PemegangTransaksi onCreateViewHolder(@NonNull ViewGroup induk, int tipe) {
        ItemTransaksiBinding ikat = ItemTransaksiBinding.inflate(
            LayoutInflater.from(induk.getContext()),
            induk,
            false
        );
        return new PemegangTransaksi(ikat);
    }

    @Override
    public void onBindViewHolder(@NonNull PemegangTransaksi pemegang, int posisi) {
        pemegang.ikat(daftarBaris.get(posisi), pendengarHapus);
    }

    @Override
    public int getItemCount() {
        return daftarBaris.size();
    }

    static class PemegangTransaksi extends RecyclerView.ViewHolder {

        private final ItemTransaksiBinding ikat;

        PemegangTransaksi(ItemTransaksiBinding ikat) {
            super(ikat.getRoot());
            this.ikat = ikat;
        }

        void ikat(BarisRiwayat baris, @Nullable PendengarHapusTransaksi pendengarHapus) {
            Context konteks = ikat.getRoot().getContext();
            ikat.teksJudulTransaksi.setText(baris.judulUtama);
            ikat.teksSubjudulTransaksi.setText(baris.subjudul);

            if (baris.apakahSetor) {
                ikat.ikonJenisTransaksi.setImageResource(R.drawable.ic_setor);
                ikat.ikonJenisTransaksi.setBackgroundResource(R.drawable.latar_status_setor);
                int warnaSetor = ContextCompat.getColor(konteks, R.color.warna_setor_teks);
                ImageViewCompat.setImageTintList(
                    ikat.ikonJenisTransaksi,
                    android.content.res.ColorStateList.valueOf(warnaSetor)
                );
                ikat.teksNominalTransaksi.setTextColor(warnaSetor);
                ikat.teksNominalTransaksi.setText("+" + FormatMataUang.keRupiah(baris.nominal));
                ikat.teksLabelJenisTransaksi.setText(R.string.riwayat_jenis_setor);
            } else {
                ikat.ikonJenisTransaksi.setImageResource(R.drawable.ic_tarik);
                ikat.ikonJenisTransaksi.setBackgroundResource(R.drawable.latar_status_tarik);
                int warnaTarik = ContextCompat.getColor(konteks, R.color.warna_tarik_teks);
                ImageViewCompat.setImageTintList(
                    ikat.ikonJenisTransaksi,
                    android.content.res.ColorStateList.valueOf(warnaTarik)
                );
                ikat.teksNominalTransaksi.setTextColor(warnaTarik);
                ikat.teksNominalTransaksi.setText("-" + FormatMataUang.keRupiah(baris.nominal));
                ikat.teksLabelJenisTransaksi.setText(R.string.riwayat_jenis_tarik);
            }

            if (pendengarHapus != null && baris.entitasAsli != null) {
                ikat.tombolHapusTransaksi.setVisibility(View.VISIBLE);
                ikat.tombolHapusTransaksi.setContentDescription(
                    konteks.getString(
                        R.string.ket_akses_hapus_transaksi,
                        baris.apakahSetor ? "setor" : "tarik",
                        FormatMataUang.keRupiah(baris.nominal)
                    )
                );
                ikat.tombolHapusTransaksi.setOnClickListener(
                    v -> pendengarHapus.saatHapusDiketuk(baris.entitasAsli)
                );
            } else {
                ikat.tombolHapusTransaksi.setVisibility(View.GONE);
                ikat.tombolHapusTransaksi.setOnClickListener(null);
            }
        }
    }
}
