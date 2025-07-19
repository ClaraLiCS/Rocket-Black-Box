/*
* ---------------------------------------------------------------------------------------
* Project Name: Rocket Black Box
* Author: [clara li, codrin and conor ]
* Date: [08/07/2025]
* ---------------------------------------------------------------------------------------
*/
/*
* ---------------------------------------------------------------------------------------
* Libraries
* ---------------------------------------------------------------------------------------
*/
// Add or remove as needed
#include <Arduino.h>
#include <Wire.h>
#include <SPI.h>
#include <Adafruit_Sensor.h>
#include <Adafruit_NeoPixel.h> // RGB_LED
#include <Adafruit_GFX.h> // Display
#include <Adafruit_ST7789.h> // Display
#include <Adafruit_BMP280.h> // BMP
#include <SensorQMI8658.hpp> // QMI
//#include <MPU6050_light.h>
#include <SD.h>
#include <vector>
#include <BLEDevice.h>
#include <BLEServer.h>
#include <BLEUtils.h>
#include <BLE2902.h>

BLECharacteristic *pCharacteristic;
bool deviceConnected = false;

#define SERVICE_UUID        "6e400001-b5a3-f393-e0a9-e50e24dcca9e"
#define CHARACTERISTIC_UUID "6e400003-b5a3-f393-e0a9-e50e24dcca9e"
/*
* ---------------------------------------------------------------------------------------
* Pin Definitions
* ---------------------------------------------------------------------------------------
*/
// RGB LED
// (GPIO33 conflicts with QMI8658C INT pin, do not use parallel)
#define LED_PIN 33
#define NUM_LEDS 1
// TFT Display Pins
#define TFT_CS 7
#define TFT_DC 39
#define TFT_RST 40
#define TFT_backlight 45
// SPI Pins
#define SPI_SCK 36
#define SPI_MISO 37
#define SPI_MOSI 35
// I2C Pins for Sensors
#define I2C_SDA 42
#define I2C_SCL 41
// Sensor Address
#define BMP_Addr 0x77
#define QMI_Addr 0x6B
//SD card
#define SD_CS 10           // Adjust this to your CS pin (often GPIO5 or GPIO4)

/*
* ---------------------------------------------------------------------------------------
* Global Objects/Varibles
* ---------------------------------------------------------------------------------------
*/
// TFT-Display
Adafruit_ST7789 tft = Adafruit_ST7789(TFT_CS, TFT_DC, TFT_RST);
// RGB LED
Adafruit_NeoPixel RGB_LED = Adafruit_NeoPixel(NUM_LEDS, LED_PIN, NEO_GRB + NEO_KHZ800);
// BMP Sensor (Temperature and Pressure Sensor)
Adafruit_BMP280 bmp;
double temp, pres;
// QMI Sensor (6-axis Gyroscope &amp; Accelerometer)
SensorQMI8658 qmi;
// Data structures for QMI Sensor readings
IMUdata acc;
IMUdata gyr;
float accX, accY, accZ;
float gyroX, gyroY, gyroZ;
// Example Variables
int randomCounter = 0;
int randomTime = 500;

File dataFile;
boolean recordData;
// ==== Timing ====
unsigned long prevMillis = 0;
const long interval = 100; // Log every 100ms
// ==== Motion ====
float velocityZ = 0;
unsigned long lastTime = 0;

std::vector<String> data;

/*
* ---------------------------------------------------------------------------------------
* Function Prototypes
* ---------------------------------------------------------------------------------------
*/
void initDisplay();
void initBMP();
void initQMI();
void initSD();
void initBLE();


class MyServerCallbacks: public BLEServerCallbacks {
  void onConnect(BLEServer* pServer) {
    deviceConnected = true;
    recordData = false;
    Serial.println("BLE connected!");

    // Enviar todos los datos almacenados
    for (String &line : data) {
      pCharacteristic->setValue(line.c_str());
      pCharacteristic->notify();
      Serial.println("Sending data...");
      delay(50);  // Pequeña pausa para evitar saturar el canal BLE
    }

    Serial.println("All stored data sent.");
  }

  void onDisconnect(BLEServer* pServer) {
    deviceConnected = false;
    Serial.println("BLE disconnected.");
  }
};

