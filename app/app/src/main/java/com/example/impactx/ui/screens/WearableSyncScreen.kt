package com.example.impactx.ui.screens

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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class SyncState {
    SCANNING,
    DEVICE_LIST,
    CONNECTING,
    DIAGNOSTICS,
    SUCCESS
}

data class WearableDevice(val name: String, val signal: Int, val description: String)
data class SensorDiagnostic(val name: String, val paramDetail: String, var status: String) // "pending", "running", "success"

@Composable
fun WearableSyncScreen(
    onNavigateBack: () -> Unit
) {
    var currentState by remember { mutableStateOf(SyncState.SCANNING) }
    var selectedDevice by remember { mutableStateOf<WearableDevice?>(null) }
    var connectionProgress by remember { mutableStateOf(0f) }
    val coroutineScope = rememberCoroutineScope()

    val devices = listOf(
        WearableDevice("Redmi Watch 5 Active", 90, "Dispositivo actual de pruebas de parámetros"),
        WearableDevice("Galaxy Watch 8 / Wear OS", 95, "Reloj objetivo de desarrollo final"),
        WearableDevice("Galaxy Watch 6 (Wear OS)", 72, "Dispositivo secundario detectado")
    )

    var diagnostics by remember {
        mutableStateOf(
            listOf(
                SensorDiagnostic("Acelerómetro Triaxial (Fuerza G)", "Ejes: Ax=0.02G, Ay=0.04G, Az=0.99G (Total: 1.05G)", "pending"),
                SensorDiagnostic("Giroscopio (Velocidad Angular)", "Rotación: Pitch=0°/s, Roll=0°/s, Yaw=0°/s", "pending"),
                SensorDiagnostic("Ritmo Cardíaco & SpO2", "Ritmo: 75 bpm | Saturación: 98% SpO2", "pending"),
                SensorDiagnostic("GPS Integrado", "Coordenadas: Lat 20.0841, Lon -99.3442", "pending"),
                SensorDiagnostic("Termómetro Corporal", "Temperatura: 36.5 °C", "pending")
            )
        )
    }

    // Radar scan pulse animations
    val infiniteTransition = rememberInfiniteTransition(label = "radar")
    val pulseScale1 by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "scale1"
    )
    val pulseAlpha1 by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "alpha1"
    )

    // Trigger state machines
    LaunchedEffect(currentState) {
        if (currentState == SyncState.SCANNING) {
            delay(3000)
            currentState = SyncState.DEVICE_LIST
        } else if (currentState == SyncState.CONNECTING) {
            connectionProgress = 0f
            while (connectionProgress < 1.0f) {
                delay(30)
                connectionProgress += 0.02f
            }
            currentState = SyncState.DIAGNOSTICS
        } else if (currentState == SyncState.DIAGNOSTICS) {
            // Sequential diagnostics simulation
            diagnostics = diagnostics.map { it.copy(status = "pending") }
            diagnostics.indices.forEach { index ->
                delay(200)
                // Mark as running
                diagnostics = diagnostics.toMutableList().apply {
                    this[index] = this[index].copy(status = "running")
                }
                delay(1200)
                // Mark as success
                diagnostics = diagnostics.toMutableList().apply {
                    this[index] = this[index].copy(status = "success")
                }
            }
            delay(1000)
            currentState = SyncState.SUCCESS
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(DarkBlue, Color(0xFF040D17))
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
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "← Atrás",
                    color = TealPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { onNavigateBack() }
                )
                Spacer(modifier = Modifier.width(24.dp))
                Text(
                    text = "Sincronizar Wearable",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(30.dp))

            when (currentState) {
                SyncState.SCANNING -> {
                    Text(
                        text = "Buscando dispositivos Wearables cercanos...",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Asegúrate de que el Bluetooth de tu reloj esté activado.",
                        color = GrayMuted,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 8.dp)
                    )

                    Spacer(modifier = Modifier.height(50.dp))

                    // Radar animation using Canvas
                    Box(
                        modifier = Modifier.size(220.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val centerOffset = androidx.compose.ui.geometry.Offset(size.width / 2, size.height / 2)
                            
                            // Pulse circle
                            drawCircle(
                                color = TealPrimary.copy(alpha = pulseAlpha1),
                                radius = (size.width / 2) * pulseScale1,
                                center = centerOffset
                            )
                            
                            // Grid circles
                            drawCircle(
                                color = TealPrimary.copy(alpha = 0.1f),
                                radius = size.width / 2,
                                center = centerOffset,
                                style = Stroke(width = 1.dp.toPx())
                            )
                            drawCircle(
                                color = TealPrimary.copy(alpha = 0.2f),
                                radius = size.width / 3,
                                center = centerOffset,
                                style = Stroke(width = 1.dp.toPx())
                            )
                            drawCircle(
                                color = TealPrimary.copy(alpha = 0.3f),
                                radius = size.width / 5,
                                center = centerOffset,
                                style = Stroke(width = 1.dp.toPx())
                            )
                        }

                        // Core watch icon center
                        Box(
                            modifier = Modifier
                                .size(70.dp)
                                .clip(CircleShape)
                                .background(Brush.radialGradient(listOf(TealPrimary, Color(0xFF006666)))),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("⌚", fontSize = 32.sp, color = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.height(40.dp))
                    
                    CircularProgressIndicator(color = TealPrimary, modifier = Modifier.size(24.dp))
                }

                SyncState.DEVICE_LIST -> {
                    Text(
                        text = "Dispositivos Bluetooth Encontrados",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.Start)
                    )
                    Text(
                        text = "Selecciona el reloj que deseas vincular para la telemetría de seguridad.",
                        color = GrayMuted,
                        fontSize = 13.sp,
                        modifier = Modifier.align(Alignment.Start).padding(top = 4.dp, bottom = 16.dp)
                    )

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        devices.forEach { device ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedDevice = device
                                        currentState = SyncState.CONNECTING
                                    },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF102238)),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = device.name,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            fontSize = 15.sp
                                        )
                                        Text(
                                            text = device.description,
                                            fontSize = 12.sp,
                                            color = GrayMuted,
                                            modifier = Modifier.padding(top = 2.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "📶 ${device.signal}%",
                                            color = TealPrimary,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                        Text(
                                            text = if (device.signal > 85) "Fuerte" else "Media",
                                            fontSize = 10.sp,
                                            color = GrayMuted
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    
                    OutlinedButton(
                        onClick = { currentState = SyncState.SCANNING },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, TealPrimary),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TealPrimary)
                    ) {
                        Text("Buscar de Nuevo", fontWeight = FontWeight.Bold)
                    }
                }

                SyncState.CONNECTING -> {
                    Text(
                        text = "Vinculando dispositivo...",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Estableciendo enlace de cifrado con ${selectedDevice?.name}...",
                        color = GrayMuted,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 8.dp)
                    )

                    Spacer(modifier = Modifier.height(60.dp))

                    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(150.dp)) {
                        CircularProgressIndicator(
                            progress = connectionProgress,
                            color = TealPrimary,
                            strokeWidth = 6.dp,
                            modifier = Modifier.fillMaxSize()
                        )
                        Text(
                            text = "${(connectionProgress * 100).toInt()}%",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(60.dp))
                }

                SyncState.DIAGNOSTICS -> {
                    Text(
                        text = "Diagnóstico de Parámetros y Sensores",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.Start)
                    )
                    Text(
                        text = "Probando la captura de datos fisiológicos y mecánicos del reloj...",
                        color = GrayMuted,
                        fontSize = 13.sp,
                        modifier = Modifier.align(Alignment.Start).padding(top = 4.dp, bottom = 20.dp)
                    )

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        diagnostics.forEach { diag ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF102238).copy(alpha = 0.5f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = diag.name,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            fontSize = 14.sp
                                        )
                                        Text(
                                            text = diag.paramDetail,
                                            fontSize = 12.sp,
                                            color = if (diag.status == "success") TealPrimary else GrayMuted,
                                            modifier = Modifier.padding(top = 2.dp),
                                            fontWeight = if (diag.status == "success") FontWeight.SemiBold else FontWeight.Normal
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    when (diag.status) {
                                        "pending" -> {
                                            Text("En espera", fontSize = 12.sp, color = GrayMuted)
                                        }
                                        "running" -> {
                                            CircularProgressIndicator(
                                                color = TealPrimary,
                                                modifier = Modifier.size(16.dp),
                                                strokeWidth = 2.dp
                                            )
                                        }
                                        "success" -> {
                                            Text(
                                                text = "✓ OK",
                                                color = Color(0xFF22C55E),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                SyncState.SUCCESS -> {
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF22C55E).copy(alpha = 0.15f))
                            .border(2.dp, Color(0xFF22C55E), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("✓", fontSize = 48.sp, fontWeight = FontWeight.Bold, color = Color(0xFF22C55E))
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "¡Dispositivo Sincronizado!",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    
                    Text(
                        text = "${selectedDevice?.name} está enlazado correctamente como monitor activo en segundo plano.\n\nLos parámetros de Fuerza G y Signos Vitales se enviarán a tu red de emergencia en caso de siniestro.",
                        color = GrayMuted,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                    )

                    Spacer(modifier = Modifier.height(30.dp))

                    Button(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
                    ) {
                        Text("Volver al Inicio", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
