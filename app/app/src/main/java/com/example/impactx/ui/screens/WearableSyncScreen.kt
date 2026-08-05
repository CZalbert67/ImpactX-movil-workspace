package com.example.impactx.ui.screens

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.math.sqrt
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

// GATT Standard UUIDs
val HEART_RATE_SERVICE_UUID: UUID = UUID.fromString("0000180d-0000-1000-8000-00805f9b34fb")
val HEART_RATE_MEASUREMENT_CHAR_UUID: UUID = UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb")
val CLIENT_CHARACTERISTIC_CONFIG_UUID: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
val BATTERY_SERVICE_UUID: UUID = UUID.fromString("0000180f-0000-1000-8000-00805f9b34fb")
val BATTERY_LEVEL_CHAR_UUID: UUID = UUID.fromString("00002a19-0000-1000-8000-00805f9b34fb")

enum class BLEState {
    PERMISSION_REQUEST,
    BLUETOOTH_OFF,
    SCANNING,
    DEVICE_LIST,
    CONNECTING,
    CONNECTED_DASHBOARD
}

object WearableManager {
    var bleState by mutableStateOf(BLEState.PERMISSION_REQUEST)
    var realHeartRate by mutableStateOf(72)
    var realBatteryLevel by mutableStateOf(100)
    var isRealConnection by mutableStateOf(false)
    var connectedDeviceName by mutableStateOf<String?>(null)
    var connectedDeviceAddress by mutableStateOf<String?>(null)
    var activeGatt: BluetoothGatt? = null
    // Trip state managed from watch
    var activeWearTripId by mutableStateOf<String?>(null)
    var lastCrashAlertId by mutableStateOf<String?>(null)
    var triggerEmergencyNav by mutableStateOf(false) // flag to navigate to chat
}

