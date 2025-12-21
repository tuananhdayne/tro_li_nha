package com.example.smarthomeui.smarthome.ai_speech_reg;

import com.example.smarthomeui.smarthome.model.Device;
import java.util.*;
import java.util.regex.*;
import static com.example.smarthomeui.smarthome.ai_speech_reg.DeviceModels.*;
import static com.example.smarthomeui.smarthome.ai_speech_reg.StringUtilsVN.fold;

import android.util.Log;

public class VietnameseCommandParser {

    private final DeviceRegistry registry;
    private static final int MIN_DEVICE_SCORE = 6; // <6 → xem như không liên quan

    public VietnameseCommandParser(DeviceRegistry registry) {
        this.registry = registry;
    }

    private static final Map<String, Integer> NUMBER_WORDS = new HashMap<>();
    static {
        String[][] pairs = {
                { "khong", "0" }, { "mot", "1" }, { "một", "1" }, { "nhat", "1" }, { "nhất", "1" },
                { "hai", "2" }, { "ba", "3" }, { "bon", "4" }, { "bốn", "4" }, { "nam", "5" }, { "năm", "5" },
                { "sau", "6" }, { "sáu", "6" }, { "bay", "7" }, { "bảy", "7" }, { "tam", "8" }, { "tám", "8" },
                { "chin", "9" }, { "chín", "9" }, { "muoi", "10" }, { "mười", "10" }
        };
        for (String[] p : pairs)
            NUMBER_WORDS.put(p[0], Integer.parseInt(p[1]));
    }

    private static final Pattern PERCENT = Pattern.compile("(?<!\\d)(\\d{1,3})\\s*[%％](?=\\D|$)");

    private static final Pattern LEVEL = Pattern.compile("\\b(?:muc|mức|cap|cấp|level)\\s*(\\d{1,2})\\b");
    // SỐ THỨ TỰ THIẾT BỊ: (đèn|cửa|khóa|nhiệt độ|chuyển động) (số|so) <num>
    private static final Pattern DEVICE_INDEX = Pattern.compile(
            "\\b(?:den led|den|cua|khóa|khoa|door|lock|nhiet do|nhiet|cam bien|sensor|chuyen dong|nguoi|light)\\s*(?:so|số)\\s*(\\d{1,3})\\b");
    private static final Pattern UPDOWN_ABS = Pattern.compile("\\b(?:len|xuong|den|toi|muc)\\s*(\\d{1,3})(?=\\D|$)");

    private static final LinkedHashMap<String, int[]> BASIC_COLORS = new LinkedHashMap<>();
    static {
        BASIC_COLORS.put("mau do", new int[] { 255, 0, 0 });
        BASIC_COLORS.put("do", new int[] { 255, 0, 0 });

        BASIC_COLORS.put("mau xanh duong", new int[] { 0, 0, 255 });
        BASIC_COLORS.put("xanh duong", new int[] { 0, 0, 255 });

        BASIC_COLORS.put("mau xanh la", new int[] { 0, 255, 0 });
        BASIC_COLORS.put("xanh la", new int[] { 0, 255, 0 });

        BASIC_COLORS.put("mau vang", new int[] { 255, 255, 0 });
        BASIC_COLORS.put("vang", new int[] { 255, 255, 0 });

        BASIC_COLORS.put("mau cam", new int[] { 255, 165, 0 });
        BASIC_COLORS.put("cam", new int[] { 255, 165, 0 });

        BASIC_COLORS.put("mau hong", new int[] { 255, 105, 180 });
        BASIC_COLORS.put("hong", new int[] { 255, 105, 180 });

        BASIC_COLORS.put("mau tim", new int[] { 128, 0, 255 });
        BASIC_COLORS.put("tim", new int[] { 128, 0, 255 });

        BASIC_COLORS.put("mau trang", new int[] { 255, 255, 255 });
        BASIC_COLORS.put("trang", new int[] { 255, 255, 255 });

        BASIC_COLORS.put("mau den", new int[] { 0, 0, 0 });
        BASIC_COLORS.put("den", new int[] { 0, 0, 0 });

        BASIC_COLORS.put("mau xanh ngoc", new int[] { 0, 255, 255 });
        BASIC_COLORS.put("xanh ngoc", new int[] { 0, 255, 255 });
    }
    private final Map<String, Integer> lastColorIndex = new HashMap<>();

