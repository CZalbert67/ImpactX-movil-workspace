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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import com.example.impactx.data.remote.ApiClient
import com.example.impactx.data.remote.ContactoDto
import com.example.impactx.data.remote.CreateContactoRequest
import kotlinx.coroutines.launch

@Composable
fun ContactsScreen(
    currentPlan: String,
    onNavigateBack: () -> Unit,
    onNavigateToPlans: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val maxContacts = when (currentPlan) {
        "Básico" -> 3
        "Premium" -> 10
        else -> 999 // Unlimited
    }

    var newContactName by remember { mutableStateOf("") }
    var newContactPhone by remember { mutableStateOf("") }
    var contactsList by remember { mutableStateOf<List<ContactoDto>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }

    fun refreshContacts() {
        isLoading = true
        scope.launch {
            try {
                val api = ApiClient.getApiService(context)
                val response = api.getContacts()
                if (response.isSuccessful) {
                    contactsList = response.body() ?: emptyList()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error de red al cargar contactos", Toast.LENGTH_SHORT).show()
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        refreshContacts()
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
            if (isLoading) {
                CircularProgressIndicator(color = TealPrimary, modifier = Modifier.padding(24.dp))
            } else if (contactsList.isEmpty()) {
                Text(
                    text = "Aún no tienes monitores registrados.",
                    color = GrayMuted,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            } else {
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
                                            text = contact.nombre.take(2).uppercase(),
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TealPrimary
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = contact.nombre,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        Text(
                                            text = contact.telefono,
                                            fontSize = 12.sp,
                                            color = GrayMuted
                                        )
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(
                                                if (contact.esPrincipal) Color(0xFF22C55E).copy(alpha = 0.15f)
                                                else Color(0xFF64748B).copy(alpha = 0.15f)
                                            )
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = if (contact.esPrincipal) "Principal" else "Secundario",
                                            fontSize = 11.sp,
                                            color = if (contact.esPrincipal) Color(0xFF22C55E) else Color(0xFF94A3B8),
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    IconButton(
                                        onClick = {
                                            scope.launch {
                                                try {
                                                    val api = ApiClient.getApiService(context)
                                                    val response = api.deleteContact(contact.id)
                                                    if (response.isSuccessful) {
                                                        Toast.makeText(context, "Monitor eliminado", Toast.LENGTH_SHORT).show()
                                                        refreshContacts()
                                                    }
                                                } catch (e: Exception) {
                                                    Toast.makeText(context, "Error al eliminar monitor", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Eliminar",
                                            tint = Color(0xFFEF4444)
                                        )
                                    }
                                }
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
                            if (isSaving) return@Button
                            isSaving = true
                            scope.launch {
                                try {
                                    val api = ApiClient.getApiService(context)
                                    val response = api.createContact(
                                        CreateContactoRequest(
                                            nombre = newContactName.trim(),
                                            telefono = newContactPhone.trim(),
                                            parentesco = "Amigo",
                                            priority = if (contactsList.isEmpty()) "Principal" else "Secundario",
                                            esPrincipal = contactsList.isEmpty()
                                        )
                                    )
                                    if (response.isSuccessful) {
                                        Toast.makeText(context, "Invitación enviada", Toast.LENGTH_SHORT).show()
                                        newContactName = ""
                                        newContactPhone = ""
                                        refreshContacts()
                                    } else {
                                        Toast.makeText(context, "Error al invitar: ${response.code()}", Toast.LENGTH_SHORT).show()
                                    }
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Error de red al enviar invitación", Toast.LENGTH_SHORT).show()
                                } finally {
                                    isSaving = false
                                }
                            }
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
                    if (isSaving) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Text("Enviar Invitación", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
