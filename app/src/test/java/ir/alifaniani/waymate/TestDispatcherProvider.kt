package ir.alifaniani.waymate

import ir.alifaniani.waymate.core.common.DispatcherProvider
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 * Deterministic coroutine dispatcher provider for local JVM unit tests.
 * Uses Dispatchers.Unconfined to execute coroutines synchronously on the test thread.
 */
class TestDispatcherProvider : DispatcherProvider {
    override val main: CoroutineDispatcher = Dispatchers.Unconfined
    override val io: CoroutineDispatcher = Dispatchers.Unconfined
    override val default: CoroutineDispatcher = Dispatchers.Unconfined
    override val unconfined: CoroutineDispatcher = Dispatchers.Unconfined
}
