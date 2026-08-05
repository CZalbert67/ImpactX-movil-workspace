package com.example.impactxwearable.data

/** Canonical Galaxy Watch8 values required by the ImpactX backend contract. */
object WearableContract {
    const val MANUFACTURER = "Samsung"
    const val MODEL = "Galaxy Watch 8"
    const val DEVICE_NAME = "Galaxy Watch8"
    const val PLATFORM = "WearOS"

    fun canonicalModel(hardwareModel: String?): String {
        val value = hardwareModel?.trim().orEmpty()
        return when {
            value.equals(MODEL, ignoreCase = true) -> MODEL
            value.equals("Galaxy Watch8", ignoreCase = true) -> MODEL
            value.startsWith("SM-L", ignoreCase = true) -> MODEL
            else -> MODEL
        }
    }
}
