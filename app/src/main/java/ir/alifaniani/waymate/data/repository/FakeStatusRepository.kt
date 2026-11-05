package ir.alifaniani.waymate.data.repository

import ir.alifaniani.waymate.core.common.Result
import ir.alifaniani.waymate.domain.model.ConnectionStatus
import ir.alifaniani.waymate.domain.model.OperationalStatus
import ir.alifaniani.waymate.domain.model.TeamLiveStatus
import ir.alifaniani.waymate.domain.repository.StatusRepository

/**
 * In-memory test fake for [StatusRepository].
 */
class FakeStatusRepository(
    initialStatuses: List<TeamLiveStatus> = defaultFakeStatuses,
    initialOperationalStatus: OperationalStatus = OperationalStatus.NORMAL
) : StatusRepository {

    private val liveStatuses = initialStatuses.toMutableList()
    var currentOperationalStatus: OperationalStatus = initialOperationalStatus
    var shouldFail: Boolean = false
    var failureMessage: String = "خطای شبیه‌سازی‌شده در وضعیت"

    override suspend fun getLiveTeamStatuses(): Result<List<TeamLiveStatus>> {
        if (shouldFail) return Result.Error(Exception(failureMessage), failureMessage)
        return Result.Success(liveStatuses.toList())
    }

    override suspend fun updateOperationalStatus(status: OperationalStatus): Result<OperationalStatus> {
        if (shouldFail) return Result.Error(Exception(failureMessage), failureMessage)
        currentOperationalStatus = status
        return Result.Success(status)
    }

    override suspend fun getMyOperationalStatus(): OperationalStatus? = currentOperationalStatus

    fun setLiveStatuses(newStatuses: List<TeamLiveStatus>) {
        liveStatuses.clear()
        liveStatuses.addAll(newStatuses)
    }

    companion object {
        val defaultFakeStatuses: List<TeamLiveStatus> = listOf(
            TeamLiveStatus(
                teamId = "team_alpha_001",
                teamName = "گشت آلفا (واکنش سریع)",
                callsign = "Falcon-Lead",
                operationalStatus = OperationalStatus.NORMAL,
                connectionStatus = ConnectionStatus.ONLINE,
                lastSeenAt = System.currentTimeMillis() - 20_000,
                isStale = false
            ),
            TeamLiveStatus(
                teamId = "team_bravo_002",
                teamName = "گشت براوو (پشتیبانی)",
                callsign = "Bravo-Lead",
                operationalStatus = OperationalStatus.PURSUING,
                connectionStatus = ConnectionStatus.STALE,
                lastSeenAt = System.currentTimeMillis() - 120_000,
                isStale = false
            )
        )
    }
}
