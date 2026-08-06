package com.example.impactx.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.impactx.data.WearableContract
import com.example.impactx.data.local.AppDatabase
import com.example.impactx.data.local.PendingSosEntity
import com.example.impactx.data.local.TelemetryQueueEntity
import com.example.impactx.data.sync.ImpactSyncScheduler
import com.example.impactx.data.sync.NetworkState
import com.example.impactx.data.sync.SyncPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID

private data class SyncDashboardState(
    val online: Boolean = false,
    val pendingSos: Int = 0,
    val pendingTelemetry: Int = 0,
    val recentSos: List<PendingSosEntity> = emptyList(),
    val lastBatch: SyncPreferences.LastBatch = SyncPreferences.LastBatch(null, 0, 0, 0, false, null),
    val lastSyncAt: Long = 0L,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MandarDatosScreen(
    onNavigateBack: () -> Unit,
    onTriggerSos: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var state by remember { mutableStateOf(SyncDashboardState()) }
    var telemetryHold by remember {
        mutableStateOf(SyncPreferences.isTelemetryHoldEnabled(context))
    }
    var busyAction by remember { mutableStateOf<String?>(null) }

    suspend fun loadState() {
        val next = withContext(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(context)
            SyncDashboardState(
                online = NetworkState.isOnline(context),
                pendingSos = db.pendingSosDao().countPending(),
                pendingTelemetry = db.telemetryQueueDao().countPending(),
                recentSos = db.pendingSosDao().getRecent(5),
                lastBatch = SyncPreferences.lastBatch(context),
                lastSyncAt = SyncPreferences.lastSyncAt(context),
            )
        }
        state = next
    }

    LaunchedEffect(Unit) {
        while (true) {
            loadState()
            delay(1_000)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(DarkBlue, DarkBlueEnd)))
            .systemBarsPadding(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Atrás", tint = TealPrimary)
                }
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(
                        "Estado de sincronización",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryColor,
                    )
                    Text(
                        "SOS inmediato y telemetría batch",
                        fontSize = 12.sp,
                        color = GrayMuted,
                    )
                }
            }

            Spacer(Modifier.height(14.dp))
            StatusCard(state, telemetryHold)
            Spacer(Modifier.height(14.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = CardBgColor),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, TealPrimary.copy(alpha = 0.25f)),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        "Comportamiento automático",
                        color = TextPrimaryColor,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "• Una colisión grave se guarda y se intenta enviar inmediatamente.\n" +
                            "• Sin internet, el SOS queda pendiente y se envía antes que la telemetría.\n" +
                            "• La telemetría se agrupa en lotes de 20 eventos o 30 segundos.\n" +
                            "• El viaje no se pausa ni finaliza por detectar un impacto.",
                        color = GrayMuted,
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                    )
                }
            }

            Spacer(Modifier.height(14.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = CardBgColor),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFFEAB308).copy(alpha = 0.35f)),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        "MODO DEMOSTRACIÓN — DATOS SIMULADOS",
                        color = Color(0xFFEAB308),
                        fontWeight = FontWeight.Black,
                        fontSize = 13.sp,
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                "Retener sólo telemetría",
                                color = TextPrimaryColor,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                "El internet continúa disponible para que el SOS sí llegue al monitor.",
                                color = GrayMuted,
                                fontSize = 11.sp,
                            )
                        }
                        Switch(
                            checked = telemetryHold,
                            onCheckedChange = { enabled ->
                                telemetryHold = enabled
                                SyncPreferences.setTelemetryHoldEnabled(context, enabled)
                                if (!enabled) {
                                    ImpactSyncScheduler.enqueueTelemetry(context, immediate = true, force = true)
                                }
                            },
                        )
                    }

                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = {
                            telemetryHold = true
                            SyncPreferences.setTelemetryHoldEnabled(context, true)
                            busyAction = "generate"
                            scope.launch {
                                val result = generateDemoTelemetry(context)
                                busyAction = null
                                Toast.makeText(context, result, Toast.LENGTH_LONG).show()
                                loadState()
                            }
                        },
                        enabled = busyAction == null,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                    ) {
                        if (busyAction == "generate") {
                            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else {
                            Text("Retener y generar 20 muestras", fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = {
                            telemetryHold = false
                            SyncPreferences.setTelemetryHoldEnabled(context, false)
                            ImpactSyncScheduler.enqueueCritical(context)
                            ImpactSyncScheduler.enqueueTelemetry(context, immediate = true, force = true)
                            Toast.makeText(context, "Sincronización liberada: SOS primero, batch después.", Toast.LENGTH_LONG).show()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22C55E)),
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Restablecer señal y sincronizar", fontWeight = FontWeight.Bold)
                    }

                    val lastBatchId = state.lastBatch.batchId
                    if (!lastBatchId.isNullOrBlank()) {
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = {
                                busyAction = "requeue"
                                scope.launch {
                                    val count = withContext(Dispatchers.IO) {
                                        AppDatabase.getDatabase(context)
                                            .telemetryQueueDao()
                                            .requeueBatch(lastBatchId)
                                    }
                                    ImpactSyncScheduler.enqueueTelemetry(context, immediate = true, force = true)
                                    busyAction = null
                                    Toast.makeText(
                                        context,
                                        "Lote reenviado con los mismos eventId: $count eventos.",
                                        Toast.LENGTH_LONG,
                                    ).show()
                                    loadState()
                                }
                            },
                            enabled = busyAction == null,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Reenviar último batch para probar idempotencia")
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))
            LastBatchCard(state.lastBatch)
            Spacer(Modifier.height(14.dp))
            RecentSosCard(state.recentSos, onTriggerSos)
            Spacer(Modifier.height(28.dp))
        }
    }
}

