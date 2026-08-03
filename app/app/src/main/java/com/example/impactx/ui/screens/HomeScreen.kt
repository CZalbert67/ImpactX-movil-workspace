package com.example.impactx.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Close
import com.example.impactx.data.remote.*
import kotlinx.coroutines.launch
import android.widget.Toast

@Composable
fun HomeScreen(
    currentPlan: String,
    userName: String,
    userId: String,
    onUserNameChange: (String) -> Unit,
    onNavigateToMedical: () -> Unit,
    onNavigateToVehicle: () -> Unit,
    onNavigateToContacts: () -> Unit,
    onNavigateToPlans: () -> Unit,
    onNavigateToWearableSync: () -> Unit,
    onNavigateToMessages: () -> Unit,
    onNavigateToProfile: () -> Unit
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

    // User name edit dialog states
    var showEditNameDialog by remember { mutableStateOf(false) }
    var newNameInput by remember { mutableStateOf(userName) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showNotificationsDialog by remember { mutableStateOf(false) }
    var unreadNotificationsCount by remember { mutableIntStateOf(0) }
    var notificationsList by remember { mutableStateOf<List<NotificacionDto>>(emptyList()) }
    var isLoadingNotifications by remember { mutableStateOf(false) }

    fun refreshNotifications() {
        isLoadingNotifications = true
        scope.launch {
            try {
                val api = ApiClient.getApiService(context)
                val response = api.getNotifications()
                if (response.isSuccessful) {
                    notificationsList = response.body() ?: emptyList()
                }
                
                val countResponse = api.getUnreadNotificationsCount()
                if (countResponse.isSuccessful) {
                    unreadNotificationsCount = countResponse.body()?.get("noLeidas") ?: 0
                }
            } catch (e: Exception) {
                // Silent catch
            } finally {
                isLoadingNotifications = false
            }
        }
    }

    LaunchedEffect(Unit) {
        refreshNotifications()
    }

    // Initials helper
    val initials = remember(userName) {
        userName.split(" ")
            .filter { it.isNotBlank() }
            .take(2)
            .map { it.take(1).uppercase() }
            .joinToString("")
            .ifEmpty { "U" }
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
                    // Profile initials avatar (Click logs out)
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(TealPrimary)
                            .clickable { onNavigateToProfile() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = initials,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable {
                                newNameInput = userName
                                showEditNameDialog = true
                            }
                        ) {
                            Text(
                                text = "Hola, $userName",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "✏️",
                                fontSize = 12.sp,
                                color = TealPrimary
                            )
                        }
                        
                        // Static User ID Display
                        Text(
                            text = "ID: $userId",
                            fontSize = 11.sp,
                            color = GrayMuted,
                            fontWeight = FontWeight.Normal,
                            modifier = Modifier.padding(vertical = 1.dp)
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

                Box {
                    IconButton(
                        onClick = {
                            refreshNotifications()
                            showNotificationsDialog = true
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Notificaciones",
                            tint = if (unreadNotificationsCount > 0) TealPrimary else Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    if (unreadNotificationsCount > 0) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(Color.Red)
                                .align(Alignment.TopEnd)
                                .offset(x = (-2).dp, y = 2.dp)
                        )
                    }
                }
            }

            val isConnected = WearableManager.bleState == BLEState.CONNECTED_DASHBOARD
            
            // Pulse animation for the heart icon on the card
            val infiniteTransitionCard = rememberInfiniteTransition(label = "heartPulseCard")
            val heartScaleCard by infiniteTransitionCard.animateFloat(
                initialValue = 1f,
                targetValue = 1.25f,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = if (isConnected) (60000 / WearableManager.realHeartRate.coerceAtLeast(30)) else 800,
                        easing = FastOutSlowInEasing
                    ),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "scale"
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Main Status Banner (Clickable to sync wearable)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToWearableSync() },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isConnected) Color(0xFF0F3A2E) else Color(0xFF102238)
                ),
                border = if (isConnected) BorderStroke(1.dp, Color(0xFF22C55E).copy(alpha = 0.4f)) else null
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "SMARTWATCH",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isConnected) Color(0xFF22C55E) else TealPrimary,
                            letterSpacing = 1.sp
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (isConnected) Color(0xFF22C55E).copy(alpha = 0.15f) else TealPrimary.copy(alpha = 0.15f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (isConnected) "🟢 VINCULADO" else "DESCONECTADO",
                                fontSize = 9.sp,
                                color = if (isConnected) Color(0xFF22C55E) else TealPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    if (isConnected) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Pulsing heart
                            Box(
                                modifier = Modifier
                                    .scale(heartScaleCard)
                                    .size(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("❤️", fontSize = 24.sp)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "${WearableManager.realHeartRate} BPM",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Batería del reloj: ${WearableManager.realBatteryLevel}%",
                                    fontSize = 12.sp,
                                    color = GrayMuted
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Dispositivo: ${WearableManager.connectedDeviceName ?: "Galaxy Watch"}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                        Text(
                            text = "Presiona para administrar la conexión y ver telemetría completa.",
                            fontSize = 11.sp,
                            color = GrayMuted
                        )
                    } else {
                        Text(
                            text = "Vincular Reloj / Sensores BLE",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Configura y diagnostica la telemetría física cardíaca y G-Force en tiempo real.",
                            fontSize = 12.sp,
                            color = GrayMuted,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Central Shield Status View
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp),
                contentAlignment = Alignment.Center
            ) {
                // Outer Pulse Ring
                Box(
                    modifier = Modifier
                        .size(pulseSize.dp)
                        .clip(CircleShape)
                        .background(TealPrimary.copy(alpha = pulseAlpha))
                )

                // Main Core Shield Circle
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(TealPrimary, Color(0xFF004D4D))
                            )
                        )
                        .border(2.dp, Color.White.copy(alpha = 0.3f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "🛡️",
                            fontSize = 42.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "PROTEGIDO",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            letterSpacing = 1.sp
                        )
                        if (isConnected) {
                            Text(
                                text = "❤️ ${WearableManager.realHeartRate} BPM",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF22C55E),
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Sub-status text
            Text(
                text = "Monitoreo de colisión en segundo plano activo.",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            Text(
                text = "El viaje se iniciará de forma automática al comenzar la actividad desde tu Smartwatch.",
                color = GrayMuted,
                fontSize = 11.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(top = 6.dp),
                textAlign = TextAlign.Center,
                lineHeight = 15.sp
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Navigation Options List
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Medical Profile Card
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
                        Text("❤️", fontSize = 20.sp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Ficha Médica de Emergencia", fontWeight = FontWeight.Bold, color = Color.White)
                            Text("Tipo de sangre, alergias y notas", fontSize = 12.sp, color = GrayMuted)
                        }
                    }
                    Text("›", fontSize = 24.sp, color = GrayMuted)
                }

                // Vehicle Profile Card
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
                            Text("Mi Vehículo Registrado", fontWeight = FontWeight.Bold, color = Color.White)
                            Text("Modelo, placas y velocidad habitual", fontSize = 12.sp, color = GrayMuted)
                        }
                    }
                    Text("›", fontSize = 24.sp, color = GrayMuted)
                }

                // Contacts and Monitors Card
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

                Spacer(modifier = Modifier.height(12.dp))

                // Messages and Chat Card
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF102238))
                        .clickable { onNavigateToMessages() }
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("💬", fontSize = 20.sp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Mensajes de Emergencia", fontWeight = FontWeight.Bold, color = Color.White)
                            Text("Chat rápido de plantillas", fontSize = 12.sp, color = GrayMuted)
                        }
                    }
                    Text("›", fontSize = 24.sp, color = GrayMuted)
                }
            }
        }
    }

    // Edit Name Dialog overlay
    if (showEditNameDialog) {
        AlertDialog(
            onDismissRequest = { showEditNameDialog = false },
            title = {
                Text(
                    text = "Editar Nombre",
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            },
            text = {
                Column {
                    Text(
                        text = "ID de Usuario (Inmutable): $userId",
                        fontSize = 12.sp,
                        color = GrayMuted,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    OutlinedTextField(
                        value = newNameInput,
                        onValueChange = { newNameInput = it },
                        label = { Text("Nombre Completo") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TealPrimary,
                            unfocusedBorderColor = GrayMuted,
                            focusedLabelColor = TealPrimary,
                            unfocusedLabelColor = GrayMuted,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newNameInput.isNotBlank()) {
                            onUserNameChange(newNameInput.trim())
                            showEditNameDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
                ) {
                    Text("Guardar", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showEditNameDialog = false },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
                ) {
                    Text("Cancelar")
                }
            },
            containerColor = Color(0xFF102238),
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Notifications Dialog Overlay
    if (showNotificationsDialog) {
        AlertDialog(
            onDismissRequest = { showNotificationsDialog = false },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🔔", fontSize = 20.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Notificaciones",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 18.sp
                        )
                    }
                    IconButton(onClick = { showNotificationsDialog = false }) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color.White)
                    }
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxHeight(0.6f).fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Marcar todas como leídas",
                            color = TealPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clickable {
                                    scope.launch {
                                        try {
                                            val api = ApiClient.getApiService(context)
                                            val response = api.markAllNotificationsAsRead()
                                            if (response.isSuccessful) {
                                                refreshNotifications()
                                            }
                                        } catch (e: Exception) {
                                            // Silent catch
                                        }
                                    }
                                }
                                .padding(vertical = 4.dp, horizontal = 2.dp)
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = "Borrar todo",
                            color = Color(0xFFEF4444),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clickable {
                                    scope.launch {
                                        try {
                                            val api = ApiClient.getApiService(context)
                                            val response = api.deleteAllNotifications()
                                            if (response.isSuccessful) {
                                                refreshNotifications()
                                            }
                                        } catch (e: Exception) {
                                            // Silent catch
                                        }
                                    }
                                }
                                .padding(vertical = 4.dp, horizontal = 2.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (isLoadingNotifications) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = TealPrimary)
                        }
                    } else if (notificationsList.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                text = "No tienes notificaciones nuevas.",
                                color = GrayMuted,
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            notificationsList.forEach { notification ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            if (!notification.leida) {
                                                scope.launch {
                                                    try {
                                                        val api = ApiClient.getApiService(context)
                                                        api.toggleNotificationRead(notification.id, ToggleReadRequest(leida = true))
                                                        refreshNotifications()
                                                    } catch (e: Exception) {
                                                        // Silent catch
                                                    }
                                                }
                                            }
                                        },
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (notification.leida) Color(0xFF162A45) else Color(0xFF1E3A5F)
                                    ),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (!notification.leida) {
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .clip(CircleShape)
                                                    .background(TealPrimary)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                        }
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = notification.titulo,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                fontSize = 13.sp
                                            )
                                            Text(
                                                text = notification.mensaje,
                                                color = if (notification.leida) GrayMuted else Color.White,
                                                fontSize = 12.sp,
                                                modifier = Modifier.padding(top = 2.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(6.dp))
                                        IconButton(
                                            onClick = {
                                                scope.launch {
                                                    try {
                                                        val api = ApiClient.getApiService(context)
                                                        val response = api.deleteNotification(notification.id)
                                                        if (response.isSuccessful) {
                                                            refreshNotifications()
                                                        }
                                                    } catch (e: Exception) {
                                                        // Silent catch
                                                    }
                                                }
                                            },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Borrar",
                                                tint = Color(0xFFEF4444).copy(alpha = 0.8f),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            containerColor = Color(0xFF0C1929),
            shape = RoundedCornerShape(16.dp)
        )
    }
}
