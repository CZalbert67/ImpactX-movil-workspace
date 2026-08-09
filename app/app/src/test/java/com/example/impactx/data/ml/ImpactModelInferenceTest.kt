package com.example.impactx.data.ml

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ImpactModelInferenceTest {
    @Test
    fun `prediction exposes the same five-feature contract`() {
        val input = ImpactModelInput(
            gForcePeak = 13.5,
            heartRateBpm = 145,
            impactDurationMs = 490,
            speedDeltaKmh = 55.0,
            postImpactInactivitySeconds = 62,
        )

        assertEquals(5, input.asVector().size)
    }

    @Test
    fun `prediction returns normalized class probabilities`() {
        val prediction = ImpactModelInference.predict(
            ImpactModelInput(
                gForcePeak = 13.5,
                heartRateBpm = 145,
                impactDurationMs = 490,
                speedDeltaKmh = 55.0,
                postImpactInactivitySeconds = 62,
            ),
        )

        assertEquals(
            setOf("sin_choque", "leve", "moderado", "grave", "critico"),
            prediction.probabilities.keys,
        )
        assertTrue(kotlin.math.abs(prediction.probabilities.values.sum() - 1.0) < 1e-9)
        assertTrue(prediction.confidence in 0.0..1.0)
    }
}