@Composable
private fun StatusCard(state: SyncDashboardState, telemetryHold: Boolean) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CardBgColor),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (state.online) Icons.Default.CheckCircle else Icons.Default.CloudOff,
                    contentDescription = null,
                    tint = if (state.online) Color(0xFF22C55E) else Color(0xFFEF4444),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    if (state.online) "Conexión disponible" else "Sin conexión",
                    color = TextPrimaryColor,
                    fontWeight = FontWeight.Bold,
                )
            }
            Divider(Modifier.padding(vertical = 12.dp), color = GrayMuted.copy(alpha = 0.2f))
            MetricRow("SOS pendientes", state.pendingSos.toString(), state.pendingSos > 0)
            MetricRow("Telemetría pendiente", state.pendingTelemetry.toString(), state.pendingTelemetry > 0)
            MetricRow("Retención demo", if (telemetryHold) "ACTIVA" else "INACTIVA", telemetryHold)
            MetricRow(
                "Última sincronización",
                if (state.lastSyncAt == 0L) "Sin registros" else formatLocalTime(state.lastSyncAt),
                false,
            )
        }
    }
}

@Composable
private fun MetricRow(label: String, value: String, warning: Boolean) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, color = GrayMuted, fontSize = 12.sp)
        Text(
            value,
            color = if (warning) Color(0xFFEAB308) else TextPrimaryColor,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
        )
    }
}

@Composable
private fun LastBatchCard(batch: SyncPreferences.LastBatch) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CardBgColor),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("Último batch confirmado por backend", color = TextPrimaryColor, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            if (batch.batchId.isNullOrBlank()) {
                Text("Todavía no se ha enviado un lote.", color = GrayMuted, fontSize = 12.sp)
            } else {
                MetricRow("Batch ID", batch.batchId.take(12) + "…", false)
                MetricRow("Recibidos", batch.count.toString(), false)
                MetricRow("Insertados", batch.inserted.toString(), false)
                MetricRow("Duplicados", batch.duplicates.toString(), batch.duplicates > 0)
                MetricRow("Capturado offline", if (batch.capturedOffline) "Sí" else "No", batch.capturedOffline)
                Text(batch.processedAt ?: "", color = GrayMuted, fontSize = 10.sp)
            }
        }
    }
}

