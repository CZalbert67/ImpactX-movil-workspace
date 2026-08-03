package com.example.impactx.data

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationManager
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

object LocationHelper {

    /**
     * Gets the best available last known location from either GPS or Network providers.
     * Returns null if no location is available or permissions are not granted.
     */
    @SuppressLint("MissingPermission")
    suspend fun getLastKnownLocation(context: Context): Location? {
        return try {
            // Try FusedLocationProvider first (more accurate, uses Google Play Services)
            val fusedClient = LocationServices.getFusedLocationProviderClient(context)
            suspendCancellableCoroutine { continuation ->
                fusedClient.lastLocation
                    .addOnSuccessListener { location ->
                        continuation.resume(location)
                    }
                    .addOnFailureListener {
                        // Fallback to legacy LocationManager
                        val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
                        val gps = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                        val network = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                        continuation.resume(gps ?: network)
                    }
            }
        } catch (e: Exception) {
            // Fallback to system LocationManager
            try {
                val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
                val gps = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                val network = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                gps ?: network
            } catch (ex: Exception) {
                null
            }
        }
    }

    /**
     * Returns a human readable address string for display, or a fallback string with coordinates.
     */
    fun formatLocation(lat: Double, lng: Double): String {
        return "Lat: ${"%.5f".format(lat)}, Lng: ${"%.5f".format(lng)}"
    }
}
