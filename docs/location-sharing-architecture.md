# WayMate — Real-Time Consent-Based Location Sharing Architecture

## 1. Executive Summary

WayMate is an outdoor recreation, hiking, and tourism companion designed to facilitate real-time location sharing between consenting group members during trips.

This system guarantees:
- **Strict consent-based participation**: Location tracking is never started automatically (not on boot, not on login, not on team join).
- **Group boundary isolation**: Location updates are delivered exclusively to authenticated members of the same hiking team.
- **Continuous background operation**: An Android Foreground Service keeps location sharing alive when the screen is locked or the app is backgrounded, accompanied by an ongoing notification with an immediate Stop action.
- **Resource protection**: High-frequency coordinate streams are handled entirely in memory via Cloudflare Durable Objects without polluting or exhausting the D1 database.
- **Sovereign, de-Googled design**: The Android client uses standard platform `LocationManager` APIs (`GPS_PROVIDER`, `NETWORK_PROVIDER`) without Google Play Services dependencies.

---

## 2. System Architecture Overview

```
+---------------------------------------------------------------------------------+
|                               Android Client                                    |
|                                                                                 |
|  +--------------------+        +-------------------------+                      |
|  | LocationDataSource | -----> | RealLocationRepository  |                      |
|  |  (LocationManager) |        +-------------------------+                      |
|  +--------------------+                     |                                   |
|            ^                                v                                   |
|  +--------------------+        +-------------------------+                      |
|  | Foreground Service | <----- | LocationWebSocketClient |                      |
|  | (Notification Stop)|        +-------------------------+                      |
|  +--------------------+                     |                                   |
+---------------------------------------------|-----------------------------------+
                                              | Authenticated WSS
                                              | (JWT + team_id query params)
                                              v
+---------------------------------------------------------------------------------+
|                        Cloudflare Workers & Edge Tier                           |
|                                                                                 |
|      +-------------------------------------------------------------------+      |
|      |               GET /api/v1/ws/locations (HTTP Upgrade)              |      |
|      |               1. Verify Bearer JWT Token                          |      |
|      |               2. Check team membership in Cloudflare D1           |      |
|      +-------------------------------------------------------------------+      |
|                                        |                                        |
|                                        v                                        |
|      +-------------------------------------------------------------------+      |
|      |          LocationSessionDO (Cloudflare Durable Object)            |      |
|      |                 or In-Worker GroupLocationSession                 |      |
|      |                                                                   |      |
|      |   - In-memory WebSocket connection registry                       |      |
|      |   - High-frequency coordinate fan-out (Zero D1 writes)            |      |
|      |   - Coordinate boundary & schema validation                       |      |
|      |   - Per-client rate limiting (max 1 msg/sec)                      |      |
|      |   - Immediate member departure broadcast                          |      |
|      +-------------------------------------------------------------------+      |
|                     |                                    |                      |
+---------------------|------------------------------------|----------------------+
                      | Fan-out broadcast                  | Fan-out broadcast
                      v                                    v
            [Group Member A Device]              [Group Member B Device]
```

---

## 3. Android Client Architecture

### 3.1 Clean Architecture Organization
1. **Domain Layer**:
   - `UserLocation`: Immutable geodetic fix containing latitude, longitude, accuracy (meters), altitude, speed, bearing, and timestamp.
   - `GroupMemberLocation`: Position of a consenting peer, including stale calculation (`isStale(now)` > 60s threshold).
   - `LocationSharingStatus`: Discrete lifecycle states (`NOT_SHARING`, `REQUESTING_PERMISSION`, `STARTING`, `SHARING`, `RECONNECTING`, `GPS_DISABLED`, `PERMISSION_DENIED`, `NETWORK_UNAVAILABLE`, `STOPPING`, `STOPPED`, `ERROR`).
   - `LocationRepository`: Contract providing `startSharing(teamId)`, `stopSharing()`, `publishLocation(location)`, and `sharingState: StateFlow<LocationSharingState>`.
   - `StartLocationSharingUseCase`, `StopLocationSharingUseCase`, `ObserveLocationSharingStateUseCase`.