@Composable
private fun RecentSosCard(events: List<PendingSosEntity>, onTriggerSos: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CardBgColor),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("SOS recientes", color = TextPrimaryColor, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            if (events.isEmpty()) {
                Text("No hay alertas registradas.", color = GrayMuted, fontSize = 12.sp)
            } else {
                events.forEach { event ->
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                if (event.status == "SENT") Icons.Default.CheckCircle else Icons.Default.Warning,
                                contentDescription = null,
                                tint = if (event.status == "SENT") Color(0xFF22C55E) else Color(0xFFEF4444),
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                            Column {
                                Text(
                                    "${event.gForce ?: "?"} G · ${event.heartRate ?: "?"} BPM",
                                    color = TextPrimaryColor,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                                Text(event.timestampUtc ?: "", color = GrayMuted, fontSize = 10.sp)
                            }
                        }
                        Text(event.status ?: "PENDING", color = GrayMuted, fontSize = 10.sp)
                    }
                }
                OutlinedButton(onClick = onTriggerSos, modifier = Modifier.fillMaxWidth()) {
                    Text("Abrir módulo de emergencia")
                }
            }
        }
    }
}

private suspend fun generateDemoTelemetry(context: Context): String = withContext(Dispatchers.IO) {
    val prefs = context.getSharedPreferences("impactx_prefs", Context.MODE_PRIVATE)
    val tripId = prefs.getString("active_trip_id", null) ?: WearableManager.activeWearTripId
        ?: return@withContext "Inicia un viaje desde el Galaxy Watch8 antes de generar el batch."
    val wearableDeviceId = WearableManager.backendDeviceId
        ?: return@withContext "El Galaxy Watch8 todavía no está vinculado con el backend."

    val db = AppDatabase.getDatabase(context)
    val now = System.currentTimeMillis()
    var inserted = 0
    repeat(20) { index ->
        val entity = TelemetryQueueEntity().apply {
            eventId = UUID.randomUUID().toString()
            this.tripId = tripId
            sequenceNumber = SyncPreferences.nextSequence(context)
            timestampUtc = utcTimestamp(now - (19 - index) * 2_000L)
            lat = 19.99750 + index * 0.00001
            lng = -99.34250 + index * 0.00001
            velocity = 12.0 + index * 0.2
            gpsAccuracyMeters = 8.0
            accelerationX = 0.10 + index * 0.01
            accelerationY = 0.05
            accelerationZ = 9.80
            accelerationMagnitude = 9.81 + index * 0.01
            gyroscopeX = 0.01
            gyroscopeY = 0.02
            gyroscopeZ = 0.01
            heartRate = 78 + (index % 5)
            batteryLevel = 86
            capturedOffline = true
            this.wearableDeviceId = wearableDeviceId
            wearableModel = WearableContract.MODEL
            status = "PENDING"
            batchId = null
            lastError = null
            createdAtMs = now - (19 - index) * 2_000L
            sentAtMs = 0L
        }
        if (db.telemetryQueueDao().insertIfAbsent(entity) != -1L) inserted++
    }

    // El botón activa la retención antes de llegar aquí, por lo que las
    // muestras permanecen visibles como PENDING hasta que se libere el demo.
    ImpactSyncScheduler.enqueueTelemetry(context, immediate = false, force = false)
    "$inserted muestras creadas y retenidas para la demostración batch."
}

private fun utcTimestamp(epochMs: Long): String = SimpleDateFormat(
    "yyyy-MM-dd'T'HH:mm:ss'Z'",
    Locale.US,
).apply { timeZone = TimeZone.getTimeZone("UTC") }.format(Date(epochMs))

private fun formatLocalTime(epochMs: Long): String = SimpleDateFormat(
    "yyyy-MM-dd HH:mm:ss",
    Locale.getDefault(),
).format(Date(epochMs))
