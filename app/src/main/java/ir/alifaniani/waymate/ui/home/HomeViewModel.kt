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
import ir.alifaniani.waymate.domain.model.ManagedUser
import ir.alifaniani.waymate.domain.model.OperationalStatus
import ir.alifaniani.waymate.domain.model.TacticalMessage
import ir.alifaniani.waymate.domain.model.Team
import ir.alifaniani.waymate.domain.model.TeamMember
import ir.alifaniani.waymate.domain.model.UserRole
import ir.alifaniani.waymate.domain.repository.AdminRepository
import ir.alifaniani.waymate.domain.repository.AuthRepository
import ir.alifaniani.waymate.domain.repository.MessageRepository
import ir.alifaniani.waymate.domain.repository.StatusRepository
import ir.alifaniani.waymate.domain.repository.TeamRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel responsible for managing tactical team roster, live link health,
 * operational status selection, real-time tactical messaging with safe polling,
 * Phase 9 roles and management operations,
 * and Phase 10 local offline storage and synchronization.
 */
class HomeViewModel(
    private val teamRepository: TeamRepository,
    private val messageRepository: MessageRepository? = null,
    private val statusRepository: StatusRepository? = null,
    private val authRepository: AuthRepository? = null,
    private val adminRepository: AdminRepository? = null,
    private val offlineSyncManager: OfflineSyncManager? = null,
    private val dispatcherProvider: DispatcherProvider = DefaultDispatcherProvider(),
    private val poller: SafePoller = SafePoller()
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadTeams()
        loadMessages()
        loadLiveStatuses()
        refreshOfflineQueueCount()
        startPolling()
    }

    fun loadTeams() {
        if (_uiState.value.isLoading) return

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
                    _uiState.update {
                        it.copy(
                            liveStatuses = result.data,
                            connectionStatus = ConnectionStatus.ONLINE,
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
        val previousStatus = _uiState.value.myOperationalStatus
        _uiState.update { it.copy(myOperationalStatus = newStatus) }

        viewModelScope.launch(dispatcherProvider.main) {
            when (val result = repo.updateOperationalStatus(newStatus)) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            myOperationalStatus = result.data,
                            connectionStatus = ConnectionStatus.ONLINE
                        )
                    }
                    refreshOfflineQueueCount()
                }
                is Result.Error -> {
                    _uiState.update { it.copy(myOperationalStatus = previousStatus) }
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
        viewModelScope.launch(dispatcherProvider.main) {
            when (val result = repo.sendMessage(body, isUrgent, targetTeamId)) {
                is Result.Success -> {
                    _uiState.update { state ->
                        state.copy(messages = listOf(result.data) + state.messages)
                    }
                    refreshOfflineQueueCount()
                }
                is Result.Error -> {
                    handleError(result)
                    refreshOfflineQueueCount()
                }
                else -> {}
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
                loadTeams()
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
                    _uiState.update {
                        it.copy(
                            isManagementLoading = false,
                            managementFeedbackMessage = "گروه عملیاتی '${result.data.name}' با موفقیت ایجاد شد",
                            isManagementFeedbackError = false
                        )
                    }
                    refreshOfflineQueueCount()
                    loadTeams()
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
        _uiState.update { it.copy(isManagementLoading = true) }

        viewModelScope.launch(dispatcherProvider.main) {
            val newStatus = !team.isActive
            when (val result = teamRepository.updateTeam(team.id, isActive = newStatus)) {
                is Result.Success -> {
                    val statusText = if (newStatus) "فعال" else "غیرفعال"
                    _uiState.update {
                        it.copy(
                            isManagementLoading = false,
                            managementFeedbackMessage = "وضعیت گروه '${team.name}' به $statusText تغییر یافت",
                            isManagementFeedbackError = false
                        )
                    }
                    loadTeams()
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

    fun retry() {
        syncOfflineQueue()
        loadTeams()
        loadMessages()
        loadLiveStatuses()
        resumePolling()
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
                        dispatcherProvider = dispatcherProvider
                    ) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
            }
        }
    }
}
