package ir.alifaniani.waymate.domain.repository

import ir.alifaniani.waymate.core.common.Result
import ir.alifaniani.waymate.domain.model.TacticalMessage

/**
 * Domain repository contract for tactical messaging and operational announcements.
 */
interface MessageRepository {
    /**
     * Fetches tactical orders and broadcast instructions for the authenticated user/team.
     */
    suspend fun getMessages(): Result<List<TacticalMessage>>

    /**
     * Transmits receipt confirmation for an operational instruction.
     */
    suspend fun acknowledgeMessage(messageId: String): Result<Boolean>

    /**
     * Dispatches an operational instruction or announcement (Commander / Owner only).
     */
    suspend fun sendMessage(
        body: String,
        isUrgent: Boolean,
        targetTeamId: String? = null
    ): Result<TacticalMessage>
}
