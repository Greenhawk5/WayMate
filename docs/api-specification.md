# WayMate — REST API Contract & Interface Specification (v1)

## 1. Overview & Protocol Architecture

This document defines the formal REST API contract for the WayMate serverless edge backend hosted on **Cloudflare Workers**. It governs all client-to-server data exchange between the Android native application and the backend API.

- **Base URL**: `https://api.waymate.internal/api/v1` (Intranet / Sovereign Edge)
- **Protocol**: HTTPS / TLS 1.3
- **Data Format**: `application/json; charset=utf-8`
- **Authentication**: Bearer Token (signed HMAC-SHA256 JWT in `Authorization: Bearer <token>` header)
- **Versioning Strategy**: URI Path versioning (`/api/v1/`) supplemented by `Accept: application/vnd.waymate.v1+json`.

---

## 2. Standard Envelopes & Response Schemas

### 2.1 Standard Success Envelope
For resource mutations or single entity responses:
```json
{
  "success": true,
  "data": { ... },
  "meta": {
    "timestamp": 1729000000000,
    "request_id": "req_01j7x8k2m9..."
  }
}
```

### 2.2 Standard Paginated List Envelope
For collection endpoints supporting pagination:
```json
{
  "success": true,
  "data": [ ... ],
  "pagination": {
    "offset": 0,
    "limit": 50,
    "total": 128,
    "has_more": true
  },
  "meta": {
    "timestamp": 1729000000000,
    "request_id": "req_01j7x8k2m9..."
  }
}
```

### 2.3 Standard Error Envelope (RFC-7807 Aligned)
Every non-2xx response returns a deterministic error object:
```json
{
  "success": false,
  "error": {
    "code": "VALIDATION_FAILED",
    "message": "یک یا چند پارامتر ارسالی نامعتبر است",
    "details": [
      {
        "field": "latitude",
        "issue": "مقدار عرض جغرافیایی باید بین ۹۰- تا ۹۰+ باشد"
      }
    ],
    "timestamp": 1729000000000,
    "request_id": "req_01j7x8k2m9..."
  }
}
```

#### Standard Error Codes
| HTTP Status | Error Code | Description |
| :--- | :--- | :--- |
| **400** | `BAD_REQUEST` | Malformed JSON or syntax failure. |
| **400** | `VALIDATION_FAILED` | Field constraints violated (e.g. invalid coordinates, missing fields). |
| **401** | `UNAUTHORIZED` | Missing, invalid, or expired authentication token. |
| **403** | `FORBIDDEN` | Role lacks permission for this resource or team scope. |
| **404** | `NOT_FOUND` | Requested entity does not exist. |
| **409** | `CONFLICT` | Unique key violation (e.g. duplicate username). |
| **422** | `TELEMETRY_REJECTED` | GPS fix rejected due to excessive accuracy degradation (> 50m) or clock drift. |
| **429** | `RATE_LIMITED` | Request quota exceeded (e.g. > 10 pings/sec per client). |
| **500** | `INTERNAL_ERROR` | Unexpected server error. |
| **503** | `SERVICE_UNAVAILABLE` | Database or upstream edge worker unreachable. |

---

## 3. Endpoints Specification

### 3.1 Authentication & Session Management

#### `POST /api/v1/auth/login`
Authenticates user credentials and issues a signed JWT.

- **Access**: Public
- **Request Body**:
  ```json
  {
    "username": "commander_demo",
    "password": "demo123"
  }
  ```
- **Validation Rules**:
  - `username`: 3–32 characters, alphanumeric and underscore only.
  - `password`: 6–128 characters.
- **Response (200 OK)**:
  ```json
  {
    "success": true,
    "data": {
      "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
      "token_type": "Bearer",
      "expires_at": 1729086400000,
      "user": {
        "id": "usr_cmd_001",
        "username": "commander_demo",
        "role": "COMMANDER",
        "callsign": "Eagle-1 (فرمانده)"
      }
    },
    "meta": { "timestamp": 1729000000000 }
  }
  ```
- **Errors**:
  - `401 UNAUTHORIZED`: "نام کاربری یا گذرواژه اشتباه است" (Generic message to prevent account enumeration).

