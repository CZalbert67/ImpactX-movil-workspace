package com.example.impactx.data.remote

import com.google.gson.annotations.SerializedName

data class LoginRequest(
    @SerializedName("correo") val correo: String,
    @SerializedName("password") val password: String
)

data class RegisterRequest(
    @SerializedName("username") val username: String,
    @SerializedName("correo") val correo: String,
    @SerializedName("password") val password: String,
    @SerializedName("nombre") val nombre: String,
    @SerializedName("telefono") val telefono: String,
    @SerializedName("planActivo") val planActivo: String? = "Free"
)

data class RefreshTokenRequest(
    @SerializedName("accessToken") val accessToken: String,
    @SerializedName("refreshToken") val refreshToken: String
)

data class AuthResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String?,
    @SerializedName("token") val accessToken: String?,
    @SerializedName("refreshToken") val refreshToken: String?,
    @SerializedName("usuario") val usuario: UsuarioDto?
)

data class UsuarioDto(
    @SerializedName("id") val id: String,
    @SerializedName("username") val username: String,
    @SerializedName("correo") val correo: String,
    @SerializedName("nombre") val nombre: String,
    @SerializedName("telefono") val telefono: String?,
    @SerializedName("planActivo") val planActivo: String?
)

data class UpdateFcmTokenRequest(
    @SerializedName("token") val token: String
)
