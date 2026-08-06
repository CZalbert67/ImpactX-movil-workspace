package com.example.impactx.data.sync

object BatchSyncPolicy {
    const val MAX_BATCH_SIZE = 20
    const val MAX_WAIT_MS = 30_000L

    fun shouldFlush(
        pendingCount: Int,
        oldestCreatedAtMs: Long?,
        nowMs: Long,
        force: Boolean
    ): Boolean {
        if (pendingCount <= 0) return false
        if (force || pendingCount >= MAX_BATCH_SIZE) return true
        return oldestCreatedAtMs != null && nowMs - oldestCreatedAtMs >= MAX_WAIT_MS
    }
}
