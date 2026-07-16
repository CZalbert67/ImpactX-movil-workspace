package com.example.impactx.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun ActiveTripScreen(
    currentPlan: String,
    onTriggerSos: () -> Unit,
    onFinishTrip: () -> Unit,
    onNavigateToPlans: () -> Unit
) {
    var speed by remember { mutableStateOf(65) }
    var gForce by remember { mutableStateOf(1.03f) }
    var timerSeconds by remember { mutableStateOf(0) }
    var showSosConfirmation by remember { mutableStateOf(false) }

    // Driving simulator loop
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            timerSeconds++
            speed = (speed + (-3..3).random()).coerceIn(45, 85)
            gForce = 1.0f + (0..15).random() / 100.0f
        }
    }

    // Animation progress for the car on the map
    val infiniteTransition = rememberInfiniteTransition(label = "mapProgress")
    val carProgress by infiniteTransition.animateFloat(
        initialValue = 0.0f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "progress"
    )

    val minutes = timerSeconds / 60
    val seconds = timerSeconds % 60
    val timeFormatted = String.format("%02d:%02d", minutes, seconds)

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
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Text(
                text = "VIAJE ACTIVO",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = TealPrimary,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = timeFormatted,
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Map Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF102238))
                    .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (currentPlan == "Básico") {
                    // Locked Overlay for Basic users
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .blur(4.dp)
                            .background(Color.Black.copy(alpha = 0.4f))
                    )
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Text("🔒", fontSize = 28.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Mapa en tiempo real bloqueado",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "El plan básico no incluye telemetría visual de mapas.",
                            fontSize = 11.sp,
                            color = GrayMuted,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Button(
                            onClick = onNavigateToPlans,
                            colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text("Ver Planes", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    // Beautiful custom Canvas map for Premium/Guardian users
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        val width = size.width
                        val height = size.height

                        // Grid lines
                        val gridCount = 5
                        for (i in 1..gridCount) {
                            val x = (width / (gridCount + 1)) * i
                            val y = (height / (gridCount + 1)) * i
                            drawLine(
                                color = Color.White.copy(alpha = 0.05f),
                                start = androidx.compose.ui.geometry.Offset(x, 0f),
                                end = androidx.compose.ui.geometry.Offset(x, height),
                                strokeWidth = 1f
                            )
                            drawLine(
                                color = Color.White.copy(alpha = 0.05f),
                                start = androidx.compose.ui.geometry.Offset(0f, y),
                                end = androidx.compose.ui.geometry.Offset(width, y),
                                strokeWidth = 1f
                            )
                        }

                        // Draw path (Road)
                        val path = Path().apply {
                            moveTo(width * 0.1f, height * 0.8f)
                            cubicTo(
                                width * 0.3f, height * 0.2f,
                                width * 0.6f, height * 0.9f,
                                width * 0.9f, height * 0.2f
                            )
                        }

                        drawPath(
                            path = path,
                            color = TealPrimary.copy(alpha = 0.3f),
                            style = Stroke(width = 8.dp.toPx())
                        )
                        drawPath(
                            path = path,
                            color = TealPrimary,
                            style = Stroke(width = 2.dp.toPx())
                        )

                        // Start Pin
                        drawCircle(
                            color = TealPrimary,
                            radius = 6.dp.toPx(),
                            center = androidx.compose.ui.geometry.Offset(width * 0.1f, height * 0.8f)
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 2.dp.toPx(),
                            center = androidx.compose.ui.geometry.Offset(width * 0.1f, height * 0.8f)
                        )

                        // End Pin
                        drawCircle(
                            color = Color(0xFFEF4444),
                            radius = 6.dp.toPx(),
                            center = androidx.compose.ui.geometry.Offset(width * 0.9f, height * 0.2f)
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 2.dp.toPx(),
                            center = androidx.compose.ui.geometry.Offset(width * 0.9f, height * 0.2f)
                        )

                        // Moving Car (Simulated dot)
                        // Simple Bezier math interpolation for rendering
                        val t = carProgress
                        val x = (1-t)*(1-t)*(1-t) * (width * 0.1f) + 
                                3*(1-t)*(1-t)*t * (width * 0.3f) + 
                                3*(1-t)*t*t * (width * 0.6f) + 
                                t*t*t * (width * 0.9f)
                        val y = (1-t)*(1-t)*(1-t) * (height * 0.8f) + 
                                3*(1-t)*(1-t)*t * (height * 0.2f) + 
                                3*(1-t)*t*t * (height * 0.9f) + 
                                t*t*t * (height * 0.2f)

                        // Glow outer
                        drawCircle(
                            color = Color(0xFF22C55E).copy(alpha = 0.4f),
                            radius = 10.dp.toPx(),
                            center = androidx.compose.ui.geometry.Offset(x, y)
                        )
                        // Car dot
                        drawCircle(
                            color = Color(0xFF22C55E),
                            radius = 5.dp.toPx(),
                            center = androidx.compose.ui.geometry.Offset(x, y)
                        )
                    }

                    // Route details overlaid text
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(10.dp),
                        contentAlignment = Alignment.BottomStart
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color.Black.copy(alpha = 0.5f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "Ruta: Tula → Tepeji",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(0.1f))

            // Telemetry gauges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(90.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF102238))
                            .border(2.dp, TealPrimary, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "$speed\nkm/h",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Velocidad", fontSize = 12.sp, color = GrayMuted)
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(90.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF102238))
                            .border(2.dp, Color(0xFF22C55E), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = String.format("%.2f G", gForce),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Fuerza G", fontSize = 12.sp, color = GrayMuted)
                }
            }

            Spacer(modifier = Modifier.weight(0.15f))

            // SOS Alert button
            if (currentPlan == "Básico") {
                // Locked SOS message for Basic users
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "🚨 Botón de SOS Desactivado",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFEF4444)
                        )
                        Text(
                            text = "El plan básico no incluye alertas automáticas ni botón de pánico SOS.",
                            fontSize = 11.sp,
                            color = GrayMuted,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            } else {
                if (!showSosConfirmation) {
                    Box(
                        modifier = Modifier
                            .size(110.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFEF4444).copy(alpha = 0.15f))
                            .clickable { showSosConfirmation = true }
                            .border(1.dp, Color(0xFFEF4444), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(85.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFEF4444)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "SOS",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Presiona en caso de emergencia",
                        fontSize = 12.sp,
                        color = GrayMuted,
                        textAlign = TextAlign.Center
                    )
                } else {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFEF4444).copy(alpha = 0.2f)),
                        border = BorderStroke(1.dp, Color(0xFFEF4444))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "¿Enviar Alerta SOS?",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Esto iniciará el chat de seguridad y notificará a tus monitores.",
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.8f),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { showSosConfirmation = false },
                                    modifier = Modifier.weight(1f),
                                    border = BorderStroke(1.dp, Color.White),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                                ) {
                                    Text("No")
                                }
                                Button(
                                    onClick = {
                                        showSosConfirmation = false
                                        onTriggerSos()
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                                ) {
                                    Text("Sí, Enviar")
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(0.1f))

            // Finish Trip Button
            Button(
                onClick = onFinishTrip,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF102238),
                    contentColor = Color.White
                )
            ) {
                Text("Finalizar Viaje", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