data class BLEDeviceItem(
    val name: String,
    val address: String,
    val rssi: Int,
    val device: BluetoothDevice? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WearableSyncScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Bluetooth Adapter & Manager
    val bluetoothManager = remember { context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager }
    val bluetoothAdapter = remember { bluetoothManager.adapter }

    var bleState by remember {
        object : MutableState<BLEState> {
            override var value: BLEState
                get() = WearableManager.bleState
                set(v) { WearableManager.bleState = v }
            override fun component1() = value
            override fun component2(): (BLEState) -> Unit = { value = it }
        }
    }
    val scannedDevices = remember { mutableStateListOf<BLEDeviceItem>() }
    var selectedDevice by remember { mutableStateOf<BLEDeviceItem?>(null) }
    var connectionProgress by remember { mutableStateOf(0f) }

    // Manual MAC address connection state
    var manualMacInput by remember { mutableStateOf("") }

    // Live telemetry values from watch (or simulated fallback)
    var realHeartRate by remember {
        object : MutableState<Int> {
            override var value: Int
                get() = WearableManager.realHeartRate
                set(v) { WearableManager.realHeartRate = v }
            override fun component1() = value
            override fun component2(): (Int) -> Unit = { value = it }
        }
    }
    var realBatteryLevel by remember {
        object : MutableState<Int> {
            override var value: Int
                get() = WearableManager.realBatteryLevel
                set(v) { WearableManager.realBatteryLevel = v }
            override fun component1() = value
            override fun component2(): (Int) -> Unit = { value = it }
        }
    }
    var isRealConnection by remember {
        object : MutableState<Boolean> {
            override var value: Boolean
                get() = WearableManager.isRealConnection
                set(v) { WearableManager.isRealConnection = v }
            override fun component1() = value
            override fun component2(): (Boolean) -> Unit = { value = it }
        }
    }
    var activeGatt by remember {
        object : MutableState<BluetoothGatt?> {
            override var value: BluetoothGatt?
                get() = WearableManager.activeGatt
                set(v) { WearableManager.activeGatt = v }
            override fun component1() = value
            override fun component2(): (BluetoothGatt?) -> Unit = { value = it }
        }
    }

    // Physical sensor accelerometer readings (phone fallback / active visual)
    val sensorManager = remember { context.getSystemService(Context.SENSOR_SERVICE) as SensorManager }
    val accelerometer = remember { sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) }
    var sensorValues by remember { mutableStateOf(floatArrayOf(0f, 0f, 9.81f)) }

    // Dynamic bio/gyro decoratives
    var liveSpO2 by remember { mutableStateOf(98) }
    var liveTemp by remember { mutableStateOf(36.5f) }
    var gyroValues by remember { mutableStateOf(floatArrayOf(0f, 0f, 0f)) }



    // Helper: list of required permissions
    val requiredPermissions = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT
            )
        } else {
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        }
    }

    // Check permission helper
    fun hasAllPermissions(): Boolean {
        return requiredPermissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun hasBluetoothConnectPermission(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
    }

    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissionsMap ->
        val granted = permissionsMap.values.all { it }
        if (granted) {
            bleState = if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
                BLEState.BLUETOOTH_OFF
            } else {
                BLEState.SCANNING
            }
        }
    }

    // Check permissions on start
    LaunchedEffect(Unit) {
        if (WearableManager.bleState == BLEState.CONNECTED_DASHBOARD) {
            return@LaunchedEffect
        }
        if (hasAllPermissions()) {
            bleState = if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
                BLEState.BLUETOOTH_OFF
            } else {
                BLEState.SCANNING
            }
        } else {
            bleState = BLEState.PERMISSION_REQUEST
        }
    }

    // Accelerometer listener lifecycle (Active in Dashboard/HUD)
    DisposableEffect(bleState) {
        if (bleState == BLEState.CONNECTED_DASHBOARD) {
            val listener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent?) {
                    if (event != null && event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                        sensorValues = event.values.clone()
                    }
                }
                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
            }
            sensorManager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_UI)
            onDispose {
                sensorManager.unregisterListener(listener)
            }
        } else {
            onDispose {}
        }
    }

    // Dynamic bio/gyro updates
    LaunchedEffect(bleState) {
        if (bleState == BLEState.CONNECTED_DASHBOARD) {
            while (true) {
                delay(1200)
                liveSpO2 = (97..99).random()
                liveTemp = 36.4f + (0..3).random() / 10.0f
                gyroValues = floatArrayOf(
                    (-8..8).random() / 10f,
                    (-8..8).random() / 10f,
                    (-8..8).random() / 10f
                )
                // If it is simulated connection, fluctuate HR
                if (!isRealConnection) {
                    realHeartRate = (68..88).random()
                }
            }
        }
    }

    // GATT Connection Callback
    val gattCallback = remember {
        object : BluetoothGattCallback() {
            override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
                        ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                        gatt?.discoverServices()
                    }
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    isRealConnection = false
                    activeGatt = null
                    WearableManager.connectedDeviceName = null
                    WearableManager.connectedDeviceAddress = null
                }
            }

            override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                    ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) return
                if (status == BluetoothGatt.GATT_SUCCESS && gatt != null) {
                    // Try subscribing to Heart Rate Service
                    val hrService = gatt.getService(HEART_RATE_SERVICE_UUID)
                    if (hrService != null) {
                        val hrChar = hrService.getCharacteristic(HEART_RATE_MEASUREMENT_CHAR_UUID)
                        if (hrChar != null) {
                            gatt.setCharacteristicNotification(hrChar, true)
                            val descriptor = hrChar.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG_UUID)
                            if (descriptor != null) {
                                descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                                gatt.writeDescriptor(descriptor)
                            }
                        }
                    }

                    // Try reading Battery Service
                    val batteryService = gatt.getService(BATTERY_SERVICE_UUID)
                    if (batteryService != null) {
                        val batteryChar = batteryService.getCharacteristic(BATTERY_LEVEL_CHAR_UUID)
                        if (batteryChar != null) {
                            gatt.readCharacteristic(batteryChar)
                        }
                    }
                }
            }

            override fun onCharacteristicChanged(
                gatt: BluetoothGatt,
                characteristic: android.bluetooth.BluetoothGattCharacteristic,
                value: ByteArray
            ) {
                if (characteristic.uuid == HEART_RATE_MEASUREMENT_CHAR_UUID) {
                    val flag = value[0].toInt()
                    val hr = if ((flag and 0x01) != 0) {
                        // 16-bit
                        ((value[2].toInt() and 0xFF) shl 8) or (value[1].toInt() and 0xFF)
                    } else {
                        // 8-bit
                        value[1].toInt() and 0xFF
                    }
                    realHeartRate = hr
                }
            }

            @Deprecated("Deprecated in Java")
            override fun onCharacteristicRead(
                gatt: BluetoothGatt?,
                characteristic: android.bluetooth.BluetoothGattCharacteristic?,
                status: Int
            ) {
                if (status == BluetoothGatt.GATT_SUCCESS && characteristic != null) {
                    if (characteristic.uuid == BATTERY_LEVEL_CHAR_UUID) {
                        realBatteryLevel = characteristic.value[0].toInt()
                    }
                }
            }
        }
    }

    // Connect helper
    fun connectToDevice(deviceItem: BLEDeviceItem) {
        bleState = BLEState.CONNECTING
        isRealConnection = deviceItem.device != null

        if (deviceItem.device != null && ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
            activeGatt = deviceItem.device.connectGatt(context, false, gattCallback)
            coroutineScope.launch {
                connectionProgress = 0f
                while (connectionProgress < 1.0f) {
                    delay(50)
                    connectionProgress += 0.05f
                }
                WearableManager.connectedDeviceName = deviceItem.name
                WearableManager.connectedDeviceAddress = deviceItem.address

                // --- Room Linkage & Backend Pairing ---
                var nodeId = "unknown-node"
                try {
                    val nodes = com.google.android.gms.wearable.Wearable.getNodeClient(context).connectedNodes.awaitTask()
                    nodeId = nodes.firstOrNull()?.id ?: "unknown-node"
                } catch (e: Exception) {
                    android.util.Log.e("WearSync", "Error getting connected nodes: ${e.message}")
                }

                val db = com.example.impactx.data.local.AppDatabase.getDatabase(context)
                val api = com.example.impactx.data.remote.ApiClient.getApiService(context)
                var linkedDeviceId: String? = null

                try {
                    val getResp = api.getWearable()
                    if (getResp.isSuccessful && getResp.body() != null) {
                        linkedDeviceId = getResp.body()!!.dispositivoId
                        android.util.Log.i("WearSync", "Wearable ya vinculado en backend: $linkedDeviceId")
                    } else if (getResp.code() == 404) {
                        val address = deviceItem.address
                        val pairReq = com.example.impactx.data.remote.PairWearableRequest(
                            dispositivoId = "GW8-PHYSICAL-$address",
                            nombre = deviceItem.name,
                            modelo = "Galaxy Watch 8",
                            fabricante = "Samsung",
                            plataforma = "WearOS"
                        )
                        val pairResp = api.pairWearable(pairReq)
                        if (pairResp.isSuccessful && pairResp.body() != null) {
                            val token = pairResp.body()!!.token
                            val confirmResp = api.confirmPairWearable(com.example.impactx.data.remote.PairConfirmRequest(token))
                            if (confirmResp.isSuccessful && confirmResp.body() != null) {
                                linkedDeviceId = confirmResp.body()!!.dispositivoId
                                android.util.Log.i("WearSync", "Auto-pairing exitoso: $linkedDeviceId")
                            }
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("WearSync", "Error en conexión con backend para pairing: ${e.message}")
                }

                if (linkedDeviceId != null && nodeId != "unknown-node") {
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                        db.wearableLinkageDao().insertLinkage(
                            com.example.impactx.data.local.WearableLinkageEntity(
                                nodeId,
                                linkedDeviceId,
                                deviceItem.name,
                                "Galaxy Watch 8",
                                "Samsung",
                                "Vinculado",
                                System.currentTimeMillis()
                            )
                        )
                    }
                    android.util.Log.i("WearSync", "Local linkage guardado: nodeId=$nodeId -> backendDeviceId=$linkedDeviceId")
                }

                bleState = BLEState.CONNECTED_DASHBOARD
                Toast.makeText(context, "¡Reloj vinculado con éxito!", Toast.LENGTH_SHORT).show()
                onNavigateBack()
            }
        } else {
            // Simulated connection simulation path
            coroutineScope.launch {
                connectionProgress = 0f
                while (connectionProgress < 1.0f) {
                    delay(30)
                    connectionProgress += 0.03f
                }
                realHeartRate = (70..80).random()
                realBatteryLevel = (85..99).random()
                WearableManager.connectedDeviceName = deviceItem.name
                WearableManager.connectedDeviceAddress = deviceItem.address

                // --- Room Linkage & Backend Pairing (Simulado) ---
                var nodeId = "unknown-node"
                try {
                    val nodes = com.google.android.gms.wearable.Wearable.getNodeClient(context).connectedNodes.awaitTask()
                    nodeId = nodes.firstOrNull()?.id ?: "unknown-node"
                } catch (e: Exception) {
                    android.util.Log.e("WearSync", "Error getting connected nodes: ${e.message}")
                }

                val db = com.example.impactx.data.local.AppDatabase.getDatabase(context)
                val api = com.example.impactx.data.remote.ApiClient.getApiService(context)
                var linkedDeviceId: String? = null

                try {
                    val getResp = api.getWearable()
                    if (getResp.isSuccessful && getResp.body() != null) {
                        linkedDeviceId = getResp.body()!!.dispositivoId
                        android.util.Log.i("WearSync", "Wearable ya vinculado en backend (simulado): $linkedDeviceId")
                    } else if (getResp.code() == 404) {
                        val address = deviceItem.address
                        val pairReq = com.example.impactx.data.remote.PairWearableRequest(
                            dispositivoId = "GW8-PHYSICAL-$address",
                            nombre = deviceItem.name,
                            modelo = "Galaxy Watch 8",
                            fabricante = "Samsung",
                            plataforma = "WearOS"
                        )
                        val pairResp = api.pairWearable(pairReq)
                        if (pairResp.isSuccessful && pairResp.body() != null) {
                            val token = pairResp.body()!!.token
                            val confirmResp = api.confirmPairWearable(com.example.impactx.data.remote.PairConfirmRequest(token))
                            if (confirmResp.isSuccessful && confirmResp.body() != null) {
                                linkedDeviceId = confirmResp.body()!!.dispositivoId
                                android.util.Log.i("WearSync", "Auto-pairing exitoso (simulado): $linkedDeviceId")
                            }
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("WearSync", "Error en conexión con backend para pairing (simulado): ${e.message}")
                }

                if (linkedDeviceId != null && nodeId != "unknown-node") {
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                        db.wearableLinkageDao().insertLinkage(
                            com.example.impactx.data.local.WearableLinkageEntity(
                                nodeId,
                                linkedDeviceId,
                                deviceItem.name,
                                "Galaxy Watch 8",
                                "Samsung",
                                "Vinculado",
                                System.currentTimeMillis()
                            )
                        )
                    }
                    android.util.Log.i("WearSync", "Local linkage guardado (simulado): nodeId=$nodeId -> backendDeviceId=$linkedDeviceId")
                }

                bleState = BLEState.CONNECTED_DASHBOARD
                Toast.makeText(context, "¡Reloj vinculado con éxito (Simulado)!", Toast.LENGTH_SHORT).show()
                onNavigateBack()
            }
        }
    }

    // Connect manually by MAC Address
    fun connectByMac(mac: String) {
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            Toast.makeText(context, "Activa el Bluetooth primero", Toast.LENGTH_LONG).show()
            return
        }
        if (!hasBluetoothConnectPermission()) {
            bleState = BLEState.PERMISSION_REQUEST
            return
        }
        try {
            val cleanMac = mac.trim().uppercase()
            if (cleanMac.length != 17 || !cleanMac.contains(":")) {
                Toast.makeText(context, "El formato de MAC debe ser AA:BB:CC:11:22:33", Toast.LENGTH_LONG).show()
                return
            }
            val device = bluetoothAdapter.getRemoteDevice(cleanMac)
            connectToDevice(BLEDeviceItem(device.name ?: "Reloj por MAC", cleanMac, -50, device))
        } catch (e: Exception) {
            Toast.makeText(context, "Error al conectar MAC: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    // Real BLE Scan Logic + Bonded Devices Extraction
    LaunchedEffect(bleState) {
        if (bleState == BLEState.SCANNING && bluetoothAdapter != null && bluetoothAdapter.isEnabled) {
            scannedDevices.clear()
            
            // Add custom simulated fallback watch so they can test even if Bluetooth has no hardware near
            scannedDevices.add(
                BLEDeviceItem(
                    name = "Samsung Galaxy Watch 6 (Simulado)",
                    address = "AA:BB:CC:DD:EE:FF",
                    rssi = -60
                )
            )

            // EXTRACT BONDED (PAIRED) DEVICES INSTANTLY
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                val bonded = bluetoothAdapter.bondedDevices
                if (!bonded.isNullOrEmpty()) {
                    for (device in bonded) {
                        val name = device.name ?: "Reloj Vinculado"
                        val address = device.address
                        if (scannedDevices.none { it.address == address }) {
                            scannedDevices.add(BLEDeviceItem(name, address, -55, device))
                        }
                    }
                }
            }

            // START ACTIVE BLE DISCOVERY SCAN
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED || Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                val scanner = bluetoothAdapter.bluetoothLeScanner
                if (scanner != null) {
                    val scanCallback = object : ScanCallback() {
                        override fun onScanResult(callbackType: Int, result: ScanResult?) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                                ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) return
                            if (result != null && result.device != null) {
                                val name = result.device.name ?: "Dispositivo desconocido"
                                val address = result.device.address
                                val rssi = result.rssi
                                
                                // Avoid duplicates
                                if (scannedDevices.none { it.address == address }) {
                                    scannedDevices.add(BLEDeviceItem(name, address, rssi, result.device))
                                }
                            }
                        }
                    }

                    scanner.startScan(scanCallback)
                    delay(8000) // Scan for 8 seconds
                    scanner.stopScan(scanCallback)
                    
                    if (bleState == BLEState.SCANNING) {
                        bleState = BLEState.DEVICE_LIST
                    }
                } else {
                    bleState = BLEState.DEVICE_LIST
                }
            } else {
                bleState = BLEState.DEVICE_LIST
            }
        }
    }

    // Clean connection on dispose
    DisposableEffect(Unit) {
        onDispose {
            activeGatt?.disconnect()
            activeGatt?.close()
        }
    }

    // Accelerometer math
    val ax = sensorValues[0] / 9.81f
    val ay = sensorValues[1] / 9.81f
    val az = sensorValues[2] / 9.81f
    val gForce = sqrt(ax * ax + ay * ay + az * az)

    // Heart beat animation spec
    val pulseTransition = rememberInfiniteTransition(label = "pulse")
    val heartbeatScale by pulseTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = (60000 / realHeartRate).coerceIn(400, 1200),
                easing = FastOutLinearInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    // Radar scan lines animations
    val infiniteTransition = rememberInfiniteTransition(label = "radar")
    val pulseScale1 by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "scale1"
    )
    val pulseAlpha1 by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "alpha1"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(DarkBlue, Color(0xFF02070D))
                )
            )
            .systemBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Top Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "← Volver",
                    color = TealPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { onNavigateBack() }
                )
                Spacer(modifier = Modifier.width(24.dp))
                Text(
                    text = "Consola Wear OS BLE",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            when (bleState) {
                BLEState.PERMISSION_REQUEST -> {
                    Text(
                        text = "Permisos de Conectividad Requeridos",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "ImpactX requiere permisos de Bluetooth Cercano y Ubicación para encontrar y conectarse al Samsung Galaxy Watch 6 de forma física.",
                        color = GrayMuted,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(40.dp))

                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .background(TealPrimary.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("📡", fontSize = 48.sp)
                    }

                    Spacer(modifier = Modifier.height(40.dp))

                    Button(
                        onClick = {
                            permissionLauncher.launch(requiredPermissions)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
                    ) {
                        Text("Conceder Permisos", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }

                BLEState.BLUETOOTH_OFF -> {
                    Text(
                        text = "Bluetooth Desactivado",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Por favor activa el Bluetooth desde los ajustes rápidos de tu celular para iniciar la búsqueda física de wearables.",
                        color = GrayMuted,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(45.dp))

                    Button(
                        onClick = {
                            if (hasAllPermissions() && bluetoothAdapter != null && bluetoothAdapter.isEnabled) {
                                bleState = BLEState.SCANNING
                            } else if (!hasAllPermissions()) {
                                bleState = BLEState.PERMISSION_REQUEST
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
                    ) {
                        Text("Reintentar Detección", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }

                BLEState.SCANNING -> {
                    Text(
                        text = "Buscando Relojes...",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Buscando señales BLE activas y dispositivos vinculados.",
                        color = GrayMuted,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(30.dp))

                    // Radar Scanning UI
                    Box(
                        modifier = Modifier.size(180.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val centerOffset = androidx.compose.ui.geometry.Offset(size.width / 2, size.height / 2)
                            
                            // Pulse Circle
                            drawCircle(
                                color = TealPrimary.copy(alpha = pulseAlpha1),
                                radius = (size.width / 2) * pulseScale1,
                                center = centerOffset
                            )

                            drawCircle(
                                color = TealPrimary.copy(alpha = 0.1f),
                                radius = size.width / 2,
                                center = centerOffset,
                                style = Stroke(width = 1.dp.toPx())
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(70.dp)
                                .clip(CircleShape)
                                .background(Brush.radialGradient(listOf(TealPrimary, Color(0xFF005555)))),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("⌚", fontSize = 32.sp, color = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.height(30.dp))
                    
                    // Android location warning note
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1400)),
                        border = BorderStroke(1.dp, Color(0xFFEAB308).copy(alpha = 0.3f))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "⚠️ IMPORTANTE: Ubicación (GPS)",
                                color = Color(0xFFEAB308),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Android requiere que la UBICACIÓN (GPS) de tu teléfono celular esté activada en los ajustes rápidos. Si el GPS está apagado, el scanner no mostrará ningún dispositivo Bluetooth cercano.",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 11.sp,
                                lineHeight = 15.sp
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    CircularProgressIndicator(color = TealPrimary, modifier = Modifier.size(24.dp))
                }

                BLEState.DEVICE_LIST -> {
                    Text(
                        text = "Relojes Encontrados / Vinculados",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.Start)
                    )
                    Text(
                        text = "Selecciona tu Galaxy Watch o conéctalo de forma directa si está emparejado.",
                        color = GrayMuted,
                        fontSize = 13.sp,
                        modifier = Modifier
                            .align(Alignment.Start)
                            .padding(top = 4.dp, bottom = 16.dp)
                    )

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        scannedDevices.forEach { deviceItem ->
                            val isSimulated = deviceItem.device == null

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { connectToDevice(deviceItem) },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF0E1A29)),
                                border = BorderStroke(
                                    1.dp,
                                    if (isSimulated) Color.White.copy(alpha = 0.05f) else TealPrimary.copy(alpha = 0.5f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = deviceItem.name,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                fontSize = 15.sp
                                            )
                                            if (!isSimulated) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(4.dp))
                                                        .background(TealPrimary.copy(alpha = 0.15f))
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Text(
                                                        text = "VINCULADO",
                                                        fontSize = 9.sp,
                                                        color = TealPrimary,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        }
                                        Text(
                                            text = "MAC: ${deviceItem.address}",
                                            fontSize = 11.sp,
                                            color = GrayMuted,
                                            modifier = Modifier.padding(top = 4.dp)
                                        )
                                    }
                                    Text(
                                        text = "📶 ${deviceItem.rssi} dBm",
                                        color = if (deviceItem.rssi > -70) Color(0xFF22C55E) else GrayMuted,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Manual MAC Input Card (Backup Connection)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.03f)),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "🔗 Conexión Manual Directa (MAC Address)",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Si el scanner no detecta el reloj, ingresa la dirección MAC de Bluetooth de tu Samsung Galaxy Watch 6 para forzar enlace directo:",
                                color = GrayMuted,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(vertical = 6.dp)
                            )
                            
                            OutlinedTextField(
                                value = manualMacInput,
                                onValueChange = { manualMacInput = it },
                                placeholder = { Text("ej. AA:BB:CC:11:22:33", color = GrayMuted, fontSize = 13.sp) },
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = TealPrimary,
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                singleLine = true
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Button(
                                onClick = { connectByMac(manualMacInput) },
                                modifier = Modifier.fillMaxWidth().height(46.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                                enabled = manualMacInput.isNotBlank()
                            ) {
                                Text("Forzar Conexión Directa", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    OutlinedButton(
                        onClick = { bleState = BLEState.SCANNING },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, TealPrimary),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TealPrimary)
                    ) {
                        Text("Volver a Buscar", fontWeight = FontWeight.Bold)
                    }
                }

                BLEState.CONNECTING -> {
                    Text(
                        text = "Conectando al Smartwatch...",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Leyendo tablas de servicios GATT e iniciando notificaciones...",
                        color = GrayMuted,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 8.dp)
                    )

                    Spacer(modifier = Modifier.height(60.dp))

                    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(140.dp)) {
                        CircularProgressIndicator(
                            progress = connectionProgress,
                            color = TealPrimary,
                            strokeWidth = 6.dp,
                            modifier = Modifier.fillMaxSize()
                        )
                        Text(
                            text = "${(connectionProgress * 100).toInt()}%",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(60.dp))
                }

                BLEState.CONNECTED_DASHBOARD -> {
                    Spacer(modifier = Modifier.height(8.dp))

                    // Pulse Card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, TealPrimary.copy(alpha = 0.3f), RoundedCornerShape(20.dp)),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF091424))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "MONITOR CARDÍACO EN VIVO",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = TealPrimary,
                                letterSpacing = 1.5.sp
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            // Heart Rate circle with dynamic heartbeat scale
                            Box(
                                modifier = Modifier
                                    .size(160.dp)
                                    .align(Alignment.CenterHorizontally),
                                contentAlignment = Alignment.Center
                            ) {
                                // Background circular ring
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    drawCircle(
                                        color = Color.White.copy(alpha = 0.05f),
                                        style = Stroke(width = 8.dp.toPx())
                                    )
                                    // Segmented accent arcs
                                    drawArc(
                                        color = TealPrimary.copy(alpha = 0.4f),
                                        startAngle = -90f,
                                        sweepAngle = (realHeartRate.toFloat() / 200f * 360f).coerceAtMost(360f),
                                        useCenter = false,
                                        style = Stroke(width = 6.dp.toPx())
                                    )
                                }

                                // Pulsing Heart Icon & BPM Text
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = "❤️",
                                        fontSize = 32.sp,
                                        modifier = Modifier.scale(heartbeatScale)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "$realHeartRate",
                                        fontSize = 44.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "LPM (BPM)",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = GrayMuted
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Dispositivo:", fontSize = 12.sp, color = GrayMuted)
                                Text(
                                    text = "${WearableManager.connectedDeviceName ?: "Desconocido"} (${WearableManager.connectedDeviceAddress ?: "N/A"})",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Fuente de datos:", fontSize = 12.sp, color = GrayMuted)
                                Text(
                                    text = if (isRealConnection) "🟢 CONEXIÓN REAL (GATT)" else "🟡 SIMULACIÓN SENSOR WATCH",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isRealConnection) Color(0xFF22C55E) else Color(0xFFEAB308)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Secondary Vital Grid Card
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Battery Card
                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF091424))
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("🔋 Batería Reloj", fontSize = 11.sp, color = GrayMuted, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "$realBatteryLevel%",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                            }
                        }

                        // SpO2 Card
                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF091424))
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("🩸 Oxígeno SpO2", fontSize = 11.sp, color = GrayMuted, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "$liveSpO2%",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Kinetic Sensor Card (Accelerometer & Gyroscope)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF091424))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "MONITOR KINÉTICO (ACELERÓMETRO Y GIROSCOPIO)",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = TealPrimary,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("Fuerza G Física", fontSize = 11.sp, color = GrayMuted)
                                    Text(
                                        text = String.format("%.2f G", gForce),
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (gForce > 2.5f) Color(0xFFEF4444) else Color.White
                                    )
                                }
                                Column {
                                    Text("Ejes Aceleración", fontSize = 11.sp, color = GrayMuted)
                                    Text(
                                        text = String.format("X:%.1f Y:%.1f Z:%.1f", ax * 9.81, ay * 9.81, az * 9.81),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color.White
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Custom Graphic Canvas to represent active movement
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(40.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.Black.copy(alpha = 0.2f))
                            ) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    val w = size.width
                                    val h = size.height
                                    val strokeWidth = 2.dp.toPx()

                                    // Render wavy pattern based on raw G-force changes
                                    val p = androidx.compose.ui.graphics.Path().apply {
                                        moveTo(0f, h / 2f)
                                        for (i in 0..10) {
                                            val xPos = (w / 10f) * i
                                            val displacement = ((gForce - 1.0f) * 15f * if (i % 2 == 0) 1f else -1f).coerceIn(-h/2, h/2)
                                            lineTo(xPos, (h / 2f) + displacement)
                                        }
                                    }
                                    drawPath(
                                        path = p,
                                        color = TealPrimary.copy(alpha = 0.7f),
                                        style = Stroke(width = strokeWidth)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            activeGatt?.disconnect()
                            activeGatt?.close()
                            activeGatt = null
                            isRealConnection = false
                            bleState = BLEState.SCANNING
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0E1A29))
                    ) {
                        Text("Desconectar Reloj", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}

// Suspending helper extension for Google tasks inside this screen
suspend fun <T> com.google.android.gms.tasks.Task<T>.awaitTask(): T = kotlinx.coroutines.suspendCancellableCoroutine { continuation ->
    addOnCompleteListener { task ->
        if (task.isSuccessful) {
            continuation.resume(task.result)
        } else {
            continuation.resumeWithException(task.exception ?: Exception("Unknown task error"))
        }
    }
}

