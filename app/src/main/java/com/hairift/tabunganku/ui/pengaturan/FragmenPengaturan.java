package com.hairift.tabunganku.ui.pengaturan;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.hairift.tabunganku.BuildConfig;
import com.hairift.tabunganku.R;
import com.hairift.tabunganku.data.lokal.PengelolaPreferensi;
import com.hairift.tabunganku.data.repositori.RepositoriTabungan;
import com.hairift.tabunganku.databinding.FragmenPengaturanBinding;
import com.hairift.tabunganku.ekspor.PengelolaCadangan;
import com.hairift.tabunganku.util.FormatTanggal;
import com.hairift.tabunganku.util.KonstantaAplikasi;
import com.hairift.tabunganku.util.PengelolaPengingat;

/**
 * Mengatur tampilan warna terang atau gelap, pengingat menabung lokal, cadangan ZIP, dan info lisensi.
 */
public class FragmenPengaturan extends Fragment {

    private FragmenPengaturanBinding ikat;
    private PengelolaPreferensi preferensi;
    private boolean sedangMenyesuaikanSwitch;

    private final ActivityResultLauncher<String> peluncurIzinNotifikasi =
        registerForActivityResult(new ActivityResultContracts.RequestPermission(), diizinkan -> {
            if (!isAdded()) {
                return;
            }
            if (diizinkan) {
                aktifkanPengingatSekarang(true);
            } else {
                sedangMenyesuaikanSwitch = true;
                ikat.sakelarPengingat.setChecked(false);
                sedangMenyesuaikanSwitch = false;
                Snackbar.make(
                    ikat.getRoot(),
                    "Izin notifikasi diperlukan untuk menampilkan pengingat harian.",
                    Snackbar.LENGTH_LONG
                ).show();
            }
        });

    private final ActivityResultLauncher<String> peluncurBuatCadangan =
        registerForActivityResult(
            new ActivityResultContracts.CreateDocument("application/zip"),
            uri -> {
                if (uri == null) {
                    return;
                }
                RepositoriTabungan.ambilInstansi(requireContext()).ambilPelaksanaIo().execute(() -> {
                    try {
                        int jumlah = PengelolaCadangan.buatCadanganKeUri(requireContext(), uri);
                        if (!isAdded()) {
                            return;
                        }
                        requireActivity().runOnUiThread(() -> Snackbar.make(
                            ikat.getRoot(),
                            "Cadangan berhasil dibuat (" + jumlah + " target disimpan).",
                            Snackbar.LENGTH_LONG
                        ).show());
                    } catch (Exception galat) {
                        if (!isAdded()) {
                            return;
                        }
                        requireActivity().runOnUiThread(() -> Snackbar.make(
                            ikat.getRoot(),
                            "Gagal mencadangkan data: " + galat.getMessage(),
                            Snackbar.LENGTH_LONG
                        ).show());
                    }
                });
            }
        );

