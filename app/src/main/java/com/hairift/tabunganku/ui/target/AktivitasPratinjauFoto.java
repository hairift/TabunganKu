package com.hairift.tabunganku.ui.target;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.hairift.tabunganku.data.lokal.PenyimpanFoto;
import com.hairift.tabunganku.databinding.ActivityPratinjauFotoBinding;
import com.hairift.tabunganku.util.KonstantaAplikasi;

/**
 * Menampilkan foto motivasi target dalam layar penuh agar mudah dilihat sebagai penyemangat.
 */
public class AktivitasPratinjauFoto extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle keadaanTersimpan) {
        super.onCreate(keadaanTersimpan);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        ActivityPratinjauFotoBinding ikat = ActivityPratinjauFotoBinding.inflate(getLayoutInflater());
        setContentView(ikat.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(ikat.bilahAtasPratinjau, (v, insets) -> {
            Insets tepi = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(tepi.left + v.getPaddingLeft(), tepi.top + 16, tepi.right + 16, 16);
            return insets;
        });

        String jalurFoto = getIntent().getStringExtra(KonstantaAplikasi.EKSTRA_JALUR_FOTO);
        String judul = getIntent().getStringExtra(KonstantaAplikasi.EKSTRA_JUDUL_FOTO);
        if (judul != null) {
            ikat.teksJudulPratinjau.setText(judul);
        }
        PenyimpanFoto.muatKeImageView(ikat.gambarPratinjauPenuh, jalurFoto, 1600);
        ikat.tombolTutupPratinjau.setOnClickListener(v -> finish());
    }
}
