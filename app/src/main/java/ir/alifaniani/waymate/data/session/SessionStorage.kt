package ir.alifaniani.waymate.data.session

import ir.alifaniani.waymate.domain.model.User

/**
 * Interface contract for persisting and retrieving authenticated sessions.
 */
interface SessionStorage {

    /**
     * Securely stores authentication token and authenticated user metadata.
     */
    fun saveSession(token: String, user: User, expiresAt: Long)

    /**
     * Retrieves the stored JWT authentication token, if any.
     */
    fun getToken(): String?

    /**
     * Retrieves the cached authenticated user domain model, if any.
     */
    fun getUser(): User?

    /**
     * Retrieves the session expiration timestamp in milliseconds.
     */
    fun getExpiresAt(): Long

    /**
     * Checks whether an active, non-expired session is available.
     */
    fun isSessionValid(): Boolean

    /**
     * Completely purges the stored authentication token and user session data.
     */
    fun clearSession()
}
