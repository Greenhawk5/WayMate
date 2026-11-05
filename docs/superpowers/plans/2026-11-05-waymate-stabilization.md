# WayMate Comprehensive UI/UX, Functionality & Real-Device Stabilization Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Stabilize the WayMate Android application by fixing root causes of crashes (TeamMember serialization), Persian 1-character vertical text wrapping, map viewport clipping, gesture interaction (pinch-to-zoom, rotation), group/member workflows, operational status persistence, and verifying on the physically connected SM-G990E device.

**Architecture:** Clean Architecture with manual dependency injection (`AppContainer`), SQLite local offline persistence, Compose UI with Material 3 Dark Tactical theme, OkHttp/Retrofit networking, and Compose Canvas custom map engine.

**Tech Stack:** Kotlin, Jetpack Compose, Kotlinx Serialization, Android SQLite (`SQLiteOpenHelper`), OkHttp, Retrofit, Gradle Kotlin DSL.

**Spec:** User request in conversation and architecture guidelines in `AGENTS.md`.

## Global Constraints

- Do not commit or push to Git; all work remains local.
- Do not modify Cloudflare Workers backend or run remote D1 migrations.
- Preserve Clean Architecture and module boundaries.
- No hardcoded secrets, synthetic test data only.
- Persian text must remain naturally horizontal, responsive, and readable; no 1-character wrapping.
- Map must clip strictly to its bounds and support pan, pinch-to-zoom, and two-finger rotation.
- All existing tests must pass without deleting or weakening them.

## Review Focus

1. **Persian Text Narrow Column Wrapping**: When unconstrained or inappropriately weighted rows squeeze text columns down to <20px, forcing 1-char-per-line vertical text.
2. **Canvas Bleeding Outside Bounds**: Canvas draw operations rendering beyond card containers onto surrounding views when not clipped.
3. **Serialization Crashes in Offline Storage**: Domain models stored in SQLite JSON columns without `@Serializable` annotation causing runtime `SerializationException`.
4. **Gesture Conflicts on Map**: Touch events on Canvas conflicting with outer vertical scroll container in `TacticalScreenContainer`.
5. **State Desynchronization in Group/Status Management**: UI state not reflecting local cache or failing silently on network degradation.

---

### Task 1: Fix TeamMember Serialization & SQLite Offline Crash (P0)

**Files:**
- Modify: `app/src/main/java/ir/alifaniani/waymate/domain/model/TeamMember.kt`
- Test: `app/src/test/java/ir/alifaniani/waymate/data/local/sqlite/SqliteOfflineStorageTest.kt`

**Interfaces:**
- Produces: `@Serializable data class TeamMember(val id: String, val fullName: String, val callsign: String, val roleInTeam: String)`

- [ ] **Step 1: Write failing unit test for TeamMember serialization in SQLite storage**
  Verify that saving and retrieving cached teams with non-empty members succeeds without `SerializationException`.
- [ ] **Step 2: Run test to verify it fails**
  Run: `./gradlew.bat testDebugUnitTest --tests "*SqliteOfflineStorageTest*"`
- [ ] **Step 3: Annotate `TeamMember` with `@Serializable` and `@SerialName`**
  Add `@Serializable` to `TeamMember` in `ir.alifaniani.waymate.domain.model.TeamMember.kt`.
- [ ] **Step 4: Run test to verify it passes**
  Run: `./gradlew.bat testDebugUnitTest --tests "*SqliteOfflineStorageTest*"`

---

### Task 2: Standardize Button Constraints & Status Badge Non-Wrapping (P2)

**Files:**
- Modify: `app/src/main/java/ir/alifaniani/waymate/ui/common/TacticalButton.kt`
- Modify: `app/src/main/java/ir/alifaniani/waymate/ui/common/TacticalStatusBadge.kt`
- Modify: `app/src/main/java/ir/alifaniani/waymate/ui/auth/LoginScreen.kt`

**Interfaces:**
- `TacticalOutlinedButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, isDestructive: Boolean = false)`
- `TacticalStatusBadge(text: String, modifier: Modifier = Modifier, tone: BadgeTone = BadgeTone.NEUTRAL)`

- [ ] **Step 1: Modify `TacticalButton.kt` to avoid hardcoding `.fillMaxWidth()`**
  In `TacticalOutlinedButton` and `TacticalPrimaryButton`, make `modifier` determine the width (allowing callers to specify `.fillMaxWidth()`, `.wrapContentWidth()`, or `Modifier.weight(1f)` without forced 100% width). Default to `.fillMaxWidth()` only where full width is standard, or preserve caller modifier.
