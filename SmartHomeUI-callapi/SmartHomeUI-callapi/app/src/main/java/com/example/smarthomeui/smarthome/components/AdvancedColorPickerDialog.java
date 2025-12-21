package com.example.smarthomeui.smarthome.components;

import android.app.Dialog;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.ColorUtils;
import androidx.fragment.app.DialogFragment;

import com.example.smarthomeui.R;

public class AdvancedColorPickerDialog extends DialogFragment {

    public interface OnPick { void onColor(int color); }

    private static final String ARG_COLOR = "c";
    private OnPick cb;

    public static AdvancedColorPickerDialog newInstance(int color, OnPick cb){
        AdvancedColorPickerDialog f = new AdvancedColorPickerDialog();
        Bundle b = new Bundle(); b.putInt(ARG_COLOR, color);
        f.setArguments(b); f.cb = cb; return f;
    }

    private float hue, sat, val; // HSV nội bộ (SV view dùng V, không phải Lightness)

    @NonNull @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Dialog d = new Dialog(requireContext());
        View v = LayoutInflater.from(getContext()).inflate(R.layout.dialog_color_picker, null, false);
        d.setContentView(v);

        View preview = v.findViewById(R.id.preview);
        SaturationValueView viewSV = v.findViewById(R.id.viewSV);
        HueBarView viewHue = v.findViewById(R.id.viewHue);
        EditText etHex = v.findViewById(R.id.etHex);

        int initial = getArguments()!=null ? getArguments().getInt(ARG_COLOR, 0xFF684CC1) : 0xFF684CC1;
        float[] hsv = new float[3]; Color.colorToHSV(initial, hsv);
        hue = hsv[0]; sat = hsv[1]; val = hsv[2];

        preview.setBackgroundColor(initial);
        etHex.setText(String.format("#%06X", 0xFFFFFF & initial));

        viewHue.setHue(hue);
        viewHue.setOnHueChange(h -> {
            hue = h;
            viewSV.setHue(hue);
            int c = Color.HSVToColor(new float[]{hue, sat, val});
            preview.setBackgroundColor(c);
            etHex.setText(String.format("#%06X", 0xFFFFFF & c));
        });

        viewSV.setHue(hue);
        viewSV.setSV(sat, val);
        viewSV.setOnSVChange((s, v1) -> {
            sat = s; val = v1;
            int c = Color.HSVToColor(new float[]{hue, sat, val});
            preview.setBackgroundColor(c);
            etHex.setText(String.format("#%06X", 0xFFFFFF & c));
        });

        etHex.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s,int a,int b,int c) {}
            @Override public void onTextChanged(CharSequence s,int a,int b,int c) {}
            @Override public void afterTextChanged(Editable s) {
                String t = s.toString().trim();
                if (!t.startsWith("#")) t = "#"+t;
                if (t.length()==7){
                    try {
                        int c = Color.parseColor(t);
                        Color.colorToHSV(c, hsv);
                        hue = hsv[0]; sat = hsv[1]; val = hsv[2];
                        viewHue.setHue(hue);
                        viewSV.setHue(hue);
                        viewSV.setSV(sat,val);
                        preview.setBackgroundColor(c);
                    } catch (Exception ignore){}
                }
            }
        });

        v.findViewById(R.id.btnCancel).setOnClickListener(x -> dismiss());
        v.findViewById(R.id.btnOk).setOnClickListener(x -> {
            int c = Color.HSVToColor(new float[]{hue, sat, val});
            if (cb != null) cb.onColor(c);
            dismiss();
        });
        return d;
    }
}
