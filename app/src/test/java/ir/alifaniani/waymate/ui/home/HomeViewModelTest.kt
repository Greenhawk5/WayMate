package ir.alifaniani.waymate.ui.home

import ir.alifaniani.waymate.TestDispatcherProvider
import ir.alifaniani.waymate.core.network.ApiException
import ir.alifaniani.waymate.data.repository.FakeAuthRepository
import ir.alifaniani.waymate.data.repository.FakeMessageRepository
import ir.alifaniani.waymate.data.repository.FakeStatusRepository
import ir.alifaniani.waymate.data.repository.FakeTeamRepository
import ir.alifaniani.waymate.domain.model.ConnectionStatus
import ir.alifaniani.waymate.domain.model.OperationalStatus
import ir.alifaniani.waymate.domain.model.TacticalMessage
import ir.alifaniani.waymate.domain.model.Team
import ir.alifaniani.waymate.data.repository.FakeAdminRepository
import ir.alifaniani.waymate.domain.model.UserRole
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class HomeViewModelTest {

    private lateinit var fakeTeamRepository: FakeTeamRepository
    private lateinit var fakeMessageRepository: FakeMessageRepository
    private lateinit var fakeStatusRepository: FakeStatusRepository
    private lateinit var fakeAuthRepository: FakeAuthRepository
    private lateinit var fakeAdminRepository: FakeAdminRepository
    private lateinit var testDispatcherProvider: TestDispatcherProvider
    private lateinit var viewModel: HomeViewModel

    @Before
    fun setup() {
        fakeTeamRepository = FakeTeamRepository(simulatedDelayMs = 0L)
        fakeMessageRepository = FakeMessageRepository()
        fakeStatusRepository = FakeStatusRepository()
        fakeAuthRepository = FakeAuthRepository(simulatedDelayMs = 0L)
        fakeAdminRepository = FakeAdminRepository()
        testDispatcherProvider = TestDispatcherProvider()

        // Seed authenticated user in fake auth repository
        runBlocking {
            fakeAuthRepository.login("commander_demo", "demo123")
        }

        viewModel = HomeViewModel(
            teamRepository = fakeTeamRepository,
            messageRepository = fakeMessageRepository,
            statusRepository = fakeStatusRepository,
            authRepository = fakeAuthRepository,
            adminRepository = fakeAdminRepository,
            dispatcherProvider = testDispatcherProvider
        )
    }

    @Test
    fun initialization_loadsTeamsAutomatically() {
        val state = viewModel.uiState.value

        assertFalse("isLoading should be false after load", state.isLoading)
        assertNull("errorMessage should be null on success", state.errorMessage)
        assertFalse("isForbidden should be false", state.isForbidden)
        assertFalse("isSessionExpired should be false", state.isSessionExpired)
        assertTrue("isSyntheticData should be true", state.isSyntheticData)
        assertEquals(1, state.teams.size)
        assertEquals("team_alpha_001", state.teams[0].id)
        assertEquals("گشت آلفا (واکنش سریع)", state.teams[0].name)
        assertEquals(2, state.teams[0].members.size)
    }

    @Test
    fun loadTeams_emptyList_emitsEmptySuccessState() {
        fakeTeamRepository.syntheticTeams.clear()

        viewModel.loadTeams()

        val state = viewModel.uiState.value
        assertFalse("isLoading should be false", state.isLoading)
        assertNull("errorMessage should be null", state.errorMessage)
        assertTrue("teams should be empty", state.teams.isEmpty())
        assertTrue("isEmpty helper property should be true", state.isEmpty)
        assertFalse("isSuccess helper property should be false", state.isSuccess)
    }

    @Test
    fun loadTeams_http403Forbidden_emitsForbiddenState() {
        fakeTeamRepository.shouldFail = true
        fakeTeamRepository.failureError = ApiException.Forbidden("دسترسی به این عملیات برای نقش شما مجاز نمی‌باشد")
        fakeTeamRepository.failureMessage = "دسترسی به این عملیات برای نقش شما مجاز نمی‌باشد"

        viewModel.loadTeams()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertTrue("isForbidden should be true", state.isForbidden)
        assertFalse("isSessionExpired should be false", state.isSessionExpired)
        assertEquals("دسترسی به این عملیات برای نقش شما مجاز نمی‌باشد", state.errorMessage)
    }

    @Test
    fun loadTeams_http401Unauthorized_emitsSessionExpiredAndLogsOut() {
        fakeTeamRepository.shouldFail = true
        fakeTeamRepository.failureError = ApiException.Unauthorized("نشست کاربری نامعتبر است یا منقضی شده است")
        fakeTeamRepository.failureMessage = "نشست کاربری نامعتبر است یا منقضی شده است"

        viewModel.loadTeams()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertTrue("isSessionExpired should be true", state.isSessionExpired)
        assertEquals("نشست کاربری نامعتبر است یا منقضی شده است", state.errorMessage)

        // Verify session was terminated in authRepository
        assertNull("User session should be cleared after 401", fakeAuthRepository.observeCurrentUser().value)
    }

    @Test
    fun loadTeams_serverError_emitsRecoverableErrorMessage() {
        fakeTeamRepository.shouldFail = true
        fakeTeamRepository.failureError = ApiException.ServerError(500, "خطای موقت در سرور رخ داده است.")
        fakeTeamRepository.failureMessage = "خطای موقت در سرور رخ داده است."

        viewModel.loadTeams()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertFalse(state.isForbidden)
        assertFalse(state.isSessionExpired)
        assertEquals("خطای موقت در سرور رخ داده است.", state.errorMessage)
    }

    @Test
    fun retry_reloadsSuccessfullyAfterError() {
        // Step 1: Simulate network failure
        fakeTeamRepository.shouldFail = true
        fakeTeamRepository.failureError = ApiException.NetworkUnavailable("خطای شبکه")
        fakeTeamRepository.failureMessage = "خطای شبکه"
        viewModel.loadTeams()

        assertTrue(viewModel.uiState.value.errorMessage != null)

        // Step 2: Clear failure flag and trigger retry
        fakeTeamRepository.shouldFail = false
        viewModel.retry()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.errorMessage)
        assertEquals(1, state.teams.size)
        assertEquals("گشت آلفا (واکنش سریع)", state.teams[0].name)
    }

    @Test
    fun syntheticDemoData_correctlyLabeled() {
        val state = viewModel.uiState.value
        assertTrue("Demo data must be explicitly flagged as synthetic", state.isSyntheticData)
        assertEquals("۴۴-ب-۱۲", state.teams[0].vehiclePlate)
    }

    @Test
    fun loadMessages_populatesStateAndUpdatesConnectionStatus() {
        val testMsg = TacticalMessage(
            id = "msg_101",
            senderCallsign = "فرمانده",
            body = "دستور استقرار در تقاطع",
            isUrgent = true,
            createdAt = 1000L,
            isAcknowledged = false
        )
        fakeMessageRepository.setMessages(listOf(testMsg))

        viewModel.loadMessages()

        val state = viewModel.uiState.value
        assertEquals(1, state.messages.size)
        assertEquals("msg_101", state.messages[0].id)
        assertTrue(state.messages[0].isUrgent)
        assertEquals(1, state.unacknowledgedMessagesCount)
        assertEquals(ConnectionStatus.ONLINE, state.connectionStatus)
    }

    @Test
    fun acknowledgeMessage_optimisticallyMarksMessageAcknowledged() {
        val testMsg = TacticalMessage(
            id = "msg_102",
            senderCallsign = "فرمانده",
            body = "گزارش وضعیت ارسال شود",
            isUrgent = false,
            createdAt = 1000L,
            isAcknowledged = false
        )
        fakeMessageRepository.setMessages(listOf(testMsg))
        viewModel.loadMessages()

        viewModel.acknowledgeMessage("msg_102")

        val state = viewModel.uiState.value
        assertTrue(state.messages[0].isAcknowledged)
        assertEquals(0, state.unacknowledgedMessagesCount)
    }

    @Test
    fun updateOperationalStatus_updatesStateImmediately() {
        assertEquals(OperationalStatus.NORMAL, viewModel.uiState.value.myOperationalStatus)

        viewModel.updateOperationalStatus(OperationalStatus.PURSUING)

        val state = viewModel.uiState.value
        assertEquals(OperationalStatus.PURSUING, state.myOperationalStatus)
        assertEquals(OperationalStatus.PURSUING, fakeStatusRepository.currentOperationalStatus)
    }

    @Test
    fun updateOperationalStatus_error_revertsToPreviousStatus() {
        fakeStatusRepository.shouldFail = true
        fakeStatusRepository.failureMessage = "خطا در برقراری ارتباط با سرور"

        viewModel.updateOperationalStatus(OperationalStatus.ENGAGED)

        val state = viewModel.uiState.value
        assertEquals(OperationalStatus.NORMAL, state.myOperationalStatus)
        assertTrue(state.errorMessage != null)
    }

    @Test
    fun sendTacticalMessage_callsRepositoryAndRefreshes() {
        viewModel.sendTacticalMessage("دستور آماده‌باش عمومی", isUrgent = true, targetTeamId = null)

        val state = viewModel.uiState.value
        assertEquals(1, state.messages.size)
        assertEquals("دستور آماده‌باش عمومی", state.messages[0].body)
        assertTrue(state.messages[0].isUrgent)
    }

    @Test
    fun pollingLifecycle_pauseAndResume_updatesState() {
        assertTrue(viewModel.uiState.value.isPollingActive)

        viewModel.pausePolling()
        assertFalse(viewModel.uiState.value.isPollingActive)

        viewModel.resumePolling()
        assertTrue(viewModel.uiState.value.isPollingActive)

        viewModel.stopPolling()
        assertFalse(viewModel.uiState.value.isPollingActive)
    }

    @Test
    fun networkError_transitionsConnectionStatusToOffline() {
        fakeMessageRepository.shouldFail = true
        fakeMessageRepository.failureMessage = "خطای اینترانت"

        viewModel.loadMessages()

        val state = viewModel.uiState.value
        assertEquals(ConnectionStatus.OFFLINE, state.connectionStatus)
    }

    // ==========================================
    // Phase 9 Management & RBAC Tests
    // ==========================================

    @Test
    fun loadManagementData_asOwner_populatesUsersLogsAndStats() {
        viewModel.loadManagementData(UserRole.OWNER)

        val state = viewModel.uiState.value
        assertFalse(state.isManagementLoading)
        assertEquals(3, state.managedUsers.size)
        assertEquals(1, state.auditLogs.size)
        assertNotNull(state.systemStats)
        assertEquals(3, state.systemStats?.totalUsers)
    }

    @Test
    fun loadManagementData_asCommander_populatesOnlyStats() {
        viewModel.loadManagementData(UserRole.COMMANDER)

        val state = viewModel.uiState.value
        assertFalse(state.isManagementLoading)
        assertTrue("Managed users must be empty for COMMANDER", state.managedUsers.isEmpty())
        assertTrue("Audit logs must be empty for COMMANDER", state.auditLogs.isEmpty())
        assertNotNull(state.systemStats)
    }

    @Test
    fun loadManagementData_asTeamLead_isNoOp() {
        viewModel.loadManagementData(UserRole.TEAM_LEAD)

        val state = viewModel.uiState.value
        assertFalse(state.isManagementLoading)
        assertTrue(state.managedUsers.isEmpty())
        assertTrue(state.auditLogs.isEmpty())
        assertNull(state.systemStats)
    }

    @Test
    fun createUser_success_updatesStateAndRefreshesUsers() {
        viewModel.createUser(
            username = "lead_charlie",
            password = "secure_password",
            role = UserRole.TEAM_LEAD,
            callsign = "سرتیم چارلی"
        )

        val state = viewModel.uiState.value
        assertFalse(state.isManagementLoading)
        assertFalse(state.isManagementFeedbackError)
        assertNotNull(state.managementFeedbackMessage)
        assertTrue(state.managementFeedbackMessage!!.contains("سرتیم چارلی"))
        assertEquals(4, state.managedUsers.size)
    }

    @Test
    fun toggleUserStatus_flipsIsActiveState() {
        viewModel.loadManagementData(UserRole.OWNER)
        val initialUser = viewModel.uiState.value.managedUsers.first { it.id == "usr_lead_001" }
        assertTrue(initialUser.isActive)

        viewModel.toggleUserStatus(initialUser)

        val state = viewModel.uiState.value
        assertFalse(state.isManagementLoading)
        assertFalse(state.isManagementFeedbackError)
        val updatedUser = state.managedUsers.first { it.id == "usr_lead_001" }
        assertFalse("User should now be deactivated", updatedUser.isActive)
    }

    @Test
    fun createTeam_provisionsTeamAndUpdatesTeamsList() {
        val initialTeamCount = viewModel.uiState.value.teams.size

        viewModel.createTeam(
            name = "گشت گاما (کوهستان)",
            leadUserId = "usr_lead_001",
            vehicleType = "پاترول",
            vehiclePlate = "۳۳-ج-۵۵",
            equipmentSummary = "طناب راپل، وینچ، جک هایلیفت",
            members = emptyList(),
            userRole = UserRole.COMMANDER
        )

        val state = viewModel.uiState.value
        assertFalse(state.isManagementLoading)
        assertFalse(state.isManagementFeedbackError)
        assertEquals(initialTeamCount + 1, state.teams.size)
        assertTrue(state.teams.any { it.name == "گشت گاما (کوهستان)" })
    }

    @Test
    fun toggleTeamStatus_flipsTeamActiveFlag() {
        val initialTeam = viewModel.uiState.value.teams.first()
        assertTrue(initialTeam.isActive)

        viewModel.toggleTeamStatus(initialTeam, UserRole.COMMANDER)

        val state = viewModel.uiState.value
        assertFalse(state.isManagementLoading)
        assertFalse(state.isManagementFeedbackError)
        val updatedTeam = state.teams.first { it.id == initialTeam.id }
        assertFalse("Team should now be deactivated", updatedTeam.isActive)
    }

    @Test
    fun dismissManagementFeedback_clearsFeedbackMessage() {
        viewModel.createUser(
            username = "lead_delta",
            password = "secure_password",
            role = UserRole.TEAM_LEAD,
            callsign = "سرتیم دلتا"
        )
        assertNotNull(viewModel.uiState.value.managementFeedbackMessage)

        viewModel.dismissManagementFeedback()
        assertNull(viewModel.uiState.value.managementFeedbackMessage)
    }
}
