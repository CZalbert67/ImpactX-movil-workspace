package com.example.impactx.data.remote

import com.google.gson.annotations.SerializedName

data class MedicalProfileDto(
    @SerializedName("tipoSangre") val tipoSangre: String?,
    @SerializedName("alergias") val alergias: String?,
    @SerializedName("condiciones") val condiciones: String?,
    @SerializedName("medicamentos") val medicamentos: String?,
    @SerializedName("nota") val nota: String?
)

data class UpdateMedicalProfileRequest(
    @SerializedName("tipoSangre") val tipoSangre: String?,
    @SerializedName("alergias") val alergias: String?,
    @SerializedName("condiciones") val condiciones: String?,
    @SerializedName("medicamentos") val medicamentos: String?,
    @SerializedName("nota") val nota: String?
)
