package com.hairift.tabunganku.data.repositori;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.lifecycle.LiveData;

import com.hairift.tabunganku.data.dao.TargetTabunganDao;
import com.hairift.tabunganku.data.dao.TransaksiTabunganDao;
import com.hairift.tabunganku.data.lokal.BasisDataTabungan;
import com.hairift.tabunganku.data.lokal.PenyimpanFoto;
import com.hairift.tabunganku.data.model.TargetTabungan;
import com.hairift.tabunganku.data.model.TransaksiDenganTarget;
import com.hairift.tabunganku.data.model.TransaksiTabungan;
import com.hairift.tabunganku.util.FormatMataUang;
import com.hairift.tabunganku.util.HasilOperasi;
import com.hairift.tabunganku.util.KonstantaAplikasi;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Pusat koordinasi transaksi basis data lokal agar saldo target dan mutasi selalu sinkron.
 */
public class RepositoriTabungan {

    public interface PanggilanBalik<T> {
        void selesai(HasilOperasi<T> hasil);
    }

    private static volatile RepositoriTabungan instansi;

    private final BasisDataTabungan basisData;
    private final TargetTabunganDao targetDao;
    private final TransaksiTabunganDao transaksiDao;
    private final ExecutorService pelaksanaIo;
    private final Handler handlerUtama;

    private RepositoriTabungan(Context konteks) {
        this.basisData = BasisDataTabungan.ambilInstansi(konteks);
        this.targetDao = basisData.targetTabunganDao();
        this.transaksiDao = basisData.transaksiTabunganDao();
        this.pelaksanaIo = Executors.newSingleThreadExecutor();
        this.handlerUtama = new Handler(Looper.getMainLooper());
    }

    public static RepositoriTabungan ambilInstansi(Context konteks) {
        if (instansi == null) {
            synchronized (RepositoriTabungan.class) {
                if (instansi == null) {
                    instansi = new RepositoriTabungan(konteks.getApplicationContext());
                }
            }
        }
        return instansi;
    }

    public LiveData<List<TargetTabungan>> pantauSemuaTarget() {
        return targetDao.pantauSemuaTarget();
    }

    public LiveData<TargetTabungan> pantauTarget(long idTarget) {
        return targetDao.pantauTargetBerdasarkanId(idTarget);
    }

    public LiveData<List<TransaksiTabungan>> pantauTransaksiTarget(long idTarget) {
        return transaksiDao.pantauTransaksiTarget(idTarget);
    }

    public LiveData<List<TransaksiDenganTarget>> pantauSemuaTransaksiDenganTarget() {
        return transaksiDao.pantauSemuaTransaksiDenganTarget();
    }

    public List<TargetTabungan> ambilSemuaTargetSinkron() {
        return targetDao.ambilSemuaTargetSinkron();
    }

    public List<TransaksiDenganTarget> ambilSemuaTransaksiDenganTargetSinkron() {
        return transaksiDao.ambilSemuaTransaksiDenganTargetSinkron();
    }

    public List<TransaksiTabungan> ambilTransaksiTargetSinkron(long idTarget) {
        return transaksiDao.ambilTransaksiTargetSinkron(idTarget);
    }

    public TargetTabungan ambilTargetSinkron(long idTarget) {
        return targetDao.ambilTargetBerdasarkanIdSinkron(idTarget);
    }

    /**
     * Membuat target baru dan mencatat saldo awal sebagai setoran perdana dalam satu transaksi atomik.
     */
    public void buatTargetBaru(
        TargetTabungan targetBaru,
        PanggilanBalik<Long> panggilanBalik
    ) {
        pelaksanaIo.execute(() -> {
            try {
                long idTerbuat = basisData.runInTransaction(() -> {
                    long saldoAwal = Math.max(0L, targetBaru.saldoTerkumpul);
                    long id = targetDao.sisipkan(targetBaru);
                    if (saldoAwal > 0L) {
                        TransaksiTabungan setoranAwal = new TransaksiTabungan(
                            id,
                            KonstantaAplikasi.JENIS_SETOR,
                            saldoAwal,
                            "Saldo awal target",
                            targetBaru.dibuatPadaEpoch,
                            targetBaru.dibuatPadaEpoch
                        );
                        transaksiDao.sisipkan(setoranAwal);
                    }
                    return id;
                });
                kirimSukses(panggilanBalik, idTerbuat);
            } catch (Exception galat) {
                kirimGagal(panggilanBalik, "Gagal menyimpan target: " + galat.getMessage());
            }
        });
    }

    /**
     * Memperbarui detail target tanpa mengubah saldo berjalan serta menghapus foto lama bila diganti.
     */
    public void perbaruiTarget(
        TargetTabungan targetTerbaru,
        String pathFotoLama,
        PanggilanBalik<Void> panggilanBalik
    ) {
        pelaksanaIo.execute(() -> {
            try {
                targetTerbaru.diperbaruiPadaEpoch = System.currentTimeMillis();
                targetDao.perbarui(targetTerbaru);
                if (pathFotoLama != null && !pathFotoLama.equals(targetTerbaru.pathFotoMotivasi)) {
                    PenyimpanFoto.hapusBerkasFoto(pathFotoLama);
                }
                kirimSukses(panggilanBalik, null);
            } catch (Exception galat) {
                kirimGagal(panggilanBalik, "Gagal memperbarui target: " + galat.getMessage());
            }
        });
    }

