package com.example.impactx.data.remote

import com.google.gson.annotations.SerializedName

data class PlanDto(
    @SerializedName("id") val id: String,
    @SerializedName("nombre") val nombre: String,
    @SerializedName("precioMensual") val precioMensual: Double,
    @SerializedName("precioAnual") val precioAnual: Double,
    @SerializedName("maxContactos") val maxContactos: Int,
    @SerializedName("maxMonitores") val maxMonitores: Int,
    @SerializedName("historialMapa") val historialMapa: Boolean,
    @SerializedName("exportacionDatos") val exportacionDatos: Boolean,
    @SerializedName("soportePrioritario") val soportePrioritario: Boolean,
    @SerializedName("duracionTrialDias") val duracionTrialDias: Int
)
