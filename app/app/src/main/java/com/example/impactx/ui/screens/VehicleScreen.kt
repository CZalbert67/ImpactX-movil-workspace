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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VehicleScreen(
    currentPlan: String,
    onNavigateBack: () -> Unit,
    onNavigateToPlans: () -> Unit
) {
    // Determine vehicle limit based on plan
    val maxVehicles = when (currentPlan) {
        "Básico" -> 1
        "Premium" -> 3
        else -> 999 // Unlimited
    }

    // Car brand and model catalog (Only 4-wheeled vehicles, no motorcycles)
    val carCatalog = remember {
        mapOf(
            "Toyota" to listOf("Corolla", "RAV4", "Hilux", "Prius", "Yaris"),
            "Mazda" to listOf("Mazda 3", "CX-5", "MX-5", "Mazda 6", "CX-30"),
            "Nissan" to listOf("Versa", "Sentra", "March", "Kicks", "Frontier"),
            "Chevrolet" to listOf("Aveo", "Onix", "Tracker", "Captiva", "Silverado"),
            "Ford" to listOf("Focus", "Explorer", "Mustang", "Ranger", "Lobo"),
            "Volkswagen" to listOf("Jetta", "Golf", "Tiguan", "Polo", "Taos"),
            "Honda" to listOf("Civic", "Accord", "CR-V", "HR-V", "City"),
            "Kia" to listOf("Forte", "Sportage", "Rio", "Seltos", "Soul"),
            "Hyundai" to listOf("Tucson", "Elantra", "Accent", "Creta", "Santa Fe"),
            "BMW" to listOf("Serie 3", "Serie 5", "X3", "X5", "M4"),
            "Audi" to listOf("A3", "A4", "Q3", "Q5", "e-tron"),
            "Mercedes-Benz" to listOf("Clase C", "Clase E", "GLA", "GLC", "GLE")
        )
    }

    // Form inputs state
    var selectedBrand by remember { mutableStateOf("") }
    var selectedModel by remember { mutableStateOf("") }
    var year by remember { mutableStateOf("") }
    var plate by remember { mutableStateOf("") }
    var mainUse by remember { mutableStateOf("") }
    var avgSpeed by remember { mutableStateOf("") }

    // Dropdowns UI state
    var brandExpanded by remember { mutableStateOf(false) }
    var modelExpanded by remember { mutableStateOf(false) }

    var vehicleList by remember {
        mutableStateOf(
            listOf(
                VehicleItem("Sedán", "Toyota", "Corolla", "2024", "XYZ-789-A", "Urbano", "55 km/h")
            )
        )
    }

    val limitReached = vehicleList.size >= maxVehicles

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
                    text = "Mi Vehículo",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Plan Info Banner
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
                        Text("Plan Activo: $currentPlan", fontWeight = FontWeight.Bold, color = Color.White)
                        Text(
                            text = if (maxVehicles == 999) "Vehículos registrados: ${vehicleList.size}" 
                                   else "Límite de vehículos: ${vehicleList.size} / $maxVehicles",
                            fontSize = 12.sp,
                            color = GrayMuted
                        )
                    }
                    if (currentPlan == "Básico") {
                        Button(
                            onClick = onNavigateToPlans,
                            colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text("Upgrade", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Vehicles List Title
            Text(
                text = "VEHÍCULOS REGISTRADOS",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = GrayMuted,
                modifier = Modifier.align(Alignment.Start),
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Render Vehicles
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                vehicleList.forEach { vehicle ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF102238).copy(alpha = 0.5f)),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${vehicle.brand} ${vehicle.model} (${vehicle.year})",
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 15.sp
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(TealPrimary.copy(alpha = 0.15f))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = vehicle.type,
                                        fontSize = 11.sp,
                                        color = TealPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Placas: ${vehicle.plate}", fontSize = 13.sp, color = GrayMuted)
                                Text("Uso: ${vehicle.mainUse}", fontSize = 13.sp, color = GrayMuted)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Form to Add New Vehicle
            if (limitReached) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFEF4444).copy(alpha = 0.1f)),
                    border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.3f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Límite de Vehículos Alcanzado",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Tu plan '$currentPlan' permite registrar un máximo de $maxVehicles vehículo(s).\nMejora tu plan para añadir más automóviles.",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.8f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                        )
                        Button(
                            onClick = onNavigateToPlans,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                        ) {
                            Text("Ver Planes de Suscripción", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                Text(
                    text = "REGISTRAR AUTOMÓVIL (4 LLANTAS)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = GrayMuted,
                    modifier = Modifier.align(Alignment.Start),
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Brand ExposedDropdownMenuBox
                ExposedDropdownMenuBox(
                    expanded = brandExpanded,
                    onExpandedChange = { brandExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedBrand.ifBlank { "Seleccionar Marca" },
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Marca") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = brandExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TealPrimary,
                            unfocusedBorderColor = GrayMuted,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedLabelColor = TealPrimary,
                            unfocusedLabelColor = GrayMuted
                        )
                    )
                    ExposedDropdownMenu(
                        expanded = brandExpanded,
                        onDismissRequest = { brandExpanded = false },
                        modifier = Modifier.background(Color(0xFF102238))
                    ) {
                        carCatalog.keys.forEach { brandName ->
                            DropdownMenuItem(
                                text = { Text(brandName, color = Color.White) },
                                onClick = {
                                    selectedBrand = brandName
                                    selectedModel = "" // Reset model selection
                                    brandExpanded = false
                                }
                            )
                        }
                    }
                }

                // Model ExposedDropdownMenuBox
                val modelsList = carCatalog[selectedBrand] ?: emptyList()
                ExposedDropdownMenuBox(
                    expanded = modelExpanded && selectedBrand.isNotEmpty(),
                    onExpandedChange = {
                        if (selectedBrand.isNotEmpty()) {
                            modelExpanded = it
                        }
                    }
                ) {
                    OutlinedTextField(
                        value = selectedModel.ifBlank { if (selectedBrand.isEmpty()) "Selecciona marca primero" else "Seleccionar Modelo" },
                        onValueChange = {},
                        readOnly = true,
                        enabled = selectedBrand.isNotEmpty(),
                        label = { Text("Modelo") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = modelExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TealPrimary,
                            unfocusedBorderColor = GrayMuted,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedLabelColor = TealPrimary,
                            unfocusedLabelColor = GrayMuted,
                            disabledBorderColor = GrayMuted.copy(alpha = 0.3f),
                            disabledTextColor = GrayMuted
                        )
                    )
                    ExposedDropdownMenu(
                        expanded = modelExpanded && selectedBrand.isNotEmpty(),
                        onDismissRequest = { modelExpanded = false },
                        modifier = Modifier.background(Color(0xFF102238))
                    ) {
                        modelsList.forEach { modelName ->
                            DropdownMenuItem(
                                text = { Text(modelName, color = Color.White) },
                                onClick = {
                                    selectedModel = modelName
                                    modelExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = year,
                    onValueChange = { year = it },
                    label = { Text("Año") },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TealPrimary,
                        unfocusedBorderColor = GrayMuted,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedLabelColor = TealPrimary,
                        unfocusedLabelColor = GrayMuted
                    ),
                    singleLine = true
                )

                OutlinedTextField(
                    value = plate,
                    onValueChange = { plate = it },
                    label = { Text("Placas / Matrícula") },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TealPrimary,
                        unfocusedBorderColor = GrayMuted,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedLabelColor = TealPrimary,
                        unfocusedLabelColor = GrayMuted
                    ),
                    singleLine = true
                )

                OutlinedTextField(
                    value = mainUse,
                    onValueChange = { mainUse = it },
                    label = { Text("Uso Principal") },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TealPrimary,
                        unfocusedBorderColor = GrayMuted,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedLabelColor = TealPrimary,
                        unfocusedLabelColor = GrayMuted
                    ),
                    singleLine = true
                )

                OutlinedTextField(
                    value = avgSpeed,
                    onValueChange = { avgSpeed = it },
                    label = { Text("Velocidad Promedio Habitual (ej. 60 km/h)") },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TealPrimary,
                        unfocusedBorderColor = GrayMuted,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedLabelColor = TealPrimary,
                        unfocusedLabelColor = GrayMuted
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        if (selectedBrand.isNotBlank() && selectedModel.isNotBlank() && plate.isNotBlank()) {
                            vehicleList = vehicleList + VehicleItem("Sedán", selectedBrand, selectedModel, year, plate, mainUse.ifBlank { "Particular" }, avgSpeed.ifBlank { "50 km/h" })
                            selectedBrand = ""
                            selectedModel = ""
                            year = ""
                            plate = ""
                            mainUse = ""
                            avgSpeed = ""
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TealPrimary,
                        contentColor = Color.White
                    ),
                    enabled = selectedBrand.isNotBlank() && selectedModel.isNotBlank() && plate.isNotBlank()
                ) {
                    Text("Registrar Vehículo", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
