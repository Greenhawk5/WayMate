# WayMate

**WayMate** is a personal educational Android application designed for group hiking, outdoor tourism, and tactical team coordination. It provides offline-first trail mapping, Points of Interest (POI) management, operational status broadcasting, and real-time consent-based location sharing backed by a Cloudflare Workers edge architecture.

The project features a **Persian RTL user interface** designed for high visibility and field ergonomics, accompanied by **English technical documentation and architecture**.

---

## 1. System Architecture

WayMate is structured under Clean Architecture principles with clear separation of concerns, manual dependency injection, and zero Google Play Services lock-in.

```
+-----------------------------------------------------------------------------------+
|                            WayMate Android Client (:app)                          |
|                                                                                   |
|  Presentation:  Jetpack Compose + Material 3 Dark Tactical Theme + RTL Layout     |
|  ViewModel:     Architecture Components ViewModel + StateFlow UDF                 |
|  Domain:        Pure Kotlin Models, Repository Interfaces, Use Cases              |
|  Data / Local:  Android SQLite (WayMateDatabaseHelper) + Store-and-Forward Queue  |
|  Data / Remote: OkHttp 4 + Kotlinx Serialization (REST & Secure WebSockets)       |
|  Sensors:       Native Android LocationManager (GPS_PROVIDER & NETWORK_PROVIDER)  |
|  Services:      Foreground Service (FOREGROUND_SERVICE_TYPE_LOCATION)             |
|  Map Engine:    Custom Compose Canvas 2D Engine with Equirectangular Projection   |
+-----------------------------------------------------------------------------------+
                                         |
                       HTTPS REST / WSS WebSocket Upgrade
                       (Authorization: Bearer <JWT>)
                                         v
+-----------------------------------------------------------------------------------+
|                         Cloudflare Workers Edge Backend                           |
|                                                                                   |
|  Runtime:       Cloudflare Workers (V8 Isolate) + TypeScript + Hono Framework     |
|  Database:      Cloudflare D1 (Serverless Distributed SQLite)                     |
|  Real-Time:     Cloudflare Durable Objects (LocationSessionDO)                    |
|  Security:      HMAC-SHA256 JWT, Isolate Rate Limiting, RBAC & Tenant Isolation   |
+-----------------------------------------------------------------------------------+
```

---

## 2. Key Features

### 2.1 Tactical Map & Jahrom GIS Vector Overlays
- **Custom Compose Canvas Map**: High-performance offline 2D map engine rendering Jahrom GIS vector geometries (EPSG:4326), including 5,351 thoroughfares and 108 waterways.
- **Multi-Layer Sheet**: 7-layer toggleable visibility control (Base Map, Thoroughfares, Waterways, Grid, Route, Simulation, Teammates).
- **Navigation Controls**: Single-finger pan gestures and dedicated on-screen zoom buttons (`+` and `-`).
- **Deterministic Route Simulation**: Real-time trail simulation calculating Haversine distance, bearings, and configurable speeds (1x, 2x, 5x).

### 2.2 Points of Interest (POI) & Offline Buffering
- **Category-Aware POIs**: Start Point, Destination, Campsite, Water Source, Rest Area, and General POIs.
- **Offline-First Synchronization**: POIs created while disconnected are buffered in local SQLite and automatically synchronized to Cloudflare D1 upon reconnection.
- **Optimistic Concurrency**: Version tracking prevents conflicting edits (HTTP 409 resolution).

### 2.3 Real-Time Operational Location Sharing (Phase 7.2)
- **Least-Privilege Permissions**: Progressive rationale dialogs, coarse/fine location fallback, and settings navigation upon permanent denial.
- **Explicit Consent**: Strictly user-initiated sharing; no background tracking without opt-in.
- **Foreground Service Persistence**: Registered under Android 14+ `FOREGROUND_SERVICE_TYPE_LOCATION` with persistent notification and immediate "Stop" action.
- **Secure Edge Transport**: WebSocket upgrade handshake authenticated via `Authorization: Bearer <token>` HTTP header, completely isolating credentials from query strings.
- **Durable Object Fan-Out**: Real-time in-memory location broadcast within team boundaries using Cloudflare Durable Objects (`LocationSessionDO`), avoiding database write overhead.
- **Dynamic Teammate Rendering**: Real-time comrade markers on the map canvas with heading, telemetry detail HUD, and 60-second stale data warnings.

### 2.4 Tactical Communications & Status
- **Operational Status Dispatch**: Real-time status reporting (`NORMAL`, `PURSUING`, `ENGAGED`) with instant persistence and team broadcasting.
- **Priority Broadcast Commands**: Commander tactical orders with read receipts and acknowledgement tracking (`تأییدشده ✓`).

---

## 3. Technology Stack

### Android Application
- **Language**: Kotlin 2.2.10
- **UI Toolkit**: Jetpack Compose (BOM 2024.12.01) with Material 3
- **Architecture**: Clean Architecture (Presentation, Domain, Data, Core)
- **Dependency Injection**: Manual `AppContainer` DI
- **Local Persistence**: Android SQLite (`SQLiteOpenHelper`)
- **Networking**: OkHttp 4.12.0 & Kotlinx Serialization JSON 1.7.3
- **Sensors**: Android Native `LocationManager`
- **Build System**: Gradle 9.0 + Android Gradle Plugin (AGP) 9.4.1
- **Minimum SDK**: 26 (Android 8.0 Oreo)
- **Target / Compile SDK**: 37 (Android 16)

