import asyncio
from bleak import BleakClient, BleakScanner

# UUIDs from your ESP32 code
SERVICE_UUID = "6e400001-b5a3-f393-e0a9-e50e24dcca9e"
CHARACTERISTIC_UUID = "6e400003-b5a3-f393-e0a9-e50e24dcca9e"
CSV_FILENAME = "esp32_ble_data.csv"

received_data = []

def handle_notify(_, data):
    line = data.decode('utf-8').strip()
    print(f"🔵 Recibido: {line}")
    received_data.append(line)

async def main():
    print("🔍 Buscando ESP32S3_BLE...")
    device = await BleakScanner.find_device_by_name("ESP32S3_BLE", timeout=10.0)

    if not device:
        print("❌ No se encontró el ESP32S3_BLE.")
        return

    async with BleakClient(device) as client:
        print("✅ Conectado a ESP32 BLE.")
        await client.start_notify(CHARACTERISTIC_UUID, handle_notify)

        print("⏳ Esperando datos... (Ctrl+C para salir)")
        try:
            while True:
                await asyncio.sleep(0.1)
        except KeyboardInterrupt:
            print("\n⏹️ Deteniendo...")
            await client.stop_notify(CHARACTERISTIC_UUID)

    # Guardar CSV
    with open(CSV_FILENAME, "w") as f:
        for line in received_data:
            f.write(line + "\n")
    print(f"💾 Datos guardados en {CSV_FILENAME}")

if __name__ == "__main__":
    asyncio.run(main())
