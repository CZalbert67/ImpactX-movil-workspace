package com.example.impactx.ui.screens

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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun HomeScreen(
    currentPlan: String,
    onNavigateToMedical: () -> Unit,
    onNavigateToVehicle: () -> Unit,
    onNavigateToContacts: () -> Unit,
    onNavigateToPlans: () -> Unit,
    onStartTrip: () -> Unit,
    onLogout: () -> Unit
) {
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

            Spacer(modifier = Modifier.height(24.dp))

            // Main Status Banner
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF102238)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Burbuja de Seguridad",
                            fontSize = 14.sp,
                            color = GrayMuted
                        )
                        Text(
                            text = if (currentPlan == "Básico") "Funciones Limitadas" else "Monitoreo Listo",
                            fontSize = 18.sp,
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
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (currentPlan == "Básico") "Básico" else "Wear OS: Ok",
                            fontSize = 12.sp,
                            color = when (currentPlan) {
                                "Básico" -> Color(0xFFF59E0B)
                                else -> Color(0xFF22C55E)
                            },
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(0.2f))

            // Central Massive Start Trip Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                contentAlignment = Alignment.Center
            ) {
                // Outer glow representation
                Box(
                    modifier = Modifier
                        .size(190.dp)
                        .clip(CircleShape)
                        .background(TealPrimary.copy(alpha = 0.1f))
                )
                Box(
                    modifier = Modifier
                        .size(165.dp)
                        .clip(CircleShape)
                        .background(TealPrimary.copy(alpha = 0.2f))
                )
                // Main Button
                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(TealPrimary, Color(0xFF008080))
                            )
                        )
                        .clickable { onStartTrip() }
                        .border(4.dp, Color.White.copy(alpha = 0.2f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "INICIAR",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "VIAJE",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(0.3f))

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
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (currentPlan == "Básico") "N/A" else "1.02 G",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text("Fuerza G", fontSize = 12.sp, color = GrayMuted)
                    }
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF102238))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("0 km/h", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("Velocidad", fontSize = 12.sp, color = GrayMuted)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Quick Access Navigation Options
            Text(
                text = "CONFIGURACIÓN DE SEGURIDAD",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = GrayMuted,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

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
                        .padding(16.dp),
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
                        .padding(16.dp),
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
                        .padding(16.dp),
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
