package ir.alifaniani.waymate.core.network

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SafePollerTest {

    @Test
    fun start_invokesPollActionImmediately() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val poller = SafePoller(
            baseIntervalMs = 5000L,
            maxIntervalMs = 60000L,
            dispatcher = testDispatcher
        )

        var invocationCount = 0
        poller.start(this) {
            invocationCount++
            true
        }

        testScheduler.advanceTimeBy(1L)
        assertEquals(1, invocationCount)
        assertTrue(poller.isRunning)
        assertFalse(poller.isPaused)

        poller.stop()
    }

    @Test
    fun successfulPoll_resetsBackoffToBaseInterval() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val poller = SafePoller(
            baseIntervalMs = 5000L,
            maxIntervalMs = 60000L,
            dispatcher = testDispatcher
        )

        var callCount = 0
        poller.start(this) {
            callCount++
            true
        }

        testScheduler.advanceTimeBy(10001L)
        assertEquals(3, callCount) // at 0ms, 5000ms, 10000ms
        assertEquals(5000L, poller.currentIntervalMs)

        poller.stop()
    }

    @Test
    fun failedPoll_appliesExponentialBackoff() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val poller = SafePoller(
            baseIntervalMs = 5000L,
            maxIntervalMs = 60000L,
            backoffMultiplier = 2.0,
            jitterRatio = 0.0, // Zero jitter for deterministic unit test
            dispatcher = testDispatcher
        )

        var returnSuccess = false
        poller.start(this) {
            returnSuccess
        }

        testScheduler.advanceTimeBy(1L) // First poll runs at 0ms and fails
        // After failure at 0ms, next interval should backoff: 5000 * 2 = 10000ms
        assertEquals(10000L, poller.currentIntervalMs)

        testScheduler.advanceTimeBy(10001L) // Second poll at 10000ms fails
        // Next interval should backoff: 10000 * 2 = 20000ms
        assertEquals(20000L, poller.currentIntervalMs)

        poller.stop()
    }

    @Test
    fun pauseAndResume_controlsPollingLifecycle() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val poller = SafePoller(
            baseIntervalMs = 5000L,
            dispatcher = testDispatcher
        )

        var callCount = 0
        poller.start(this) {
            callCount++
            true
        }

        testScheduler.advanceTimeBy(5001L) // Runs at 0ms and 5000ms
        assertEquals(2, callCount)

        poller.pause()
        assertTrue(poller.isPaused)

        testScheduler.advanceTimeBy(15000L) // Paused -> no additional invocations
        assertEquals(2, callCount)

        poller.resume()
        assertFalse(poller.isPaused)
        assertEquals(5000L, poller.currentIntervalMs)

        testScheduler.advanceTimeBy(5001L)
        assertTrue(callCount >= 3)

        poller.stop()
        assertFalse(poller.isRunning)
    }

    @Test
    fun stop_cancelsPollingLoop() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val poller = SafePoller(
            baseIntervalMs = 5000L,
            dispatcher = testDispatcher
        )

        var count = 0
        poller.start(this) {
            count++
            true
        }

        testScheduler.advanceTimeBy(1L)
        assertEquals(1, count)

        poller.stop()
        assertFalse(poller.isRunning)

        testScheduler.advanceTimeBy(20000L)
        assertEquals(1, count) // Did not increase
    }
}
