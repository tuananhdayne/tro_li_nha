package com.example.smarthomeui.smarthome.ai_speech_reg;

import android.content.Context;

import androidx.annotation.Nullable;

import com.example.smarthomeui.smarthome.model.Device;
import com.example.smarthomeui.smarthome.network.Api;
import com.example.smarthomeui.smarthome.network.ApiClient;
import com.example.smarthomeui.smarthome.network.DeviceDto;
import com.example.smarthomeui.smarthome.network.DeviceListWrap;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static com.example.smarthomeui.smarthome.ai_speech_reg.StringUtilsVN.fold;

public class DeviceRegistry {

    public static class CandidateResult {
        public final Device device;
        public final int score;
        public CandidateResult(Device d, int s) { this.device = d; this.score = s; }
    }

    public interface LoadCallback {
        void onLoaded(int count);
        void onError(String message);
    }

    private final List<Device> devices = new ArrayList<>();
    private volatile boolean loading = false;

    public DeviceRegistry() {}

    public boolean isEmpty() { synchronized (devices) { return devices.isEmpty(); } }
    public boolean isLoading() { return loading; }

    public List<Device> getAll() {
        synchronized (devices) {
            return Collections.unmodifiableList(new ArrayList<>(devices));
        }
    }

    private void replaceAll(List<Device> newDevices) {
        synchronized (devices) {
            devices.clear();
            if (newDevices != null) devices.addAll(newDevices);
        }
    }

    /** Nạp danh sách thiết bị thực từ API: GET /api/device?skip=&take= */
    public void refreshFromApi(Context ctx, int skip, int take, @Nullable LoadCallback cb) {
        if (loading) return;
        loading = true;

        Api api = ApiClient.getClient(ctx).create(Api.class);
        api.getDevices(skip, take).enqueue(new Callback<DeviceListWrap>() {
            @Override
            public void onResponse(Call<DeviceListWrap> call, Response<DeviceListWrap> resp) {
                loading = false;
                if (!resp.isSuccessful() || resp.body() == null) {
                    if (cb != null) cb.onError("API lỗi: " + resp.code());
                    return;
                }
                List<DeviceDto> dtos = resp.body().getDevices();
                List<Device> mapped = mapDtosToDevices(dtos);
                replaceAll(mapped);
                if (cb != null) cb.onLoaded(mapped.size());
            }

            @Override
            public void onFailure(Call<DeviceListWrap> call, Throwable t) {
                loading = false;
                if (cb != null) cb.onError("Mạng/API lỗi: " + t.getMessage());
            }
        });
    }

    // ---------- Mapping: DeviceDto -> model.Device (không dùng setId) ----------
    private List<Device> mapDtosToDevices(List<DeviceDto> dtos) {
        List<Device> out = new ArrayList<>();
        if (dtos == null) return out;

        for (DeviceDto dto : dtos) {
            // Device không có setId -> dùng constructor
            String id    = String.valueOf(dto.getId());
            String name  = dto.getName() != null ? dto.getName() : "Thiết bị";
            String type  = dto.getType() != null ? dto.getType() : "Light";
            String room  = "không rõ"; // DTO chưa có room
            boolean online = false;    // chưa có trạng thái -> mặc định false
            int value = 0;
            String power = null;

            Device d = new Device(id, name, room, type, online, value, power);

            // Gán capabilities theo type để UI/voice hiểu đúng
            if ("RGBLight".equalsIgnoreCase(type)) {
                d.addCaps(Device.CAP_POWER, Device.CAP_BRIGHTNESS, Device.CAP_COLOR);
            } else if ("Light".equalsIgnoreCase(type)) {
                d.addCaps(Device.CAP_POWER, Device.CAP_BRIGHTNESS);
            } else if ("Fan".equalsIgnoreCase(type)) {
                d.addCaps(Device.CAP_POWER, Device.CAP_SPEED);
            } else if ("AC".equalsIgnoreCase(type)) {
                d.addCaps(Device.CAP_POWER, Device.CAP_TEMPERATURE);
            } else {
                d.addCaps(Device.CAP_POWER); // mặc định có bật/tắt
            }

            out.add(d);
        }
        return out;
    }

    // ===================== Fuzzy Matching / Scoring =====================
    public List<CandidateResult> rankCandidates(String deviceText, @Nullable String roomTextOpt, int limit) {
        String key = fold(deviceText + " " + (roomTextOpt == null ? "" : roomTextOpt));
        List<CandidateResult> list = new ArrayList<>();
        List<Device> snapshot = getAll();

        for (Device d : snapshot) {
            int score = 0;
            String nameF = fold(ns(d.getName()));
            String roomF = fold(ns(d.getRoom()));

            if (key.contains(nameF)) score += nameF.length();
            for (String token : nameF.split(" ")) if (!token.isEmpty() && key.contains(token)) score++;
            if (roomTextOpt != null && key.contains(roomF)) score += roomF.length();

            String type = ns(d.getType()).toLowerCase();
            if (key.contains("den")  && (type.contains("light") || d.isLight())) score += 2;
            if (key.contains("quat") && (type.contains("fan")   || d.isFan()))   score += 2;

            list.add(new CandidateResult(d, score));
        }
        list.sort((a, b) -> Integer.compare(b.score, a.score));
        return list.size() > limit ? list.subList(0, limit) : list;
    }

    public int estimateTopScore(String deviceText, @Nullable String roomTextOpt) {
        List<CandidateResult> c = rankCandidates(deviceText, roomTextOpt, 1);
        return c.isEmpty() ? 0 : c.get(0).score;
    }

    private static String ns(String s) { return s == null ? "" : s; }
}