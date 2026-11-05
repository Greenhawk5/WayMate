# Changelog

All notable changes to the WayMate project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [Unreleased]

### Planned
- Physical dual-Android-phone live location exchange verification (pending second hardware phone).
- Physical touchscreen multi-finger pinch-to-zoom and two-finger rotation gesture calibration.
- Multi-hour continuous battery and thermal field endurance benchmarks.
- Upgrade backend devDependencies (`vitest`) to resolve 3 moderate test runner vulnerabilities.
- Phase 15: Final project verification, architecture review, and handover.

---

## [0.14.0-beta.1] - 2026-11-06

### Added
- **Phase 7.2 (Operational Geolocation & Real-Time Tracking)**:
  - Native Android `LocationManager` integration supporting `GPS_PROVIDER` and `NETWORK_PROVIDER` without Google Play Services dependencies.
  - Progressive permission handling with pre-permission rationale dialogs, coarse-only fallback, and settings navigation upon permanent denial.
  - Android 14+ `FOREGROUND_SERVICE_TYPE_LOCATION` compliance with `LocationSharingService`, persistent ongoing notification, and immediate "Stop" action.
  - Real-time WebSocket streaming client (`LocationWebSocketClient`) with automatic reconnection and exponential backoff.
  - Secure WebSocket handshake authentication via `Authorization: Bearer <token>` HTTP upgrade headers.
  - Real-time teammate rendering on `TacticalMapCanvas` with direction-facing avatars, emerald halo indicators, and 60-second stale data warnings.
  - Interactive teammate telemetry detail card (callsign, speed, altitude, accuracy, coordinates).
  - Cross-group tenant isolation enforced at the Cloudflare edge, rejecting unauthorized WebSocket handshakes with HTTP 403 Forbidden.
- **Phase 14 (Beta Release Preparation & Packaging)**:
  - Dedicated external RSA 2048-bit release keystore (`waymate-beta.jks`) and APK Signature Scheme v2 signing.
  - Complete beta documentation suite in `docs/beta-release/` (Manifest, Notes, Installation Guide, Testing Guide, Known Issues).
- **Phase 13 (Testing & Quality Assurance)**:
  - Android test suite expanded to 203 passing JVM unit tests.
  - Backend test suite expanded to 107 passing Vitest tests.
  - Zero blocking Android lint errors achieved across debug build.
- **Phase 12 (Security & Privacy Hardening)**:
  - Cryptographic JWT validation with algorithm pinning (`HS256`).
  - In-memory edge login rate limiter middleware with sliding window.
  - Request body size limit middleware (100 KB max).
  - Session cleanup and credential purging in Android Keystore on logout.
  - Backup rules configuration excluding databases and sensitive preferences from cloud backups.
- **Phase 11 (Points of Interest & Synchronization)**:
  - Categorized POI management (Start, Destination, Campsite, Water Source, Rest Area, General).
  - Local SQLite persistence and transactional store-and-forward queueing (`offline_queue`).
  - Cloudflare D1 table migration `0003_points_of_interest.sql`.
  - Optimistic concurrency control (HTTP 409 conflict detection).
- **Phases 6–10 (Tactical Map, Groups & Communications)**:
  - Custom Jetpack Compose Canvas 2D map engine rendering Jahrom GIS geometries (5,351 thoroughfares, 108 waterways).
  - Deterministic hiking route simulation engine with Haversine distance, bearings, and multi-speed playback (1x, 2x, 5x).
  - 7-layer toggleable map visibility bottom sheet.
  - Group creation, join by code, and member list management.
  - Operational status reporting (`NORMAL`, `PURSUING`, `ENGAGED`) and priority broadcast commands with read receipts.

### Changed
- **Architecture**:
  - Replaced proposed REST polling with real-time WebSockets over Cloudflare Durable Objects (`LocationSessionDO`).
  - Updated `MainActivity.kt` to dynamically bind active `teamId` when launching `LocationSharingService`.
  - Streamlined `LocationSharingControlCard.kt` to give intrinsic width to telemetry badges, preventing Persian clipping in RTL rows.
- **Packaging & Build**:
  - Migrated Gradle configuration to `versionName = "0.14.0-beta.1"` and `versionCode = 2`.
  - Stabilized release packaging rules to avoid experimental `packageScope` R8 optimization incompatibilities.

### Fixed
- **UI & Persian Typography**:
  - Resolved 1-character vertical text stacking by replacing unconstrained min-width constraints with flexible RTL layout wrappers.
  - Fixed gesture conflicts between canvas drag gestures and vertical parent scroll containers.
- **Lifecycle & Memory**:
  - Resolved Foreground Service lifecycle leaks, ensuring clean destruction and UI state resets when location sharing is stopped.
  - Removed Android framework `android.util.Log` references from `RealLocationRepository.kt` to ensure clean execution in pure JVM test environments.
- **Edge Routing**:
  - Handled WebSocket upgrade authorization header parsing robustly across Edge runtimes.

### Security
- Excluded sensitive keystore, environment, and property files from Git tracking via `.gitignore`.
- Removed `todo.md` from Git repository index while preserving local workspace copy.
- Enforced server-authoritative role verification and tenant isolation in Cloudflare Workers.
- Protected user privacy by purging location cache immediately upon receiving `SHARING_STOPPED`.

### Testing
- **Android Unit Tests**: 203 passing tests (`.\gradlew.bat testDebugUnitTest`).
- **Backend Tests**: 107 passing tests across 12 suites (`npm test`).
- **TypeScript Typecheck**: 0 errors (`npx tsc --noEmit`).
- **Android Lint**: 0 blocking errors, 33 non-blocking warnings (`.\gradlew.bat lintDebug`).
- **Physical Device**: Verified on Samsung Galaxy S21 FE 5G (Android 16 API 36).

### Documentation
- Updated `README.md` with complete architectural blueprints, feature descriptions, setup instructions, and beta release metadata.
- Updated `docs/architecture.md` resolving Phase 2A open proposals into approved, verified architectural decisions.
- Created `docs/phase-7.2-operational-geolocation.md` detailing the complete implementation and verification of real-time location sharing.
- Updated `docs/location-sharing-protocol.md` documenting `Authorization: Bearer <token>` header authentication.
- Maintained beta testing suite in `docs/beta-release/`.

