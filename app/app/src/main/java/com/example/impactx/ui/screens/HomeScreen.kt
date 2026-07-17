package com.example.impactx.ui.screens

import androidx.compose.animation.core.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun HomeScreen(
    currentPlan: String,
    onNavigateToMedical: () -> Unit,
    onNavigateToVehicle: () -> Unit,
    onNavigateToContacts: () -> Unit,
    onNavigateToPlans: () -> Unit,
    onNavigateToWearableSync: () -> Unit,
    onLogout: () -> Unit
) {
    // Pulse animation for the active shield
    val infiniteTransition = rememberInfiniteTransition(label = "shieldPulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )
    val pulseSize by infiniteTransition.animateFloat(
        initialValue = 110.dp.value,
        targetValue = 130.dp.value,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "size"
    )

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
                .padding(20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Custom Top Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Profile initials avatar
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(TealPrimary)
                            .clickable { onLogout() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "AC",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Hola, Alberto Zepeda",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { onNavigateToPlans() }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when (currentPlan) {
                                            "Básico" -> Color(0xFFF59E0B)
                                            else -> Color(0xFF22C55E)
                                        }
                                    )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Plan $currentPlan >",
                                fontSize = 12.sp,
                                color = when (currentPlan) {
                                    "Básico" -> Color(0xFFF59E0B)
                                    else -> Color(0xFF22C55E)
                                },
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Main Status Banner (Clickable to sync wearable)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToWearableSync() },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF102238)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Burbuja de Seguridad",
                                fontSize = 13.sp,
                                color = GrayMuted
                            )
                            Text(
                                text = if (currentPlan == "Básico") "Funciones Limitadas" else "Wear OS: Conectado",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    when (currentPlan) {
                                        "Básico" -> Color(0xFFF59E0B).copy(alpha = 0.15f)
                                        else -> Color(0xFF22C55E).copy(alpha = 0.15f)
                                    }
                                )
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (currentPlan == "Básico") "Básico" else "Autodiagnóstico Ok",
                                fontSize = 11.sp,
                                color = when (currentPlan) {
                                    "Básico" -> Color(0xFFF59E0B)
                                    else -> Color(0xFF22C55E)
                                },
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    Divider(color = Color.White.copy(alpha = 0.05f))
                    Spacer(modifier = Modifier.height(6.dp))
                    
                    Text(
                        text = "⌚ Pulsa para sincronizar o probar sensores del reloj",
                        fontSize = 12.sp,
                        color = TealPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.weight(0.15f))

            // Central Passive Shield/Status Representation (Replaces Start Trip Button)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                contentAlignment = Alignment.Center
            ) {
                // Animated pulse glow rings
                Box(
                    modifier = Modifier
                        .size(pulseSize.dp)
                        .clip(CircleShape)
                        .background(TealPrimary.copy(alpha = pulseAlpha * 0.15f))
                )
                Box(
                    modifier = Modifier
                        .size((pulseSize - 20.dp.value).dp)
                        .clip(CircleShape)
                        .background(TealPrimary.copy(alpha = pulseAlpha * 0.3f))
                )
                
                // Solid center shield representation
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(TealPrimary, Color(0xFF006666))
                            )
                        )
                        .border(2.dp, Color.White.copy(alpha = 0.3f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "🛡️",
                        fontSize = 44.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Trip instruction status text
            Text(
                text = "Monitoreo en Segundo Plano Activo",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            Text(
                text = "El viaje se iniciará de forma automática en cuanto comiences la actividad desde tu Smartwatch.",
                fontSize = 13.sp,
                color = GrayMuted,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 6.dp),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.weight(0.2f))

            // Live Telemetry Mini-KPI Panel
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF102238))
                        .padding(14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (currentPlan == "Básico") "N/A" else "75 bpm",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text("Ritmo Cardíaco", fontSize = 11.sp, color = GrayMuted)
                    }
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF102238))
                        .padding(14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (currentPlan == "Básico") "N/A" else "98% SpO2",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text("Oxígeno", fontSize = 11.sp, color = GrayMuted)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Quick Access Navigation Options
            Text(
                text = "CONFIGURACIÓN DE SEGURIDAD",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = GrayMuted,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(6.dp))

            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Medical Card Link
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF102238))
                        .clickable { onNavigateToMedical() }
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🏥", fontSize = 20.sp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Ficha Médica", fontWeight = FontWeight.Bold, color = Color.White)
                            Text("Grupo sanguíneo y condiciones", fontSize = 12.sp, color = GrayMuted)
                        }
                    }
                    Text("›", fontSize = 24.sp, color = GrayMuted)
                }

                // Vehicle Profile Card Link
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF102238))
                        .clickable { onNavigateToVehicle() }
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🚗", fontSize = 20.sp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Mi Vehículo", fontWeight = FontWeight.Bold, color = Color.White)
                            Text("Detalles y velocidad promedio", fontSize = 12.sp, color = GrayMuted)
                        }
                    }
                    Text("›", fontSize = 24.sp, color = GrayMuted)
                }

                // Contacts / Monitors Card Link
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF102238))
                        .clickable { onNavigateToContacts() }
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🛡️", fontSize = 20.sp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Contactos y Monitores", fontWeight = FontWeight.Bold, color = Color.White)
                            Text("Red de alerta en colisiones", fontSize = 12.sp, color = GrayMuted)
                        }
                    }
                    Text("›", fontSize = 24.sp, color = GrayMuted)
                }
            }
        }
    }
}
