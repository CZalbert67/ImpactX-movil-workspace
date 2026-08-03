package com.example.impactx.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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



@Composable
fun ContactsScreen(
    currentPlan: String,
    onNavigateBack: () -> Unit,
    onNavigateToPlans: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var activeTab by remember { mutableIntStateOf(0) } // 0 = Monitoreo, 1 = Contactos SOS

    // --- Tab 1: Monitoreo State ---
    var myUsername by remember { mutableStateOf("") }
    var myProfileId by remember { mutableStateOf("") }
    var manualCodeInput by remember { mutableStateOf("") }
    var inviteUsernameInput by remember { mutableStateOf("") }
    
    // Permission toggles for new invitation
    var permViewRoutes by remember { mutableStateOf(true) }
    var permViewLocation by remember { mutableStateOf(true) }
    var permViewEmergency by remember { mutableStateOf(true) }
    var permViewIncidents by remember { mutableStateOf(true) }
    var permReceiveAlerts by remember { mutableStateOf(true) }
    var permSendMessages by remember { mutableStateOf(true) }
    var permViewTelemetry by remember { mutableStateOf(true) }
    var permReceiveNotifications by remember { mutableStateOf(true) }

    var relationshipsList by remember { mutableStateOf<List<MonitoringRelationshipDto>>(emptyList()) }
    var isLoadingMonitors by remember { mutableStateOf(true) }
    var showInvitationCodeDialog by remember { mutableStateOf<String?>(null) }

    // --- Tab 2: Contactos SOS State ---
    var newContactName by remember { mutableStateOf("") }
    var newContactPhone by remember { mutableStateOf("") }
    var contactsList by remember { mutableStateOf<List<ContactoDto>>(emptyList()) }
    var isLoadingContacts by remember { mutableStateOf(true) }
    var isSavingContact by remember { mutableStateOf(false) }

    // --- Actions ---
    fun refreshMonitors() {
        isLoadingMonitors = true
        scope.launch {
            try {
                val api = ApiClient.getApiService(context)
                val response = api.getMonitoringRelationships()
                if (response.isSuccessful) {
                    relationshipsList = response.body() ?: emptyList()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error de red al cargar relaciones", Toast.LENGTH_SHORT).show()
            } finally {
                isLoadingMonitors = false
            }
        }
    }

    fun refreshContacts() {
        isLoadingContacts = true
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
                isLoadingContacts = false
            }
        }
    }

    // Load username info
    LaunchedEffect(Unit) {
        scope.launch {
            try {
                val api = ApiClient.getApiService(context)
                val response = api.getProfileUsername()
                if (response.isSuccessful) {
                    myUsername = response.body()?.username ?: ""
                    myProfileId = response.body()?.publicProfileId ?: ""
                }
            } catch (e: Exception) {
                // Ignore
            }
        }
        refreshMonitors()
        refreshContacts()
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
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
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
                    text = "Monitoreo y Contactos",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Tab Selector
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF102238))
                    .padding(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (activeTab == 0) TealPrimary else Color.Transparent)
                        .clickable { activeTab = 0 }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Monitoreo",
                        fontWeight = FontWeight.Bold,
                        color = if (activeTab == 0) Color.White else GrayMuted,
                        fontSize = 14.sp
                    )
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (activeTab == 1) TealPrimary else Color.Transparent)
                        .clickable { activeTab = 1 }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Contactos SOS",
                        fontWeight = FontWeight.Bold,
                        color = if (activeTab == 1) Color.White else GrayMuted,
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // --- TAB CONTENT ---
            if (activeTab == 0) {
                // --- MONITOREO TAB ---
                
                // My Invitation Code Section
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF102238))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Mi Código de Invitación",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Comparte este código para que otros te agreguen como monitor.",
                            color = GrayMuted,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                        

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF0A1624))
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (myProfileId.isEmpty()) "Cargando código..." else myProfileId,
                                color = TealPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            if (myProfileId.isNotEmpty()) {
                                Button(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        val clip = ClipData.newPlainText("ImpactX Profile ID", myProfileId)
                                        clipboard.setPrimaryClip(clip)
                                        Toast.makeText(context, "Código copiado", Toast.LENGTH_SHORT).show()
                                    },
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    shape = RoundedCornerShape(6.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Text(
                                        text = "Copiar",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Código Manual Recibido (Aceptar invitaciones compartidas)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF102238).copy(alpha = 0.6f)),
                    border = BorderStroke(1.dp, Color(0xFF102238))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Código Manual Recibido",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        OutlinedTextField(
                            value = manualCodeInput,
                            onValueChange = { manualCodeInput = it },
                            label = { Text("Pegar código de invitación") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = TealPrimary,
                                unfocusedBorderColor = GrayMuted,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            singleLine = true
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Button(
                            onClick = {
                                if (manualCodeInput.isBlank()) return@Button
                                scope.launch {
                                    try {
                                        val api = ApiClient.getApiService(context)
                                        val response = api.acceptMonitoringInvitation(
                                            AcceptMonitoringInvitationRequest(code = manualCodeInput.trim().uppercase())
                                        )
                                        if (response.isSuccessful) {
                                            Toast.makeText(context, "Invitación aceptada con éxito!", Toast.LENGTH_SHORT).show()
                                            manualCodeInput = ""
                                            refreshMonitors()
                                        } else {
                                            Toast.makeText(context, "Código inválido o expirado", Toast.LENGTH_SHORT).show()
                                        }
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Error de red", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
                        ) {
                            Text("Aceptar Invitación", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Invitar a Monitoreo Form
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF102238).copy(alpha = 0.8f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Invitar a Monitoreo",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        OutlinedTextField(
                            value = inviteUsernameInput,
                            onValueChange = { inviteUsernameInput = it },
                            label = { Text("Nombre de usuario a invitar") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = TealPrimary,
                                unfocusedBorderColor = GrayMuted,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Permisos Iniciales",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 12.sp
                        )
                        
                        // Permisos Checklist Grid
                        Column(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                             val items = listOf(
                                 Triple("Rutas", permViewRoutes) { valChecked: Boolean -> permViewRoutes = valChecked },
                                 Triple("Ubicación", permViewLocation) { valChecked: Boolean -> permViewLocation = valChecked },
                                 Triple("Ubicación de Emergencia", permViewEmergency) { valChecked: Boolean -> permViewEmergency = valChecked },
                                 Triple("Incidentes", permViewIncidents) { valChecked: Boolean -> permViewIncidents = valChecked },
                                 Triple("Alertas Críticas", permReceiveAlerts) { valChecked: Boolean -> permReceiveAlerts = valChecked },
                                 Triple("Enviar Mensajes", permSendMessages) { valChecked: Boolean -> permSendMessages = valChecked },
                                 Triple("Telemetría", permViewTelemetry) { valChecked: Boolean -> permViewTelemetry = valChecked },
                                 Triple("Notificaciones", permReceiveNotifications) { valChecked: Boolean -> permReceiveNotifications = valChecked }
                             )
                            items.chunked(2).forEach { pair ->
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    pair.forEach { (label, value, onChecked) ->
                                        Row(
                                            modifier = Modifier.weight(1f).padding(vertical = 2.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Checkbox(
                                                checked = value,
                                                onCheckedChange = onChecked,
                                                colors = CheckboxDefaults.colors(checkedColor = TealPrimary)
                                            )
                                            Text(label, color = Color.White, fontSize = 11.sp, maxLines = 1)
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Button(
                            onClick = {
                                if (inviteUsernameInput.isBlank()) {
                                    Toast.makeText(context, "Ingresa un nombre de usuario", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                scope.launch {
                                    try {
                                        val api = ApiClient.getApiService(context)
                                        val request = CreateMonitoringInvitationRequest(
                                            username = inviteUsernameInput.trim(),
                                            permissions = MonitoringPermissionsRequest(
                                                viewRoutes = permViewRoutes,
                                                viewLocation = permViewLocation,
                                                viewEmergencyLocation = permViewEmergency,
                                                viewIncidents = permViewIncidents,
                                                receiveCriticalAlerts = permReceiveAlerts,
                                                sendMessages = permSendMessages,
                                                viewTelemetry = permViewTelemetry,
                                                receiveNotifications = permReceiveNotifications
                                            )
                                        )
                                        val response = api.createMonitoringInvitation(request)
                                        if (response.isSuccessful) {
                                            val body = response.body()
                                            showInvitationCodeDialog = body?.manualCode
                                            inviteUsernameInput = ""
                                            refreshMonitors()
                                        } else {
                                            Toast.makeText(context, "Usuario no encontrado o conflicto", Toast.LENGTH_SHORT).show()
                                        }
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Error de red", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
                        ) {
                            Text("Crear Invitación", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Relations Title
                Text(
                    text = "RELACIONES DE MONITOREO",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = GrayMuted,
                    modifier = Modifier.align(Alignment.Start),
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(10.dp))

                // Relations List
                if (isLoadingMonitors) {
                    CircularProgressIndicator(color = TealPrimary, modifier = Modifier.padding(24.dp))
                } else if (relationshipsList.isEmpty()) {
                    Text(
                        text = "Aún no tienes relaciones de monitoreo.",
                        color = GrayMuted,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                } else {
                    relationshipsList.forEach { relation ->
                        // Distinguish direction
                        val isMyMonitor = relation.monitorPublicProfileId != myProfileId
                        val title = if (isMyMonitor) "Monitorea mi cuenta" else "Cuenta que monitoreo"
                        val userLabel = if (isMyMonitor) relation.monitorUsername else (relation.monitoredUsername ?: "Invitado")
                        val code = if (isMyMonitor) relation.monitorPublicProfileId else (relation.monitoredPublicProfileId ?: "")

                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF102238).copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = "@$userLabel",
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                fontSize = 16.sp
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            
                                            // Status Badge
                                            val badgeBg = when (relation.status.lowercase()) {
                                                "accepted" -> Color(0xFF22C55E).copy(alpha = 0.15f)
                                                "pending" -> Color(0xFFF59E0B).copy(alpha = 0.15f)
                                                else -> Color(0xFFEF4444).copy(alpha = 0.15f)
                                            }
                                            val badgeFg = when (relation.status.lowercase()) {
                                                "accepted" -> Color(0xFF22C55E)
                                                "pending" -> Color(0xFFF59E0B)
                                                else -> Color(0xFFEF4444)
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(badgeBg)
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = relation.status.uppercase(),
                                                    fontSize = 10.sp,
                                                    color = badgeFg,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                        Text(
                                            text = title,
                                            fontSize = 12.sp,
                                            color = GrayMuted,
                                            modifier = Modifier.padding(top = 2.dp)
                                        )
                                    }

                                    // Action Button
                                    if (relation.status.lowercase() == "accepted" || relation.status.lowercase() == "pending") {
                                        IconButton(
                                            onClick = {
                                                scope.launch {
                                                    try {
                                                        val api = ApiClient.getApiService(context)
                                                        val response = api.revokeMonitoringRelationship(relation.publicRelationshipId)
                                                        if (response.isSuccessful) {
                                                            Toast.makeText(context, "Relación revocada", Toast.LENGTH_SHORT).show()
                                                            refreshMonitors()
                                                        }
                                                    } catch (e: Exception) {
                                                        Toast.makeText(context, "Error al revocar", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Revocar",
                                                tint = Color(0xFFEF4444)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))
                                
                                // Render Permissions summary
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    val permissionsText = listOf(
                                        "Rutas" to relation.permissions.viewRoutes,
                                        "Ubicación" to relation.permissions.viewLocation,
                                        "Telemetría" to relation.permissions.viewTelemetry,
                                        "Mensajes" to relation.permissions.sendMessages
                                    )
                                    permissionsText.forEach { (permName, isGranted) ->
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(if (isGranted) Color(0xFF00BFA5).copy(alpha = 0.1f) else Color(0xFFEF4444).copy(alpha = 0.1f))
                                                .padding(horizontal = 6.dp, vertical = 3.dp)
                                        ) {
                                            Text(
                                                text = "$permName: ${if (isGranted) "Sí" else "No"}",
                                                fontSize = 9.sp,
                                                color = if (isGranted) Color(0xFF00BFA5) else Color(0xFFEF4444),
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

            } else {
                // --- TAB 2: CONTACTOS SOS TAB ---
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
                            Text("Contactos de Respaldo", fontWeight = FontWeight.Bold, color = Color.White)
                            Text(
                                text = "Contactos manuales registrados: ${contactsList.size}",
                                fontSize = 12.sp,
                                color = GrayMuted
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Traditional SOS Contacts List
                if (isLoadingContacts) {
                    CircularProgressIndicator(color = TealPrimary, modifier = Modifier.padding(24.dp))
                } else if (contactsList.isEmpty()) {
                    Text(
                        text = "Aún no tienes contactos manuales registrados.",
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
                                                            Toast.makeText(context, "Contacto SOS eliminado", Toast.LENGTH_SHORT).show()
                                                            refreshContacts()
                                                        }
                                                    } catch (e: Exception) {
                                                        Toast.makeText(context, "Error al eliminar contacto", Toast.LENGTH_SHORT).show()
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

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "AÑADIR NUEVO CONTACTO SOS",
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
                            if (isSavingContact) return@Button
                            isSavingContact = true
                            scope.launch {
                                try {
                                    val api = ApiClient.getApiService(context)
                                    val response = api.createContact(
                                        CreateContactoRequest(
                                            nombre = newContactName.trim(),
                                            telefono = newContactPhone.trim(),
                                            parentesco = "Contacto",
                                            priority = if (contactsList.isEmpty()) "Principal" else "Secundario",
                                            esPrincipal = contactsList.isEmpty()
                                        )
                                    )
                                    if (response.isSuccessful) {
                                        Toast.makeText(context, "Contacto SOS guardado", Toast.LENGTH_SHORT).show()
                                        newContactName = ""
                                        newContactPhone = ""
                                        refreshContacts()
                                    }
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Error de red al guardar contacto", Toast.LENGTH_SHORT).show()
                                } finally {
                                    isSavingContact = false
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
                ) {
                    if (isSavingContact) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Text("Guardar Contacto SOS", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // --- Invitation Manual Code Dialog ---
    if (showInvitationCodeDialog != null) {
        val code = showInvitationCodeDialog!!
        AlertDialog(
            onDismissRequest = { showInvitationCodeDialog = null },
            title = { Text("Código de Invitación", color = Color.White) },
            text = {
                Column {
                    Text(
                        text = "El código se mostrará una sola vez. Compártelo con tu monitor.",
                        color = GrayMuted,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF102238))
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = code,
                            color = TealPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            text = "COPIAR",
                            color = TealPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            modifier = Modifier
                                .clickable {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("ImpactX Manual Code", code)
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, "Código copiado", Toast.LENGTH_SHORT).show()
                                }
                                .padding(8.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showInvitationCodeDialog = null },
                    colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
                ) {
                    Text("Cerrar", color = Color.White)
                }
            },
            containerColor = Color(0xFF0C1929)
        )
    }
}
