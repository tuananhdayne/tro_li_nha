package com.example.smarthomeui.smarthome.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smarthomeui.R;
import com.example.smarthomeui.smarthome.model.Device;
import com.google.android.material.materialswitch.MaterialSwitch;

import java.util.List;

public class SingleRoomAdapter extends RecyclerView.Adapter<SingleRoomAdapter.VH> {

    public interface OnDeviceClick {
        void onClick(Device d, int position);
        void onEdit(Device d, int position);
        void onDelete(Device d, int position);
    }

    private final List<Device> data;
    private final OnDeviceClick listener;

    public SingleRoomAdapter(List<Device> data) { this(data, null); }
    public SingleRoomAdapter(List<Device> data, OnDeviceClick l) {
        this.data = data;
        this.listener = l;
        setHasStableIds(true);
    }

    @Override public long getItemId(int position) {
        return data.get(position).getId().hashCode();
    }

    @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup p, int v) {
        View view = LayoutInflater.from(p.getContext()).inflate(R.layout.single_room_row, p, false);
        return new VH(view);
    }

    @Override public void onBindViewHolder(@NonNull VH h, int pos) {
        Device d = data.get(pos);

        // Tên & loại
        h.tvDeviceName.setText(d.getName());
        h.tvDeviceType.setText(d.getType() + " • " + d.getStatus());

        // Icon theo loại
        int icon = R.drawable.ic_device_other;
        String t = d.getType() == null ? "" : d.getType().toLowerCase();
        if (t.contains("light") || t.contains("đèn") || t.contains("den")) {
            icon = R.drawable.ic_device_light;
        } else if (t.contains("fan") || t.contains("quạt") || t.contains("quat")) {
            icon = R.drawable.ic_device_fan;
        }
        h.ivDeviceIcon.setImageResource(icon);

        // Màu icon nếu là đèn
        if (d.isLight()) {
            h.ivDeviceIcon.setColorFilter(d.getColor(), android.graphics.PorterDuff.Mode.SRC_IN);
        } else {
            h.ivDeviceIcon.clearColorFilter();
        }

        // Switch: tránh callback lặp
        h.swDeviceOnOff.setOnCheckedChangeListener(null);
        h.swDeviceOnOff.setChecked(d.isOn());
        h.swDeviceOnOff.setOnCheckedChangeListener((btn, checked) -> {
            d.setOn(checked);
            // cập nhật badge khi bật/tắt
            bindBadge(h, d);
        });

        // Badge: speed/brightness
        bindBadge(h, d);

        // Click item -> mở điều khiển
        h.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onClick(d, h.getBindingAdapterPosition());
        });

        h.ivMenu.setOnClickListener(v -> {
            PopupMenu pm = new PopupMenu(v.getContext(), v);
            pm.getMenu().add(0, 1, 0, "Sửa thiết bị");
            pm.getMenu().add(0, 2, 0, "Xóa thiết bị");
            pm.setOnMenuItemClickListener(item -> {
                if (listener == null) return true;
                int id = item.getItemId();
                if (id == 1) listener.onEdit(d, h.getBindingAdapterPosition());
                else if (id == 2) listener.onDelete(d, h.getBindingAdapterPosition());
                return true;
            });
            pm.show();
        });
    }

    @Override public int getItemCount() { return data == null ? 0 : data.size(); }

    /** Cập nhật nội dung/visibility của badge theo loại thiết bị */
    private void bindBadge(@NonNull VH h, @NonNull Device d) {
        if (d.isFan()) {
            h.tvBadge.setVisibility(View.VISIBLE);
            h.tvBadge.setText("S" + d.getSpeed()); // S0..S3
        } else if (d.isLight()) {
            h.tvBadge.setVisibility(View.VISIBLE);
            String txt = d.isOn() ? (d.getBrightness() + "%") : "Off";
            h.tvBadge.setText(txt);
        } else {
            h.tvBadge.setVisibility(View.GONE);
        }
    }

    static class VH extends RecyclerView.ViewHolder {
        ImageView ivDeviceIcon, ivMenu;
        TextView tvDeviceName, tvDeviceType, tvBadge;
        MaterialSwitch swDeviceOnOff;

        VH(@NonNull View itemView) {
            super(itemView);
            ivDeviceIcon   = itemView.findViewById(R.id.ivDeviceIcon);
            tvDeviceName   = itemView.findViewById(R.id.tvDeviceName);
            tvDeviceType   = itemView.findViewById(R.id.tvDeviceType);
            swDeviceOnOff  = itemView.findViewById(R.id.swDeviceOnOff);
            tvBadge        = itemView.findViewById(R.id.tvBadge);
            ivMenu         = itemView.findViewById(R.id.ivMenu);
        }
    }
}
