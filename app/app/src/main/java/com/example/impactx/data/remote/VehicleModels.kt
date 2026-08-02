package com.example.impactx.data.remote

import com.google.gson.annotations.SerializedName

data class CreateVehicleRequest(
    @SerializedName("tipoVehiculo") val tipoVehiculo: String, // "Automovil", "Suv", "Camioneta", "Van"
    @SerializedName("marca") val marca: String,
    @SerializedName("modelo") val modelo: String,
    @SerializedName("ano") val ano: Int,
    @SerializedName("velocidadPromedio") val velocidadPromedio: Double,
    @SerializedName("usoPrincipalVehiculo") val usoPrincipalVehiculo: String, // "Ciudad", "Carretera", "Mixto"
    @SerializedName("esPrincipal") val esPrincipal: Boolean? = false
)

data class VehicleDto(
    @SerializedName("publicVehicleId") val publicVehicleId: String,
    @SerializedName("tipoVehiculo") val tipoVehiculo: String,
    @SerializedName("marca") val marca: String,
    @SerializedName("modelo") val modelo: String,
    @SerializedName("ano") val ano: Int,
    @SerializedName("velocidadPromedio") val velocidadPromedio: Double,
    @SerializedName("usoPrincipalVehiculo") val usoPrincipalVehiculo: String,
    @SerializedName("esPrincipal") val esPrincipal: Boolean
)
