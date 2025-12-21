package com.example.smarthomeui.smarthome.network;

import com.google.gson.*;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/** Phản hồi cho GET /api/house (dạng .NET có $values, và mỗi item có field "house") */
public class HouseListWrap {
    public List<HouseDto> houses = new ArrayList<>();
    public Integer total;
    public Integer skip;
    public Integer take;

    public static class Deserializer implements JsonDeserializer<HouseListWrap> {
        @Override
        public HouseListWrap deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext ctx)
                throws JsonParseException {
            HouseListWrap out = new HouseListWrap();
            if (json == null || !json.isJsonObject()) return out;

            JsonObject root = json.getAsJsonObject();

            // meta
            if (root.has("total") && root.get("total").isJsonPrimitive()) out.total = root.get("total").getAsInt();
            if (root.has("skip")  && root.get("skip").isJsonPrimitive())  out.skip  = root.get("skip").getAsInt();
            if (root.has("take")  && root.get("take").isJsonPrimitive())  out.take  = root.get("take").getAsInt();

            // houses: object có $values
            if (root.has("houses") && root.get("houses").isJsonObject()) {
                JsonObject housesObj = root.getAsJsonObject("houses");
                if (housesObj.has("$values") && housesObj.get("$values").isJsonArray()) {
                    JsonArray arr = housesObj.getAsJsonArray("$values");
                    for (JsonElement e : arr) {
                        if (!e.isJsonObject()) continue;
                        JsonObject item = e.getAsJsonObject();

                        // ===== DẠNG A: mỗi phần tử có field "house" =====
                        JsonObject h = null;
                        if (item.has("house") && item.get("house").isJsonObject()) {
                            h = item.getAsJsonObject("house");
                        }

                        HouseDto dto = new HouseDto();
                        if (h != null) {
                            if (h.has("id")       && h.get("id").isJsonPrimitive())       dto.id       = h.get("id").getAsInt();
                            if (h.has("name")     && h.get("name").isJsonPrimitive())     dto.name     = h.get("name").getAsString();
                            if (h.has("location") && h.get("location").isJsonPrimitive()) dto.location = h.get("location").getAsString();
                        } else {
                            // ===== DẠNG B: id/name/location nằm ngay trong item =====
                            if (item.has("id")       && item.get("id").isJsonPrimitive())       dto.id       = item.get("id").getAsInt();
                            if (item.has("name")     && item.get("name").isJsonPrimitive())     dto.name     = item.get("name").getAsString();
                            if (item.has("location") && item.get("location").isJsonPrimitive()) dto.location = item.get("location").getAsString();
                        }

                        // roomsCount (nếu có)
                        int roomsCount = 0;
                        if (item.has("rooms") && item.get("rooms").isJsonObject()) {
                            JsonObject roomsObj = item.getAsJsonObject("rooms");
                            if (roomsObj.has("$values") && roomsObj.get("$values").isJsonArray()) {
                                roomsCount = roomsObj.getAsJsonArray("$values").size();
                            }
                        }
                        dto.roomsCount = roomsCount;

                        out.houses.add(dto);
                    }
                }
            }
            return out;
        }
    }
}
