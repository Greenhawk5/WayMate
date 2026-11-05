package ir.alifaniani.waymate.data.network

import ir.alifaniani.waymate.domain.model.ClientLocationUpdatePayload
import ir.alifaniani.waymate.domain.model.ClientPingPayload
import ir.alifaniani.waymate.domain.model.ClientSharingStartedPayload
import ir.alifaniani.waymate.domain.model.ClientSharingStoppedPayload
import ir.alifaniani.waymate.domain.model.ServerLocationEnvelope
import ir.alifaniani.waymate.domain.model.UserLocation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.min
import kotlin.random.Random

enum class WsConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    RECONNECTING
}

interface LocationWebSocketClient {
    val connectionState: StateFlow<WsConnectionState>
    val incomingMessages: SharedFlow<ServerLocationEnvelope>
    fun connect(token: String, teamId: String? = null)
    fun disconnect()
    fun sendLocationUpdate(location: UserLocation): Boolean
    fun sendSharingStopped(): Boolean
}

/**
 * Robust, reconnecting OkHttp WebSocket client for real-time location streaming.
 *
 * Implements:
 * - Exponential backoff with random jitter on network loss
 * - Sequence numbering
 * - Periodic keepalive ping/pong
 * - Thread-safe message publication
 */
class RealLocationWebSocketClient(
    private val okHttpClient: OkHttpClient,
    private val json: Json,
    private val baseUrlProvider: () -> String = { ApiConfig.getBaseUrl() }
) : LocationWebSocketClient {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _connectionState = MutableStateFlow(WsConnectionState.DISCONNECTED)
    override val connectionState: StateFlow<WsConnectionState> = _connectionState.asStateFlow()

    private val _incomingMessages = MutableSharedFlow<ServerLocationEnvelope>(extraBufferCapacity = 64)
    override val incomingMessages: SharedFlow<ServerLocationEnvelope> = _incomingMessages.asSharedFlow()

    private var activeWebSocket: WebSocket? = null
    private var sequenceNumber = AtomicLong(0L)

    private var targetToken: String? = null
    private var targetTeamId: String? = null
    private var shouldKeepConnected = false

    private var reconnectJob: Job? = null
    private var pingJob: Job? = null
    private var reconnectAttempt = 0

    override fun connect(token: String, teamId: String?) {
        targetToken = token
        targetTeamId = teamId
        shouldKeepConnected = true
        reconnectAttempt = 0
        initiateConnection()
    }

    override fun disconnect() {
        shouldKeepConnected = false
        reconnectJob?.cancel()
        reconnectJob = null
        stopPingJob()

        activeWebSocket?.close(1000, "Client stopped location sharing")
        activeWebSocket = null
        _connectionState.value = WsConnectionState.DISCONNECTED
    }

    override fun sendLocationUpdate(location: UserLocation): Boolean {
        if (_connectionState.value != WsConnectionState.CONNECTED) return false

        val payload = ClientLocationUpdatePayload(
            seq = sequenceNumber.incrementAndGet(),
            latitude = location.latitude,
            longitude = location.longitude,
            accuracy = location.accuracyMeters,
            altitude = location.altitudeMeters,
            speed = location.speedMps,
            bearing = location.bearingDegrees,
            timestamp = location.timestamp
        )

        return sendPayload(payload)
    }

    fun sendSharingStarted(): Boolean {
        if (_connectionState.value != WsConnectionState.CONNECTED) return false
        val payload = ClientSharingStartedPayload(
            seq = sequenceNumber.incrementAndGet(),
            timestamp = System.currentTimeMillis()
        )
        return sendPayload(payload)
    }

    override fun sendSharingStopped(): Boolean {
        if (_connectionState.value != WsConnectionState.CONNECTED) return false
        val payload = ClientSharingStoppedPayload(
            seq = sequenceNumber.incrementAndGet(),
            timestamp = System.currentTimeMillis()
        )
        return sendPayload(payload)
    }

    private inline fun <reified T> sendPayload(payload: T): Boolean {
        val socket = activeWebSocket ?: return false
        return try {
            val jsonText = json.encodeToString(payload)
            socket.send(jsonText)
        } catch (_: Exception) {
            false
        }
    }

    private fun initiateConnection() {
        val token = targetToken ?: return
        if (!shouldKeepConnected) return

        _connectionState.value = if (reconnectAttempt == 0) WsConnectionState.CONNECTING else WsConnectionState.RECONNECTING

        val wsUrl = buildWsUrl(targetTeamId)
        val request = Request.Builder()
            .url(wsUrl)
            .addHeader("Authorization", "Bearer $token")
            .build()

        activeWebSocket?.cancel()
        activeWebSocket = okHttpClient.newWebSocket(request, createListener())
    }

    private fun createListener(): WebSocketListener {
        return object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                android.util.Log.d("WayMateWS", "WebSocket connected successfully to ${webSocket.request().url}")
                activeWebSocket = webSocket
                reconnectAttempt = 0
                _connectionState.value = WsConnectionState.CONNECTED
                startPingJob()
                sendSharingStarted()
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                android.util.Log.d("WayMateWS", "onMessage received: $text")
                try {
                    val envelope = json.decodeFromString<ServerLocationEnvelope>(text)
                    val emitted = _incomingMessages.tryEmit(envelope)
                    android.util.Log.d("WayMateWS", "Emitted envelope ${envelope.type}: success=$emitted")
                } catch (e: Exception) {
                    android.util.Log.e("WayMateWS", "Failed to decode envelope: $text", e)
                }
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                android.util.Log.d("WayMateWS", "WebSocket closing: code=$code reason=$reason")
                webSocket.close(1000, null)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                android.util.Log.d("WayMateWS", "WebSocket closed: code=$code reason=$reason")
                stopPingJob()
                if (shouldKeepConnected) {
                    scheduleReconnect()
                } else {
                    _connectionState.value = WsConnectionState.DISCONNECTED
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                android.util.Log.e("WayMateWS", "WebSocket failure: ${t.message}, response code=${response?.code}", t)
                stopPingJob()
                if (shouldKeepConnected) {
                    scheduleReconnect()
                } else {
                    _connectionState.value = WsConnectionState.DISCONNECTED
                }
            }
        }
    }

    private fun scheduleReconnect() {
        if (!shouldKeepConnected) return
        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            reconnectAttempt++
            _connectionState.value = WsConnectionState.RECONNECTING

            // Exponential backoff: 1s, 2s, 4s, 8s, capped at 30s + 20% jitter
            val baseDelayMs = min(30_000L, 1000L * (1L shl min(reconnectAttempt, 5)))
            val jitter = Random.nextLong(0, baseDelayMs / 5)
            delay(baseDelayMs + jitter)

            if (shouldKeepConnected) {
                initiateConnection()
            }
        }
    }

    private fun startPingJob() {
        stopPingJob()
        pingJob = scope.launch {
            while (isActive && shouldKeepConnected) {
                delay(20_000L) // 20s heartbeat
                if (_connectionState.value == WsConnectionState.CONNECTED) {
                    val ping = ClientPingPayload(timestamp = System.currentTimeMillis())
                    sendPayload(ping)
                }
            }
        }
    }

    private fun stopPingJob() {
        pingJob?.cancel()
        pingJob = null
    }

    private fun buildWsUrl(teamId: String?): String {
        val httpBase = baseUrlProvider()
        val wsBase = when {
            httpBase.startsWith("https://") -> "wss://" + httpBase.removePrefix("https://")
            httpBase.startsWith("http://") -> "ws://" + httpBase.removePrefix("http://")
            else -> "ws://$httpBase"
        }

        val trimmed = wsBase.trimEnd('/')
        val path = if (trimmed.endsWith("/api/v1")) "$trimmed/ws/locations" else "$trimmed/api/v1/ws/locations"

        return if (!teamId.isNullOrBlank()) {
            "$path?team_id=$teamId"
        } else {
            path
        }
    }
}
