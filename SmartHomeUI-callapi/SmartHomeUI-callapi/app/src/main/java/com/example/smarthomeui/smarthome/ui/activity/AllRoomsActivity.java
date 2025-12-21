package com.example.smarthomeui.smarthome.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smarthomeui.R;
import com.example.smarthomeui.smarthome.ai_speech_reg.MainActivitySR;
import com.example.smarthomeui.smarthome.model.Room;
import com.example.smarthomeui.smarthome.network.*;

import java.util.*;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AllRoomsActivity extends AppCompatActivity {

    interface Row {}

    static class HeaderRow implements Row {
        final String houseId;
        final String houseName;
        final String houseLocation;
        HeaderRow(String id, String name, @Nullable String location) {
            houseId = id; houseName = name; houseLocation = location;
        }
    }
    static class RoomRow implements Row { final String houseId; final Room room; RoomRow(String hid, Room r){ houseId=hid; room=r; } }

    private final List<Row> rows = new ArrayList<>();
    private final Map<Integer, String> houseLocMap = new HashMap<>(); // houseId -> location

    private RoomsSectionAdapter adapter;
    private View loading;

    @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_house_rooms);
        setTitle("Danh sách phòng");

        View back = findViewById(R.id.ivBack);
        if (back != null) back.setOnClickListener(v -> onBackPressed());
        TextView title = findViewById(R.id.tvHouseTitle);
        if (title != null) title.setText("Danh sách phòng");

        RecyclerView rv = findViewById(R.id.rvRoomsOfHouse);
        GridLayoutManager glm = new GridLayoutManager(this, 2);
        rv.setLayoutManager(glm);
        adapter = new RoomsSectionAdapter(rows);
        glm.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override public int getSpanSize(int pos) {
                return adapter.getItemViewType(pos) == RoomsSectionAdapter.VT_HEADER ? 2 : 1;
            }
        });
        rv.setAdapter(adapter);

        View ivHome = findViewById(R.id.ivHome);
        if (ivHome != null) ivHome.setOnClickListener(v ->
                startActivity(new Intent(this, HouseListActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)));
        findViewById(R.id.ivRooms).setSelected(true);
        View ivDevices = findViewById(R.id.ivDevices);
        if (ivDevices != null) ivDevices.setOnClickListener(v ->
                startActivity(new Intent(this, DeviceInventoryActivity.class)));

       ImageView ivPlus = findViewById(R.id.ivPlus);
        if (ivPlus != null) ivPlus.setOnClickListener(v -> {
            com.example.smarthomeui.smarthome.ui.bottomsheet.VoiceAssistantBottomSheet sheet =
                new com.example.smarthomeui.smarthome.ui.bottomsheet.VoiceAssistantBottomSheet();
            sheet.show(getSupportFragmentManager(), "VoiceAssistant");
        });

        View ivSetting = findViewById(R.id.ivSetting);
        if (ivSetting != null) ivSetting.setOnClickListener(v ->
                startActivity(new Intent(this, SettingsActivity.class)));

        View fab = findViewById(R.id.fabAddRoom);
        if (fab != null) fab.setOnClickListener(v -> showCreateRoomDialog());

        loading = attachSimpleLoading();

        // 1) Lấy danh sách nhà (để có location), 2) rồi lấy rooms grouped
        fetchHouseLocationsThenRooms(0, 200, 0, 50);
    }

    /** Bước 1: GET /api/house -> build map houseId -> location, rồi gọi bước 2 */
    private void fetchHouseLocationsThenRooms(int houseSkip, int houseTake, int roomSkip, int roomTake) {
        showLoading(true);
        Api api = ApiClient.getClient(this).create(Api.class);
        api.getHouses(houseSkip, houseTake).enqueue(new Callback<HouseListWrap>() {
            @Override public void onResponse(Call<HouseListWrap> call, Response<HouseListWrap> resp) {
                if (resp.isSuccessful() && resp.body() != null && resp.body().houses != null) {
                    houseLocMap.clear();
                    for (HouseDto h : resp.body().houses) {
                        houseLocMap.put(h.id, h.location); // có thể null
                    }
                }
                // Dù thành công hay không, vẫn tiếp tục gọi rooms (địa chỉ sẽ là "Địa chỉ" khi null)
                fetchRoomsGrouped(roomSkip, roomTake);
            }
            @Override public void onFailure(Call<HouseListWrap> call, Throwable t) {
                // Không có map -> location sẽ để mặc định
                fetchRoomsGrouped(roomSkip, roomTake);
            }
        });
    }

    /** Bước 2: GET /api/room -> hiển thị theo nhóm nhà, chèn location từ map */
    private void fetchRoomsGrouped(int skip, int take) {
        Api api = ApiClient.getClient(this).create(Api.class);
        api.getRoomsGrouped(skip, take).enqueue(new Callback<RoomsByHouseWrap>() {
            @Override public void onResponse(Call<RoomsByHouseWrap> call, Response<RoomsByHouseWrap> resp) {
                showLoading(false);

                // In toàn bộ JSON trả về để debug
                try {
                    if (resp.body() != null) {
                        android.util.Log.d("AllRoomsActivity", "Raw JSON: " + new com.google.gson.Gson().toJson(resp.raw().body()));
                    }
                } catch (Exception e) {
                    android.util.Log.e("AllRoomsActivity", "Không thể in JSON: " + e.getMessage());
                }

                // Thêm log để kiểm tra dữ liệu trả về
                if (resp.body() != null) {
                    android.util.Log.d("AllRoomsActivity", "Số nhóm nhà: " + (resp.body().groups != null ? resp.body().groups.size() : 0));
                    if (resp.body().groups != null) {
                        for (RoomsByHouseWrap.Group g : resp.body().groups) {
                            android.util.Log.d("AllRoomsActivity", "Nhà: " + g.houseName + ", Địa chỉ: " + g.houseLocation + ", Số phòng: " + (g.rooms != null ? g.rooms.size() : 0));
                        }
                    }
                }
                if (!resp.isSuccessful() || resp.body() == null) {
                    toast("Tải danh sách phòng thất bại: " + resp.code());
                    return;
                }
                rows.clear();
                RoomsByHouseWrap data = resp.body();
                if (data.groups != null) {
                    for (RoomsByHouseWrap.Group g : data.groups) {
                        int hidInt = g.houseId;
                        String hid  = String.valueOf(hidInt);
                        String name = safe(g.houseName, "Nhà");
                        // Ưu tiên location từ API group; nếu null thì lấy từ map houses
                        String loc  = safe(nonNull(g.houseLocation, houseLocMap.get(hidInt)), "Địa chỉ");
                        rows.add(new HeaderRow(hid, name, loc));

                        if (g.rooms != null) {
                            for (RoomDto r : g.rooms) {
                                rows.add(new RoomRow(hid, mapToUiRoom(r)));
                            }
                        }
                    }
                }
                adapter.notifyDataSetChanged();
            }

            @Override public void onFailure(Call<RoomsByHouseWrap> call, Throwable t) {
                showLoading(false);
                toast("Lỗi mạng: " + (t != null ? t.getMessage() : ""));
            }
        });
    }

    private Room mapToUiRoom(RoomDto r) {
        int icon = mapIcon(r.iconKey, r.type);
        Room room = new Room(String.valueOf(r.id), safe(r.name, "Phòng"), icon);
        room.setDescription(safe(r.detail, "")); // Thêm null-safety cho detail
        room.setDeviceCount(r.deviceCount != null ? r.deviceCount : 0);
        return room;
    }

    private int mapIcon(String iconKey, String type) {
        String k = (iconKey != null ? iconKey : (type != null ? type : "")).toLowerCase();
        if (k.contains("living"))  return R.drawable.ic_room_living;
        if (k.contains("bed"))     return R.drawable.ic_room_bed;
        if (k.contains("kitchen")) return R.drawable.ic_room_kitchen;
        return R.drawable.ic_room_generic;
    }

    private String nonNull(String a, String b){ return a != null ? a : b; }
    private String safe(String s, String def){ return (s == null || s.trim().isEmpty()) ? def : s.trim(); }

    private View attachSimpleLoading() {
        ViewGroup root = findViewById(android.R.id.content);
        FrameLayout overlay = new FrameLayout(this);
        overlay.setClickable(true);
        overlay.setVisibility(View.GONE);
        ProgressBar bar = new ProgressBar(this, null, android.R.attr.progressBarStyleLarge);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.gravity = Gravity.CENTER;
        overlay.addView(bar, lp);
        root.addView(overlay, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        return overlay;
    }
    private void showLoading(boolean show){ if (loading != null) loading.setVisibility(show ? View.VISIBLE : View.GONE); }
    private void toast(String s){ Toast.makeText(this, s, Toast.LENGTH_SHORT).show(); }

    private void showCreateRoomDialog() {
        // Cần lấy danh sách nhà để người dùng chọn
        Api api = ApiClient.getClient(this).create(Api.class);
        showLoading(true);

        api.getHouses(0, 100).enqueue(new Callback<HouseListWrap>() {
            @Override
            public void onResponse(Call<HouseListWrap> call, Response<HouseListWrap> response) {
                showLoading(false);

                if (!response.isSuccessful() || response.body() == null || response.body().houses == null || response.body().houses.isEmpty()) {
                    toast("Không thể tải danh sách nhà");
                    return;
                }

                // Hiển thị dialog với danh sách nhà
                showCreateRoomDialogWithHouses(response.body().houses);
            }

            @Override
            public void onFailure(Call<HouseListWrap> call, Throwable t) {
                showLoading(false);
                toast("Lỗi kết nối: " + t.getMessage());
            }
        });
    }

    private void showCreateRoomDialogWithHouses(List<HouseDto> houses) {
        // Inflate custom layout for the dialog
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_room_add_all, null);

        // Get references to views
        Spinner spHouse = dialogView.findViewById(R.id.spHouse);
        EditText etName = dialogView.findViewById(R.id.etName);
        EditText etDescription = dialogView.findViewById(R.id.etDescription);

        // Set up house spinner adapter
        String[] houseNames = new String[houses.size()];
        final int[] houseIds = new int[houses.size()];

        for (int i = 0; i < houses.size(); i++) {
            houseNames[i] = houses.get(i).name;
            houseIds[i] = houses.get(i).id;
        }

        ArrayAdapter<String> houseAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, houseNames);
        houseAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spHouse.setAdapter(houseAdapter);

        // Create and show the dialog
        new AlertDialog.Builder(this)
                .setTitle("Tạo phòng mới")
                .setView(dialogView)
                .setPositiveButton("Tạo", (dialog, which) -> {
                    String roomName = etName.getText().toString().trim();
                    String roomDetail = etDescription.getText().toString().trim();
                    int selectedHouseId = houseIds[spHouse.getSelectedItemPosition()];

                    if (roomName.isEmpty()) {
                        toast("Vui lòng nhập tên phòng");
                        return;
                    }

                    // Use "generic" as default room type since we removed the room type spinner
                    String roomType = "generic";
                    String iconKey = "generic";

                    // Gọi API tạo phòng
                    createRoom(roomName, roomDetail, roomType, iconKey, selectedHouseId);
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void createRoom(String name, String detail, String type, String iconKey, int houseId) {
        showLoading(true);

        Api api = ApiClient.getClient(this).create(Api.class);
        CreateRoomReq request = new CreateRoomReq(name, detail, type, iconKey, houseId);

        api.createRoom(request).enqueue(new Callback<RoomDto>() {
            @Override
            public void onResponse(Call<RoomDto> call, Response<RoomDto> response) {
                showLoading(false);

                if (response.isSuccessful() && response.body() != null) {
                    toast("Phòng đã được tạo thành công");
                    // Cập nhật lại danh sách phòng
                    fetchHouseLocationsThenRooms(0, 200, 0, 50);
                } else {
                    int code = response.code();
                    String message = "";
                    try {
                        if (response.errorBody() != null) {
                            message = response.errorBody().string();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    toast("Lỗi tạo phòng: " + code + " " + message);
                }
            }

            @Override
            public void onFailure(Call<RoomDto> call, Throwable t) {
                showLoading(false);
                toast("Lỗi kết nối: " + t.getMessage());
            }
        });
    }
    /* ============ Adapter ============ */
    static class RoomsSectionAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        static final int VT_HEADER = 0, VT_ROOM = 1;
        private final List<Row> data;
        RoomsSectionAdapter(List<Row> d){ data=d; }

        @Override public int getItemViewType(int pos) { return (data.get(pos) instanceof HeaderRow) ? VT_HEADER : VT_ROOM; }

        @NonNull @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup p, int vt) {
            LayoutInflater inf = LayoutInflater.from(p.getContext());
            if (vt == VT_HEADER) return new HeaderVH(inf.inflate(R.layout.row_section_header, p, false));
            return new RoomVH(inf.inflate(R.layout.home_row, p, false));
        }

        @Override public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int pos) {
            if (getItemViewType(pos) == VT_HEADER) {
                HeaderRow hr = (HeaderRow) data.get(pos);
                HeaderVH hv = (HeaderVH) holder;
                hv.tvTitle.setText(hr.houseName);
                if (hv.tvSubtitle != null) hv.tvSubtitle.setText(hr.houseLocation); // <— địa chỉ
                return;
            }
            RoomRow rr = (RoomRow) data.get(pos);
            Room r = rr.room;
            RoomVH vh = (RoomVH) holder;
            vh.ivIcon.setImageResource(r.getIconRes() != 0 ? r.getIconRes() : R.drawable.room);
            vh.tvName.setText(r.getName());
            vh.tvCount.setText(r.getDeviceCount() + " thiết bị");

            // Hiển thị mô tả phòng (description) nếu có
            if (r.getDescription() != null && !r.getDescription().isEmpty()) {
                vh.tvSubtitle.setText(r.getDescription());
                vh.tvSubtitle.setVisibility(View.VISIBLE);
            } else {
                vh.tvSubtitle.setVisibility(View.GONE);
            }

            vh.itemView.setOnClickListener(v -> {
                Intent i = new Intent(v.getContext(), RoomDetailsActivity.class);
                i.putExtra("house_id", rr.houseId);
                i.putExtra("room_id",  r.getId());
                v.getContext().startActivity(i);
            });
            vh.itemView.setOnLongClickListener(v -> false);

            // Xử lý click vào nút menu (3 chấm)
            vh.btnMenuOptions.setOnClickListener(v -> {
                Context context = v.getContext();
                if (context instanceof AllRoomsActivity) {
                    ((AllRoomsActivity) context).showRoomOptionsDialog(v, rr.houseId, r);
                }
            });
        }

        @Override public int getItemCount() { return data.size(); }

        static class HeaderVH extends RecyclerView.ViewHolder {
            final TextView tvTitle, tvSubtitle;
            HeaderVH(View v){ super(v);
                tvTitle = v.findViewById(R.id.tvSectionTitle);
                tvSubtitle = v.findViewById(R.id.tvSectionSubtitle); // cần có trong XML
            }
        }
        static class RoomVH extends RecyclerView.ViewHolder {
            final ImageView ivIcon; final TextView tvName, tvCount, tvSubtitle;
            final ImageButton btnMenuOptions;

            RoomVH(View v){ super(v);
                ivIcon=v.findViewById(R.id.ivRoomIcon);
                tvName=v.findViewById(R.id.tvRoomName);
                tvCount=v.findViewById(R.id.tvDeviceCount);
                tvSubtitle=v.findViewById(R.id.tvSubtitle);
                btnMenuOptions=v.findViewById(R.id.btnMenuOptions);
            }
        }
    }

    /**
     * Hiển thị dialog với các tùy chọn sửa và xóa phòng
     * @param view View được click (để định vị dialog)
     * @param houseId ID của nhà chứa phòng
     * @param room Đối tượng phòng cần thao tác
     */
    public void showRoomOptionsDialog(View view, String houseId, Room room) {
        // Tạo dialog với 2 tùy chọn: Sửa và Xóa
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Tùy chọn phòng");

        // Tạo danh sách các tùy chọn
        String[] options = {"Sửa phòng", "Xóa phòng"};

        builder.setItems(options, (dialog, which) -> {
            switch (which) {
                case 0: // Sửa phòng
                    showEditRoomDialog(houseId, room);
                    break;
                case 1: // Xóa phòng
                    showDeleteConfirmDialog(room);
                    break;
            }
        });

        builder.show();
    }

    /**
     * Hiển thị dialog xác nhận xóa phòng
     * @param room Phòng cần xóa
     */
    private void showDeleteConfirmDialog(Room room) {
        new AlertDialog.Builder(this)
                .setTitle("Xác nhận xóa")
                .setMessage("Bạn có chắc chắn muốn xóa phòng \"" + room.getName() + "\"?")
                .setPositiveButton("Xóa", (dialog, which) -> deleteRoom(room.getId()))
                .setNegativeButton("Hủy", null)
                .show();
    }

    /**
     * Gọi API xóa phòng
     * @param roomId ID của phòng cần xóa
     */
    private void deleteRoom(String roomId) {
        showLoading(true);

        Api api = ApiClient.getClient(this).create(Api.class);
        api.deleteRoom(Integer.parseInt(roomId)).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                showLoading(false);

                if (response.isSuccessful()) {
                    toast("Đã xóa phòng thành công");
                    // Cập nhật lại danh sách phòng
                    fetchHouseLocationsThenRooms(0, 200, 0, 50);
                } else {
                    toast("Lỗi khi xóa phòng: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                showLoading(false);
                toast("Lỗi kết nối: " + t.getMessage());
            }
        });
    }

    /**
     * Hiển thị dialog chỉnh sửa thông tin phòng
     * @param houseId ID của nhà chứa phòng
     * @param room Phòng cần chỉnh sửa
     */
    private void showEditRoomDialog(String houseId, Room room) {
        // Cần lấy danh sách nhà để người dùng có thể chọn chuyển phòng sang nhà khác
        showLoading(true);

        Api api = ApiClient.getClient(this).create(Api.class);
        api.getHouses(0, 100).enqueue(new Callback<HouseListWrap>() {
            @Override
            public void onResponse(Call<HouseListWrap> call, Response<HouseListWrap> response) {
                showLoading(false);

                if (!response.isSuccessful() || response.body() == null || response.body().houses == null || response.body().houses.isEmpty()) {
                    toast("Không thể tải danh sách nhà");
                    return;
                }

                // Chuẩn bị dialog chỉnh sửa với danh sách nhà
                showEditRoomDialogWithHouses(houseId, room, response.body().houses);
            }

            @Override
            public void onFailure(Call<HouseListWrap> call, Throwable t) {
                showLoading(false);
                toast("Lỗi kết nối: " + t.getMessage());
            }
        });
    }

    /**
     * Hiển thị dialog chỉnh sửa thông tin phòng với danh sách nhà
     * @param houseId ID của nhà hiện tại
     * @param room Phòng cần chỉnh sửa
     * @param houses Danh sách nhà để chọn
     */
    private void showEditRoomDialogWithHouses(String houseId, Room room, List<HouseDto> houses) {
        // Tạo dialog view từ layout
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_room_add_all, null);

        // Lấy references đến các view
        Spinner spHouse = dialogView.findViewById(R.id.spHouse);
        EditText etName = dialogView.findViewById(R.id.etName);
        EditText etDescription = dialogView.findViewById(R.id.etDescription);

        // Thiết lập giá trị hiện tại
        etName.setText(room.getName());
        etDescription.setText(room.getDescription() != null ? room.getDescription() : "");

        // Thiết lập spinner nhà
        String[] houseNames = new String[houses.size()];
        final int[] houseIds = new int[houses.size()];
        int selectedPosition = 0;

        for (int i = 0; i < houses.size(); i++) {
            houseNames[i] = houses.get(i).name;
            houseIds[i] = houses.get(i).id;

            // Tìm vị trí nhà hiện tại của phòng
            if (String.valueOf(houses.get(i).id).equals(houseId)) {
                selectedPosition = i;
            }
        }

        // Thiết lập adapter cho spinner
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, houseNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spHouse.setAdapter(adapter);
        spHouse.setSelection(selectedPosition);

        // Hiển thị dialog
        new AlertDialog.Builder(this)
                .setTitle("Sửa phòng")
                .setView(dialogView)
                .setPositiveButton("Lưu", (dialog, which) -> {
                    // Lấy thông tin từ form
                    String name = etName.getText().toString().trim();
                    String description = etDescription.getText().toString().trim();
                    int selectedHouseId = houseIds[spHouse.getSelectedItemPosition()];

                    // Kiểm tra tên phòng
                    if (name.isEmpty()) {
                        toast("Vui lòng nhập tên phòng");
                        return;
                    }

                    // Giữ nguyên type và iconKey
                    updateRoom(room.getId(), name, description, "generic", "generic", selectedHouseId);
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    /**
     * Gọi API cập nhật thông tin phòng
     * @param roomId ID của phòng cần cập nhật
     * @param name Tên mới của phòng
     * @param detail Mô tả mới
     * @param type Loại phòng
     * @param iconKey Mã icon
     * @param houseId ID của nhà (có thể đã thay đổi)
     */
    private void updateRoom(String roomId, String name, String detail, String type, String iconKey, int houseId) {
        showLoading(true);

        Api api = ApiClient.getClient(this).create(Api.class);
        UpdateRoomReq request = new UpdateRoomReq(name, detail, type, iconKey, houseId);

        api.updateRoom(Integer.parseInt(roomId), request).enqueue(new Callback<RoomDto>() {
            @Override
            public void onResponse(Call<RoomDto> call, Response<RoomDto> response) {
                showLoading(false);

                if (response.isSuccessful() && response.body() != null) {
                    toast("Cập nhật phòng thành công");
                    // Cập nhật lại danh sách phòng
                    fetchHouseLocationsThenRooms(0, 200, 0, 50);
                } else {
                    String errorMessage = "";
                    try {
                        if (response.errorBody() != null) {
                            errorMessage = response.errorBody().string();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    toast("Lỗi khi cập nhật phòng: " + response.code() + " " + errorMessage);
                }
            }

            @Override
            public void onFailure(Call<RoomDto> call, Throwable t) {
                showLoading(false);
                toast("Lỗi kết nối: " + t.getMessage());
            }
        });
    }
}
