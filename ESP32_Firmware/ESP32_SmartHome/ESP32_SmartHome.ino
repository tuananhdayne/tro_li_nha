/*
 * ============================================================
 *  ESP32 SmartHome Firmware - Auto Provisioning
 *  Dự án: SmartHome-dev
 *  
 *  ESP32 tự động:
 *  1. Kết nối WiFi
 *  2. Gọi API /api/device/esp32/provision để nhận tokens
 *  3. Lưu tokens vào EEPROM
 *  4. Kết nối ThingsBoard và hoạt động
 * ============================================================
 */

#include <WiFi.h>
#include <HTTPClient.h>
#include <PubSubClient.h>
#include <DHT.h>
#include <ESP32Servo.h>
#include <ArduinoJson.h>
#include <Preferences.h>  // Lưu trữ NVS (thay EEPROM)

// ===============================================================
//                     CẤU HÌNH
// ===============================================================

const char* WIFI_SSID     = "Dung Land 501";
const char* WIFI_PASSWORD = "11116666";

// Server WebApp của bạn (ĐỔI THEO IP/DOMAIN CỦA BẠN)
const char* WEBAPP_SERVER = "http://192.168.61.180:5000";  // <-- ĐỔI IP SERVER

// ThingsBoard
const char* TB_SERVER = "demo.thingsboard.io";
const int   TB_PORT   = 1883;

// ===============================================================
//                     GPIO PINS
// ===============================================================

#define DHT_PIN     19
#define PIR_PIN     16
#define SERVO_PIN   15
#define LED_PIN     23
#define DHT_TYPE    DHT11

// ===============================================================
//                     LEDC PWM
// ===============================================================

#define LED_CHANNEL     0
#define LED_FREQ        5000
#define LED_RESOLUTION  8

// ===============================================================
//                     GLOBAL
// ===============================================================

WiFiClient wifiClient;
HTTPClient http;
PubSubClient mqtt(wifiClient);
DHT dht(DHT_PIN, DHT_TYPE);
Servo servo;
Preferences prefs;

// Device tokens (lưu từ NVS hoặc nhận từ API)
String tokenLight = "";
String tokenDoorLock = "";
String tokenTempSensor = "";
String tokenMotion = "";

bool isProvisioned = false;
bool doorLocked = true;
bool ledOn = false;
int ledDim = 0;

unsigned long lastTelemetry = 0;
const unsigned long TELEMETRY_INTERVAL = 5000;

int currentRpcDevice = 0;
unsigned long lastMqttSwitch = 0;
const unsigned long MQTT_SWITCH_INTERVAL = 2000;

// ===============================================================
//                     WIFI
// ===============================================================

void setupWiFi() {
  Serial.print("[WiFi] Connecting to ");
  Serial.println(WIFI_SSID);
  
  WiFi.begin(WIFI_SSID, WIFI_PASSWORD);
  int attempts = 0;
  while (WiFi.status() != WL_CONNECTED && attempts < 30) {
    delay(500);
    Serial.print(".");
    attempts++;
  }
  
  if (WiFi.status() == WL_CONNECTED) {
    Serial.println("\n[WiFi] Connected! IP: " + WiFi.localIP().toString());
  } else {
    Serial.println("\n[WiFi] Failed! Restarting...");
    ESP.restart();
  }
}

// ===============================================================
//                     LƯU/ĐỌC TOKENS TỪ NVS
// ===============================================================

void saveTokensToNVS() {
  prefs.begin("esp32-tokens", false);
  prefs.putString("light", tokenLight);
  prefs.putString("doorlock", tokenDoorLock);
  prefs.putString("temp", tokenTempSensor);
  prefs.putString("motion", tokenMotion);
  prefs.putBool("provisioned", true);
  prefs.end();
  Serial.println("[NVS] Tokens saved");
}

