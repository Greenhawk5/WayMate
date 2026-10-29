package ir.alifaniani.waymate.data.session

import ir.alifaniani.waymate.domain.model.User
import ir.alifaniani.waymate.domain.model.UserRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SessionStorageTest {

    private lateinit var sessionStorage: SessionStorage

    @Before
    fun setup() {
        sessionStorage = InMemorySessionStorage()
    }

    @Test
    fun initialState_hasNoSession() {
        assertNull(sessionStorage.getToken())
        assertNull(sessionStorage.getUser())
        assertEquals(0L, sessionStorage.getExpiresAt())
        assertFalse(sessionStorage.isSessionValid())
    }

    @Test
    fun saveSession_persistsTokenAndUser() {
        val user = User(
            id = "usr_001",
            username = "test_commander",
            role = UserRole.COMMANDER,
            callsign = "Eagle-1"
        )
        val futureExpiry = System.currentTimeMillis() + 3600000L // +1 hour

        sessionStorage.saveSession("jwt_token_sample", user, futureExpiry)

        assertEquals("jwt_token_sample", sessionStorage.getToken())
        assertNotNull(sessionStorage.getUser())
        assertEquals("test_commander", sessionStorage.getUser()?.username)
        assertEquals(UserRole.COMMANDER, sessionStorage.getUser()?.role)
        assertEquals(futureExpiry, sessionStorage.getExpiresAt())
        assertTrue(sessionStorage.isSessionValid())
    }

    @Test
    fun isSessionValid_returnsFalseWhenTokenIsExpired() {
        val user = User("usr_002", "expired_user", UserRole.TEAM_LEAD, "Falcon-2")
        val pastExpiry = System.currentTimeMillis() - 10000L // 10 seconds ago

        sessionStorage.saveSession("expired_token", user, pastExpiry)

        assertFalse("Expired session should not be valid", sessionStorage.isSessionValid())
    }

    @Test
    fun clearSession_removesAllStoredData() {
        val user = User("usr_003", "logout_user", UserRole.OWNER, "Alpha-Base")
        sessionStorage.saveSession("valid_token", user, System.currentTimeMillis() + 60000)
        assertTrue(sessionStorage.isSessionValid())

        sessionStorage.clearSession()

        assertNull(sessionStorage.getToken())
        assertNull(sessionStorage.getUser())
        assertEquals(0L, sessionStorage.getExpiresAt())
        assertFalse(sessionStorage.isSessionValid())
    }
}
