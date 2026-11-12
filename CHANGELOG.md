# Changelog

All notable changes to the WayMate project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [Unreleased] (Candidate: [0.15.0-beta.1])

### Added
- **Interactive Onboarding Experience**:
  - Implemented 3-step interactive onboarding walkthrough (`OnboardingScreen.kt`) featuring custom illustrations, high-contrast skip action, and Persian RTL typography.
  - Added SharedPreferences-backed persistence layer (`OnboardingPreferences.kt`) with an in-memory test double (`InMemoryOnboardingStorage`).
  - Unit tests verifying first-launch detection, skip flow, and persistence (`OnboardingPreferencesTest.kt`).
- **Android 12+ System Splash Screen**:
  - Added modern splash theme (`Theme.WayMate.Splash` in `res/values-v31/themes.xml`) using `windowSplashScreenAnimatedIcon` and vector splash logo (`ic_splash_logo.png`).
- **Curved Circular Orbit Navigation Architecture**:
  - Replaced legacy single-scroll home layout with a 5-destination navigation system driven by a custom bottom bar (`CircularOrbitNavigationBar.kt`) and `HorizontalPager`.
  - Added dedicated primary destinations:
    - `LiveMapScreen.kt` (Tab 0: Tactical GIS Canvas, 7-layer control sheet, route simulation, teammate HUD).
    - `OverviewScreen.kt` (Tab 1: Operational readiness metrics, touch-scrubbing area chart, quick actions).
    - `TeamsScreen.kt` (Tab 2: Unit roster, vehicle/equipment allocations, member roles).
    - `OperationsScreen.kt` (Tab 3: Tactical broadcast messaging, priority orders, status dispatch).
    - `ProfileScreen.kt` (Tab 4: Operator callsign card, clearance level, routing to secondary screens).
  - Added secondary full-screen destinations reachable from Profile:
    - `SettingsScreen.kt`: Granular toggles for high-accuracy GPS, coordinate grids, sound/vibration alerts, and offline cache management.
    - `AboutScreen.kt`: Full architectural specification, version details, and privacy pledge.
  - Comprehensive unit test suite verifying tab synchronization, back-stack popping, and role permissions (`CircularNavigationArchitectureTest.kt`).
- **Tactical Design System & Component Library (`ui/designsystem/`)**:
  - `CircularOrbitNavigationBar.kt`: Curved orbit bar with animated active glow indicator.
  - `TacticalAreaChart.kt`: Responsive quadratic bezier area curve with touch-scrubbing time/percentage inspector.
  - `TacticalIcons.kt`: Dedicated tactical iconography library containing 15+ custom vectors.
  - `WayMateLoaders.kt`: Animated radar pulse, orbital rotation, and telemetry sweep indicators.
  - `WayMateComponents.kt`: Standardized tactical cards, metric chips, status badges, and empty-state cards.
- **15-Class Tactical Notification System (`service/WayMateNotificationManager.kt`)**:
  - Introduced 15 distinct operational notification classifications mapped to custom drawables (`ic_stat_notify_*.png`).
  - Defined 6 dedicated Android notification channels: Emergency (`CHANNEL_EMERGENCY`), Location Sharing (`CHANNEL_LOCATION_SHARING`), Tactical Orders (`CHANNEL_TACTICAL_ORDERS`), Navigation (`CHANNEL_NAVIGATION`), Squad (`CHANNEL_SQUAD`), and Sync Queue (`CHANNEL_SYNC`).
  - Unit test suite covering channel configuration, notification building, and intent binding (`WayMateNotificationManagerTest.kt`).
- **Store & Distribution Collateral (`distribution/store_assets/`)**:
  - Created complete Google Play Store and Cafe Bazaar promotional package: feature banner (`banner.png`), 5 screenshot cards (`card_1.png` - `card_5.png`), tablet overview graphic (`waymate_overview.png`), splash master artwork, and Open Graph card.
  - Authored comprehensive store listing documentation (`distribution/store_assets/store_listing.md`) in Persian and English.
- **Test Suite Expansion**:
  - Expanded Android unit test suite to **218 passing tests** (up from 203 in `0.14.0-beta.1`).

### Changed
- **Navigation & Gesture Ergonomics**:
  - Isolated map touch gestures: horizontal pager swiping is conditionally disabled on Tab 0 (`LiveMapScreen`) to prevent conflicts with single-finger map panning.
  - Unified system back-handler logic: pressing back from secondary screens returns to Profile; pressing back from any primary tab returns to Overview (Tab 1).
- **Design Tokens**:
  - Standardized color system with `WayMateMidnightDark` (#0A0E17), `WayMateCyan` (#00E5FF), and semantic tactical feedback tokens.

### Fixed
- **Readiness Chart Header Constraint Overlap & Truncation (BUG 1)**:
  - Re-architected layout constraints in `TacticalManagementSection.kt` and `TacticalAreaChart.kt`.
  - Assigned independent, weighted layout space to titles, subtitles, and the dynamic readiness badge, completely eliminating Persian text truncation and ellipsis artifacts (`نرخ برخط بودن و تله‌متری گرو...`).
- **Navigation Bar Glow Direction in RTL Layouts (BUG 2)**:
  - Corrected physical-vs-logical coordinate mapping in `CircularOrbitNavigationBar.kt`.
  - The radial cyan glow now accurately follows the selected icon's physical position during horizontal animations in RTL layouts instead of inverting direction.
- **Git Repository Hygiene & Ignore Configuration**:
  - Removed overbroad `/docs` ignore rule that was silently blocking new markdown documentation in `docs/superpowers/plans/`.
  - Removed accidental `app/src/main/assets/gis` ignore rule to protect offline Jahrom GIS vector geometries.
  - Added targeted ignore rules for `.kotlin/` compiler cache, `/current_screen.png`, and `/screen*.png`.
  - Safely purged 27 ephemeral root ADB screenshots (~9.5 MB) from the local working directory.

### Security & Hygiene
- Verified that zero temporary artifacts, logs, APKs, or secrets exist in Git history across all branches and commits.
- Kept Git repository database compact at 3.13 MiB.
- Ensured strict exclusion of local properties, keystores, and signing keys.

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