bool loadTokensFromNVS() {
  prefs.begin("esp32-tokens", true);
  isProvisioned = prefs.getBool("provisioned", false);
  
  if (isProvisioned) {
    tokenLight = prefs.getString("light", "");
    tokenDoorLock = prefs.getString("doorlock", "");
    tokenTempSensor = prefs.getString("temp", "");
    tokenMotion = prefs.getString("motion", "");
    prefs.end();
    
    Serial.println("[NVS] Tokens loaded:");
    Serial.println("  Light: " + tokenLight);
    Serial.println("  DoorLock: " + tokenDoorLock);
    Serial.println("  TempSensor: " + tokenTempSensor);
    Serial.println("  Motion: " + tokenMotion);
    return true;
  }
  
  prefs.end();
  return false;
}

void clearNVS() {
  prefs.begin("esp32-tokens", false);
  prefs.clear();
  prefs.end();
  Serial.println("[NVS] Cleared");
}

// ===============================================================
//                     AUTO PROVISIONING
// ===============================================================

bool provisionFromServer() {
  if (WiFi.status() != WL_CONNECTED) return false;

  // Lấy MAC address
  String macAddress = WiFi.macAddress();
  macAddress.replace(":", "");  // Bỏ dấu : -> VD: AABBCCDDEEFF
  
  Serial.println("[Provision] MAC: " + macAddress);
  Serial.println("[Provision] Calling API...");

  // Gọi API provisioning
  String url = String(WEBAPP_SERVER) + "/api/device/esp32/provision";
  
  http.begin(url);
  http.addHeader("Content-Type", "application/json");
  
  // Tạo request body
  StaticJsonDocument<200> reqDoc;
  reqDoc["macAddress"] = macAddress;
  reqDoc["firmwareVersion"] = "1.0";
  String requestBody;
  serializeJson(reqDoc, requestBody);
  
  Serial.println("[Provision] Request: " + requestBody);
  
  int httpCode = http.POST(requestBody);
  
  if (httpCode == 200) {
    String response = http.getString();
    Serial.println("[Provision] Response: " + response);
    
    // Parse response
    StaticJsonDocument<1024> resDoc;
    if (deserializeJson(resDoc, response) == DeserializationError::Ok) {
      if (resDoc["success"].as<bool>()) {
        // Lấy tokens từ response
        JsonArray devices = resDoc["devices"].as<JsonArray>();
        for (JsonObject device : devices) {
          String type = device["type"].as<String>();
          String token = device["deviceToken"].as<String>();
          
          if (type == "Light") tokenLight = token;
          else if (type == "DoorLock") tokenDoorLock = token;
          else if (type == "TemperatureHumiditySensor") tokenTempSensor = token;
          else if (type == "MotionSensor") tokenMotion = token;
        }
        
        // Lưu vào NVS
        saveTokensToNVS();
        isProvisioned = true;
        
        Serial.println("[Provision] SUCCESS!");
        http.end();
        return true;
      }
    }
  } else {
    Serial.printf("[Provision] HTTP Error: %d\n", httpCode);
  }
  
  http.end();
  return false;
}

// ===============================================================
//                     ĐIỀU KHIỂN LED
// ===============================================================

void setLed(bool on) {
  ledOn = on;
  ledcWrite(LED_CHANNEL, on ? 255 : 0);
  ledDim = on ? 100 : 0;
  Serial.printf("[LED] %s\n", on ? "ON" : "OFF");
}

void setLedDim(int value) {
  value = constrain(value, 0, 255);
  ledcWrite(LED_CHANNEL, value);
  ledDim = map(value, 0, 255, 0, 100);
  ledOn = (value > 0);
  Serial.printf("[LED] Dim: %d%%\n", ledDim);
}

// ===============================================================
//                     ĐIỀU KHIỂN SERVO
// ===============================================================

void setDoorLock(bool lock) {
  doorLocked = lock;
  servo.write(lock ? 0 : 90);
  Serial.printf("[Door] %s\n", lock ? "LOCKED" : "UNLOCKED");
}

