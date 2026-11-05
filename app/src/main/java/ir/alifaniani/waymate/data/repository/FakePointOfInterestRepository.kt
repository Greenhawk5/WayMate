package ir.alifaniani.waymate.data.repository

import ir.alifaniani.waymate.core.common.Result
import ir.alifaniani.waymate.domain.model.PointCategory
import ir.alifaniani.waymate.domain.model.PointOfInterest
import ir.alifaniani.waymate.domain.repository.PointOfInterestRepository

/**
 * In-memory test fake for [PointOfInterestRepository].
 */
class FakePointOfInterestRepository(
    initialPoints: List<PointOfInterest> = defaultDemoPoints
) : PointOfInterestRepository {

    private val points = initialPoints.toMutableList()
    var shouldFail: Boolean = false
    var failureMessage: String = "خطای شبیه‌سازی‌شده در نقاط"

    override suspend fun getPoints(teamId: String?, forceRefresh: Boolean): Result<List<PointOfInterest>> {
        if (shouldFail) return Result.Error(Exception(failureMessage), failureMessage)
        val filtered = points.filter { !it.isDeleted && (teamId == null || it.teamId == teamId) }
        return Result.Success(filtered.sortedByDescending { it.updatedAt })
    }

    override suspend fun getPointById(id: String): Result<PointOfInterest> {
        if (shouldFail) return Result.Error(Exception(failureMessage), failureMessage)
        val point = points.find { it.id == id && !it.isDeleted }
        return if (point != null) Result.Success(point) else Result.Error(Exception("نقطه یافت نشد"), "نقطه یافت نشد")
    }

    override suspend fun createPoint(
        title: String,
        description: String,
        category: PointCategory,
        latitude: Double,
        longitude: Double,
        teamId: String?
    ): Result<PointOfInterest> {
        if (shouldFail) return Result.Error(Exception(failureMessage), failureMessage)
        val now = System.currentTimeMillis()
        val poi = PointOfInterest(
            id = "poi_fake_${System.currentTimeMillis()}",
            teamId = teamId ?: "team_alpha_001",
            title = title,
            description = description,
            category = category,
            latitude = latitude,
            longitude = longitude,
            createdByUserId = "usr_lead_001",
            createdByCallsign = "Falcon-Lead",
            updatedByUserId = "usr_lead_001",
            updatedByCallsign = "Falcon-Lead",
            version = 1,
            createdAt = now,
            updatedAt = now
        )
        points.add(0, poi)
        return Result.Success(poi)
    }

    override suspend fun updatePoint(
        id: String,
        title: String,
        description: String,
        category: PointCategory,
        latitude: Double,
        longitude: Double,
        version: Int
    ): Result<PointOfInterest> {
        if (shouldFail) return Result.Error(Exception(failureMessage), failureMessage)
        val index = points.indexOfFirst { it.id == id && !it.isDeleted }
        if (index == -1) return Result.Error(Exception("نقطه یافت نشد"), "نقطه یافت نشد")

        val current = points[index]
        if (current.version != version) {
            return Result.Error(Exception("تعارض در نسخه"), "تعارض در نسخه")
        }

        val updated = current.copy(
            title = title,
            description = description,
            category = category,
            latitude = latitude,
            longitude = longitude,
            version = version + 1,
            updatedAt = System.currentTimeMillis()
        )
        points[index] = updated
        return Result.Success(updated)
    }

    override suspend fun deletePoint(id: String): Result<Unit> {
        if (shouldFail) return Result.Error(Exception(failureMessage), failureMessage)
        val index = points.indexOfFirst { it.id == id }
        if (index != -1) {
            points[index] = points[index].copy(isDeleted = true)
            return Result.Success(Unit)
        }
        return Result.Error(Exception("نقطه یافت نشد"), "نقطه یافت نشد")
    }

    fun setPoints(newPoints: List<PointOfInterest>) {
        points.clear()
        points.addAll(newPoints)
    }

    companion object {
        val defaultDemoPoints = listOf(
            PointOfInterest(
                id = "poi_demo_001",
                teamId = "team_alpha_001",
                title = "نقطه شروع پیمایش غار سنگ‌شکنان",
                description = "محل تجمع اولیه گروه، پارکینگ خودروها و بررسی تجهیزات",
                category = PointCategory.START_POINT,
                latitude = 28.4870,
                longitude = 53.5540,
                createdByUserId = "usr_lead_001",
                createdByCallsign = "Falcon-Lead",
                updatedByUserId = "usr_lead_001",
                updatedByCallsign = "Falcon-Lead",
                version = 1,
                createdAt = 1729000000000,
                updatedAt = 1729000000000
            ),
            PointOfInterest(
                id = "poi_demo_002",
                teamId = "team_alpha_001",
                title = "چشمه و استراحت‌گاه کوهستان قلات",
                description = "منبع آب آشامیدنی طبیعی و سایه‌بان مناسب برای استراحت میانی",
                category = PointCategory.WATER_SOURCE,
                latitude = 28.5120,
                longitude = 53.5830,
                createdByUserId = "usr_lead_001",
                createdByCallsign = "Falcon-Lead",
                updatedByUserId = "usr_lead_001",
                updatedByCallsign = "Falcon-Lead",
                version = 1,
                createdAt = 1729000000000,
                updatedAt = 1729000000000
            ),
            PointOfInterest(
                id = "poi_demo_003",
                teamId = "team_alpha_001",
                title = "محل کمپ شبانه چاتیز",
                description = "فضای مسطح با دسترسی به مسیر پاکوب مناسب برای برپایی چادرهای اقامتی",
                category = PointCategory.CAMPSITE,
                latitude = 28.5340,
                longitude = 53.6120,
                createdByUserId = "usr_cmd_001",
                createdByCallsign = "Eagle-1",
                updatedByUserId = "usr_cmd_001",
                updatedByCallsign = "Eagle-1",
                version = 1,
                createdAt = 1729000000000,
                updatedAt = 1729000000000
            )
        )
    }
}
