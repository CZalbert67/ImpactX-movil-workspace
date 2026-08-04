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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ContentCopy
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
    onNavigateToPlans: () -> Unit,
    onNavigateToMessages: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var activeTab by remember { mutableIntStateOf(0) } // 0 = Mi Grupo, 1 = Permisos

    // Profile state
    var myProfileId by remember { mutableStateOf("") }
    var myUsername by remember { mutableStateOf("") }

    // Family Summary
    var familySummary by remember { mutableStateOf<FamilySubscriptionSummaryDto?>(null) }
    var isLoadingSummary by remember { mutableStateOf(true) }

    // Members list
    var membersList by remember { mutableStateOf<List<FamilyMemberDto>>(emptyList()) }
    var isLoadingMembers by remember { mutableStateOf(true) }

    // Invitations
    var sentInvitations by remember { mutableStateOf<List<FamilyInvitationDto>>(emptyList()) }
    var isLoadingSentInvites by remember { mutableStateOf(true) }

    var incomingInvitations by remember { mutableStateOf<List<IncomingFamilyInvitationDto>>(emptyList()) }
    var isLoadingIncomingInvites by remember { mutableStateOf(true) }

    // Access list (Permissions)
    var accessList by remember { mutableStateOf<List<FamilyMemberAccessDto>>(emptyList()) }
    var isLoadingAccess by remember { mutableStateOf(true) }

    // Inputs for invite
    var inviteUsername by remember { mutableStateOf("") }
    var inviteEmail by remember { mutableStateOf("") }
    var isSendingInvite by remember { mutableStateOf(false) }

    // Input for redeem code
    var redeemCodeInput by remember { mutableStateOf("") }
    var isRedeemingCode by remember { mutableStateOf(false) }

    // Dialog code for newly created invitation
    var showManualCodeDialog by remember { mutableStateOf<String?>(null) }

    // Refresh action
    fun refreshAll() {
        scope.launch {
            try {
                val api = ApiClient.getApiService(context)
                
                // Fetch profile
                val profileRes = api.getProfileUsername()
                if (profileRes.isSuccessful) {
                    myProfileId = profileRes.body()?.publicProfileId ?: ""
                    myUsername = profileRes.body()?.username ?: ""
                }

                // Get summary
                isLoadingSummary = true
                val summaryRes = api.getFamilySubscription()
                if (summaryRes.isSuccessful) {
                    familySummary = summaryRes.body()
                }
                isLoadingSummary = false

                // Get members
                isLoadingMembers = true
                val membersRes = api.getFamilyMembers()
                if (membersRes.isSuccessful) {
                    membersList = membersRes.body() ?: emptyList()
                }
                isLoadingMembers = false

                // Get sent invitations
                isLoadingSentInvites = true
                val sentRes = api.getFamilyInvitations()
                if (sentRes.isSuccessful) {
                    sentInvitations = sentRes.body() ?: emptyList()
                }
                isLoadingSentInvites = false

                // Get incoming invitations
                isLoadingIncomingInvites = true
                val incomingRes = api.getIncomingFamilyInvitations()
                if (incomingRes.isSuccessful) {
                    incomingInvitations = incomingRes.body() ?: emptyList()
                }
                isLoadingIncomingInvites = false

                // Get access list
                isLoadingAccess = true
                val accessRes = api.getFamilyMembersAccess()
                if (accessRes.isSuccessful) {
                    accessList = accessRes.body() ?: emptyList()
                }
                isLoadingAccess = false
            } catch (e: Exception) {
                Toast.makeText(context, "Error de red al actualizar datos", Toast.LENGTH_SHORT).show()
            }
        }
    }

    LaunchedEffect(Unit) {
        refreshAll()
    }

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
                .padding(16.dp)
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
                    text = "Grupo y Seguridad V2",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryColor
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Current plan badge banner
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = CardBgColor),
                border = BorderStroke(1.dp, GrayMuted.copy(alpha = 0.2f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Mi Suscripción",
                            fontSize = 12.sp,
                            color = GrayMuted,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = familySummary?.planName?.let {
                                when(it.lowercase()) {
                                    "free" -> "Plan Básico"
                                    "basic" -> "Plan Premium"
                                    "premium" -> "Plan Familiar Guardián"
                                    else -> it
                                }
                            } ?: "Plan Básico",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryColor
                        )
                    }
                    Button(
                        onClick = onNavigateToPlans,
                        colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
                    ) {
                        Text("Ver Planes", color = Color.White)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Tab Selector
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(CardBgColor)
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
                        text = "Mi Grupo",
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
                        text = "Permisos y SOS",
                        fontWeight = FontWeight.Bold,
                        color = if (activeTab == 1) Color.White else GrayMuted,
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            if (activeTab == 0) {
                // ================== TAB 0: MI GRUPO ==================
                
                // 1. Members List
                Text(
                    text = "MIEMBROS DE MI GRUPO FAMILIAR",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = GrayMuted,
                    modifier = Modifier.align(Alignment.Start),
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                if (isLoadingMembers) {
                    CircularProgressIndicator(color = TealPrimary)
                } else if (membersList.isEmpty()) {
                    Text("No se encontraron miembros en el grupo familiar.", color = GrayMuted, fontSize = 13.sp)
                } else {
                    membersList.forEach { member ->
                        val isMe = member.publicProfileId == myProfileId
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = CardBgColor),
                            border = BorderStroke(1.dp, if (isMe) TealPrimary.copy(alpha = 0.5f) else GrayMuted.copy(alpha = 0.2f))
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(TealPrimary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = member.displayName.take(2).uppercase(),
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = member.displayName + if (isMe) " (Tú)" else "",
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimaryColor
                                    )
                                    Text(
                                        text = "@${member.username} • Role: ${if (member.role == FamilyMembershipRole.Owner) "Dueño" else "Miembro"}",
                                        color = GrayMuted,
                                        fontSize = 12.sp
                                    )
                                }
                                
                                // Delete button for owner
                                if (!isMe && familySummary?.currentUserRole == FamilyMembershipRole.Owner) {
                                    IconButton(
                                        onClick = {
                                            scope.launch {
                                                try {
                                                    val api = ApiClient.getApiService(context)
                                                    val res = api.removeFamilyMember(member.publicMembershipId)
                                                    if (res.isSuccessful) {
                                                        Toast.makeText(context, "Miembro eliminado", Toast.LENGTH_SHORT).show()
                                                        refreshAll()
                                                    }
                                                } catch (e: Exception) {
                                                    // ignore
                                                }
                                            }
                                        }
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = Color(0xFFEF4444))
                                    }
                                }

                                // Leave button for member
                                if (isMe && member.role != FamilyMembershipRole.Owner && familySummary?.canLeaveGroup == true) {
                                    OutlinedButton(
                                        onClick = {
                                            scope.launch {
                                                try {
                                                    val api = ApiClient.getApiService(context)
                                                    val res = api.leaveFamilyGroup()
                                                    if (res.isSuccessful) {
                                                        Toast.makeText(context, "Has salido del grupo", Toast.LENGTH_SHORT).show()
                                                        refreshAll()
                                                    }
                                                } catch (e: Exception) {
                                                    // ignore
                                                }
                                            }
                                        },
                                        border = BorderStroke(1.dp, Color(0xFFEF4444)),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444))
                                    ) {
                                        Text("Salir", fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // 2. Send Invitation Section
                if (familySummary?.canInviteMembers == true) {
                    Text(
                        text = "INVITAR AL GRUPO",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = GrayMuted,
                        modifier = Modifier.align(Alignment.Start),
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = CardBgColor),
                        border = BorderStroke(1.dp, GrayMuted.copy(alpha = 0.2f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            OutlinedTextField(
                                value = inviteUsername,
                                onValueChange = { inviteUsername = it },
                                label = { Text("Nombre de usuario (Opcional)") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = TealPrimary,
                                    unfocusedBorderColor = GrayMuted,
                                    focusedTextColor = TextPrimaryColor,
                                    unfocusedTextColor = TextPrimaryColor
                                )
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedTextField(
                                value = inviteEmail,
                                onValueChange = { inviteEmail = it },
                                label = { Text("Email (Opcional)") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = TealPrimary,
                                    unfocusedBorderColor = GrayMuted,
                                    focusedTextColor = TextPrimaryColor,
                                    unfocusedTextColor = TextPrimaryColor
                                )
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = {
                                    if (inviteUsername.isBlank() && inviteEmail.isBlank()) {
                                        Toast.makeText(context, "Escribe un usuario o email", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    isSendingInvite = true
                                    scope.launch {
                                        try {
                                            val api = ApiClient.getApiService(context)
                                            val response = api.createFamilyInvitation(
                                                CreateFamilyInvitationRequest(
                                                    username = inviteUsername.trim().ifEmpty { null },
                                                    email = inviteEmail.trim().ifEmpty { null },
                                                    createMonitoringRelationship = true
                                                )
                                            )
                                            if (response.isSuccessful) {
                                                val body = response.body()
                                                showManualCodeDialog = body?.manualCode
                                                inviteUsername = ""
                                                inviteEmail = ""
                                                Toast.makeText(context, "Invitación creada!", Toast.LENGTH_SHORT).show()
                                                refreshAll()
                                            } else {
                                                Toast.makeText(context, "Error al invitar: ${response.code()}", Toast.LENGTH_SHORT).show()
                                            }
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Error de red al invitar", Toast.LENGTH_SHORT).show()
                                        } finally {
                                            isSendingInvite = false
                                        }
                                    }
                                },
                                enabled = !isSendingInvite,
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
                            ) {
                                if (isSendingInvite) {
                                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                                } else {
                                    Text("Generar Invitación / Código", color = Color.White)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // 3. Redeem Manual Code
                Text(
                    text = "UNIRSE CON CÓDIGO",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = GrayMuted,
                    modifier = Modifier.align(Alignment.Start),
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBgColor),
                    border = BorderStroke(1.dp, GrayMuted.copy(alpha = 0.2f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = redeemCodeInput,
                            onValueChange = { redeemCodeInput = it },
                            label = { Text("Código (Ej. Sim123)") },
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = TealPrimary,
                                unfocusedBorderColor = GrayMuted,
                                focusedTextColor = TextPrimaryColor,
                                unfocusedTextColor = TextPrimaryColor
                            )
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Button(
                            onClick = {
                                if (redeemCodeInput.isBlank()) return@Button
                                isRedeemingCode = true
                                scope.launch {
                                    try {
                                        val api = ApiClient.getApiService(context)
                                        val response = api.redeemFamilyInvitation(
                                            RedeemFamilyInvitationRequest(code = redeemCodeInput.trim())
                                        )
                                        if (response.isSuccessful) {
                                            Toast.makeText(context, "¡Te has unido con éxito!", Toast.LENGTH_SHORT).show()
                                            redeemCodeInput = ""
                                            refreshAll()
                                        } else {
                                            Toast.makeText(context, "Código inválido o ya usado", Toast.LENGTH_SHORT).show()
                                        }
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Error de red al canjear", Toast.LENGTH_SHORT).show()
                                    } finally {
                                        isRedeemingCode = false
                                    }
                                }
                            },
                            enabled = !isRedeemingCode,
                            colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
                        ) {
                            if (isRedeemingCode) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                            } else {
                                Text("Canjear", color = Color.White)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // 4. Sent Pending Invitations
                if (sentInvitations.isNotEmpty()) {
                    Text(
                        text = "INVITACIONES ENVIADAS PENDIENTES",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = GrayMuted,
                        modifier = Modifier.align(Alignment.Start),
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    sentInvitations.forEach { invite ->
                        if (invite.status == FamilyInvitationStatus.Pending) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = CardBgColor)
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = invite.targetUsername?.let { "@$it" } ?: invite.targetEmail ?: "Invitado",
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimaryColor
                                        )
                                        Text(
                                            text = "Expira: ${invite.expiresAtUtc.take(16).replace("T", " ")} UTC",
                                            color = GrayMuted,
                                            fontSize = 11.sp
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            scope.launch {
                                                try {
                                                    val api = ApiClient.getApiService(context)
                                                    val res = api.revokeFamilyInvitation(invite.publicInvitationId)
                                                    if (res.isSuccessful) {
                                                        Toast.makeText(context, "Invitación cancelada", Toast.LENGTH_SHORT).show()
                                                        refreshAll()
                                                    }
                                                } catch (e: Exception) {
                                                    // ignore
                                                }
                                            }
                                        }
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "Cancelar", tint = Color(0xFFEF4444))
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // 5. Incoming Pending Invitations
                if (incomingInvitations.isNotEmpty()) {
                    Text(
                        text = "INVITACIONES DE GRUPO RECIBIDAS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = GrayMuted,
                        modifier = Modifier.align(Alignment.Start),
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    incomingInvitations.forEach { incoming ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = CardBgColor),
                            border = BorderStroke(1.dp, TealPrimary)
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Invitación de @${incoming.ownerUsername}",
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimaryColor
                                    )
                                    Text(
                                        text = "Grupo de ${incoming.ownerName} (${incoming.planName})",
                                        color = GrayMuted,
                                        fontSize = 11.sp
                                    )
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    IconButton(
                                        onClick = {
                                            scope.launch {
                                                try {
                                                    val api = ApiClient.getApiService(context)
                                                    val res = api.acceptFamilyInvitation(incoming.publicInvitationId)
                                                    if (res.isSuccessful) {
                                                        Toast.makeText(context, "¡Aceptado con éxito!", Toast.LENGTH_SHORT).show()
                                                        refreshAll()
                                                    }
                                                } catch (e: Exception) {
                                                    // ignore
                                                }
                                            }
                                        }
                                    ) {
                                        Icon(Icons.Default.Check, contentDescription = "Aceptar", tint = Color(0xFF22C55E))
                                    }
                                    IconButton(
                                        onClick = {
                                            scope.launch {
                                                try {
                                                    val api = ApiClient.getApiService(context)
                                                    val res = api.rejectFamilyInvitation(incoming.publicInvitationId)
                                                    if (res.isSuccessful) {
                                                        Toast.makeText(context, "Invitación rechazada", Toast.LENGTH_SHORT).show()
                                                        refreshAll()
                                                    }
                                                } catch (e: Exception) {
                                                    // ignore
                                                }
                                            }
                                        }
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "Rechazar", tint = Color(0xFFEF4444))
                                    }
                                }
                            }
                        }
                    }
                }

            } else {
                // ================== TAB 1: PERMISOS Y SOS V2 ==================
                Text(
                    text = "CONFIGURACIÓN DE ACCESOS Y PRIORIDAD SOS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = GrayMuted,
                    modifier = Modifier.align(Alignment.Start),
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                if (isLoadingAccess) {
                    CircularProgressIndicator(color = TealPrimary)
                } else if (accessList.isEmpty()) {
                    Text("No tienes miembros activos configurados para permisos.", color = GrayMuted, fontSize = 13.sp)
                } else {
                    accessList.forEach { access ->
                        // Determine the correct target ID and names
                        val targetId = if (access.viewerPublicProfileId == myProfileId) access.subjectPublicProfileId else access.viewerPublicProfileId
                        val targetUsername = if (access.viewerPublicProfileId == myProfileId) access.subjectUsername else access.viewerUsername
                        val targetName = if (access.viewerPublicProfileId == myProfileId) access.subjectName else access.viewerName

                        // Check if we already have local editing states for this membership
                        var isSosContact by remember(access.publicRelationshipId) { mutableStateOf(access.isSosContact) }
                        var sosPriority by remember(access.publicRelationshipId) { mutableStateOf(access.sosPriority ?: 0) }
                        var viewLocation by remember(access.publicRelationshipId) { mutableStateOf(access.permissions.viewLocation) }
                        var viewRoutes by remember(access.publicRelationshipId) { mutableStateOf(access.permissions.viewRoutes) }
                        var viewTelemetry by remember(access.publicRelationshipId) { mutableStateOf(access.permissions.viewTelemetry) }
                        var viewMedicalProfile by remember(access.publicRelationshipId) { mutableStateOf(access.permissions.viewMedicalProfile) }
                        var isUpdatingAccess by remember { mutableStateOf(false) }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = CardBgColor),
                            border = BorderStroke(1.dp, GrayMuted.copy(alpha = 0.2f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(TealPrimary.copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = targetName.take(2).uppercase(),
                                            color = TealPrimary,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(targetName, fontWeight = FontWeight.Bold, color = TextPrimaryColor)
                                        Text("@$targetUsername", color = GrayMuted, fontSize = 12.sp)
                                    }
                                }

                                Divider(color = GrayMuted.copy(alpha = 0.15f), modifier = Modifier.padding(vertical = 12.dp))

                                // SOS Contact Settings
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("Contacto SOS", fontWeight = FontWeight.Bold, color = TextPrimaryColor, fontSize = 13.sp)
                                        Text("Notificar a esta persona en incidentes", color = GrayMuted, fontSize = 11.sp)
                                    }
                                    Switch(
                                        checked = isSosContact,
                                        onCheckedChange = { isSosContact = it },
                                        colors = SwitchDefaults.colors(checkedThumbColor = TealPrimary)
                                    )
                                }

                                if (isSosContact) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text("Prioridad SOS: $sosPriority", fontWeight = FontWeight.Bold, color = TextPrimaryColor, fontSize = 13.sp)
                                            Text("Orden de llamada en emergencia", color = GrayMuted, fontSize = 11.sp)
                                        }
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            IconButton(
                                                onClick = { if (sosPriority > 0) sosPriority-- },
                                                modifier = Modifier.size(32.dp).background(CardElevatedColor, CircleShape)
                                            ) {
                                                Icon(Icons.Default.ArrowDownward, contentDescription = "Bajar", tint = TextPrimaryColor, modifier = Modifier.size(16.dp))
                                            }
                                            IconButton(
                                                onClick = { sosPriority++ },
                                                modifier = Modifier.size(32.dp).background(CardElevatedColor, CircleShape)
                                            ) {
                                                Icon(Icons.Default.ArrowUpward, contentDescription = "Subir", tint = TextPrimaryColor, modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }
                                }

                                Divider(color = GrayMuted.copy(alpha = 0.15f), modifier = Modifier.padding(vertical = 12.dp))

                                // Privacy & Monitoring Details
                                Text("Permisos concedidos a este miembro:", fontWeight = FontWeight.Bold, color = TextPrimaryColor, fontSize = 12.sp)
                                Spacer(modifier = Modifier.height(8.dp))

                                // Permission checkboxes
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                    Checkbox(
                                        checked = viewLocation,
                                        onCheckedChange = { viewLocation = it },
                                        colors = CheckboxDefaults.colors(checkedColor = TealPrimary)
                                    )
                                    Text("Ver ubicación en mapa", color = TextPrimaryColor, fontSize = 13.sp)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                    Checkbox(
                                        checked = viewRoutes,
                                        onCheckedChange = { viewRoutes = it },
                                        colors = CheckboxDefaults.colors(checkedColor = TealPrimary)
                                    )
                                    Text("Ver rutas y trayectos", color = TextPrimaryColor, fontSize = 13.sp)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                    Checkbox(
                                        checked = viewTelemetry,
                                        onCheckedChange = { viewTelemetry = it },
                                        colors = CheckboxDefaults.colors(checkedColor = TealPrimary)
                                    )
                                    Text("Ver telemetría (Ritmo Cardíaco / G-Force)", color = TextPrimaryColor, fontSize = 13.sp)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                    Checkbox(
                                        checked = viewMedicalProfile,
                                        onCheckedChange = { viewMedicalProfile = it },
                                        colors = CheckboxDefaults.colors(checkedColor = TealPrimary)
                                    )
                                    Text("Ver Ficha Médica", color = TextPrimaryColor, fontSize = 13.sp)
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                Button(
                                    onClick = {
                                        isUpdatingAccess = true
                                        scope.launch {
                                            try {
                                                val api = ApiClient.getApiService(context)
                                                val res = api.updateFamilyMemberAccess(
                                                    targetPublicProfileId = targetId,
                                                    request = UpdateFamilyMemberAccessRequest(
                                                        viewLocation = viewLocation,
                                                        viewRoutes = viewRoutes,
                                                        viewTelemetry = viewTelemetry,
                                                        viewMedicalProfile = viewMedicalProfile,
                                                        viewEmergencyLocation = true,
                                                        viewIncidents = true,
                                                        receiveCriticalAlerts = true,
                                                        sendMessages = true,
                                                        receiveNotifications = true,
                                                        confirmMedicalConsent = true,
                                                        sosPriority = if (isSosContact) sosPriority else null
                                                    )
                                                )
                                                if (res.isSuccessful) {
                                                    Toast.makeText(context, "Accesos de @$targetUsername actualizados!", Toast.LENGTH_SHORT).show()
                                                    refreshAll()
                                                } else {
                                                    Toast.makeText(context, "Error al actualizar accesos: ${res.code()}", Toast.LENGTH_SHORT).show()
                                                }
                                            } catch (e: Exception) {
                                                Toast.makeText(context, "Error de red al actualizar", Toast.LENGTH_SHORT).show()
                                            } finally {
                                                isUpdatingAccess = false
                                            }
                                        }
                                    },
                                    enabled = !isUpdatingAccess,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
                                ) {
                                    if (isUpdatingAccess) {
                                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                                    } else {
                                        Text("Guardar Accesos y Prioridad", color = Color.White)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Show Manual Code copyable popup dialog
        showManualCodeDialog?.let { code ->
            AlertDialog(
                onDismissRequest = { showManualCodeDialog = null },
                title = { Text("Invitación Generada", color = TextPrimaryColor, fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        Text(
                            text = "Código de un solo uso:",
                            color = GrayMuted,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(CardElevatedColor)
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = code,
                                color = TealPrimary,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 22.sp
                            )
                            IconButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("Family Code", code)
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, "Copiado al portapapeles 📋", Toast.LENGTH_SHORT).show()
                                }
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copiar", tint = TextPrimaryColor)
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Comparte este código con tu familiar para que pueda unirse directamente en su aplicación en 'Canjear Código'.",
                            color = GrayMuted,
                            fontSize = 12.sp
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { showManualCodeDialog = null },
                        colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
                    ) {
                        Text("Aceptar", color = Color.White)
                    }
                },
                containerColor = CardBgColor
            )
        }
    }
}
