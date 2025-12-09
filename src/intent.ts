export function detectIntent(text: string) {
  text = text.toLowerCase();

  if (text.includes("bật đèn")) return { intent: "light_on" };
  if (text.includes("tắt đèn")) return { intent: "light_off" };
  if (text.includes("mở cửa")) return { intent: "door_open" };
  if (text.includes("đóng cửa")) return { intent: "door_close" };

  if (text.includes("thời tiết")) return { intent: "weather" };

  if (text.includes("mấy giờ") || text.includes("giờ là"))
    return { intent: "time" };

  if (text.includes("hôm nay ngày") || text.includes("ngày bao nhiêu"))
    return { intent: "date" };

  return { intent: "chat" };
}
