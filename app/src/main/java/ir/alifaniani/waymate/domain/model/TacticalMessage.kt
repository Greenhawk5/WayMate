package ir.alifaniani.waymate.domain.model

/**
 * In-band operational order or broadcast dispatch received from the Commander.
 */
data class TacticalMessage(
    val id: String,
    val senderCallsign: String,
    val body: String,
    val isUrgent: Boolean,
    val createdAt: Long,
    val isAcknowledged: Boolean,
    val isPendingSync: Boolean = false
)
