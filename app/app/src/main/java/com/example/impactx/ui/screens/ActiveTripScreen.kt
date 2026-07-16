package com.example.impactx.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun ActiveTripScreen(
    onFinishTrip: () -> Unit
) {
    var speed by remember { mutableStateOf(65) }
    var gForce by remember { mutableStateOf(1.03f) }
    var timerSeconds by remember { mutableStateOf(0) }
    var showSosConfirmation by remember { mutableStateOf(false) }

    // Simulating driving changes
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            timerSeconds++
            // Slightly fluctuate speed and G-Force
            speed = (speed + (-3..3).random()).coerceIn(45, 85)
            gForce = 1.0f + (0..15).random() / 100.0f
        }
    }

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
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            // Trip header
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

            // Route details card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF102238)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(TealPrimary)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Partida: Tula de Allende",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFEF4444))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Destino: Tepeji del Río",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(0.2f))

            // Telemetry circular gauges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF102238))
                            .border(2.dp, TealPrimary, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "$speed\nkm/h",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            lineHeight = 22.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Velocidad", fontSize = 12.sp, color = GrayMuted)
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF102238))
                            .border(2.dp, Color(0xFF22C55E), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = String.format("%.2f G", gForce),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Fuerza G", fontSize = 12.sp, color = GrayMuted)
                }
            }

            Spacer(modifier = Modifier.weight(0.3f))

            // SOS Alert button
            if (!showSosConfirmation) {
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFEF4444).copy(alpha = 0.15f))
                        .clickable { showSosConfirmation = true }
                        .border(1.dp, Color(0xFFEF4444), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(90.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFEF4444)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "SOS",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Presiona en caso de emergencia médica o vial",
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
                            text = "Esto notificará a tus monitores y enviará tu ficha médica inmediatamente.",
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
                                Text("Cancelar")
                            }
                            Button(
                                onClick = { showSosConfirmation = false },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                            ) {
                                Text("Sí, Enviar")
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(0.2f))

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
