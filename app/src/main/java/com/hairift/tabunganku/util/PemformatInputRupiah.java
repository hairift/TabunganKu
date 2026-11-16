package com.hairift.tabunganku.util;

import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

/**
 * Pemformat input angka ribuan otomatis saat mengetik nominal uang.
 */
public class PemformatInputRupiah implements TextWatcher {

    private final EditText bidangInput;
    private boolean sedangMemperbarui;

    public PemformatInputRupiah(EditText bidangInput) {
        this.bidangInput = bidangInput;
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
    }

    @Override
    public void afterTextChanged(Editable teks) {
        if (sedangMemperbarui) {
            return;
        }
        sedangMemperbarui = true;
        String asli = teks.toString();
        if (!asli.isEmpty()) {
            long nilai = FormatMataUang.uraiDariTeks(asli);
            String hasil = nilai > 0 ? FormatMataUang.keAngkaRibuan(nilai) : "";
            if (!hasil.equals(asli)) {
                bidangInput.setText(hasil);
                bidangInput.setSelection(hasil.length());
            }
        }
        sedangMemperbarui = false;
    }
}
