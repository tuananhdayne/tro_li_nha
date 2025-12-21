package com.example.smarthomeui.smarthome.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smarthomeui.R;
import com.example.smarthomeui.smarthome.adapter.HouseAdapter;
import com.example.smarthomeui.smarthome.ai_speech_reg.MainActivitySR;
import com.example.smarthomeui.smarthome.model.House;
import com.example.smarthomeui.smarthome.network.Api;
import com.example.smarthomeui.smarthome.network.ApiClient;
import com.example.smarthomeui.smarthome.network.CreateHouseReq;
import com.example.smarthomeui.smarthome.network.HouseDto;
import com.example.smarthomeui.smarthome.network.HouseListWrap;
import com.example.smarthomeui.smarthome.network.ToggleActiveRequest;
import com.example.smarthomeui.smarthome.network.ToggleResponse;
import com.example.smarthomeui.smarthome.network.UpdateHouseReq;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HouseListActivity extends BaseActivity {

    private TextView tvUserEmail, tvUserName;

    private RecyclerView rv;
    private HouseAdapter adapter;
    private final List<House> houses = new ArrayList<>();

    private View loadingView;
    private boolean isLoading = false;

    private ImageView ivPlus;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initUserViews();
        loadUserProfile();      // lấy tên/email user cho header

        setupRecycler();
        setupBottomBar();
        attachSimpleLoading();

        // Gọi API lấy danh sách nhà
        fetchHousesFromApi(0, 20);

        // FAB (tạm thời tắt tạo nhà local để tránh lệch dữ liệu)
        View fab = findViewById(R.id.fabAddHouse);
        if (fab != null) {
            fab.setOnClickListener(v -> showCreateHouseDialog());
        }
    }
    private void showCreateHouseDialog() {
        View form = getLayoutInflater().inflate(R.layout.dialog_house_form, null, false);
        android.widget.EditText etName = form.findViewById(R.id.etName);
        android.widget.EditText etDesc = form.findViewById(R.id.etDescription); // dùng cho location

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Thêm nhà")
                .setView(form)
                .setPositiveButton("Tạo", (d, w) -> {
                    String name = etName.getText() == null ? "" : etName.getText().toString().trim();
                    String location = etDesc.getText() == null ? "" : etDesc.getText().toString().trim();
                    callApiCreateHouse(name, location);
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void callApiCreateHouse(String name, String location) {
        if (name.isEmpty()) { toast("Tên nhà không được rỗng"); return; }

        showLoading(true);
        Api api = ApiClient.getClient(this).create(Api.class);
        api.createHouse(new CreateHouseReq(name, location)).enqueue(new retrofit2.Callback<HouseDto>() {
            @Override public void onResponse(Call<HouseDto> call, Response<HouseDto> resp) {
                showLoading(false);
                if (!resp.isSuccessful() || resp.body() == null) {
                    toast("Tạo nhà thất bại: " + resp.code());
                    return;
                }
                // Thêm vào list hiện tại
                HouseDto dto = resp.body();
                House h = mapToUiHouse(dto);
                houses.add(0, h);
                adapter.notifyItemInserted(0);
                rv.scrollToPosition(0);
                toast("Đã tạo nhà");
            }
            @Override public void onFailure(Call<HouseDto> call, Throwable t) {
                showLoading(false);
                toast("Lỗi mạng khi tạo nhà: " + (t != null ? t.getMessage() : ""));
            }
        });
    }
    /* ---------------- UI ---------------- */

    private void initUserViews() {
        tvUserEmail = findViewById(R.id.tvUserEmail);
        tvUserName  = findViewById(R.id.tvUserName);
        ivPlus = findViewById(R.id.ivPlus);
    }

    private void setupRecycler() {
        rv = findViewById(R.id.recycler_view);
        rv.setLayoutManager(new GridLayoutManager(this, 2));

        adapter = new HouseAdapter(
                houses,
                house -> {
                    Intent i = new Intent(HouseListActivity.this, HouseRoomsActivity.class);
                    i.putExtra("house_id", house.getId());
                    startActivity(i);
                },
                (house, position) -> showHouseActionsDialog(house, position),  // long click
                (house, position) -> showHouseActionsDialog(house, position)   // menu click
        );

        // Set toggle listener để gọi API khi bật/tắt nhà
        adapter.setOnHouseToggleListener((house, isActive) -> {
            callApiToggleHouse(house, isActive);
        });

        rv.setAdapter(adapter);
    }

    private void callApiToggleHouse(House house, boolean isActive) {
        Api api = ApiClient.getClient(this).create(Api.class);
        int houseId = Integer.parseInt(house.getId());
        
        api.toggleHouseActive(houseId, new ToggleActiveRequest(isActive)).enqueue(new Callback<ToggleResponse>() {
            @Override
            public void onResponse(Call<ToggleResponse> call, Response<ToggleResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    String msg = isActive ? "Đã kích hoạt " + house.getName() : "Đã tắt " + house.getName();
                    toast(msg);
                } else {
                    toast("Lỗi: " + response.code());
                    // Revert state
                    house.setActive(!isActive);
                    adapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onFailure(Call<ToggleResponse> call, Throwable t) {
                toast("Lỗi mạng: " + t.getMessage());
                // Revert state
                house.setActive(!isActive);
                adapter.notifyDataSetChanged();
            }
        });
    }
    private void showHouseActionsDialog(House house, int position) {
        String[] actions = {"Sửa", "Xoá"};
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(house.getName())
                .setItems(actions, (d, which) -> {
                    if (which == 0) showEditHouseDialog(house, position);
                    else if (which == 1) confirmDeleteHouse(house, position);
                })
                .show();
    }
    private void showEditHouseDialog(House house, int position) {
        View form = getLayoutInflater().inflate(R.layout.dialog_house_form, null, false);
        android.widget.EditText etName = form.findViewById(R.id.etName);
        android.widget.EditText etDesc = form.findViewById(R.id.etDescription);
        etName.setText(house.getName());
        etDesc.setText(house.getDescription()); // đang dùng tạm cho location

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Sửa nhà")
                .setView(form)
                .setPositiveButton("Lưu", (d, w) -> {
                    String name = etName.getText() == null ? "" : etName.getText().toString().trim();
                    String location = etDesc.getText() == null ? "" : etDesc.getText().toString().trim();
                    callApiUpdateHouse(house, position, name, location);
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void callApiUpdateHouse(House house, int position, String name, String location) {
        int id = safeParseInt(house.getId(), -1);
        if (id <= 0) { toast("ID nhà không hợp lệ"); return; }

        showLoading(true);
        Api api = ApiClient.getClient(this).create(Api.class);
        api.updateHouse(id, new UpdateHouseReq(name, location)).enqueue(new retrofit2.Callback<HouseDto>() {
            @Override public void onResponse(Call<HouseDto> call, Response<HouseDto> resp) {
                showLoading(false);
                if (!resp.isSuccessful() || resp.body() == null) {
                    toast("Cập nhật nhà thất bại: " + resp.code());
                    return;
                }
                // Cập nhật UI
                house.setName(name.isEmpty() ? house.getName() : name);
                house.setDescription(location);
                adapter.notifyItemChanged(position);
                toast("Đã cập nhật nhà");
            }
            @Override public void onFailure(Call<HouseDto> call, Throwable t) {
                showLoading(false);
                toast("Lỗi mạng khi cập nhật: " + (t != null ? t.getMessage() : ""));
            }
        });
    }
    private void confirmDeleteHouse(House house, int position) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Xoá nhà")
                .setMessage("Bạn chắc chắn muốn xoá nhà này?")
                .setPositiveButton("Xoá", (d, w) -> callApiDeleteHouse(house, position))
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void callApiDeleteHouse(House house, int position) {
        int id = safeParseInt(house.getId(), -1);
        if (id <= 0) { toast("ID nhà không hợp lệ"); return; }

        showLoading(true);
        Api api = ApiClient.getClient(this).create(Api.class);
        api.deleteHouse(id).enqueue(new retrofit2.Callback<Void>() {
            @Override public void onResponse(Call<Void> call, Response<Void> resp) {
                showLoading(false);
                if (!resp.isSuccessful()) {
                    toast("Xoá nhà thất bại: " + resp.code());
                    return;
                }
                houses.remove(position);
                adapter.notifyItemRemoved(position);
                toast("Đã xoá nhà");
            }
            @Override public void onFailure(Call<Void> call, Throwable t) {
                showLoading(false);
                toast("Lỗi mạng khi xoá: " + (t != null ? t.getMessage() : ""));
            }
        });
    }

    private void setupBottomBar() {
        findViewById(R.id.ivRooms).setOnClickListener(v ->
                startActivity(new Intent(this, AllRoomsActivity.class)));

        findViewById(R.id.ivDevices).setOnClickListener(v ->
                startActivity(new Intent(this, DeviceInventoryActivity.class)));

        View ivSetting = findViewById(R.id.ivSetting);
        if (ivSetting != null) ivSetting.setOnClickListener(v ->
                startActivity(new Intent(this, SettingsActivity.class)));

        ivPlus.setOnClickListener(v -> {
            com.example.smarthomeui.smarthome.ui.bottomsheet.VoiceAssistantBottomSheet sheet = 
                new com.example.smarthomeui.smarthome.ui.bottomsheet.VoiceAssistantBottomSheet();
            sheet.show(getSupportFragmentManager(), "VoiceAssistant");
        });

        // đánh dấu tab Home
        View ivHome = findViewById(R.id.ivHome);
        if (ivHome != null) ivHome.setSelected(true);
    }

    private void attachSimpleLoading() {
        loadingView = findViewById(R.id.loadingView);
        // Nếu layout chưa có loadingView, có thể thêm 1 ProgressBar overlay sau
    }
    private void showLoading(boolean show) {
        if (loadingView != null) loadingView.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    /* --------------- USER PROFILE (giữ nguyên cách gọi trong BaseActivity) --------------- */

    private void loadUserProfile() {
        getUserProfile(profile -> {
            if (tvUserEmail != null) tvUserEmail.setText(profile.getEmail());
            if (tvUserName != null)  tvUserName.setText(profile.getName() != null ? profile.getName() : "User");
        }, err -> {
            String email = getCurrentUserEmail();
            if (tvUserEmail != null && email != null) tvUserEmail.setText(email);
            if (tvUserName != null) tvUserName.setText("User");
            android.util.Log.e("HouseList", "Lỗi load profile: " + err);
        });
    }

    /* ---------------- API ---------------- */

    // com.example.smarthomeui.smarthome.ui.activity.HouseListActivity
    private void fetchHousesFromApi(int skip, int take) {
        if (isLoading) return;
        isLoading = true;
        showLoading(true);

        Api api = ApiClient.getClient(this).create(Api.class); //

        // Nếu chưa có token thì về login
        if (getAccessToken() == null || getAccessToken().isEmpty()) {
            toast("Phiên đăng nhập đã hết hạn.");
            redirectToLogin();
            return;
        }



        api.getHouses(skip, take).enqueue(new Callback<HouseListWrap>() {
            @Override public void onResponse(Call<HouseListWrap> call, Response<HouseListWrap> resp) {
                isLoading = false; showLoading(false);

                if (!resp.isSuccessful() || resp.body() == null) {
                    String err = null;
                    try { err = resp.errorBody() != null ? resp.errorBody().string() : null; } catch (Exception ignore) {}
                    android.util.Log.e("HOUSES", "code=" + resp.code() + " body=" + err);
                    toast("Tải danh sách nhà thất bại: " + resp.code());
                    if (resp.code() == 401 || resp.code() == 403) redirectToLogin();
                    return;
                }
                houses.clear();
                if (resp.body().houses != null) {
                    for (HouseDto dto : resp.body().houses) {
                        houses.add(mapToUiHouse(dto));
                    }
                }
                adapter.notifyDataSetChanged();
            }

            @Override public void onFailure(Call<HouseListWrap> call, Throwable t) {
                isLoading = false; showLoading(false);
                android.util.Log.e("HOUSES", "onFailure", t);
                toast("Lỗi mạng: " + (t != null ? t.getMessage() : ""));
            }
        });
    }

    private House mapToUiHouse(HouseDto dto) {
        House h = new House(String.valueOf(dto.id),
                safe(dto.name, "Nhà"),
                R.drawable.home);
        h.setDescription(dto.location); // dùng field description để tạm chứa location
        h.setActive(dto.isActive); // set trạng thái kích hoạt từ server
        h.setRoomCount(dto.roomsCount); // set số phòng từ API
        return h;
    }

    public String getBearerTokenOrNull() {
        String raw = getAccessToken(); // <-- dùng hàm có sẵn trong BaseActivity
        if (raw == null || raw.isEmpty()) return null;
        return raw.startsWith("Bearer ") ? raw : ("Bearer " + raw);
    }

    private static String safe(String s, String def) {
        return (s == null || s.trim().isEmpty()) ? def : s.trim();
    }
    private int safeParseInt(String s, int def) {
        try { return Integer.parseInt(s); } catch (Exception e) { return def; }
    }

    private void toast(String s) { Toast.makeText(this, s, Toast.LENGTH_SHORT).show(); }
}


