package ir.alifaniani.waymate.data.repository

import ir.alifaniani.waymate.core.common.Result
import ir.alifaniani.waymate.domain.model.Team
import ir.alifaniani.waymate.domain.model.TeamMember
import ir.alifaniani.waymate.domain.repository.TeamRepository
import kotlinx.coroutines.delay

/**
 * DEVELOPMENT AND TESTING ONLY.
 *
 * In-memory fake implementation of [TeamRepository] containing synthetic tactical records.
 */
class FakeTeamRepository(
    private val simulatedDelayMs: Long = 0L
) : TeamRepository {

    var shouldFail: Boolean = false
    var failureError: Throwable = Exception("خطای شبیه‌سازی‌شده در ارتباط با سرور")
    var failureMessage: String = "خطای شبیه‌سازی‌شده در ارتباط با سرور"

    val syntheticTeams: MutableList<Team> = mutableListOf(
        Team(
            id = "team_alpha_001",
            name = "گشت آلفا (واکنش سریع)",
            leadUserId = "usr_lead_001",
            vehicleType = "تویوتا هایلوکس",
            vehiclePlate = "۴۴-ب-۱۲",
            equipmentSummary = "بی‌سیم فرکانس اختصاصی، جعبه امداد تاکتیکی",
            members = listOf(
                TeamMember(
                    id = "mem_001",
                    fullName = "رضا کریمی",
                    callsign = "البرز ۱",
                    roleInTeam = "راننده و اپراتور فنی"
                ),
                TeamMember(
                    id = "mem_002",
                    fullName = "محمد حسینی",
                    callsign = "سهند ۲",
                    roleInTeam = "دیده‌بان و سرتیم"
                )
            ),
            memberCount = 2,
            isActive = true
        )
    )

    override suspend fun getTeams(fetchDetails: Boolean): Result<List<Team>> {
        if (simulatedDelayMs > 0) delay(simulatedDelayMs)
        if (shouldFail) {
            return Result.Error(failureError, failureMessage)
        }
        return Result.Success(syntheticTeams.toList())
    }

    override suspend fun getTeamDetails(teamId: String): Result<Team> {
        if (simulatedDelayMs > 0) delay(simulatedDelayMs)
        if (shouldFail) {
            return Result.Error(failureError, failureMessage)
        }
        val team = syntheticTeams.find { it.id == teamId }
        return if (team != null) {
            Result.Success(team)
        } else {
            Result.Error(Exception("تیم عملیاتی یافت نشد"), "تیم عملیاتی یافت نشد")
        }
    }

    override suspend fun createTeam(
        name: String,
        leadUserId: String,
        vehicleType: String,
        vehiclePlate: String,
        equipmentSummary: String,
        members: List<TeamMember>
    ): Result<Team> {
        if (simulatedDelayMs > 0) delay(simulatedDelayMs)
        if (shouldFail) {
            return Result.Error(failureError, failureMessage)
        }
        val newTeam = Team(
            id = "team_${System.currentTimeMillis()}",
            name = name,
            leadUserId = leadUserId,
            vehicleType = vehicleType,
            vehiclePlate = vehiclePlate,
            equipmentSummary = equipmentSummary,
            members = members,
            memberCount = members.size,
            isActive = true
        )
        syntheticTeams.add(newTeam)
        return Result.Success(newTeam)
    }

    override suspend fun updateTeam(
        teamId: String,
        name: String?,
        leadUserId: String?,
        vehicleType: String?,
        vehiclePlate: String?,
        equipmentSummary: String?,
        isActive: Boolean?
    ): Result<Team> {
        if (simulatedDelayMs > 0) delay(simulatedDelayMs)
        if (shouldFail) {
            return Result.Error(failureError, failureMessage)
        }
        val index = syntheticTeams.indexOfFirst { it.id == teamId }
        if (index == -1) {
            return Result.Error(Exception("گروه عملیاتی یافت نشد"), "گروه عملیاتی یافت نشد")
        }
        val current = syntheticTeams[index]
        val updated = current.copy(
            name = name ?: current.name,
            leadUserId = leadUserId ?: current.leadUserId,
            vehicleType = vehicleType ?: current.vehicleType,
            vehiclePlate = vehiclePlate ?: current.vehiclePlate,
            equipmentSummary = equipmentSummary ?: current.equipmentSummary,
            isActive = isActive ?: current.isActive
        )
        syntheticTeams[index] = updated
        return Result.Success(updated)
    }
}
