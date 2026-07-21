# Python BLE GATT Server Simulator for Impact.X Wearable
# Requisitos: pip install bless bleak
# Ejecución: python simulate_wearable.py

import asyncio
import logging
import random
from bless import (
    BlessServer,
    BlessGATTCharacteristic,
    GATTCharacteristicProperties,
    GATTAttributePermissions
)

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger("BLE-Wearable-Simulator")

# UUIDs oficiales correspondientes al código de la app móvil
HEART_RATE_SERVICE_UUID = "0000180d-0000-1000-8000-00805f9b34fb"
HEART_RATE_MEASUREMENT_UUID = "00002a37-0000-1000-8000-00805f9b34fb" # ¡Corregido a 2a37!

BATTERY_SERVICE_UUID = "0000180f-0000-1000-8000-00805f9b34fb"
BATTERY_LEVEL_UUID = "00002a19-0000-1000-8000-00805f9b34fb"

async def run():
    logger.info("Iniciando servidor de simulación BLE en tu Laptop...")
    
    # Nombre del dispositivo que se transmitirá por Bluetooth
    server = BlessServer(name="Galaxy Watch 6 LPT")
    
    # 1. Agregar Servicio de Ritmo Cardíaco (0x180D)
    await server.add_new_service(HEART_RATE_SERVICE_UUID)
    
    # 2. Agregar Característica de Medida de Ritmo Cardíaco (0x2A37)
    # Propiedades: NOTIFY (notificación en vivo)
    await server.add_new_characteristic(
        HEART_RATE_SERVICE_UUID,
        HEART_RATE_MEASUREMENT_UUID,
        GATTCharacteristicProperties.notify,
        None,
        GATTAttributePermissions.readable
    )
    
    # 3. Agregar Servicio de Batería (0x180F)
    await server.add_new_service(BATTERY_SERVICE_UUID)
    
    # 4. Agregar Característica de Nivel de Batería (0x2A19)
    # Propiedades: READ y NOTIFY
    await server.add_new_characteristic(
        BATTERY_SERVICE_UUID,
        BATTERY_LEVEL_UUID,
        GATTCharacteristicProperties.read | GATTCharacteristicProperties.notify,
        bytearray([98]), # Valor inicial 98%
        GATTAttributePermissions.readable
    )
    
    logger.info("Servicios y Características agregados con éxito.")
    
    # Iniciar la transmisión de publicidad (Advertising)
    await server.start()
    logger.info("Transmisión BLE activa como 'Galaxy Watch 6 LPT'.")
    logger.info("Usa la app de Impact.X en tu celular para escanear o conectarte por MAC.")
    
    try:
        current_battery = 98
        while True:
            # Fluctuar ritmo cardíaco aleatoriamente
            heart_rate = random.randint(72, 88)
            
            # Formato estándar de Bluetooth SIG para Heart Rate (Flags en byte 0, BPM en byte 1)
            # Byte 0 = 0x00 (indica formato de 8 bits sin sensor de contacto)
            # Byte 1 = valor del pulso
            hr_payload = bytearray([0x00, heart_rate])
            
            # Actualizar y notificar ritmo cardíaco
            server.get_characteristic(HEART_RATE_MEASUREMENT_UUID).value = hr_payload
            server.update_value(HEART_RATE_SERVICE_UUID, HEART_RATE_MEASUREMENT_UUID)
            logger.info(f"Transmitiendo ritmo cardíaco: {heart_rate} BPM")
            
            # Bajar batería muy despacio para simular consumo
            if random.random() < 0.1 and current_battery > 5:
                current_battery -= 1
                server.get_characteristic(BATTERY_LEVEL_UUID).value = bytearray([current_battery])
                server.update_value(BATTERY_SERVICE_UUID, BATTERY_LEVEL_UUID)
                logger.info(f"Batería actualizada: {current_battery}%")
                
            await asyncio.sleep(2.0)
            
    except asyncio.CancelledError:
        logger.info("Deteniendo el simulador...")
    finally:
        await server.stop()
        logger.info("Servidor BLE detenido.")

if __name__ == "__main__":
    loop = asyncio.get_event_loop()
    try:
        loop.run_until_complete(run())
    except KeyboardInterrupt:
        logger.info("Interrumpido por el usuario.")