- [ ] **Step 2: Modify `TacticalStatusBadge.kt` to guarantee horizontal integrity**
  Add `maxLines = 1`, `softWrap = false`, and `Modifier.widthIn(min = ...)` so badges never wrap vertically into 1-character columns.
- [ ] **Step 3: Add Quick-Fill Demo Account buttons to `LoginScreen.kt`**
  Provide one-tap helper buttons for "فرمانده (Commander)" and "سرتیم (Team Lead)" to facilitate effortless physical device testing.
- [ ] **Step 4: Verify compilation and tests**
  Run: `./gradlew.bat testDebugUnitTest`

---

### Task 3: Repair Persian RTL Layouts Across All Screen Sections (P2)

**Files:**
- Modify: `app/src/main/java/ir/alifaniani/waymate/ui/home/HomeScreen.kt`
- Modify: `app/src/main/java/ir/alifaniani/waymate/ui/home/LocationSharingControlCard.kt`
- Modify: `app/src/main/java/ir/alifaniani/waymate/ui/home/TacticalMapAndLinkSection.kt`
- Modify: `app/src/main/java/ir/alifaniani/waymate/ui/home/TacticalCommunicationsSection.kt`
- Modify: `app/src/main/java/ir/alifaniani/waymate/ui/home/TacticalManagementSection.kt`
- Modify: `app/src/main/java/ir/alifaniani/waymate/ui/home/TacticalUserGroupSection.kt`

**Interfaces:**
- Ensure all headers distribute width with `Modifier.weight(1f)` for text titles.
- Move crowded action buttons in `TacticalMapAndLinkSection` to a dedicated responsive action row.
- Ensure all `DetailItem` rows in `HomeScreen` take `fillMaxWidth()` and align consistently.

- [ ] **Step 1: Fix `LocationSharingControlCard.kt`**
  Give the title `Text` / `Row` `Modifier.weight(1f)` so the status badge retains its natural width and does not vertically stack.
- [ ] **Step 2: Fix `TacticalMapAndLinkSection.kt`**
  Separate title/subtitle from action buttons. Put title and active layer badge in the header row, and place "شبیه‌ساز پیمایش ⛰" and "نقطه جدید (+)" in an action row beneath it.
- [ ] **Step 3: Fix `TacticalCommunicationsSection.kt`**
  Apply `Modifier.weight(1f)` to title columns in the header and `LiveUnitStatusRow`. Fix the offline queue banner row so button does not starve text column.
- [ ] **Step 4: Fix `TacticalManagementSection.kt` and `TacticalUserGroupSection.kt`**
  Verify all rows have proper weight distribution and RTL alignment.
- [ ] **Step 5: Run tests and install on device to verify visual layout**
  Run: `./gradlew.bat assembleDebug` and inspect with adb screencap.

---

### Task 4: Map Engine Canvas Clipping, Touch Gestures & Coordinate Stability (P0, P1)

**Files:**
- Modify: `app/src/main/java/ir/alifaniani/waymate/ui/home/map/TacticalMapCanvas.kt`
- Modify: `app/src/main/java/ir/alifaniani/waymate/ui/home/map/TacticalMapState.kt`
- Test: `app/src/test/java/ir/alifaniani/waymate/ui/home/map/TacticalMapStateTest.kt`

**Interfaces:**
- `detectTransformGestures` supporting:
  - 1-finger pan (`panOffset += pan`)
  - 2-finger pinch-to-zoom (`zoomLevel` adjustment with smooth scaling)
  - 2-finger rotation (`rotationAngle` state)
- Viewport clipping: `Modifier.clipToBounds()` on map Box and `clipRect` inside Canvas.

- [ ] **Step 1: Add viewport clipping to `TacticalMapCanvas.kt`**
  Add `.clip(RoundedCornerShape(10.dp))` and `.clipToBounds()` to the Map container Box and Canvas so drawings never bleed into surrounding cards.
- [ ] **Step 2: Add rotation and continuous zoom support to `TacticalMapUiState`**
  Add `mapRotationDegrees: Float = 0f` and continuous zoom scale in `TacticalMapUiState`.
- [ ] **Step 3: Implement multi-touch gesture detection in `TacticalMapCanvas.kt`**
  Use `detectTransformGestures` with focal point calculation and proper gesture consumption. Keep tap gestures working for POI/marker hit-testing.
- [ ] **Step 4: Update Canvas drawing transformation pipeline**
  Apply rotation around center canvas point (`rotate(degrees, pivot)`) and verify marker, route, and road transformations.
- [ ] **Step 5: Verify tests and run physical device touchscreen test**
  Run: `./gradlew.bat testDebugUnitTest` and test pan/zoom/rotate on device.

---

