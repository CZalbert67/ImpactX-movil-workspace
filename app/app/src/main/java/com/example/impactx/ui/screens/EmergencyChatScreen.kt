package com.example.impactx.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.impactx.data.remote.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun EmergencyChatScreen(
    onCloseChat: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    var chatMessages by remember {
        mutableStateOf(
            listOf(
                ChatMessage("sistema", "Alerta SOS activada. Transmitiendo geolocalización GPS y ficha médica en tiempo real...")
            )
        )
    }

    val quickReplies = listOf(
        "Falsa alarma, estoy bien 👍",
        "Colisión menor, necesito grúa 🚗",
        "¡Accidente! Envíen ambulancia 🚑",
        "Estoy atrapado, llamen al 911 🚨"
    )

    // Trigger auto-SOS sending to all monitors fetched from API
    LaunchedEffect(Unit) {
        coroutineScope.launch {
            try {
                val api = ApiClient.getApiService(context)
                
                // 1. Get my profile info
                val profileResponse = api.getProfileUsername()
                if (!profileResponse.isSuccessful) {
                    chatMessages = chatMessages + ChatMessage("sistema", "Error al consultar perfil. Reintentando...")
                    return@launch
                }
                val myProfileId = profileResponse.body()?.publicProfileId ?: ""
                
                // 2. Get my active templates
                val templatesResponse = api.getQuickMessageTemplates()
                if (!templatesResponse.isSuccessful || templatesResponse.body().isNullOrEmpty()) {
                    chatMessages = chatMessages + ChatMessage("sistema", "Error: No se encontraron plantillas de mensajes rápidos.")
                    return@launch
                }
                val templates = templatesResponse.body()!!
                
                // Find emergency template containing 'ayuda', 'sos', or 'emergencia'
                val sosTemplate = templates.find {
                    val textLower = it.text.lowercase()
                    textLower.contains("ayuda") || textLower.contains("sos") || textLower.contains("emergencia")
                } ?: templates.first()
                
                // 3. Get my monitoring relationships
                val relResponse = api.getMonitoringRelationships()
                if (!relResponse.isSuccessful) {
                    chatMessages = chatMessages + ChatMessage("sistema", "Error al obtener relaciones de monitoreo.")
                    return@launch
                }
                
                // Filter active accepted monitors where I am the one monitored
                val acceptedMonitors = (relResponse.body() ?: emptyList()).filter {
                    it.status.lowercase() == "accepted"
                }
                
                if (acceptedMonitors.isEmpty()) {
                    chatMessages = chatMessages + ChatMessage("sistema", "No tienes monitores activos configurados.")
                    return@launch
                }
                
                chatMessages = chatMessages + ChatMessage("sistema", "Despachando SOS automático a tus monitores...")
                
                acceptedMonitors.forEach { rel ->
                    // Determine monitor public profile ID and username
                    val monitorProfileId = if (rel.monitoredPublicProfileId == myProfileId) {
                        rel.monitorPublicProfileId
                    } else {
                        rel.monitoredPublicProfileId
                    }
                    
                    val monitorUsername = if (rel.monitoredPublicProfileId == myProfileId) {
                        rel.monitorUsername
                    } else {
                        rel.monitoredUsername ?: "Usuario"
                    }
                    
                    if (monitorProfileId != null) {
                        try {
                            val sendResponse = api.sendQuickMessage(
                                SendQuickMessageRequest(
                                    recipientPublicProfileId = monitorProfileId,
                                    publicTemplateId = sosTemplate.publicTemplateId
                                )
                            )
                            if (sendResponse.isSuccessful) {
                                chatMessages = chatMessages + ChatMessage(
                                    "sistema",
                                    "SOS enviado correctamente a @$monitorUsername"
                                )
                            } else {
                                chatMessages = chatMessages + ChatMessage(
                                    "sistema",
                                    "Fallo al enviar a @$monitorUsername (${sendResponse.code()})"
                                )
                            }
                        } catch (e: Exception) {
                            chatMessages = chatMessages + ChatMessage(
                                "sistema",
                                "Error al enviar a @$monitorUsername"
                              )
                        }
                    }
                }
                
            } catch (e: Exception) {
                chatMessages = chatMessages + ChatMessage("sistema", "Error durante el despacho automático de alertas.")
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF1C0808), Color(0xFF070202))
                )
            )
            .systemBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Top SOS Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFEF4444).copy(alpha = 0.15f))
                    .border(1.dp, Color(0xFFEF4444), RoundedCornerShape(12.dp))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "EMERGENCIA SOS ACTIVA",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFEF4444),
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Centro de Monitoreo Conectado",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(Color(0xFFEF4444))
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Chat Messages List
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(chatMessages) { msg ->
                    ChatBubble(message = msg)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Quick Replies Panel
            Text(
                text = "RESPUESTA RÁPIDA DE EMERGENCIA",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = 0.6f),
                modifier = Modifier.padding(bottom = 8.dp),
                letterSpacing = 1.sp
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // We show quick replies in 2 columns
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    quickReplies.take(2).forEach { text ->
                        QuickReplyPill(text = text) {
                            // Add user message
                            chatMessages = chatMessages + ChatMessage("usuario", text)
                            // Simulate response from Central
                            coroutineScope.launch {
                                delay(1500)
                                chatMessages = chatMessages + ChatMessage(
                                    "central",
                                    "Entendido. Registro su respuesta. Estamos compartiendo su estado con los paramédicos y contactos."
                                )
                            }
                        }
                    }
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    quickReplies.drop(2).forEach { text ->
                        QuickReplyPill(text = text) {
                            // Add user message
                            chatMessages = chatMessages + ChatMessage("usuario", text)
                            // Simulate response from Central
                            coroutineScope.launch {
                                delay(1500)
                                chatMessages = chatMessages + ChatMessage(
                                    "central",
                                    "Alerta crítica recibida. Se han enviado las coordenadas GPS a las unidades de auxilio más cercanas. Quédate en el vehículo si es seguro."
                                )
                            }
                        }
                    }
                }
            }

            // Cancel SOS Button
            Button(
                onClick = onCloseChat,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1C0A0A),
                    contentColor = Color.White
                )
            ) {
                Text("Desactivar Alerta / Finalizar SOS", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun ChatBubble(message: ChatMessage) {
    val isSystem = message.sender == "sistema"
    val isUser = message.sender == "usuario"
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        contentAlignment = when {
            isSystem -> Alignment.Center
            isUser -> Alignment.CenterEnd
            else -> Alignment.CenterStart
        }
    ) {
        Box(
            modifier = Modifier
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isUser) 16.dp else 4.dp,
                        bottomEnd = if (isUser) 4.dp else 16.dp
                    )
                )
                .background(
                    when {
                        isSystem -> Color.White.copy(alpha = 0.05f)
                        isUser -> Color(0xFFEF4444)
                        else -> Color(0xFF2C1616)
                    }
                )
                .border(
                    width = 1.dp,
                    color = when {
                        isSystem -> Color.White.copy(alpha = 0.1f)
                        isUser -> Color.Transparent
                        else -> Color(0xFFEF4444).copy(alpha = 0.2f)
                    },
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(horizontal = 14.dp, vertical = 10.dp)
                .widthIn(max = 280.dp)
        ) {
            Text(
                text = message.text,
                fontSize = 13.sp,
                color = if (isSystem) Color.White.copy(alpha = 0.6f) else Color.White,
                fontWeight = if (isSystem) FontWeight.Normal else FontWeight.Medium,
                textAlign = if (isSystem) TextAlign.Center else TextAlign.Start
            )
        }
    }
}

@Composable
fun QuickReplyPill(
    text: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF240E0E))
            .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.3f), RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )
    }
}

data class ChatMessage(val sender: String, val text: String)
