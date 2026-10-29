package ir.alifaniani.waymate.data

import ir.alifaniani.waymate.core.common.Result
import ir.alifaniani.waymate.data.repository.FakeAuthRepository
import ir.alifaniani.waymate.domain.model.UserRole
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FakeAuthRepositoryTest {

    private lateinit var repository: FakeAuthRepository

    @Before
    fun setup() {
        // Zero delay for deterministic, rapid test execution
        repository = FakeAuthRepository(simulatedDelayMs = 0L)
    }

    @Test
    fun login_withValidCommanderCredentials_returnsSuccessAndUpdatesUser() = runBlocking {
        val result = repository.login("commander_demo", "demo123")

        assertTrue("Expected Result.Success", result is Result.Success)
        val user = (result as Result.Success).data
        assertEquals("commander_demo", user.username)
        assertEquals(UserRole.COMMANDER, user.role)
        assertEquals(user, repository.observeCurrentUser().value)
    }

    @Test
    fun login_withValidPatrolLeadCredentials_returnsSuccessAndUpdatesUser() = runBlocking {
        val result = repository.login("patrol_lead_demo", "demo123")

        assertTrue("Expected Result.Success", result is Result.Success)
        val user = (result as Result.Success).data
        assertEquals("patrol_lead_demo", user.username)
        assertEquals(UserRole.TEAM_LEAD, user.role)
        assertEquals(user, repository.observeCurrentUser().value)
    }

    @Test
    fun login_withInvalidPassword_returnsErrorAndDoesNotUpdateUser() = runBlocking {
        val result = repository.login("commander_demo", "wrong_password")

        assertTrue("Expected Result.Error", result is Result.Error)
        assertNull("Current user should remain null", repository.observeCurrentUser().value)
    }

    @Test
    fun login_withUnknownUsername_returnsError() = runBlocking {
        val result = repository.login("unknown_user", "demo123")

        assertTrue("Expected Result.Error", result is Result.Error)
        assertNull(repository.observeCurrentUser().value)
    }

    @Test
    fun login_withBlankUsernameOrPassword_returnsError() = runBlocking {
        val emptyUserResult = repository.login("   ", "demo123")
        assertTrue("Blank username should fail", emptyUserResult is Result.Error)

        val emptyPassResult = repository.login("commander_demo", "")
        assertTrue("Blank password should fail", emptyPassResult is Result.Error)
    }

    @Test
    fun logout_clearsCurrentUser() = runBlocking {
        repository.login("commander_demo", "demo123")
        assertNotNull(repository.observeCurrentUser().value)

        val logoutResult = repository.logout()
        assertTrue("Logout should succeed", logoutResult is Result.Success)
        assertNull("Current user should be null after logout", repository.observeCurrentUser().value)
    }
}
