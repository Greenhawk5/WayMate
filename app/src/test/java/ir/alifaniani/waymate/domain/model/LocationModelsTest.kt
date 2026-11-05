package ir.alifaniani.waymate.domain.model

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LocationModelsTest {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    @Test
    fun userLocation_validation_acceptsValidCoordinates() {
        val loc = UserLocation(
            latitude = 28.5303,
            longitude = 53.6008,
            accuracyMeters = 4.2f,
            altitudeMeters = 1050.0,
            speedMps = 1.2f,
            bearingDegrees = 180f,
            timestamp = 1700000000000L,
            provider = "gps"
        )
        assertTrue(loc.isValid)
    }

    @Test
    fun userLocation_validation_boundaryCoordinates() {
        val northPole = UserLocation(90.0, 0.0, 5f, null, null, null, 1000L, "gps")
        val southPole = UserLocation(-90.0, 0.0, 5f, null, null, null, 1000L, "gps")
        val eastLimit = UserLocation(0.0, 180.0, 5f, null, null, null, 1000L, "gps")
        val westLimit = UserLocation(0.0, -180.0, 5f, null, null, null, 1000L, "gps")

        assertTrue(northPole.isValid)
        assertTrue(southPole.isValid)
        assertTrue(eastLimit.isValid)
        assertTrue(westLimit.isValid)
    }

    @Test
    fun userLocation_validation_rejectsInvalidLatitude() {
        val tooHigh = UserLocation(90.001, 53.0, 5f, null, null, null, 1000L, "gps")
        val tooLow = UserLocation(-90.001, 53.0, 5f, null, null, null, 1000L, "gps")

        assertFalse(tooHigh.isValid)
        assertFalse(tooLow.isValid)
    }

    @Test
    fun userLocation_validation_rejectsInvalidLongitude() {
        val tooEast = UserLocation(28.0, 180.001, 5f, null, null, null, 1000L, "gps")
        val tooWest = UserLocation(28.0, -180.001, 5f, null, null, null, 1000L, "gps")

        assertFalse(tooEast.isValid)
        assertFalse(tooWest.isValid)
    }

    @Test
    fun userLocation_validation_rejectsNegativeAccuracyOrTimestamp() {
        val negativeAccuracy = UserLocation(28.0, 53.0, -1.0f, null, null, null, 1000L, "gps")
        val zeroTimestamp = UserLocation(28.0, 53.0, 5f, null, null, null, 0L, "gps")
        val negativeTimestamp = UserLocation(28.0, 53.0, 5f, null, null, null, -100L, "gps")

        assertFalse(negativeAccuracy.isValid)
        assertFalse(zeroTimestamp.isValid)
        assertFalse(negativeTimestamp.isValid)
    }

    @Test
    fun groupMemberLocation_isStale_calculatesAccurately() {
        val now = 1700000060000L
        val freshMember = GroupMemberLocation(
            userId = "user_001",
            callsign = "شاهین",
            teamId = "team_alpha",
            latitude = 28.5303,
            longitude = 53.6008,
            accuracyMeters = 5f,
            bearingDegrees = 90f,
            speedMps = 1.0f,
            altitudeMeters = 1100.0,
            timestamp = now - 15000L // 15 seconds ago
        )
        assertFalse(freshMember.isStale(now))

        val staleMember = freshMember.copy(timestamp = now - 65000L) // 65 seconds ago
        assertTrue(staleMember.isStale(now))
    }

    @Test
    fun clientLocationUpdatePayload_serializesAndDeserializes() {
        val message = ClientLocationUpdatePayload(
            type = "LOCATION_UPDATE",
            version = 1,
            seq = 42L,
            latitude = 28.5303,
            longitude = 53.6008,
            accuracy = 3.5f,
            altitude = 1050.0,
            speed = 1.2f,
            bearing = 270f,
            timestamp = 1700000000000L
        )

        val jsonString = json.encodeToString(ClientLocationUpdatePayload.serializer(), message)
        assertTrue(jsonString.contains("\"type\":\"LOCATION_UPDATE\""))
        assertTrue(jsonString.contains("\"seq\":42"))

        val decoded = json.decodeFromString(ClientLocationUpdatePayload.serializer(), jsonString)
        assertEquals(message.seq, decoded.seq)
        assertEquals(message.latitude, decoded.latitude, 0.00001)
        assertEquals(message.timestamp, decoded.timestamp)
    }

    @Test
    fun serverLocationEnvelope_deserializesActiveSharers() {
        val rawJson = """
            {
              "type": "INITIAL_SYNC",
              "version": 1,
              "teamId": "team_alpha",
              "activeSharers": [
                {
                  "userId": "usr_99",
                  "callsign": "البرز",
                  "latitude": 28.55,
                  "longitude": 53.62,
                  "accuracy": 4.0,
                  "altitude": 1200.0,
                  "speed": 0.8,
                  "bearing": 45.0,
                  "timestamp": 1700000010000
                }
              ]
            }
        """.trimIndent()

        val decoded = json.decodeFromString(ServerLocationEnvelope.serializer(), rawJson)
        assertEquals("INITIAL_SYNC", decoded.type)
        assertEquals("team_alpha", decoded.teamId)
        assertNotNull(decoded.activeSharers)
        assertEquals(1, decoded.activeSharers?.size)
        assertEquals("usr_99", decoded.activeSharers?.first()?.userId)
        assertEquals("البرز", decoded.activeSharers?.first()?.callsign)
    }

    @Test
    fun serverLocationEnvelope_deserializesMemberDisconnected() {
        val rawJson = """
            {
              "type": "MEMBER_DISCONNECTED",
              "version": 1,
              "teamId": "team_alpha",
              "userId": "usr_99"
            }
        """.trimIndent()

        val decoded = json.decodeFromString(ServerLocationEnvelope.serializer(), rawJson)
        assertEquals("MEMBER_DISCONNECTED", decoded.type)
        assertEquals("usr_99", decoded.userId)
    }

    @Test
    fun clientSharingStoppedPayload_serializesProperly() {
        val stopMsg = ClientSharingStoppedPayload(
            type = "SHARING_STOPPED",
            version = 1,
            seq = 5L,
            timestamp = 1700000050000L
        )
        val jsonString = json.encodeToString(ClientSharingStoppedPayload.serializer(), stopMsg)
        assertTrue(jsonString.contains("\"type\":\"SHARING_STOPPED\""))
        assertTrue(jsonString.contains("\"seq\":5"))
    }
}
