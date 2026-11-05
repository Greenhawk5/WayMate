# WayMate Beta 1 — Release Manifest

**Date & Time of Build:** November 6, 2026 00:44:10 GMT+03:30  
**Target Environment:** Closed Beta Testing  

---

## 1. Package & Version Metadata

| Attribute | Value |
| :--- | :--- |
| **Application ID** | `ir.alifaniani.waymate` |
| **Version Name** | `0.14.0-beta.1` |
| **Version Code** | `2` |
| **Min SDK** | `26` (Android 8.0 Oreo) |
| **Target SDK** | `37` (Android 16 Vanilla Ice Cream) |
| **Compile SDK** | `37` |
| **Package Type** | Signed Release APK (`app-release.apk`) |

---

## 2. File Verification & Checksums

| File Property | Value |
| :--- | :--- |
| **Artifact Filename** | `app-release.apk` |
| **Relative Path** | `app/build/outputs/apk/release/app-release.apk` |
| **Absolute Path** | `E:\2.Things\Android-Projects\WayMate\app\build\outputs\apk\release\app-release.apk` |
| **File Size** | `9,796,126 bytes` (9.34 MB) |
| **SHA-256 Checksum** | `79fa0d60a200cdb6afe2ac6f10abed247ec0c2fbec9380a2c77742bb48065b2d` |

---

## 3. Cryptographic Signing Details

| Signing Property | Value |
| :--- | :--- |
| **Keystore Location** | `E:\2.Things\Android-Projects\keys\WayMate\waymate-beta.jks` (Outside Git) |
| **Key Alias** | `waymate-beta` |
| **Key Algorithm** | RSA 2048-bit (PKCS12) |
| **Signing Scheme** | APK Signature Scheme v2 (Verified: true) |
| **Signer Certificate DN** | `CN=WayMate Beta Tester, OU=Mobile Engineering, O=WayMate, L=Jahrom, ST=Fars, C=IR` |
| **Certificate SHA-256 Fingerprint** | `0D:AE:13:99:10:91:81:D9:F2:56:35:94:30:4B:60:A0:B9:E5:06:70:40:15:73:9F:12:D8:46:F4:7C:34:6D:E1` |
| **Certificate SHA-1 Fingerprint** | `0C:B9:A7:53:A0:01:60:5A:65:A0:3B:47:94:EC:AC:AD:AF:C7:56:12` |
| **Validity Period** | Valid until March 24, 2054 |

---

## 4. Backend Edge Binding Configuration

| Service | Target Production URL | Notes |
| :--- | :--- | :--- |
| **REST API Base URL** | `https://waymate-backend.hawkroid.workers.dev/api/v1/` | HTTPS TLS 1.3 |
| **WebSocket Location URL** | `wss://waymate-backend.hawkroid.workers.dev/api/v1/ws/locations` | TLS WSS Connection |
| **Database Binding** | Cloudflare D1 (`DB`) | Remote production D1 |
| **Durable Object** | `LocationSessionDO` (`LOCATION_SESSIONS`) | Real-time coordinate fan-out |

---

## 5. Build Environment Specifications

- **Gradle:** 9.0 (via `gradlew.bat`)
- **Android Gradle Plugin (AGP):** 9.4.1
- **Kotlin Compiler:** 2.2.10
- **Java Runtime:** OpenJDK 17.0.12 (Eclipse Adoptium)
- **Host System:** Windows 11 Enterprise (64-bit)

