package com.example.impactx.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WearableContractTest {
    @Test
    fun `physical SM-L330 maps to backend Galaxy Watch 8 contract`() {
        assertEquals("Galaxy Watch 8", WearableContract.canonicalModel("SM-L330"))
    }

    @Test
    fun `canonical target values are accepted`() {
        assertTrue(
            WearableContract.isSupported(
                manufacturer = "Samsung",
                model = "Galaxy Watch 8",
                platform = "WearOS"
            )
        )
    }
}
