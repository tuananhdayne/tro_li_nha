package com.example.smarthomeui.smarthome.adapter;

import android.view.*;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.smarthomeui.R;
import com.example.smarthomeui.smarthome.model.Device;
import java.util.List;

public class DeviceInventoryAdapter extends RecyclerView.Adapter<DeviceInventoryAdapter.VH> {

    public interface OnItemAction {
        void onControl(Device d, int pos);   // mở bottom sheet điều khiển (tuỳ chọn)
        void onEdit(Device d, int pos);      // sửa thông tin thiết bị
        void onDelete(Device d, int pos);    // xóa khỏi kho
    }

    private final List<Device> data;
    private final OnItemAction listener;

    public DeviceInventoryAdapter(List<Device> data, OnItemAction l) {
        this.data = data; this.listener = l; setHasStableIds(true);
    }
    @Override public long getItemId(int position) { return data.get(position).getId().hashCode(); }

    @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup p, int v) {
        View view = LayoutInflater.from(p.getContext()).inflate(R.layout.inventory_device_row, p, false);
        return new VH(view);
    }

    @Override public void onBindViewHolder(@NonNull VH h, int pos) {
        Device d = data.get(pos);
        h.tvName.setText(d.getName());
        h.tvType.setText(d.getType());

        // Icon theo type/cap
        int icon = R.drawable.ic_device_other;
        String type = d.getType() == null ? "" : d.getType().toLowerCase();
        if (type.startsWith("light")) icon = R.drawable.ic_device_light;
        else if (type.startsWith("fan")) icon = R.drawable.ic_device_fan;
        else if (type.startsWith("ac")) icon = R.drawable.ic_device_ac;
        h.ivIcon.setImageResource(icon);
        if (d.has(Device.CAP_COLOR)) {
            h.ivIcon.setColorFilter(d.getColor(), android.graphics.PorterDuff.Mode.SRC_IN);
        } else {
            h.ivIcon.clearColorFilter();
        }

        // Huy hiệu capabilities
        String caps = "";
        if (d.has(Device.CAP_POWER)) caps += "Pwr";
        if (d.has(Device.CAP_BRIGHTNESS)) caps += (caps.isEmpty()?"":"/") + "Dim";
        if (d.has(Device.CAP_COLOR)) caps += (caps.isEmpty()?"":"/") + "RGB";
        if (d.has(Device.CAP_SPEED)) caps += (caps.isEmpty()?"":"/") + "Spd";
        if (d.has(Device.CAP_TEMPERATURE)) caps += (caps.isEmpty()?"":"/") + "Temp";
        h.tvCaps.setText(caps.isEmpty() ? "-" : caps);

        // Bấm item -> control (tuỳ chọn)
        h.itemView.setOnClickListener(v -> { if (listener != null) listener.onControl(d, h.getBindingAdapterPosition()); });

        // Menu 3 chấm
        h.ivMenu.setOnClickListener(v -> {
            PopupMenu pm = new PopupMenu(v.getContext(), v);
            pm.getMenu().add(0, 1, 0, "Sửa thiết bị");
            pm.getMenu().add(0, 2, 1, "Xóa thiết bị");
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

    static class VH extends RecyclerView.ViewHolder {
        ImageView ivIcon, ivMenu; TextView tvName, tvType, tvCaps;
        VH(@NonNull View itemView) {
            super(itemView);
            ivIcon = itemView.findViewById(R.id.ivIcon);
            ivMenu = itemView.findViewById(R.id.ivMenu);
            tvName = itemView.findViewById(R.id.tvName);
            tvType = itemView.findViewById(R.id.tvType);
            tvCaps = itemView.findViewById(R.id.tvCaps);
        }
    }
}
