package ir.alifaniani.waymate.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import ir.alifaniani.waymate.core.common.DefaultDispatcherProvider
import ir.alifaniani.waymate.core.common.DispatcherProvider
import ir.alifaniani.waymate.core.common.Result
import ir.alifaniani.waymate.domain.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel managing the state and user actions of the authentication screen.
 */
class LoginViewModel(
    private val authRepository: AuthRepository,
    private val dispatcherProvider: DispatcherProvider = DefaultDispatcherProvider()
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch(dispatcherProvider.main) {
            authRepository.observeCurrentUser().collect { user ->
                _uiState.update { it.copy(authenticatedUser = user) }
            }
        }
    }

    fun onUsernameChanged(newUsername: String) {
        _uiState.update { it.copy(usernameInput = newUsername, errorMessage = null) }
    }

    fun onPasswordChanged(newPassword: String) {
        _uiState.update { it.copy(passwordInput = newPassword, errorMessage = null) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun logout() {
        viewModelScope.launch(dispatcherProvider.main) {
            authRepository.logout()
            _uiState.update { LoginUiState() }
        }
    }

    fun login() {
        val currentState = _uiState.value
        // Prevent duplicate login submissions while a request is in progress
        if (currentState.isLoading) return

        val username = currentState.usernameInput.trim()
        val password = currentState.passwordInput

        if (username.isBlank() || password.isBlank()) {
            _uiState.update {
                it.copy(errorMessage = "لطفاً نام کاربری و گذرواژه را وارد کنید.")
            }
            return
        }

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch(dispatcherProvider.main) {
            when (val result = authRepository.login(username, password)) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            authenticatedUser = result.data,
                            errorMessage = null
                        )
                    }
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = result.message ?: "خطا در احراز هویت."
                        )
                    }
                }
                is Result.Loading -> {
                    _uiState.update { it.copy(isLoading = true) }
                }
            }
        }
    }

    companion object {
        fun provideFactory(
            authRepository: AuthRepository,
            dispatcherProvider: DispatcherProvider = DefaultDispatcherProvider()
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(LoginViewModel::class.java)) {
                    return LoginViewModel(authRepository, dispatcherProvider) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
            }
        }
    }
}
