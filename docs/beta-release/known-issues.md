# WayMate Beta 1 — Known Issues & Tracking

**Document:** Beta Issue Registry & Technical Debt  
**Release:** `0.14.0-beta.1` (versionCode `2`)  
**Status:** Active  

---

## 1. Active Issues & Verification Gaps

### BUG-001: Physical Touch Multi-Finger Map Gestures (Pinch & Rotation)
- **Bug ID:** `BUG-001`
- **Severity:** Medium
- **Description:** Two-finger rotation and pinch-to-zoom gestures on the Compose Canvas map have not been fully verified on physical multi-touch screens.
- **Reproduction Steps:**
  1. Open Tactical Map on physical phone.
  2. Attempt simultaneous two-finger rotation or pinch.
- **Expected Behavior:** Canvas rotates smoothly around the touch midpoint and zooms continuously.
- **Actual Behavior:** Rotation calculation may conflict with single-touch drag offsets; pinch zoom may stutter compared to button zoom.
- **Device / API Level:** Tested on Samsung Galaxy S21 FE 5G (Android 16 / API 36).
- **Workaround:** Use dedicated on-screen zoom buttons (`+` and `-`) located on the left edge of the map.
- **Proposed Fix:** Refactor pointer input gesture detector to use `detectTransformGestures` with centroid stabilization.
- **Target Release:** `0.14.1-beta.2`

---

### BUG-002: Physical Dual-Device Live Location Exchange
- **Bug ID:** `BUG-002`
- **Severity:** Medium
- **Description:** Real-time location exchange between two independent physical Android phones via Cloudflare Durable Objects WebSocket fan-out has not been validated end-to-end simultaneously on physical phones.
- **Reproduction Steps:**
  1. Launch WayMate on Phone A and start location sharing in Group Alpha.
  2. Launch WayMate on Phone B in Group Alpha.
- **Expected Behavior:** Phone B immediately renders Phone A's moving coordinate marker in emerald green.
- **Actual Behavior:** Verified comprehensively through automated integration tests, mock WebSocket tests (`locations_ws.test.ts`), and Phase 7.2 hybrid live testing (1 physical Samsung Galaxy S21 FE phone + 1 automated Node.js streaming client over production Cloudflare Durable Objects). However, simultaneous end-to-end exchange between two separate physical Android smartphones remains pending availability of a second physical device.
- **Device / API Level:** Any Android 8.0+ device.
- **Workaround:** Hybrid live testing and automated suites confirm protocol compliance and clean fan-out.
- **Proposed Fix:** Conduct two-phone field trial upon acquiring a second connected test phone.
- **Target Release:** `0.14.1-beta.2`

---

### BUG-003: Backend Development Tooling Vulnerabilities (`npm audit`)
- **Bug ID:** `BUG-003`
- **Severity:** Low (Non-production)
- **Description:** `npm audit` in the `backend/` directory reports 3 moderate vulnerabilities in development packages: `vitest`, `@vitest/mocker`, and `tinypool`.
- **Reproduction Steps:**
  1. Navigate to `backend/`.
  2. Run `npm audit`.
- **Expected Behavior:** Zero vulnerabilities across devDependencies.
- **Actual Behavior:** 3 moderate vulnerabilities flagged in local test runner packages.
- **Device / API Level:** Host development workstation (Node.js v22.12.0).
- **Workaround:** None required for production; runtime dependencies (`hono`, `@cloudflare/workers-types`) have 0 vulnerabilities and edge deployment bundles exclude `devDependencies`.
- **Proposed Fix:** Upgrade Vitest to 5.x once breaking API changes are mapped and tested.
- **Target Release:** `0.15.0` (Production Hardening)

---

### BUG-004: In-Memory Rate Limiter Worker Isolate Scoping
- **Bug ID:** `BUG-004`
- **Severity:** Low
- **Description:** The edge rate limiting middleware (`MemoryRateLimiter`) operates in-memory within individual Cloudflare Worker isolates.
- **Reproduction Steps:**
  1. Send 10 rapid login attempts from Region A.
  2. Route 11th request to Region B edge node.
- **Expected Behavior:** Global rate limiting enforced strictly across all global edge locations.
- **Actual Behavior:** Rate limiting applies per Worker isolate; if requests hit different isolates, counters are independent.
- **Device / API Level:** Cloudflare Workers Edge Runtime.
- **Workaround:** In-memory bucket suffices for prototype and small beta teams.
- **Proposed Fix:** Bind rate limiter state to a centralized Durable Object or configure Cloudflare WAF Rate Limiting Rules.
- **Target Release:** `0.15.0` (Production Hardening)

---

## 2. Resolved Defects During Beta Preparation

### RESOLVED-001: R8 Experimental Optimization `packageScope` Conflict
- **Bug ID:** `RESOLVED-001`
- **Severity:** High / Blocker
- **Description:** Enabling AGP 9.4 `optimization { packageScope = setOf("kotlin.**") }` caused `java.lang.IllegalAccessError: Class kotlin.text.f extended by class na1 is inaccessible` at startup when OkHttp 4 accessed Kotlin internal string utilities.
- **Resolution:** Replaced the experimental `packageScope` optimization with safe release packaging (`isMinifyEnabled = false`), allowing standard ART optimizations while preventing runtime class access errors. Verified on physical device.
- **Status:** **RESOLVED** in `0.14.0-beta.1`.

---

## 3. Feedback Collection & Next Beta Process

1. **Bug Submission:** Beta testers submit issues using the markdown template in `beta-testing-guide.md`.
2. **Triage:** Issues are classified by severity (Blocker, High, Medium, Low).
3. **Patch Cadence:** Bug fixes will be packaged into weekly beta releases (`0.14.1-beta.2`, `0.14.2-beta.3`).
4. **Key Continuity:** All subsequent beta releases will be signed with the dedicated `waymate-beta.jks` key, enabling seamless in-place APK updates without data loss.

