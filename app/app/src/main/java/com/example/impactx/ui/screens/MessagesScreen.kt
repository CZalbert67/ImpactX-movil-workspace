package com.example.impactx.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
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
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessagesScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // --- Data States ---
    var relationsList by remember { mutableStateOf<List<MonitoringRelationshipDto>>(emptyList()) }
    var selectedRelation by remember { mutableStateOf<MonitoringRelationshipDto?>(null) }
    var templatesList by remember { mutableStateOf<List<QuickMessageTemplateDto>>(emptyList()) }
    var selectedTemplate by remember { mutableStateOf<QuickMessageTemplateDto?>(null) }
    var chatHistory by remember { mutableStateOf<List<QuickMessageDto>>(emptyList()) }

    var isLoadingRelations by remember { mutableStateOf(true) }
    var isLoadingHistory by remember { mutableStateOf(false) }
    var isLoadingTemplates by remember { mutableStateOf(true) }
    var isSendingMessage by remember { mutableStateOf(false) }
    var myProfileId by remember { mutableStateOf("") }
    var myUsername by remember { mutableStateOf("") }
    
    // UI dropdown states
    var isRecipientDropdownExpanded by remember { mutableStateOf(false) }
    var isTemplateDropdownExpanded by remember { mutableStateOf(false) }
    var showNewTemplateDialog by remember { mutableStateOf(false) }
    var newTemplateTextInput by remember { mutableStateOf("") }

    // --- Fetch Operations ---
    fun refreshRelations() {
        isLoadingRelations = true
        scope.launch {
            try {
                val api = ApiClient.getApiService(context)
                val response = api.getMonitoringRelationships()
                if (response.isSuccessful) {
                    // Filter: Accepted relationships with SendMessages permission
                    relationsList = (response.body() ?: emptyList()).filter {
                        it.status.lowercase() == "accepted" && it.permissions.sendMessages
                    }
                }
            } catch (e: Exception) {
                // Ignore
            } finally {
                isLoadingRelations = false
            }
        }
    }

    fun refreshTemplates() {
        isLoadingTemplates = true
        scope.launch {
            try {
                val api = ApiClient.getApiService(context)
                val response = api.getQuickMessageTemplates()
                if (response.isSuccessful) {
                    templatesList = response.body() ?: emptyList()
                    if (selectedTemplate == null && templatesList.isNotEmpty()) {
                        selectedTemplate = templatesList.first()
                    }
                }
            } catch (e: Exception) {
                // Ignore
            } finally {
                isLoadingTemplates = false
            }
        }
    }

    fun refreshHistory(otherPublicProfileId: String) {
        isLoadingHistory = true
        scope.launch {
            try {
                val api = ApiClient.getApiService(context)
                val response = api.getQuickMessageHistory(otherPublicProfileId)
                if (response.isSuccessful) {
                    chatHistory = response.body() ?: emptyList()
                }
            } catch (e: Exception) {
                // Ignore
            } finally {
                isLoadingHistory = false
            }
        }
    }

    // Load initial data
    LaunchedEffect(Unit) {
        scope.launch {
            try {
                val api = ApiClient.getApiService(context)
                val response = api.getProfileUsername()
                if (response.isSuccessful) {
                    myProfileId = response.body()?.publicProfileId ?: ""
                    myUsername = response.body()?.username ?: ""
                }
            } catch (e: Exception) {
                // Ignore
            }
        }
        refreshRelations()
        refreshTemplates()
    }

    // Trigger history refresh when relation changes
    LaunchedEffect(selectedRelation, myProfileId) {
        val rel = selectedRelation
        if (rel != null && myProfileId.isNotEmpty()) {
            // Find which profile id to use for history (not mine)
            val otherProfileId = if (rel.monitorPublicProfileId == myProfileId) {
                rel.monitoredPublicProfileId
            } else {
                rel.monitorPublicProfileId
            }
            if (otherProfileId != null) {
                refreshHistory(otherProfileId)
            }
        } else {
            chatHistory = emptyList()
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
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Bar
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
                    text = "Mensajes Rápidos",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // --- RECIPIENT SELECTOR ---
            Text(
                text = "DESTINATARIO",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = GrayMuted,
                modifier = Modifier.align(Alignment.Start),
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(6.dp))

            Box(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF102238))
                        .clickable { isRecipientDropdownExpanded = true }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val rel = selectedRelation
                    if (rel != null) {
                        Column {
                            Text(
                                text = "@${rel.monitorUsername.ifEmpty { rel.monitoredUsername ?: "Usuario" }}",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Text(
                                text = rel.monitorName.ifEmpty { rel.monitoredName ?: "Relación Activa" },
                                color = GrayMuted,
                                fontSize = 12.sp
                            )
                        }
                    } else {
                        Text("Selecciona una persona...", color = GrayMuted, fontSize = 14.sp)
                    }
                    Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null, tint = TealPrimary)
                }

                DropdownMenu(
                    expanded = isRecipientDropdownExpanded,
                    onDismissRequest = { isRecipientDropdownExpanded = false },
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .background(Color(0xFF0F1E30))
                ) {
                    if (isLoadingRelations) {
                        DropdownMenuItem(
                            text = { Text("Cargando destinatarios...", color = Color.White) },
                            onClick = {}
                        )
                    } else if (relationsList.isEmpty()) {
                        DropdownMenuItem(
                            text = { Text("No tienes relaciones activas con chat", color = GrayMuted) },
                            onClick = {}
                        )
                    } else {
                        relationsList.forEach { rel ->
                            val username = rel.monitorUsername.ifEmpty { rel.monitoredUsername ?: "Usuario" }
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text("@$username", color = Color.White, fontWeight = FontWeight.Bold)
                                        Text(rel.monitorName.ifEmpty { rel.monitoredName ?: "Relación" }, color = GrayMuted, fontSize = 11.sp)
                                    }
                                },
                                onClick = {
                                    selectedRelation = rel
                                    isRecipientDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- TEMPLATE SELECTOR AND SEND BUTTON ---
            Text(
                text = "PLANTILLA DE ENVÍO RÁPIDO",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = GrayMuted,
                modifier = Modifier.align(Alignment.Start),
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF102238))
                            .clickable { isTemplateDropdownExpanded = true }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = selectedTemplate?.text ?: "Selecciona plantilla...",
                            color = Color.White,
                            fontSize = 14.sp,
                            maxLines = 1
                        )
                        Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null, tint = TealPrimary)
                    }

                    DropdownMenu(
                        expanded = isTemplateDropdownExpanded,
                        onDismissRequest = { isTemplateDropdownExpanded = false },
                        modifier = Modifier
                            .fillMaxWidth(0.6f)
                            .background(Color(0xFF0F1E30))
                    ) {
                        if (isLoadingTemplates) {
                            DropdownMenuItem(
                                text = { Text("Cargando...", color = Color.White) },
                                onClick = {}
                            )
                        } else {
                            templatesList.forEach { temp ->
                                DropdownMenuItem(
                                    text = { Text(temp.text, color = Color.White) },
                                    onClick = {
                                        selectedTemplate = temp
                                        isTemplateDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                Button(
                    onClick = {
                        val rel = selectedRelation
                        val temp = selectedTemplate
                        if (rel == null || temp == null) {
                            Toast.makeText(context, "Elige destinatario y plantilla", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        
                        val otherProfileId = if (rel.monitorPublicProfileId == myProfileId) {
                            rel.monitoredPublicProfileId
                        } else {
                            rel.monitorPublicProfileId
                        }

                        if (otherProfileId == null) return@Button

                        isSendingMessage = true
                        scope.launch {
                            try {
                                val api = ApiClient.getApiService(context)
                                val response = api.sendQuickMessage(
                                    SendQuickMessageRequest(
                                        recipientPublicProfileId = otherProfileId,
                                        publicTemplateId = temp.publicTemplateId
                                    )
                                )
                                if (response.isSuccessful) {
                                    Toast.makeText(context, "Mensaje enviado 🚀", Toast.LENGTH_SHORT).show()
                                    refreshHistory(otherProfileId)
                                } else {
                                    Toast.makeText(context, "Error al enviar: ${response.code()}", Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                Toast.makeText(context, "Error de red", Toast.LENGTH_SHORT).show()
                            } finally {
                                isSendingMessage = false
                            }
                        }
                    },
                    enabled = selectedRelation != null && selectedTemplate != null && !isSendingMessage,
                    modifier = Modifier.height(48.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
                ) {
                    if (isSendingMessage) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                    } else {
                        Text("Enviar", fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // --- CHAT HISTORY SECTION ---
            Text(
                text = "HISTORIAL DE CONVERSACIÓN",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = GrayMuted,
                modifier = Modifier.align(Alignment.Start),
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0C1929)),
                border = BorderStroke(1.dp, Color(0xFF102238))
            ) {
                if (selectedRelation == null) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "Selecciona un destinatario para cargar el chat.",
                            color = GrayMuted,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                } else if (isLoadingHistory) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = TealPrimary)
                    }
                } else if (chatHistory.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "No hay mensajes en este chat.",
                            color = GrayMuted,
                            fontSize = 13.sp
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        chatHistory.reversed().forEach { msg ->
                            val isOutgoing = msg.senderPublicProfileId == myProfileId || msg.senderUsername == myUsername
                            
                            val align = if (isOutgoing) Alignment.End else Alignment.Start
                            val bubbleColor = if (isOutgoing) Color(0xFF005C4B) else Color(0xFF202C33)

                            Column(
                                modifier = Modifier
                                    .align(align)
                                    .widthIn(max = 280.dp)
                            ) {
                                Card(
                                    shape = RoundedCornerShape(
                                        topStart = 12.dp,
                                        topEnd = 12.dp,
                                        bottomStart = if (isOutgoing) 12.dp else 0.dp,
                                        bottomEnd = if (isOutgoing) 0.dp else 12.dp
                                    ),
                                    colors = CardDefaults.cardColors(containerColor = bubbleColor),
                                    modifier = Modifier.align(align)
                                ) {
                                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                                        if (!isOutgoing) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "@${msg.senderUsername}",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = TealPrimary
                                                )
                                                if (!msg.isRead) {
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(4.dp))
                                                            .background(Color(0xFFF59E0B).copy(alpha = 0.2f))
                                                            .padding(horizontal = 4.dp, vertical = 1.dp)
                                                    ) {
                                                        Text("NUEVO", fontSize = 8.sp, color = Color(0xFFF59E0B), fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                        }

                                        Text(
                                            text = msg.text,
                                            color = Color.White,
                                            fontSize = 14.sp
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        
                                        // Time format
                                        val timeStr = try {
                                            val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                                            parser.timeZone = TimeZone.getTimeZone("UTC")
                                            val date = parser.parse(msg.sentAtUtc)
                                            val formatter = SimpleDateFormat("h:mm a", Locale.getDefault())
                                            formatter.format(date ?: Date())
                                        } catch (e: Exception) {
                                            ""
                                        }
                                        
                                        Row(
                                            modifier = Modifier.align(Alignment.End),
                                            horizontalArrangement = Arrangement.End,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = timeStr,
                                                fontSize = 9.sp,
                                                color = Color.White.copy(alpha = 0.6f)
                                            )
                                            if (isOutgoing) {
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "✓✓",
                                                    fontSize = 11.sp,
                                                    color = if (msg.isRead) Color(0xFF53BDEB) else Color.White.copy(alpha = 0.4f),
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                            if (!msg.isRead && !isOutgoing) {
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = "Marcar leído",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = TealPrimary,
                                                    modifier = Modifier.clickable {
                                                        scope.launch {
                                                            try {
                                                                val api = ApiClient.getApiService(context)
                                                                val response = api.markQuickMessageRead(msg.publicMessageId)
                                                                if (response.isSuccessful) {
                                                                    val otherProfileId = if (selectedRelation?.monitorPublicProfileId == myProfileId) {
                                                                        selectedRelation?.monitoredPublicProfileId
                                                                    } else {
                                                                        selectedRelation?.monitorPublicProfileId
                                                                    }
                                                                    if (otherProfileId != null) {
                                                                        refreshHistory(otherProfileId)
                                                                    }
                                                                }
                                                            } catch (e: Exception) {
                                                                // Ignore
                                                            }
                                                        }
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // --- TEMPLATES MANAGER SECTION ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "MIS PLANTILLAS PERSONALIZADAS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = GrayMuted,
                    letterSpacing = 1.sp
                )
                IconButton(
                    onClick = { showNewTemplateDialog = true },
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(TealPrimary)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Añadir", tint = Color.White, modifier = Modifier.size(16.dp))
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            // Templates list manager
            if (isLoadingTemplates) {
                CircularProgressIndicator(color = TealPrimary)
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    templatesList.forEach { temp ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF102238).copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (temp.isSystem) Color(0xFF00BFA5).copy(alpha = 0.15f) else Color(0xFF64748B).copy(alpha = 0.15f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = if (temp.isSystem) "OFICIAL" else "PROPIA",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (temp.isSystem) Color(0xFF00BFA5) else Color(0xFF94A3B8)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = temp.text,
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        maxLines = 2
                                    )
                                }

                                if (!temp.isSystem) {
                                    IconButton(
                                        onClick = {
                                            scope.launch {
                                                try {
                                                    val api = ApiClient.getApiService(context)
                                                    val response = api.deleteQuickMessageTemplate(temp.publicTemplateId)
                                                    if (response.isSuccessful) {
                                                        Toast.makeText(context, "Plantilla eliminada", Toast.LENGTH_SHORT).show()
                                                        refreshTemplates()
                                                    }
                                                } catch (e: Exception) {
                                                    Toast.makeText(context, "Error al eliminar plantilla", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Eliminar", tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // --- Create New Custom Template Dialog ---
    if (showNewTemplateDialog) {
        AlertDialog(
            onDismissRequest = { showNewTemplateDialog = false },
            title = { Text("Nueva Plantilla", color = Color.White) },
            text = {
                Column {
                    Text(
                        text = "Escribe un mensaje rápido y directo para enviar a tus monitores.",
                        color = GrayMuted,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = newTemplateTextInput,
                        onValueChange = { newTemplateTextInput = it },
                        label = { Text("Texto del mensaje") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TealPrimary,
                            unfocusedBorderColor = GrayMuted,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        maxLines = 3
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newTemplateTextInput.isBlank()) return@Button
                        scope.launch {
                            try {
                                val api = ApiClient.getApiService(context)
                                val response = api.createQuickMessageTemplate(
                                    UpsertQuickMessageTemplateRequest(
                                        text = newTemplateTextInput.trim(),
                                        sortOrder = templatesList.size
                                    )
                                )
                                if (response.isSuccessful) {
                                    Toast.makeText(context, "Plantilla creada con éxito!", Toast.LENGTH_SHORT).show()
                                    newTemplateTextInput = ""
                                    showNewTemplateDialog = false
                                    refreshTemplates()
                                } else {
                                    Toast.makeText(context, "Límite alcanzado o error", Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                Toast.makeText(context, "Error de red", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
                ) {
                    Text("Crear", color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showNewTemplateDialog = false },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    border = BorderStroke(1.dp, GrayMuted)
                ) {
                    Text("Cancelar")
                }
            },
            containerColor = Color(0xFF0C1929)
        )
    }
}
