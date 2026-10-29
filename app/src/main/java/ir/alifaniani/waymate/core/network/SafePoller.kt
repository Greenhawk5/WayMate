package ir.alifaniani.waymate.core.network

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.random.Random

/**
 * Safe, lifecycle-aware polling engine designed for zero-budget tactical field clients.
 *
 * Implements:
 * - Deterministic base polling interval (5,000ms per AD-002)
 * - Exponential backoff on communication failure (5s -> 10s -> 20s -> max 60s)
 * - Random jitter (+/- 10%) to prevent synchronized thundering-herd on the serverless edge
 * - Explicit lifecycle pause/resume controls to completely prevent background battery drain
 * - Non-blocking coroutine execution
 */
class SafePoller(
    val baseIntervalMs: Long = 5_000L,
    val maxIntervalMs: Long = 60_000L,
    private val backoffMultiplier: Double = 2.0,
    private val jitterRatio: Double = 0.1,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default
) {
    var currentIntervalMs: Long = baseIntervalMs
        private set

    var isRunning: Boolean = false
        private set

    var isPaused: Boolean = false
        private set

    private var activeJob: Job? = null

    /**
     * Starts continuous polling within the provided [CoroutineScope].
     * [pollAction] is invoked once immediately and then on each interval.
     * Returning `true` resets the backoff interval to base; returning `false` increments backoff.
     */
    fun start(scope: CoroutineScope, pollAction: suspend () -> Boolean): Job {
        stop()
        isRunning = true
        isPaused = false
        currentIntervalMs = baseIntervalMs

        val job = scope.launch(dispatcher) {
            while (isActive && isRunning) {
                if (!isPaused) {
                    val success = try {
                        pollAction()
                    } catch (e: CancellationException) {
                        throw e
                    } catch (_: Exception) {
                        false
                    }

                    if (success) {
                        currentIntervalMs = baseIntervalMs
                    } else {
                        val jitter = (Random.nextDouble() * 2 - 1) * jitterRatio * currentIntervalMs
                        currentIntervalMs = ((currentIntervalMs * backoffMultiplier) + jitter)
                            .toLong()
                            .coerceIn(baseIntervalMs, maxIntervalMs)
                    }
                }
                delay(currentIntervalMs)
            }
        }
        activeJob = job
        return job
    }

    /**
     * Temporarily pauses polling without cancelling the background loop.
     * Used when the app enters the background or screen turns off.
     */
    fun pause() {
        isPaused = true
    }

    /**
     * Resumes active polling with immediate interval reset.
     * Used when the app returns to the foreground.
     */
    fun resume() {
        if (isPaused) {
            isPaused = false
            currentIntervalMs = baseIntervalMs
        }
    }

    /**
     * Completely halts polling and cancels active background job.
     */
    fun stop() {
        isRunning = false
        isPaused = false
        activeJob?.cancel()
        activeJob = null
    }
}
