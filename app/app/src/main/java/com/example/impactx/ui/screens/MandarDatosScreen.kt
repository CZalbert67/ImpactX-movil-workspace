package com.example.impactx.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.CheckCircle
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
import com.example.impactx.data.local.AppDatabase
import com.example.impactx.data.local.AccidentEntity
import com.example.impactx.data.remote.ApiClient
import com.example.impactx.data.remote.SosRequest
import com.example.impactx.data.LocationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MandarDatosScreen(
    onNavigateBack: () -> Unit,
    onTriggerSos: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var accidents by remember { mutableStateOf<List<AccidentEntity>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var transmittingId by remember { mutableStateOf<Int?>(null) }

    fun loadAccidents() {
        scope.launch(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(context)
            val list = db.accidentDao().getAllAccidents()
            withContext(Dispatchers.Main) {
                accidents = list
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        loadAccidents()
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
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    colors = IconButtonDefaults.iconButtonColors(contentColor = TealPrimary)
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Atrás")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Mandar Datos (SQLite)",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryColor
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Historial local de colisiones detectadas por el reloj. Al presionar 'Mandar a la Web', se notificará de inmediato a tus contactos SOS y servicios médicos de emergencia a través del backend.",
                fontSize = 12.sp,
                color = GrayMuted,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = TealPrimary)
                }
            } else if (accidents.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "🛡️",
                            fontSize = 44.sp,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        Text(
                            text = "No hay siniestros pendientes",
                            color = TextPrimaryColor,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Tu base de datos SQLite no contiene alertas de impacto locales.",
                            color = GrayMuted,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 4.dp, start = 24.dp, end = 24.dp)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(accidents) { record ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = CardBgColor),
                            border = BorderStroke(
                                width = 1.dp,
                                color = if (record.sent) Color.White.copy(alpha = 0.05f) else Color(0xFFEF4444).copy(alpha = 0.3f)
                            )
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                // Title and Badge Row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = if (record.sent) Icons.Default.CheckCircle else Icons.Default.Warning,
                                            contentDescription = null,
                                            tint = if (record.sent) Color(0xFF22C55E) else Color(0xFFEF4444),
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (record.sent) "SOS Enviado" else "Choque Registrado",
                                            fontWeight = FontWeight.Bold,
                                            color = if (record.sent) Color(0xFF22C55E) else Color(0xFFEF4444),
                                            fontSize = 14.sp
                                        )
                                    }

                                    // Status pill
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                if (record.sent) Color(0xFF22C55E).copy(alpha = 0.1f) 
                                                else Color(0xFFEAB308).copy(alpha = 0.1f)
                                            )
                                            .padding(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = if (record.sent) "Sincronizado" else "Pendiente",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (record.sent) Color(0xFF22C55E) else Color(0xFFEAB308)
                                        )
                                    }
                                }

                                Divider(
                                    color = GrayMuted.copy(alpha = 0.15f), 
                                    modifier = Modifier.padding(vertical = 10.dp)
                                )

                                // Metrics details grid
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("Fuerza G", fontSize = 11.sp, color = GrayMuted)
                                        Text(
                                            text = String.format("%.1f G", record.gForce),
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Black,
                                            color = TextPrimaryColor
                                        )
                                    }
                                    Column {
                                        Text("Ritmo Cardíaco", fontSize = 11.sp, color = GrayMuted)
                                        Text(
                                            text = "${record.heartRate} BPM",
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Black,
                                            color = TextPrimaryColor
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("Hora del Choque", fontSize = 11.sp, color = GrayMuted)
                                        Text(
                                            text = record.timestamp.takeLast(8),
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimaryColor
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Date and Location info
                                Text(
                                    text = "Fecha: ${record.timestamp.take(10)}",
                                    fontSize = 11.sp,
                                    color = GrayMuted
                                )
                                Text(
                                    text = "GPS: ${if (record.lat != 0.0) String.format("%.5f, %.5f", record.lat, record.lng) else "No disponible"}",
                                    fontSize = 11.sp,
                                    color = GrayMuted
                                )

                                // Action Button
                                if (!record.sent) {
                                    Spacer(modifier = Modifier.height(14.dp))
                                    val isTransmitting = transmittingId == record.id
                                    
                                    Button(
                                        onClick = {
                                            transmittingId = record.id
                                            scope.launch {
                                                try {
                                                    val api = ApiClient.getApiService(context)
                                                    val placeName = if (record.lat != 0.0) {
                                                        LocationHelper.formatLocation(record.lat, record.lng)
                                                    } else {
                                                        "Ubicación no disponible"
                                                    }
                                                    
                                                    val response = api.sendSos(
                                                        SosRequest(
                                                            lat = record.lat,
                                                            lng = record.lng,
                                                            lugar = placeName,
                                                            severidad = "severe",
                                                            canal = "wearable",
                                                            gForce = String.format("%.2f", record.gForce),
                                                            frecuenciaCardiaca = record.heartRate.toString(),
                                                            modo = "automatico",
                                                            viajeId = WearableManager.activeWearTripId
                                                        )
                                                    )
                                                    
                                                    if (response.isSuccessful) {
                                                        val alert = response.body()
                                                        WearableManager.lastCrashAlertId = alert?.id
                                                        
                                                        withContext(Dispatchers.IO) {
                                                            val db = AppDatabase.getDatabase(context)
                                                            db.accidentDao().markAsSent(record.id)
                                                        }
                                                        
                                                        Toast.makeText(context, "¡SOS Enviado y Sincronizado! 🚨", Toast.LENGTH_LONG).show()
                                                        loadAccidents()
                                                        onTriggerSos()
                                                    } else {
                                                        Toast.makeText(context, "Error de Ingesta: ${response.code()}", Toast.LENGTH_SHORT).show()
                                                    }
                                                } catch (e: Exception) {
                                                    Toast.makeText(context, "Error de red al transmitir datos", Toast.LENGTH_SHORT).show()
                                                } finally {
                                                    transmittingId = null
                                                }
                                            }
                                        },
                                        enabled = !isTransmitting,
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                                    ) {
                                        if (isTransmitting) {
                                            CircularProgressIndicator(
                                                color = Color.White, 
                                                modifier = Modifier.size(18.dp),
                                                strokeWidth = 2.dp
                                            )
                                        } else {
                                            Text(
                                                text = "Mandar a la Web / Activar SOS 🚨", 
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp
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
    }
}