    public ParseResult parse(String raw) {
        ParseResult out = new ParseResult();
        out.raw = raw;

        String f = fold(raw);
        // for (char c : f.toCharArray()) {
        // Log.d("FoldDebug", c + " -> " + (int)c);
        // }
        if (f == null)
            f = "";
        f = f.trim();
        if (f.isEmpty())
            return out; // giữ UNKNOWN/-1

        // CẮT CỤM “<thiết bị> số <n>” ĐỂ KHÔNG LẪN VÀO value
        Matcher mDev = DEVICE_INDEX.matcher(f);
        if (mDev.find()) {
            try {
                out.deviceOrdinal = Integer.parseInt(mDev.group(1));
            } catch (Exception ignore) {
            }
            f = mDev.replaceFirst("").trim();
        }

        // Action
        if (f.matches(".*\\b(bat|mo|bật|mở|turn on)\\b.*"))
            out.action = Action.TURN_ON;
        else if (f.matches(".*\\b(tat|dong|tắt|đóng|turn off)\\b.*"))
            out.action = Action.TURN_OFF;
        else if (f.matches(".*\\b(tang|tăng|len|lên|up|increase)\\b.*"))
            out.action = Action.INCREASE;
        else if (f.matches(".*\\b(giam|giảm|xuong|xuống|down|decrease)\\b.*"))
            out.action = Action.DECREASE;
        else if (f.matches(".*\\b(dat|đặt|set|doi|đổi|switch|change)\\b.*"))
            out.action = Action.SET;

        // if ((out.action == Action.INCREASE || out.action == Action.DECREASE) &&
        // out.value >= 0) {
        // out.action = Action.SET;
        // }
        if (f.matches(".*\\b(doi|dat)\\b.*\\bmau\\b.*")) {
            out.action = Action.SET;

            // Tìm xem người dùng nói màu cụ thể nào
            for (Map.Entry<String, int[]> e : BASIC_COLORS.entrySet()) {
                if (f.contains(e.getKey())) {
                    out.colorRgb = e.getValue();
                    break;
                }
            }

            if (out.colorRgb == null && out.deviceId != "UNKNOWN") {
                int nextIndex = getNextColorIndex(out.deviceId);
                int i = 0;
                for (int[] c : BASIC_COLORS.values()) {
                    if (i == nextIndex) {
                        out.colorRgb = c;
                        break;
                    }
                    i++;
                }
            }

            // fallback nếu vẫn null
            if (out.colorRgb == null)
                out.colorRgb = new int[] { 255, 255, 255 };
        } else {
            Integer raw255 = extractValue(f);
            out.value = (raw255 != null) ? raw255 : -1; // -1 để builder hiểu là không có số

        }

        // Room (sửa regex nhóm)
        String room = extractRoom(f);
        out.room = (room != null) ? room : "UNKNOWN";

        // Đánh giá liên quan thiết bị
        int topScore = registry.estimateTopScore(f, out.isRoomKnown() ? out.room : null);
        if (topScore < MIN_DEVICE_SCORE) {
            // Không liên quan: giữ device UNKNOWN/-1
            return out;
        }

        // Ứng viên tốt nhất
        List<DeviceRegistry.CandidateResult> cands = registry.rankCandidates(f, out.isRoomKnown() ? out.room : null, 1);
        if (!cands.isEmpty()) {
            Device d = cands.get(0).device;
            out.deviceId = d.getId();
            out.deviceName = d.getName();
            out.deviceType = d.getType(); // <== thêm dòng này
            if (!out.isRoomKnown())
                out.room = d.getRoom();
        }

        Log.d("VNParse", "fold=" + f);
        Matcher test = PERCENT.matcher(normalizeVi(raw));
        Log.d("VNParse", "percentFound=" + test.find());
        return out;
    }

    // Bỏ dấu tiếng Việt + hạ chữ + xoá ký tự rác sau số (vd: "2'" -> "2")
    private static String normalizeVi(String s) {
        if (s == null)
            return "";
        String noAccent = java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
        String t = noAccent.toLowerCase(java.util.Locale.ROOT);
        // CHỈ xóa ký tự rác sau số, nhưng CHỪA % và ％
        t = t.replaceAll("(\\d)[^\\d\\s%％]+", "$1");
        return t;
    }

    private static Integer parseIntSafe(Matcher m, int group) {
        try {
            if (!m.find())
                return null;
        } catch (Exception e) {
            return null;
        }
        try {
            return Integer.parseInt(m.group(group));
        } catch (Exception e) {
            return null;
        }
    }

    private int getNextColorIndex(String deviceId) {
        int size = BASIC_COLORS.size();
        int current = lastColorIndex.getOrDefault(deviceId, -1);
        int next = (current + 1) % size;
        lastColorIndex.put(deviceId, next);
        return next;
    }

    private Integer extractValue(String raw) {
        String f = normalizeVi(raw);
        // giữ lại số khi có ký tự lạ dính sau: "20%," -> "20%"
        f = f.replaceAll("(\\d)[^\\d\\s%]+", "$1");

        // 1) % (ưu tiên)
        Matcher m = PERCENT.matcher(f);
        Integer pct = parseIntSafe(m, 1);
        if (pct != null)
            return pct;

        // 2) "muc/cap/level <num>" (0..3)
        m = LEVEL.matcher(f);
        Integer lvl = parseIntSafe(m, 1);
        if (lvl != null)
            return lvl;

        // 3) "len/xuong/den/toi <num>" → hiểu là % tuyệt đối
        m = UPDOWN_ABS.matcher(f);
        Integer abs = parseIntSafe(m, 1);
        if (abs != null)
            return abs;

        // 4) Không bắt số rời rạc để tránh dính "đèn số 1"
        return null;
    }

    private String extractRoom(String f0) {
        String f = normalizeVi(f0);
        // sửa nhóm: (phong|room)\s+(ngu|khach|...)
        Matcher mr = Pattern.compile(
                "\\b(?:phong|room)\\s+(ngu|bedroom|khach|livingroom|bep|kitchen|lam viec|hoc|tam|bathroom|wc)\\b")
                .matcher(f);
        if (mr.find()) {
            switch (mr.group(1)) {
                case "ngu":
                case "bedroom":
                    return "phòng ngủ";
                case "khach":
                case "livingroom":
                    return "phòng khách";
                case "bep":
                case "kitchen":
                    return "phòng bếp";
                case "lam viec":
                    return "phòng làm việc";
                case "hoc":
                    return "phòng học";
                case "tam":
                case "bathroom":
                    return "phòng tắm";
                case "wc":
                    return "phòng vệ sinh";
            }
        }
        return null;
    }
}