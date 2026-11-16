package com.hairift.tabunganku.ui.beranda;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.hairift.tabunganku.R;
import com.hairift.tabunganku.data.lokal.PenyimpanFoto;
import com.hairift.tabunganku.data.model.TargetTabungan;
import com.hairift.tabunganku.databinding.ItemKartuTargetBinding;
import com.hairift.tabunganku.util.FormatMataUang;
import com.hairift.tabunganku.util.FormatTanggal;
import com.hairift.tabunganku.util.KalkulatorTarget;

import java.util.Objects;

/**
 * Adaptor daftar kartu target tabungan di Beranda dengan pembaruan diferensial efisien.
 */
public class AdaptorKartuTarget extends ListAdapter<TargetTabungan, AdaptorKartuTarget.PemegangKartu> {

    public interface PendengarKartuTarget {
        void saatKartuDiketuk(TargetTabungan target);

        void saatSetorCepatDiketuk(TargetTabungan target);
    }

    private static final DiffUtil.ItemCallback<TargetTabungan> PEMBANDING_ITEM =
        new DiffUtil.ItemCallback<TargetTabungan>() {
            @Override
            public boolean areItemsTheSame(@NonNull TargetTabungan lama, @NonNull TargetTabungan baru) {
                return lama.id == baru.id;
            }

            @Override
            public boolean areContentsTheSame(@NonNull TargetTabungan lama, @NonNull TargetTabungan baru) {
                return lama.nominalTarget == baru.nominalTarget
                    && lama.saldoTerkumpul == baru.saldoTerkumpul
                    && lama.tanggalTargetEpoch == baru.tanggalTargetEpoch
                    && lama.diarsipkan == baru.diarsipkan
                    && lama.diperbaruiPadaEpoch == baru.diperbaruiPadaEpoch
                    && Objects.equals(lama.namaTarget, baru.namaTarget)
                    && Objects.equals(lama.kategori, baru.kategori)
                    && Objects.equals(lama.pathFotoMotivasi, baru.pathFotoMotivasi)
                    && Objects.equals(lama.catatanMotivasi, baru.catatanMotivasi);
            }
        };

    private final PendengarKartuTarget pendengar;

    public AdaptorKartuTarget(PendengarKartuTarget pendengar) {
        super(PEMBANDING_ITEM);
        this.pendengar = pendengar;
    }

    @NonNull
    @Override
    public PemegangKartu onCreateViewHolder(@NonNull ViewGroup induk, int tipeTampilan) {
        ItemKartuTargetBinding pengikatan = ItemKartuTargetBinding.inflate(
            LayoutInflater.from(induk.getContext()),
            induk,
            false
        );
        return new PemegangKartu(pengikatan);
    }

    @Override
    public void onBindViewHolder(@NonNull PemegangKartu pemegang, int posisi) {
        pemegang.ikat(getItem(posisi), pendengar);
    }

    static class PemegangKartu extends RecyclerView.ViewHolder {

        private final ItemKartuTargetBinding ikat;

        PemegangKartu(ItemKartuTargetBinding pengikatan) {
            super(pengikatan.getRoot());
            this.ikat = pengikatan;
        }

        void ikat(TargetTabungan target, PendengarKartuTarget pendengar) {
            Context konteks = ikat.getRoot().getContext();
            ikat.teksNamaTargetKartu.setText(target.namaTarget);
            ikat.teksKategoriKartu.setText(target.kategori);

            String statusLabel = target.diarsipkan
                ? "Diarsipkan"
                : (target.apakahTercapai() ? "Tercapai" : "Berjalan");
            ikat.teksStatusKartu.setText(statusLabel);

            if (target.catatanMotivasi != null && !target.catatanMotivasi.trim().isEmpty()) {
                ikat.teksMotivasiKartu.setVisibility(View.VISIBLE);
                ikat.teksMotivasiKartu.setText(target.catatanMotivasi);
            } else {
                ikat.teksMotivasiKartu.setVisibility(View.GONE);
            }

            ikat.teksSaldoKartu.setText(FormatMataUang.keRupiah(target.saldoTerkumpul));
            ikat.teksTargetNominalKartu.setText(
                konteks.getString(
                    R.string.beranda_label_dari_target,
                    FormatMataUang.keRupiah(target.nominalTarget)
                )
            );

            int persen = KalkulatorTarget.hitungPersentase(target.saldoTerkumpul, target.nominalTarget);
            ikat.teksPersenKartu.setText(persen + "%");
            ikat.progresKartu.setProgress(persen);

            String infoBawah;
            if (target.apakahTercapai()) {
                infoBawah = target.tanggalTargetEpoch > 0
                    ? "Tercapai · Target " + FormatTanggal.keBulanTahun(target.tanggalTargetEpoch)
                    : "Target dana sudah terpenuhi";
            } else {
                long sisa = KalkulatorTarget.hitungSisa(target.saldoTerkumpul, target.nominalTarget);
                String bagianSisa = "Kurang " + FormatMataUang.keRupiah(sisa);
                infoBawah = target.tanggalTargetEpoch > 0
                    ? bagianSisa + " · " + FormatTanggal.keBulanTahun(target.tanggalTargetEpoch)
                    : bagianSisa;
            }
            ikat.teksSisaWaktuKartu.setText(infoBawah);

            int lebarFotoPx = Math.max(
                640,
                konteks.getResources().getDisplayMetrics().widthPixels
            );
            PenyimpanFoto.muatKeImageView(
                ikat.gambarMotivasiTarget,
                target.pathFotoMotivasi,
                lebarFotoPx
            );
            ikat.gambarMotivasiTarget.setContentDescription(
                target.pathFotoMotivasi != null && !target.pathFotoMotivasi.isEmpty()
                    ? konteks.getString(R.string.ket_akses_foto_target, target.namaTarget)
                    : konteks.getString(R.string.ket_akses_tanpa_foto, target.namaTarget)
            );

            ikat.tombolSetorKartu.setVisibility(target.diarsipkan ? View.GONE : View.VISIBLE);
            ikat.tombolSetorKartu.setOnClickListener(v -> pendengar.saatSetorCepatDiketuk(target));
            ikat.kartuTarget.setOnClickListener(v -> pendengar.saatKartuDiketuk(target));
        }
    }
}
