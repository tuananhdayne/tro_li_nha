import { detectIntent } from "./intent.js";
import { getWeather } from "./weather.js";
import { genAI } from "./gemini.js";

export async function runAssistant(text: string) {
  const { intent, deviceType } = detectIntent(text);

  // Device control intents - return action + deviceType
  if (intent === "light_on") {
    return {
      action: "light_on",
      deviceType: "Light",
      command: { method: "setLedStatus", params: 1 },
      message: "Tôi đã bật đèn cho bạn"
    };
  }

  if (intent === "light_off") {
    return {
      action: "light_off",
      deviceType: "Light",
      command: { method: "setLedStatus", params: 0 },
      message: "Tôi đã tắt đèn"
    };
  }

  if (intent === "door_unlock") {
    return {
      action: "door_unlock",
      deviceType: "DoorLock",
      command: { method: "unlock", params: {} },
      message: "Tôi đã mở khóa cửa cho bạn"
    };
  }

  if (intent === "door_lock") {
    return {
      action: "door_lock",
      deviceType: "DoorLock",
      command: { method: "lock", params: {} },
      message: "Tôi đã khóa cửa"
    };
  }

  if (intent === "get_temperature") {
    return {
      action: "get_temperature",
      deviceType: "TemperatureHumiditySensor",
      command: { method: "getTemperature", params: {} },
      message: "Đang kiểm tra nhiệt độ..."
    };
  }

  if (intent === "get_humidity") {
    return {
      action: "get_humidity",
      deviceType: "TemperatureHumiditySensor",
      command: { method: "getHumidity", params: {} },
      message: "Đang kiểm tra độ ẩm..."
    };
  }

  if (intent === "get_motion") {
    return {
      action: "get_motion",
      deviceType: "MotionSensor",
      command: { method: "getMotionStatus", params: {} },
      message: "Đang kiểm tra cảm biến chuyển động..."
    };
  }

  // Info intents - no device control
  if (intent === "time") {
    const now = new Date();
    return {
      message: `Bây giờ là ${now.getHours()} giờ ${now.getMinutes()} phút.`
    };
  }

  if (intent === "date") {
    const now = new Date();
    return {
      message: `Hôm nay là ngày ${now.getDate()}/${now.getMonth() + 1}/${now.getFullYear()}.`
    };
  }

  if (intent === "weather") {
    return {
      message: await getWeather()
    };
  }

  // Fallback to Gemini for general chat
  try {
    const model = genAI.getGenerativeModel({ model: "gemini-2.0-flash-exp" });
    const result = await model.generateContent(text);
    const response = await result.response;

    return {
      message: response.text() || "Xin lỗi, tôi không thể trả lời câu hỏi này."
    };
  } catch (error: any) {
    if (error.message?.includes("API Key") || error.status === 403) {
      return {
        message: "Lỗi: API Key chưa được thiết lập. Hãy kiểm tra file .env"
      };
    }
    if (error.status === 404) {
      return {
        message: `Lỗi: Model không tìm thấy. ${error.message}`
      };
    }
    throw error;
  }
}
