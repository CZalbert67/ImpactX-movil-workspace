package com.example.impactxwearable.data

import org.junit.Assert.assertEquals
import org.junit.Test

class WearableContractTest {
    @Test
    fun `physical SM-L330 maps to backend Galaxy Watch 8 contract`() {
        assertEquals("Galaxy Watch 8", WearableContract.canonicalModel("SM-L330"))
    }

    @Test
    fun `device info constants match backend contract`() {
        assertEquals("Samsung", WearableContract.MANUFACTURER)
        assertEquals("Galaxy Watch 8", WearableContract.MODEL)
        assertEquals("WearOS", WearableContract.PLATFORM)
    }
}
