package com.example.smarthomeui.smarthome.network;

import com.google.gson.*;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Gói dữ liệu trả về từ GET /api/room: nhóm phòng theo nhà (dạng .NET có $values) */
public class RoomsByHouseWrap {
    public List<Group> groups = new ArrayList<>();
    public Integer total;
    public Integer skip;
    public Integer take;

    public static class Group {
        public int houseId;
        public String houseName;
        public String houseLocation;
        public List<RoomDto> rooms = new ArrayList<>();
    }

    /**
     * Lấy tất cả các phòng từ tất cả các nhóm (flatten)
     */
    public List<RoomDto> getRooms() {
        List<RoomDto> allRooms = new ArrayList<>();
        if (groups != null) {
            for (Group group : groups) {
                if (group.rooms != null) {
                    allRooms.addAll(group.rooms);
                }
            }
        }
        return allRooms;
    }

    public static class Deserializer implements JsonDeserializer<RoomsByHouseWrap> {
        @Override
        public RoomsByHouseWrap deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext ctx)
                throws JsonParseException {
            RoomsByHouseWrap out = new RoomsByHouseWrap();
            if (json == null || !json.isJsonObject()) return out;

            JsonObject root = json.getAsJsonObject();
            if (root.has("total")) out.total = safeInt(root.get("total"));
            if (root.has("skip"))  out.skip  = safeInt(root.get("skip"));
            if (root.has("take"))  out.take  = safeInt(root.get("take"));

            // Map để lưu trữ các đối tượng theo ID tham chiếu
            Map<String, JsonObject> refMap = new HashMap<>();

            // Xây dựng refMap từ tất cả các đối tượng có $id
            buildReferenceMap(root, refMap);

            // Xử lý cấu trúc mới: housesWithRooms là một object với key là houseId
            if (root.has("housesWithRooms") && root.get("housesWithRooms").isJsonObject()) {
                JsonObject housesObj = root.getAsJsonObject("housesWithRooms");

                // Bỏ qua các trường $id, xử lý các key là houseId
                for (Map.Entry<String, JsonElement> entry : housesObj.entrySet()) {
                    String key = entry.getKey();
                    JsonElement value = entry.getValue();

                    // Bỏ qua các trường không phải số (như $id)
                    if (!isNumeric(key) || !value.isJsonObject()) continue;

                    int houseId = Integer.parseInt(key);
                    JsonObject houseRoomsObj = value.getAsJsonObject();

                    if (houseRoomsObj.has("$values") && houseRoomsObj.get("$values").isJsonArray()) {
                        JsonArray roomsArray = houseRoomsObj.getAsJsonArray("$values");

                        // Nếu không có phòng nào, vẫn tạo header cho nhà
                        if (roomsArray.size() == 0) {
                            // Tìm thông tin nhà từ API nhà đã lấy trước đó
                            continue; // Bỏ qua nhà không có phòng
                        }

                        Group g = new Group();
                        g.houseId = houseId;
                        g.rooms = new ArrayList<>();

                        // Khởi tạo thông tin nhà mặc định
                        g.houseName = "Nhà";
                        g.houseLocation = "Địa chỉ";

                        // Tìm thông tin nhà từ API hoặc trong JSON
                        boolean houseInfoFound = false;

                        for (JsonElement roomEl : roomsArray) {
                            JsonObject roomObj = null;

                            // Xử lý trường hợp đối tượng là tham chiếu ($ref)
                            if (roomEl.isJsonObject() && roomEl.getAsJsonObject().has("$ref")) {
                                String refId = roomEl.getAsJsonObject().get("$ref").getAsString();
                                if (refMap.containsKey(refId)) {
                                    roomObj = refMap.get(refId);
                                }
                            } else if (roomEl.isJsonObject()) {
                                roomObj = roomEl.getAsJsonObject();
                            }

                            if (roomObj == null) continue;

                            // Lấy thông tin phòng
                            RoomDto dto = new RoomDto();
                            dto.id = roomObj.has("id") ? roomObj.get("id").getAsInt() : 0;
                            dto.name = roomObj.has("name") ? roomObj.get("name").getAsString() : "Phòng";
                            dto.detail = roomObj.has("detail") ? roomObj.get("detail").getAsString() : null;

                            dto.deviceCount = 0; // Mặc định

                            if (roomObj.has("devices") && roomObj.get("devices").isJsonObject()) {
                                JsonObject devicesObj = roomObj.getAsJsonObject("devices");
                                if (devicesObj.has("$values") && devicesObj.get("$values").isJsonArray()) {
                                    dto.deviceCount = devicesObj.getAsJsonArray("$values").size();
                                }
                            }

                            // Lấy thông tin nhà từ phòng đầu tiên
                            if (!houseInfoFound && roomObj.has("house") && roomObj.get("house").isJsonObject()) {
                                JsonObject houseObj = roomObj.getAsJsonObject("house");
                                if (houseObj.has("name") && !houseObj.get("name").isJsonNull()) {
                                    g.houseName = houseObj.get("name").getAsString();
                                }
                                if (houseObj.has("location") && !houseObj.get("location").isJsonNull()) {
                                    g.houseLocation = houseObj.get("location").getAsString();
                                }
                                houseInfoFound = true;
                            }

                            g.rooms.add(dto);
                        }

                        if (g.rooms.size() > 0) {
                            out.groups.add(g);
                        }
                    }
                }
            }

            return out;
        }

        // Hàm đệ quy để xây dựng map các object có $id
        private void buildReferenceMap(JsonElement element, Map<String, JsonObject> refMap) {
            if (element == null) return;

            if (element.isJsonObject()) {
                JsonObject obj = element.getAsJsonObject();

                // Nếu đối tượng có $id, thêm vào map
                if (obj.has("$id")) {
                    String id = obj.get("$id").getAsString();
                    refMap.put(id, obj);
                }

                // Đệ quy với tất cả các thuộc tính
                for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
                    buildReferenceMap(entry.getValue(), refMap);
                }
            }
            else if (element.isJsonArray()) {
                // Đệ quy với tất cả các phần tử trong mảng
                for (JsonElement item : element.getAsJsonArray()) {
                    buildReferenceMap(item, refMap);
                }
            }
        }

        private boolean isNumeric(String str) {
            try {
                Integer.parseInt(str);
                return true;
            } catch (NumberFormatException e) {
                return false;
            }
        }

        private static Integer safeInt(JsonElement e) {
            return (e != null && e.isJsonPrimitive()) ? e.getAsInt() : null;
        }
    }
}
