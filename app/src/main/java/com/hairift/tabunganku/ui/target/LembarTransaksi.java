package com.hairift.tabunganku.ui.target;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.hairift.tabunganku.R;
import com.hairift.tabunganku.data.model.TargetTabungan;
import com.hairift.tabunganku.data.repositori.RepositoriTabungan;
import com.hairift.tabunganku.databinding.LembarTransaksiBinding;
import com.hairift.tabunganku.util.FormatMataUang;
import com.hairift.tabunganku.util.FormatTanggal;
import com.hairift.tabunganku.util.KonstantaAplikasi;
import com.hairift.tabunganku.util.PemformatInputRupiah;

/**
 * Lembar bawah interaktif untuk mencatat setoran atau penarikan dana dengan nominal cepat.
 */
public class LembarTransaksi extends BottomSheetDialogFragment {

    private static final String ARG_ID_TARGET = "arg_id_target";
    private static final String ARG_NAMA_TARGET = "arg_nama_target";
    private static final String ARG_JENIS = "arg_jenis";
    private static final String ARG_SALDO_SAAT_INI = "arg_saldo_saat_ini";
    private static final String ARG_TARGET_NOMINAL = "arg_target_nominal";

    private LembarTransaksiBinding ikat;
    private long idTarget;
    private String namaTarget;
    private String jenisTransaksi;
    private long saldoSaatIni;
    private long targetNominal;
    private long tanggalTerpilihEpoch;

    public static LembarTransaksi buatInstansi(
        TargetTabungan target,
        String jenisTransaksi
    ) {
        LembarTransaksi lembar = new LembarTransaksi();
        Bundle argumen = new Bundle();
        argumen.putLong(ARG_ID_TARGET, target.id);
        argumen.putString(ARG_NAMA_TARGET, target.namaTarget);
        argumen.putString(ARG_JENIS, jenisTransaksi);
        argumen.putLong(ARG_SALDO_SAAT_INI, target.saldoTerkumpul);
        argumen.putLong(ARG_TARGET_NOMINAL, target.nominalTarget);
        lembar.setArguments(argumen);
        return lembar;
    }

    @Nullable
    @Override
    public View onCreateView(
        @NonNull LayoutInflater pengembang,
        @Nullable ViewGroup wadah,
        @Nullable Bundle keadaanTersimpan
    ) {
        ikat = LembarTransaksiBinding.inflate(pengembang, wadah, false);
        return ikat.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View tampilan, @Nullable Bundle keadaanTersimpan) {
        super.onViewCreated(tampilan, keadaanTersimpan);
        Bundle arg = requireArguments();
        idTarget = arg.getLong(ARG_ID_TARGET);
        namaTarget = arg.getString(ARG_NAMA_TARGET, "");
        jenisTransaksi = arg.getString(ARG_JENIS, KonstantaAplikasi.JENIS_SETOR);
        saldoSaatIni = arg.getLong(ARG_SALDO_SAAT_INI, 0L);
        targetNominal = arg.getLong(ARG_TARGET_NOMINAL, 0L);
        tanggalTerpilihEpoch = System.currentTimeMillis();

        boolean apakahSetor = KonstantaAplikasi.JENIS_SETOR.equals(jenisTransaksi);
        ikat.teksJudulLembarTransaksi.setText(
            apakahSetor ? R.string.lembar_judul_setor : R.string.lembar_judul_tarik
        );
        ikat.teksSubjudulLembarTransaksi.setText(
            namaTarget + " · Saldo saat ini " + FormatMataUang.keRupiah(saldoSaatIni)
        );
        ikat.tombolSimpanLembar.setText(
            apakahSetor ? R.string.lembar_tombol_simpan_setor : R.string.lembar_tombol_simpan_tarik
        );

        ikat.gulirNominalCepat.setVisibility(apakahSetor ? View.VISIBLE : View.GONE);
        ikat.inputNominalLembar.addTextChangedListener(new PemformatInputRupiah(ikat.inputNominalLembar));

        aturChipNominalCepat();
        perbaruiTeksTanggal();

        ikat.tombolTanggalLembar.setOnClickListener(v -> bukaPemilihTanggal());
        ikat.tombolBatalLembar.setOnClickListener(v -> dismiss());
        ikat.tombolSimpanLembar.setOnClickListener(v -> simpanTransaksi());
    }