2. **Data Layer**:
   - `LocationDataSource`: Interface abstracting GPS coordinate acquisition.
   - `AndroidLocationDataSource`: Implementation querying `LocationManager` using `GPS_PROVIDER` (primary) and `NETWORK_PROVIDER` (fallback) with minTime 3000ms and minDistance 3m. Validates fixes before emitting.
   - `LocationWebSocketClient` / `RealLocationWebSocketClient`: OkHttp WebSocket manager with automatic reconnection (exponential backoff with 20% jitter), monotonic sequence counter, and keepalive ping heartbeats.
   - `RealLocationRepository`: Coordinates `LocationDataSource`, `LocationWebSocketClient`, and `SessionStorage`.

3. **Background Foreground Service**:
   - `LocationSharingService`: Subclass of Android `Service`.
   - Manifest declaration: `android:foregroundServiceType="location"`.
   - Service notification channel: `waymate_location_sharing` with `IMPORTANCE_LOW` (non-intrusive ongoing presence).
   - Ongoing notification displays the user's active sharing status and an explicit action button: **«توقف اشتراک‌گذاری»**.
   - `onStartCommand` returns `START_NOT_STICKY` to prevent zombie service resurrection upon process termination.
   - Stops immediately and releases all wake resources when the user taps Stop or logs out.

### 3.2 Platform & Operating System Limitations
- **OS Process Killing**: If the user force-stops the app in Android system settings or aggressive battery management kills the process, Android does not guarantee service survival. WayMate documents this limitation honestly and does not promise uninterrupted tracking after force-stop.
- **Android TargetSdk 37 Compliance**: `FOREGROUND_SERVICE_LOCATION` permission is declared and verified at runtime along with `ACCESS_FINE_LOCATION` and `POST_NOTIFICATIONS` (Android 13+).
- **Zero Automatic Restart**: No broadcast receivers for `BOOT_COMPLETED` or `MY_PACKAGE_REPLACED` are registered for location sharing. Consent is never assumed.

---

## 4. Real-Time Communication Protocol & Fan-Out

### 4.1 WebSocket Gateway Handshake
The client initiates a WebSocket connection:
```
GET /api/v1/ws/locations?token=<JWT>&team_id=<TEAM_ID> HTTP/1.1
Host: waymate.local
Upgrade: websocket
Connection: Upgrade
```
1. **Authentication**: Handshake extracts the `token` parameter, verifies signature via HMAC-SHA256, and checks token expiration.
2. **Authorization**: Handshake verifies in D1 that the user is an active member or leader of `team_id`. Unassigned or unauthorized users receive HTTP 403.
3. **Session Binding**: Connection is routed to `LocationSessionDO` (or in-worker `GroupLocationSession`) keyed by `team_id`.

### 4.2 Group Isolation & Durable Objects
- Each hiking group is partitioned into an isolated session namespace.
- Connections from Team A never receive, route, or inspect messages belonging to Team B.
- Upon connecting, a new client receives an `INITIAL_ROSTER` containing all currently active sharers in that group.
- Real-time updates from any active sharer are broadcast immediately to all other connected sockets in the group.
- When a user stops sharing or closes their connection, a `SHARING_STOPPED` or `MEMBER_STATE` envelope is broadcast to remove their marker from peers' maps.

---

## 5. UI & Map Canvas Presentation

### 5.1 Layer 7: Live Group Members
`TacticalMapCanvas` integrates live group member locations on top of Jahrom GIS road and waterway vectors:
- **Pulsing Halo Aura**: Drawn using Compose Canvas drawScope.
  - Active fresh fix (<= 60s): Vibrant emerald green aura (`#4CAF50`).
  - Stale fix (> 60s): Warning amber aura (`#FFB300`).
- **Directional Bearing Arrow**: When sensor bearing is available, a tactical directional pointer indicates the member's current heading.
- **Callsign Label**: The member's registered Persian callsign and distance/accuracy are rendered above the marker.
- **Selection & HUD Pill**: Tapping a member marker highlights them and displays a tactical HUD pill at the bottom of the map with altitude, speed, accuracy, and update age.

### 5.2 Control Card & Rationale Dialog
- `LocationSharingControlCard`: Displays current sharing state, GPS status, accuracy, and big tactile Start/Stop buttons in Persian RTL typography.
- `LocationPermissionRationaleDialog`: Displayed prior to requesting Android system permissions, transparently explaining to the user why location is needed and reaffirming user control.
