package ir.alifaniani.waymate.data.repository

import ir.alifaniani.waymate.core.common.DefaultDispatcherProvider
import ir.alifaniani.waymate.core.common.DispatcherProvider
import ir.alifaniani.waymate.core.common.Result
import ir.alifaniani.waymate.core.network.ApiException
import ir.alifaniani.waymate.data.dto.CreatePointRequestDto
import ir.alifaniani.waymate.data.dto.UpdatePointRequestDto
import ir.alifaniani.waymate.data.local.OfflineStorage
import ir.alifaniani.waymate.data.local.model.ActionType
import ir.alifaniani.waymate.data.local.model.QueuedAction
import ir.alifaniani.waymate.data.mapper.toDomain
import ir.alifaniani.waymate.data.network.NetworkErrorMapper
import ir.alifaniani.waymate.data.network.PointOfInterestApiService
import ir.alifaniani.waymate.data.sync.CreatePointQueuePayload
import ir.alifaniani.waymate.data.sync.DeletePointQueuePayload
import ir.alifaniani.waymate.data.sync.UpdatePointQueuePayload
import ir.alifaniani.waymate.domain.model.PointCategory
import ir.alifaniani.waymate.domain.model.PointOfInterest
import ir.alifaniani.waymate.domain.repository.PointOfInterestRepository
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import retrofit2.Response
import java.util.UUID

/**
 * Production implementation of [PointOfInterestRepository] featuring:
 * 1. Cache-first and offline-resilient reads.
 * 2. Automatic Store-and-Forward offline mutation queuing on network failures.
 * 3. Optimistic local state updates.
 */
