package ir.alifaniani.waymate.data.repository

import ir.alifaniani.waymate.TestDispatcherProvider
import ir.alifaniani.waymate.core.common.Result
import ir.alifaniani.waymate.data.location.LocationDataSource
import ir.alifaniani.waymate.data.network.LocationWebSocketClient
import ir.alifaniani.waymate.data.network.WsConnectionState
import ir.alifaniani.waymate.data.session.SessionStorage
import ir.alifaniani.waymate.domain.model.ActiveSharerDto
import ir.alifaniani.waymate.domain.model.LocationSharingStatus
import ir.alifaniani.waymate.domain.model.ServerLocationEnvelope
import ir.alifaniani.waymate.domain.model.User
import ir.alifaniani.waymate.domain.model.UserLocation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RealLocationRepositoryTest {

    private lateinit var fakeLocationDataSource: FakeLocationDataSource
    private lateinit var fakeWebSocketClient: FakeLocationWebSocketClient
    private lateinit var fakeSessionStorage: FakeSessionStorage
    private lateinit var repository: RealLocationRepository
    private val testDispatcher = TestDispatcherProvider()

    @Before
    fun setUp() {
        fakeLocationDataSource = FakeLocationDataSource()
        fakeWebSocketClient = FakeLocationWebSocketClient()
        fakeSessionStorage = FakeSessionStorage()

        repository = RealLocationRepository(
            locationDataSource = fakeLocationDataSource,
            webSocketClient = fakeWebSocketClient,
            sessionStorage = fakeSessionStorage,
            dispatcherProvider = testDispatcher
        )
    }

    @Test
    fun startSharing_failsWhenUnauthenticated() = runBlocking {
        fakeSessionStorage.storedToken = null

        val result = repository.startSharing("team_alpha")
        assertTrue(result is Result.Error)
        assertEquals(LocationSharingStatus.ERROR, repository.sharingState.value.status)
        assertFalse(fakeWebSocketClient.isConnectCalled)
    }

    @Test
    fun startSharing_failsWhenPermissionDenied() = runBlocking {
        fakeLocationDataSource.hasPermission = false

        val result = repository.startSharing("team_alpha")
        assertTrue(result is Result.Error)
        assertEquals(LocationSharingStatus.PERMISSION_DENIED, repository.sharingState.value.status)
        assertFalse(fakeWebSocketClient.isConnectCalled)
    }

    @Test
    fun startSharing_failsWhenGpsDisabled() = runBlocking {
        fakeLocationDataSource.isGpsOn = false

        val result = repository.startSharing("team_alpha")
        assertTrue(result is Result.Error)
        assertEquals(LocationSharingStatus.GPS_DISABLED, repository.sharingState.value.status)
        assertFalse(fakeWebSocketClient.isConnectCalled)
    }

    @Test
    fun startSharing_succeedsAndConnectsWebSocket() = runBlocking {
        val result = repository.startSharing("team_alpha")
        assertTrue(result is Result.Success)
        assertTrue(fakeWebSocketClient.isConnectCalled)
        assertEquals("test_token_123", fakeWebSocketClient.connectedToken)
        assertEquals("team_alpha", fakeWebSocketClient.connectedTeamId)
        assertEquals(LocationSharingStatus.STARTING, repository.sharingState.value.status)
    }

    @Test
    fun connectionState_updatesSharingStatusCorrectly() = runBlocking {
        repository.startSharing("team_alpha")

        fakeWebSocketClient.setConnectionState(WsConnectionState.CONNECTED)
        assertEquals(LocationSharingStatus.SHARING, repository.sharingState.value.status)

        fakeWebSocketClient.setConnectionState(WsConnectionState.RECONNECTING)
        assertEquals(LocationSharingStatus.RECONNECTING, repository.sharingState.value.status)

        fakeWebSocketClient.setConnectionState(WsConnectionState.DISCONNECTED)
        assertEquals(LocationSharingStatus.NETWORK_UNAVAILABLE, repository.sharingState.value.status)
    }

    @Test
    fun publishLocation_ignoresInvalidLocation() = runBlocking {
        repository.startSharing("team_alpha")
        fakeWebSocketClient.setConnectionState(WsConnectionState.CONNECTED)

        val invalidLocation = UserLocation(
            latitude = 95.0, // Invalid latitude
            longitude = 53.0,
            accuracyMeters = 5f,
            timestamp = 1000L
        )

        repository.publishLocation(invalidLocation)
        assertNull(repository.sharingState.value.myLocation)
        assertTrue(fakeWebSocketClient.sentUpdates.isEmpty())
    }

    @Test
    fun publishLocation_sendsValidLocationWhenSharing() = runBlocking {
        repository.startSharing("team_alpha")
        fakeWebSocketClient.setConnectionState(WsConnectionState.CONNECTED)

        val validLocation = UserLocation(
            latitude = 28.5303,
            longitude = 53.6008,
            accuracyMeters = 4.0f,
            altitudeMeters = 1050.0,
            speedMps = 1.1f,
            bearingDegrees = 180f,
            timestamp = 1700000000000L,
            provider = "gps"
        )

        repository.publishLocation(validLocation)
        assertEquals(validLocation, repository.sharingState.value.myLocation)
        assertEquals(1, fakeWebSocketClient.sentUpdates.size)
        assertEquals(28.5303, fakeWebSocketClient.sentUpdates.first().latitude, 0.0001)
    }

    @Test
    fun incoming_initialRoster_populatesActiveSharers() = runBlocking {
        repository.startSharing("team_alpha")

        val rosterEnvelope = ServerLocationEnvelope(
            type = "INITIAL_ROSTER",
            teamId = "team_alpha",
            activeSharers = listOf(
                ActiveSharerDto(
                    userId = "usr_001",
                    callsign = "شاهین",
                    latitude = 28.53,
                    longitude = 53.60,
                    accuracy = 5f,
                    timestamp = 1700000000000L
                )
            )
        )

        fakeWebSocketClient.emitMessage(rosterEnvelope)
        val sharers = repository.sharingState.value.activeSharers
        assertEquals(1, sharers.size)
        assertEquals("usr_001", sharers.first().userId)
        assertEquals("شاهین", sharers.first().callsign)
    }

    @Test
    fun incoming_locationUpdate_addsAndUpdatesMember() = runBlocking {
        repository.startSharing("team_alpha")

        val update1 = ServerLocationEnvelope(
            type = "LOCATION_UPDATE",
            userId = "usr_002",
            callsign = "عقاب",
            teamId = "team_alpha",
            latitude = 28.54,
            longitude = 53.61,
            accuracy = 4f,
            timestamp = 1700000005000L
        )
        fakeWebSocketClient.emitMessage(update1)

        var sharers = repository.sharingState.value.activeSharers
        assertEquals(1, sharers.size)
        assertEquals(28.54, sharers.first().latitude, 0.001)

        // Member coordinates update
        val update2 = update1.copy(latitude = 28.545, timestamp = 1700000010000L)
        fakeWebSocketClient.emitMessage(update2)

        sharers = repository.sharingState.value.activeSharers
        assertEquals(1, sharers.size)
        assertEquals(28.545, sharers.first().latitude, 0.001)
    }

    @Test
    fun incoming_sharingStopped_removesMemberFromActiveSharers() = runBlocking {
        repository.startSharing("team_alpha")

        // First add member
        val update = ServerLocationEnvelope(
            type = "LOCATION_UPDATE",
            userId = "usr_003",
            callsign = "دالامپر",
            teamId = "team_alpha",
            latitude = 28.55,
            longitude = 53.62,
            accuracy = 3f,
            timestamp = 1700000005000L
        )
        fakeWebSocketClient.emitMessage(update)
        assertEquals(1, repository.sharingState.value.activeSharers.size)

        // Stop signal arrives
        val stopEnvelope = ServerLocationEnvelope(
            type = "SHARING_STOPPED",
            userId = "usr_003",
            teamId = "team_alpha"
        )
        fakeWebSocketClient.emitMessage(stopEnvelope)
        assertTrue(repository.sharingState.value.activeSharers.isEmpty())
    }

    @Test
    fun stopSharing_cleansUpAndDisconnects() = runBlocking {
        repository.startSharing("team_alpha")
        fakeWebSocketClient.setConnectionState(WsConnectionState.CONNECTED)

        repository.stopSharing()

        assertEquals(LocationSharingStatus.STOPPED, repository.sharingState.value.status)
        assertTrue(fakeWebSocketClient.isSendSharingStoppedCalled)
        assertTrue(fakeWebSocketClient.isDisconnectCalled)
    }

    @Test
    fun locationFlow_transitionsToPermissionDeniedOnSecurityException() = runBlocking {
        fakeLocationDataSource.shouldThrowOnCollect = SecurityException("Permission revoked")
        repository.startSharing("team_alpha")
        yield()

        assertEquals(LocationSharingStatus.PERMISSION_DENIED, repository.sharingState.value.status)
    }

    @Test
    fun locationFlow_transitionsToGpsDisabledOnProviderException() = runBlocking {
        fakeLocationDataSource.shouldThrowOnCollect = IllegalStateException("GPS turned off")
        repository.startSharing("team_alpha")
        yield()

        assertEquals(LocationSharingStatus.GPS_DISABLED, repository.sharingState.value.status)
    }

    // ==========================================
    // Test Doubles
    // ==========================================

    private class FakeLocationDataSource : LocationDataSource {
        var hasPermission = true
        var isGpsOn = true
        var shouldThrowOnCollect: Throwable? = null
        val locationFlow = MutableSharedFlow<UserLocation>()

        override fun getLocationUpdates(): Flow<UserLocation> = flow {
            val err = shouldThrowOnCollect
            if (err != null) {
                throw err
            }
            locationFlow.collect { emit(it) }
        }
        override fun isGpsEnabled(): Boolean = isGpsOn
        override fun hasLocationPermission(): Boolean = hasPermission
        override fun hasFineLocation(): Boolean = hasPermission
        override fun hasCoarseLocation(): Boolean = hasPermission
    }

    private class FakeLocationWebSocketClient : LocationWebSocketClient {
        private val _connectionState = MutableStateFlow(WsConnectionState.DISCONNECTED)
        override val connectionState: StateFlow<WsConnectionState> = _connectionState.asStateFlow()

        private val _incomingMessages = MutableSharedFlow<ServerLocationEnvelope>(extraBufferCapacity = 64)
        override val incomingMessages: SharedFlow<ServerLocationEnvelope> = _incomingMessages.asSharedFlow()

        var isConnectCalled = false
        var isDisconnectCalled = false
        var isSendSharingStoppedCalled = false
        var connectedToken: String? = null
        var connectedTeamId: String? = null
        val sentUpdates = mutableListOf<UserLocation>()

        fun setConnectionState(state: WsConnectionState) {
            _connectionState.value = state
        }

        suspend fun emitMessage(envelope: ServerLocationEnvelope) {
            _incomingMessages.emit(envelope)
        }

        override fun connect(token: String, teamId: String?) {
            isConnectCalled = true
            connectedToken = token
            connectedTeamId = teamId
            _connectionState.value = WsConnectionState.CONNECTING
        }

        override fun disconnect() {
            isDisconnectCalled = true
            _connectionState.value = WsConnectionState.DISCONNECTED
        }

        override fun sendLocationUpdate(location: UserLocation): Boolean {
            sentUpdates.add(location)
            return true
        }

        override fun sendSharingStopped(): Boolean {
            isSendSharingStoppedCalled = true
            return true
        }
    }

    private class FakeSessionStorage : SessionStorage {
        var storedToken: String? = "test_token_123"
        var storedUser: User? = null
        private var storedExpiresAt: Long = Long.MAX_VALUE

        override fun saveSession(token: String, user: User, expiresAt: Long) {
            this.storedToken = token
            this.storedUser = user
            this.storedExpiresAt = expiresAt
        }

        override fun getToken(): String? = storedToken

        override fun getUser(): User? = storedUser

        override fun getExpiresAt(): Long = storedExpiresAt

        override fun isSessionValid(): Boolean = storedToken != null

        override fun clearSession() {
            storedToken = null
            storedUser = null
            storedExpiresAt = 0L
        }
    }
}
