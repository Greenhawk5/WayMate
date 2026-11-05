# WayMate Beta 1 — Release Notes

**Application ID:** `ir.alifaniani.waymate`  
**Release Name:** WayMate Beta 1  
**Version Name:** `0.14.0-beta.1`  
**Version Code:** `2`  
**Release Date:** November 6, 2026  
**Channel:** Closed Beta Testing  
**Language Policy:** English technical documentation / Persian RTL user interface  

---

## 1. Overview

WayMate `0.14.0-beta.1` is the first formal, signed beta release candidate of the WayMate group hiking and tourism Android application. This release stabilizes the client-side architecture, ensures robust offline resilience, provides real-time location sharing under strict user consent, and integrates with the production Cloudflare Workers edge backend.

---

## 2. Major Features Included

### 2.1 Tactical Map & GIS Overlays
- **Custom Compose Canvas Map:** High-performance, offline-capable 2D map engine rendering Jahrom GIS vector geometries (EPSG:4326), including 5,351 thoroughfares and 108 waterways.
- **Interactive Multi-Layer System:** 7-layer toggleable bottom sheet supporting base map, thoroughfares, waterways, coordinate grid, route waypoints, hiking avatars, and group markers.
- **Interactive Pan & Zoom:** Responsive 2D drag gestures and dedicated zoom controls (+/-) with adaptive boundary snapping.
- **Hiking Route Simulation:** Deterministic simulation engine calculating Haversine distance, bearings, polyline interpolation, and configurable hiking playback speeds (1x, 2x, 5x).

### 2.2 Points of Interest (POI) Management
- **Local & Cloud POI Sync:** Category-aware markers (Start, Destination, Campsite, Water Source, Rest Area, General) synchronized with Cloudflare D1.
- **Offline Buffering:** New POIs created while disconnected are buffered in local SQLite and automatically synced when connectivity resumes.
- **Optimistic Concurrency:** Versioned records prevent concurrent editing conflicts (HTTP 409 handling).

### 2.3 Live Location Sharing (Opt-In & Consent-Driven)
- **Zero-Google-Play-Services Architecture:** Uses native Android `LocationManager` GPS provider without vendor lock-in.
- **Foreground Service Compliance:** Registered as `FOREGROUND_SERVICE_TYPE_LOCATION` (0x08) with persistent ongoing system notification containing immediate stop action.
- **WebSocket Edge Streaming:** Authenticated WebSocket communication (`/api/v1/ws/locations`) handled by Cloudflare Durable Objects (`LocationSessionDO`).
- **Tactical Member HUD:** Group members' locations rendered with direction-facing avatars, emerald halo indicators, and 60-second stale data warnings.

### 2.4 Tactical Communications & Status
- **Operational Status Dispatch:** Real-time state selection (`NORMAL`, `PURSUING`, `ENGAGED`) with instant persistence and team broadcasting.
- **Broadcast Commands:** Priority tactical announcements dispatched by commanders to field teams with read receipt and acknowledgement tracking (`تأییدشده ✓`).

### 2.5 Security & Data Isolation
- **Role-Based Access Control (RBAC):** Strict hierarchy enforcing `OWNER`, `COMMANDER`, and `TEAM_LEAD` authorizations.
- **Tenant Isolation:** Complete cross-group isolation prevents unauthorized access to other teams' data.
- **Encrypted Local Storage:** Session tokens stored securely in Android Keystore with AES-GCM encryption.
- **Backup Protection:** Sensitive credentials and offline SQLite database files are excluded from Android auto-backups.

---

## 3. Important Bug Fixes & Stabilization

1. **R8 Repackaging Incompatibility:** Fixed an `IllegalAccessError` occurring at startup where aggressive package-scope optimization (`packageScope = setOf("kotlin.**")`) broke OkHttp 4's access to Kotlin internal string utilities.
2. **Persian Typography & Text Wrapping:** Resolved vertical character stacking by replacing unbounded min-width constraints with flexible RTL layout wrappers.
3. **Map Canvas Gestures:** Fixed gesture conflicts between canvas drag gestures and vertical parent scroll containers.
4. **Foreground Service Lifecycle:** Resolved service lifecycle leaks, ensuring clean destruction and UI state resets when location sharing is stopped.

---

## 4. Known Limitations

The following items are documented limitations for this beta release:
1. **Pinch-to-Zoom & Two-Finger Map Rotation:** Physical two-finger rotation gestures remain under development; map scale is reliably controlled via the UI zoom buttons.
2. **Two-Device Live Location Verification:** Two-client WebSocket fan-out is verified via automated unit and mock tests; physical validation across two independent hardware devices is pending a second test phone.
3. **Development Tooling Dependencies:** Three moderate vulnerabilities identified in local devDependencies (`vitest`, `@vitest/mocker`, `tinypool`). These are strictly dev-time tools and are not packaged into the runtime APK or Cloudflare Worker.

---

## 5. Compatibility & System Requirements

- **Operating System:** Android 8.0 (API Level 26) through Android 16 (API Level 36).
- **Architecture:** 64-bit ARM (`arm64-v8a`), 32-bit ARM (`armeabi-v7a`), x86/x86_64 emulator support.
- **Sensors:** GPS / GNSS sensor recommended for live location features.
- **Internet:** HTTPS connection for cloud synchronization; fully functional offline for local map and cached data.

