package ir.alifaniani.waymate.domain.repository

import ir.alifaniani.waymate.core.common.Result
import ir.alifaniani.waymate.domain.model.PointCategory
import ir.alifaniani.waymate.domain.model.PointOfInterest

/**
 * Domain repository contract for managing shared group Points of Interest.
 */
interface PointOfInterestRepository {

    /**
     * Retrieves all active points of interest for the user's active team/group.
     */
    suspend fun getPoints(teamId: String? = null, forceRefresh: Boolean = false): Result<List<PointOfInterest>>

    /**
     * Retrieves a single point of interest by its identifier.
     */
    suspend fun getPointById(id: String): Result<PointOfInterest>

    /**
     * Creates a new point of interest. Supports optimistic local caching and offline queuing.
     */
    suspend fun createPoint(
        title: String,
        description: String,
        category: PointCategory,
        latitude: Double,
        longitude: Double,
        teamId: String? = null
    ): Result<PointOfInterest>

    /**
     * Updates an existing point of interest with optimistic concurrency.
     */
    suspend fun updatePoint(
        id: String,
        title: String,
        description: String,
        category: PointCategory,
        latitude: Double,
        longitude: Double,
        version: Int
    ): Result<PointOfInterest>

    /**
     * Deletes a point of interest (soft-delete).
     */
    suspend fun deletePoint(id: String): Result<Unit>
}
