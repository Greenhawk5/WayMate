package ir.alifaniani.waymate.data.repository

import ir.alifaniani.waymate.core.common.DefaultDispatcherProvider
import ir.alifaniani.waymate.core.common.DispatcherProvider
import ir.alifaniani.waymate.core.common.Result
import ir.alifaniani.waymate.data.location.LocationDataSource
import ir.alifaniani.waymate.data.network.LocationWebSocketClient
import ir.alifaniani.waymate.data.network.WsConnectionState
import ir.alifaniani.waymate.data.session.SessionStorage
import ir.alifaniani.waymate.domain.model.GroupMemberLocation
import ir.alifaniani.waymate.domain.model.LocationSharingState
import ir.alifaniani.waymate.domain.model.LocationSharingStatus
import ir.alifaniani.waymate.domain.model.ServerLocationEnvelope
import ir.alifaniani.waymate.domain.model.UserLocation
import ir.alifaniani.waymate.domain.repository.LocationRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class RealLocationRepository(
    private val locationDataSource: LocationDataSource,
    private val webSocketClient: LocationWebSocketClient,
    private val sessionStorage: SessionStorage,
    private val dispatcherProvider: DispatcherProvider = DefaultDispatcherProvider()
) : LocationRepository {

    private val scope = CoroutineScope(SupervisorJob() + dispatcherProvider.io)

    private val _sharingState = MutableStateFlow(LocationSharingState())
    override val sharingState: StateFlow<LocationSharingState> = _sharingState.asStateFlow()

    private var locationJob: Job? = null
    private var wsMessagesJob: Job? = null
    private var wsConnectionJob: Job? = null

    override suspend fun startSharing(teamId: String?): Result<Unit> {
        val token = sessionStorage.getToken()
        if (token.isNullOrBlank()) {
            val msg = "کاربر احراز هویت نشده است — لطفاً مجدداً وارد شوید"
            updateStatus(LocationSharingStatus.ERROR, msg)
            return Result.Error(IllegalStateException(msg))
        }

        if (!locationDataSource.hasLocationPermission()) {
            val msg = "مجوز دسترسی به موقعیت مکانی داده نشده است"
            updateStatus(LocationSharingStatus.PERMISSION_DENIED, msg)
            return Result.Error(SecurityException(msg))
        }

        if (!locationDataSource.isGpsEnabled()) {
            val msg = "حسگر موقعیت مکانی (GPS) دستگاه خاموش است"
            updateStatus(LocationSharingStatus.GPS_DISABLED, msg)
            return Result.Error(IllegalStateException(msg))
        }

        updateStatus(LocationSharingStatus.STARTING)

        // Connect WebSocket client
        webSocketClient.connect(token, teamId)

        // Listen for connection states
        wsConnectionJob?.cancel()
        wsConnectionJob = scope.launch(dispatcherProvider.main) {
            webSocketClient.connectionState.collect { state ->
                when (state) {
                    WsConnectionState.CONNECTED -> {
                        _sharingState.update { it.copy(status = LocationSharingStatus.SHARING, errorMessage = null) }
                    }
                    WsConnectionState.RECONNECTING -> {
                        if (_sharingState.value.isSharingActive) {
                            _sharingState.update { it.copy(status = LocationSharingStatus.RECONNECTING) }
                        }
                    }
                    WsConnectionState.DISCONNECTED -> {
                        if (_sharingState.value.isSharingActive) {
                            _sharingState.update { it.copy(status = LocationSharingStatus.NETWORK_UNAVAILABLE) }
                        }
                    }
                    WsConnectionState.CONNECTING -> {
                        if (_sharingState.value.status != LocationSharingStatus.SHARING) {
                            _sharingState.update { it.copy(status = LocationSharingStatus.STARTING) }
                        }
                    }
                }
            }
        }

        // Listen for incoming server envelopes
        wsMessagesJob?.cancel()
        wsMessagesJob = scope.launch(dispatcherProvider.main) {
            webSocketClient.incomingMessages.collect { envelope ->
                handleIncomingEnvelope(envelope)
            }
        }

        // Start listening to local GPS fixes
        locationJob?.cancel()
        locationJob = scope.launch(dispatcherProvider.io) {
            locationDataSource.getLocationUpdates()
                .catch { e ->
                    if (e is SecurityException) {
                        updateStatus(LocationSharingStatus.PERMISSION_DENIED, "مجوز دسترسی به موقعیت مکانی لغو شد")
                    } else {
                        updateStatus(LocationSharingStatus.GPS_DISABLED, "حسگر موقعیت مکانی غیرفعال شد")
                    }
                }
                .collect { location ->
                    publishLocation(location)
                }
        }

        return Result.Success(Unit)
    }

    override fun stopSharing() {
        locationJob?.cancel()
        locationJob = null

        wsMessagesJob?.cancel()
        wsMessagesJob = null

        wsConnectionJob?.cancel()
        wsConnectionJob = null

        webSocketClient.sendSharingStopped()
        webSocketClient.disconnect()

        _sharingState.update {
            it.copy(
                status = LocationSharingStatus.STOPPED,
                errorMessage = null
            )
        }
    }

    override fun publishLocation(location: UserLocation) {
        if (!location.isValid) return

        _sharingState.update {
            it.copy(
                myLocation = location,
                lastPublishedTimestamp = location.timestamp
            )
        }

        if (_sharingState.value.isSharingActive) {
            webSocketClient.sendLocationUpdate(location)
        }
    }

    override fun updateStatus(status: LocationSharingStatus, errorMessage: String?) {
        _sharingState.update {
            it.copy(
                status = status,
                errorMessage = errorMessage
            )
        }
    }

    private fun handleIncomingEnvelope(envelope: ServerLocationEnvelope) {
        when (envelope.type) {
            "INITIAL_ROSTER" -> {
                val sharers = envelope.activeSharers?.map { dto ->
                    GroupMemberLocation(
                        userId = dto.userId,
                        callsign = dto.callsign,
                        teamId = envelope.teamId ?: "",
                        latitude = dto.latitude,
                        longitude = dto.longitude,
                        accuracyMeters = dto.accuracy,
                        altitudeMeters = dto.altitude,
                        speedMps = dto.speed,
                        bearingDegrees = dto.bearing,
                        timestamp = dto.timestamp,
                        isSharing = true
                    )
                } ?: emptyList()

                _sharingState.update { it.copy(activeSharers = sharers) }
            }

            "LOCATION_UPDATE" -> {
                val userId = envelope.userId ?: return
                val newMemberLocation = GroupMemberLocation(
                    userId = userId,
                    callsign = envelope.callsign ?: "عضو تیم",
                    teamId = envelope.teamId ?: "",
                    latitude = envelope.latitude ?: 0.0,
                    longitude = envelope.longitude ?: 0.0,
                    accuracyMeters = envelope.accuracy ?: 0f,
                    altitudeMeters = envelope.altitude,
                    speedMps = envelope.speed,
                    bearingDegrees = envelope.bearing,
                    timestamp = envelope.timestamp ?: System.currentTimeMillis(),
                    isSharing = true
                )

                _sharingState.update { state ->
                    val existingIndex = state.activeSharers.indexOfFirst { it.userId == userId }
                    val updatedList = if (existingIndex >= 0) {
                        state.activeSharers.toMutableList().apply { set(existingIndex, newMemberLocation) }
                    } else {
                        state.activeSharers + newMemberLocation
                    }
                    state.copy(activeSharers = updatedList)
                }
            }

            "SHARING_STOPPED" -> {
                val userId = envelope.userId ?: return
                // Remove or invalidate member from active sharers list per privacy rules
                _sharingState.update { state ->
                    state.copy(activeSharers = state.activeSharers.filterNot { it.userId == userId })
                }
            }

            "MEMBER_STATE" -> {
                val userId = envelope.userId ?: return
                if (envelope.status == "DISCONNECTED" || envelope.isSharing == false) {
                    _sharingState.update { state ->
                        state.copy(activeSharers = state.activeSharers.filterNot { it.userId == userId })
                    }
                }
            }

            "ERROR" -> {
                val errorText = envelope.message ?: "خطا در ارتباط با سرور موقعیت"
                _sharingState.update { it.copy(errorMessage = errorText) }
            }
        }
    }
}
