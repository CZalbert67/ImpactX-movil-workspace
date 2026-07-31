package com.example.impactx.data.remote

import android.content.Context
import com.example.impactx.data.local.AppDatabase
import com.example.impactx.data.local.SessionEntity
import kotlinx.coroutines.runBlocking
import okhttp3.*
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.UUID
import java.util.concurrent.TimeUnit

object ApiClient {
    private const val BASE_URL = "http://10.0.2.2:5000/" // Android emulator default gateway to localhost
    private var apiService: ApiService? = null

    fun getApiService(context: Context): ApiService {
        return apiService ?: synchronized(this) {
            val service = createService(context)
            apiService = service
            service
        }
    }

    private fun createService(context: Context): ApiService {
        val database = AppDatabase.getDatabase(context)
        val sessionDao = database.sessionDao()

        // 1. Logging Interceptor
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        // 2. Request Interceptor (JWT injection + Correlation Id)
        val requestInterceptor = Interceptor { chain ->
            val session = runBlocking { sessionDao.getSession() }
            val originalRequest = chain.request()
            val builder = originalRequest.newBuilder()
                .header("X-Correlation-Id", UUID.randomUUID().toString())

            // Auto inject Bearer Token if present and not already specified
            if (session != null && originalRequest.header("Authorization") == null) {
                builder.header("Authorization", "Bearer ${session.accessToken}")
            }
            chain.proceed(builder.build())
        }

        // 3. OkHttp Authenticator (Auto-refresh on 401)
        val authenticator = Authenticator { _, response ->
            val session = runBlocking { sessionDao.getSession() }
            if (session == null) return@Authenticator null

            // Avoid infinite loop if refreshing fails repeatedly
            if (responseCount(response) >= 3) {
                runBlocking { sessionDao.clearSession() }
                return@Authenticator null
            }

            synchronized(this) {
                // Check if another request already refreshed it
                val currentSession = runBlocking { sessionDao.getSession() }
                if (currentSession == null) return@Authenticator null

                val newAccessToken: String
                val newRefreshToken: String

                if (currentSession.accessToken != session.accessToken) {
                    newAccessToken = currentSession.accessToken
                } else {
                    // Synchronously call refresh endpoint
                    val refreshService = Retrofit.Builder()
                        .baseUrl(BASE_URL)
                        .addConverterFactory(GsonConverterFactory.create())
                        .build()
                        .create(ApiService::class.java)

                    val call = refreshService.refresh(
                        RefreshTokenRequest(
                            accessToken = session.accessToken,
                            refreshToken = session.refreshToken
                        )
                    )

                    try {
                        val refreshResponse = call.execute()
                        if (refreshResponse.isSuccessful && refreshResponse.body()?.success == true) {
                            val body = refreshResponse.body()!!
                            newAccessToken = body.accessToken!!
                            newRefreshToken = body.refreshToken!!

                            // Save new tokens
                            runBlocking {
                                sessionDao.saveSession(
                                    SessionEntity(
                                        userId = session.userId,
                                        username = session.username,
                                        correo = session.correo,
                                        planActivo = session.planActivo,
                                        accessToken = newAccessToken,
                                        refreshToken = newRefreshToken,
                                        expiresAt = System.currentTimeMillis() + (15 * 60 * 1000) // 15 mins
                                    )
                                )
                            }
                        } else {
                            // Refresh token failed/expired -> force user to log in again
                            runBlocking { sessionDao.clearSession() }
                            return@Authenticator null
                        }
                    } catch (e: Exception) {
                        return@Authenticator null
                    }
                }

                // Retry request with new token
                return@Authenticator response.request.newBuilder()
                    .header("Authorization", "Bearer $newAccessToken")
                    .build()
            }
        }

        val client = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .addInterceptor(logging)
            .addInterceptor(requestInterceptor)
            .authenticator(authenticator)
            .build()

        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }

    private fun responseCount(response: Response?): Int {
        var result = 1
        var prior = response?.priorResponse
        while (prior != null) {
            result++
            prior = prior.priorResponse
        }
        return result
    }
}
