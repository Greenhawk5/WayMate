package ir.alifaniani.waymate.domain.usecase

import ir.alifaniani.waymate.core.common.Result
import ir.alifaniani.waymate.domain.model.LocationSharingState
import ir.alifaniani.waymate.domain.repository.LocationRepository
import kotlinx.coroutines.flow.StateFlow

class StartLocationSharingUseCase(
    private val locationRepository: LocationRepository
) {
    suspend operator fun invoke(teamId: String? = null): Result<Unit> {
        return locationRepository.startSharing(teamId)
    }
}

class StopLocationSharingUseCase(
    private val locationRepository: LocationRepository
) {
    operator fun invoke() {
        locationRepository.stopSharing()
    }
}

class ObserveLocationSharingStateUseCase(
    private val locationRepository: LocationRepository
) {
    operator fun invoke(): StateFlow<LocationSharingState> {
        return locationRepository.sharingState
    }
}
