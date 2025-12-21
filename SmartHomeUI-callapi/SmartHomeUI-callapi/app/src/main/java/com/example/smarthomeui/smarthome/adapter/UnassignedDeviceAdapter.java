package com.example.smarthomeui.smarthome.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smarthomeui.R;
import com.example.smarthomeui.smarthome.network.DeviceDto;

import java.util.List;

public class UnassignedDeviceAdapter extends RecyclerView.Adapter<UnassignedDeviceAdapter.ViewHolder> {

    private final List<DeviceDto> devices;
    private final OnDeviceClickListener listener;

    public interface OnDeviceClickListener {
        void onDeviceClick(DeviceDto device);
    }

    public UnassignedDeviceAdapter(List<DeviceDto> devices, OnDeviceClickListener listener) {
        this.devices = devices;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.inventory_device_row, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DeviceDto device = devices.get(position);

        holder.tvName.setText(device.getName());
        holder.tvType.setText(device.getType() != null ? device.getType() : "Unknown");

        // Set icon based on device type
        int iconRes = getDeviceIcon(device.getType());
        holder.ivIcon.setImageResource(iconRes);

        // Hide capabilities badge and menu for this simple picker
        holder.tvCaps.setVisibility(View.GONE);
        holder.ivMenu.setVisibility(View.GONE);

        // Click to add device to room
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDeviceClick(device);
            }
        });
    }

    @Override
    public int getItemCount() {
        return devices != null ? devices.size() : 0;
    }

    private int getDeviceIcon(String type) {
        if (type == null) return R.drawable.ic_device_other;

        switch (type.toLowerCase()) {
            case "light":
                return R.drawable.ic_device_light;
            case "fan":
                return R.drawable.ic_device_fan;
            case "ac":
            case "air conditioner":
                return R.drawable.ic_device_ac;
            default:
                return R.drawable.ic_device_other;
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivIcon;
        TextView tvName;
        TextView tvType;
        TextView tvCaps;
        ImageView ivMenu;

        ViewHolder(View itemView) {
            super(itemView);
            ivIcon = itemView.findViewById(R.id.ivIcon);
            tvName = itemView.findViewById(R.id.tvName);
            tvType = itemView.findViewById(R.id.tvType);
            tvCaps = itemView.findViewById(R.id.tvCaps);
            ivMenu = itemView.findViewById(R.id.ivMenu);
        }
    }
}

