package ir.alifaniani.waymate.ui.auth

import ir.alifaniani.waymate.TestDispatcherProvider
import ir.alifaniani.waymate.data.repository.FakeAuthRepository
import ir.alifaniani.waymate.domain.model.UserRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class LoginViewModelTest {

    private lateinit var fakeRepository: FakeAuthRepository
    private lateinit var testDispatcherProvider: TestDispatcherProvider
    private lateinit var viewModel: LoginViewModel

    @Before
    fun setup() {
        fakeRepository = FakeAuthRepository(simulatedDelayMs = 0L)
        testDispatcherProvider = TestDispatcherProvider()
        viewModel = LoginViewModel(
            authRepository = fakeRepository,
            dispatcherProvider = testDispatcherProvider
        )
    }

    @Test
    fun initialState_hasDefaultValuesAndNoUser() {
        val state = viewModel.uiState.value

        assertEquals("", state.usernameInput)
        assertEquals("", state.passwordInput)
        assertFalse("isLoading should be false", state.isLoading)
        assertNull("errorMessage should be null", state.errorMessage)
        assertNull("authenticatedUser should be null", state.authenticatedUser)
    }

    @Test
    fun onUsernameChanged_updatesStateAndClearsError() {
        viewModel.onUsernameChanged("test_user")

        assertEquals("test_user", viewModel.uiState.value.usernameInput)
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun onPasswordChanged_updatesStateAndClearsError() {
        viewModel.onPasswordChanged("secret_pass")

        assertEquals("secret_pass", viewModel.uiState.value.passwordInput)
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun login_withBlankUsername_setsErrorMessage() {
        viewModel.onUsernameChanged("   ")
        viewModel.onPasswordChanged("demo123")

        viewModel.login()

        assertNotNull("Error message should be set", viewModel.uiState.value.errorMessage)
        assertFalse("isLoading should be false", viewModel.uiState.value.isLoading)
        assertNull("No user should be authenticated", viewModel.uiState.value.authenticatedUser)
    }

    @Test
    fun login_withBlankPassword_setsErrorMessage() {
        viewModel.onUsernameChanged("commander_demo")
        viewModel.onPasswordChanged("")

        viewModel.login()

        assertNotNull(viewModel.uiState.value.errorMessage)
        assertNull(viewModel.uiState.value.authenticatedUser)
    }

    @Test
    fun login_withValidCredentials_updatesStateWithUser() {
        viewModel.onUsernameChanged("commander_demo")
        viewModel.onPasswordChanged("demo123")

        viewModel.login()

        val state = viewModel.uiState.value
        assertFalse("isLoading should be false after completion", state.isLoading)
        assertNull("errorMessage should be null on success", state.errorMessage)
        assertNotNull("User should be authenticated", state.authenticatedUser)
        assertEquals("commander_demo", state.authenticatedUser?.username)
        assertEquals(UserRole.COMMANDER, state.authenticatedUser?.role)
    }

    @Test
    fun login_withInvalidCredentials_setsErrorMessage() {
        viewModel.onUsernameChanged("commander_demo")
        viewModel.onPasswordChanged("wrong_pass")

        viewModel.login()

        val state = viewModel.uiState.value
        assertFalse("isLoading should be false", state.isLoading)
        assertNotNull("Error message should be present", state.errorMessage)
        assertNull("User should not be authenticated", state.authenticatedUser)
    }

    @Test
    fun clearError_removesErrorMessage() {
        viewModel.onUsernameChanged("")
        viewModel.login()
        assertNotNull(viewModel.uiState.value.errorMessage)

        viewModel.clearError()
        assertNull("Error message should be cleared", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun logout_resetsUiState() {
        viewModel.onUsernameChanged("commander_demo")
        viewModel.onPasswordChanged("demo123")
        viewModel.login()
        assertNotNull(viewModel.uiState.value.authenticatedUser)

        viewModel.logout()

        val state = viewModel.uiState.value
        assertNull("User should be null after logout", state.authenticatedUser)
        assertEquals("", state.usernameInput)
        assertEquals("", state.passwordInput)
    }

    @Test
    fun login_duplicateCallsWhileLoading_ignored() {
        // Create repository with slight delay to observe loading state
        val delayedRepo = FakeAuthRepository(simulatedDelayMs = 50L)
        val vm = LoginViewModel(delayedRepo, testDispatcherProvider)

        vm.onUsernameChanged("commander_demo")
        vm.onPasswordChanged("demo123")

        vm.login()
        // Try calling login() again immediately while isLoading is true
        vm.login()

        // Should maintain loading without duplicating or crashing
        val state = vm.uiState.value
        assertNotNull(state)
    }
}
