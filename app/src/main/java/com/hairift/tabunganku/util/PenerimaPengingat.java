package com.hairift.tabunganku.util;

import android.Manifest;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.hairift.tabunganku.R;
import com.hairift.tabunganku.data.lokal.PengelolaPreferensi;
import com.hairift.tabunganku.ui.utama.AktivitasUtama;

/**
 * Menampilkan notifikasi pengingat lokal atau menjadwalkan ulang setelah perangkat menyala kembali.
 */
public class PenerimaPengingat extends BroadcastReceiver {

    public static final String AKSI_INGATKAN = "com.hairift.tabunganku.AKSI_INGATKAN";
    private static final int ID_NOTIFIKASI = 3001;

    @Override
    public void onReceive(Context konteks, Intent intent) {
        String aksi = intent != null ? intent.getAction() : null;
        if (Intent.ACTION_BOOT_COMPLETED.equals(aksi)) {
            PengelolaPreferensi preferensi = new PengelolaPreferensi(konteks);
            if (preferensi.apakahPengingatAktif()) {
                PengelolaPengingat.aturPengingatHarian(konteks, true);
            }
            return;
        }

        if (AKSI_INGATKAN.equals(aksi)) {
            tampilkanNotifikasi(konteks);
        }
    }

    private void tampilkanNotifikasi(Context konteks) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            int izin = ContextCompat.checkSelfPermission(
                konteks,
                Manifest.permission.POST_NOTIFICATIONS
            );
            if (izin != PackageManager.PERMISSION_GRANTED) {
                return;
            }
        }

        PengelolaPengingat.siapkanKanalNotifikasi(konteks);

        Intent bukaAplikasi = new Intent(konteks, AktivitasUtama.class);
        bukaAplikasi.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent niatBuka = PendingIntent.getActivity(
            konteks,
            0,
            bukaAplikasi,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder pembangun =
            new NotificationCompat.Builder(konteks, PengelolaPengingat.ID_KANAL)
                .setSmallIcon(R.drawable.ic_nav_beranda)
                .setContentTitle("Waktunya menyisihkan tabungan hari ini")
                .setContentText("Buka TabunganKu dan catat setoran kecil menuju target impianmu.")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(niatBuka)
                .setAutoCancel(true);

        NotificationManager pengelola =
            (NotificationManager) konteks.getSystemService(Context.NOTIFICATION_SERVICE);
        if (pengelola != null) {
            pengelola.notify(ID_NOTIFIKASI, pembangun.build());
        }
    }
}
