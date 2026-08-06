package com.example.impactx.data.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.util.concurrent.TimeUnit

object ImpactSyncScheduler {
    private const val UNIQUE_CRITICAL = "impactx-critical-sync"
    private const val UNIQUE_TELEMETRY_DELAYED = "impactx-telemetry-delayed"
    private const val UNIQUE_TELEMETRY_NOW = "impactx-telemetry-now"
    private const val UNIQUE_PERIODIC = "impactx-periodic-sync"
    const val INPUT_FORCE_TELEMETRY = "forceTelemetry"

    private val connectedConstraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    fun enqueueCritical(context: Context) {
        val request = OneTimeWorkRequestBuilder<ImpactSyncWorker>()
            .setConstraints(connectedConstraints)
            .setInputData(workDataOf(INPUT_FORCE_TELEMETRY to false))
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .addTag(UNIQUE_CRITICAL)
            .build()
        WorkManager.getInstance(context.applicationContext)
            .enqueueUniqueWork(UNIQUE_CRITICAL, ExistingWorkPolicy.KEEP, request)
    }

    fun enqueueTelemetry(context: Context, immediate: Boolean = false, force: Boolean = false) {
        val builder = OneTimeWorkRequestBuilder<ImpactSyncWorker>()
            .setConstraints(connectedConstraints)
            .setInputData(workDataOf(INPUT_FORCE_TELEMETRY to force))

        if (!immediate) {
            builder.setInitialDelay(BatchSyncPolicy.MAX_WAIT_MS, TimeUnit.MILLISECONDS)
        }

        val request = builder.addTag(
            if (immediate) UNIQUE_TELEMETRY_NOW else UNIQUE_TELEMETRY_DELAYED
        ).build()

        WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
            if (immediate) UNIQUE_TELEMETRY_NOW else UNIQUE_TELEMETRY_DELAYED,
            if (immediate) ExistingWorkPolicy.REPLACE else ExistingWorkPolicy.KEEP,
            request,
        )
    }

    fun ensurePeriodic(context: Context) {
        val request = PeriodicWorkRequestBuilder<ImpactSyncWorker>(15, TimeUnit.MINUTES)
            .setConstraints(connectedConstraints)
            .setInputData(workDataOf(INPUT_FORCE_TELEMETRY to false))
            .addTag(UNIQUE_PERIODIC)
            .build()
        WorkManager.getInstance(context.applicationContext).enqueueUniquePeriodicWork(
            UNIQUE_PERIODIC,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }
}
