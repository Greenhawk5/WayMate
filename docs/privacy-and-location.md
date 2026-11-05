# WayMate — Privacy, Location Permissions & Real-Time Sharing Architecture

This document formalizes the privacy boundaries, location permission lifecycle, consent mechanisms, and security model for the WayMate outdoor recreation companion.

---

## 1. Ethical & Architectural Boundaries

### Explicit Principles
To preserve user autonomy, battery longevity, and fundamental civil privacy:
1. **Consent-Based Location Sharing**: Location is shared **strictly upon the explicit, conscious activation** by the individual user tapping «شروع اشتراک‌گذاری».
2. **Never Inferred from Group Membership**: Being added to or belonging to a hiking team does **never** authorize automatic location acquisition or broadcasting.
3. **No Automatic Boot or Login Sharing**: Location sharing never starts automatically after device reboot, app install, or user authentication.
4. **Immediate Stop Action**: The user can terminate location sharing instantly from either the in-app control card or the persistent notification action («توقف اشتراک‌گذاری»).
5. **Zero Background Location Permission**: WayMate strictly **does not** request `ACCESS_BACKGROUND_LOCATION`. Background execution during hiking trips is achieved transparently through an active Android Foreground Service (`FOREGROUND_SERVICE_LOCATION`) with an ongoing notification.
6. **No Unnecessary Location History**: Live coordinate streams are routed through in-memory Durable Objects/sessions without being persisted to the Cloudflare D1 database.
7. **Simulation Isolation**: Synthetic coordinates from the `HikingSimulationEngine` remain purely in-memory and are never broadcast over the network.

---

## 2. Location Permission Model & Transparency

### Permission States & Graceful Degradation
| Permission State | System Status | Application Capability | User Feedback |
| :--- | :--- | :--- | :--- |
| **Denied / Unrequested** (Default) | No runtime location permissions granted | Full offline vector map navigation, Jahrom GIS routes, shared POIs, tactical HUD, and synthetic simulation. | Informational badge in control card; sharing disabled. |
| **Coarse Location Only** (`ACCESS_COARSE_LOCATION`) | Approximate (~1-2 km) coordinate precision | Regional map centering; coarse location sharing with peers if enabled. | "موقعیت تقریبی فعال است" badge. |
| **Fine Location** (`ACCESS_FINE_LOCATION`) | High-accuracy GPS/GNSS active while service is running | High-precision live location sharing with peers, directional bearing, altitude, speed. | Precise glowing marker on canvas; persistent ongoing notification. |

### User-Visible Permission Rationale
Before the system permission prompt is invoked, WayMate displays a transparent explanation dialog in Persian:
> **متن شفاف به کاربر:**
> «وای‌میت برای نمایش موقعیت زندهٔ شما به همنوردان گروه در طول پیمایش و حفظ ایمنی شما در زمان قفل بودن صفحه، نیازمند مجوز موقعیت مکانی و نمایش یک اعلان ماندگار است.
> اطلاعات مکانی شما بدون درخواست مستقیم خودتان ارسال نمی‌شود و هر زمان بخواهید می‌توانید اشتراک‌گذاری را فوراً متوقف کنید.»

---

## 3. Background Foreground Service Lifecycle

### Service Lifecycle Rules
- **Component**: `LocationSharingService` (`foregroundServiceType="location"`).
- **Persistent Notification**: While active, displays channel `waymate_location_sharing` with an ongoing notification explaining that location sharing is running.
- **Immediate Stop Action**: Includes a direct pending intent action on the notification: **«توقف اشتراک‌گذاری»**.
- **Screen Locking & Backgrounding**: Keeps the GPS sensor and WebSocket connection alive while hiking with the phone in pocket or screen locked.
- **START_NOT_STICKY**: Does not restart if killed by the OS or user force-stop.
- **Platform Limitations**: If the operating system or user force-stops the app or revokes permissions in system settings, execution ceases. WayMate documents this limitation honestly.

---

## 4. Backend Privacy & Security Guarantees

1. **Authentication & Authorization**:
   - WebSocket handshakes require a valid Bearer JWT.
   - The user's identity is derived server-side from the verified token, preventing impersonation.
2. **Strict Group Isolation**:
   - The server verifies in Cloudflare D1 that the user is an authorized member of the requested group (`team_id`).
   - Cross-group access is blocked; members of Team Alpha cannot see or receive location fixes from Team Beta.
3. **Transient Coordinate In-Memory Routing**:
   - Cloudflare Durable Objects (`LocationSessionDO`) route coordinate fixes in-memory to active group sockets.
   - No high-frequency coordinate dumps are written to D1 disk, protecting user movement histories from permanent retention.
4. **Immediate Invalidation on Stop**:
   - When a user stops sharing, a `SHARING_STOPPED` signal is fan-out broadcast. Receiving group devices immediately discard the user's active marker.
5. **No Coordinate Logging**:
   - Production server and Android loggers strictly avoid logging raw latitude and longitude coordinates.

---

## 5. Practical Two-Device Verification Procedure

To verify real-time consent-based location sharing in a controlled testing environment:

### Prerequisites
- Device A: Android device or emulator with test account `usr_hiker_001` (Callsign: «شاهین»).
- Device B: Android device or emulator with test account `usr_hiker_002` (Callsign: «البرز»).
- Both users belong to the same authorized test group: `team_jahrom_alpha`.

### Verification Steps
1. **Authentication & Team Setup**:
   - Log into Device A as `usr_hiker_001`. Confirm team `team_jahrom_alpha` appears in the operational dashboard.
   - Log into Device B as `usr_hiker_002`. Confirm team `team_jahrom_alpha` appears in the operational dashboard.
2. **Start Sharing on Device A**:
   - On Device A, tap **«شروع اشتراک‌گذاری زنده»** in the Location Sharing Control Card.
   - Confirm the Persian pre-permission rationale dialog appears explaining location usage.
   - Confirm and grant Android location and notification permissions.
   - Verify on Device A:
     - The persistent notification appears with the title «وای‌میت — اشتراک موقعیت فعال است» and the «توقف» action button.
     - The card status turns green: «در حال اشتراک‌گذاری زنده».
3. **Verify Map Presentation on Device B**:
   - On Device B, observe the tactical map canvas.
   - Verify Device A's marker appears with callsign «شاهین», pulsing emerald green aura, and directional heading pointer.
   - Tap Device A's marker on Device B to inspect the tactical HUD pill (verifying altitude, accuracy, and update timestamp).
4. **Lock Device A's Screen (Background Execution)**:
   - Lock the screen or press Home on Device A.
   - Move or simulate position changes on Device A.
   - Verify Device B continues to receive smooth real-time updates while Device A is locked in the background.
5. **Stop Sharing on Device A**:
   - On Device A, either tap «توقف اشتراک‌گذاری» in the app card OR tap «توقف» directly from the Android ongoing notification.
   - Verify on Device A:
     - Notification is immediately dismissed.
     - Card status changes to «متوقف شد».
     - GPS updates cease.
   - Verify on Device B:
     - Device A's live marker is immediately invalidated and removed from the tactical map canvas per privacy rules.
6. **Cross-Group Isolation Check**:
   - Authenticate Device C as a user from an unrelated team (`team_beta`).
   - Confirm Device C receives zero location packets or markers from Device A.
