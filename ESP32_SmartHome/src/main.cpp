/*
 * ============================================================
 *  ESP32 SmartHome Firmware (Tối ưu cho người mới bắt đầu)
 *  Dự án: SmartHome-dev
 *  
 *  TÍNH NĂNG CHÍNH:
 *  1. Tự động kết nối WiFi.
 *  2. Tự động lấy "Device Tokens" từ WebApp (không cần copy bằng tay).
 *  3. Điều khiển 4 thiết bị qua MQTT (ThingsBoard).
 *  4. Gửi dữ liệu cảm biến định kỳ.
 * ============================================================
 */

#include <WiFi.h>
#include <HTTPClient.h>
#include <PubSubClient.h>
#include <DHT.h>
#include <ESP32Servo.h>
#include <ArduinoJson.h>

// ===============================================================
//                     1. CẤU HÌNH WIFI (BẮT BUỘC)
// ===============================================================
const char* WIFI_SSID     = "Dung Land 501";      // <-- ĐỔI TÊN WIFI CỦA BẠN
const char* WIFI_PASSWORD = "11116666";            // <-- ĐỔI MẬT KHẨU WIFI

// ===============================================================
//                     2. CẤU HÌNH WEBAPP (BẮT BUỘC)
// ===============================================================
// Thay địa chỉ IP dưới đây bằng IP máy tính chạy WebApp của bạn
// Bạn có thể xem IP bằng cách gõ 'ipconfig' trong CMD trên Windows
const char* WEB_SERVER_URL = "http://192.168.61.2:5000"; 

// ===============================================================
//                     3. CẤU HÌNH THINGSBOARD
// ===============================================================
const char* TB_SERVER = "demo.thingsboard.io";
const int   TB_PORT   = 1883;

// ===============================================================
//          DỮ LIỆU TOKEN (SẼ ĐƯỢC TỰ ĐỘNG CẬP NHẬT)
// ===============================================================
String tokenLight    = "";
String tokenDoorLock = "";
String tokenTemp     = "";
String tokenMotion   = "";

// ===============================================================
//                     4. CẤU HÌNH PHẦN CỨNG (GPIO)
// ===============================================================
#define DHT_PIN     18
#define PIR_PIN     4
#define SERVO_PIN   17
#define LED_PIN     2
#define DHT_TYPE    DHT11

#define LED_CHANNEL     0
#define LED_FREQ        5000
#define LED_RESOLUTION  8

// ĐỔI SANG true NẾU BẠN DÙNG RELAY KÍCH MỨC THẤP (LOW)
const bool LED_ACTIVE_LOW = false; 
const bool SERVO_INVERTED = false;
const int  INTERNAL_LED   = 2;    // Chân D2 (trùng với đèn xanh hệ thống)

// ===============================================================
//                     BIẾN TOÀN CỤC
// ===============================================================
WiFiClient wifiClient;
PubSubClient mqtt(wifiClient);
DHT dht(DHT_PIN, DHT_TYPE);
Servo servo;

bool doorLocked = true;
bool ledOn = false;
int ledDim = 0;

unsigned long lastTelemetry = 0;
const unsigned long TELEMETRY_INTERVAL = 5000; // Gửi 1 thiết bị mỗi 5 giây để không làm nghẽn lệnh điều khiển
int telemetryStep = 0; 

int currentRpcDevice = 0;
unsigned long lastMqttSwitch = 0;
const unsigned long MQTT_SWITCH_INTERVAL = 2000;

// ===============================================================
//                     KẾT NỐI WIFI
// ===============================================================
void setupWiFi() {
  Serial.printf("\n[WiFi] Đang kết nối tới: %s\n", WIFI_SSID);
  WiFi.begin(WIFI_SSID, WIFI_PASSWORD);
  
  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
    Serial.print(".");
  }
  
  Serial.println("\n[WiFi] Đã kết nối!");
  Serial.print("[WiFi] Địa chỉ IP: ");
  Serial.println(WiFi.localIP());
  Serial.print("[WiFi] Địa chỉ MAC: ");
  Serial.println(WiFi.macAddress());
}

