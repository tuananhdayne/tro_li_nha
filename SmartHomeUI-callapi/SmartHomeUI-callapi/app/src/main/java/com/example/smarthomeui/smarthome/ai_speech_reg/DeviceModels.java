package com.example.smarthomeui.smarthome.ai_speech_reg;

import java.io.Serializable;

public class DeviceModels {

    public enum Action implements Serializable {
        TURN_ON, TURN_OFF, INCREASE, DECREASE, SET, UNKNOWN
    }

    /** Kết quả NLU (giữ UNKNOWN/-1 nếu mơ hồ) */
    public static class ParseResult implements Serializable {
        public Action  action    = Action.UNKNOWN;
        public String  deviceId  = "UNKNOWN";      // "UNKNOWN" khi chưa rõ
        public String  deviceName= "UNKNOWN";
        public String  deviceType= "UNKNOWN";      // <== thêm trường này
        public String  room      = "UNKNOWN";
        public Integer value     = -1;             // -1 khi không rõ
        public int[]   colorRgb;                   // chỉ dùng cho RGB
        public int     deviceOrdinal;
        public String  raw;

        public boolean isDeviceKnown() { return deviceId != null && !"UNKNOWN".equals(deviceId); }
        public boolean isActionKnown() { return action != Action.UNKNOWN; }
        public boolean isValueKnown()  { return value != null && value >= 0; }
        public boolean isRoomKnown()   { return room != null && !"UNKNOWN".equals(room); }
        public boolean isRgbAction()   { return deviceType != null && deviceType.toLowerCase().contains("rgb"); }

        @Override public String toString() {
            return "action=" + action +
                    ", deviceId=" + deviceId +
                    ", deviceName=" + deviceName +
                    ", deviceType=" + deviceType +
                    ", room=" + room +
                    ", value=" + value;
        }
    }

}