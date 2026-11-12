package ir.alifaniani.waymate.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import ir.alifaniani.waymate.core.common.DefaultDispatcherProvider
import ir.alifaniani.waymate.core.common.DispatcherProvider
import ir.alifaniani.waymate.core.common.Result
import ir.alifaniani.waymate.core.network.ApiException
import ir.alifaniani.waymate.core.network.SafePoller
import ir.alifaniani.waymate.data.sync.OfflineSyncManager
import ir.alifaniani.waymate.domain.model.ConnectionStatus
import ir.alifaniani.waymate.domain.model.GroupMemberLocation
import ir.alifaniani.waymate.domain.model.ManagedUser
import ir.alifaniani.waymate.domain.model.OperationalStatus
import ir.alifaniani.waymate.domain.model.PointCategory
import ir.alifaniani.waymate.domain.model.PointOfInterest
import ir.alifaniani.waymate.domain.model.TacticalMessage
import ir.alifaniani.waymate.domain.model.Team
import ir.alifaniani.waymate.domain.model.TeamLiveStatus
import ir.alifaniani.waymate.domain.model.TeamMember
import ir.alifaniani.waymate.domain.model.UserRole
import ir.alifaniani.waymate.domain.repository.AdminRepository
import ir.alifaniani.waymate.domain.repository.AuthRepository
import ir.alifaniani.waymate.domain.repository.MessageRepository
import ir.alifaniani.waymate.domain.repository.PointOfInterestRepository
import ir.alifaniani.waymate.domain.repository.StatusRepository
import ir.alifaniani.waymate.domain.repository.TeamRepository
import ir.alifaniani.waymate.domain.usecase.ObserveLocationSharingStateUseCase
import ir.alifaniani.waymate.domain.usecase.StopLocationSharingUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel responsible for managing tactical team roster, live link health,
 * operational status selection, real-time tactical messaging with safe polling,
 * Phase 9 roles and management operations,
 * Phase 10 local offline storage and synchronization,
 * Phase 11 shared points of interest,
 * and real-time consent-based location sharing.
 */