#### `GET /api/v1/auth/me`
Fetches the active session profile and permission claims.

- **Access**: Authenticated (`OWNER`, `COMMANDER`, `TEAM_LEAD`)
- **Response (200 OK)**:
  ```json
  {
    "success": true,
    "data": {
      "id": "usr_cmd_001",
      "username": "commander_demo",
      "role": "COMMANDER",
      "callsign": "Eagle-1 (فرمانده)",
      "is_active": true
    }
  }
  ```

#### `POST /api/v1/auth/logout`
Revokes the current session token on the server.

- **Access**: Authenticated
- **Response (200 OK)**:
  ```json
  {
    "success": true,
    "data": { "message": "خروج با موفقیت انجام شد" }
  }
  ```

---

### 3.2 Teams & Organizational Roster

#### `GET /api/v1/teams`
Lists all active tactical teams in the operational perimeter.

- **Access**: `COMMANDER`, `OWNER`
- **Query Parameters**:
  - `offset` (integer, default: 0)
  - `limit` (integer, default: 50, max: 100)
- **Response (200 OK)**:
  ```json
  {
    "success": true,
    "data": [
      {
        "id": "team_alpha_001",
        "name": "گشت آلفا (واکنش سریع)",
        "lead_user_id": "usr_lead_001",
        "vehicle_type": "تویوتا هایلوکس",
        "vehicle_plate": "۴۴-ب-۱۲",
        "equipment_summary": "بی‌سیم فرکانس اختصاصی، جعبه امداد تاکتیکی",
        "member_count": 3,
        "is_active": true
      }
    ],
    "pagination": { "offset": 0, "limit": 50, "total": 1, "has_more": false }
  }
  ```

#### `GET /api/v1/teams/{id}`
Retrieves detailed team metadata and roster members.

- **Access**: Authenticated (`COMMANDER`, `OWNER`, or the assigned `TEAM_LEAD`)
- **Response (200 OK)**:
  ```json
  {
    "success": true,
    "data": {
      "id": "team_alpha_001",
      "name": "گشت آلفا (واکنش سریع)",
      "vehicle_type": "تویوتا هایلوکس",
      "vehicle_plate": "۴۴-ب-۱۲",
      "equipment_summary": "بی‌سیم فرکانس اختصاصی، جعبه امداد تاکتیکی",
      "members": [
        {
          "id": "mem_001",
          "full_name": "رضا کریمی",
          "callsign": "البرز ۱",
          "role_in_team": "فرمانده عملیات"
        },
        {
          "id": "mem_002",
          "full_name": "محمد حسینی",
          "callsign": "سهند ۲",
          "role_in_team": "سرتیم گشت"
        }
      ]
    }
  }
  ```

---

### 3.3 Telemetry & Blue Force Tracking

#### `POST /api/v1/telemetry/location`
Ingests a single real-time GPS telemetry point from an active field unit.

- **Access**: `TEAM_LEAD`
- **Request Body**:
  ```json
  {
    "latitude": 35.6961,
    "longitude": 51.4231,
    "accuracy_meters": 4.5,
    "speed_mps": 8.2,
    "heading_degrees": 355.0,
    "operational_status": "NORMAL",
    "recorded_at": 1729000000000
  }
  ```
- **Validation Rules**:
  - `latitude`: -90.0 to +90.0.
  - `longitude`: -180.0 to +180.0.
  - `accuracy_meters`: >= 0.0 (points with accuracy > 50.0m trigger warning or rejection).
  - `speed_mps`: >= 0.0.
  - `heading_degrees`: 0.0 to 360.0.
  - `operational_status`: Must be one of `NORMAL`, `PURSUING`, `ENGAGED`.
  - `recorded_at`: Timestamp must not drift > +/- 3600 seconds from server clock.
- **Response (201 Created)**:
  ```json
  {
    "success": true,
    "data": { "ping_id": "ping_01j7x9k..." }
  }
  ```

#### `POST /api/v1/telemetry/batch`
Ingests store-and-forward buffered pings replayed after an offline period.

