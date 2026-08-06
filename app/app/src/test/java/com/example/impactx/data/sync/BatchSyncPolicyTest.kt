package com.example.impactx.data.sync

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BatchSyncPolicyTest {
    @Test
    fun emptyQueueDoesNotFlushEvenWhenForced() {
        assertFalse(BatchSyncPolicy.shouldFlush(0, null, 100_000L, force = true))
    }

    @Test
    fun twentyEventsFlushImmediately() {
        assertTrue(BatchSyncPolicy.shouldFlush(20, 99_999L, 100_000L, force = false))
    }

    @Test
    fun partialBatchFlushesAfterThirtySeconds() {
        assertTrue(BatchSyncPolicy.shouldFlush(3, 70_000L, 100_000L, force = false))
    }

    @Test
    fun partialFreshBatchWaits() {
        assertFalse(BatchSyncPolicy.shouldFlush(3, 90_001L, 100_000L, force = false))
    }

    @Test
    fun manualSyncFlushesPartialBatch() {
        assertTrue(BatchSyncPolicy.shouldFlush(1, 99_999L, 100_000L, force = true))
    }
}
