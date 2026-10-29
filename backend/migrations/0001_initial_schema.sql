-- Migration: 0001_initial_schema.sql
-- Cloudflare D1 SQLite Compatible Schema for WayMate

PRAGMA foreign_keys = ON;

-- 1. Users Table
CREATE TABLE IF NOT EXISTS users (
    id TEXT PRIMARY KEY,
    username TEXT NOT NULL COLLATE NOCASE,
    password_hash TEXT NOT NULL,
    role TEXT NOT NULL CHECK (role IN ('OWNER', 'COMMANDER', 'TEAM_LEAD')),
    callsign TEXT NOT NULL,
    is_active INTEGER NOT NULL DEFAULT 1 CHECK (is_active IN (0, 1)),
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_users_username ON users (username);
CREATE INDEX IF NOT EXISTS idx_users_role ON users (role);

-- 2. Teams Table
CREATE TABLE IF NOT EXISTS teams (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    lead_user_id TEXT NOT NULL,
    vehicle_type TEXT NOT NULL,
    vehicle_plate TEXT NOT NULL,
    equipment_summary TEXT NOT NULL DEFAULT '',
    is_active INTEGER NOT NULL DEFAULT 1 CHECK (is_active IN (0, 1)),
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL,
    FOREIGN KEY (lead_user_id) REFERENCES users (id) ON DELETE RESTRICT
);

CREATE INDEX IF NOT EXISTS idx_teams_lead_user_id ON teams (lead_user_id);
CREATE INDEX IF NOT EXISTS idx_teams_is_active ON teams (is_active);

-- 3. Team Members Table (Personnel without client apps)
CREATE TABLE IF NOT EXISTS team_members (
    id TEXT PRIMARY KEY,
    team_id TEXT NOT NULL,
    full_name TEXT NOT NULL,
    callsign TEXT NOT NULL,
    role_in_team TEXT NOT NULL,
    created_at INTEGER NOT NULL,
    FOREIGN KEY (team_id) REFERENCES teams (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_team_members_team_id ON team_members (team_id);

-- 4. Telemetry Pings Table (High-volume Blue Force Tracking stream)
CREATE TABLE IF NOT EXISTS telemetry_pings (
    id TEXT PRIMARY KEY,
    team_id TEXT NOT NULL,
    user_id TEXT NOT NULL,
    latitude REAL NOT NULL CHECK (latitude BETWEEN -90.0 AND 90.0),
    longitude REAL NOT NULL CHECK (longitude BETWEEN -180.0 AND 180.0),
    accuracy_meters REAL NOT NULL CHECK (accuracy_meters >= 0.0),
    speed_mps REAL NOT NULL DEFAULT 0.0 CHECK (speed_mps >= 0.0),
    heading_degrees REAL NOT NULL DEFAULT 0.0 CHECK (heading_degrees BETWEEN 0.0 AND 360.0),
    operational_status TEXT NOT NULL DEFAULT 'NORMAL' CHECK (operational_status IN ('NORMAL', 'PURSUING', 'ENGAGED')),
    recorded_at INTEGER NOT NULL,
    received_at INTEGER NOT NULL,
    FOREIGN KEY (team_id) REFERENCES teams (id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_telemetry_team_recorded ON telemetry_pings (team_id, recorded_at DESC);
CREATE INDEX IF NOT EXISTS idx_telemetry_received_at ON telemetry_pings (received_at DESC);

-- 5. Incidents Table
CREATE TABLE IF NOT EXISTS incidents (
    id TEXT PRIMARY KEY,
    team_id TEXT NOT NULL,
    reporter_user_id TEXT NOT NULL,
    title TEXT NOT NULL,
    description TEXT NOT NULL,
    incident_type TEXT NOT NULL CHECK (incident_type IN (
        'SUSPICIOUS_PERSON', 'VEHICLE_TRESPASS', 'ROAD_HAZARD', 'SECURITY_BREACH', 'QUICK_ALERT', 'OTHER'
    )),
    severity TEXT NOT NULL CHECK (severity IN ('ROUTINE', 'WARNING', 'URGENT')),
    latitude REAL NOT NULL CHECK (latitude BETWEEN -90.0 AND 90.0),
    longitude REAL NOT NULL CHECK (longitude BETWEEN -180.0 AND 180.0),
    status TEXT NOT NULL DEFAULT 'OPEN' CHECK (status IN ('OPEN', 'IN_PROGRESS', 'RESOLVED')),
    created_at INTEGER NOT NULL,
    resolved_at INTEGER,
    FOREIGN KEY (team_id) REFERENCES teams (id) ON DELETE RESTRICT,
    FOREIGN KEY (reporter_user_id) REFERENCES users (id) ON DELETE RESTRICT
);

CREATE INDEX IF NOT EXISTS idx_incidents_status_created ON incidents (status, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_incidents_team ON incidents (team_id);

-- 6. Tactical Messages Table
CREATE TABLE IF NOT EXISTS tactical_messages (
    id TEXT PRIMARY KEY,
    sender_user_id TEXT NOT NULL,
    target_team_id TEXT, -- NULL denotes broadcast to all teams
    body TEXT NOT NULL,
    is_urgent INTEGER NOT NULL DEFAULT 0 CHECK (is_urgent IN (0, 1)),
    created_at INTEGER NOT NULL,
    acknowledged_at INTEGER,
    FOREIGN KEY (sender_user_id) REFERENCES users (id) ON DELETE RESTRICT,
    FOREIGN KEY (target_team_id) REFERENCES teams (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_messages_target_created ON tactical_messages (target_team_id, created_at DESC);

-- 7. Audit Event Logs Table
CREATE TABLE IF NOT EXISTS audit_logs (
    id TEXT PRIMARY KEY,
    actor_user_id TEXT,
    event_type TEXT NOT NULL CHECK (event_type IN (
        'AUTH_LOGIN_SUCCESS', 'AUTH_LOGIN_FAILED', 'PERMISSION_DENIED',
        'EMERGENCY_ALERT_DISPATCHED', 'TEAM_CREATED', 'TEAM_UPDATED', 'INCIDENT_RESOLVED'
    )),
    resource_type TEXT NOT NULL,
    resource_id TEXT NOT NULL,
    ip_address TEXT NOT NULL,
    details_json TEXT NOT NULL DEFAULT '{}',
    created_at INTEGER NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_audit_logs_event_created ON audit_logs (event_type, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_audit_logs_created_at ON audit_logs (created_at DESC);