- **Access**: `TEAM_LEAD`
- **Request Body**:
  ```json
  {
    "pings": [
      {
        "latitude": 35.6961,
        "longitude": 51.4231,
        "accuracy_meters": 5.0,
        "speed_mps": 6.1,
        "heading_degrees": 180.0,
        "operational_status": "NORMAL",
        "recorded_at": 1728999800000
      },
      {
        "latitude": 35.6968,
        "longitude": 51.4235,
        "accuracy_meters": 4.8,
        "speed_mps": 7.0,
        "heading_degrees": 182.0,
        "operational_status": "NORMAL",
        "recorded_at": 1728999820000
      }
    ]
  }
  ```
- **Validation Rules**: Maximum 100 pings per batch.
- **Response (200 OK)**:
  ```json
  {
    "success": true,
    "data": { "inserted_count": 2 }
  }
  ```

#### `GET /api/v1/telemetry/live`
Polls current positions of all active field teams for Commander Situational Map.

- **Access**: `COMMANDER`, `OWNER`
- **Response (200 OK)**:
  ```json
  {
    "success": true,
    "data": [
      {
        "team_id": "team_alpha_001",
        "team_name": "گشت آلفا (واکنش سریع)",
        "callsign": "Falcon-Lead",
        "latitude": 35.6968,
        "longitude": 51.4235,
        "accuracy_meters": 4.8,
        "speed_mps": 7.0,
        "heading_degrees": 182.0,
        "operational_status": "NORMAL",
        "last_seen_at": 1728999820000,
        "is_stale": false
      }
    ]
  }
  ```

---

### 3.4 Incidents & Emergency Quick Action

#### `POST /api/v1/incidents/quick`
1-tap instant emergency alert trigger.

- **Access**: `TEAM_LEAD`
- **Request Body**:
  ```json
  {
    "latitude": 35.6968,
    "longitude": 51.4235,
    "preset_reason": "مورد مشکوک تاکتیکی رویت شد"
  }
  ```
- **Response (201 Created)**:
  ```json
  {
    "success": true,
    "data": {
      "incident_id": "inc_01j7xa9...",
      "severity": "URGENT",
      "status": "OPEN",
      "acknowledged": true
    }
  }
  ```

#### `POST /api/v1/incidents`
Submits structured tactical observation report.

- **Access**: `TEAM_LEAD`, `COMMANDER`
- **Request Body**:
  ```json
  {
    "title": "مشاهده خودرو مشکوک",
    "description": "پژو ۴۰۵ نقره‌ای فاقد پلاک در محدوده حفاظتی تردد داشت",
    "incident_type": "VEHICLE_TRESPASS",
    "severity": "WARNING",
    "latitude": 35.6970,
    "longitude": 51.4240
  }
  ```
- **Response (201 Created)**:
  ```json
  {
    "success": true,
    "data": { "incident_id": "inc_01j7xb2...", "status": "OPEN" }
  }
  ```

---

### 3.5 Tactical Messaging & Orders

#### `GET /api/v1/messages`
Fetches pending operational orders and broadcast instructions.

- **Access**: Authenticated
- **Response (200 OK)**:
  ```json
  {
    "success": true,
    "data": [
      {
        "id": "msg_001",
        "sender_callsign": "Eagle-1 (فرمانده)",
        "body": "واحد آلفا به تقاطع شمالی اعزام و وضعیت را کنترل نماید.",
        "is_urgent": true,
        "created_at": 1729000010000,
        "acknowledged": false
      }
    ]
  }
  ```

#### `POST /api/v1/messages/{id}/acknowledge`
Transmits read confirmation for an operational instruction.

- **Access**: Authenticated (`TEAM_LEAD`)
- **Response (200 OK)**:
  ```json
  {
    "success": true,
    "data": { "acknowledged_at": 1729000020000 }
  }
  ```

---

### 3.6 Infrastructure Health

#### `GET /api/v1/health`
Edge health and database link verification.

- **Access**: Public
- **Response (200 OK)**:
  ```json
  {
    "success": true,
    "data": {
      "status": "HEALTHY",
      "database": "CONNECTED",
      "region": "THR",
      "version": "1.0.0",
      "timestamp": 1729000000000
    }
  }
  ```
