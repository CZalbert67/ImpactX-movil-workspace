package com.example.impactx.data.ml

import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.max

data class ImpactModelInput(
    val gForcePeak: Double,
    val heartRateBpm: Int,
    val impactDurationMs: Int = 100,
    val speedDeltaKmh: Double = 0.0,
    val postImpactInactivitySeconds: Int = 0,
) {
    fun asVector(): DoubleArray = doubleArrayOf(
        gForcePeak.coerceIn(0.0, 30.0),
        heartRateBpm.coerceIn(25, 240).toDouble(),
        impactDurationMs.coerceIn(0, 2_000).toDouble(),
        speedDeltaKmh.coerceIn(0.0, 180.0),
        postImpactInactivitySeconds.coerceIn(0, 600).toDouble(),
    )
}

data class ImpactModelDecision(
    val action: String,
    val dispatchAlert: Boolean,
    val countdownSeconds: Int,
    val reason: String,
    val safetyOverride: Boolean = false,
)

data class ImpactModelPrediction(
    val severity: String,
    val confidence: Double,
    val probabilities: Map<String, Double>,
    val decision: ImpactModelDecision,
    val modelVersion: String,
)

object ImpactModelInference {
    const val MODEL_VERSION = "impactx-collision-v1"

    private data class ModelClass(
        val label: String,
        val prior: Double,
        val means: DoubleArray,
        val standardDeviations: DoubleArray,
    )

    private val classes = listOf(
        ModelClass("sin_choque", 0.44, doubleArrayOf(1.2, 78.0, 35.0, 1.5, 1.0), doubleArrayOf(0.7, 16.0, 24.0, 2.0, 2.0)),
        ModelClass("leve", 0.23, doubleArrayOf(4.2, 92.0, 110.0, 9.0, 4.0), doubleArrayOf(1.3, 22.0, 55.0, 6.0, 5.0)),
        ModelClass("moderado", 0.16, doubleArrayOf(7.8, 116.0, 245.0, 28.0, 18.0), doubleArrayOf(2.0, 28.0, 105.0, 13.0, 17.0)),
        ModelClass("grave", 0.11, doubleArrayOf(13.5, 145.0, 490.0, 55.0, 62.0), doubleArrayOf(3.0, 34.0, 180.0, 21.0, 45.0)),
        ModelClass("critico", 0.06, doubleArrayOf(20.5, 168.0, 820.0, 88.0, 150.0), doubleArrayOf(4.0, 42.0, 260.0, 28.0, 78.0)),
    )

    fun predict(input: ImpactModelInput): ImpactModelPrediction {
        val vector = input.asVector()
        val logits = classes.map { modelClass -> logLikelihood(modelClass, vector) }
        val probabilities = softmax(logits)
        val winnerIndex = probabilities.indices.maxBy { probabilities[it] }
        val winner = classes[winnerIndex]
        val probabilityMap = classes.indices.associate { index ->
            classes[index].label to probabilities[index]
        }
        val confidence = probabilities[winnerIndex]

        return ImpactModelPrediction(
            severity = winner.label,
            confidence = confidence,
            probabilities = probabilityMap,
            decision = resolveDecision(winner.label, confidence, input),
            modelVersion = MODEL_VERSION,
        )
    }

    private fun logLikelihood(modelClass: ModelClass, vector: DoubleArray): Double {
        var score = ln(modelClass.prior)

        vector.indices.forEach { index ->
            val standardDeviation = max(modelClass.standardDeviations[index], 1e-6)
            val delta = (vector[index] - modelClass.means[index]) / standardDeviation
            score += -0.5 * delta * delta - ln(standardDeviation)
        }

        return score
    }

    private fun softmax(logits: List<Double>): DoubleArray {
        val maximum = logits.max()
        val exponentials = logits.map { value -> exp(value - maximum) }
        val total = exponentials.sum()

        if (total <= 0.0 || total.isNaN() || total.isInfinite()) {
            return DoubleArray(logits.size) { 1.0 / logits.size }
        }

        return DoubleArray(logits.size) { index -> exponentials[index] / total }
    }

    private fun resolveDecision(
        severity: String,
        confidence: Double,
        input: ImpactModelInput,
    ): ImpactModelDecision {
        val extremeImpact = input.gForcePeak >= 18.0 || input.speedDeltaKmh >= 85.0
        val prolongedInactivity = input.gForcePeak >= 11.0 && input.postImpactInactivitySeconds >= 45
        val physiologicRisk = input.gForcePeak >= 8.0 &&
            (input.heartRateBpm <= 38 || input.heartRateBpm >= 205)

        if (extremeImpact || prolongedInactivity || physiologicRisk) {
            return ImpactModelDecision(
                action = "alerta_inmediata",
                dispatchAlert = true,
                countdownSeconds = 0,
                reason = "safety_override",
                safetyOverride = true,
            )
        }

        return when (severity) {
            "critico", "grave" -> ImpactModelDecision(
                "alerta_inmediata", true, 0, "high_severity",
            )
            "moderado" -> ImpactModelDecision(
                "validacion_5_segundos", true, 5, "moderate_severity",
            )
            "leve" -> if (confidence >= 0.55) {
                ImpactModelDecision("validacion_10_segundos", true, 10, "mild_severity")
            } else {
                ImpactModelDecision("sin_alerta", false, 0, "low_confidence")
            }
            else -> ImpactModelDecision("sin_alerta", false, 0, "no_dispatch")
        }
    }
}
