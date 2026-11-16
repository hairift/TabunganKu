package com.hairift.tabunganku.util;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import java.util.Calendar;

/**
 * Mengatur kanal notifikasi lokal serta penjadwalan pengingat menabung harian pukul 20.00.
 */
public final class PengelolaPengingat {

    public static final String ID_KANAL = "kanal_pengingat_tabungan";
    private static final int KODE_PERMINTAAN_ALARM = 2001;

    private PengelolaPengingat() {
    }

    public static void siapkanKanalNotifikasi(Context konteks) {
        NotificationManager pengelola =
            (NotificationManager) konteks.getSystemService(Context.NOTIFICATION_SERVICE);
        if (pengelola == null) {
            return;
        }
        NotificationChannel kanal = new NotificationChannel(
            ID_KANAL,
            "Pengingat Menabung",
            NotificationManager.IMPORTANCE_DEFAULT
        );
        kanal.setDescription("Pengingat harian lokal untuk mencatat setoran tabungan.");
        pengelola.createNotificationChannel(kanal);
    }

    /**
     * Menjadwalkan atau membatalkan pengingat harian pukul 20.00 waktu perangkat.
     */
    public static void aturPengingatHarian(Context konteks, boolean aktif) {
        siapkanKanalNotifikasi(konteks);
        AlarmManager pengelolaAlarm =
            (AlarmManager) konteks.getSystemService(Context.ALARM_SERVICE);
        if (pengelolaAlarm == null) {
            return;
        }

        Intent niat = new Intent(konteks, PenerimaPengingat.class);
        niat.setAction(PenerimaPengingat.AKSI_INGATKAN);
        PendingIntent niatTertunda = PendingIntent.getBroadcast(
            konteks,
            KODE_PERMINTAAN_ALARM,
            niat,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        if (!aktif) {
            pengelolaAlarm.cancel(niatTertunda);
            return;
        }

        Calendar jadwal = Calendar.getInstance();
        jadwal.set(Calendar.HOUR_OF_DAY, 20);
        jadwal.set(Calendar.MINUTE, 0);
        jadwal.set(Calendar.SECOND, 0);
        if (jadwal.getTimeInMillis() <= System.currentTimeMillis()) {
            jadwal.add(Calendar.DAY_OF_MONTH, 1);
        }

        pengelolaAlarm.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            jadwal.getTimeInMillis(),
            AlarmManager.INTERVAL_DAY,
            niatTertunda
        );
    }
}