### Edge Backend
- **Platform**: Cloudflare Workers
- **Language**: TypeScript 5.7+
- **Framework**: Hono 4.6+
- **Persistent Storage**: Cloudflare D1 (SQLite)
- **Real-Time Coordination**: Cloudflare Durable Objects
- **Testing**: Vitest 3.0+

---

## 4. Getting Started & Development

### 4.1 Prerequisites
- **Java Development Kit (JDK)**: OpenJDK 17 or higher
- **Android Studio**: Ladybug / Meerkat or compatible command-line tools
- **Node.js**: Node.js 20+ and npm (for backend development and tests)
- **Hardware (Optional)**: Android physical device with USB debugging enabled

### 4.2 Building & Testing the Android App
From the project root directory:

```bash
# Run JVM unit test suite (203 tests)
.\gradlew.bat testDebugUnitTest

# Build debug APK
.\gradlew.bat assembleDebug

# Run Android Lint check (0 blocking errors)
.\gradlew.bat lintDebug

# Install debug APK to connected device
.\gradlew.bat installDebug
```

### 4.3 Building & Testing the Edge Backend
From the `backend/` directory:

```bash
cd backend

# Install dependencies
npm install

# Run backend unit & integration tests (107 tests)
npm test

# Run TypeScript type check
npx tsc --noEmit

# Test edge deployment packaging (dry-run, no deployment)
npx wrangler deploy --dry-run
```

---

## 5. Beta 1 Release (`0.14.0-beta.1`)

WayMate Beta 1 represents the first signed candidate release for field evaluation.

| Attribute | Details |
| :--- | :--- |
| **Version Name** | `0.14.0-beta.1` |
| **Version Code** | `2` |
| **Package ID** | `ir.alifaniani.waymate` |
| **Release Artifact** | `app/build/outputs/apk/release/app-release.apk` |
| **APK Size** | 9,796,126 bytes (9.34 MB) |
| **SHA-256 Checksum** | `79fa0d60a200cdb6afe2ac6f10abed247ec0c2fbec9380a2c77742bb48065b2d` |
| **Signing Key** | RSA 2048-bit (`waymate-beta.jks`, external storage) |
| **Signing Scheme** | APK Signature Scheme v2 (Verified) |
| **Primary Test Device** | Samsung Galaxy S21 FE 5G (`SM-G990E`, Android 16, API 36) |

Detailed release documentation is available in `docs/beta-release/`:
- [Release Notes](file:///E:/2.Things/Android-Projects/WayMate/docs/beta-release/release-notes.md)
- [Release Manifest](file:///E:/2.Things/Android-Projects/WayMate/docs/beta-release/release-manifest.md)
- [Installation Guide](file:///E:/2.Things/Android-Projects/WayMate/docs/beta-release/installation-guide.md)
- [Beta Testing Guide](file:///E:/2.Things/Android-Projects/WayMate/docs/beta-release/beta-testing-guide.md)
- [Known Issues & Bug Tracking](file:///E:/2.Things/Android-Projects/WayMate/docs/beta-release/known-issues.md)

---

## 6. Known Limitations & Scope Boundaries

In accordance with strict technical integrity guidelines, the following boundaries and limitations are documented:

1. **Touchscreen Multi-Finger Map Gestures**: Single-finger panning and on-screen zoom buttons (`+` and `-`) are fully verified on hardware. Two-finger rotation and pinch-to-zoom gestures on multi-touch screens remain unverified.
2. **Dual-Phone Live Exchange**: Real-time bidirectional location sharing has been verified between one physical Android smartphone (Samsung Galaxy S21 FE) and one automated Node.js streaming client over production Cloudflare Durable Objects. Simultaneous live testing between two separate physical Android phones remains pending availability of a second device.
3. **Preliminary Battery Measurement**: An initial 5-minute continuous live-tracking benchmark demonstrated low CPU overhead (<1%) and stable thermal regulation ($36.4^\circ\text{C}$). A full multi-hour expedition battery endurance benchmark will be conducted in subsequent field tests.
4. **Backend Dev Tooling Vulnerabilities**: `npm audit` reports 3 moderate vulnerabilities in development test runner packages (`vitest`, `@vitest/mocker`, `tinypool`). Runtime edge dependencies (`hono`, `@cloudflare/workers-types`) have zero vulnerabilities.

---

## 7. Security & Privacy Rules

- **Zero Hardcoded Secrets**: No credentials, API tokens, private keys, or keystores are committed to the repository.
- **Local Machine Config Excluded**: `local.properties`, `keystore.properties`, `key.properties`, and `*.jks` are strictly excluded via `.gitignore`.
- **Server-Authoritative Security**: Client UI permissions are advisory; Cloudflare Workers authoritatively enforce tenant isolation and role validation.
- **Synthetic Test Data Only**: All testing accounts (`commander_demo`, `patrol_lead_demo`) and trail coordinates use strictly synthetic test data.
- **Consent-Driven Geolocation**: Location tracking requires explicit user opt-in, is accompanied by a persistent foreground notification, and is terminated immediately upon user request.

---

## 8. License & Educational Context

WayMate is an educational Android engineering project developed for research, architectural study, and personal learning.
