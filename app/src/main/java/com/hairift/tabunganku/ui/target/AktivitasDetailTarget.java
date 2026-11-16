package com.hairift.tabunganku.ui.target;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.hairift.tabunganku.R;
import com.hairift.tabunganku.data.lokal.PenyimpanFoto;
import com.hairift.tabunganku.data.model.TargetTabungan;
import com.hairift.tabunganku.data.model.TransaksiDenganTarget;
import com.hairift.tabunganku.data.model.TransaksiTabungan;
import com.hairift.tabunganku.data.repositori.RepositoriTabungan;
import com.hairift.tabunganku.databinding.ActivityDetailTargetBinding;
import com.hairift.tabunganku.ekspor.PengelolaEkspor;
import com.hairift.tabunganku.util.FormatMataUang;
import com.hairift.tabunganku.util.FormatTanggal;
import com.hairift.tabunganku.util.KalkulatorTarget;
import com.hairift.tabunganku.util.KonstantaAplikasi;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Layar detail target tabungan berisi foto impian ukuran besar, metrik progres, dan mutasi transaksi.
 */
public class AktivitasDetailTarget extends AppCompatActivity
    implements AdaptorRiwayatTransaksi.PendengarHapusTransaksi {

    private ActivityDetailTargetBinding ikat;
    private AdaptorRiwayatTransaksi adaptorTransaksi;
    private long idTarget;
    private TargetTabungan targetSaatIni;
    private PengelolaEkspor.HasilEkspor hasilEksporTerakhir;

    private final ActivityResultLauncher<String> peluncurSimpanSaf =
        registerForActivityResult(
            new ActivityResultContracts.CreateDocument(PengelolaEkspor.MIME_EXCEL),
            uriTujuan -> {
                if (uriTujuan == null || hasilEksporTerakhir == null) {
                    return;
                }
                RepositoriTabungan.ambilInstansi(this).ambilPelaksanaIo().execute(() -> {
                    try {
                        PengelolaEkspor.salinBerkasKeUri(
                            this,
                            hasilEksporTerakhir.berkasLokal,
                            uriTujuan
                        );
                        runOnUiThread(() -> Snackbar.make(
                            ikat.getRoot(),
                            "Berkas Excel target berhasil disimpan.",
                            Snackbar.LENGTH_LONG
                        ).show());
                    } catch (IOException galat) {
                        runOnUiThread(() -> Snackbar.make(
                            ikat.getRoot(),
                            "Gagal menyimpan ke lokasi tersebut: " + galat.getMessage(),
                            Snackbar.LENGTH_LONG
                        ).show());
                    }
                });
            }
        );

    @Override
    protected void onCreate(Bundle keadaanTersimpan) {
        super.onCreate(keadaanTersimpan);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        ikat = ActivityDetailTargetBinding.inflate(getLayoutInflater());
        setContentView(ikat.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(ikat.getRoot(), (v, insets) -> {
            Insets tepi = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(tepi.left, tepi.top, tepi.right, tepi.bottom);
            return insets;
        });

        idTarget = getIntent().getLongExtra(KonstantaAplikasi.EKSTRA_ID_TARGET, -1L);
        if (idTarget <= 0L) {
            finish();
            return;
        }

        adaptorTransaksi = new AdaptorRiwayatTransaksi(this);
        ikat.daftarTransaksiDetail.setLayoutManager(new LinearLayoutManager(this));
        ikat.daftarTransaksiDetail.setAdapter(adaptorTransaksi);

        ikat.tombolKembaliDetail.setOnClickListener(v -> finish());
        ikat.tombolMenuDetail.setOnClickListener(this::tampilkanMenuTarget);

        ikat.fotoMotivasiDetail.setOnClickListener(v -> {
            if (targetSaatIni != null
                && targetSaatIni.pathFotoMotivasi != null
                && !targetSaatIni.pathFotoMotivasi.isEmpty()) {
                Intent niat = new Intent(this, AktivitasPratinjauFoto.class);
                niat.putExtra(KonstantaAplikasi.EKSTRA_JALUR_FOTO, targetSaatIni.pathFotoMotivasi);
                niat.putExtra(KonstantaAplikasi.EKSTRA_JUDUL_FOTO, targetSaatIni.namaTarget);
                startActivity(niat);
            }
        });

        ikat.tombolSetorDetail.setOnClickListener(v -> {
            if (targetSaatIni != null) {
                LembarTransaksi.buatInstansi(targetSaatIni, KonstantaAplikasi.JENIS_SETOR)
                    .show(getSupportFragmentManager(), "LEMBAR_SETOR_DETAIL");
            }
        });

        ikat.tombolTarikDetail.setOnClickListener(v -> {
            if (targetSaatIni == null) {
                return;
            }
            if (targetSaatIni.saldoTerkumpul <= 0L) {
                Snackbar.make(
                    ikat.getRoot(),
                    "Saldo target masih Rp 0 sehingga belum dapat ditarik.",
                    Snackbar.LENGTH_SHORT
                ).show();
                return;
            }
            LembarTransaksi.buatInstansi(targetSaatIni, KonstantaAplikasi.JENIS_TARIK)
                .show(getSupportFragmentManager(), "LEMBAR_TARIK_DETAIL");
        });

        RepositoriTabungan repositori = RepositoriTabungan.ambilInstansi(this);
        repositori.pantauTarget(idTarget).observe(this, target -> {
            if (target == null) {
                finish();
                return;
            }
            targetSaatIni = target;
            tampilkanDetailTarget(target);
        });

        repositori.pantauTransaksiTarget(idTarget).observe(this, daftar -> {
            if (daftar == null || daftar.isEmpty()) {
                ikat.teksRiwayatTargetKosong.setVisibility(View.VISIBLE);
                ikat.daftarTransaksiDetail.setVisibility(View.GONE);
            } else {
                ikat.teksRiwayatTargetKosong.setVisibility(View.GONE);
                ikat.daftarTransaksiDetail.setVisibility(View.VISIBLE);
                adaptorTransaksi.aturDariTransaksiTarget(daftar);
            }
        });
    }

    private void tampilkanDetailTarget(TargetTabungan target) {
        ikat.teksKategoriDetail.setText(target.kategori);
        String status = target.diarsipkan
            ? "Diarsipkan"
            : (target.apakahTercapai() ? "Tercapai" : "Berjalan");
        ikat.teksStatusDetail.setText(status);

        ikat.teksNamaDetail.setText(target.namaTarget);
        if (target.catatanMotivasi != null && !target.catatanMotivasi.trim().isEmpty()) {
            ikat.teksCatatanDetail.setVisibility(View.VISIBLE);
            ikat.teksCatatanDetail.setText(target.catatanMotivasi);
        } else {
            ikat.teksCatatanDetail.setVisibility(View.GONE);
        }

        int persen = KalkulatorTarget.hitungPersentase(target.saldoTerkumpul, target.nominalTarget);
        ikat.teksSaldoDetail.setText(FormatMataUang.keRupiah(target.saldoTerkumpul));
        ikat.teksPersenDetail.setText(persen + "%");
        ikat.progresDetail.setProgress(persen);

        ikat.teksTargetNominalDetail.setText(FormatMataUang.keRupiah(target.nominalTarget));
        if (target.saldoTerkumpul > target.nominalTarget) {
            long lebih = target.saldoTerkumpul - target.nominalTarget;
            ikat.teksSisaDetail.setText(
                getString(R.string.detail_label_kelebihan, FormatMataUang.keRupiah(lebih))
            );
        } else {
            long sisa = KalkulatorTarget.hitungSisa(target.saldoTerkumpul, target.nominalTarget);
            ikat.teksSisaDetail.setText(FormatMataUang.keRupiah(sisa));
        }

        String infoTenggat = target.tanggalTargetEpoch > 0L
            ? "Tanggal target: " + FormatTanggal.keTanggalPenuh(target.tanggalTargetEpoch)
            : "Tanpa batas tanggal target";
        ikat.teksTenggatDetail.setText(infoTenggat);
        ikat.teksRekomendasiDetail.setText(KalkulatorTarget.buatRekomendasiNabung(target));

        int lebarLayar = getResources().getDisplayMetrics().widthPixels;
        PenyimpanFoto.muatKeImageView(ikat.fotoMotivasiDetail, target.pathFotoMotivasi, lebarLayar);
    }

    private void tampilkanMenuTarget(View jangkar) {
        if (targetSaatIni == null) {
            return;
        }
        PopupMenu menu = new PopupMenu(this, jangkar);
        menu.getMenuInflater().inflate(R.menu.menu_detail_target, menu.getMenu());
        MenuItem itemArsip = menu.getMenu().findItem(R.id.aksi_arsip_target);
        if (itemArsip != null) {
            itemArsip.setTitle(
                targetSaatIni.diarsipkan
                    ? R.string.detail_menu_buka_arsip
                    : R.string.detail_menu_arsip
            );
        }
        menu.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.aksi_ubah_target) {
                Intent niat = new Intent(this, AktivitasTambahUbahTarget.class);
                niat.putExtra(KonstantaAplikasi.EKSTRA_ID_TARGET, targetSaatIni.id);
                startActivity(niat);
                return true;
            }
            if (id == R.id.aksi_ekspor_target) {
                eksporTargetTunggalKeExcel();
                return true;
            }
            if (id == R.id.aksi_arsip_target) {
                ubahStatusArsip();
                return true;
            }
            if (id == R.id.aksi_hapus_target) {
                konfirmasiHapusTarget();
                return true;
            }
            return false;
        });
        menu.show();
    }

    private void eksporTargetTunggalKeExcel() {
        TargetTabungan targetSnapshot = targetSaatIni;
        if (targetSnapshot == null) {
            return;
        }
        RepositoriTabungan repositori = RepositoriTabungan.ambilInstansi(this);
        repositori.ambilPelaksanaIo().execute(() -> {
            try {
                List<TransaksiTabungan> transaksiLokal =
                    repositori.ambilTransaksiTargetSinkron(targetSnapshot.id);
                List<TransaksiDenganTarget> daftarGabungan = new ArrayList<>();
                for (int i = transaksiLokal.size() - 1; i >= 0; i--) {
                    TransaksiTabungan tr = transaksiLokal.get(i);
                    TransaksiDenganTarget item = new TransaksiDenganTarget();
                    item.id = tr.id;
                    item.idTarget = tr.idTarget;
                    item.jenis = tr.jenis;
                    item.nominal = tr.nominal;
                    item.catatan = tr.catatan;
                    item.tanggalTransaksiEpoch = tr.tanggalTransaksiEpoch;
                    item.namaTarget = targetSnapshot.namaTarget;
                    item.kategori = targetSnapshot.kategori;
                    daftarGabungan.add(item);
                }

                PengelolaEkspor.HasilEkspor hasil = PengelolaEkspor.eksporLaporan(
                    this,
                    true,
                    "Target: " + targetSnapshot.namaTarget,
                    "TabunganKu-Target",
                    Collections.singletonList(targetSnapshot),
                    daftarGabungan
                );
                runOnUiThread(() -> tampilkanDialogEkspor(hasil));
            } catch (IOException galat) {
                runOnUiThread(() -> Snackbar.make(
                    ikat.getRoot(),
                    "Gagal membuat berkas Excel: " + galat.getMessage(),
                    Snackbar.LENGTH_LONG
                ).show());
            }
        });
    }

    private void tampilkanDialogEkspor(PengelolaEkspor.HasilEkspor hasil) {
        this.hasilEksporTerakhir = hasil;
        new MaterialAlertDialogBuilder(this)
            .setTitle(R.string.laporan_dialog_berhasil_judul)
            .setMessage("Berkas tersimpan di:\n" + hasil.lokasiUnduhanPublik)
            .setPositiveButton(R.string.laporan_dialog_buka, (d, w) -> bukaBerkas(hasil))
            .setNeutralButton(
                R.string.laporan_dialog_simpan_lain,
                (d, w) -> peluncurSimpanSaf.launch(hasil.namaBerkas)
            )
            .setNegativeButton(R.string.laporan_dialog_bagikan, (d, w) -> bagikanBerkas(hasil))
            .show();
    }

    private void bukaBerkas(PengelolaEkspor.HasilEkspor hasil) {
        Intent niat = new Intent(Intent.ACTION_VIEW);
        niat.setDataAndType(hasil.uriBagikan, hasil.tipeMime);
        niat.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            startActivity(Intent.createChooser(niat, "Buka laporan Excel"));
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

    private void ubahStatusArsip() {
        boolean tujuanArsip = !targetSaatIni.diarsipkan;
        RepositoriTabungan.ambilInstansi(this).aturStatusArsip(
            targetSaatIni.id,
            tujuanArsip,
            hasil -> {
                if (!hasil.apakahBerhasil()) {
                    Snackbar.make(ikat.getRoot(), hasil.ambilPesanGalat(), Snackbar.LENGTH_SHORT).show();
                } else {
                    Snackbar.make(
                        ikat.getRoot(),
                        tujuanArsip ? "Target dipindahkan ke arsip." : "Target diaktifkan kembali.",
                        Snackbar.LENGTH_SHORT
                    ).show();
                }
            }
        );
    }

    private void konfirmasiHapusTarget() {
        new MaterialAlertDialogBuilder(this)
            .setTitle(R.string.konfirmasi_hapus_target_judul)
            .setMessage(getString(R.string.konfirmasi_hapus_target_isi, targetSaatIni.namaTarget))
            .setNegativeButton(R.string.tombol_batal, null)
            .setPositiveButton(R.string.tombol_hapus, (d, w) ->
                RepositoriTabungan.ambilInstansi(this).hapusTarget(targetSaatIni, hasil -> {
                    if (hasil.apakahBerhasil()) {
                        finish();
                    } else {
                        Snackbar.make(ikat.getRoot(), hasil.ambilPesanGalat(), Snackbar.LENGTH_LONG).show();
                    }
                })
            )
            .show();
    }

    @Override
    public void saatHapusDiketuk(TransaksiTabungan transaksi) {
        new MaterialAlertDialogBuilder(this)
            .setTitle(R.string.konfirmasi_hapus_transaksi_judul)
            .setMessage(R.string.konfirmasi_hapus_transaksi_isi)
            .setNegativeButton(R.string.tombol_batal, null)
            .setPositiveButton(R.string.tombol_hapus, (d, w) ->
                RepositoriTabungan.ambilInstansi(this).hapusTransaksi(transaksi, hasil -> {
                    if (!hasil.apakahBerhasil()) {
                        Snackbar.make(ikat.getRoot(), hasil.ambilPesanGalat(), Snackbar.LENGTH_LONG).show();
                    } else {
                        Snackbar.make(ikat.getRoot(), "Catatan transaksi dihapus.", Snackbar.LENGTH_SHORT).show();
                    }
                })
            )
            .show();
    }
}
