package com.example.impactx.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ContactsScreen(
    currentPlan: String,
    onNavigateBack: () -> Unit,
    onNavigateToPlans: () -> Unit
) {
    val maxContacts = when (currentPlan) {
        "Básico" -> 1
        "Premium" -> 3
        else -> 999 // Unlimited
    }

    var newContactName by remember { mutableStateOf("") }
    var newContactPhone by remember { mutableStateOf("") }
    var contactsList by remember {
        mutableStateOf(
            listOf(
                Contact("Omar Picazo", "5512345678", "Activo")
            )
        )
    }

    val limitReached = contactsList.size >= maxContacts

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
            Spacer(modifier = Modifier.height(8.dp))

            // Custom Top bar with back arrow
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
                    text = "Monitores de Emergencia",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Plan Info Banner
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF102238))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Plan Activo: $currentPlan", fontWeight = FontWeight.Bold, color = Color.White)
                        Text(
                            text = if (maxContacts == 999) "Monitores registrados: ${contactsList.size}" 
                                   else "Límite de monitores: ${contactsList.size} / $maxContacts",
                            fontSize = 12.sp,
                            color = GrayMuted
                        )
                    }
                    if (currentPlan == "Básico") {
                        Button(
                            onClick = onNavigateToPlans,
                            colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text("Upgrade", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Tus monitores recibirán notificaciones automáticas y tu ubicación GPS en tiempo real si el sistema detecta una colisión fuerte.",
                fontSize = 14.sp,
                color = GrayMuted,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Contacts List
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                contactsList.forEach { contact ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF102238).copy(alpha = 0.5f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(TealPrimary.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = contact.name.take(2).uppercase(),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TealPrimary
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = contact.name,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = contact.phone,
                                        fontSize = 12.sp,
                                        color = GrayMuted
                                    )
                                }
                            }
                            
                            val isPending = contact.status.contains("pendiente")
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(
                                        if (isPending) Color(0xFFF59E0B).copy(alpha = 0.15f)
                                        else Color(0xFF22C55E).copy(alpha = 0.15f)
                                    )
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = contact.status,
                                    fontSize = 11.sp,
                                    color = if (isPending) Color(0xFFF59E0B) else Color(0xFF22C55E),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Add new monitor section
            if (limitReached) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFEF4444).copy(alpha = 0.1f)),
                    border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.3f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Límite de Monitores Alcanzado",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Tu plan '$currentPlan' permite registrar un máximo de $maxContacts contacto(s).\nMejora tu plan para proteger a más personas.",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.8f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                        )
                        Button(
                            onClick = onNavigateToPlans,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                        ) {
                            Text("Ver Planes de Suscripción", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                Text(
                    text = "INVITAR NUEVO MONITOR",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = GrayMuted,
                    modifier = Modifier.align(Alignment.Start),
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = newContactName,
                    onValueChange = { newContactName = it },
                    label = { Text("Nombre del Contacto") },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TealPrimary,
                        unfocusedBorderColor = GrayMuted,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    singleLine = true
                )

                OutlinedTextField(
                    value = newContactPhone,
                    onValueChange = { newContactPhone = it },
                    label = { Text("Teléfono de contacto") },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TealPrimary,
                        unfocusedBorderColor = GrayMuted,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        if (newContactName.isNotBlank() && newContactPhone.isNotBlank()) {
                            contactsList = contactsList + Contact(newContactName, newContactPhone, "Invitación pendiente")
                            newContactName = ""
                            newContactPhone = ""
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TealPrimary,
                        contentColor = Color.White
                    )
                ) {
                    Text("Enviar Invitación", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