    private void aturChipNominalCepat() {
        ikat.chipNominal50rb.setOnClickListener(v -> tambahNominalCepat(50_000L));
        ikat.chipNominal100rb.setOnClickListener(v -> tambahNominalCepat(100_000L));
        ikat.chipNominal250rb.setOnClickListener(v -> tambahNominalCepat(250_000L));
        ikat.chipNominal500rb.setOnClickListener(v -> tambahNominalCepat(500_000L));
        ikat.chipNominal1jt.setOnClickListener(v -> tambahNominalCepat(1_000_000L));
    }

    private void tambahNominalCepat(long tambahan) {
        String teksSekarang = ikat.inputNominalLembar.getText() != null
            ? ikat.inputNominalLembar.getText().toString()
            : "";
        long saatIni = FormatMataUang.uraiDariTeks(teksSekarang);
        long hasil = saatIni + tambahan;
        ikat.inputNominalLembar.setText(FormatMataUang.keAngkaRibuan(hasil));
        ikat.wadahNominalLembar.setError(null);
    }

    private void bukaPemilihTanggal() {
        MaterialDatePicker<Long> pemilih = MaterialDatePicker.Builder.datePicker()
            .setTitleText(R.string.lembar_label_tanggal)
            .setSelection(tanggalTerpilihEpoch)
            .build();
        pemilih.addOnPositiveButtonClickListener(pilihan -> {
            if (pilihan != null) {
                tanggalTerpilihEpoch = pilihan;
                perbaruiTeksTanggal();
            }
        });
        pemilih.show(getChildFragmentManager(), "PEMILIH_TANGGAL_TRANSAKSI");
    }

    private void perbaruiTeksTanggal() {
        ikat.tombolTanggalLembar.setText(FormatTanggal.keTanggalPenuh(tanggalTerpilihEpoch));
    }

    private void simpanTransaksi() {
        String teksNominal = ikat.inputNominalLembar.getText() != null
            ? ikat.inputNominalLembar.getText().toString()
            : "";
        long nominal = FormatMataUang.uraiDariTeks(teksNominal);
        if (nominal <= 0L) {
            ikat.wadahNominalLembar.setError(getString(R.string.galat_nominal_transaksi_wajib));
            return;
        }
        boolean apakahSetor = KonstantaAplikasi.JENIS_SETOR.equals(jenisTransaksi);
        if (!apakahSetor && nominal > saldoSaatIni) {
            ikat.wadahNominalLembar.setError(
                getString(R.string.galat_saldo_tidak_cukup, FormatMataUang.keRupiah(saldoSaatIni))
            );
            return;
        }
        ikat.wadahNominalLembar.setError(null);
        ikat.tombolSimpanLembar.setEnabled(false);

        String catatan = ikat.inputCatatanLembar.getText() != null
            ? ikat.inputCatatanLembar.getText().toString().trim()
            : "";

        boolean belumTercapaiSebelumnya = targetNominal > 0L && saldoSaatIni < targetNominal;
        RepositoriTabungan.ambilInstansi(requireContext()).catatTransaksi(
            idTarget,
            jenisTransaksi,
            nominal,
            catatan,
            tanggalTerpilihEpoch,
            hasil -> {
                if (!isAdded()) {
                    return;
                }
                ikat.tombolSimpanLembar.setEnabled(true);
                if (!hasil.apakahBerhasil()) {
                    ikat.wadahNominalLembar.setError(hasil.ambilPesanGalat());
                    return;
                }

                TargetTabungan targetSesudah = hasil.ambilData();
                if (apakahSetor
                    && belumTercapaiSebelumnya
                    && targetSesudah != null
                    && targetSesudah.apakahTercapai()
                    && getActivity() != null) {
                    new MaterialAlertDialogBuilder(requireActivity())
                        .setTitle(R.string.dialog_target_tercapai_judul)
                        .setMessage(getString(
                            R.string.dialog_target_tercapai_isi,
                            targetSesudah.namaTarget,
                            FormatMataUang.keRupiah(targetSesudah.nominalTarget)
                        ))
                        .setPositiveButton(R.string.tombol_tutup, null)
                        .show();
                } else if (getActivity() != null) {
                    View akar = getActivity().findViewById(android.R.id.content);
                    if (akar != null) {
                        Snackbar.make(
                            akar,
                            apakahSetor ? "Setoran berhasil dicatat." : "Penarikan berhasil dicatat.",
                            Snackbar.LENGTH_SHORT
                        ).show();
                    }
                }
                dismiss();
            }
        );
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        ikat = null;
    }
}
