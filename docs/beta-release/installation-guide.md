# WayMate Beta — Installation & Setup Guide

**Target Audience:** Authorized Beta Testers  
**Release:** WayMate `0.14.0-beta.1` (Version Code: `2`)  
**Package Name:** `ir.alifaniani.waymate`  
**Distribution Channel:** Closed Beta (Direct APK Sideload)  

---

## 1. System Requirements

- **Supported Android OS:** Android 8.0 (Oreo / API 26) or newer (tested up to Android 16 / API 36).
- **Free Storage:** At least 50 MB of available storage for application installation and offline vector cache.
- **Hardware Permissions:**
  - `ACCESS_FINE_LOCATION` and `ACCESS_COARSE_LOCATION` (Required for live location sharing and trail navigation).
  - `FOREGROUND_SERVICE` and `FOREGROUND_SERVICE_LOCATION` (Required for persistent location tracking during screen lock).
  - `POST_NOTIFICATIONS` (Required on Android 13+ to display the ongoing safety notification).

---

## 2. APK Installation Steps

### Step 1: Transfer or Download the APK
Copy `app-release.apk` to your Android device via USB cable, ADB, or your organization's secure file repository.

### Step 2: Enable "Install Unknown Apps"
Because this is a beta release distributed outside Google Play Store, Android requires granting sideload permission:
1. Open your device **Settings**.
2. Navigate to **Apps** > **Special app access** > **Install unknown apps**.
3. Select the file manager or browser you are using to open the APK.
4. Toggle on **Allow from this source**.

### Step 3: Install the Package
1. Tap on `app-release.apk` in your device's file manager.
2. When prompted by Android Package Installer, tap **Install**.
3. If Google Play Protect displays an "Unrecognized Developer" dialog, tap **More details** > **Install anyway**. (This warning appears because the beta key is a private developer certificate).
4. Tap **Open** once installation finishes.

---

## 3. Updating from an Existing Build

- **From a Previous Beta Build:** If updating from a future beta build signed with the same `waymate-beta.jks` key, you can install the new APK directly over the existing one without losing local data.
- **From an Internal Debug Build:** If you previously had an internal debug build installed, Android security strictly prohibits in-place upgrades (`INSTALL_FAILED_UPDATE_INCOMPATIBLE`). You must uninstall the debug version before installing this signed beta.

---

## 4. First-Time Launch & Permissions

1. **Launch the Application:** Launch WayMate from your application drawer.
2. **Login Credentials:**
   - For authorized testing, test accounts with pre-configured synthetic teams are provided:
     - **Commander Role:** Username `commander_demo`, Password `demo123`
     - **Patrol Lead Role:** Username `patrol_lead_demo`, Password `demo123`
   - Alternatively, use the quick-fill buttons on the login screen:
     - Tap **ورود آزمایشی فرمانده** (Quick Commander Login)
     - Tap **ورود آزمایشی سرتیم** (Quick Patrol Lead Login)
   - Tap **ورود به سامانه** (Login).
3. **Location Permission Granting:**
   - When tapping **شروع اشتراک‌گذاری موقعیت** (Start Location Sharing), an educational privacy rationale dialog will appear in Persian explaining the purpose of location tracking.
   - Tap **متوجه شدم و تایید می‌کنم** (Understood & Confirm).
   - In the system prompt, select **While using the app** and enable **Precise Location**.
   - If prompted for notification permissions, select **Allow** so the ongoing safety notification can be displayed.

---

## 5. Controlling & Stopping Location Sharing

- To stop sharing at any time, tap the red button **توقف اشتراک‌گذاری موقعیت (⏹)** on the home screen.
- You can also pull down the Android notification shade and tap the action button on the persistent WayMate notification.
- When stopped, the background service terminates immediately, and the status badge updates to **متوقف شد** (Stopped).

---

## 6. Safe Uninstallation & Data Cleanup

1. Open Android **Settings** > **Apps** > **WayMate**.
2. Tap **Storage** > **Clear Data** (optional: completely purges the local SQLite database and cached vector tiles).
3. Tap **Uninstall** to remove the application from your device.