    private final ActivityResultLauncher<String[]> peluncurPulihkanCadangan =
        registerForActivityResult(
            new ActivityResultContracts.OpenDocument(),
            uri -> {
                if (uri == null || !isAdded()) {
                    return;
                }
                new MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.konfirmasi_pulihkan_judul)
                    .setMessage(R.string.konfirmasi_pulihkan_isi)
                    .setNegativeButton(R.string.tombol_batal, null)
                    .setPositiveButton(R.string.tombol_pulihkan, (d, w) -> pulihkanDariUri(uri))
                    .show();
            }
        );

    @Nullable
    @Override
    public View onCreateView(
        @NonNull LayoutInflater pengembang,
        @Nullable ViewGroup wadah,
        @Nullable Bundle keadaanTersimpan
    ) {
        ikat = FragmenPengaturanBinding.inflate(pengembang, wadah, false);
        return ikat.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View tampilan, @Nullable Bundle keadaanTersimpan) {
        super.onViewCreated(tampilan, keadaanTersimpan);
        preferensi = new PengelolaPreferensi(requireContext());

        ikat.teksVersiAplikasi.setText(getString(R.string.pengaturan_versi, BuildConfig.VERSION_NAME));

        siapkanPemilihTema();
        siapkanSakelarPengingat();

        ikat.tombolCadangkanData.setOnClickListener(v -> {
            String namaBerkas = "TabunganKu-Cadangan-"
                + FormatTanggal.kePenandaBerkas(System.currentTimeMillis()) + ".zip";
            peluncurBuatCadangan.launch(namaBerkas);
        });

        ikat.tombolPulihkanData.setOnClickListener(
            v -> peluncurPulihkanCadangan.launch(new String[]{"application/zip", "application/octet-stream"})
        );
    }

    private void siapkanPemilihTema() {
        int temaSaatIni = preferensi.ambilPilihanTema();
        if (temaSaatIni == KonstantaAplikasi.TEMA_TERANG) {
            ikat.radioTemaTerang.setChecked(true);
        } else if (temaSaatIni == KonstantaAplikasi.TEMA_GELAP) {
            ikat.radioTemaGelap.setChecked(true);
        } else {
            ikat.radioTemaSistem.setChecked(true);
        }

        ikat.grupRadioTema.setOnCheckedChangeListener((grup, idTerpilih) -> {
            int pilihanBaru = KonstantaAplikasi.TEMA_SISTEM;
            if (idTerpilih == R.id.radio_tema_terang) {
                pilihanBaru = KonstantaAplikasi.TEMA_TERANG;
            } else if (idTerpilih == R.id.radio_tema_gelap) {
                pilihanBaru = KonstantaAplikasi.TEMA_GELAP;
            }
            if (pilihanBaru != preferensi.ambilPilihanTema()) {
                preferensi.simpanPilihanTema(pilihanBaru);
            }
        });
    }

    private void siapkanSakelarPengingat() {
        sedangMenyesuaikanSwitch = true;
        ikat.sakelarPengingat.setChecked(preferensi.apakahPengingatAktif());
        sedangMenyesuaikanSwitch = false;

        ikat.sakelarPengingat.setOnCheckedChangeListener((tombol, aktif) -> {
            if (sedangMenyesuaikanSwitch) {
                return;
            }
            if (!aktif) {
                aktifkanPengingatSekarang(false);
                return;
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                int izin = ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.POST_NOTIFICATIONS
                );
                if (izin != PackageManager.PERMISSION_GRANTED) {
                    peluncurIzinNotifikasi.launch(Manifest.permission.POST_NOTIFICATIONS);
                    return;
                }
            }
            aktifkanPengingatSekarang(true);
        });
    }

    private void aktifkanPengingatSekarang(boolean aktif) {
        preferensi.simpanPengingatAktif(aktif);
        PengelolaPengingat.aturPengingatHarian(requireContext(), aktif);
        Snackbar.make(
            ikat.getRoot(),
            aktif ? "Pengingat harian pukul 20.00 diaktifkan." : "Pengingat harian dimatikan.",
            Snackbar.LENGTH_SHORT
        ).show();
    }

    private void pulihkanDariUri(android.net.Uri uri) {
        RepositoriTabungan.ambilInstansi(requireContext()).ambilPelaksanaIo().execute(() -> {
            try {
                int jumlah = PengelolaCadangan.pulihkanDariUri(requireContext(), uri);
                if (!isAdded()) {
                    return;
                }
                requireActivity().runOnUiThread(() -> Snackbar.make(
                    ikat.getRoot(),
                    "Pemulihan selesai (" + jumlah + " target dimuat ulang).",
                    Snackbar.LENGTH_LONG
                ).show());
            } catch (Exception galat) {
                if (!isAdded()) {
                    return;
                }
                requireActivity().runOnUiThread(() -> Snackbar.make(
                    ikat.getRoot(),
                    "Gagal memulihkan cadangan: " + galat.getMessage(),
                    Snackbar.LENGTH_LONG
                ).show());
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        ikat = null;
    }
}