/*
* ---------------------------------------------------------------------------------------
* Setup Function — runs once at startup
* @ brief Example Setup Code. Add and adjust Code as needed:
* ---------------------------------------------------------------------------------------
*/
void setup() {
  // Start serial interface
  Serial.begin(115200);
  // write on serial interface
  Serial.println("Starting...");
  // Start SPI and I2C communication
  SPI.begin(36, 37, 35, 10);
  initSD();

  Wire.begin(I2C_SDA,I2C_SCL);
  // Initialize TFT Display
  initDisplay();
  // write on TFT Display
  // Cursor position (x,y)
  tft.setCursor(0, 0);
  tft.print("Starting...");
  // Initialize BMP280
  initBMP();
  // Initialize QMI8658C
  initQMI();
  Serial.println("Setup finished");
  //Initialize Bloetooth
  //initBLE();
  // Wait for 1000ms
  delay(30000);
  recordData = true;
}
/*
* ---------------------------------------------------------------------------------------
* Loop Function — runs repeatedly
* @brief Example Code. Put your own Code here:
* ---------------------------------------------------------------------------------------
*/
void loop() {
  if(recordData){
    // Refresh Display
    tft.fillScreen(ST77XX_BLACK);
    tft.setCursor(0,0);
    pres = bmp.readPressure();
    tft.printf("Pressure: %.2f", pres);
    tft.setCursor(0,20);
    temp = bmp.readTemperature();
    tft.printf("Temperature: %.2f", temp);
    tft.setCursor(0,40);
    qmi.getAccelerometer(accX, accY, accZ);
    qmi.getGyroscope(gyroX, gyroY, gyroZ);
    tft.printf("Acc X: %.2f\nAcc Y: %.2f\nAcc Z: %.2f\n", accX, accY, accZ);
    tft.printf("Gyro X: %.2f\nGyro Y: %.2f\nGyro Z: %.2f", gyroX, gyroY, gyroZ);
    
    String dataLine = String(millis()) + " " + String(pres, 2) + " " + String(temp, 2) + " " +
                      String(accX, 2) + " " + String(accY, 2) + " " + String(accZ, 2) + " " +
                      String(gyroX, 2) + " " + String(gyroY, 2) + " " + String(gyroZ, 2);
    
    //Guardar en memoria
    //data.push_back(dataLine);
    Serial.println(dataLine);
    // Guardar en archivo
    dataFile = SD.open("/rocket_log.csv", FILE_APPEND);
    if (dataFile) {
      dataFile.println(dataLine);
      dataFile.close();
    } else {
      Serial.println("Error al escribir archivo");
    }
  }
  delay(100);
}
/*
* ---------------------------------------------------------------------------------------
* Functions
* ---------------------------------------------------------------------------------------
*/
/**
* @brief Initializes the TFT display, sets rotation, background color, text color and
size.
*/
void initDisplay(){
  // Turn on TFT backlight
  pinMode(TFT_backlight, OUTPUT);
  digitalWrite(TFT_backlight, HIGH);
  // Initialize TFT Display
  tft.init(135, 240); // Width x Height
  tft.setRotation(3); // Adjust as needed
  tft.fillScreen(ST77XX_BLACK);
  tft.setTextColor(ST77XX_WHITE);
  tft.setTextSize(2); // Adjust as needed
}
/**
* @brief Initializes the BMP280 sensor and handles failure if not found.
*
* @note Uses I2C address defined by BMP_ADDR.
*/
void initBMP(){
  // Initialize BMP280 Sensor (address is 0x77)
  if (!bmp.begin(BMP_Addr)) {
    // if Sensor could not be found print error
    tft.setCursor(0, 20);
    tft.print("BMP not found");
    Serial.println("BMP not found");
    // endless loop
    while (1);
  }
}
/**
* @brief Initializes the QMI8658C IMU sensor and configures accelerometer and gyroscope.
*
* @note Uses I2C address defined by QMI_ADDR.
*/
void initQMI(){
  // Initialize QMI8658C Sensor (address is 0x6B)
  if (!qmi.begin(Wire,QMI_Addr,I2C_SDA,I2C_SCL)) {
    // if Sensor could not be found print error
    tft.setCursor(0, 20);
    tft.print("QMI not found");
    Serial.println("QMI not found");
    // endless loop
    while (1);
  }
  // Configure QMI sensor
  qmi.enableGyroscope();
  qmi.enableAccelerometer();
  qmi.configAccelerometer(
  SensorQMI8658::ACC_RANGE_4G,
  SensorQMI8658::ACC_ODR_1000Hz,
  SensorQMI8658::LPF_MODE_0);
  qmi.configGyroscope(
  SensorQMI8658::GYR_RANGE_64DPS,
  SensorQMI8658::GYR_ODR_896_8Hz,
  SensorQMI8658::LPF_MODE_3);
}

//SPIClass spiSD = SPIClass(FSPI);
void initSD(){
  //spiSD.begin(SPI_SCK, SPI_MOSI, SPI_MISO, SD_CS);
  if(!SD.begin(SD_CS)){
    Serial.println("SD Card failed!");
    while(1);
  }
  else{
    dataFile = SD.open("/rocket_log.csv", FILE_WRITE);
    if(dataFile){
      dataFile.println("Time Pressure Temperature AccX AccY AccZ GyroX GyroY GyroZ");
      dataFile.close();
    }
    else{
      Serial.println("Error opening file");
    }
  }
}

void initBLE(){
  // BLE Init
  BLEDevice::init("ESP32S3_BLE");
  BLEServer *pServer = BLEDevice::createServer();
  pServer->setCallbacks(new MyServerCallbacks());

  BLEService *pService = pServer->createService(SERVICE_UUID);
  pCharacteristic = pService->createCharacteristic(
    CHARACTERISTIC_UUID,
    BLECharacteristic::PROPERTY_NOTIFY
  );
  pCharacteristic->addDescriptor(new BLE2902());
  pService->start();

  BLEAdvertising *pAdvertising = pServer->getAdvertising();
  pAdvertising->start();
  Serial.println("BLE ready, waiting for connection...");
}

/*
* ---------------------------------------------------------------------------------------
* End of file
* ---------------------------------------------------------------------------------------
*/
