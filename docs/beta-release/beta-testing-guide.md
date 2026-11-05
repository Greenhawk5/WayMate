# WayMate Beta — Testing & Quality Assurance Guide

**Phase:** Beta Release Candidate Validation  
**Target Build:** `0.14.0-beta.1` (versionCode `2`)  
**Target Audience:** Field Beta Testers & QA Engineers  

---

## 1. Testing Objectives

The primary goals of the Beta 1 testing program are to:
1. Verify functional stability under actual field conditions.
2. Confirm battery and thermal behavior during continuous foreground location sharing.
3. Validate offline queueing and recovery across intermittent mobile network connectivity.
4. Assess the readability and ergonomics of the Persian RTL tactical user interface.

---

## 2. Test Accounts & Synthetic Data

All beta testing must be conducted using authorized synthetic identities. **Do not use real personal credentials or input sensitive operational data.**

| Role | Callsign | Username | Password | Assigned Group |
| :--- | :--- | :--- | :--- | :--- |
| **Commander** | `Eagle-1` | `commander_demo` | `demo123` | Quick Reaction Patrol Alpha |
| **Patrol Lead** | `Falcon-Lead` | `patrol_lead_demo` | `demo123` | Quick Reaction Patrol Alpha |
| **Field Member** | `Alborz-1` | `member_demo_1` | `demo123` | Test Group One |

---

## 3. Core Test Scenarios

### Scenario 1: Authentication & Session Lifecycle
- **Action:** Log in with `commander_demo`.
- **Expected:** Home screen loads in under 2 seconds; user card displays `Eagle-1 (فرمانده)` with `COMMANDER` role; offline cache initializes.
- **Action:** Scroll to the bottom and tap **خروج از حساب کاربری** (Logout).
- **Expected:** Session token is purged from Keystore; application returns to login screen.

### Scenario 2: Tactical Canvas Map Navigation
- **Action:** Navigate to the map section and drag across the viewport.
- **Expected:** Smooth 60fps pan gesture without jumping; thoroughfares and waterways remain properly projected.
- **Action:** Tap `+` and `-` zoom buttons.
- **Expected:** Map scale increases and decreases cleanly.
- **Action:** Open layer control sheet and toggle thoroughfares or grid lines.
- **Expected:** Selected layer elements instantly appear/disappear without redrawing the entire screen.

### Scenario 3: Points of Interest (POI) Management
- **Action:** Tap **نقطه جدید (+)** to open the POI creation dialog.
- **Expected:** Persian category chips (Start, Destination, Campsite, Water Source, Rest Area, General) are selectable.
- **Action:** Enter a name (e.g. `کمپ تستی جهرم`) and submit.
- **Expected:** New marker appears on the map with category icon and synchronization badge.

### Scenario 4: Live Location Sharing Lifecycle
- **Action:** Tap **شروع اشتراک‌گذاری موقعیت (▶)**.
- **Expected:** Location permission dialog appears; upon approval, real GPS coordinates (e.g. `31.2980°N, 48.6410°E`) appear in the HUD; persistent notification appears in system shade.
- **Action:** Lock the phone screen for 2 minutes, then unlock.
- **Expected:** Service remained active; location updates continue streaming; battery drain remains minimal.
- **Action:** Tap **توقف اشتراک‌گذاری موقعیت (⏹)**.
- **Expected:** Notification disappears; background service terminates; status badge shows **متوقف شد**.

### Scenario 5: Offline Resilience & Recovery
- **Action:** Put the phone into Airplane Mode (disable Wi-Fi and mobile data).
- **Expected:** UI indicates offline status without crashing; local map remains fully browsable.
- **Action:** Submit an operational command or update status to **تعقیب (Pursuing)** while offline.
- **Expected:** Action is buffered in the local `offline_queue` table; queue indicator increments.
- **Action:** Disable Airplane Mode and tap **همگام‌سازی فوری وضعیت و پیام‌ها** (Sync).
- **Expected:** Queued actions are transmitted to Cloudflare Workers edge; server acknowledges receipt; status updates to confirmed.

---

## 4. Defect Reporting Protocol

When encountering an unexpected behavior, crash, or layout defect, submit a report using the format below:

```markdown
### Bug Report Template
- **Title:** [Brief summary of the issue]
- **Severity:** [Blocker / High / Medium / Low]
- **Device Model:** [e.g., Samsung Galaxy S21 FE, Xiaomi Redmi Note 12]
- **Android Version:** [e.g., Android 14, Android 16]
- **Steps to Reproduce:**
  1. Step 1
  2. Step 2
  3. Step 3
- **Expected Result:** [What should have happened]
- **Actual Result:** [What actually happened]
- **Logcat or Screenshot:** [Attach screenshot or relevant logcat excerpt]
```

