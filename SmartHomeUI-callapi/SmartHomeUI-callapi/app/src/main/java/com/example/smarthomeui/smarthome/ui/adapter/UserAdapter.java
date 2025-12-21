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
import com.example.smarthomeui.smarthome.model.User;
import com.example.smarthomeui.smarthome.ui.activity.UserManagementActivity;
import de.hdodenhof.circleimageview.CircleImageView;
import java.util.List;

/**
 * UserAdapter
 * Adapter cho RecyclerView hiển thị danh sách người dùng
 */
public class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {

    private List<User> userList;
    private Context context;
    private UserManagementActivity activity;

    public UserAdapter(List<User> userList, UserManagementActivity activity) {
        this.userList = userList;
        this.activity = activity;
        this.context = activity;
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_user, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        User user = userList.get(position);

        holder.tvUserName.setText(user.getName());
        holder.tvUserEmail.setText(user.getEmail());
        holder.tvUserRole.setText(user.getRoleDisplayName());

        // Set role color
        int roleColor = getRoleColor(user.getRole());
        holder.tvUserRole.getBackground().setTint(roleColor);

        // TODO: Load avatar with image loading library like Glide
        // Glide.with(context).load(user.getAvatar()).into(holder.imgUserAvatar);

        // Set click listeners
        holder.btnEditUser.setOnClickListener(v -> activity.editUser(user));
        holder.btnDeleteUser.setOnClickListener(v -> activity.deleteUser(user));

        // Set item click listener
        holder.itemView.setOnClickListener(v -> {
            // TODO: Open user detail or edit activity
        });
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    private int getRoleColor(String role) {
        switch (role.toLowerCase()) {
            case "admin":
                return context.getColor(R.color.red);
            case "moderator":
                return context.getColor(R.color.orange);
            default:
                return context.getColor(R.color.primaryColor);
        }
    }

    static class UserViewHolder extends RecyclerView.ViewHolder {
        CircleImageView imgUserAvatar;
        TextView tvUserName, tvUserEmail, tvUserRole;
        ImageView btnEditUser, btnDeleteUser;

        UserViewHolder(View itemView) {
            super(itemView);
            imgUserAvatar = itemView.findViewById(R.id.imgUserAvatar);
            tvUserName = itemView.findViewById(R.id.tvUserName);
            tvUserEmail = itemView.findViewById(R.id.tvUserEmail);
            tvUserRole = itemView.findViewById(R.id.tvUserRole);
            btnEditUser = itemView.findViewById(R.id.btnEditUser);
            btnDeleteUser = itemView.findViewById(R.id.btnDeleteUser);
        }
    }
}
