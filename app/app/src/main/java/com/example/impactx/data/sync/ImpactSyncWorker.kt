package com.example.impactx.data.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class ImpactSyncWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val forceTelemetry = inputData.getBoolean(
            ImpactSyncScheduler.INPUT_FORCE_TELEMETRY,
            false,
        )
        val outcome = ImpactSyncProcessor.run(
            applicationContext,
            forceTelemetry = forceTelemetry,
            includeTelemetry = true,
        )
        return if (outcome.retryNeeded) Result.retry() else Result.success()
    }
}