    /**
     * Mengubah status arsip target tabungan.
     */
    public void aturStatusArsip(long idTarget, boolean arsipkan, PanggilanBalik<Void> panggilanBalik) {
        pelaksanaIo.execute(() -> {
            try {
                TargetTabungan target = targetDao.ambilTargetBerdasarkanIdSinkron(idTarget);
                if (target == null) {
                    kirimGagal(panggilanBalik, "Target tidak ditemukan.");
                    return;
                }
                target.diarsipkan = arsipkan;
                target.diperbaruiPadaEpoch = System.currentTimeMillis();
                targetDao.perbarui(target);
                kirimSukses(panggilanBalik, null);
            } catch (Exception galat) {
                kirimGagal(panggilanBalik, "Gagal mengubah status arsip: " + galat.getMessage());
            }
        });
    }

    /**
     * Mencatat setoran atau penarikan dana secara atomik dengan validasi saldo tidak negatif.
     */
    public void catatTransaksi(
        long idTarget,
        String jenis,
        long nominal,
        String catatan,
        long tanggalTransaksiEpoch,
        PanggilanBalik<TargetTabungan> panggilanBalik
    ) {
        pelaksanaIo.execute(() -> {
            try {
                if (nominal <= 0L) {
                    kirimGagal(panggilanBalik, "Nominal transaksi harus lebih dari Rp 0.");
                    return;
                }
                TargetTabungan targetTerbarui = basisData.runInTransaction(() -> {
                    TargetTabungan target = targetDao.ambilTargetBerdasarkanIdSinkron(idTarget);
                    if (target == null) {
                        throw new IllegalStateException("Target tabungan tidak ditemukan.");
                    }
                    boolean apakahSetor = KonstantaAplikasi.JENIS_SETOR.equals(jenis);
                    if (!apakahSetor && nominal > target.saldoTerkumpul) {
                        throw new IllegalStateException(
                            "Penarikan melebihi saldo tersedia ("
                                + FormatMataUang.keRupiah(target.saldoTerkumpul) + ")."
                        );
                    }
                    long sekarang = System.currentTimeMillis();
                    TransaksiTabungan transaksi = new TransaksiTabungan(
                        idTarget,
                        apakahSetor ? KonstantaAplikasi.JENIS_SETOR : KonstantaAplikasi.JENIS_TARIK,
                        nominal,
                        catatan != null && !catatan.trim().isEmpty() ? catatan.trim() : null,
                        tanggalTransaksiEpoch,
                        sekarang
                    );
                    transaksiDao.sisipkan(transaksi);

                    target.saldoTerkumpul = apakahSetor
                        ? target.saldoTerkumpul + nominal
                        : target.saldoTerkumpul - nominal;
                    target.diperbaruiPadaEpoch = sekarang;
                    targetDao.perbarui(target);
                    return target;
                });
                kirimSukses(panggilanBalik, targetTerbarui);
            } catch (IllegalStateException galatValidasi) {
                kirimGagal(panggilanBalik, galatValidasi.getMessage());
            } catch (Exception galat) {
                kirimGagal(panggilanBalik, "Gagal mencatat transaksi: " + galat.getMessage());
            }
        });
    }

    /**
     * Menghapus satu transaksi dan mengembalikan saldo target secara akurat dalam satu transaksi DB.
     */
    public void hapusTransaksi(TransaksiTabungan transaksi, PanggilanBalik<Void> panggilanBalik) {
        pelaksanaIo.execute(() -> {
            try {
                basisData.runInTransaction(() -> {
                    TargetTabungan target = targetDao.ambilTargetBerdasarkanIdSinkron(transaksi.idTarget);
                    if (target == null) {
                        throw new IllegalStateException("Target induk tidak ditemukan.");
                    }
                    long saldoSesudah = transaksi.apakahSetoran()
                        ? target.saldoTerkumpul - transaksi.nominal
                        : target.saldoTerkumpul + transaksi.nominal;
                    if (saldoSesudah < 0L) {
                        throw new IllegalStateException(
                            "Setoran tidak dapat dihapus karena sebagian dananya sudah ditarik."
                        );
                    }
                    transaksiDao.hapus(transaksi);
                    target.saldoTerkumpul = saldoSesudah;
                    target.diperbaruiPadaEpoch = System.currentTimeMillis();
                    targetDao.perbarui(target);
                    return null;
                });
                kirimSukses(panggilanBalik, null);
            } catch (IllegalStateException galatAturan) {
                kirimGagal(panggilanBalik, galatAturan.getMessage());
            } catch (Exception galat) {
                kirimGagal(panggilanBalik, "Gagal menghapus transaksi: " + galat.getMessage());
            }
        });
    }

    /**
     * Menghapus target beserta seluruh riwayat transaksinya (cascade) dan foto lokal terkait.
     */
    public void hapusTarget(TargetTabungan target, PanggilanBalik<Void> panggilanBalik) {
        pelaksanaIo.execute(() -> {
            try {
                targetDao.hapus(target);
                PenyimpanFoto.hapusBerkasFoto(target.pathFotoMotivasi);
                kirimSukses(panggilanBalik, null);
            } catch (Exception galat) {
                kirimGagal(panggilanBalik, "Gagal menghapus target: " + galat.getMessage());
            }
        });
    }

    public ExecutorService ambilPelaksanaIo() {
        return pelaksanaIo;
    }

    private <T> void kirimSukses(PanggilanBalik<T> panggilanBalik, T data) {
        if (panggilanBalik == null) {
            return;
        }
        handlerUtama.post(() -> panggilanBalik.selesai(HasilOperasi.sukses(data)));
    }

    private <T> void kirimGagal(PanggilanBalik<T> panggilanBalik, String pesan) {
        if (panggilanBalik == null) {
            return;
        }
        handlerUtama.post(() -> panggilanBalik.selesai(HasilOperasi.gagal(pesan)));
    }
}
