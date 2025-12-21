package com.example.smarthomeui.smarthome.ui.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.smarthomeui.R;
import com.example.smarthomeui.smarthome.model.Device;
import com.example.smarthomeui.smarthome.ui.activity.DeviceManagementActivity;
import java.util.List;

/**
 * DeviceAdminAdapter
 * Adapter cho RecyclerView hiển thị danh sách thiết bị trong chế độ admin
 */
public class DeviceAdminAdapter extends RecyclerView.Adapter<DeviceAdminAdapter.DeviceViewHolder> {

    private List<Device> deviceList;
    private Context context;
    private DeviceManagementActivity activity;

    public DeviceAdminAdapter(List<Device> deviceList, DeviceManagementActivity activity) {
        this.deviceList = deviceList;
        this.activity = activity;
        this.context = activity;
    }

    @NonNull
    @Override
    public DeviceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_device_admin, parent, false);
        return new DeviceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DeviceViewHolder holder, int position) {
        Device device = deviceList.get(position);

        holder.tvDeviceName.setText(device.getName());
        holder.tvDeviceRoom.setText(device.getRoom());
        holder.tvDeviceId.setText("ID: " + device.getId());
        holder.tvDeviceType.setText(device.getType());
        holder.tvPowerConsumption.setText(device.getPowerConsumption());
        holder.tvLastActivity.setText(device.getLastActivity());
        holder.tvDeviceStatus.setText(device.getStatus());
        holder.tvDeviceValue.setText(device.getValueText());

        // Set status color
        int statusColor = device.isOnline() ?
            context.getColor(R.color.green) :
            context.getColor(R.color.red);
        holder.tvDeviceStatus.getBackground().setTint(statusColor);

        // Set device switch
        holder.switchDevicePower.setChecked(device.isOnline());
        holder.switchDevicePower.setOnCheckedChangeListener((buttonView, isChecked) -> {
            activity.toggleDevice(device, isChecked);
        });

        // Set click listeners
        holder.btnEditDevice.setOnClickListener(v -> activity.editDevice(device));
        holder.btnDeleteDevice.setOnClickListener(v -> activity.deleteDevice(device));

        // Set item click listener
        holder.itemView.setOnClickListener(v -> {
            // TODO: Open device detail activity
        });
    }

    @Override
    public int getItemCount() {
        return deviceList.size();
    }

    static class DeviceViewHolder extends RecyclerView.ViewHolder {
        ImageView imgDeviceIcon, btnEditDevice, btnDeleteDevice;
        TextView tvDeviceName, tvDeviceRoom, tvDeviceId, tvDeviceType,
                 tvPowerConsumption, tvLastActivity, tvDeviceStatus, tvDeviceValue;
        Switch switchDevicePower;

        DeviceViewHolder(View itemView) {
            super(itemView);
            imgDeviceIcon = itemView.findViewById(R.id.imgDeviceIcon);
            tvDeviceName = itemView.findViewById(R.id.tvDeviceName);
            tvDeviceRoom = itemView.findViewById(R.id.tvDeviceRoom);
            tvDeviceId = itemView.findViewById(R.id.tvDeviceId);
            tvDeviceType = itemView.findViewById(R.id.tvDeviceType);
            tvPowerConsumption = itemView.findViewById(R.id.tvPowerConsumption);
            tvLastActivity = itemView.findViewById(R.id.tvLastActivity);
            tvDeviceStatus = itemView.findViewById(R.id.tvDeviceStatus);
            tvDeviceValue = itemView.findViewById(R.id.tvDeviceValue);
            switchDevicePower = itemView.findViewById(R.id.switchDevicePower);
            btnEditDevice = itemView.findViewById(R.id.btnEditDevice);
            btnDeleteDevice = itemView.findViewById(R.id.btnDeleteDevice);
        }
    }
}
