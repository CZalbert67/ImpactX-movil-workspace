package com.example.impactx.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.example.impactx.data.remote.ApiClient

@Composable
fun PlansScreen(
    currentPlan: String,
    onNavigateBack: () -> Unit,
    onPlanSelected: (String) -> Unit
) {
    val context = LocalContext.current
    var freePrice by remember { mutableStateOf("0") }
    var basicPrice by remember { mutableStateOf("99") }
    var premiumPrice by remember { mutableStateOf("199") }
    var showSuccessDialog by remember { mutableStateOf(false) }
    var selectedPlanForPurchase by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        try {
            val api = ApiClient.getApiService(context)
            val response = api.getPlans()
            if (response.isSuccessful) {
                val plans = response.body() ?: emptyList()
                plans.forEach { plan ->
                    when (plan.nombre.lowercase()) {
                        "free" -> freePrice = plan.precioMensual.toInt().toString()
                        "basic" -> basicPrice = plan.precioMensual.toInt().toString()
                        "premium" -> premiumPrice = plan.precioMensual.toInt().toString()
                    }
                }
            }
        } catch (e: Exception) {
            // keep fallback defaults on error
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
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Custom Top bar
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
                    text = "Planes de Suscripción",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Selecciona el plan que se adapte mejor a tus necesidades de seguridad. Los límites de vehículos y contactos se actualizarán de forma automática.",
                fontSize = 14.sp,
                color = GrayMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Plan 1: Básico
            PlanCard(
                name = "Básico",
                price = "$$freePrice MXN",
                period = "/ siempre gratis",
                features = listOf(
                    "✓ 3 Contactos de emergencia",
                    "✓ 1 Monitor de emergencia",
                    "✓ Monitoreo de velocidad básico",
                    "✗ Mini-mapa en tiempo real",
                    "✗ Botón de pánico SOS / Chat de choque"
                ),
                isActive = currentPlan == "Básico",
                buttonText = "Plan Actual",
                onSelect = {
                    selectedPlanForPurchase = "Básico"
                    showSuccessDialog = true
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Plan 2: Premium (Recommended)
            PlanCard(
                name = "Premium",
                price = "$$basicPrice MXN",
                period = "/ mes",
                features = listOf(
                    "✓ Hasta 10 Contactos de emergencia",
                    "✓ Hasta 3 Monitores de emergencia",
                    "✓ Mini-mapa en tiempo real (glowing map)",
                    "✓ Botón de pánico SOS",
                    "✓ Chat automático en caso de choque",
                    "✓ Soporte de telemetría por G-Force"
                ),
                isActive = currentPlan == "Premium",
                isRecommended = true,
                buttonText = "Adquirir Premium",
                onSelect = {
                    selectedPlanForPurchase = "Premium"
                    showSuccessDialog = true
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Plan 3: Familiar Guardián
            PlanCard(
                name = "Familiar Guardián",
                price = "$$premiumPrice MXN",
                period = "/ mes",
                features = listOf(
                    "✓ Contactos de emergencia ILIMITADOS",
                    "✓ Monitores de emergencia ILIMITADOS",
                    "✓ Mini-mapa en tiempo real (glowing map)",
                    "✓ Botón de pánico SOS",
                    "✓ Chat automático en caso de choque",
                    "✓ Reportes de telemetría semanales",
                    "✓ Soporte prioritario 24/7"
                ),
                isActive = currentPlan == "Familiar Guardián",
                buttonText = "Adquirir Familiar",
                onSelect = {
                    selectedPlanForPurchase = "Familiar Guardián"
                    showSuccessDialog = true
                }
            )

            Spacer(modifier = Modifier.height(24.dp))
        }

        // Purchase Success Dialog
        if (showSuccessDialog) {
            AlertDialog(
                onDismissRequest = { showSuccessDialog = false },
                title = {
                    Text(
                        text = "¡Suscripción Actualizada!",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                text = {
                    Text(
                        text = "Has seleccionado el plan '$selectedPlanForPurchase' con éxito.\n\n(Esta es una simulación de pago escolar. Los límites en vehículos, contactos y telemetría han sido actualizados en la app móvil).",
                        color = GrayMuted
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            onPlanSelected(selectedPlanForPurchase)
                            showSuccessDialog = false
                            onNavigateBack()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
                    ) {
                        Text("Aceptar", color = Color.White)
                    }
                },
                containerColor = Color(0xFF102238)
            )
        }
    }
}

@Composable
fun PlanCard(
    name: String,
    price: String,
    period: String,
    features: List<String>,
    isActive: Boolean,
    isRecommended: Boolean = false,
    buttonText: String,
    onSelect: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                border = if (isRecommended) BorderStroke(2.dp, TealPrimary) else BorderStroke(0.dp, Color.Transparent),
                shape = RoundedCornerShape(16.dp)
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) Color(0xFF102A45) else Color(0xFF102238)
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = name,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                if (isRecommended) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(TealPrimary)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "RECOMENDADO",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
                if (isActive) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF22C55E).copy(alpha = 0.2f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "ACTIVO",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF22C55E)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = price,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
                Text(
                    text = period,
                    fontSize = 14.sp,
                    color = GrayMuted,
                    modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

            Spacer(modifier = Modifier.height(16.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                features.forEach { feature ->
                    val isCrossed = feature.startsWith("✗")
                    Text(
                        text = feature,
                        fontSize = 13.sp,
                        color = if (isCrossed) Color.White.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.8f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            if (!isActive) {
                Button(
                    onClick = onSelect,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isRecommended) TealPrimary else Color(0xFF1B355A),
                        contentColor = Color.White
                    )
                ) {
                    Text(buttonText, fontWeight = FontWeight.Bold)
                }
            } else {
                OutlinedButton(
                    onClick = { /* Do nothing since it is active */ },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFF22C55E)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF22C55E))
                ) {
                    Text("Plan de Seguridad Activo", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
