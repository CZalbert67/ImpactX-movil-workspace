package com.example.impactx.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.example.impactx.data.local.AppDatabase
import com.example.impactx.data.local.SessionEntity
import com.example.impactx.data.remote.ApiClient
import com.example.impactx.data.remote.RegisterRequest
import kotlinx.coroutines.launch

@Composable
fun RegisterScreen(
    onNavigateBack: () -> Unit,
    onRegisterSuccess: (String) -> Unit,
    onNavigateToLogin: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

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
            Spacer(modifier = Modifier.height(32.dp))

            // Title
            Text(
                text = "Crear Cuenta",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.align(Alignment.Start)
            )
            Text(
                text = "Regístrate para comenzar a proteger tus viajes",
                fontSize = 14.sp,
                color = GrayMuted,
                modifier = Modifier
                    .align(Alignment.Start)
                    .padding(top = 4.dp, bottom = 24.dp)
            )

            // TextFields
            OutlinedTextField(
                value = name,
                onValueChange = { name = it; errorMessage = "" },
                label = { Text("Nombre Completo") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = TealPrimary,
                    unfocusedBorderColor = GrayMuted,
                    focusedLabelColor = TealPrimary,
                    unfocusedLabelColor = GrayMuted,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                singleLine = true
            )

            OutlinedTextField(
                value = email,
                onValueChange = { email = it; errorMessage = "" },
                label = { Text("Correo Electrónico") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = TealPrimary,
                    unfocusedBorderColor = GrayMuted,
                    focusedLabelColor = TealPrimary,
                    unfocusedLabelColor = GrayMuted,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                singleLine = true
            )

            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it; errorMessage = "" },
                label = { Text("Teléfono Móvil") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = TealPrimary,
                    unfocusedBorderColor = GrayMuted,
                    focusedLabelColor = TealPrimary,
                    unfocusedLabelColor = GrayMuted,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                singleLine = true
            )

            OutlinedTextField(
                value = password,
                onValueChange = { password = it; errorMessage = "" },
                label = { Text("Contraseña") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                shape = RoundedCornerShape(12.dp),
                visualTransformation = PasswordVisualTransformation(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = TealPrimary,
                    unfocusedBorderColor = GrayMuted,
                    focusedLabelColor = TealPrimary,
                    unfocusedLabelColor = GrayMuted,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                singleLine = true
            )

            if (errorMessage.isNotEmpty()) {
                Text(
                    text = errorMessage,
                    color = Color(0xFFEF4444),
                    fontSize = 14.sp,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Register Button
            Button(
                onClick = {
                    val emailPattern = android.util.Patterns.EMAIL_ADDRESS
                    val isEmailValid = emailPattern.matcher(email.trim()).matches()
                    
                    val isPasswordValid = password.length >= 8 && 
                            password.any { it.isUpperCase() } && 
                            password.any { it.isLowerCase() } && 
                            password.any { it.isDigit() }

                    if (name.isBlank() || email.isBlank() || phone.isBlank() || password.isBlank()) {
                        errorMessage = "Por favor completa todos los campos."
                    } else if (!isEmailValid) {
                        errorMessage = "El correo electrónico no tiene un formato válido."
                    } else if (!isPasswordValid) {
                        errorMessage = "La contraseña debe tener al menos 8 caracteres, incluir una mayúscula, una minúscula y un número."
                    } else if (isLoading) {
                        // Already loading
                    } else {
                        isLoading = true
                        errorMessage = ""
                        coroutineScope.launch {
                            try {
                                val apiService = ApiClient.getApiService(context)
                                val generatedUsername = name.trim().replace(" ", "").lowercase() + (10..99).random().toString()
                                val response = apiService.register(
                                    RegisterRequest(
                                        username = generatedUsername,
                                        correo = email.trim(),
                                        password = password,
                                        nombre = name.trim(),
                                        telefono = phone.trim()
                                    )
                                )
                                if (response.isSuccessful && response.body()?.success == true) {
                                    val authBody = response.body()!!
                                    val userDto = authBody.usuario!!
                                    
                                    // Save Session in Room
                                    val db = AppDatabase.getDatabase(context)
                                    db.sessionDao().saveSession(
                                        SessionEntity(
                                            userId = userDto.id,
                                            username = userDto.username,
                                            correo = userDto.correo,
                                            planActivo = userDto.planActivo,
                                            accessToken = authBody.accessToken!!,
                                            refreshToken = authBody.refreshToken!!,
                                            expiresAt = System.currentTimeMillis() + (15 * 60 * 1000) // 15 mins
                                        )
                                    )
                                    
                                    isLoading = false
                                    onRegisterSuccess(name)
                                } else {
                                    isLoading = false
                                    errorMessage = response.body()?.message ?: "Error al registrar la cuenta."
                                }
                            } catch (e: Exception) {
                                isLoading = false
                                errorMessage = "Error de red: no se pudo conectar al servidor."
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
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Text("Registrarme", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Footer
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(text = "¿Ya tienes cuenta? ", color = GrayMuted, fontSize = 14.sp)
                Text(
                    text = "Inicia Sesión",
                    color = TealPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    modifier = Modifier.clickable { onNavigateToLogin() }
                )
            }
        }
    }
}
