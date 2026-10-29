package ir.alifaniani.waymate.data.repository

import ir.alifaniani.waymate.TestDispatcherProvider
import ir.alifaniani.waymate.core.common.Result
import ir.alifaniani.waymate.data.dto.AcknowledgeResponseDto
import ir.alifaniani.waymate.data.dto.ApiResponseDto
import ir.alifaniani.waymate.data.dto.SendMessageRequestDto
import ir.alifaniani.waymate.data.dto.TacticalMessageDto
import ir.alifaniani.waymate.data.network.MessageApiService
import ir.alifaniani.waymate.domain.model.TacticalMessage
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response

class MessageRepositoryTest {

    private val testDispatcherProvider = TestDispatcherProvider()

    @Test
    fun fakeMessageRepository_getMessages_returnsSeededMessages() = runBlocking {
        val seeded = listOf(
            TacticalMessage(
                id = "msg_001",
                senderCallsign = "Commander",
                body = "دستور گشت‌زنی",
                isUrgent = true,
                createdAt = 1000L,
                isAcknowledged = false
            )
        )
        val repository = FakeMessageRepository(seeded)

        val result = repository.getMessages()
        assertTrue(result is Result.Success)
        val messages = (result as Result.Success).data
        assertEquals(1, messages.size)
        assertEquals("msg_001", messages[0].id)
        assertTrue(messages[0].isUrgent)
        assertFalse(messages[0].isAcknowledged)
    }

    @Test
    fun fakeMessageRepository_acknowledgeMessage_updatesAckState() = runBlocking {
        val seeded = listOf(
            TacticalMessage(
                id = "msg_001",
                senderCallsign = "Commander",
                body = "دستور توقف",
                isUrgent = false,
                createdAt = 1000L,
                isAcknowledged = false
            )
        )
        val repository = FakeMessageRepository(seeded)

        val ackResult = repository.acknowledgeMessage("msg_001")
        assertTrue(ackResult is Result.Success)

        val fetchResult = repository.getMessages()
        assertTrue((fetchResult as Result.Success).data[0].isAcknowledged)
    }

    @Test
    fun fakeMessageRepository_sendMessage_prependsNewMessage() = runBlocking {
        val repository = FakeMessageRepository()

        val sendResult = repository.sendMessage("دستور پیشروی", isUrgent = true)
        assertTrue(sendResult is Result.Success)

        val fetchResult = repository.getMessages()
        val list = (fetchResult as Result.Success).data
        assertEquals(1, list.size)
        assertEquals("دستور پیشروی", list[0].body)
        assertTrue(list[0].isUrgent)
    }

    @Test
    fun realMessageRepository_getMessages_success() = runBlocking {
        val fakeApi = object : MessageApiService {
            override suspend fun getMessages(): Response<ApiResponseDto<List<TacticalMessageDto>>> {
                val list = listOf(
                    TacticalMessageDto(
                        id = "msg_real_01",
                        senderCallsign = "Eagle",
                        body = "دستور عملیاتی",
                        isUrgent = true,
                        createdAt = 2000L,
                        acknowledged = false
                    )
                )
                return Response.success(ApiResponseDto(success = true, data = list))
            }

            override suspend fun acknowledgeMessage(id: String): Response<ApiResponseDto<AcknowledgeResponseDto>> {
                return Response.success(ApiResponseDto(success = true, data = AcknowledgeResponseDto(acknowledgedAt = 3000L)))
            }

            override suspend fun sendMessage(request: SendMessageRequestDto): Response<ApiResponseDto<TacticalMessageDto>> {
                val dto = TacticalMessageDto(
                    id = "msg_new",
                    senderCallsign = "Cmd",
                    body = request.body,
                    isUrgent = request.isUrgent,
                    createdAt = 4000L,
                    acknowledged = false
                )
                return Response.success(ApiResponseDto(success = true, data = dto))
            }
        }

        val repository = RealMessageRepository(fakeApi, testDispatcherProvider)
        val result = repository.getMessages()
        assertTrue(result is Result.Success)
        val messages = (result as Result.Success).data
        assertEquals(1, messages.size)
        assertEquals("msg_real_01", messages[0].id)
    }

    @Test
    fun realMessageRepository_getMessages_httpError_mapsToError() = runBlocking {
        val fakeApi = object : MessageApiService {
            override suspend fun getMessages(): Response<ApiResponseDto<List<TacticalMessageDto>>> {
                val errorBody = "{\"success\":false,\"error\":{\"code\":\"UNAUTHORIZED\",\"message\":\"نشست منقضی شده است\"}}"
                    .toResponseBody("application/json".toMediaType())
                return Response.error(401, errorBody)
            }

            override suspend fun acknowledgeMessage(id: String): Response<ApiResponseDto<AcknowledgeResponseDto>> {
                throw NotImplementedError()
            }

            override suspend fun sendMessage(request: SendMessageRequestDto): Response<ApiResponseDto<TacticalMessageDto>> {
                throw NotImplementedError()
            }
        }

        val repository = RealMessageRepository(fakeApi, testDispatcherProvider)
        val result = repository.getMessages()
        assertTrue(result is Result.Error)
    }
}
