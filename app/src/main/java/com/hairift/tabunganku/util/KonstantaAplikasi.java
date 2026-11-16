package com.hairift.tabunganku.util;

/**
 * Konstanta global aplikasi TabunganKu yang dipakai lintas lapisan.
 */
public final class KonstantaAplikasi {

    public static final String JENIS_SETOR = "SETOR";
    public static final String JENIS_TARIK = "TARIK";

    public static final String[] DAFTAR_KATEGORI = {
        "Rumah & Properti",
        "Kendaraan",
        "Dana Darurat",
        "Pendidikan",
        "Perjalanan",
        "Elektronik",
        "Lainnya"
    };

    public static final int TEMA_SISTEM = 0;
    public static final int TEMA_TERANG = 1;
    public static final int TEMA_GELAP = 2;

    public static final String EKSTRA_ID_TARGET = "ekstra_id_target";
    public static final String EKSTRA_JALUR_FOTO = "ekstra_jalur_foto";
    public static final String EKSTRA_JUDUL_FOTO = "ekstra_judul_foto";

    public static final String FOLDER_FOTO = "foto_target";
    public static final String FOLDER_EKSPOR = "ekspor";
    public static final String FOLDER_UNDUHAN_PUBLIK = "TabunganKu";

    private KonstantaAplikasi() {
    }
}
