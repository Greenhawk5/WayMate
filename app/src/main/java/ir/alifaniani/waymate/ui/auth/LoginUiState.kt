package ir.alifaniani.waymate.ui.auth

import ir.alifaniani.waymate.domain.model.User

/**
 * Immutable UI state model for the authentication screen.
 */
data class LoginUiState(
    val usernameInput: String = "",
    val passwordInput: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val authenticatedUser: User? = null
)
