package com.example.smarthomeui.smarthome.network;

import com.google.gson.*;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class RoomListWrap {
    public List<RoomDto> rooms = new ArrayList<>();
    public Integer total;
    public Integer skip;
    public Integer take;

    /**
     * Deserializer tùy chỉnh để xử lý cấu trúc JSON phức tạp từ API
     */
    public static class Deserializer implements JsonDeserializer<RoomListWrap> {
        @Override
        public RoomListWrap deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                throws JsonParseException {
            RoomListWrap result = new RoomListWrap();

            if (json == null || !json.isJsonObject()) {
                return result;
            }

            JsonObject rootObj = json.getAsJsonObject();

            // Lấy skip, take nếu có
            if (rootObj.has("skip") && !rootObj.get("skip").isJsonNull()) {
                result.skip = rootObj.get("skip").getAsInt();
            }
            if (rootObj.has("take") && !rootObj.get("take").isJsonNull()) {
                result.take = rootObj.get("take").getAsInt();
            }

            // Xử lý danh sách phòng
            if (rootObj.has("rooms") && rootObj.get("rooms").isJsonObject()) {
                JsonObject roomsObj = rootObj.getAsJsonObject("rooms");

                if (roomsObj.has("$values") && roomsObj.get("$values").isJsonArray()) {
                    JsonArray roomsArray = roomsObj.getAsJsonArray("$values");

                    for (JsonElement roomElem : roomsArray) {
                        if (roomElem.isJsonObject()) {
                            JsonObject roomObj = roomElem.getAsJsonObject();
                            RoomDto roomDto = new RoomDto();

                            if (roomObj.has("id") && !roomObj.get("id").isJsonNull()) {
                                roomDto.id = roomObj.get("id").getAsInt();
                            }
                            if (roomObj.has("name") && !roomObj.get("name").isJsonNull()) {
                                roomDto.name = roomObj.get("name").getAsString();
                            }
                            if (roomObj.has("detail") && !roomObj.get("detail").isJsonNull()) {
                                roomDto.detail = roomObj.get("detail").getAsString();
                            }
                            if (roomObj.has("houseID") && !roomObj.get("houseID").isJsonNull()) {
                                roomDto.houseID = roomObj.get("houseID").getAsInt();
                            }

                            // Đếm số thiết bị nếu có
                            if (roomObj.has("devices") && roomObj.get("devices").isJsonObject()) {
                                JsonObject devicesObj = roomObj.getAsJsonObject("devices");
                                if (devicesObj.has("$values") && devicesObj.get("$values").isJsonArray()) {
                                    JsonArray devicesArray = devicesObj.getAsJsonArray("$values");
                                    roomDto.deviceCount = devicesArray.size();
                                } else {
                                    roomDto.deviceCount = 0;
                                }
                            } else {
                                roomDto.deviceCount = 0;
                            }

                            result.rooms.add(roomDto);
                        }
                    }
                }
            }

            // Cập nhật tổng số phòng
            result.total = result.rooms.size();

            return result;
        }
    }
}