class HomeViewModel(
    private val teamRepository: TeamRepository,
    private val messageRepository: MessageRepository? = null,
    private val statusRepository: StatusRepository? = null,
    private val authRepository: AuthRepository? = null,
    private val adminRepository: AdminRepository? = null,
    private val offlineSyncManager: OfflineSyncManager? = null,
    private val pointRepository: PointOfInterestRepository? = null,
    private val observeLocationSharingStateUseCase: ObserveLocationSharingStateUseCase? = null,
    private val stopLocationSharingUseCase: StopLocationSharingUseCase? = null,
    private val dispatcherProvider: DispatcherProvider = DefaultDispatcherProvider(),
    private val poller: SafePoller = SafePoller()
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var statusUpdateJob: Job? = null
    private var committedOperationalStatus: OperationalStatus = OperationalStatus.NORMAL

    init {
        loadMyOperationalStatus()
        loadTeams()
        loadMessages()
        loadLiveStatuses()
        loadPoints()
        refreshOfflineQueueCount()
        startPolling()
        observeLocationSharingState()
    }

    private fun loadMyOperationalStatus() {
        val repo = statusRepository ?: return
        viewModelScope.launch(dispatcherProvider.main) {
            repo.getMyOperationalStatus()?.let { savedStatus ->
                committedOperationalStatus = savedStatus
                _uiState.update { state ->
                    state.copy(
                        myOperationalStatus = savedStatus,
                        liveStatuses = syncLiveStatusesWith(state.liveStatuses, savedStatus)
                    )
                }
            }
        }
    }

    private fun syncLiveStatusesWith(
        statuses: List<TeamLiveStatus>,
        myStatus: OperationalStatus
    ): List<TeamLiveStatus> {
        if (statuses.isEmpty()) return statuses
        return statuses.mapIndexed { index, unit ->
            if (index == 0 || unit.teamId == "team_alpha_001") {
                unit.copy(operationalStatus = myStatus)
            } else {
                unit
            }
        }
    }

    private fun observeLocationSharingState() {
        observeLocationSharingStateUseCase?.let { useCase ->
            viewModelScope.launch(dispatcherProvider.main) {
                useCase().collect { sharingState ->
                    _uiState.update { it.copy(locationSharingState = sharingState) }
                }
            }
        }
    }

    fun loadTeams(force: Boolean = false) {
        if (!force && _uiState.value.isLoading) return

        _uiState.update {
            it.copy(
                isLoading = true,
                errorMessage = null,
                isForbidden = false,
                isSessionExpired = false
            )
        }

        viewModelScope.launch(dispatcherProvider.main) {
            when (val result = teamRepository.getTeams()) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            teams = result.data,
                            errorMessage = null,
                            connectionStatus = ConnectionStatus.ONLINE,
                            lastSyncTimestamp = System.currentTimeMillis()
                        )
                    }
                }
                is Result.Error -> {
                    handleError(result)
                }
                else -> {}
            }
        }
    }

    fun loadMessages() {
        val repo = messageRepository ?: return
        viewModelScope.launch(dispatcherProvider.main) {
            when (val result = repo.getMessages()) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            messages = result.data,
                            connectionStatus = ConnectionStatus.ONLINE,
                            lastSyncTimestamp = System.currentTimeMillis()
                        )
                    }
                }
                is Result.Error -> {
                    // Do not overwrite main error if teams loaded, but record link degradation
                    _uiState.update { it.copy(connectionStatus = ConnectionStatus.OFFLINE) }
                }
                else -> {}
            }
        }
    }

    fun loadLiveStatuses() {
        val repo = statusRepository ?: return
        viewModelScope.launch(dispatcherProvider.main) {
            when (val result = repo.getLiveTeamStatuses()) {
                is Result.Success -> {
                    _uiState.update { state ->
                        state.copy(
                            liveStatuses = syncLiveStatusesWith(result.data, state.myOperationalStatus),
                            lastSyncTimestamp = System.currentTimeMillis()
                        )
                    }
                }
                is Result.Error -> {
                    // Handled gracefully in background
                }
                else -> {}
            }
        }
    }

    fun updateOperationalStatus(newStatus: OperationalStatus) {
        val repo = statusRepository ?: return
        statusUpdateJob?.cancel()
        _uiState.update { state ->
            state.copy(
                myOperationalStatus = newStatus,
                liveStatuses = syncLiveStatusesWith(state.liveStatuses, newStatus),
                errorMessage = null
            )
        }

        statusUpdateJob = viewModelScope.launch(dispatcherProvider.main) {
            when (val result = repo.updateOperationalStatus(newStatus)) {
                is Result.Success -> {
                    committedOperationalStatus = result.data
                    _uiState.update { state ->
                        state.copy(
                            myOperationalStatus = result.data,
                            liveStatuses = syncLiveStatusesWith(state.liveStatuses, result.data),
                            connectionStatus = ConnectionStatus.ONLINE
                        )
                    }
                    refreshOfflineQueueCount()
                }
                is Result.Error -> {
                    _uiState.update { state ->
                        state.copy(
                            myOperationalStatus = committedOperationalStatus,
                            liveStatuses = syncLiveStatusesWith(state.liveStatuses, committedOperationalStatus)
                        )
                    }
                    handleError(result)
                    refreshOfflineQueueCount()
                }
                else -> {}
            }
        }
    }

    fun acknowledgeMessage(messageId: String) {
        val repo = messageRepository ?: return
        viewModelScope.launch(dispatcherProvider.main) {
            when (repo.acknowledgeMessage(messageId)) {
                is Result.Success -> {
                    // Locally update message status to acknowledged
                    _uiState.update { state ->
                        state.copy(
                            messages = state.messages.map { msg ->
                                if (msg.id == messageId) msg.copy(isAcknowledged = true) else msg
                            }
                        )
                    }
                    refreshOfflineQueueCount()
                }
                is Result.Error -> {
                    refreshOfflineQueueCount()
                }
                else -> {}
            }
        }
    }

    fun sendTacticalMessage(body: String, isUrgent: Boolean, targetTeamId: String?) {
        val repo = messageRepository ?: return
        if (_uiState.value.isSendingMessage) return
        _uiState.update { it.copy(isSendingMessage = true) }
        viewModelScope.launch(dispatcherProvider.main) {
            try {
                when (val result = repo.sendMessage(body, isUrgent, targetTeamId)) {
                    is Result.Success -> {
                        _uiState.update { state ->
                            state.copy(
                                isSendingMessage = false,
                                messages = listOf(result.data) + state.messages
                            )
                        }
                        refreshOfflineQueueCount()
                    }
                    is Result.Error -> {
                        _uiState.update { it.copy(isSendingMessage = false) }
                        handleError(result)
                        refreshOfflineQueueCount()
                    }
                    else -> {
                        _uiState.update { it.copy(isSendingMessage = false) }
                    }
                }
            } catch (_: Exception) {
                _uiState.update { it.copy(isSendingMessage = false) }
            }
        }
    }

    // ==========================================
    // Phase 10 Offline Queue & Synchronization
    // ==========================================

    fun refreshOfflineQueueCount() {
        val manager = offlineSyncManager ?: return
        viewModelScope.launch(dispatcherProvider.main) {
            manager.refreshPendingCount()
            _uiState.update { it.copy(offlineQueueCount = manager.pendingQueueCount.value) }
        }
    }

    fun syncOfflineQueue() {
        val manager = offlineSyncManager ?: return
        if (_uiState.value.isSyncing) return

        _uiState.update { it.copy(isSyncing = true, syncFeedbackMessage = null) }

        viewModelScope.launch(dispatcherProvider.main) {
            val result = manager.drainQueue()
            val feedback = if (result.syncedCount > 0) {
                "${result.syncedCount} عملیات با موفقیت به سرور ارسال شد"
            } else if (result.failedCount > 0) {
                "خطا در ارسال برخی داده‌های صف آفلاین"
            } else null

            _uiState.update {
                it.copy(
                    isSyncing = false,
                    offlineQueueCount = result.remainingCount,
                    syncFeedbackMessage = feedback
                )
            }

            if (result.syncedCount > 0) {
                loadTeams(force = true)
                loadMessages()
                loadLiveStatuses()
            }
        }
    }

    fun dismissSyncFeedback() {
        _uiState.update { it.copy(syncFeedbackMessage = null) }
    }

    // ==========================================
    // Phase 9 Management & Roles Operations
    // ==========================================

    fun loadManagementData(userRole: UserRole) {
        if (userRole == UserRole.TEAM_LEAD) return

        _uiState.update { it.copy(isManagementLoading = true) }

        viewModelScope.launch(dispatcherProvider.main) {
            if (userRole == UserRole.OWNER) {
                adminRepository?.let { repo ->
                    when (val usersResult = repo.getUsers()) {
                        is Result.Success -> _uiState.update { it.copy(managedUsers = usersResult.data) }
                        is Result.Error -> {}
                        else -> {}
                    }
                    when (val logsResult = repo.getAuditLogs()) {
                        is Result.Success -> _uiState.update { it.copy(auditLogs = logsResult.data) }
                        is Result.Error -> {}
                        else -> {}
                    }
                }
            }

            // Stats available for OWNER and COMMANDER
            adminRepository?.let { repo ->
                when (val statsResult = repo.getSystemStats()) {
                    is Result.Success -> _uiState.update { it.copy(systemStats = statsResult.data) }
                    is Result.Error -> {}
                    else -> {}
                }
            }

            _uiState.update { it.copy(isManagementLoading = false) }
        }
    }

    fun createUser(username: String, password: String, role: UserRole, callsign: String) {
        val repo = adminRepository ?: return
        _uiState.update { it.copy(isManagementLoading = true, managementFeedbackMessage = null) }

        viewModelScope.launch(dispatcherProvider.main) {
            when (val result = repo.createUser(username, password, role, callsign)) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            isManagementLoading = false,
                            managementFeedbackMessage = "کاربر '${result.data.callsign}' با موفقیت ایجاد شد",
                            isManagementFeedbackError = false
                        )
                    }
                    loadManagementData(UserRole.OWNER)
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isManagementLoading = false,
                            managementFeedbackMessage = result.message ?: "خطا در ایجاد کاربر",
                            isManagementFeedbackError = true
                        )
                    }
                }
                else -> {}
            }
        }
    }

    fun toggleUserStatus(user: ManagedUser) {
        val repo = adminRepository ?: return
        _uiState.update { it.copy(isManagementLoading = true) }

        viewModelScope.launch(dispatcherProvider.main) {
            val newStatus = !user.isActive
            when (val result = repo.updateUser(user.id, isActive = newStatus)) {
                is Result.Success -> {
                    val statusText = if (newStatus) "فعال" else "غیرفعال"
                    _uiState.update {
                        it.copy(
                            isManagementLoading = false,
                            managementFeedbackMessage = "وضعیت کاربر '${user.callsign}' به $statusText تغییر یافت",
                            isManagementFeedbackError = false
                        )
                    }
                    loadManagementData(UserRole.OWNER)
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isManagementLoading = false,
                            managementFeedbackMessage = result.message ?: "خطا در تغییر وضعیت کاربر",
                            isManagementFeedbackError = true
                        )
                    }
                }
                else -> {}
            }
        }
    }

    fun createTeam(
        name: String,
        leadUserId: String,
        vehicleType: String,
        vehiclePlate: String,
        equipmentSummary: String,
        members: List<TeamMember>,
        userRole: UserRole
    ) {
        _uiState.update { it.copy(isManagementLoading = true, managementFeedbackMessage = null) }

        viewModelScope.launch(dispatcherProvider.main) {
            when (val result = teamRepository.createTeam(name, leadUserId, vehicleType, vehiclePlate, equipmentSummary, members)) {
                is Result.Success -> {
                    _uiState.update { state ->
                        val updatedTeams = if (state.teams.any { it.id == result.data.id }) {
                            state.teams.map { if (it.id == result.data.id) result.data else it }
                        } else {
                            state.teams + result.data
                        }
                        state.copy(
                            teams = updatedTeams,
                            isManagementLoading = false,
                            managementFeedbackMessage = "گروه عملیاتی '${result.data.name}' با موفقیت ایجاد شد",
                            isManagementFeedbackError = false
                        )
                    }
                    refreshOfflineQueueCount()
                    loadTeams(force = true)
                    loadManagementData(userRole)
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isManagementLoading = false,
                            managementFeedbackMessage = result.message ?: "خطا در ایجاد گروه عملیاتی",
                            isManagementFeedbackError = true
                        )
                    }
                    refreshOfflineQueueCount()
                }
                else -> {}
            }
        }
    }

    fun toggleTeamStatus(team: Team, userRole: UserRole) {
        _uiState.update { it.copy(isManagementLoading = true, managementFeedbackMessage = null) }

        viewModelScope.launch(dispatcherProvider.main) {
            val newStatus = !team.isActive
            when (val result = teamRepository.updateTeam(team.id, isActive = newStatus)) {
                is Result.Success -> {
                    val statusText = if (newStatus) "فعال" else "غیرفعال"
                    _uiState.update { state ->
                        state.copy(
                            teams = state.teams.map { if (it.id == team.id) result.data else it },
                            isManagementLoading = false,
                            managementFeedbackMessage = "وضعیت گروه '${team.name}' به $statusText تغییر یافت",
                            isManagementFeedbackError = false
                        )
                    }
                    loadTeams(force = true)
                    loadManagementData(userRole)
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isManagementLoading = false,
                            managementFeedbackMessage = result.message ?: "خطا در تغییر وضعیت گروه",
                            isManagementFeedbackError = true
                        )
                    }
                }
                else -> {}
            }
        }
    }

    fun addTeamMember(teamId: String, fullName: String, callsign: String, roleInTeam: String) {
        _uiState.update { it.copy(isManagementLoading = true, managementFeedbackMessage = null) }
        viewModelScope.launch(dispatcherProvider.main) {
            when (val result = teamRepository.addMember(teamId, fullName, callsign, roleInTeam)) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            isManagementLoading = false,
                            managementFeedbackMessage = "عضو '${result.data.callsign}' با موفقیت به گروه اضافه شد",
                            isManagementFeedbackError = false
                        )
                    }
                    loadTeams(force = true)
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isManagementLoading = false,
                            managementFeedbackMessage = result.message ?: "خطا در افزودن عضو به گروه",
                            isManagementFeedbackError = true
                        )
                    }
                }
                else -> {}
            }
        }
    }

    fun removeTeamMember(teamId: String, memberId: String) {
        _uiState.update { it.copy(isManagementLoading = true, managementFeedbackMessage = null) }
        viewModelScope.launch(dispatcherProvider.main) {
            when (val result = teamRepository.removeMember(teamId, memberId)) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            isManagementLoading = false,
                            managementFeedbackMessage = "عضو با موفقیت از گروه حذف شد",
                            isManagementFeedbackError = false
                        )
                    }
                    loadTeams(force = true)
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isManagementLoading = false,
                            managementFeedbackMessage = result.message ?: "خطا در حذف عضو از گروه",
                            isManagementFeedbackError = true
                        )
                    }
                }
                else -> {}
            }
        }
    }

    fun dismissManagementFeedback() {
        _uiState.update { it.copy(managementFeedbackMessage = null) }
    }

    // ==========================================
    // Polling Lifecycle
    // ==========================================

    private fun startPolling() {
        _uiState.update { it.copy(isPollingActive = true) }
        poller.start(viewModelScope) {
            if (_uiState.value.offlineQueueCount > 0) {
                val manager = offlineSyncManager
                if (manager != null && !_uiState.value.isSyncing) {
                    val res = manager.drainQueue()
                    if (res.syncedCount > 0) {
                        _uiState.update { it.copy(offlineQueueCount = res.remainingCount) }
                        loadTeams(force = true)
                    }
                }
            }
            loadMessages()
            loadLiveStatuses()
            true
        }
    }

    fun pausePolling() {
        _uiState.update { it.copy(isPollingActive = false) }
        poller.pause()
    }

    fun resumePolling() {
        _uiState.update { it.copy(isPollingActive = true) }
        poller.resume()
    }

    fun stopPolling() {
        _uiState.update { it.copy(isPollingActive = false) }
        poller.stop()
    }

    private fun handleError(result: Result.Error) {
        val throwable = result.throwable
        val is401 = throwable is ApiException.Unauthorized ||
                result.message?.contains("نشست") == true ||
                result.message?.contains("منقضی") == true
        val is403 = throwable is ApiException.Forbidden ||
                result.message?.contains("مجوز") == true

        if (is401) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    errorMessage = result.message ?: "نشست کاربری شما منقضی شده است.",
                    isSessionExpired = true,
                    connectionStatus = ConnectionStatus.OFFLINE
                )
            }
            authRepository?.let { repo ->
                viewModelScope.launch(dispatcherProvider.io) {
                    repo.logout()
                }
            }
            stopPolling()
        } else if (is403) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    errorMessage = result.message ?: "دسترسی به این بخش برای نقش شما مجاز نمی‌باشد.",
                    isForbidden = true
                )
            }
        } else {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    errorMessage = result.message ?: "خطا در ارتباط با سرور.",
                    connectionStatus = ConnectionStatus.OFFLINE
                )
            }
        }
    }

    fun loadPoints(forceRefresh: Boolean = false) {
        val repo = pointRepository ?: return
        viewModelScope.launch(dispatcherProvider.main) {
            when (val result = repo.getPoints(forceRefresh = forceRefresh)) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            points = result.data,
                            lastSyncTimestamp = System.currentTimeMillis()
                        )
                    }
                }
                is Result.Error -> {
                    // Fail silently for points on background load without disrupting primary status
                }
                else -> {}
            }
        }
    }

    fun selectPoint(point: PointOfInterest?) {
        _uiState.update { it.copy(selectedPoint = point) }
    }

    fun openCreatePointDialog() {
        _uiState.update { it.copy(isPointDialogVisible = true, editingPoint = null) }
    }

    fun openEditPointDialog(point: PointOfInterest) {
        _uiState.update { it.copy(isPointDialogVisible = true, editingPoint = point) }
    }

    fun dismissPointDialog() {
        _uiState.update { it.copy(isPointDialogVisible = false, editingPoint = null) }
    }

    fun createPoint(
        title: String,
        description: String,
        category: PointCategory,
        latitude: Double,
        longitude: Double,
        teamId: String? = null
    ) {
        val repo = pointRepository ?: return
        viewModelScope.launch(dispatcherProvider.main) {
            when (val result = repo.createPoint(title, description, category, latitude, longitude, teamId)) {
                is Result.Success -> {
                    loadPoints()
                    refreshOfflineQueueCount()
                    _uiState.update {
                        it.copy(
                            selectedPoint = result.data,
                            pointFeedbackMessage = "نقطه «$title» با موفقیت ثبت شد."
                        )
                    }
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(pointFeedbackMessage = "خطا در ثبت نقطه: ${result.message}")
                    }
                }
                else -> {}
            }
        }
    }

    fun updatePoint(
        id: String,
        title: String,
        description: String,
        category: PointCategory,
        latitude: Double,
        longitude: Double,
        version: Int
    ) {
        val repo = pointRepository ?: return
        viewModelScope.launch(dispatcherProvider.main) {
            when (val result = repo.updatePoint(id, title, description, category, latitude, longitude, version)) {
                is Result.Success -> {
                    loadPoints()
                    refreshOfflineQueueCount()
                    _uiState.update {
                        it.copy(
                            selectedPoint = result.data,
                            pointFeedbackMessage = "نقطه «$title» با موفقیت به‌روزرسانی شد."
                        )
                    }
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(pointFeedbackMessage = "خطا در ویرایش نقطه: ${result.message}")
                    }
                }
                else -> {}
            }
        }
    }

    fun deletePoint(id: String) {
        val repo = pointRepository ?: return
        viewModelScope.launch(dispatcherProvider.main) {
            when (val result = repo.deletePoint(id)) {
                is Result.Success -> {
                    loadPoints()
                    refreshOfflineQueueCount()
                    _uiState.update {
                        it.copy(
                            selectedPoint = null,
                            pointFeedbackMessage = "نقطه با موفقیت حذف شد."
                        )
                    }
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(pointFeedbackMessage = "خطا در حذف نقطه: ${result.message}")
                    }
                }
                else -> {}
            }
        }
    }

    fun dismissPointFeedback() {
        _uiState.update { it.copy(pointFeedbackMessage = null) }
    }

    fun retry() {
        syncOfflineQueue()
        loadTeams(force = true)
        loadMessages()
        loadLiveStatuses()
        loadPoints()
        resumePolling()
    }

    fun showLocationRationale() {
        _uiState.update { it.copy(isLocationPermissionRationaleVisible = true) }
    }

    fun dismissLocationRationale() {
        _uiState.update { it.copy(isLocationPermissionRationaleVisible = false) }
    }

    fun onLocationPermissionDenied(isPermanentlyDenied: Boolean = false) {
        _uiState.update {
            it.copy(
                isLocationPermissionDenied = true,
                isLocationPermissionPermanentlyDenied = isPermanentlyDenied
            )
        }
    }

    fun dismissLocationPermissionDenied() {
        _uiState.update {
            it.copy(
                isLocationPermissionDenied = false,
                isLocationPermissionPermanentlyDenied = false
            )
        }
    }

    fun stopLocationSharing() {
        stopLocationSharingUseCase?.invoke()
    }

    fun selectGroupMember(member: GroupMemberLocation?) {
        _uiState.update { it.copy(selectedGroupMember = member) }
    }

    override fun onCleared() {
        super.onCleared()
        stopPolling()
    }

    companion object {
        fun provideFactory(
            teamRepository: TeamRepository,
            messageRepository: MessageRepository? = null,
            statusRepository: StatusRepository? = null,
            authRepository: AuthRepository? = null,
            adminRepository: AdminRepository? = null,
            offlineSyncManager: OfflineSyncManager? = null,
            pointRepository: PointOfInterestRepository? = null,
            observeLocationSharingStateUseCase: ObserveLocationSharingStateUseCase? = null,
            stopLocationSharingUseCase: StopLocationSharingUseCase? = null,
            dispatcherProvider: DispatcherProvider = DefaultDispatcherProvider()
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
                    return HomeViewModel(
                        teamRepository = teamRepository,
                        messageRepository = messageRepository,
                        statusRepository = statusRepository,
                        authRepository = authRepository,
                        adminRepository = adminRepository,
                        offlineSyncManager = offlineSyncManager,
                        pointRepository = pointRepository,
                        observeLocationSharingStateUseCase = observeLocationSharingStateUseCase,
                        stopLocationSharingUseCase = stopLocationSharingUseCase,
                        dispatcherProvider = dispatcherProvider
                    ) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
            }
        }
    }
}
