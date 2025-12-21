package com.example.smarthomeui.smarthome.adapter;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smarthomeui.R;
import com.example.smarthomeui.smarthome.model.House;
import com.google.android.material.materialswitch.MaterialSwitch;

import java.util.List;

public class HouseAdapter extends RecyclerView.Adapter<HouseAdapter.VH> {

    public interface OnHouseClick { void onClick(House h); }
    public interface OnHouseLongClick { void onLongClick(House h, int position); }
    public interface OnHouseMenuClick { void onMenuClick(House h, int position); }
    public interface OnHouseToggle { void onToggle(House h, boolean isActive); }

    private final List<House> data;
    private final OnHouseClick listener;
    private final OnHouseLongClick longListener;
    private final OnHouseMenuClick menuListener;
    private OnHouseToggle toggleListener;

    public HouseAdapter(List<House> data, OnHouseClick l) {
        this(data, l, null);
    }

    public HouseAdapter(List<House> data, OnHouseClick l, OnHouseLongClick ll) {
        this(data, l, ll, null);
    }

    public HouseAdapter(List<House> data, OnHouseClick l, OnHouseLongClick ll, OnHouseMenuClick ml) {
        this.data = data;
        this.listener = l;
        this.longListener = ll;
        this.menuListener = ml;
    }

    public void setOnHouseToggleListener(OnHouseToggle listener) {
        this.toggleListener = listener;
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.home_row, parent, false);
        return new VH(view);
    }


    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        House x = data.get(position);

        h.ivIcon.setImageResource(R.drawable.ic_house_green);
        h.tvName.setText(x.getName());

        // Hiển thị LOCATION dưới tên nhà (map từ x.getDescription() hoặc x.getLocation())
        String location = x.getDescription(); // hoặc x.getLocation() nếu bạn có field riêng
        if (location != null && !location.trim().isEmpty()) {
            h.tvSubtitle.setVisibility(View.VISIBLE);
            h.tvSubtitle.setText(location.trim());
        } else {
            h.tvSubtitle.setVisibility(View.GONE);
        }

        // GIỮ NGUYÊN phần hiển thị số phòng
        h.tvCount.setText(x.getRoomCount() + " phòng");

        // Set switch state từ model
        if (h.swMaster != null) {
            h.swMaster.setOnCheckedChangeListener(null); // Reset listener trước khi set state
            h.swMaster.setChecked(x.isActive());
            h.swMaster.setOnCheckedChangeListener((buttonView, isChecked) -> {
                x.setActive(isChecked);
                if (toggleListener != null) {
                    toggleListener.onToggle(x, isChecked);
                }
            });
        }

        h.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onClick(x);
        });

        h.itemView.setOnLongClickListener(v -> {
            if (longListener != null) {
                int pos = h.getBindingAdapterPosition();
                if (pos != RecyclerView.NO_POSITION) {
                    longListener.onLongClick(x, pos);
                }
                return true;
            }
            return false;
        });

        // Xử lý click vào nút menu
        if (h.btnMenu != null) {
            h.btnMenu.setOnClickListener(v -> {
                if (menuListener != null) {
                    int pos = h.getBindingAdapterPosition();
                    if (pos != RecyclerView.NO_POSITION) {
                        menuListener.onMenuClick(x, pos);
                    }
                } else if (longListener != null) {
                    // Nếu không có menuListener, sử dụng longListener thay thế
                    int pos = h.getBindingAdapterPosition();
                    if (pos != RecyclerView.NO_POSITION) {
                        longListener.onLongClick(x, pos);
                    }
                }
            });
        }
    }


    @Override
    public int getItemCount() {
        return data == null ? 0 : data.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        ImageView ivIcon;
        ImageButton btnMenu;
        TextView tvSubtitle;
        TextView tvName, tvCount;
        MaterialSwitch swMaster;

        VH(@NonNull View v) {
            super(v);
            ivIcon = v.findViewById(R.id.ivRoomIcon);
            btnMenu = v.findViewById(R.id.btnMenuOptions);
            tvSubtitle = itemView.findViewById(R.id.tvSubtitle);
            tvName = v.findViewById(R.id.tvRoomName);
            tvCount = v.findViewById(R.id.tvDeviceCount);
            swMaster = v.findViewById(R.id.swMaster);
        }
    }
}

