package com.example.impactxwearable.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.wear.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

// ─── Calibration screen shown on first start ─────────────────────────────────
@Composable
fun WearCalibrationScreen(onCalibrationDone: () -> Unit) {
    var progress by remember { mutableStateOf(0f) }
    var statusText by remember { mutableStateOf("Iniciando sensores...") }

    val infiniteTransition = rememberInfiniteTransition(label = "calibSpin")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(animation = tween(1200, easing = LinearEasing)),
        label = "rot"
    )

    LaunchedEffect(Unit) {
        val steps = listOf(
            "Calibrando acelerómetro..." to 0.33f,
            "Calibrando giroscopio..." to 0.66f,
            "Verificando ritmo cardíaco..." to 1.0f
        )
        for ((text, target) in steps) {
            statusText = text
            val start = progress
            val steps2 = 30
            repeat(steps2) {
                progress = start + (target - start) * (it + 1f) / steps2
                delay(30L)
            }
            delay(400)
        }
        delay(300)
        onCalibrationDone()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Spinning arc + shield logo
            Box(contentAlignment = Alignment.Center) {
                Canvas(modifier = Modifier.size(80.dp).rotate(rotation)) {
                    drawArc(
                        color = Color(0xFF00BFA5),
                        startAngle = 0f,
                        sweepAngle = 270f,
                        useCenter = false,
                        style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
                // Shield logo
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(Color(0xFF00BFA5), Color(0xFF004D4D))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🛡️", fontSize = 22.sp)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "IMPACT.X",
                fontSize = 13.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFF00BFA5),
                letterSpacing = 2.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = statusText,
                fontSize = 9.sp,
                color = Color.LightGray,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Progress bar
            Box(
                modifier = Modifier
                    .width(100.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.DarkGray)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(progress)
                        .clip(RoundedCornerShape(2.dp))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(Color(0xFF00BFA5), Color(0xFF80CBC4))
                            )
                        )
                )
            }
        }
    }
}

// ─── Main Watch Home Screen ───────────────────────────────────────────────────
@Composable
fun WearHomeScreen(
    isServiceRunning: Boolean,
    heartRate: Int,
    gForce: Float,
    maxGForce: Float,
    isConnected: Boolean,
    isTripActive: Boolean = false,
    onToggleService: () -> Unit,
    onSimulateImpact: () -> Unit,
    onStartTrip: () -> Unit = {},
    onFinishTrip: () -> Unit = {}
) {
    var showCalibration by remember { mutableStateOf(true) }

    if (showCalibration && !isServiceRunning) {
        WearCalibrationScreen(onCalibrationDone = { showCalibration = false })
        return
    }

    // Heart pulse animation synced with BPM
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val heartScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (heartRate > 0) (60000 / heartRate.coerceAtLeast(40)) else 800,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "heartScale"
    )

    // Shield ring pulse
    val ringAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ringAlpha"
    )

    val bgColor = when {
        isTripActive && gForce > 3.0f -> Color(0xFF1A0000)
        isTripActive -> Color(0xFF001520)
        else -> Color.Black
    }

    val statusColor = when {
        !isServiceRunning -> Color.Gray
        isTripActive -> Color(0xFF29B6F6)
        gForce > 3.0f -> Color(0xFFEF5350)
        else -> Color(0xFF00BFA5)
    }

    val statusText = when {
        !isServiceRunning -> "INACTIVO"
        isTripActive && gForce > 3.0f -> "⚠ IMPACTO"
        isTripActive -> "EN VIAJE"
        else -> "PROTEGIDO"
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
        ) {
            // Logo + Status Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text("🛡️", fontSize = 12.sp)
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "IMPACT.X",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF00BFA5),
                    letterSpacing = 1.sp
                )
            }

            Spacer(modifier = Modifier.height(3.dp))

            // Status badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(statusColor.copy(alpha = 0.2f))
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = statusText,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = statusColor
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Central metrics
            if (isServiceRunning) {
                // BPM with pulse
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = null,
                        tint = Color(0xFFEF5350),
                        modifier = Modifier
                            .size(14.dp)
                            .scale(heartScale)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (heartRate > 0) "$heartRate lpm" else "-- lpm",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                // G-Force big display
                val gColor = when {
                    gForce > 4.5f -> Color(0xFFEF5350)
                    gForce > 2.5f -> Color(0xFFFFB74D)
                    else -> Color.White
                }
                Text(
                    text = "${"%.1f".format(gForce)} G",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = gColor
                )
                Text(
                    text = "Pico: ${"%.1f".format(maxGForce)} G",
                    fontSize = 9.sp,
                    color = Color.Gray
                )
            } else {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Presiona ▶ para\nactivar monitoreo",
                    fontSize = 10.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    lineHeight = 14.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Connection dot
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(if (isConnected) Color(0xFF81C784) else Color(0xFFEF5350))
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = if (isConnected) "Celular en línea" else "Sin conexión",
                    fontSize = 8.sp,
                    color = Color.LightGray
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Action Buttons Row
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Monitor ON/OFF toggle
                Button(
                    onClick = onToggleService,
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = if (isServiceRunning) Color(0xFF37474F) else Color(0xFF00BFA5)
                    ),
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = if (isServiceRunning) Icons.Default.Close else Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Trip button
                if (isServiceRunning) {
                    Button(
                        onClick = if (isTripActive) onFinishTrip else onStartTrip,
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = if (isTripActive) Color(0xFF37474F) else Color(0xFF1565C0)
                        ),
                        modifier = Modifier.size(34.dp)
                    ) {
                        Text(
                            text = if (isTripActive) "🏁" else "🚗",
                            fontSize = 14.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Simulate Impact button (only in trip mode)
                    Button(
                        onClick = onSimulateImpact,
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = if (isTripActive) Color(0xFFB71C1C) else Color(0xFF263238)
                        ),
                        enabled = isTripActive,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Text("⚡", fontSize = 13.sp)
                    }
                }
            }

            // Trip label
            if (isServiceRunning) {
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = if (isTripActive) "🏁 Terminar  ⚡ Golpe" else "🚗 Iniciar Viaje",
                    fontSize = 8.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