class RealPointOfInterestRepository(
    private val pointApiService: PointOfInterestApiService,
    private val offlineStorage: OfflineStorage,
    private val dispatcherProvider: DispatcherProvider = DefaultDispatcherProvider(),
    private val json: Json = Json { ignoreUnknownKeys = true }
) : PointOfInterestRepository {

    override suspend fun getPoints(
        teamId: String?,
        forceRefresh: Boolean
    ): Result<List<PointOfInterest>> = withContext(dispatcherProvider.io) {
        val cached = offlineStorage.getCachedPoints(teamId)
        if (!forceRefresh && cached.isNotEmpty()) {
            return@withContext Result.Success(cached)
        }

        try {
            val response = pointApiService.getPoints(teamId = teamId)
            if (response.isSuccessful && response.body()?.success == true) {
                val dtos = response.body()?.data ?: emptyList()
                val points = dtos.map { it.toDomain() }
                offlineStorage.savePoints(points)
                Result.Success(points)
            } else {
                if (cached.isNotEmpty()) {
                    Result.Success(cached)
                } else {
                    mapErrorResponse(response)
                }
            }
        } catch (e: Exception) {
            if (cached.isNotEmpty()) {
                Result.Success(cached)
            } else {
                mapException(e)
            }
        }
    }

    override suspend fun getPointById(id: String): Result<PointOfInterest> = withContext(dispatcherProvider.io) {
        val cached = offlineStorage.getCachedPointById(id)
        try {
            val response = pointApiService.getPointById(id)
            if (response.isSuccessful && response.body()?.data != null) {
                val domain = response.body()!!.data!!.toDomain()
                offlineStorage.saveLocalPoint(domain)
                Result.Success(domain)
            } else {
                if (cached != null) {
                    Result.Success(cached)
                } else {
                    mapErrorResponse(response)
                }
            }
        } catch (e: Exception) {
            if (cached != null) {
                Result.Success(cached)
            } else {
                mapException(e)
            }
        }
    }

    override suspend fun createPoint(
        title: String,
        description: String,
        category: PointCategory,
        latitude: Double,
        longitude: Double,
        teamId: String?
    ): Result<PointOfInterest> = withContext(dispatcherProvider.io) {
        val tempId = "poi_local_${UUID.randomUUID().toString().replace("-", "")}"
        val now = System.currentTimeMillis()
        val localPoint = PointOfInterest(
            id = tempId,
            teamId = teamId ?: "",
            title = title,
            description = description,
            category = category,
            latitude = latitude,
            longitude = longitude,
            createdByUserId = "",
            createdByCallsign = "شما (در صف ارسال)",
            updatedByUserId = "",
            updatedByCallsign = "شما (در صف ارسال)",
            isDeleted = false,
            version = 1,
            createdAt = now,
            updatedAt = now,
            isPendingSync = true
        )

        try {
            val request = CreatePointRequestDto(
                title = title,
                description = description,
                category = category.name,
                latitude = latitude,
                longitude = longitude,
                teamId = teamId
            )
            val response = pointApiService.createPoint(request)
            if (response.isSuccessful && response.body()?.data != null) {
                val serverPoint = response.body()!!.data!!.toDomain().copy(isPendingSync = false)
                offlineStorage.saveLocalPoint(serverPoint)
                Result.Success(serverPoint)
            } else {
                mapErrorResponse(response)
            }
        } catch (e: Exception) {
            // Network failure: queue for offline sync and save locally
            offlineStorage.saveLocalPoint(localPoint)
            val queuePayload = CreatePointQueuePayload(
                localId = tempId,
                title = title,
                description = description,
                category = category.name,
                latitude = latitude,
                longitude = longitude,
                teamId = teamId
            )
            val action = QueuedAction(
                id = UUID.randomUUID().toString(),
                actionType = ActionType.CREATE_POINT,
                payloadJson = json.encodeToString(queuePayload),
                createdAt = now
            )
            offlineStorage.enqueueAction(action)
            Result.Success(localPoint)
        }
    }

    override suspend fun updatePoint(
        id: String,
        title: String,
        description: String,
        category: PointCategory,
        latitude: Double,
        longitude: Double,
        version: Int
    ): Result<PointOfInterest> = withContext(dispatcherProvider.io) {
        val now = System.currentTimeMillis()
        val existing = offlineStorage.getCachedPointById(id)
        val updatedLocal = existing?.copy(
            title = title,
            description = description,
            category = category,
            latitude = latitude,
            longitude = longitude,
            version = version + 1,
            updatedAt = now,
            isPendingSync = true
        ) ?: PointOfInterest(
            id = id,
            teamId = "",
            title = title,
            description = description,
            category = category,
            latitude = latitude,
            longitude = longitude,
            createdByUserId = "",
            createdByCallsign = "",
            updatedByUserId = "",
            updatedByCallsign = "",
            version = version + 1,
            createdAt = now,
            updatedAt = now,
            isPendingSync = true
        )

        try {
            val request = UpdatePointRequestDto(
                title = title,
                description = description,
                category = category.name,
                latitude = latitude,
                longitude = longitude,
                version = version
            )
            val response = pointApiService.updatePoint(id, request)
            if (response.isSuccessful && response.body()?.data != null) {
                val serverPoint = response.body()!!.data!!.toDomain().copy(isPendingSync = false)
                offlineStorage.saveLocalPoint(serverPoint)
                Result.Success(serverPoint)
            } else {
                mapErrorResponse(response)
            }
        } catch (e: Exception) {
            // Network failure: queue for offline sync and update locally
            offlineStorage.saveLocalPoint(updatedLocal)
            val queuePayload = UpdatePointQueuePayload(
                id = id,
                title = title,
                description = description,
                category = category.name,
                latitude = latitude,
                longitude = longitude,
                version = version
            )
            val action = QueuedAction(
                id = UUID.randomUUID().toString(),
                actionType = ActionType.UPDATE_POINT,
                payloadJson = json.encodeToString(queuePayload),
                createdAt = now
            )
            offlineStorage.enqueueAction(action)
            Result.Success(updatedLocal)
        }
    }

    override suspend fun deletePoint(id: String): Result<Unit> = withContext(dispatcherProvider.io) {
        try {
            val response = pointApiService.deletePoint(id)
            if (response.isSuccessful || response.code() == 404) {
                offlineStorage.markPointDeletedLocally(id)
                Result.Success(Unit)
            } else {
                mapErrorResponse(response)
            }
        } catch (e: Exception) {
            // Network failure: mark deleted locally and queue deletion action
            offlineStorage.markPointDeletedLocally(id)
            val queuePayload = DeletePointQueuePayload(id = id)
            val action = QueuedAction(
                id = UUID.randomUUID().toString(),
                actionType = ActionType.DELETE_POINT,
                payloadJson = json.encodeToString(queuePayload),
                createdAt = System.currentTimeMillis()
            )
            offlineStorage.enqueueAction(action)
            Result.Success(Unit)
        }
    }

    private fun mapErrorResponse(response: Response<*>): Result.Error {
        val apiError = NetworkErrorMapper.parseApiError(response.errorBody())
        val errorMessage = NetworkErrorMapper.mapHttpError(response.code(), apiError)
        val exception = when (response.code()) {
            401 -> ApiException.Unauthorized(errorMessage)
            403 -> ApiException.Forbidden(errorMessage)
            404 -> ApiException.NotFound(errorMessage)
            409 -> ApiException.Conflict(errorMessage)
            429 -> ApiException.RateLimited(errorMessage)
            in 500..599 -> ApiException.ServerError(response.code(), errorMessage)
            else -> Exception(errorMessage)

        }
        return Result.Error(exception, errorMessage)
    }

    private fun mapException(e: Exception): Result.Error {
        val errorMessage = NetworkErrorMapper.mapThrowable(e)
        return Result.Error(ApiException.NetworkUnavailable(errorMessage), errorMessage)
    }
}
