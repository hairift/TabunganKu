package com.hairift.tabunganku.ui.target;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.snackbar.Snackbar;
import com.hairift.tabunganku.R;
import com.hairift.tabunganku.data.lokal.PenyimpanFoto;
import com.hairift.tabunganku.data.model.TargetTabungan;
import com.hairift.tabunganku.data.repositori.RepositoriTabungan;
import com.hairift.tabunganku.databinding.ActivityTambahUbahTargetBinding;
import com.hairift.tabunganku.util.FormatMataUang;
import com.hairift.tabunganku.util.FormatTanggal;
import com.hairift.tabunganku.util.KonstantaAplikasi;
import com.hairift.tabunganku.util.PemformatInputRupiah;

import java.io.IOException;

/**
 * Layar pembuatan target tabungan baru dan penyuntingan target yang sudah ada beserta foto motivasi.
 */
public class AktivitasTambahUbahTarget extends AppCompatActivity {

    private ActivityTambahUbahTargetBinding ikat;
    private long idTargetDiedit = -1L;
    private TargetTabungan targetAsli;
    private String pathFotoSaatIni;
    private String pathFotoLama;
    private long tanggalTargetEpoch;

    private final ActivityResultLauncher<PickVisualMediaRequest> peluncurPemilihFoto =
        registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
            if (uri == null) {
                return;
            }
            RepositoriTabungan.ambilInstansi(this).ambilPelaksanaIo().execute(() -> {
                try {
                    String jalurTersalin = PenyimpanFoto.salinDariUri(this, uri);
                    runOnUiThread(() -> {
                        if (pathFotoSaatIni != null && !pathFotoSaatIni.equals(pathFotoLama)) {
                            PenyimpanFoto.hapusBerkasFoto(pathFotoSaatIni);
                        }
                        pathFotoSaatIni = jalurTersalin;
                        perbaruiPratinjauFoto();
                    });
                } catch (IOException galat) {
                    runOnUiThread(() -> Snackbar.make(
                        ikat.getRoot(),
                        R.string.galat_foto_gagal_dimuat,
                        Snackbar.LENGTH_LONG
                    ).show());
                }
            });
        });

    @Override
    protected void onCreate(Bundle keadaanTersimpan) {
        super.onCreate(keadaanTersimpan);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        ikat = ActivityTambahUbahTargetBinding.inflate(getLayoutInflater());
        setContentView(ikat.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(ikat.getRoot(), (v, insets) -> {
            Insets tepi = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(tepi.left, tepi.top, tepi.right, tepi.bottom);
            return insets;
        });

        idTargetDiedit = getIntent().getLongExtra(KonstantaAplikasi.EKSTRA_ID_TARGET, -1L);

        ArrayAdapter<String> adaptorKategori = new ArrayAdapter<>(
            this,
            android.R.layout.simple_dropdown_item_1line,
            KonstantaAplikasi.DAFTAR_KATEGORI
        );
        ikat.inputKategoriTarget.setAdapter(adaptorKategori);
        ikat.inputKategoriTarget.setText(KonstantaAplikasi.DAFTAR_KATEGORI[0], false);

        ikat.inputNominalTarget.addTextChangedListener(new PemformatInputRupiah(ikat.inputNominalTarget));
        ikat.inputSaldoAwal.addTextChangedListener(new PemformatInputRupiah(ikat.inputSaldoAwal));

        ikat.tombolKembaliForm.setOnClickListener(v -> finish());
        ikat.wadahFotoForm.setOnClickListener(v -> pilihFotoDariGaleri());
        ikat.tombolPilihFoto.setOnClickListener(v -> pilihFotoDariGaleri());
        ikat.tombolHapusFoto.setOnClickListener(v -> {
            if (pathFotoSaatIni != null && !pathFotoSaatIni.equals(pathFotoLama)) {
                PenyimpanFoto.hapusBerkasFoto(pathFotoSaatIni);
            }
            pathFotoSaatIni = null;
            perbaruiPratinjauFoto();
        });

        ikat.tombolPilihTanggalTarget.setOnClickListener(v -> bukaPemilihTanggal());
        ikat.tombolHapusTanggalTarget.setOnClickListener(v -> {
            tanggalTargetEpoch = 0L;
            perbaruiTampilanTanggal();
        });

        ikat.tombolSimpanTarget.setOnClickListener(v -> validasiDanSimpan());

        if (idTargetDiedit > 0L) {
            muatDataTargetUntukUbah();
        } else {
            perbaruiPratinjauFoto();
            perbaruiTampilanTanggal();
        }
    }

    private void muatDataTargetUntukUbah() {
        ikat.teksJudulForm.setText(R.string.judul_ubah_target);
        ikat.tombolSimpanTarget.setText(R.string.tombol_perbarui_target);
        ikat.kelompokSaldoAwal.setVisibility(View.GONE);

        RepositoriTabungan.ambilInstansi(this).ambilPelaksanaIo().execute(() -> {
            TargetTabungan ditemukan = RepositoriTabungan.ambilInstansi(this)
                .ambilTargetSinkron(idTargetDiedit);
            runOnUiThread(() -> {
                if (ditemukan == null) {
                    finish();
                    return;
                }
                targetAsli = ditemukan;
                pathFotoSaatIni = ditemukan.pathFotoMotivasi;
                pathFotoLama = ditemukan.pathFotoMotivasi;
                tanggalTargetEpoch = ditemukan.tanggalTargetEpoch;

                ikat.inputNamaTarget.setText(ditemukan.namaTarget);
                ikat.inputKategoriTarget.setText(ditemukan.kategori, false);
                ikat.inputNominalTarget.setText(FormatMataUang.keAngkaRibuan(ditemukan.nominalTarget));
                if (ditemukan.catatanMotivasi != null) {
                    ikat.inputCatatanMotivasi.setText(ditemukan.catatanMotivasi);
                }
                perbaruiPratinjauFoto();
                perbaruiTampilanTanggal();
            });
        });
    }

    private void pilihFotoDariGaleri() {
        peluncurPemilihFoto.launch(
            new PickVisualMediaRequest.Builder()
                .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                .build()
        );
    }

    private void perbaruiPratinjauFoto() {
        if (pathFotoSaatIni != null && !pathFotoSaatIni.trim().isEmpty()) {
            PenyimpanFoto.muatKeImageView(ikat.pratinjauFotoForm, pathFotoSaatIni, 900);
            ikat.tombolPilihFoto.setText(R.string.tombol_ganti_foto);
            ikat.tombolHapusFoto.setVisibility(View.VISIBLE);
        } else {
            ikat.pratinjauFotoForm.setImageResource(R.drawable.ic_foto_kosong);
            ikat.tombolPilihFoto.setText(R.string.tombol_pilih_foto);
            ikat.tombolHapusFoto.setVisibility(View.GONE);
        }
    }

    private void bukaPemilihTanggal() {
        long pilihanAwal = tanggalTargetEpoch > 0L
            ? tanggalTargetEpoch
            : System.currentTimeMillis();
        MaterialDatePicker<Long> pemilih = MaterialDatePicker.Builder.datePicker()
            .setTitleText(R.string.label_tanggal_target)
            .setSelection(pilihanAwal)
            .build();
        pemilih.addOnPositiveButtonClickListener(pilihan -> {
            if (pilihan != null) {
                tanggalTargetEpoch = pilihan;
                perbaruiTampilanTanggal();
            }
        });
        pemilih.show(getSupportFragmentManager(), "PEMILIH_TANGGAL_TARGET");
    }

    private void perbaruiTampilanTanggal() {
        if (tanggalTargetEpoch > 0L) {
            ikat.tombolPilihTanggalTarget.setText(FormatTanggal.keTanggalPenuh(tanggalTargetEpoch));
            ikat.tombolHapusTanggalTarget.setVisibility(View.VISIBLE);
        } else {
            ikat.tombolPilihTanggalTarget.setText(R.string.petunjuk_tanggal_belum_dipilih);
            ikat.tombolHapusTanggalTarget.setVisibility(View.GONE);
        }
    }

    private void validasiDanSimpan() {
        String nama = ikat.inputNamaTarget.getText() != null
            ? ikat.inputNamaTarget.getText().toString().trim()
            : "";
        if (nama.isEmpty()) {
            ikat.wadahNamaTarget.setError(getString(R.string.galat_nama_wajib));
            ikat.inputNamaTarget.requestFocus();
            return;
        }
        ikat.wadahNamaTarget.setError(null);

        String teksNominal = ikat.inputNominalTarget.getText() != null
            ? ikat.inputNominalTarget.getText().toString()
            : "";
        long nominalTarget = FormatMataUang.uraiDariTeks(teksNominal);
        if (nominalTarget <= 0L) {
            ikat.wadahNominalTarget.setError(getString(R.string.galat_nominal_target_wajib));
            ikat.inputNominalTarget.requestFocus();
            return;
        }
        ikat.wadahNominalTarget.setError(null);

        String kategori = ikat.inputKategoriTarget.getText() != null
            ? ikat.inputKategoriTarget.getText().toString().trim()
            : KonstantaAplikasi.DAFTAR_KATEGORI[0];
        if (kategori.isEmpty()) {
            kategori = KonstantaAplikasi.DAFTAR_KATEGORI[0];
        }

        String catatan = ikat.inputCatatanMotivasi.getText() != null
            ? ikat.inputCatatanMotivasi.getText().toString().trim()
            : "";
        String catatanBersih = catatan.isEmpty() ? null : catatan;

        ikat.tombolSimpanTarget.setEnabled(false);
        RepositoriTabungan repositori = RepositoriTabungan.ambilInstansi(this);

        if (targetAsli != null) {
            targetAsli.namaTarget = nama;
            targetAsli.kategori = kategori;
            targetAsli.nominalTarget = nominalTarget;
            targetAsli.pathFotoMotivasi = pathFotoSaatIni;
            targetAsli.catatanMotivasi = catatanBersih;
            targetAsli.tanggalTargetEpoch = tanggalTargetEpoch;

            repositori.perbaruiTarget(targetAsli, pathFotoLama, hasil -> {
                ikat.tombolSimpanTarget.setEnabled(true);
                if (hasil.apakahBerhasil()) {
                    finish();
                } else {
                    Snackbar.make(ikat.getRoot(), hasil.ambilPesanGalat(), Snackbar.LENGTH_LONG).show();
                }
            });
            return;
        }

        String teksSaldoAwal = ikat.inputSaldoAwal.getText() != null
            ? ikat.inputSaldoAwal.getText().toString()
            : "";
        long saldoAwal = FormatMataUang.uraiDariTeks(teksSaldoAwal);
        long sekarang = System.currentTimeMillis();

        TargetTabungan targetBaru = new TargetTabungan(
            nama,
            kategori,
            nominalTarget,
            saldoAwal,
            pathFotoSaatIni,
            catatanBersih,
            tanggalTargetEpoch,
            sekarang
        );

        repositori.buatTargetBaru(targetBaru, hasil -> {
            ikat.tombolSimpanTarget.setEnabled(true);
            if (hasil.apakahBerhasil()) {
                finish();
            } else {
                Snackbar.make(ikat.getRoot(), hasil.ambilPesanGalat(), Snackbar.LENGTH_LONG).show();
            }
        });
    }
}
