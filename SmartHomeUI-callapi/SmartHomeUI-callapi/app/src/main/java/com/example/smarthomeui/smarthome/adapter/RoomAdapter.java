package com.example.smarthomeui.smarthome.adapter;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smarthomeui.R;
import com.example.smarthomeui.smarthome.model.Room;
import com.google.android.material.materialswitch.MaterialSwitch;

import java.util.List;

public class RoomAdapter extends RecyclerView.Adapter<RoomAdapter.VH> {

    public interface OnRoomClick { void onClick(Room r); }
    public interface OnRoomLongClick { void onLongClick(Room r, int position); }
    public interface OnRoomToggle { void onToggle(Room r, boolean isActive); }

    private final List<Room> data;
    private final OnRoomClick listener;
    private final OnRoomLongClick longListener;
    private OnRoomToggle toggleListener;

    public RoomAdapter(List<Room> data, OnRoomClick l) {
        this(data, l, null);
    }
    public RoomAdapter(List<Room> data, OnRoomClick l, OnRoomLongClick ll) {
        this.data = data;
        this.listener = l;
        this.longListener = ll;
    }

    public void setOnRoomToggleListener(OnRoomToggle listener) {
        this.toggleListener = listener;
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.home_row, parent, false); // dùng chung item
        return new VH(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Room r = data.get(position);
        h.ivIcon.setImageResource(R.drawable.ic_room_green);
        h.tvName.setText(r.getName());
        h.tvCount.setText(r.getDeviceCount() + " thiết bị");

        // Hiển thị thông tin chi tiết (detail) của phòng nếu có
        if (h.tvSubtitle != null) {
            String detail = r.getDescription();
            if (!TextUtils.isEmpty(detail)) {
                h.tvSubtitle.setVisibility(View.VISIBLE);
                h.tvSubtitle.setText(detail);
            } else {
                h.tvSubtitle.setVisibility(View.GONE);
            }
        }

        // Set switch state từ model
        if (h.swMaster != null) {
            h.swMaster.setOnCheckedChangeListener(null); // Reset listener trước
            h.swMaster.setChecked(r.isActive());
            h.swMaster.setOnCheckedChangeListener((buttonView, isChecked) -> {
                r.setActive(isChecked);
                if (toggleListener != null) {
                    toggleListener.onToggle(r, isChecked);
                }
            });
        }

        h.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onClick(r);
        });
        h.itemView.setOnLongClickListener(v -> {
            if (longListener != null) {
                int pos = h.getBindingAdapterPosition();
                if (pos != RecyclerView.NO_POSITION) longListener.onLongClick(r, pos);
                return true;
            }
            return false;
        });

        // Xử lý sự kiện click cho nút menu (3 chấm)
        if (h.btnMenuOptions != null) {
            h.btnMenuOptions.setOnClickListener(v -> {
                if (longListener != null) {
                    int pos = h.getBindingAdapterPosition();
                    if (pos != RecyclerView.NO_POSITION) {
                        longListener.onLongClick(r, pos); // Sử dụng cùng callback với long click
                    }
                }
            });
        }

        // Load sensor data for this room
        h.loadSensorData(r.getId());
    }

    @Override
    public int getItemCount() {
        return data == null ? 0 : data.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        ImageView ivIcon;
        TextView tvName, tvCount, tvSubtitle;
        TextView tvTemperature, tvHumidity, tvMotion;
        View sensorsLayout;
        View btnMenuOptions;
        MaterialSwitch swMaster;

        VH(@NonNull View v) {
            super(v);
            ivIcon = v.findViewById(R.id.ivRoomIcon);
            tvName = v.findViewById(R.id.tvRoomName);
            tvCount = v.findViewById(R.id.tvDeviceCount);
            tvSubtitle = v.findViewById(R.id.tvSubtitle);
            tvTemperature = v.findViewById(R.id.tvTemperature);
            tvHumidity = v.findViewById(R.id.tvHumidity);
            tvMotion = v.findViewById(R.id.tvMotion);
            sensorsLayout = v.findViewById(R.id.sensorsLayout);
            btnMenuOptions = v.findViewById(R.id.btnMenuOptions);
            swMaster = v.findViewById(R.id.swMaster);
        }

        void loadSensorData(String roomIdStr) {
            int roomId;
            try {
                roomId = Integer.parseInt(roomIdStr);
            } catch (NumberFormatException e) {
                return; // Invalid room ID
            }
            // Call API to get sensor data
            com.example.smarthomeui.smarthome.network.ApiClient.getClientNoAuth()
                    .create(com.example.smarthomeui.smarthome.network.Api.class)
                    .getRoomSensors(roomId)
                    .enqueue(new retrofit2.Callback<com.example.smarthomeui.smarthome.network.RoomSensorsDto>() {
                        @Override
                        public void onResponse(retrofit2.Call<com.example.smarthomeui.smarthome.network.RoomSensorsDto> call, 
                                             retrofit2.Response<com.example.smarthomeui.smarthome.network.RoomSensorsDto> response) {
                            if (response.isSuccessful() && response.body() != null) {
                                updateSensorsUI(response.body());
                            }
                        }

                        @Override
                        public void onFailure(retrofit2.Call<com.example.smarthomeui.smarthome.network.RoomSensorsDto> call, Throwable t) {
                            // Hide sensors layout if API fails
                            if (sensorsLayout != null) {
                                sensorsLayout.setVisibility(View.GONE);
                            }
                        }
                    });
        }

        void updateSensorsUI(com.example.smarthomeui.smarthome.network.RoomSensorsDto sensors) {
            if (!sensors.hasSensors || sensorsLayout == null) {
                sensorsLayout.setVisibility(View.GONE);
                return;
            }

            sensorsLayout.setVisibility(View.VISIBLE);

            // Update temperature
            if (sensors.temperature != null && tvTemperature != null) {
                tvTemperature.setText(String.format("%.1f°C", sensors.temperature));
                tvTemperature.setTextColor(android.graphics.Color.parseColor("#4CAF50"));
            }

            // Update humidity
            if (sensors.humidity != null && tvHumidity != null) {
                tvHumidity.setText(String.format("%.0f%%", sensors.humidity));
                tvHumidity.setTextColor(android.graphics.Color.parseColor("#2196F3"));
            }

            // Update motion
            if (sensors.motion != null && tvMotion != null) {
                tvMotion.setText(sensors.motion ? "Có người" : "Trống");
                tvMotion.setTextColor(sensors.motion ? 
                    android.graphics.Color.parseColor("#FF9800") : 
                    android.graphics.Color.parseColor("#999999"));
            }
        }
    }
}
