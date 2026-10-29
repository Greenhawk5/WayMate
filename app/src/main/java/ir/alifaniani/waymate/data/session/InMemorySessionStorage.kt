package ir.alifaniani.waymate.data.session

import ir.alifaniani.waymate.domain.model.User

/**
 * In-memory implementation of [SessionStorage] primarily used for unit testing
 * and environments where Android Keystore is unavailable.
 */
class InMemorySessionStorage : SessionStorage {

    private var storedToken: String? = null
    private var storedUser: User? = null
    private var storedExpiresAt: Long = 0L

    override fun saveSession(token: String, user: User, expiresAt: Long) {
        storedToken = token
        storedUser = user
        storedExpiresAt = expiresAt
    }

    override fun getToken(): String? = storedToken

    override fun getUser(): User? = storedUser

    override fun getExpiresAt(): Long = storedExpiresAt

    override fun isSessionValid(): Boolean {
        val token = storedToken ?: return false
        if (token.isBlank()) return false
        if (storedExpiresAt > 0L && System.currentTimeMillis() >= storedExpiresAt) {
            return false
        }
        return true
    }

    override fun clearSession() {
        storedToken = null
        storedUser = null
        storedExpiresAt = 0L
    }
}