// ===============================================================
//              TỰ ĐỘNG LẤY CẤU HÌNH (PROVISIONING)
// ===============================================================
bool provisionDevice() {
  if (WiFi.status() != WL_CONNECTED) return false;
  
  Serial.println("\n[Hệ thống] Đang kiểm tra thiết bị trên WebApp...");
  String url = String(WEB_SERVER_URL) + "/api/device/esp32/provision";
  
  HTTPClient http;
  http.begin(url);
  http.addHeader("Content-Type", "application/json");
  
  JsonDocument req;
  req["macAddress"] = WiFi.macAddress();
  String body;
  serializeJson(req, body);
  
  int httpCode = http.POST(body);
  if (httpCode == 200) {
    String payload = http.getString();
    Serial.println("[Hệ thống] Đã nhận phản hồi từ server. Đang giải mã...");
    // Serial.println(payload); // Bỏ comment để xem toàn bộ JSON nếu cần

    JsonDocument doc;
    DeserializationError error = deserializeJson(doc, payload);
    if (error) {
      Serial.print("[Hệ thống] Lỗi giải mã JSON: ");
      Serial.println(error.c_str());
      http.end();
      return false;
    }
    
    // Thử cả "devices" và "Devices" (hỗ trợ cả 2 chuẩn đặt tên)
    JsonArray devices = doc["devices"];
    if (devices.isNull()) devices = doc["Devices"];

    if (devices.isNull()) {
      Serial.println("[Hệ thống] Lỗi: Không tìm thấy danh sách 'devices' trong phản hồi.");
      http.end();
      return false;
    }

    int count = 0;
    for (JsonObject dev : devices) {
      String type = dev["type"].as<String>();
      if (type == "" || type == "null") type = dev["Type"].as<String>();
      
      String token = dev["deviceToken"].as<String>();
      if (token == "" || token == "null") token = dev["DeviceToken"].as<String>();
      
      if (type == "Light") { tokenLight = token; count++; }
      else if (type == "DoorLock") { tokenDoorLock = token; count++; }
      else if (type == "TemperatureHumiditySensor") { tokenTemp = token; count++; }
      else if (type == "MotionSensor") { tokenMotion = token; count++; }
    }
    
    if (count > 0) {
      Serial.printf("[Hệ thống] Thành công! Đã nhận được %d Token.\n", count);
      Serial.print("[Hệ thống] Token Đèn: "); Serial.println(tokenLight);
      Serial.print("[Hệ thống] Token Khóa: "); Serial.println(tokenDoorLock);
      Serial.print("[Hệ thống] Token Nhiệt độ: "); Serial.println(tokenTemp);
      Serial.print("[Hệ thống] Token Cđộng: "); Serial.println(tokenMotion);
      http.end();
      return true;
    } else {
      Serial.println("[Hệ thống] Lỗi: Server trả về danh sách rỗng.");
      http.end();
      return false;
    }
  } else {
    Serial.printf("[Hệ thống] Lỗi (%d): Không thể lấy token. Kiểm tra IP server.\n", httpCode);
    http.end();
    return false;
  }
}

// ===============================================================
//                     ĐIỀU KHIỂN THIẾT BỊ
// ===============================================================
void setLed(bool on) {
  ledOn = on;
  int level = on ? HIGH : LOW;
  // Nếu là Active Low, ta đảo ngược mức điện: ON -> LOW, OFF -> HIGH
  if (LED_ACTIVE_LOW) level = (level == HIGH) ? LOW : HIGH;
  
  digitalWrite(LED_PIN, level);
  
  ledDim = on ? 100 : 0;
  Serial.printf("[Thiết bị] Đèn: %s (Mức GPIO: %s)\n", 
                on ? "BẬT" : "TẮT", 
                (level == HIGH) ? "HIGH" : "LOW");
}

void setLedDim(int value) {
  // Vì đã bỏ PWM để ưu tiên tính ổn định, ta sẽ coi Dim > 0 là Bật
  setLed(value > 0);
  Serial.printf("[Thiết bị] Độ sáng (Giả lập): %d%%\n", value);
}

void setDoorLock(bool lock) {
  doorLocked = lock;
  int angle = lock ? 0 : 90;
  if (SERVO_INVERTED) angle = 180 - angle;
  
  servo.write(angle);
  Serial.printf("[Thiết bị] Cửa: %s (Góc Servo: %d)\n", 
                lock ? "ĐÃ KHÓA" : "MỞ KHÓA", angle);
}

// ===============================================================
//                     GỬI DỮ LIỆU (XOAY VÒNG - ROUND ROBIN)
// ===============================================================
void sendTelemetry(String token, String payload) {
  if (token.length() == 0) return;
  String url = "http://" + String(TB_SERVER) + "/api/v1/" + token + "/telemetry";
  
  HTTPClient http;
  http.begin(url);
  http.setConnectTimeout(400); // Rút ngắn tối đa để không gây lag
  http.setTimeout(400);
  http.addHeader("Content-Type", "application/json");
  
  int code = http.POST(payload);
  if (code != 200) {
    Serial.printf("[HTTP] Lỗi (%d) Token: %s...\n", code, token.substring(0, 5).c_str());
  } else {
    Serial.printf("[HTTP] Đã gửi dữ liệu cho %s...\n", token.substring(0, 5).c_str());
  }
  http.end();
}

