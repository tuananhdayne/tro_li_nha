export function detectIntent(text: string) {
  const lower = text.toLowerCase();

  // Light control
  if (lower.match(/bật|mở|turn on/gi) && lower.match(/đèn|light/gi))
    return { intent: "light_on", deviceType: "Light" };
  if (lower.match(/tắt|turn off/gi) && lower.match(/đèn|light/gi))
    return { intent: "light_off", deviceType: "Light" };

  // Door lock control
  if (lower.match(/mở|unlock|open/gi) && lower.match(/cửa|khóa|door|lock/gi))
    return { intent: "door_unlock", deviceType: "DoorLock" };
  if (lower.match(/khóa|đóng|lock|close/gi) && lower.match(/cửa|door/gi))
    return { intent: "door_lock", deviceType: "DoorLock" };

  // Temperature & Humidity sensor
  if (lower.match(/nhiệt độ|temperature|temp/gi))
    return { intent: "get_temperature", deviceType: "TemperatureHumiditySensor" };
  if (lower.match(/độ ẩm|humidity/gi))
    return { intent: "get_humidity", deviceType: "TemperatureHumiditySensor" };

  // Motion sensor
  if (lower.match(/có người|chuyển động|motion|người/gi))
    return { intent: "get_motion", deviceType: "MotionSensor" };

  // Info intents
  if (lower.match(/thời tiết|weather/gi))
    return { intent: "weather" };
  if (lower.match(/mấy giờ|giờ là|time/gi))
    return { intent: "time" };
  if (lower.match(/hôm nay ngày|ngày bao nhiêu|date/gi))
    return { intent: "date" };

  return { intent: "chat" };
}
