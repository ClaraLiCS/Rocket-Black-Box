# Rocket-Black-Box

This project was developed as part of a Blended Intensive Programme (BIP) on Embedded Systems and Model Rocketry, held at Darmstadt University of Applied Sciences (Germany). During this international workshop, students from different countries collaborated to design and build embedded systems for model rockets, culminating in a live rocket launch.

The goal of this project was to create a black box for a model rocket, similar to those used in airplanes. The system records flight data from onboard sensors and stores it on a microSD card, allowing post-flight analysis through a desktop application.

## Features

-  **Microcontroller**: ESP32-S3
-  **BMP280**: Pressure and temperature sensor
-  **QMI8658C**: 6-axis gyroscope and accelerometer
-  **1.14” TFT display**: Real-time data visualization (ST7789)
-  **SD Card logging**: Stores telemetry as CSV
-  **WS2812 RGB LED**: System status indicator
-  **Bluetooth LE**: Sends recorded data to external app
-  **Battery-powered**, designed for onboard rocket use


Sensor data is logged to a CSV file at 10 Hz. The file can later be read and visualized on a computer using a Java application built with NetBeans.

An additional goal was to implement Bluetooth Low Energy (BLE) to store the data temporarily in RAM and transmit it wirelessly after the flight. However, this feature was not fully implemented, so some parts of the code related to BLE remain unused or commented out.

## Authors

- Clara Fangli Caudeli Soriano
- Codrin Radasanu
- Conor Ellis

## License

MIT License