void sendTelemetryStep() {
  switch (telemetryStep) {
    case 0: // Gửi dữ liệu Đèn
      if (tokenLight.length() > 0) 
        sendTelemetry(tokenLight, "{\"ledStatus\":" + String(ledOn ? "true" : "false") + ",\"ledDim\":" + String(ledDim) + "}");
      break;
      
    case 1: // Gửi dữ liệu Khóa
      if (tokenDoorLock.length() > 0)
        sendTelemetry(tokenDoorLock, "{\"isLocked\":" + String(doorLocked ? "true" : "false") + "}");
      break;
      
    case 2: // Gửi dữ liệu Nhiệt độ
      if (tokenTemp.length() > 0) {
        float t = NAN, h = NAN;
        // Thử đọc tối đa 3 lần nếu gặp lỗi
        for (int i = 0; i < 3; i++) {
          t = dht.readTemperature();
          h = dht.readHumidity();
          if (!isnan(t) && t > 1.0) break; // Bỏ qua 0.1C vì đó là lỗi sensor
          delay(500); 
        }

        if (!isnan(t) && t > 1.0) { 
          Serial.printf("[Cảm biến] Nhiệt độ: %.1f C, Độ ẩm: %.1f%%\n", t, h);
          sendTelemetry(tokenTemp, "{\"temperature\":" + String(t, 1) + ",\"humidity\":" + String(h, 1) + "}");
        } else {
          Serial.printf("[Cảm biến] DHT11 báo lỗi hoặc giá trị sai (%.1f C). Kiểm tra dây cắm.\n", t);
        }
      }
      break;
      
    case 3: // Gửi dữ liệu Chuyển động
      if (tokenMotion.length() > 0) {
        bool motion = digitalRead(PIR_PIN);
        Serial.printf("[Cảm biến] Chuyển động: %s\n", motion ? "CÓ" : "KHÔNG");
        sendTelemetry(tokenMotion, "{\"motion\":" + String(motion ? "true" : "false") + "}");
      }
      break;
  }
  
  telemetryStep = (telemetryStep + 1) % 4; // Chuyển sang thiết bị tiếp theo
}

// ===============================================================
//                     XỬ LÝ RPC (LỆNH TỪ WEB/APP)
// ===============================================================
void handleRpc(String method, JsonVariant params, String requestId) {
  JsonDocument res;
  
  if (method == "setLedStatus") { setLed(params.as<int>() == 1); res["ledStatus"] = ledOn; }
  else if (method == "setLedDim") { setLedDim(params.as<int>()); res["ledDim"] = ledDim; }
  else if (method == "lock") { setDoorLock(true); res["isLocked"] = true; }
  else if (method == "unlock") { setDoorLock(false); res["isLocked"] = false; }
  
  String topic = "v1/devices/me/rpc/response/" + requestId;
  String resStr;
  serializeJson(res, resStr);
  mqtt.publish(topic.c_str(), resStr.c_str());
}

void mqttCallback(char* topic, byte* payload, unsigned int length) {
  String p = "";
  for (int i = 0; i < length; i++) p += (char)payload[i];
  String t = String(topic);
  
  if (t.startsWith("v1/devices/me/rpc/request/")) {
    String reqId = t.substring(26);
    JsonDocument doc;
    if (deserializeJson(doc, p) == DeserializationError::Ok) {
      handleRpc(doc["method"].as<String>(), doc["params"], reqId);
    }
  }
}

// ===============================================================
//                     KHỞI TẠO (SETUP)
// ===============================================================
void setup() {
  Serial.begin(115200);
  
  // Cấu hình chân cắm
  // Cấu hình chân cắm
  pinMode(PIR_PIN, INPUT);
  pinMode(LED_PIN, OUTPUT);
  
  Serial.println("[Hệ thống] Đang khởi động cảm biến (chờ 2s)...");
  delay(2000); 
  
  dht.begin();
  
  // Thiết lập Servo với dải xung chuẩn (500us - 2400us) cho SG90/MG90S
  ESP32PWM::allocateTimer(0);
  servo.setPeriodHertz(50);
  servo.attach(SERVO_PIN, 500, 2400); 
  
  setupWiFi(); 
  
  provisionDevice();
  
  setLed(false);
  setDoorLock(true); // Trạng thái mặc định khi khởi động
  
  mqtt.setServer(TB_SERVER, TB_PORT);
  mqtt.setCallback(mqttCallback);
}

// ===============================================================
//                     VÒNG LẶP CHÍNH (LOOP)
// ===============================================================
void loop() {
  // ƯU TIÊN 1: Luôn kiểm tra lệnh từ Web/App trước
  mqtt.loop();

  // Nếu chưa có token, thử lấy lại sau mỗi 15 giây
  static unsigned long lastProv = 0;
  bool noToken = (tokenLight.length() == 0);
  
  if (noToken && millis() - lastProv > 15000) {
    lastProv = millis();
    provisionDevice();
  }
  
  if (noToken) { delay(100); return; }

  // Kết nối MQTT nếu bị ngắt
  if (!mqtt.connected()) {
    Serial.printf("[MQTT] Đang kết nối lại...\n");
    String clientId = "ESP32_" + String(WiFi.macAddress());
    if (mqtt.connect(clientId.c_str(), tokenLight.c_str(), NULL)) {
      mqtt.subscribe("v1/devices/me/rpc/request/+");
    } else {
      delay(500); 
      return;
    }
  }

  // ƯU TIÊN 2: Gửi dữ liệu cảm biến (chỉ thực hiện khi rảnh)
  if (millis() - lastTelemetry > TELEMETRY_INTERVAL) {
    lastTelemetry = millis();
    sendTelemetryStep();
    mqtt.loop(); // Kiểm tra lệnh lần nữa ngay sau khi gửi sensor
  }
  
  delay(10); // Nghỉ một chút để hệ thống ổn định
}
