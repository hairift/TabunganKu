package com.hairift.tabunganku.ui.utama;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.hairift.tabunganku.R;
import com.hairift.tabunganku.databinding.ActivityUtamaBinding;
import com.hairift.tabunganku.ui.beranda.FragmenBeranda;
import com.hairift.tabunganku.ui.laporan.FragmenLaporan;
import com.hairift.tabunganku.ui.pengaturan.FragmenPengaturan;
import com.hairift.tabunganku.ui.riwayat.FragmenRiwayat;

/**
 * Aktivitas induk penampung empat tab utama navigasi bawah TabunganKu.
 */
public class AktivitasUtama extends AppCompatActivity {

    private static final String KUNCI_TAB_AKTIF = "kunci_tab_aktif";

    private ActivityUtamaBinding pengikatan;
    private int idMenuAktif = R.id.menu_beranda;

    @Override
    protected void onCreate(Bundle keadaanTersimpan) {
        super.onCreate(keadaanTersimpan);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        pengikatan = ActivityUtamaBinding.inflate(getLayoutInflater());
        setContentView(pengikatan.getRoot());

        terapkanJarakTepiLayar();

        if (keadaanTersimpan != null) {
            idMenuAktif = keadaanTersimpan.getInt(KUNCI_TAB_AKTIF, R.id.menu_beranda);
        }

        pengikatan.navigasiBawah.setOnItemSelectedListener(item -> {
            idMenuAktif = item.getItemId();
            tampilkanFragmen(idMenuAktif);
            return true;
        });

        if (keadaanTersimpan == null) {
            tampilkanFragmen(idMenuAktif);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle keluarState) {
        super.onSaveInstanceState(keluarState);
        keluarState.putInt(KUNCI_TAB_AKTIF, idMenuAktif);
    }

    private void terapkanJarakTepiLayar() {
        ViewCompat.setOnApplyWindowInsetsListener(pengikatan.wadahFragmen, (tampilan, insets) -> {
            Insets tepiSistem = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            tampilan.setPadding(tepiSistem.left, tepiSistem.top, tepiSistem.right, 0);
            return insets;
        });
        ViewCompat.setOnApplyWindowInsetsListener(pengikatan.navigasiBawah, (tampilan, insets) -> {
            Insets tepiSistem = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            tampilan.setPadding(tepiSistem.left, 0, tepiSistem.right, tepiSistem.bottom);
            return insets;
        });
    }

    private void tampilkanFragmen(int idMenu) {
        String penanda = "TAB_" + idMenu;
        Fragment fragmenAda = getSupportFragmentManager().findFragmentByTag(penanda);
        FragmentTransaction transaksi = getSupportFragmentManager().beginTransaction();

        for (Fragment fragmenLain : getSupportFragmentManager().getFragments()) {
            if (fragmenLain != null && !penanda.equals(fragmenLain.getTag())) {
                transaksi.hide(fragmenLain);
            }
        }

        if (fragmenAda == null) {
            fragmenAda = buatFragmenUntukMenu(idMenu);
            transaksi.add(R.id.wadah_fragmen, fragmenAda, penanda);
        } else {
            transaksi.show(fragmenAda);
        }
        transaksi.commit();
    }

    private Fragment buatFragmenUntukMenu(int idMenu) {
        if (idMenu == R.id.menu_riwayat) {
            return new FragmenRiwayat();
        }
        if (idMenu == R.id.menu_laporan) {
            return new FragmenLaporan();
        }
        if (idMenu == R.id.menu_pengaturan) {
            return new FragmenPengaturan();
        }
        return new FragmenBeranda();
    }
}
