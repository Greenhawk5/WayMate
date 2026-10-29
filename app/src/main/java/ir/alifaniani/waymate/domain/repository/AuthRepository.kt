package ir.alifaniani.waymate.domain.repository

import ir.alifaniani.waymate.core.common.Result
import ir.alifaniani.waymate.domain.model.User
import kotlinx.coroutines.flow.StateFlow

/**
 * Contract for authentication and session observation.
 * Pure Kotlin interface independent of Android and UI frameworks.
 */
interface AuthRepository {
    suspend fun login(username: String, password: String): Result<User>
    suspend fun logout(): Result<Unit>
    fun observeCurrentUser(): StateFlow<User?>
}
