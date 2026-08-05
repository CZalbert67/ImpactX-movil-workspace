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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import com.example.impactx.data.remote.*
import kotlinx.coroutines.launch
import android.widget.Toast
import android.content.Context

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
    onNavigateToProfile: () -> Unit,
    onNavigateToMandarDatos: () -> Unit,
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
        val savedTripId = context
            .getSharedPreferences("impactx_prefs", Context.MODE_PRIVATE)
            .getString("active_trip_id", null)
        if (!savedTripId.isNullOrBlank()) {
            WearableManager.activeWearTripId = savedTripId
        }
    }

    val activeTripId = WearableManager.activeWearTripId
    val isTripActive = !activeTripId.isNullOrBlank()

    // Initials helper
    val initials = remember(userName) {
        userName.split(" ")
            .filter { it.isNotBlank() }
            .take(2)
            .map { it.take(1).uppercase() }
            .joinToString("")
            .ifEmpty { "U" }
    }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = CardBgColor,
                modifier = Modifier.width(280.dp),
                drawerShape = RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp)
                ) {
                    Text(
                        text = "Impact.X",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TealPrimary,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 24.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(TealPrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = initials,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = userName,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimaryColor
                            )
                            Text(
                                text = "Plan $currentPlan",
                                fontSize = 12.sp,
                                color = if (currentPlan == "Básico") Color(0xFFF59E0B) else Color(0xFF22C55E),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Divider(color = GrayMuted.copy(alpha = 0.2f), modifier = Modifier.padding(bottom = 16.dp))

                    val menuItems: List<Triple<String, ImageVector, () -> Unit>> = listOf(
                        Triple("Inicio", Icons.Default.Home, { scope.launch { drawerState.close() }; Unit }),
                        Triple("Ficha Médica", Icons.Default.Favorite, { scope.launch { drawerState.close() }; onNavigateToMedical() }),
                        Triple("Mi Vehículo", Icons.Default.Info, { scope.launch { drawerState.close() }; onNavigateToVehicle() }),
                        Triple("Contactos y SOS", Icons.Default.Share, { scope.launch { drawerState.close() }; onNavigateToContacts() }),
                        Triple("Mensajes Rápidos", Icons.Default.Send, { scope.launch { drawerState.close() }; onNavigateToMessages() }),
                        Triple("Sincronizar Reloj", Icons.Default.Refresh, { scope.launch { drawerState.close() }; onNavigateToWearableSync() }),
                        Triple("Mandar Datos", Icons.Default.ArrowUpward, { scope.launch { drawerState.close() }; onNavigateToMandarDatos() }),
                        Triple("Mis Planes", Icons.Default.Star, { scope.launch { drawerState.close() }; onNavigateToPlans() }),
                        Triple("Mi Perfil", Icons.Default.Person, { scope.launch { drawerState.close() }; onNavigateToProfile() }),
                        Triple("Cerrar Sesión", Icons.Default.ExitToApp, { scope.launch { drawerState.close() }; onLogout() })
                    )

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        menuItems.forEach { (title, icon, action) ->
                            NavigationDrawerItem(
                                label = { Text(text = title, fontWeight = FontWeight.Medium, color = TextPrimaryColor) },
                                selected = false,
                                onClick = action,
                                icon = { Icon(imageVector = icon, contentDescription = title, tint = TealPrimary) },
                                colors = NavigationDrawerItemDefaults.colors(
                                    unselectedContainerColor = Color.Transparent
                                )
                            )
                        }
                    }

                    Divider(color = GrayMuted.copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 16.dp))

                    Text(
                        text = "DISEÑO / TEMA",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = GrayMuted,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        AppTheme.values().forEach { theme ->
                            val isSelected = ThemeConfig.currentTheme == theme
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) TealPrimary else CardElevatedColor)
                                    .clickable { ThemeConfig.currentTheme = theme }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = when (theme) {
                                        AppTheme.IMPACTX_NEON -> "Neon"
                                        AppTheme.PROFESSIONAL -> "Pro"
                                        AppTheme.CLARO -> "Claro"
                                    },
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.Black else TextSecondaryColor
                                )
                            }
                        }
                    }
                }
            }
        }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(DarkBlue, DarkBlueEnd)
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

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Abrir menú",
                                tint = TealPrimary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
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
                                color = TextPrimaryColor
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Editar",
                                tint = TealPrimary,
                                modifier = Modifier.size(16.dp)
                            )
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
                                tint = if (unreadNotificationsCount > 0) TealPrimary else TextPrimaryColor,
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

                val isConnected = WearableManager.isRealConnection || WearableManager.bleState == BLEState.CONNECTED_DASHBOARD
                val isBackendLinked = WearableManager.backendLinked && !WearableManager.backendDeviceId.isNullOrBlank()
                
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

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToWearableSync() },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isConnected) Color(0xFF0F3A2E) else CardBgColor
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
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(if (isConnected) Color(0xFF22C55E) else TealPrimary)
                                )
                                Text(
                                    text = when {
                                        isTripActive -> "VIAJE ACTIVO"
                                        isBackendLinked -> "VINCULADO CON IMPACTX"
                                        isConnected -> "CONECTADO · VINCULANDO"
                                        else -> "DISPOSITIVO DESCONECTADO"
                                    },
                                    fontSize = 9.sp,
                                    color = if (isConnected || isTripActive) Color(0xFF22C55E) else TealPrimary,
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
                                Icon(
                                    imageVector = Icons.Default.Favorite,
                                    contentDescription = "Ritmo cardíaco",
                                    tint = Color.Red,
                                    modifier = Modifier
                                        .scale(heartScaleCard)
                                        .size(32.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "${WearableManager.realHeartRate} BPM",
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimaryColor
                                    )
                                    Text(
                                        text = "Batería del reloj: ${WearableManager.realBatteryLevel}%",
                                        fontSize = 12.sp,
                                        color = TextSecondaryColor
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Dispositivo: ${WearableManager.connectedDeviceName ?: "Galaxy Watch"}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimaryColor
                            )
                            Text(
                                text = when {
                                    isBackendLinked -> "Vinculación confirmada. Presiona para ver la telemetría."
                                    !WearableManager.pairingError.isNullOrBlank() -> WearableManager.pairingError!!
                                    else -> "Conexión detectada. Esperando confirmación con ImpactX..."
                                },
                                fontSize = 11.sp,
                                color = if (WearableManager.pairingError.isNullOrBlank()) TextSecondaryColor else Color(0xFFEF5350)
                            )
                        } else {
                            Text(
                                text = "Vincular Reloj / Sensores BLE",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimaryColor
                            )
                            Text(
                                text = "Configura y diagnostica la telemetría física cardíaca y G-Force en tiempo real.",
                                fontSize = 12.sp,
                                color = TextSecondaryColor,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(pulseSize.dp)
                            .clip(CircleShape)
                            .background(TealPrimary.copy(alpha = pulseAlpha))
                    )

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
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = if (isTripActive) "Viaje activo" else "Protección activa",
                                tint = Color.White,
                                modifier = Modifier.size(42.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (isTripActive) "EN VIAJE" else "PROTEGIDO",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                letterSpacing = 1.sp
                            )
                            if (isConnected) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                                    modifier = Modifier.padding(top = 2.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Favorite,
                                        contentDescription = null,
                                        tint = Color(0xFF22C55E),
                                        modifier = Modifier.size(10.dp)
                                    )
                                    Text(
                                        text = "${WearableManager.realHeartRate} BPM",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF22C55E)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = if (isTripActive) "Viaje activo sincronizado con ImpactX." else "Monitoreo de colisión en segundo plano activo.",
                    color = TextPrimaryColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = if (isTripActive) {
                        "El viaje iniciado desde el Galaxy Watch8 ya está registrado. ID: ${activeTripId?.take(8)}…"
                    } else {
                        "El viaje se inicia manualmente desde tu Galaxy Watch8. La app sincroniza el estado con el backend ImpactX."
                    },
                    color = TextSecondaryColor,
                    fontSize = 11.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(top = 6.dp),
                    textAlign = TextAlign.Center,
                    lineHeight = 15.sp
                )
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
            containerColor = CardBgColor,
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
            containerColor = CardElevatedColor,
            shape = RoundedCornerShape(16.dp)
        )
    }
}
