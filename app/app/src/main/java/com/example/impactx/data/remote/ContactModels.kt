package com.example.impactx.data.remote

import com.google.gson.annotations.SerializedName

data class ContactoDto(
    @SerializedName("id") val id: String,
    @SerializedName("nombre") val nombre: String,
    @SerializedName("telefono") val telefono: String,
    @SerializedName("parentesco") val parentesco: String?,
    @SerializedName("priority") val priority: String,
    @SerializedName("esPrincipal") val esPrincipal: Boolean
)

data class CreateContactoRequest(
    @SerializedName("nombre") val nombre: String,
    @SerializedName("telefono") val telefono: String,
    @SerializedName("parentesco") val parentesco: String?,
    @SerializedName("priority") val priority: String = "Secundario",
    @SerializedName("esPrincipal") val esPrincipal: Boolean = false
)

data class UpdateContactoRequest(
    @SerializedName("nombre") val nombre: String?,
    @SerializedName("telefono") val telefono: String?,
    @SerializedName("parentesco") val parentesco: String?,
    @SerializedName("priority") val priority: String?
)
