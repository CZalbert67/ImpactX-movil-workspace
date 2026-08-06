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
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import com.example.impactx.data.local.AppDatabase
import com.example.impactx.data.local.SessionEntity
import com.example.impactx.data.remote.ApiClient
import com.example.impactx.data.sync.FcmTokenRegistrar
import com.example.impactx.data.sync.ImpactSyncScheduler
import com.example.impactx.data.remote.LoginRequest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers

@Composable
fun LoginScreen(
    onNavigateBack: () -> Unit,
    onLoginSuccess: (String) -> Unit,
    onNavigateToRegister: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
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
            Spacer(modifier = Modifier.height(48.dp))

            // Title
            Text(
                text = "Iniciar Sesión",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.align(Alignment.Start)
            )
            Text(
                text = "Ingresa tus credenciales para continuar",
                fontSize = 14.sp,
                color = GrayMuted,
                modifier = Modifier
                    .align(Alignment.Start)
                    .padding(top = 4.dp, bottom = 32.dp)
            )

            // TextFields
            OutlinedTextField(
                value = email,
                onValueChange = { email = it; errorMessage = "" },
                label = { Text("Correo o Usuario") },
                placeholder = { Text("Ingresa tu correo o usuario") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
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
                    .padding(vertical = 8.dp),
                shape = RoundedCornerShape(12.dp),
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    val icon = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = icon,
                            contentDescription = if (passwordVisible) "Ocultar contraseña" else "Mostrar contraseña",
                            tint = TealPrimary
                        )
                    }
                },
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

            Spacer(modifier = Modifier.height(24.dp))

            // Login Button
            Button(
                onClick = {
                    if (email.isBlank() || password.isBlank()) {
                        errorMessage = "Por favor completa todos los campos."
                    } else if (password.length < 6) {
                        errorMessage = "La contraseña debe tener al menos 6 caracteres."
                    } else if (isLoading) {
                        // Already loading
                    } else {
                        isLoading = true
                        errorMessage = ""
                        coroutineScope.launch {
                            try {
                                val apiService = ApiClient.getApiService(context)
                                val response = apiService.login(LoginRequest(email.trim(), password.trim()))
                                if (response.isSuccessful && response.body()?.success == true) {
                                    val authBody = response.body()!!
                                    val userDto = authBody.usuario!!
                                    
                                    // Save Session in Room
                                    val db = AppDatabase.getDatabase(context)
                                    withContext(Dispatchers.IO) {
                                        db.sessionDao().saveSession(
                                            SessionEntity(
                                                userDto.id,
                                                userDto.username,
                                                userDto.correo,
                                                userDto.planActivo,
                                                authBody.accessToken!!,
                                                authBody.refreshToken!!,
                                                System.currentTimeMillis() + (15 * 60 * 1000) // 15 mins
                                            )
                                        )
                                    }
                                    
                                    FcmTokenRegistrar.ensureRegistered(context)
                                    ImpactSyncScheduler.enqueueCritical(context)
                                    isLoading = false
                                    onLoginSuccess(userDto.username)
                                } else {
                                    isLoading = false
                                    errorMessage = response.body()?.message ?: "Error de credenciales."
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
                    Text("Entrar", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Footer
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(text = "¿No tienes cuenta? ", color = GrayMuted, fontSize = 14.sp)
                Text(
                    text = "Regístrate",
                    color = TealPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    modifier = Modifier.clickable { onNavigateToRegister() }
                )
            }
        }
    }
}
