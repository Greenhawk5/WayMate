# WayMate — Architectural Decision Log

## 1. Established Decisions (Binding Baseline)

The following architectural decisions are established by authoritative project guidelines ([AGENTS.md](file:///E:/2.Things/Android-Projects/WayMate/AGENTS.md), [README.md](file:///E:/2.Things/Android-Projects/WayMate/README.md), and [docs/architecture.md](file:///E:/2.Things/Android-Projects/WayMate/docs/architecture.md)) and serve as non-negotiable boundaries:

| Decision ID | Area | Established Baseline | Classification | Rationale & Constraint |
| :--- | :--- | :--- | :--- | :--- |
| **DEC-001** | **Client Platform** | Android Native (Kotlin + Jetpack Compose) | **Established Constraint** | Modern, responsive UI with declarative state management, strong type safety, and direct hardware API access (GPS/Bluetooth/Sensors). |
| **DEC-002** | **Backend Runtime** | Cloudflare Workers (TypeScript / JavaScript) | **Established Constraint** | Highly responsive serverless edge compute with low latency, zero server maintenance, and strong free-tier availability. |
| **DEC-003** | **Persistent Storage** | Cloudflare D1 (SQLite-compatible) | **Established Constraint** | Relational SQL database with sub-second query latency for structured telemetry, user rosters, incidents, and metadata. |
| **DEC-004** | **Storage Exclusion** | Cloudflare R2 is **strictly excluded** | **Established Constraint** | Constraint mandated by *AGENTS.md*. No raw binary object storage or large file hosting until baseline is stabilized and formally reassessed. |
| **DEC-005** | **Codebase Separation**| Backend and Android codebases remain decoupled | **Established Constraint** | Backend will reside in a separate directory/repository from the Android client to prevent dependency leaks and maintain clean CI/CD boundaries. |
| **DEC-006** | **Credential Security**| Zero hardcoded secrets in source control | **Established Constraint** | All API keys, database credentials, and signing certificates must be injected via runtime environment variables and secrets managers. |
| **DEC-007** | **Tactical Map Engine, Layers & Providers** | Native Compose Canvas Vector Renderer | **Established Constraint** | Native Compose Canvas retained for low-budget prototype (0 KB added dependencies, 100% offline). MapLibre migration deferred. Commercial SDKs, paid imagery, billing, API keys, and external tile dependencies strictly excluded. Standard street-map and satellite options honestly marked unconfigured. Operational GPS sharing, background tracking, real-user location display, and location uplinks deferred. Offline GIS overlay (5,459 features) and multi-layer architecture preserved. |

---

## 2. Architectural & Technical Decision Matrix

The following architectural choices have been evaluated and recorded:

---

### `AD-001`: Tactical Map Rendering Engine, Tile Provider & Offline Strategy
- **Context**: The app must render tactical spatial data (base map, coordinate grid, team locations, breadcrumbs, status markers) within a zero-budget, sovereign/intranet architecture without leaking operator telemetry or incurring vendor lock-in.
- **Source Reference**: *سند پروژه.md (3.الف)*, *docs/architecture.md (Section 8)*, *docs/requirements.md (REQ-F-004)*.
- **Classification**: **ADOPTED BASELINE (Pre-Phase-8 Finalization)**.

#### 1. Map SDK & Engine Comparison
| Evaluation Dimension | Option A: MapLibre Android SDK | Option B: osmdroid | Option C: Google Maps SDK | Option D: Pure Compose Tactical Engine (Phase 7 Prototype) |
| :--- | :--- | :--- | :--- | :--- |
| **License** | **Permissive (BSD-2-Clause / Apache 2.0)**. Free for commercial & educational use. | **LGPL-3.0**. Copyleft linking requires careful compliance and source disclosure audit. | **Proprietary**. Strictly tied to Google Play Services Terms of Service. | **In-house (Apache 2.0)**. 100% owned, zero third-party license restrictions. |
| **Project & Toolchain Compatibility** | Moderate. Requires C++ native libraries (`.so`). Needs verification for 16KB memory page size alignment on Android 15+ (AGP 9.4 / targetSdk 37). Requires `AndroidView` wrapper in Compose. | High. Pure Java/Kotlin bytecode. Runs on any Android 8.0+ (minSdk 26). Uses legacy Android View hierarchy. | Low for Sovereign Use. Hard dependency on Google Play Services; fails on de-Googled or air-gapped devices. | **Maximal**. Pure Kotlin & Jetpack Compose. Zero NDK/JNI overhead, zero ABI splits, 100% JVM unit-testable. |
| **Binary Size Overhead** | +15 to +22 MB APK overhead due to native binaries (`arm64-v8a`, `armeabi-v7a`, `x86_64`). | +1.5 MB APK overhead. | +2 to +4 MB (Play Services client). | **0 KB additional dependencies** (uses existing Compose foundation). |
| **Rendering Architecture** | Native OpenGL ES 2.0+ / Vulkan vector rendering; fluid 60fps rotation, dynamic pitch, sharp high-DPI typography. | Android Canvas raster tile blitting; text labels rotate clumsily, zoom is pixelated between discrete zoom levels. | Google Maps vector engine; smooth rotation and 3D buildings. | Compose Hardware-Accelerated Canvas; sub-millisecond drawing of coordinate grids and tactical symbology. |
| **Offline Vector Capability** | **Outstanding**. First-class support for `.mbtiles` and `.pmtiles` vector archives. | Weak. Raster-only offline tile caching (`.sqlite`, `.zip`). | **Prohibited**. Google TOS explicitly forbids pre-caching or offline tile extraction. | **Complete**. 100% offline-first. Never fails due to network outage or missing map servers. |

#### 2. Map Tile & Cartography Source Analysis & Legal Policies
A critical finding of Phase 7 evaluation is that **no open-source Map SDK bundles free map tiles or satellite imagery**. Map engines and data providers are completely decoupled:
1. **OpenStreetMap Public Standard Tile Server (`tile.openstreetmap.org`)**:
   - *Terms of Service*: Governed by the strict *OpenStreetMap Foundation (OSMF) Tile Usage Policy*.
   - *Restrictions*: "OpenStreetMap data is free for everyone to use. Our tile servers are not."
   - *Constraints*: Bulk downloading or pre-caching for offline use is **strictly forbidden**. Requires an explicit, unique `User-Agent`. Unannounced rate-limiting or IP bans are applied to heavy clients. No SLA or uptime guarantee.
   - *Verdict*: **Unsuitable for operational production or offline field patrols**, acceptable only for low-frequency development if policy is respected.
2. **Commercial Cloud Tile & Satellite APIs (Mapbox, MapTiler, Google Maps, Maxar)**:
   - *Terms*: Require commercial API keys, monthly active user (MAU) quotas, or per-request billing once free tiers expire. Mapbox SDK additionally bundles closed-source telemetry.
   - *Satellite Imagery*: High-resolution commercial imagery (0.3m–0.5m/pixel) costs $5–$25+ per 1000 views, strictly prohibits offline caching or storage on sovereign air-gapped servers, and requires continuous cloud connectivity.
   - *Verdict*: **Rejected for MVP**. Violates the project's non-negotiable **zero-budget constraint** (*سند پروژه.md Section 2*) and sovereign intranet data rules.
3. **Public Satellite Imagery (Copernicus Sentinel-2 & USGS Landsat)**:
   - *Copernicus Sentinel-2*:
     - *Resolution*: 10 meters per pixel for visible RGB bands (B02, B03, B04).
     - *Suitability*: Adequate for macro-terrain, mountain ranges, regional water bodies, and vegetation; **inadequate for urban street-level tactics or identifying individual buildings and alleys** (a typical 6m alley is smaller than a single pixel).
     - *License*: Creative Commons Attribution 4.0 (CC BY-SA 4.0) — completely free for sovereign/offline hosting.
     - *Infrastructure Requirement*: Requires downloading large multi-spectral SAFE/GeoTIFF granules, reprojection, cloud-masking, and slicing into an offline raster MBTiles pyramid.
   - *USGS Landsat 8/9*:
     - *Resolution*: 15 meters pan-sharpened, 30 meters multi-spectral. Too coarse for city navigation.
     - *Verdict*: Public satellite imagery is useful for regional topography but cannot replace street-level cartography.
4. **Self-Hosted Vector Tiles & Offline Packages (`.mbtiles` / `.pmtiles`)**:
   - *Terms*: OpenMapTiles / Protomaps schema built once from OpenStreetMap extracts and stored locally on the device or served from a sovereign local edge proxy.
   - *Storage Footprint*: Vector format compresses an entire province or metropolitan area into **20–50 MB**, compared to multi-gigabyte raster pyramids.
   - *Verdict*: **The only legally, financially, and operationally viable long-term strategy for WayMate**.

#### 3. Base Map Engine Comparison & Trade-Offs

| Option | Rendering Engine | Offline Capability | APK Size Impact | Complexity & Tooling | Status in WayMate |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **Option 1: Pure Compose Hardware Canvas** | Android Hardware-Accelerated 2D Canvas | **100% Offline** (Zero network calls, zero tile servers) | **0 KB added libraries** (Assets: ~0.5 MB compressed) | Low. Viewport culling, direct polyline drawing, 100% JVM testable. | **Active & Functional (Phase 7 Baseline)** |
| **Option 2: MapLibre Native Android + Offline MBTiles** | OpenGL ES / Vulkan vector tile renderer | **100% Offline** (Packaged `.mbtiles` or `.pmtiles`) | **+18 to +22 MB** (C++ native `.so` binaries, ABI splits) | Medium. Requires font glyphs (PBF), sprite sheets, and JSON style sheets. | **Recommended Target for Phase 8** |
| **Option 3: Web-Based Leaflet / OpenLayers in WebView** | Chromium WebKit canvas | Poor offline without local HTTP server | +0 KB (uses system WebView) | High battery consumption, clunky touch gesture responsiveness. | **Rejected** |

#### 4. Recommendation & Phased Execution Strategy
- **Phase 7 Safe Map Milestone (Current Verified Baseline)**:
  - Operate with the pure **Compose Hardware Canvas** vector engine (`OFFLINE_TACTICAL`).
  - Load all 5,459 audited Jahrom vector features (5,351 roads + 108 waterways) asynchronously from local assets.
  - Provide independent layer controls (roads, waterways, coordinate grid, synthetic units).
  - Explicitly mark Satellite and Standard Street tile servers as **unconfigured/disabled** until tile hosting and imagery datasets are provisioned.
- **Phase 8 (Production Multi-Province Cartography)**:
  - Project Owner decision needed: Provision provincial `.mbtiles` package and migrate to MapLibre Native Android SDK for GPU-accelerated labels, text shields, and 3D terrain.
- **Status**: **ADOPTED BASELINE — Native Compose Canvas retained for low-budget prototype; MapLibre migration deferred to Phase 8+ post-MVP evaluation**.

---

### `AD-002`: Real-Time Communication & Location Ingestion Transport
- **Context**: Field units transmit coordinates/status updates; Commanders need to observe team status and tactical orders within a few seconds without violating the zero-budget constraint or exceeding Cloudflare free tier limits.
- **Source Reference**: *سند پروژه.md (3.ج)*, *docs/architecture.md (Section 5)*, *todo.md (مرحله ۸)*.
- **Classification**: **ADOPTED BASELINE (Phase 8 Implementation & Verification)**.
- **Options Evaluated**:
  1. **Option A: Short-Interval REST Polling (5s Baseline with Jitter, Backoff & Lifecycle Awareness)**
     - *Pros*: Simplest architecture, 100% stateless Cloudflare Workers, zero persistent connection state, fully covered by free-tier Workers + D1, robust over flaky national networks.
     - *Quantitative Quota Analysis*: For 5 field clients active during a typical 2-hour tactical exercise with 5-second polling:
       - Total HTTP requests: `5 units × (7200s / 5s) = 7,200 requests`.
       - Cloudflare Free Tier Daily Request Quota: `100,000 requests/day`.
       - Consumption: Only **7.2%** of the daily free limit.
       - D1 Read Quota: `GET /api/v1/telemetry/live` inspects active units (~10 rows per query). `7,200 × 10 = 72,000 rows read`, which is **1.44%** of Cloudflare's 5,000,000 daily row read quota.
       - Battery and Data Conservation: Pauses immediately when app enters background (`LifecycleEventObserver.ON_STOP`) and resumes on foreground (`ON_START`) with immediate update.
       - Resiliency: Linear jitter (±15%) prevents thundering herd; exponential backoff (up to 30s) prevents resource exhaustion during server outage.
     - *Cons*: Modest latency (2–5 seconds behind real-time), acceptable for educational tactical exercises.
  2. **Option B: Cloudflare Durable Objects + WebSockets**
     - *Pros*: Sub-second latency, bidirectional push.
     - *Cons*: Durable Objects require a paid Cloudflare Workers plan ($5/mo minimum), violating the non-negotiable "zero budget" requirement (*سند پروژه.md Section 2*). Added state coordination complexity.
  3. **Option C: Server-Sent Events (SSE) via Workers**
     - *Pros*: Lightweight one-way push to Commander.
     - *Cons*: Edge runtime connection duration limits on free-tier Workers; reconnection logic over unstable mobile intranet can be fragile.
- **Adoption & Verification**: **Option A (Short-Interval REST Polling via `SafePoller`)** adopted and implemented for Phase 8. Verified via 89 Android unit tests, 37 backend Vitest tests, and running on Samsung S21 FE hardware.
- **Status**: **ADOPTED & VERIFIED IN PHASE 8**.

---

### `AD-003`: Commander Console Interface Architecture
- **Context**: The Commander role requires an operational situational map, team roster, and incident alert center.
- **Source Reference**: *سند پروژه.md (3.ب & 5)*, *قابلیت اجرایی شدن پروژه.md (بخش ۱)*.
- **Classification**: **Proposed Recommendation (Awaiting Approval)**.
- **Options Evaluated**:
  1. **Option A: Dual-Role Android Application (Phone & Tablet UI)**
     - *Pros*: Single codebase (Kotlin/Compose), operates completely offline when connected to local server, shares all data models and map components.
     - *Cons*: Commander must use an Android device; larger screens (desktop monitors) require emulator or Android-x86.
  2. **Option B: Web-Based Single Page Application (SPA on Cloudflare Pages / Workers)**
     - *Pros*: Commander can use any PC, laptop, or operations room desktop browser without installing software.
     - *Cons*: Requires maintaining a second frontend codebase (HTML/JS/Vue/React) alongside the Android app.
  3. **Option C: Unified Android App with Responsive Role-Based Navigation**
     - *Pros*: Android app adapts between "Field Mode" (simplified tactical FAB) and "Commander Mode" (multi-unit map drawer) based on login role.
- **Recommendation**: **Option C (Unified Android App with Role-Based Navigation)** for the MVP. It keeps the project focused on a single maintainable codebase. A dedicated Web Command Console can be introduced as a separate project in a later phase.
- **Status**: **PROPOSED — Awaiting Project Owner Approval**.

---

### `AD-004`: Mobile Local Database Engine & Offline Buffering
- **Context**: The app must buffer tactical messages, status updates, team mutations, and server caches during network disconnections (*سند پروژه.md 3.ج*, *todo.md مرحله ۱۰*).
- **Source Reference**: *سند پروژه.md (3.ج & 3.هـ)*, *todo.md (مرحله ۱۰)*, *docs/architecture.md*.
- **Classification**: **ADOPTED BASELINE (Phase 10 Implementation & Verification)**.
- **Options Evaluated**:
  1. **Option A: Android Framework SQLite (`SQLiteOpenHelper`) with `OfflineStorage` Interface**
     - *Pros*: Zero external dependencies, zero KSP/compiler plugin overhead with Kotlin 2.2.10 and AGP 9.4.1, 0 KB APK overhead, full SQLite ACID transactions and indexing. Pure Kotlin interface abstraction enables pure JVM in-memory test doubles (`InMemoryOfflineStorage`) for sub-second unit tests without Robolectric or emulator overhead.
     - *Cons*: Requires handwritten SQL queries and cursor mapping.
  2. **Option B: Android Room (SQLite ORM)**
     - *Pros*: Compile-time query verification, automatic entity mapping.
     - *Cons*: Requires Google KSP Gradle plugin, adds potential toolchain friction with preview/cutting-edge Kotlin/AGP versions, adds APK size.
  3. **Option C: SQLCipher (Full Database Encryption at Rest)**
     - *Pros*: Transparent hardware-level encryption.
     - *Cons*: High APK size overhead (+10MB), C++ native binaries, unnecessary complexity for synthetic tactical prototype data when session secrets are already hardware-protected via Android Keystore (`KeystoreSessionStorage`).
- **Adoption & Verification**: **Option A (Android Framework SQLite + `OfflineStorage` interface)** adopted. Implemented via `WayMateDatabaseHelper`, `SqliteOfflineStorage`, `InMemoryOfflineStorage`, and `OfflineSyncManager`. Verified via 21 offline-focused unit tests and integration with repositories and UI state.
- **Status**: **ADOPTED & VERIFIED IN PHASE 10**.

---

### `AD-005`: Incident Report Attachments under R2 Exclusion
- **Context**: *سند پروژه.md (4.ج)* mentions optional photo attachments in incident reports, but *AGENTS.md* explicitly forbids introducing Cloudflare R2, and D1 is not designed for BLOB storage.
- **Source Reference**: *سند پروژه.md (4.ج)*, *AGENTS.md*, *docs/architecture.md (Section 5)*.
- **Classification**: **Proposed Recommendation (Awaiting Approval)**.
- **Options Evaluated**:
  1. **Option A: Defer Image Attachments to Phase 3**
     - *Pros*: Adheres 100% strictly to *AGENTS.md* and *docs/architecture.md*. Incident reports in MVP contain rich text, categories, timestamps, and GPS coordinates without media overhead.
     - *Cons*: Field operators cannot upload photos in MVP.
  2. **Option B: Highly Compressed Inline Base64 Thumbnail in D1**
     - *Pros*: Allows small thumbnail (e.g. < 50 KB, 320x240 JPEG) in D1.
     - *Cons*: Drastically bloats D1 database storage, degrades query performance, bad database engineering practice.
- **Recommendation**: **Option A (Strict Deferral of Image Attachments)**. Keep MVP incident reporting strictly structured text and coordinates. Reassess image storage (e.g., Cloudflare Images or authorized object storage) in Phase 3.
- **Status**: **PROPOSED — Awaiting Project Owner Approval**.

---

### `AD-006`: Peer-to-Peer / Local Mesh Relay Scope Strategy
- **Context**: *سند پروژه.md (Section 2)* proposes P2P relay for message daisy-chaining in dead zones.
- **Source Reference**: *سند پروژه.md (2)*, *قابلیت اجرایی شدن پروژه.md (بخش ۱)*.
- **Classification**: **Proposed Recommendation (Awaiting Approval)**.
- **Options Evaluated**:
  1. **Option A: Formal Deferral to Phase 3 (Post-MVP)**
     - *Pros*: Endorsed strongly by *قابلیت اجرایی شدن پروژه.md*. Prevents project failure from tackling mesh routing protocols (Wi-Fi Direct/BLE), high battery drain, and fragmented Android hardware radios simultaneously.
     - *Cons*: In network dead zones, units rely exclusively on Store-and-Forward (data syncs when any unit returns to coverage) rather than direct hop-by-hop mesh.
  2. **Option B: Attempt Wi-Fi Direct Mesh in MVP**
     - *Pros*: Matches source document vision early.
     - *Cons*: Extreme technical risk; easily delays MVP by 3–6 months.
- **Recommendation**: **Option A (Formal Deferral to Phase 3)**. Rely on reliable Store-and-Forward (`MVP-11`) for offline tolerance during the MVP.
- **Status**: **PROPOSED — Awaiting Project Owner Approval**.

---

### `AD-007`: Background Location Tracking & Android 14+ Compliance
- **Context**: Android 14 (API 34) and Android 15/16 enforce strict restrictions on background location access, requiring specific foreground service types and runtime permissions.
- **Source Reference**: *AGENTS.md*, *app/build.gradle.kts*.
- **Classification**: **Proposed Recommendation (Awaiting Approval)**.
- **Options Evaluated**:
  1. **Option A: Foreground Service with `foregroundServiceType="location"`**
     - *Pros*: Fully compliant with Android 14+ platform policies. Displays persistent user notification ensuring transparency (*AGENTS.md* rule: location sharing visible to user). Prevents OS process termination.
     - *Cons*: User sees permanent notification in status bar while tracking is active (which is actually a feature for tactical transparency).
  2. **Option B: WorkManager Periodic Worker**
     - *Pros*: Standard background job scheduler.
     - *Cons*: Minimum interval is 15 minutes (unusable for tactical tracking requiring 15–30 second updates), subject to OS battery optimization delays.
- **Recommendation**: **Option A (Foreground Service with Ongoing Notification)**. It satisfies both real-time operational tracking requirements and Android platform background security policies.
- **Status**: **PROPOSED — Awaiting Project Owner Approval**.

---

### `AD-008`: Dependency Injection Framework Strategy
- **Context**: The app needs dependency management between UI ViewModels, Repositories, Data Sources, and System Services.
- **Source Reference**: *Phase 2A Architecture Analysis*.
- **Classification**: **Proposed Recommendation (Awaiting Approval)**.
- **Options Evaluated**:
  1. **Option A: Manual Constructor Injection via an `AppContainer`**
     - *Pros*: Zero external dependencies, pure Kotlin, instant Gradle build times, transparent debugging with zero code generation / reflection, completely simple for educational development and test fakes.
     - *Cons*: Requires manually wiring dependencies in `AppContainer` and passing factories to ViewModels.
  2. **Option B: Koin (Lightweight Service Locator / DI)**
     - *Pros*: Pure Kotlin DSL, no annotation processor or code generation, easy ViewModel injection.
     - *Cons*: Runtime resolution (errors caught at runtime rather than compile-time), adds an external third-party library dependency.
  3. **Option C: Dagger Hilt**
     - *Pros*: Google's official Android recommendation, compile-time graph verification.
     - *Cons*: Heavy annotation processing (KSP/kapt), noticeably increases Gradle build time, generates substantial boilerplate code, steep learning curve and brittle setup for a small MVP.
- **Recommendation**: **Option A (Manual `AppContainer` Injection)**. For an educational project with a focused MVP scope, manual constructor injection provides complete transparency, zero build overhead, and maximum testability without third-party dependencies. Can easily transition to Koin or Hilt if the graph grows complex in Phase 10+.
- **Status**: **PROPOSED — Awaiting Project Owner Approval**.

---

### `AD-009`: Android HTTP Networking Engine
- **Context**: The Android client must communicate with the Cloudflare Workers REST API over HTTPS/TLS 1.3.
- **Source Reference**: *Phase 2A Architecture Analysis*.
- **Classification**: **Proposed Recommendation (Awaiting Approval)**.
- **Options Evaluated**:
  1. **Option A: Retrofit + OkHttp**
     - *Pros*: Industry standard for Android, rock-solid connection pooling, mature interceptor chain (for automatic JWT injection and logging), excellent error handling, seamless converter integration.
     - *Cons*: Java/Android specific (not multiplatform, though WayMate is native Android).
  2. **Option B: Ktor Client (Android/CIO Engine)**
     - *Pros*: 100% pure Kotlin coroutine-first architecture, lightweight, Kotlin Multiplatform ready.
     - *Cons*: Slightly less familiar interceptor model for Android-only developers, occasional breaking API changes across major Ktor releases.
- **Recommendation**: **Option A (Retrofit + OkHttp)**. It is the most stable, extensively documented, and reliable HTTP client stack for Android native applications.
- **Status**: **PROPOSED — Awaiting Project Owner Approval**.

---

### `AD-010`: JSON Serialization Framework
- **Context**: Transforming network DTOs to/from JSON payloads.
- **Source Reference**: *Phase 2A Architecture Analysis*.
- **Classification**: **Proposed Recommendation (Awaiting Approval)**.
- **Options Evaluated**:
  1. **Option A: Kotlinx Serialization (`kotlinx-serialization-json`)**
     - *Pros*: Official Jetpack Compose and Kotlin library, compile-time code generation via Kotlin compiler plugin (zero runtime reflection), high performance, tiny footprint.
     - *Cons*: Requires Kotlin serialization compiler plugin in Gradle.
  2. **Option B: Square Moshi**
     - *Pros*: Excellent Android JSON parser with code-gen support via KSP.
     - *Cons*: Requires separate KSP plugin configuration.
- **Recommendation**: **Option A (Kotlinx Serialization)**. Aligns natively with Kotlin 2.x and Compose without adding extra annotation processors.
- **Status**: **PROPOSED — Awaiting Project Owner Approval**.

---

### `AD-011`: Unit and Integration Testing Strategy
- **Context**: Validating ViewModels, Use Cases, Repositories, and offline sync logic.
- **Source Reference**: *AGENTS.md*, *Phase 2A Architecture Analysis*.
- **Classification**: **Proposed Recommendation (Awaiting Approval)**.
- **Options Evaluated**:
  1. **Option A: In-Memory Fake Repositories + Local JUnit 4 Tests**
     - *Pros*: Tests are fast, deterministic, and readable. Business logic is verified against realistic in-memory state rather than complex mock setups (`whenever/verify`). Refactoring implementations doesn't break over-mocked tests.
     - *Cons*: Requires writing lightweight fake classes (`FakeAuthRepository`, `FakeTelemetryRepository`).
  2. **Option B: Heavy Mocking Frameworks (MockK / Mockito)**
     - *Pros*: Quick to mock individual calls on the fly.
     - *Cons*: Brittle tests tightly coupled to internal implementation details; slower test suite startup; mocking coroutines and flows can be error-prone.
- **Recommendation**: **Option A (In-Memory Fake Repositories)** for ViewModels and Domain Use Cases, combined with **MockWebServer** for testing raw API parsing and **in-memory Room** for DAO testing.
- **Status**: **PROPOSED — Awaiting Project Owner Approval**.

---

## 3. Decision Status Summary

### 3.1 Adopted Architectural Baselines (Phases 0–8 Verified)

| Decision Reference | Area | Adopted Option | Implementation & Verification Status |
| :--- | :--- | :--- | :--- |
| **`DEC-001`** | Client Platform | Android Native (Kotlin + Compose) | Verified in Phases 0–8 (89 unit tests, APK 14MB). |
| **`DEC-002`** | Backend Runtime | Cloudflare Workers (TypeScript) | Verified in Phases 5 & 8 (37/37 Vitest tests, tsc 0 errors). |
| **`DEC-003`** | Storage | Cloudflare D1 (SQLite-compatible) | Verified with local D1 migrations and seed data. |
| **`DEC-004`** | Storage Boundary | Cloudflare R2 strictly excluded | Preserved across all phases. |
| **`DEC-005`** | Separation | Decoupled Android & Backend | Preserved in repository layout (`app/` vs `backend/`). |
| **`DEC-006`** | Secrets | Zero hardcoded secrets in Git | Enforced via `.env` / runtime injection. |
| **`DEC-007` / `AD-001`** | Map Engine & Overlays | Native Compose Canvas Vector Renderer | Audited Jahrom 5,459 features loaded in assets. Multi-layer controls active. Street/satellite unconfigured. |
| **`AD-002`** | Communications Transport | Safe REST Polling (5s Baseline + Jitter + Backoff) | Implemented via `SafePoller`, `GET /api/v1/telemetry/live`, `POST/GET /api/v1/messages`. Verified in Phase 8. |
| **`AD-008`** | Dependency Injection | Manual `AppContainer` Injection | Active in `WayMateApp` / `MainActivity`. |
| **`AD-009`** | HTTP Networking | Retrofit + OkHttp Interceptors | Active in `ir.alifaniani.waymate.data.network`. |
| **`AD-010`** | JSON Serialization | Kotlinx Serialization | Active in DTOs, network mappers, and GIS parser. |
| **`AD-011`** | Testing Strategy | In-Memory Fakes + JUnit 4 + Vitest | Active across client (89 tests) and server (37 tests) suites. |

### 3.2 Pending Decisions Requiring Project Owner Sign-Off (Phase 9 and Beyond)

| Decision Reference | Recommended Option | Target Phase | Context & Next Action |
| :--- | :--- | :--- | :--- |
| **`AD-003` (Commander UI)** | Unified Android Client (Role-Adapted) | **Phase 9 (Admin/Roles)** | Avoids maintaining separate web console in MVP. |
| **`AD-004` (Offline DB)** | Room SQLite (Plaintext MVP -> SQLCipher Phase 2) | **Phase 10 (Offline Cache)** | Validates store-and-forward queueing before native crypto. |
| **`AD-005` (Attachments)** | Defer Photos to Phase 3 | **Phase 11 (Advanced)** | Strictly adheres to R2 exclusion in AGENTS.md. |
| **`AD-006` (P2P Mesh)** | Defer to Phase 3 | **Phase 11 (Advanced)** | Excludes Wi-Fi Direct/BLE mesh complexities from MVP. |
| **`AD-007` (Background Location)**| Foreground Service (`type="location"`) | **Phase 7.2 (Deferred)** | Android 14+ compliant ongoing notification. Awaiting explicit opt-in privacy workflow. |