### Task 5: Live Location Sharing Verification & Error/Recovery Lifecycle (P1)

**Files:**
- Modify: `app/src/main/java/ir/alifaniani/waymate/service/LocationSharingService.kt`
- Modify: `app/src/main/java/ir/alifaniani/waymate/data/network/LocationWebSocketClient.kt`
- Modify: `app/src/main/java/ir/alifaniani/waymate/ui/home/LocationSharingControlCard.kt`

- [ ] **Step 1: Audit permission requesting and foreground service lifecycle**
  Ensure notifications and location updates start cleanly upon user confirmation.
- [ ] **Step 2: Verify WebSocket error and reconnecting states**
  Ensure `LocationSharingStatus.RECONNECTING` and `ERROR` are gracefully reflected with Persian labels.
- [ ] **Step 3: Verify stop event handling and service termination**
  Ensure `LocationSharingService.stop()` releases GPS updates and closes WebSocket cleanly.

---

### Task 6: Operational Status Persistence & Command Dispatch Hardening (P1)

**Files:**
- Modify: `app/src/main/java/ir/alifaniani/waymate/data/local/OfflineStorage.kt`
- Modify: `app/src/main/java/ir/alifaniani/waymate/data/local/sqlite/SqliteOfflineStorage.kt`
- Modify: `app/src/main/java/ir/alifaniani/waymate/data/local/InMemoryOfflineStorage.kt`
- Modify: `app/src/main/java/ir/alifaniani/waymate/domain/repository/StatusRepository.kt`
- Modify: `app/src/main/java/ir/alifaniani/waymate/data/repository/RealStatusRepository.kt`
- Modify: `app/src/main/java/ir/alifaniani/waymate/ui/home/HomeViewModel.kt`
- Modify: `app/src/main/java/ir/alifaniani/waymate/ui/home/TacticalCommunicationsSection.kt`

- [ ] **Step 1: Add user operational status persistence to `OfflineStorage` and `StatusRepository`**
  Save the user's selected status to local storage so it is retained when reopening the screen.
- [ ] **Step 2: Load saved operational status on `HomeViewModel.init`**
  Ensure the active status chip matches persisted state immediately upon loading.
- [ ] **Step 3: Prevent duplicate command submissions and add in-flight loading state**
  Disable send button while sending, clear input upon success, and show immediate feedback.
- [ ] **Step 4: Verify status switching and offline queue buffering**
  Ensure selecting statuses offline queues the action and shows proper offline count.

---

### Task 7: Group & Member Management End-to-End Implementation (P1)

**Files:**
- Modify: `app/src/main/java/ir/alifaniani/waymate/data/network/TeamApiService.kt`
- Modify: `app/src/main/java/ir/alifaniani/waymate/domain/repository/TeamRepository.kt`
- Modify: `app/src/main/java/ir/alifaniani/waymate/data/repository/RealTeamRepository.kt`
- Modify: `app/src/main/java/ir/alifaniani/waymate/data/repository/FakeTeamRepository.kt`
- Modify: `app/src/main/java/ir/alifaniani/waymate/ui/home/HomeViewModel.kt`
- Modify: `app/src/main/java/ir/alifaniani/waymate/ui/home/TacticalManagementSection.kt`
- Modify: `app/src/main/java/ir/alifaniani/waymate/ui/home/TacticalUserGroupSection.kt`

- [ ] **Step 1: Add `addMember` and `removeMember` to `TeamApiService` and repositories**
  Connect to existing Cloudflare Worker endpoints: `POST /api/v1/teams/:id/members` and `DELETE /api/v1/teams/:id/members/:memberId`.
- [ ] **Step 2: Add member management functions in `HomeViewModel`**
  `addTeamMember(teamId, fullName, callsign, role)` and `removeTeamMember(teamId, memberId)`.
- [ ] **Step 3: Add UI dialogs in `TacticalManagementSection` for member management**
  Allow adding personnel and removing personnel directly from team cards.
- [ ] **Step 4: Verify group creation, listing, member addition, and deletion**

---

### Task 8: Physical Device QA & Automated Regression Tests (P0-P3)

- [ ] **Step 1: Run complete automated test suite**
  `./gradlew.bat testDebugUnitTest --offline`
- [ ] **Step 2: Build debug APK**
  `./gradlew.bat assembleDebug --offline`
- [ ] **Step 3: Run lint**
  `./gradlew.bat lintDebug --offline`
- [ ] **Step 4: Install and capture verification screenshots on physical device SM-G990E**
  Capture all screens: Home, Map with gestures, Status, Commands, Groups, Members, Settings.
- [ ] **Step 5: Prepare final engineering report**