// ===============================================================
//              GỬI TELEMETRY QUA HTTP
// ===============================================================

void sendHttpTelemetry(const String& token, const String& payload) {
  if (WiFi.status() != WL_CONNECTED || token.isEmpty()) return;
  
  String url = "http://" + String(TB_SERVER) + "/api/v1/" + token + "/telemetry";
  
  http.begin(url);
  http.addHeader("Content-Type", "application/json");
  
  int httpCode = http.POST(payload);
  if (httpCode > 0) {
    Serial.printf("[HTTP] Sent to %s\n", token.c_str());
  }
  http.end();
}

void sendAllTelemetry() {
  // Light
  StaticJsonDocument<128> lightDoc;
  lightDoc["ledStatus"] = ledOn;
  lightDoc["ledDim"] = ledDim;
  String lightPayload;
  serializeJson(lightDoc, lightPayload);
  sendHttpTelemetry(tokenLight, lightPayload);
  
  // DoorLock
  StaticJsonDocument<128> doorDoc;
  doorDoc["doorLocked"] = doorLocked;
  doorDoc["isLocked"] = doorLocked;
  String doorPayload;
  serializeJson(doorDoc, doorPayload);
  sendHttpTelemetry(tokenDoorLock, doorPayload);
  
  // TempSensor
  float temp = dht.readTemperature();
  float hum = dht.readHumidity();
  StaticJsonDocument<128> tempDoc;
  tempDoc["temperature"] = isnan(temp) ? -999 : temp;
  tempDoc["humidity"] = isnan(hum) ? -999 : hum;
  String tempPayload;
  serializeJson(tempDoc, tempPayload);
  sendHttpTelemetry(tokenTempSensor, tempPayload);
  
  // Motion
  StaticJsonDocument<128> motionDoc;
  motionDoc["motion"] = (bool)digitalRead(PIR_PIN);
  String motionPayload;
  serializeJson(motionDoc, motionPayload);
  sendHttpTelemetry(tokenMotion, motionPayload);
  
  Serial.println("[Telemetry] Sent");
}

// ===============================================================
//                     XỬ LÝ RPC
// ===============================================================

void handleRpc(const String& method, JsonVariant params, const String& requestId) {
  Serial.printf("[RPC] Method: %s\n", method.c_str());
  
  StaticJsonDocument<128> res;

  if (method == "setLedStatus") {
    setLed(params.as<int>() == 1);
    res["ledStatus"] = ledOn;
  }
  else if (method == "setLedDim") {
    setLedDim(params.as<int>());
    res["ledDim"] = ledDim;
  }
  else if (method == "lock") {
    setDoorLock(true);
    res["isLocked"] = true;
  }
  else if (method == "unlock") {
    setDoorLock(false);
    res["isLocked"] = false;
  }
  else if (method == "getLockStatus") {
    res["isLocked"] = doorLocked;
  }
  else if (method == "getTemperature") {
    float t = dht.readTemperature();
    res["temperature"] = isnan(t) ? -999 : t;
  }
  else if (method == "getHumidity") {
    float h = dht.readHumidity();
    res["humidity"] = isnan(h) ? -999 : h;
  }
  else if (method == "getSensorData") {
    float t = dht.readTemperature();
    float h = dht.readHumidity();
    res["temperature"] = isnan(t) ? -999 : t;
    res["humidity"] = isnan(h) ? -999 : h;
  }
  else if (method == "getMotionStatus") {
    res["motion"] = (bool)digitalRead(PIR_PIN);
  }
  else {
    res["error"] = "Unknown";
  }

  String topic = "v1/devices/me/rpc/response/" + requestId;
  String payload;
  serializeJson(res, payload);
  mqtt.publish(topic.c_str(), payload.c_str());
}

// ===============================================================
//                     MQTT
// ===============================================================

