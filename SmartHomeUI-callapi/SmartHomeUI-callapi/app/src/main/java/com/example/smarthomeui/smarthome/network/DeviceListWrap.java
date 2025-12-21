package com.example.smarthomeui.smarthome.network;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.List;

public class DeviceListWrap {
    private List<DeviceDto> devices;
    private int total;
    private int skip;
    private int take;

    public DeviceListWrap() {}

    public DeviceListWrap(List<DeviceDto> devices, int total, int skip, int take) {
        this.devices = devices;
        this.total = total;
        this.skip = skip;
        this.take = take;
    }

    public List<DeviceDto> getDevices() { return devices; }
    public void setDevices(List<DeviceDto> devices) { this.devices = devices; }

    public int getTotal() { return total; }
    public void setTotal(int total) { this.total = total; }

    public int getSkip() { return skip; }
    public void setSkip(int skip) { this.skip = skip; }

    public int getTake() { return take; }
    public void setTake(int take) { this.take = take; }

    public static class Deserializer implements JsonDeserializer<DeviceListWrap> {
        @Override
        public DeviceListWrap deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            JsonObject obj = json.getAsJsonObject();
            DeviceListWrap wrap = new DeviceListWrap();

            if (obj.has("total")) {
                wrap.total = obj.get("total").getAsInt();
            }
            if (obj.has("skip")) {
                wrap.skip = obj.get("skip").getAsInt();
            }
            if (obj.has("take")) {
                wrap.take = obj.get("take").getAsInt();
            }

            if (obj.has("devices")) {
                JsonObject devicesObj = obj.getAsJsonObject("devices");
                if (devicesObj.has("$values")) {
                    JsonArray valuesArray = devicesObj.getAsJsonArray("$values");
                    Type listType = new TypeToken<List<DeviceDto>>(){}.getType();
                    wrap.devices = context.deserialize(valuesArray, listType);
                }
            }

            return wrap;
        }
    }
}
