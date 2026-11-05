package ir.alifaniani.waymate.data.repository

import ir.alifaniani.waymate.core.common.DefaultDispatcherProvider
import ir.alifaniani.waymate.core.common.DispatcherProvider
import ir.alifaniani.waymate.core.common.Result
import ir.alifaniani.waymate.data.dto.LoginRequestDto
import ir.alifaniani.waymate.data.local.OfflineStorage
import ir.alifaniani.waymate.data.mapper.toDomain
import ir.alifaniani.waymate.data.network.AuthApiService
import ir.alifaniani.waymate.data.network.NetworkErrorMapper
import ir.alifaniani.waymate.data.session.SessionStorage
import ir.alifaniani.waymate.domain.model.User
import ir.alifaniani.waymate.domain.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/**
 * Production implementation of [AuthRepository] that coordinates between the
 * Cloudflare Workers authentication backend, secure session persistence, and domain state.
 */
class RealAuthRepository(
    private val authApiService: AuthApiService,
    private val sessionStorage: SessionStorage,
    private val dispatcherProvider: DispatcherProvider = DefaultDispatcherProvider(),
    private val offlineStorage: OfflineStorage? = null
) : AuthRepository {

    private val _currentUser = MutableStateFlow<User?>(null)

    init {
        restoreSession()
    }

    override fun observeCurrentUser(): StateFlow<User?> = _currentUser.asStateFlow()

    override suspend fun login(username: String, password: String): Result<User> =
        withContext(dispatcherProvider.io) {
            val cleanUsername = username.trim()
            if (cleanUsername.isBlank() || password.isBlank()) {
                return@withContext Result.Error(
                    IllegalArgumentException("نام کاربری و گذرواژه الزامی است.")
                )
            }

            try {
                val response = authApiService.login(
                    LoginRequestDto(
                        username = cleanUsername,
                        password = password
                    )
                )

                if (response.isSuccessful) {
                    val body = response.body()
                    val loginData = body?.data
                    if (body != null && body.success && loginData != null) {
                        val domainUser = loginData.user.toDomain()
                        sessionStorage.saveSession(
                            token = loginData.token,
                            user = domainUser,
                            expiresAt = loginData.expiresAt
                        )
                        _currentUser.value = domainUser
                        Result.Success(domainUser)
                    } else {
                        val errorMsg = body?.error?.message ?: "پاسخ نامعتبر از سرور دریافت شد."
                        Result.Error(Exception(errorMsg), errorMsg)
                    }
                } else {
                    val apiError = NetworkErrorMapper.parseApiError(response.errorBody())
                    val errorMessage = NetworkErrorMapper.mapHttpError(response.code(), apiError)
                    Result.Error(Exception(errorMessage), errorMessage)
                }
            } catch (e: Exception) {
                val errorMessage = NetworkErrorMapper.mapThrowable(e)
                Result.Error(e, errorMessage)
            }
        }

    override suspend fun logout(): Result<Unit> = withContext(dispatcherProvider.io) {
        try {
            // Attempt remote revocation request if online
            authApiService.logout()
        } catch (_: Exception) {
            // Local session purge must always succeed regardless of remote connectivity
        } finally {
            sessionStorage.clearSession()
            offlineStorage?.clearAll()
            _currentUser.value = null
        }
        Result.Success(Unit)
    }

    private fun restoreSession() {
        if (sessionStorage.isSessionValid()) {
            val cachedUser = sessionStorage.getUser()
            if (cachedUser != null) {
                _currentUser.value = cachedUser
            } else {
                sessionStorage.clearSession()
            }
        } else {
            sessionStorage.clearSession()
        }
    }
}