void mqttCallback(char* topic, byte* payload, unsigned int length) {
  String msg;
  for (unsigned int i = 0; i < length; i++) msg += (char)payload[i];

  String t = String(topic);
  if (t.startsWith("v1/devices/me/rpc/request/")) {
    String requestId = t.substring(t.lastIndexOf('/') + 1);
    
    StaticJsonDocument<256> doc;
    if (deserializeJson(doc, msg)) return;
    
    handleRpc(doc["method"].as<String>(), doc["params"], requestId);
  }
}

String getTokenByIndex(int idx) {
  switch(idx) {
    case 0: return tokenLight;
    case 1: return tokenDoorLock;
    case 2: return tokenTempSensor;
    case 3: return tokenMotion;
    default: return "";
  }
}

void connectMqttDevice(int idx) {
  String token = getTokenByIndex(idx);
  if (token.isEmpty()) return;
  
  if (mqtt.connected()) {
    mqtt.disconnect();
    delay(100);
  }
  
  String clientId = "ESP32_" + String(idx) + "_" + String(millis());
  
  if (mqtt.connect(clientId.c_str(), token.c_str(), NULL)) {
    mqtt.subscribe("v1/devices/me/rpc/request/+");
    currentRpcDevice = idx;
  }
}

void rotateMqttDevice() {
  if (millis() - lastMqttSwitch >= MQTT_SWITCH_INTERVAL) {
    int next = (currentRpcDevice + 1) % 4;
    connectMqttDevice(next);
    lastMqttSwitch = millis();
  }
}

// ===============================================================
//                     SETUP
// ===============================================================

void setup() {
  Serial.begin(115200);
  Serial.println("\n========================================");
  Serial.println("  ESP32 SmartHome - Auto Provisioning");
  Serial.println("========================================");
  
  // GPIO
  pinMode(PIR_PIN, INPUT);
  ledcSetup(LED_CHANNEL, LED_FREQ, LED_RESOLUTION);
  ledcAttachPin(LED_PIN, LED_CHANNEL);
  ledcWrite(LED_CHANNEL, 0);
  dht.begin();
  servo.attach(SERVO_PIN);
  servo.write(0);
  
  // WiFi
  setupWiFi();
  
  // Đọc tokens từ NVS
  if (!loadTokensFromNVS()) {
    Serial.println("[Setup] Not provisioned yet, calling API...");
    
    // Thử provision từ server
    int attempts = 0;
    while (!isProvisioned && attempts < 5) {
      if (provisionFromServer()) {
        break;
      }
      attempts++;
      Serial.printf("[Setup] Retry %d/5...\n", attempts);
      delay(3000);
    }
    
    if (!isProvisioned) {
      Serial.println("[Setup] FAILED to provision! Check server.");
      Serial.println("[Setup] Continuing with empty tokens...");
    }
  }
  
  // MQTT
  mqtt.setServer(TB_SERVER, TB_PORT);
  mqtt.setCallback(mqttCallback);
  mqtt.setBufferSize(512);
  
  if (isProvisioned) {
    connectMqttDevice(0);
  }
  
  Serial.println("[Ready]");
}

// ===============================================================
//                     LOOP
// ===============================================================

void loop() {
  if (!isProvisioned) {
    // Thử provision lại mỗi 30 giây
    static unsigned long lastProvisionAttempt = 0;
    if (millis() - lastProvisionAttempt > 30000) {
      provisionFromServer();
      lastProvisionAttempt = millis();
    }
    delay(1000);
    return;
  }

  // MQTT
  if (!mqtt.connected()) {
    connectMqttDevice(currentRpcDevice);
  }
  mqtt.loop();
  rotateMqttDevice();

  // Telemetry
  if (millis() - lastTelemetry >= TELEMETRY_INTERVAL) {
    sendAllTelemetry();
    lastTelemetry = millis();
  }
}
