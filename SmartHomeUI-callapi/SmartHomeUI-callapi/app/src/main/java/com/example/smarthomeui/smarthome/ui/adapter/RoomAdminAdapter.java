package com.example.smarthomeui.smarthome.ui.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.smarthomeui.R;
import com.example.smarthomeui.smarthome.model.Room;
import com.example.smarthomeui.smarthome.ui.activity.RoomManagementActivity;
import java.util.List;

/**
 * RoomAdminAdapter
 * Adapter cho RecyclerView hiển thị danh sách phòng trong chế độ admin
 */
public class RoomAdminAdapter extends RecyclerView.Adapter<RoomAdminAdapter.RoomViewHolder> {

    private List<Room> roomList;
    private Context context;
    private RoomManagementActivity activity;

    public RoomAdminAdapter(List<Room> roomList, RoomManagementActivity activity) {
        this.roomList = roomList;
        this.activity = activity;
        this.context = activity;
    }

    @NonNull
    @Override
    public RoomViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_room_admin, parent, false);
        return new RoomViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RoomViewHolder holder, int position) {
        Room room = roomList.get(position);

        holder.tvRoomName.setText(room.getName());
        holder.tvRoomLocation.setText(room.getLocation());
        holder.tvDeviceCount.setText(String.valueOf(room.getDeviceCount()));
        holder.tvActiveDevices.setText(String.valueOf(room.getActiveDevices()));
        holder.tvTemperature.setText(room.getTemperatureText());
        holder.tvRoomStatus.setText(room.getStatusText());
        holder.tvLastUpdated.setText("Cập nhật: 2 phút trước");

        // Set status color
        int statusColor = room.isActive() ?
            context.getColor(R.color.green) :
            context.getColor(R.color.orange);
        holder.tvRoomStatus.getBackground().setTint(statusColor);

        // Set click listeners
        holder.btnEditRoom.setOnClickListener(v -> activity.editRoom(room));
        holder.btnDeleteRoom.setOnClickListener(v -> activity.deleteRoom(room));

        // Set item click listener
        holder.itemView.setOnClickListener(v -> {
            // TODO: Open room detail activity
        });
    }

    @Override
    public int getItemCount() {
        return roomList.size();
    }

    static class RoomViewHolder extends RecyclerView.ViewHolder {
        ImageView imgRoomIcon, btnEditRoom, btnDeleteRoom;
        TextView tvRoomName, tvRoomLocation, tvDeviceCount, tvActiveDevices,
                 tvTemperature, tvRoomStatus, tvLastUpdated;

        RoomViewHolder(View itemView) {
            super(itemView);
            imgRoomIcon = itemView.findViewById(R.id.imgRoomIcon);
            tvRoomName = itemView.findViewById(R.id.tvRoomName);
            tvRoomLocation = itemView.findViewById(R.id.tvRoomLocation);
            tvDeviceCount = itemView.findViewById(R.id.tvDeviceCount);
            tvActiveDevices = itemView.findViewById(R.id.tvActiveDevices);
            tvTemperature = itemView.findViewById(R.id.tvTemperature);
            tvRoomStatus = itemView.findViewById(R.id.tvRoomStatus);
            tvLastUpdated = itemView.findViewById(R.id.tvLastUpdated);
            btnEditRoom = itemView.findViewById(R.id.btnEditRoom);
            btnDeleteRoom = itemView.findViewById(R.id.btnDeleteRoom);
        }
    }
}
