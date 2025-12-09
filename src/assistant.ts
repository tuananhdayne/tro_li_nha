import { detectIntent } from "./intent.js";
import { getWeather } from "./weather.js";
import { genAI } from "./gemini.js";

export async function runAssistant(text: string) {
  const { intent } = detectIntent(text);

  if (intent === "weather") return await getWeather();

  if (intent === "time") {
    const now = new Date();
    return `Bây giờ là ${now.getHours()} giờ ${now.getMinutes()} phút.`;
  }

  if (intent === "date") {
    const now = new Date();
    return `Hôm nay là ngày ${now.getDate()}/${now.getMonth() + 1}/${now.getFullYear()}.`;
  }
  // const { intent } = await detectIntent(text);

  // Xử lý các intent có action (lệnh) - trả về cả action và message
  if (intent === "light_on") {
    return {
      action: "light_on",
      message: "Tôi đã bật đèn cho bạn  "
    };
  }
  if (intent === "light_off") {
    return {
      action: "light_off",
      message: "Tôi đã tắt đèn  "
    };
  }
  if (intent === "door_open") {
    return {
      action: "door_open",
      message: "Tôi đã mở cửa cho bạn  "
    };
  }
  if (intent === "door_close") {
    return {
      action: "door_close",
      message: "Tôi đã đóng cửa cho bạn  "
    };
  }

  // Xử lý các intent thông tin - chỉ trả về message, không có action
  if (intent === "weather") {
    return {
      message: await getWeather()
    };
  }

  // GEMINI
  try {
    const model = genAI.getGenerativeModel({ model: "gemini-2.5-flash" });
    const result = await model.generateContent(text);
    const response = await result.response;
    
    return response.text() || "Xin lỗi, tôi không thể trả lời câu hỏi này.";
  } catch (error: any) {
    if (error.message?.includes("API Key") || error.status === 403) {
      return "Lỗi: API Key chưa được thiết lập hoặc không hợp lệ. Vui lòng kiểm tra file .env";
    }
    if (error.status === 404) {
      return `Lỗi: Model không tìm thấy. ${error.message}`;
    }
    throw error;
  }
}
