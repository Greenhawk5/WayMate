package ir.alifaniani.waymate.data.repository

import ir.alifaniani.waymate.core.common.Result
import ir.alifaniani.waymate.domain.model.User
import ir.alifaniani.waymate.domain.model.UserRole
import ir.alifaniani.waymate.domain.repository.AuthRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * DEVELOPMENT AND TESTING DEMONSTRATION ONLY.
 *
 * This class provides an in-memory fake implementation of [AuthRepository]
 * using synthetic test accounts. It does NOT connect to a real backend, does NOT
 * provide security, and does NOT store passwords.
 */
class FakeAuthRepository(
    private val simulatedDelayMs: Long = 300L
) : AuthRepository {

    // Synthetic test accounts for development and automated tests
    private val syntheticUsers = mapOf(
        "commander_demo" to User(
            id = "usr_cmd_001",
            username = "commander_demo",
            role = UserRole.COMMANDER,
            callsign = "Eagle-1 (فرمانده)"
        ),
        "patrol_lead_demo" to User(
            id = "usr_lead_001",
            username = "patrol_lead_demo",
            role = UserRole.TEAM_LEAD,
            callsign = "Patrol-Alpha (سرتیم آلفا)"
        )
    )

    private val _currentUser = MutableStateFlow<User?>(null)

    override fun observeCurrentUser(): StateFlow<User?> = _currentUser.asStateFlow()

    override suspend fun login(username: String, password: String): Result<User> {
        if (simulatedDelayMs > 0) {
            delay(simulatedDelayMs)
        }

        val cleanUsername = username.trim()
        if (cleanUsername.isBlank() || password.isBlank()) {
            return Result.Error(
                IllegalArgumentException("نام کاربری و گذرواژه الزامی است.")
            )
        }

        // Demo credential validation (password must be "demo123" for any synthetic user)
        val user = syntheticUsers[cleanUsername]
        return if (user != null && password == "demo123") {
            _currentUser.value = user
            Result.Success(user)
        } else {
            Result.Error(
                SecurityException("نام کاربری یا گذرواژه نامعتبر است.")
            )
        }
    }

    override suspend fun logout(): Result<Unit> {
        _currentUser.value = null
        return Result.Success(Unit)
    }
}
