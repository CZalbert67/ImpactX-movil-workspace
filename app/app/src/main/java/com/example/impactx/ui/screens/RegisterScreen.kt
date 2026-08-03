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
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.impactx.data.local.AppDatabase
import com.example.impactx.data.local.SessionEntity
import com.example.impactx.data.remote.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    onNavigateBack: () -> Unit,
    onRegisterSuccess: (String) -> Unit,
    onNavigateToLogin: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Onboarding step tracker (1 to 5)
    var currentStep by remember { mutableIntStateOf(1) }

    // --- STEP 1 STATE: Account Creation ---
    var name by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    // Agreement checkboxes
    var acceptTerms by remember { mutableStateOf(false) }
    var acceptPrivacy by remember { mutableStateOf(false) }
    var allowLocationEmergency by remember { mutableStateOf(false) }
    var allowPatternAnalysis by remember { mutableStateOf(false) }

    // --- STEP 2 STATE: Plan Selection ---
    var myProfileId by remember { mutableStateOf("") }
    var selectedPlan by remember { mutableStateOf("Gratuito") }

    // --- STEP 3 STATE: Vehicle Registration ---
    var vehicleType by remember { mutableStateOf("Automovil") }
    var vehicleUse by remember { mutableStateOf("Mixto") }
    var vehicleBrand by remember { mutableStateOf("") }
    var vehicleModel by remember { mutableStateOf("") }
    var vehicleYear by remember { mutableStateOf("2026") }
    var vehicleAvgSpeed by remember { mutableStateOf("40") }
    var vehicleColor by remember { mutableStateOf("") }
    var vehiclePlate by remember { mutableStateOf("") }
    var vehicleDropdownExpanded by remember { mutableStateOf(false) }
    var useDropdownExpanded by remember { mutableStateOf(false) }
    var yearDropdownExpanded by remember { mutableStateOf(false) }

    // --- STEP 4 STATE: Medical Profile ---
    var bloodType by remember { mutableStateOf("Selecciona") }
    var allergies by remember { mutableStateOf("") }
    var conditions by remember { mutableStateOf("") }
    var medications by remember { mutableStateOf("") }
    var emergencyNote by remember { mutableStateOf("") }
    var bloodDropdownExpanded by remember { mutableStateOf(false) }

    // --- STEP 5 STATE: Protection Network ---
    var invitationType by remember { mutableStateOf("Contacto") } // "Contacto", "Monitor"
    var searchBy by remember { mutableStateOf("Username") } // "Username", "Email"
    var inviteInput by remember { mutableStateOf("") }
    var inviteRelationship by remember { mutableStateOf("Familiar") }
    var invitePriority by remember { mutableStateOf("Secondary") }
    var makePrimaryWhenAccepted by remember { mutableStateOf(true) }
    var priorityDropdownExpanded by remember { mutableStateOf(false) }

    // Global loading and error message
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

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
            Spacer(modifier = Modifier.height(16.dp))

            // Step Indicator Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "PASO $currentStep DE 5",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TealPrimary,
                    letterSpacing = 1.5.sp
                )
                if (currentStep > 1) {
                    Text(
                        text = "ID: $myProfileId",
                        fontSize = 10.sp,
                        color = GrayMuted
                    )
                }
            }

            // Progress bar
            LinearProgressIndicator(
                progress = { currentStep / 5f },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = TealPrimary,
                trackColor = Color.White.copy(alpha = 0.1f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // --- STEP WIZARD RENDERING ---
            when (currentStep) {
                1 -> {
                    // STEP 1: CREATE ACCOUNT
                    Text(
                        text = "Crea tu cuenta",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.align(Alignment.Start)
                    )
                    Text(
                        text = "Después te preguntaremos por tu vehículo, ficha médica y una persona de confianza. Tu cuenta inicia en el plan Gratuito.",
                        fontSize = 13.sp,
                        color = GrayMuted,
                        modifier = Modifier
                            .align(Alignment.Start)
                            .padding(top = 4.dp, bottom = 16.dp)
                    )

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it; errorMessage = "" },
                        label = { Text("Nombre Completo *") },
                        placeholder = { Text("Tu nombre completo") },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TealPrimary,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                            focusedLabelColor = TealPrimary,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it; errorMessage = "" },
                        label = { Text("Nombre de usuario *") },
                        placeholder = { Text("tu_usuario") },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TealPrimary,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                            focusedLabelColor = TealPrimary,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        singleLine = true
                    )
                    Text(
                        text = "Letras, números, punto y guion bajo; sin puntos consecutivos.",
                        fontSize = 10.sp,
                        color = GrayMuted,
                        modifier = Modifier.align(Alignment.Start).padding(start = 4.dp, bottom = 4.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it; errorMessage = "" },
                            label = { Text("Correo electrónico *") },
                            placeholder = { Text("tucorreo@ejemplo.com") },
                            modifier = Modifier.weight(1.1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = TealPrimary,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                                focusedLabelColor = TealPrimary,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it; errorMessage = "" },
                            label = { Text("Teléfono *") },
                            placeholder = { Text("55 0000 0000") },
                            modifier = Modifier.weight(0.9f),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = TealPrimary,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                                focusedLabelColor = TealPrimary,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            singleLine = true
                        )
                    }

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it; errorMessage = "" },
                        label = { Text("Contraseña *") },
                        placeholder = { Text("Crea una contraseña segura") },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        shape = RoundedCornerShape(12.dp),
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = "Ver contraseña",
                                    tint = TealPrimary
                                )
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TealPrimary,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                            focusedLabelColor = TealPrimary,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        singleLine = true
                    )
                    Text(
                        text = "8-100 caracteres, con mayúscula, minúscula, número y símbolo.",
                        fontSize = 10.sp,
                        color = GrayMuted,
                        modifier = Modifier.align(Alignment.Start).padding(start = 4.dp, bottom = 4.dp)
                    )

                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it; errorMessage = "" },
                        label = { Text("Confirmar contraseña *") },
                        placeholder = { Text("Repite tu contraseña") },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        shape = RoundedCornerShape(12.dp),
                        visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                                Icon(
                                    imageVector = if (confirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = "Ver contraseña",
                                    tint = TealPrimary
                                )
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TealPrimary,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                            focusedLabelColor = TealPrimary,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Agreement checkboxes list
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.03f))
                            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = acceptTerms,
                                onCheckedChange = { acceptTerms = it; errorMessage = "" },
                                colors = CheckboxDefaults.colors(checkedColor = TealPrimary)
                            )
                            Text("Acepto los Términos de uso (1.0-2026-08-03).", color = Color.White, fontSize = 12.sp)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = acceptPrivacy,
                                onCheckedChange = { acceptPrivacy = it; errorMessage = "" },
                                colors = CheckboxDefaults.colors(checkedColor = TealPrimary)
                            )
                            Text("Acepto el Aviso de privacidad (1.0-2026-08-03).", color = Color.White, fontSize = 12.sp)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = allowLocationEmergency,
                                onCheckedChange = { allowLocationEmergency = it },
                                colors = CheckboxDefaults.colors(checkedColor = TealPrimary)
                            )
                            Text("Permito usar mi ubicación únicamente durante incidentes y viajes.", color = Color.White, fontSize = 12.sp)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = allowPatternAnalysis,
                                onCheckedChange = { allowPatternAnalysis = it },
                                colors = CheckboxDefaults.colors(checkedColor = TealPrimary)
                            )
                            Text("Permito analizar patrones de conducción para mejorar la detección.", color = Color.White, fontSize = 12.sp)
                        }
                    }

                    if (errorMessage.isNotEmpty()) {
                        Text(
                            text = errorMessage,
                            color = Color(0xFFEF4444),
                            fontSize = 13.sp,
                            modifier = Modifier.padding(top = 12.dp).align(Alignment.Start),
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = {
                            val emailPattern = android.util.Patterns.EMAIL_ADDRESS
                            val isEmailValid = emailPattern.matcher(email.trim()).matches()
                            
                            val isPasswordValid = password.length >= 8 &&
                                    password.any { it.isUpperCase() } &&
                                    password.any { it.isLowerCase() } &&
                                    password.any { it.isDigit() }
                            
                            val usernamePattern = "^[a-z0-9_.]+$".toRegex()
                            val phonePattern = "^[0-9]+$".toRegex()

                            if (name.isBlank() || username.isBlank() || email.isBlank() || phone.isBlank() || password.isBlank() || confirmPassword.isBlank()) {
                                errorMessage = "Por favor completa todos los campos requeridos (*)."
                            } else if (name.trim().length < 3) {
                                errorMessage = "El nombre debe tener al menos 3 caracteres."
                            } else if (!username.trim().matches(usernamePattern)) {
                                errorMessage = "El nombre de usuario solo permite letras minúsculas, números, guion bajo (_) y punto (.)."
                            } else if (username.trim().length < 3 || username.trim().length > 20) {
                                errorMessage = "El nombre de usuario debe medir entre 3 y 20 caracteres."
                            } else if (!isEmailValid) {
                                errorMessage = "El formato del correo electrónico no es válido."
                            } else if (!phone.trim().matches(phonePattern) || phone.trim().length !in 8..15) {
                                errorMessage = "El teléfono debe contener únicamente números y medir entre 8 y 15 dígitos."
                            } else if (password != confirmPassword) {
                                errorMessage = "Las contraseñas ingresadas no coinciden."
                            } else if (!isPasswordValid) {
                                errorMessage = "La contraseña debe tener al menos 8 caracteres, e incluir mayúsculas, minúsculas y números."
                            } else if (!acceptTerms || !acceptPrivacy) {
                                errorMessage = "Debes aceptar los Términos de uso y el Aviso de privacidad para continuar."
                            } else {
                                isLoading = true
                                errorMessage = ""
                                scope.launch {
                                    try {
                                        val api = ApiClient.getApiService(context)
                                        val response = api.register(
                                            RegisterRequest(
                                                username = username.trim().lowercase(),
                                                correo = email.trim(),
                                                password = password,
                                                nombre = name.trim(),
                                                telefono = phone.trim(),
                                                planActivo = "Free"
                                            )
                                        )
                                        if (response.isSuccessful && response.body()?.success == true) {
                                            val auth = response.body()!!
                                            val user = auth.usuario!!
                                            
                                            // Save session locally
                                            val db = AppDatabase.getDatabase(context)
                                            withContext(Dispatchers.IO) {
                                                db.sessionDao().saveSession(
                                                    SessionEntity(
                                                        user.id,
                                                        user.username,
                                                        user.correo,
                                                        user.planActivo ?: "Free",
                                                        auth.accessToken!!,
                                                        auth.refreshToken!!,
                                                        System.currentTimeMillis() + (15 * 60 * 1000)
                                                    )
                                                )
                                            }

                                            // Retrieve the public profile ID
                                            val profileResponse = api.getProfileUsername()
                                            if (profileResponse.isSuccessful) {
                                                myProfileId = profileResponse.body()?.publicProfileId ?: ""
                                            }

                                            currentStep = 2
                                        } else {
                                            errorMessage = response.body()?.message ?: "Error al registrar la cuenta."
                                        }
                                    } catch (e: Exception) {
                                        errorMessage = "Error de conexión: no se pudo establecer contacto con el servidor."
                                    } finally {
                                        isLoading = false
                                    }
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                        } else {
                            Text("Crear cuenta y continuar", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(text = "¿Ya tienes cuenta? ", color = GrayMuted, fontSize = 14.sp)
                        Text(
                            text = "Inicia sesión",
                            color = TealPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            modifier = Modifier.clickable { onNavigateToLogin() }
                        )
                    }
                }

                2 -> {
                    // STEP 2: PLAN SELECTOR
                    Text(
                        text = "Elige tu plan familiar",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.align(Alignment.Start)
                    )
                    Text(
                        text = "El plan Gratuito ya está seleccionado. Solo cambia la opción si deseas activar un plan de pago simulado.",
                        fontSize = 13.sp,
                        color = GrayMuted,
                        modifier = Modifier
                            .align(Alignment.Start)
                            .padding(top = 4.dp, bottom = 16.dp)
                    )

                    // Public ID Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF102238))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text("TU ID PÚBLICO DE IMPACTX", fontSize = 10.sp, color = GrayMuted, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = myProfileId,
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f)
                                )
                                TextButton(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        val clip = ClipData.newPlainText("ImpactX ID", myProfileId)
                                        clipboard.setPrimaryClip(clip)
                                        Toast.makeText(context, "ID copiado", Toast.LENGTH_SHORT).show()
                                    }
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "Copiar", tint = TealPrimary, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Copiar", color = TealPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Text(
                                text = "Se generó automáticamente, es único y puedes compartirlo para recibir invitaciones. No sirve para iniciar sesión.",
                                fontSize = 10.sp,
                                color = GrayMuted,
                                modifier = Modifier.padding(top = 6.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Plans Cards List
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        listOf(
                            Triple("Gratuito", "0 MXN", "La opción inicial para probar la red de protección.\n• 2 personas en total\n• 1 vehículo por usuario"),
                            Triple("Estándar", "99 MXN / mes", "Para una familia pequeña con varios vehículos.\n• 3 personas en total\n• 3 vehículos por usuario"),
                            Triple("Premium", "199 MXN / mes", "Mayor capacidad con protecciones técnicas contra abuso.\n• 6 personas en total\n• Vehículos sin límite fijo")
                        ).forEach { (plan, price, desc) ->
                            val isSelected = selectedPlan == plan
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedPlan = plan }
                                    .border(
                                        1.dp,
                                        if (isSelected) TealPrimary else Color.Transparent,
                                        RoundedCornerShape(12.dp)
                                    ),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) Color(0xFF162E4A) else Color(0xFF102238)
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = { selectedPlan = plan },
                                        colors = RadioButtonDefaults.colors(selectedColor = TealPrimary)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(plan, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
                                            Text(price, fontWeight = FontWeight.Bold, color = TealPrimary, fontSize = 14.sp)
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(desc, color = GrayMuted, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.02f)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "ℹ️ La capacidad total incluye a la persona titular. Los cobros de Estándar y Premium son simulados en esta versión.",
                            color = GrayMuted,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(12.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            currentStep = 3
                        },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
                    ) {
                        Text("Continuar con $selectedPlan", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                }

                3 -> {
                    // STEP 3: VEHICLE REGISTRATION
                    Text(
                        text = "Registra tu vehículo principal",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.align(Alignment.Start)
                    )
                    Text(
                        text = "Con estos datos identificaremos correctamente los viajes, alertas e incidentes asociados a tu cuenta.",
                        fontSize = 13.sp,
                        color = GrayMuted,
                        modifier = Modifier
                            .align(Alignment.Start)
                            .padding(top = 4.dp, bottom = 16.dp)
                    )

                    // Vehicle Type Dropdown Selector
                    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        OutlinedTextField(
                            value = vehicleType,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Tipo de vehículo *") },
                            trailingIcon = {
                                IconButton(onClick = { vehicleDropdownExpanded = true }) {
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Expandir")
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = TealPrimary,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                                focusedLabelColor = TealPrimary,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )
                        DropdownMenu(
                            expanded = vehicleDropdownExpanded,
                            onDismissRequest = { vehicleDropdownExpanded = false }
                        ) {
                            listOf("Automovil", "Suv", "Camioneta", "Van", "Motocicleta").forEach { type ->
                                DropdownMenuItem(
                                    text = { Text(type) },
                                    onClick = {
                                        vehicleType = type
                                        vehicleDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Vehicle Use Dropdown Selector
                    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        OutlinedTextField(
                            value = vehicleUse,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Uso principal *") },
                            trailingIcon = {
                                IconButton(onClick = { useDropdownExpanded = true }) {
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Expandir")
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = TealPrimary,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                                focusedLabelColor = TealPrimary,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )
                        DropdownMenu(
                            expanded = useDropdownExpanded,
                            onDismissRequest = { useDropdownExpanded = false }
                        ) {
                            listOf("Ciudad", "Carretera", "Mixto").forEach { use ->
                                DropdownMenuItem(
                                    text = { Text(use) },
                                    onClick = {
                                        vehicleUse = use
                                        useDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = vehicleBrand,
                        onValueChange = { vehicleBrand = it; errorMessage = "" },
                        label = { Text("Marca *") },
                        placeholder = { Text("Busca, por ejemplo: Nissan") },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TealPrimary,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                            focusedLabelColor = TealPrimary,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = vehicleModel,
                        onValueChange = { vehicleModel = it; errorMessage = "" },
                        label = { Text("Modelo *") },
                        placeholder = { Text("Selecciona una marca") },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TealPrimary,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                            focusedLabelColor = TealPrimary,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        singleLine = true
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Year Dropdown Select (Calendario desglosado con años límites de 1980 a 2027)
                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedTextField(
                                value = vehicleYear,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Año *") },
                                trailingIcon = {
                                    IconButton(onClick = { yearDropdownExpanded = true }) {
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = "Expandir años")
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = TealPrimary,
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                                    focusedLabelColor = TealPrimary,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                )
                            )
                            DropdownMenu(
                                expanded = yearDropdownExpanded,
                                onDismissRequest = { yearDropdownExpanded = false }
                            ) {
                                (2027 downTo 1980).forEach { y ->
                                    DropdownMenuItem(
                                        text = { Text(y.toString()) },
                                        onClick = {
                                            vehicleYear = y.toString()
                                            yearDropdownExpanded = false
                                            errorMessage = ""
                                        }
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = vehicleAvgSpeed,
                            onValueChange = { vehicleAvgSpeed = it; errorMessage = "" },
                            label = { Text("Velocidad promedio *") },
                            placeholder = { Text("Kms/hr") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = TealPrimary,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                                focusedLabelColor = TealPrimary,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            singleLine = true
                        )
                    }

                    OutlinedTextField(
                        value = vehicleColor,
                        onValueChange = { vehicleColor = it; errorMessage = "" },
                        label = { Text("Color") },
                        placeholder = { Text("Blanco, Negro, Gris...") },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TealPrimary,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                            focusedLabelColor = TealPrimary,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = vehiclePlate,
                        onValueChange = { vehiclePlate = it; errorMessage = "" },
                        label = { Text("Placa") },
                        placeholder = { Text("ABC-123") },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TealPrimary,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                            focusedLabelColor = TealPrimary,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        singleLine = true
                    )

                    if (errorMessage.isNotEmpty()) {
                        Text(
                            text = errorMessage,
                            color = Color(0xFFEF4444),
                            fontSize = 13.sp,
                            modifier = Modifier.padding(top = 12.dp).align(Alignment.Start),
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            val yearVal = vehicleYear.toIntOrNull()
                            val speedVal = vehicleAvgSpeed.toDoubleOrNull()

                            if (vehicleBrand.isBlank() || vehicleModel.isBlank() || yearVal == null || speedVal == null) {
                                errorMessage = "Por favor completa todos los campos obligatorios (*) con valores correctos."
                            } else if (vehicleBrand.trim().length < 2) {
                                errorMessage = "La marca debe tener al menos 2 caracteres."
                            } else if (vehicleModel.trim().length < 2) {
                                errorMessage = "El modelo debe tener al menos 2 caracteres."
                            } else if (speedVal < 10.0 || speedVal > 250.0) {
                                errorMessage = "La velocidad promedio debe ser un número válido entre 10 y 250 km/h."
                            } else if (vehiclePlate.isNotBlank() && vehiclePlate.trim().length !in 3..15) {
                                errorMessage = "La placa del vehículo debe medir entre 3 y 15 caracteres."
                            } else if (vehicleColor.isNotBlank() && vehicleColor.trim().length !in 3..20) {
                                errorMessage = "El color del vehículo debe medir entre 3 y 20 caracteres."
                            } else {
                                isLoading = true
                                errorMessage = ""
                                scope.launch {
                                    try {
                                        val api = ApiClient.getApiService(context)
                                        val response = api.createVehicle(
                                            CreateVehicleRequest(
                                                tipoVehiculo = vehicleType,
                                                marca = vehicleBrand.trim(),
                                                modelo = vehicleModel.trim(),
                                                ano = yearVal,
                                                velocidadPromedio = speedVal,
                                                usoPrincipalVehiculo = vehicleUse,
                                                esPrincipal = true
                                            )
                                        )
                                        if (response.isSuccessful) {
                                            errorMessage = ""
                                            currentStep = 4
                                        } else {
                                            errorMessage = "Error al guardar el vehículo. Comprueba tus datos."
                                        }
                                    } catch (e: Exception) {
                                        // Network error, skip to step 4 as option
                                        currentStep = 4
                                    } finally {
                                        isLoading = false
                                    }
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                        } else {
                            Text("Guardar y continuar", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Omitir por ahora",
                        color = TealPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        modifier = Modifier
                            .clickable {
                                currentStep = 4
                            }
                            .padding(8.dp)
                    )
                }

                4 -> {
                    // STEP 4: MEDICAL PROFILE
                    Text(
                        text = "Ficha médica de emergencia",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.align(Alignment.Start)
                    )
                    Text(
                        text = "Es opcional, pero puede ayudar a tus contactos durante un incidente. Solo se comparte con consentimiento explícito.",
                        fontSize = 13.sp,
                        color = GrayMuted,
                        modifier = Modifier
                            .align(Alignment.Start)
                            .padding(top = 4.dp, bottom = 16.dp)
                    )

                    // Blood Type Dropdown
                    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        OutlinedTextField(
                            value = bloodType,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Tipo de sangre") },
                            trailingIcon = {
                                IconButton(onClick = { bloodDropdownExpanded = true }) {
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Expandir")
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = TealPrimary,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                                focusedLabelColor = TealPrimary,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )
                        DropdownMenu(
                            expanded = bloodDropdownExpanded,
                            onDismissRequest = { bloodDropdownExpanded = false }
                        ) {
                            listOf("Selecciona", "O-", "O+", "A-", "A+", "B-", "B+", "AB-", "AB+").forEach { type ->
                                DropdownMenuItem(
                                    text = { Text(type) },
                                    onClick = {
                                        bloodType = type
                                        bloodDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = allergies,
                        onValueChange = { allergies = it; errorMessage = "" },
                        label = { Text("Alergias") },
                        placeholder = { Text("Medicamentos, alimentos...") },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TealPrimary,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                            focusedLabelColor = TealPrimary,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = conditions,
                        onValueChange = { conditions = it; errorMessage = "" },
                        label = { Text("Condiciones o padecimientos") },
                        placeholder = { Text("Diabetes, hipertensión...") },
                        modifier = Modifier.fillMaxWidth().height(100.dp).padding(vertical = 4.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TealPrimary,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                            focusedLabelColor = TealPrimary,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        maxLines = 4
                    )

                    OutlinedTextField(
                        value = medications,
                        onValueChange = { medications = it; errorMessage = "" },
                        label = { Text("Medicamentos actuales") },
                        placeholder = { Text("Nombre y dosis, si aplica") },
                        modifier = Modifier.fillMaxWidth().height(100.dp).padding(vertical = 4.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TealPrimary,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                            focusedLabelColor = TealPrimary,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        maxLines = 4
                    )

                    OutlinedTextField(
                        value = emergencyNote,
                        onValueChange = { emergencyNote = it; errorMessage = "" },
                        label = { Text("Nota para una emergencia") },
                        placeholder = { Text("Información breve que debería conocer un contacto de confianza") },
                        modifier = Modifier.fillMaxWidth().height(120.dp).padding(vertical = 4.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TealPrimary,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                            focusedLabelColor = TealPrimary,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        maxLines = 5
                    )

                    if (errorMessage.isNotEmpty()) {
                        Text(
                            text = errorMessage,
                            color = Color(0xFFEF4444),
                            fontSize = 13.sp,
                            modifier = Modifier.padding(top = 12.dp).align(Alignment.Start),
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            if (allergies.length > 500 || conditions.length > 500 || medications.length > 500 || emergencyNote.length > 500) {
                                errorMessage = "Ninguno de los campos médicos puede exceder los 500 caracteres."
                            } else {
                                isLoading = true
                                errorMessage = ""
                                scope.launch {
                                    try {
                                        val api = ApiClient.getApiService(context)
                                        val response = api.updateMedicalProfile(
                                            UpdateMedicalProfileRequest(
                                                tipoSangre = if (bloodType == "Selecciona") null else bloodType,
                                                alergias = if (allergies.isBlank()) null else allergies.trim(),
                                                condiciones = if (conditions.isBlank()) null else conditions.trim(),
                                                medicamentos = if (medications.isBlank()) null else medications.trim(),
                                                nota = if (emergencyNote.isBlank()) null else emergencyNote.trim()
                                            )
                                        )
                                        if (response.isSuccessful) {
                                            errorMessage = ""
                                            currentStep = 5
                                        } else {
                                            errorMessage = "Error al actualizar la ficha médica."
                                        }
                                    } catch (e: Exception) {
                                        currentStep = 5
                                    } finally {
                                        isLoading = false
                                    }
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                        } else {
                            Text("Guardar y continuar", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Prefiero omitirla",
                        color = TealPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        modifier = Modifier
                            .clickable {
                                currentStep = 5
                            }
                            .padding(8.dp)
                    )
                }

                5 -> {
                    // STEP 5: PROTECTION NETWORK / INITIAL INVITATION
                    Text(
                        text = "Crea tu red de protección",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.align(Alignment.Start)
                    )
                    Text(
                        text = "Invita ahora a una persona de confianza. Podrás agregar más desde el panel después.",
                        fontSize = 13.sp,
                        color = GrayMuted,
                        modifier = Modifier
                            .align(Alignment.Start)
                            .padding(top = 4.dp, bottom = 16.dp)
                    )

                    // Selector: Contacto vs Monitor
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.03f))
                            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { invitationType = "Contacto"; errorMessage = "" },
                            colors = CardDefaults.cardColors(
                                containerColor = if (invitationType == "Contacto") Color(0xFF162E4A) else Color.Transparent
                            )
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    RadioButton(
                                        selected = invitationType == "Contacto",
                                        onClick = { invitationType = "Contacto"; errorMessage = "" },
                                        colors = RadioButtonDefaults.colors(selectedColor = TealPrimary)
                                    )
                                    Text("Contacto", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                                }
                                Text("Persona principal para emergencias.", color = GrayMuted, fontSize = 11.sp, modifier = Modifier.padding(start = 4.dp))
                            }
                        }

                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { invitationType = "Monitor"; errorMessage = "" },
                            colors = CardDefaults.cardColors(
                                containerColor = if (invitationType == "Monitor") Color(0xFF162E4A) else Color.Transparent
                            )
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    RadioButton(
                                        selected = invitationType == "Monitor",
                                        onClick = { invitationType = "Monitor"; errorMessage = "" },
                                        colors = RadioButtonDefaults.colors(selectedColor = TealPrimary)
                                    )
                                    Text("Monitor", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                                }
                                Text("Podrá consultar ubicación y viajes.", color = GrayMuted, fontSize = 11.sp, modifier = Modifier.padding(start = 4.dp))
                            }
                        }
                    }

                    // Selector: Search by
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = searchBy == "Username",
                                onClick = { searchBy = "Username"; errorMessage = "" },
                                colors = RadioButtonDefaults.colors(selectedColor = TealPrimary)
                            )
                            Text("Nombre de usuario", color = Color.White, fontSize = 13.sp)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = searchBy == "Email",
                                onClick = { searchBy = "Email"; errorMessage = "" },
                                colors = RadioButtonDefaults.colors(selectedColor = TealPrimary)
                            )
                            Text("Correo electrónico", color = Color.White, fontSize = 13.sp)
                        }
                    }

                    OutlinedTextField(
                        value = inviteInput,
                        onValueChange = { inviteInput = it; errorMessage = "" },
                        label = { Text("Persona a invitar *") },
                        placeholder = { Text(if (searchBy == "Username") "nombre_usuario" else "correo@ejemplo.com") },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TealPrimary,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                            focusedLabelColor = TealPrimary,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        singleLine = true
                    )

                    if (invitationType == "Contacto") {
                        OutlinedTextField(
                            value = inviteRelationship,
                            onValueChange = { inviteRelationship = it; errorMessage = "" },
                            label = { Text("Relación contigo *") },
                            placeholder = { Text("Familiar, Amigo, etc.") },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = TealPrimary,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                                focusedLabelColor = TealPrimary,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            singleLine = true
                        )

                        // Priority Selector Dropdown
                        Box(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                            OutlinedTextField(
                                value = if (invitePriority == "Primary") "Principal" else "Secundario",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Prioridad") },
                                trailingIcon = {
                                    IconButton(onClick = { priorityDropdownExpanded = true }) {
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = "Expandir")
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = TealPrimary,
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                                    focusedLabelColor = TealPrimary,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                )
                            )
                            DropdownMenu(
                                expanded = priorityDropdownExpanded,
                                onDismissRequest = { priorityDropdownExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Principal") },
                                    onClick = {
                                        invitePriority = "Primary"
                                        priorityDropdownExpanded = false
                                        errorMessage = ""
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Secundario") },
                                    onClick = {
                                        invitePriority = "Secondary"
                                        priorityDropdownExpanded = false
                                        errorMessage = ""
                                    }
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        ) {
                            Checkbox(
                                checked = makePrimaryWhenAccepted,
                                onCheckedChange = { makePrimaryWhenAccepted = it },
                                colors = CheckboxDefaults.colors(checkedColor = TealPrimary)
                            )
                            Text("Convertirlo en mi contacto principal cuando acepte.", color = Color.White, fontSize = 12.sp)
                        }
                    }

                    if (errorMessage.isNotEmpty()) {
                        Text(
                            text = errorMessage,
                            color = Color(0xFFEF4444),
                            fontSize = 13.sp,
                            modifier = Modifier.padding(top = 12.dp).align(Alignment.Start),
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            val emailPattern = android.util.Patterns.EMAIL_ADDRESS
                            val isEmailValid = emailPattern.matcher(inviteInput.trim()).matches()
                            val usernamePattern = "^[a-zA-Z0-9_.]+$".toRegex()

                            if (inviteInput.isBlank()) {
                                errorMessage = "Por favor escribe el correo o nombre de usuario de la persona."
                            } else if (searchBy == "Email" && !isEmailValid) {
                                errorMessage = "Por favor ingresa un correo electrónico válido."
                            } else if (searchBy == "Username" && (!inviteInput.trim().matches(usernamePattern) || inviteInput.trim().length < 3)) {
                                errorMessage = "El nombre de usuario debe ser válido (mínimo 3 caracteres)."
                            } else if (invitationType == "Contacto" && inviteRelationship.trim().length < 2) {
                                errorMessage = "La relación debe tener al menos 2 caracteres (ej: Familiar, Amigo)."
                            } else {
                                isLoading = true
                                errorMessage = ""
                                scope.launch {
                                    try {
                                        val api = ApiClient.getApiService(context)
                                        val inviteUser = if (searchBy == "Username") inviteInput.trim() else null
                                        val inviteEmail = if (searchBy == "Email") inviteInput.trim() else null

                                        if (invitationType == "Contacto") {
                                            val response = api.createEmergencyContactInvitation(
                                                CreateEmergencyContactInvitationRequest(
                                                    username = inviteUser,
                                                    email = inviteEmail,
                                                    publicProfileId = null,
                                                    relationship = inviteRelationship.trim(),
                                                    priority = invitePriority,
                                                    makePrimaryWhenAccepted = makePrimaryWhenAccepted
                                                )
                                            )
                                            if (response.isSuccessful) {
                                                Toast.makeText(context, "Invitación enviada", Toast.LENGTH_SHORT).show()
                                                onRegisterSuccess(name)
                                            } else {
                                                errorMessage = "Error al enviar la invitación: usuario no encontrado."
                                            }
                                        } else {
                                            val response = api.createMonitoringInvitation(
                                                CreateMonitoringInvitationRequest(
                                                    username = inviteUser,
                                                    email = inviteEmail,
                                                    publicProfileId = null,
                                                    permissions = MonitoringPermissionsRequest()
                                                )
                                            )
                                            if (response.isSuccessful) {
                                                Toast.makeText(context, "Invitación enviada", Toast.LENGTH_SHORT).show()
                                                onRegisterSuccess(name)
                                            } else {
                                                errorMessage = "Error al enviar la invitación: usuario no encontrado o conflicto."
                                            }
                                        }
                                    } catch (e: Exception) {
                                        onRegisterSuccess(name)
                                    } finally {
                                        isLoading = false
                                    }
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                        } else {
                            Text("Enviar invitación y terminar", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Terminar sin invitar",
                        color = TealPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        modifier = Modifier
                            .clickable {
                                onRegisterSuccess(name)
                            }
                            .padding(8.dp)
                    )
                }
            }
        }
    }
}
