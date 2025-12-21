package com.example.smarthomeui.smarthome.ai_speech_reg;

import java.text.Normalizer;
import java.util.Locale;

public class StringUtilsVN {
    public static String fold(String s) {
        if (s == null) return "";

        // 1️⃣ Thay thế thủ công các ký tự tiếng Việt đặc biệt
        String n = s;
        n = n.replaceAll("[áàảãạăắằẳẵặâấầẩẫậ]", "a");
        n = n.replaceAll("[ÁÀẢÃẠĂẮẰẲẴẶÂẤẦẨẪẬ]", "A");
        n = n.replaceAll("[éèẻẽẹêếềểễệ]", "e");
        n = n.replaceAll("[ÉÈẺẼẸÊẾỀỂỄỆ]", "E");
        n = n.replaceAll("[íìỉĩị]", "i");
        n = n.replaceAll("[ÍÌỈĨỊ]", "I");
        n = n.replaceAll("[óòỏõọôốồổỗộơớờởỡợ]", "o");
        n = n.replaceAll("[ÓÒỎÕỌÔỐỒỔỖỘƠỚỜỞỠỢ]", "O");
        n = n.replaceAll("[úùủũụưứừửữự]", "u");
        n = n.replaceAll("[ÚÙỦŨỤƯỨỪỬỮỰ]", "U");
        n = n.replaceAll("[ýỳỷỹỵ]", "y");
        n = n.replaceAll("[ÝỲỶỸỴ]", "Y");
        n = n.replaceAll("[đ]", "d");
        n = n.replaceAll("[Đ]", "D");

        // 2️⃣ Bỏ các ký tự tổ hợp Unicode còn sót
        n = Normalizer.normalize(n, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");

        // 3️⃣ Chuyển thường
        n = n.toLowerCase(Locale.ROOT);

        // 4️⃣ Giữ lại các ký tự quan trọng, loại bỏ ký tự rác
        n = n.replaceAll("[^a-z0-9%.,:/\\-\\s]", " ");

        // 5️⃣ Gom khoảng trắng
        n = n.replaceAll("\\s+", " ").trim();

        return n;
    }
}

