#include <Arduino.h>
#include <DHT.h>
#include <BLEDevice.h>
#include <BLEServer.h>
#include <BLEUtils.h>
#include <BLE2902.h>

#define DHTTYPE DHT22
#define LIGHT_PIN 34
#define DHT_PIN 23

DHT dht(DHT_PIN, DHTTYPE);
const String deviceUuid = "123e4567-e89b-12d3-a456-426614174000";

const char* SERVICE_UUID = "4fafc201-1fb5-459e-8fcc-c5c9c5c9c5c9";
const char* CHARACTERISTIC_UUID = "beb5483e-36e1-4688-b7f5-e0b3c3f4e4c5";

BLECharacteristic* pCharacteristic = nullptr;
bool deviceConnected = false;

class MyServerCallbacks : public BLEServerCallbacks {
  void onConnect(BLEServer* pServer) override {
    deviceConnected = true;
    Serial.println("BLE client connected");
  }

  void onDisconnect(BLEServer* pServer) override {
    deviceConnected = false;
    pServer->startAdvertising();
    Serial.println("BLE client disconnected");
  }
};

// Median of 15 samples: rejects ADC spikes/saturation glitches
float readLight() {
  int s[15];
  for (int i = 0; i < 15; i++) { s[i] = analogRead(LIGHT_PIN); delayMicroseconds(200); }
  for (int i = 1; i < 15; i++) {
    int v = s[i], j = i - 1;
    while (j >= 0 && s[j] > v) { s[j + 1] = s[j]; j--; }
    s[j + 1] = v;
  }
  return s[7];
}

void sendSensorData() {
  if (!deviceConnected || pCharacteristic == nullptr) {
    return;
  }

  float humidity = dht.readHumidity();
  float temperature = dht.readTemperature();
  float light = readLight();

  String payload = String(temperature, 1) + "," + String(humidity, 1) + "," + String(light, 0);

  pCharacteristic->setValue(payload.c_str());
  pCharacteristic->notify();
}

void setup() {
  Serial.begin(115200);
  dht.begin();
  pinMode(LIGHT_PIN, INPUT);

  BLEDevice::init("ESP32_SENSOR");
  BLEServer* pServer = BLEDevice::createServer();
  pServer->setCallbacks(new MyServerCallbacks());

  BLEService* pService = pServer->createService(SERVICE_UUID);
  pCharacteristic = pService->createCharacteristic(
      CHARACTERISTIC_UUID,
      BLECharacteristic::PROPERTY_READ | BLECharacteristic::PROPERTY_NOTIFY);

  pCharacteristic->addDescriptor(new BLE2902());
  pCharacteristic->setValue("ESP32 sensor ready");
  pService->start();

  BLEAdvertising* pAdvertising = BLEDevice::getAdvertising();
  pAdvertising->addServiceUUID(SERVICE_UUID);
  pAdvertising->setScanResponse(true);
  pAdvertising->setMinPreferred(0x06);
  pAdvertising->setMinPreferred(0x12);
  BLEDevice::startAdvertising();

  Serial.println("BLE ready. Scan for ESP32_SENSOR.");
  Serial.print("Device UUID: ");
  Serial.println(deviceUuid);
}

void loop() {
  delay(1000);

  float humidity = dht.readHumidity();
  float temperature = dht.readTemperature();
  float light = readLight();

  Serial.printf("Humidity: %.2f%%\n", humidity);
  Serial.printf("Temperature: %.2f°C\n", temperature);
  Serial.printf("Light: %.2f\n", light);

  if (deviceConnected && pCharacteristic != nullptr) {
    String payload = String(temperature, 1) + "," + String(humidity, 1) + "," + String(light, 0);

    pCharacteristic->setValue(payload.c_str());
    pCharacteristic->notify();
  }
}
