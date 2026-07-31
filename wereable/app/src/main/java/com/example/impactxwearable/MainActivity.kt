package com.example.impactxwearable

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.content.ContextCompat
import com.example.impactxwearable.data.SensorService
import com.example.impactxwearable.theme.ImpactXWearableTheme
import com.example.impactxwearable.ui.WearAlertScreen
import com.example.impactxwearable.ui.WearHomeScreen

class MainActivity : ComponentActivity() {

    private var sensorService by mutableStateOf<SensorService?>(null)
    private var isServiceRunning by mutableStateOf(false)
    private var isBound by mutableStateOf(false)

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as SensorService.SensorBinder
            sensorService = binder.getService()
            isBound = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            sensorService = null
            isBound = false
        }
    }

    // Permission request launcher
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val sensorsGranted = permissions[Manifest.permission.BODY_SENSORS] ?: false
        val activityGranted = permissions[Manifest.permission.ACTIVITY_RECOGNITION] ?: false
        if (sensorsGranted && activityGranted) {
            Toast.makeText(this, "Permisos de sensores concedidos", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Se requieren permisos para monitorear incidentes", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        checkAndRequestPermissions()

        setContent {
            ImpactXWearableTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Black
                ) {
                    val service = sensorService
                    
                    if (isServiceRunning && service != null) {
                        val heartRate by service.heartRate.collectAsState()
                        val gForce by service.gForce.collectAsState()
                        val maxGForce by service.maxGForce.collectAsState()
                        val isConnected by service.isConnected.collectAsState()
                        val impactDetected by service.impactDetected.collectAsState()

                        // Alarm check from Intent
                        val triggerAlarmFromIntent = intent?.getBooleanExtra("TRIGGER_ALARM", false) ?: false
                        if (triggerAlarmFromIntent && !impactDetected) {
                            // Trigger it locally if intent requested it
                            service.sendSignalToPhone("/impact-detected", "CRITICAL_IMPACT")
                        }

                        if (impactDetected || triggerAlarmFromIntent) {
                            WearAlertScreen(
                                onCancel = {
                                    intent?.removeExtra("TRIGGER_ALARM")
                                    service.resetAlarm()
                                },
                                onTimeout = {
                                    service.sendSignalToPhone("/sos-triggered", "CRITICAL_SOS")
                                }
                            )
                        } else {
                            WearHomeScreen(
                                isServiceRunning = true,
                                heartRate = heartRate,
                                gForce = gForce,
                                maxGForce = maxGForce,
                                isConnected = isConnected,
                                onToggleService = { toggleMonitoringService() },
                                onSimulateImpact = { service.sendSignalToPhone("/simulate-impact", "SIMULATE") }
                            )
                        }
                    } else {
                        // Monitor not running or loading binding
                        WearHomeScreen(
                            isServiceRunning = false,
                            heartRate = 0,
                            gForce = 1.0f,
                            maxGForce = 1.0f,
                            isConnected = false,
                            onToggleService = { toggleMonitoringService() },
                            onSimulateImpact = {}
                        )
                    }
                }
            }
        }
    }

    private fun checkAndRequestPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.BODY_SENSORS,
            Manifest.permission.ACTIVITY_RECOGNITION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        val missingPermissions = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isNotEmpty()) {
            requestPermissionLauncher.launch(missingPermissions.toTypedArray())
        }
    }

    private fun toggleMonitoringService() {
        val intent = Intent(this, SensorService::class.java)
        if (isServiceRunning) {
            if (isBound) {
                unbindService(connection)
                isBound = false
            }
            stopService(intent)
            isServiceRunning = false
            sensorService = null
        } else {
            startForegroundService(intent)
            bindService(intent, connection, Context.BIND_AUTO_CREATE)
            isServiceRunning = true
        }
    }

    override fun onDestroy() {
        if (isBound) {
            unbindService(connection)
            isBound = false
        }
        super.onDestroy()
    }
}
