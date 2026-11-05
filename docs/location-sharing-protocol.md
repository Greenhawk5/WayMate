# WayMate — Real-Time Location Sharing Protocol Specification

## 1. Overview & Protocol Versioning

This document specifies the wire protocol for real-time consent-based location sharing between Android clients and Cloudflare Workers over WebSockets.

- **Transport**: Secure WebSocket (`wss://`)
- **Format**: JSON UTF-8
- **Endpoint**: `/api/v1/ws/locations?team_id=<TEAM_ID>`
- **Authentication**: `Authorization: Bearer <JWT>` HTTP handshake header (URL query parameter `?token=<JWT>` supported as legacy fallback)
- **Protocol Version**: `1` (Integer)

All messages exchanged MUST include the `version: 1` field. Any message with an unsupported version will be rejected with an `ERROR` envelope.

---

## 2. Authorization & Identity Derivation

1. **Server-Authoritative Identity**:
   - The server **NEVER** trusts a client-supplied `userId`, `role`, or `callsign` in the WebSocket payload.
   - The user's identity is derived strictly from the verified JWT provided during the HTTP Upgrade handshake.
2. **Team Boundary Isolation**:
   - The `team_id` query parameter is validated against Cloudflare D1 team membership records.
   - Broadcasts are strictly isolated to the session of the specified `team_id`. Cross-group data leakage is impossible by design.

---

## 3. Client-to-Server Message Schemas

### 3.1 `LOCATION_UPDATE`
Published periodically by the client when active GPS fixes are acquired (recommended interval: 3–5 seconds).

```json
{
  "type": "LOCATION_UPDATE",
  "version": 1,
  "seq": 42,
  "latitude": 28.5303,
  "longitude": 53.6008,
  "accuracy": 4.5,
  "altitude": 1055.2,
  "speed": 1.2,
  "bearing": 185.0,
  "timestamp": 1700000000000
}
```

| Field | Type | Required | Description |
| :--- | :--- | :--- | :--- |
| `type` | String | Yes | Constant `"LOCATION_UPDATE"` |
| `version` | Number | Yes | Protocol schema version (`1`) |
| `seq` | Number | Yes | Client monotonic sequence number |
| `latitude` | Number | Yes | WGS-84 latitude (`-90.0` to `90.0`) |
| `longitude` | Number | Yes | WGS-84 longitude (`-180.0` to `180.0`) |
| `accuracy` | Number | Yes | Horizontal 1-sigma accuracy in meters (`>= 0.0`) |
| `altitude` | Number | No | Height above sea level in meters |
| `speed` | Number | No | Instantaneous speed over ground in m/s |
| `bearing` | Number | No | Direction of travel in degrees (`0.0` to `360.0`) |
| `timestamp` | Number | Yes | Sensor epoch timestamp in milliseconds |

### 3.2 `SHARING_STARTED`
Sent immediately when the user taps "Start Sharing".

```json
{
  "type": "SHARING_STARTED",
  "version": 1,
  "seq": 1,
  "timestamp": 1700000000000
}
```

### 3.3 `SHARING_STOPPED`
Sent immediately when the user taps "Stop Sharing" or when the Foreground Service is dismissed.

```json
{
  "type": "SHARING_STOPPED",
  "version": 1,
  "seq": 43,
  "timestamp": 1700000120000
}
```

### 3.4 `PING`
Sent every 20 seconds to keep the WebSocket connection alive and monitor round-trip latency.

```json
{
  "type": "PING",
  "version": 1,
  "timestamp": 1700000020000
}
```

---

## 4. Server-to-Client Message Schemas

### 4.1 `INITIAL_ROSTER`
Dispatched to a newly connected client immediately after successful handshake, reporting all currently active sharers in their group.

```json
{
  "type": "INITIAL_ROSTER",
  "version": 1,
  "teamId": "team_jahrom_alpha",
  "activeSharers": [
    {
      "userId": "usr_hiker_002",
      "callsign": "البرز",
      "latitude": 28.5312,
      "longitude": 53.6021,
      "accuracy": 3.8,
      "altitude": 1060.0,
      "speed": 0.9,
      "bearing": 120.0,
      "timestamp": 1700000001000
    }
  ],
  "serverTimestamp": 1700000002000
}
```

### 4.2 `LOCATION_UPDATE` (Broadcast)
Broadcasted to all other group members whenever a valid location update is received from an active sharer.

```json
{
  "type": "LOCATION_UPDATE",
  "version": 1,
  "userId": "usr_hiker_001",
  "callsign": "شاهین",
  "teamId": "team_jahrom_alpha",
  "latitude": 28.5303,
  "longitude": 53.6008,
  "accuracy": 4.5,
  "altitude": 1055.2,
  "speed": 1.2,
  "bearing": 185.0,
  "timestamp": 1700000000000,
  "serverTimestamp": 1700000000050
}
```

### 4.3 `SHARING_STOPPED` (Broadcast)
Broadcasted to group members when a user explicitly stops sharing. Receiving clients immediately remove the user's live marker from their map.

```json
{
  "type": "SHARING_STOPPED",
  "version": 1,
  "userId": "usr_hiker_001",
  "callsign": "شاهین",
  "teamId": "team_jahrom_alpha",
  "serverTimestamp": 1700000120050
}
```

### 4.4 `MEMBER_STATE` (Broadcast)
Broadcasted when a member disconnects or reconnects.

```json
{
  "type": "MEMBER_STATE",
  "version": 1,
  "userId": "usr_hiker_001",
  "callsign": "شاهین",
  "status": "DISCONNECTED",
  "isSharing": false,
  "serverTimestamp": 1700000130000
}
```

### 4.5 `PONG`
Responded immediately to client `PING` messages.

```json
{
  "type": "PONG",
  "version": 1,
  "timestamp": 1700000020000,
  "serverTimestamp": 1700000020015
}
```

### 4.6 `ERROR`
Returned if an incoming client message violates protocol constraints.

```json
{
  "type": "ERROR",
  "version": 1,
  "code": "INVALID_COORDINATES",
  "message": "Latitude must be between -90 and 90 degrees",
  "serverTimestamp": 1700000000100
}
```

---

## 5. Server-Side Validation Rules

1. **WGS-84 Coordinate Limits**:
   - `latitude` MUST satisfy `-90.0 <= lat <= 90.0`.
   - `longitude` MUST satisfy `-180.0 <= lon <= 180.0`.
   - Violations reject the message with code `INVALID_COORDINATES`.
2. **Horizontal Accuracy Check**:
   - `accuracy` MUST be non-negative (`accuracy >= 0.0`). Negative values are rejected.
3. **Timestamp Drift Window**:
   - Coordinates older than 120,000ms or more than 30,000ms in the future compared to the server's clock are rejected with code `OUTDATED_TIMESTAMP`.
4. **Rate Limiting**:
   - Each connected client is throttled to a maximum of 1 `LOCATION_UPDATE` per second.
   - Bursts exceeding this threshold are throttled with code `RATE_LIMITED`.
5. **Monotonic Sequence Enforcement**:
   - Out-of-order sequence updates from the same client are dropped to avoid jumping positions.
