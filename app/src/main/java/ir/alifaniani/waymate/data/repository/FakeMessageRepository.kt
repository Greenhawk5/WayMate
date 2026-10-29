package ir.alifaniani.waymate.data.repository

import ir.alifaniani.waymate.core.common.Result
import ir.alifaniani.waymate.domain.model.TacticalMessage
import ir.alifaniani.waymate.domain.repository.MessageRepository

/**
 * In-memory test fake for [MessageRepository].
 */
class FakeMessageRepository(
    initialMessages: List<TacticalMessage> = emptyList()
) : MessageRepository {

    private val messages = initialMessages.toMutableList()
    var shouldFail: Boolean = false
    var failureMessage: String = "خطای شبیه‌سازی‌شده در پیام‌ها"

    override suspend fun getMessages(): Result<List<TacticalMessage>> {
        if (shouldFail) return Result.Error(Exception(failureMessage), failureMessage)
        return Result.Success(messages.toList())
    }

    override suspend fun acknowledgeMessage(messageId: String): Result<Boolean> {
        if (shouldFail) return Result.Error(Exception(failureMessage), failureMessage)
        val index = messages.indexOfFirst { it.id == messageId }
        if (index != -1) {
            messages[index] = messages[index].copy(isAcknowledged = true)
            return Result.Success(true)
        }
        return Result.Error(Exception("پیام یافت نشد"), "پیام یافت نشد")
    }

    override suspend fun sendMessage(
        body: String,
        isUrgent: Boolean,
        targetTeamId: String?
    ): Result<TacticalMessage> {
        if (shouldFail) return Result.Error(Exception(failureMessage), failureMessage)
        val msg = TacticalMessage(
            id = "fake_msg_${System.currentTimeMillis()}",
            senderCallsign = "فرمانده آزمایشی",
            body = body,
            isUrgent = isUrgent,
            createdAt = System.currentTimeMillis(),
            isAcknowledged = false
        )
        messages.add(0, msg)
        return Result.Success(msg)
    }

    fun setMessages(newMessages: List<TacticalMessage>) {
        messages.clear()
        messages.addAll(newMessages)
    }
}
