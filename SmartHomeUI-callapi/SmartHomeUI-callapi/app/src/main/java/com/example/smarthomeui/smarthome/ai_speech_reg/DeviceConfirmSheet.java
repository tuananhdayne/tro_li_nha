package com.example.smarthomeui.smarthome.ai_speech_reg;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.smarthomeui.R;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import java.util.List;
import com.example.smarthomeui.smarthome.model.Device;
import static com.example.smarthomeui.smarthome.ai_speech_reg.DeviceModels.*;

public class DeviceConfirmSheet {

    public interface Callback {
        void onConfirmed(Device device, ParseResult planned);
        void onCanceled();
    }

    private final BottomSheetDialog dialog;
    private final ImageView imgDevice;
    private final TextView tvDeviceName, tvRoomType, tvPlannedAction;
    private final View viewColorPreview;
    private final Button btnCancel, btnNext, btnConfirm;

    private final List<DeviceRegistry.CandidateResult> candidates;
    private int index = 0;
    private final ParseResult planned;

    public DeviceConfirmSheet(Context ctx, List<DeviceRegistry.CandidateResult> candidates, ParseResult planned, Callback cb) {
        dialog = new BottomSheetDialog(ctx);
        View v = LayoutInflater.from(ctx).inflate(R.layout.sheet_device_confirm, null, false);
        dialog.setContentView(v);

        imgDevice = v.findViewById(R.id.imgDevice);
        tvDeviceName = v.findViewById(R.id.tvDeviceName);
        tvRoomType = v.findViewById(R.id.tvRoomType);
        tvPlannedAction = v.findViewById(R.id.tvPlannedAction);
        viewColorPreview = v.findViewById(R.id.viewColorPreview);
        btnCancel = v.findViewById(R.id.btnCancel);
        btnNext = v.findViewById(R.id.btnNext);
        btnConfirm = v.findViewById(R.id.btnConfirm);

        this.candidates = candidates;
        this.planned = planned;

        btnCancel.setOnClickListener(view -> { dialog.dismiss(); cb.onCanceled(); });
        btnNext.setOnClickListener(view -> { nextCandidate(); });
        btnConfirm.setOnClickListener(view -> {
            Device d = candidates.get(index).device;
            dialog.dismiss();
            cb.onConfirmed(d, planned);
        });

        bindCandidate();
    }

    private void bindCandidate() {
        Device d = candidates.get(index).device;
        tvDeviceName.setText(d.getName() + " • " + d.getType());
        tvRoomType.setText("Phòng: " + d.getRoom());

        // --- PHÂN BIỆT RGB VÀ THIẾT BỊ KHÁC ---
        if (planned.deviceType != null && planned.deviceType.toLowerCase().contains("rgb")) {
            // Chỉ cho phép ĐẶT / ĐỔI MÀU
            tvPlannedAction.setText("Hành động: ĐẶT MÀU " + getColorName(planned.colorRgb));

            int[] rgb = planned.colorRgb != null ? planned.colorRgb : new int[]{255,255,255};
            int color = Color.rgb(rgb[0], rgb[1], rgb[2]);
            viewColorPreview.setBackgroundColor(color);

            imgDevice.setImageResource(R.drawable.lightbulb_24px); // icon riêng nếu có
        } else {
            // --- ĐÈN / QUẠT ---
            viewColorPreview.setVisibility(View.GONE);

            String actionStr;
            switch (planned.action) {
                case TURN_ON:
                    actionStr = "Hành động: BẬT";
                    break;
                case TURN_OFF:
                    actionStr = "Hành động: TẮT";
                    break;
                case INCREASE:
                    actionStr = "Hành động: TĂNG" +
                            (planned.value != null && planned.value >= 0 ? (" lên " + planned.value) : "");
                    break;
                case DECREASE:
                    actionStr = "Hành động: GIẢM" +
                            (planned.value != null && planned.value >= 0 ? (" xuống " + planned.value) : "");
                    break;
                case SET:
                    actionStr = "Hành động: ĐẶT" +
                            (planned.value != null && planned.value >= 0 ? (" = " + planned.value) : "");
                    break;
                default:
                    actionStr = "Hành động: (không rõ)";
            }
            tvPlannedAction.setText(actionStr);

            if (d.isFan()) imgDevice.setImageResource(R.drawable.toys_fan_24px);
            else imgDevice.setImageResource(R.drawable.lightbulb_24px);
        }

        if (candidates.size() <= 1) {
            btnNext.setVisibility(View.GONE);
        } else {
            btnNext.setVisibility(View.VISIBLE);
            btnNext.setText("Không phải, xem tiếp (" + (index+1) + "/" + candidates.size() + ")");
        }
    }

    private String getColorName(int[] rgb) {
        if (rgb == null) return "TRẮNG";
        int r = rgb[0], g = rgb[1], b = rgb[2];
        if (r == 255 && g == 0 && b == 0) return "ĐỎ";
        if (r == 0 && g == 255 && b == 0) return "XANH LÁ";
        if (r == 0 && g == 0 && b == 255) return "XANH DƯƠNG";
        if (r == 255 && g == 255 && b == 0) return "VÀNG";
        if (r == 255 && g == 165 && b == 0) return "CAM";
        if (r == 255 && g == 105 && b == 180) return "HỒNG";
        if (r == 128 && g == 0 && b == 255) return "TÍM";
        if (r == 0 && g == 255 && b == 255) return "XANH NGỌC";
        if (r == 255 && g == 255 && b == 255) return "TRẮNG";
        if (r == 0 && g == 0 && b == 0) return "ĐEN";
        return "KHÔNG RÕ";
    }

    private void nextCandidate() {
        index = (index + 1) % candidates.size();
        bindCandidate();
    }

    public void show() { dialog.show(); }
}