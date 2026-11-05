-- Migration: 0003_points_of_interest.sql
-- Group Points of Interest (POI) for shared hiking and outdoor exploration

PRAGMA foreign_keys = ON;

CREATE TABLE IF NOT EXISTS points_of_interest (
    id TEXT PRIMARY KEY,
    team_id TEXT NOT NULL,
    created_by_user_id TEXT NOT NULL,
    updated_by_user_id TEXT NOT NULL,
    title TEXT NOT NULL,
    description TEXT NOT NULL DEFAULT '',
    category TEXT NOT NULL CHECK (category IN (
        'START_POINT', 'DESTINATION', 'CAMPSITE', 'REST_AREA', 'WATER_SOURCE', 'GENERAL'
    )),
    latitude REAL NOT NULL CHECK (latitude BETWEEN -90.0 AND 90.0),
    longitude REAL NOT NULL CHECK (longitude BETWEEN -180.0 AND 180.0),
    is_deleted INTEGER NOT NULL DEFAULT 0 CHECK (is_deleted IN (0, 1)),
    version INTEGER NOT NULL DEFAULT 1 CHECK (version >= 1),
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL,
    FOREIGN KEY (team_id) REFERENCES teams (id) ON DELETE CASCADE,
    FOREIGN KEY (created_by_user_id) REFERENCES users (id) ON DELETE RESTRICT,
    FOREIGN KEY (updated_by_user_id) REFERENCES users (id) ON DELETE RESTRICT
);

CREATE INDEX IF NOT EXISTS idx_poi_team_active ON points_of_interest (team_id, is_deleted, updated_at DESC);
CREATE INDEX IF NOT EXISTS idx_poi_created_by ON points_of_interest (created_by_user_id);
CREATE INDEX IF NOT EXISTS idx_poi_updated_at ON points_of_interest (updated_at DESC);

-- Synthetic Demonstration POIs for Hiking & Outdoor Exploration (Jahrom Demo Area)
INSERT OR IGNORE INTO points_of_interest (
    id, team_id, created_by_user_id, updated_by_user_id, title, description, category, latitude, longitude, is_deleted, version, created_at, updated_at
) VALUES
(
    'poi_demo_001',
    'team_alpha_001',
    'usr_lead_001',
    'usr_lead_001',
    'نقطه شروع پیمایش غار سنگ‌شکنان',
    'محل تجمع اولیه گروه، پارکینگ خودروها و بررسی تجهیزات',
    'START_POINT',
    28.4870,
    53.5540,
    0,
    1,
    1729000000000,
    1729000000000
),
(
    'poi_demo_002',
    'team_alpha_001',
    'usr_lead_001',
    'usr_lead_001',
    'چشمه و استراحت‌گاه کوهستان قلات',
    'منبع آب آشامیدنی طبیعی و سایه‌بان مناسب برای استراحت میانی',
    'WATER_SOURCE',
    28.5120,
    53.5830,
    0,
    1,
    1729000000000,
    1729000000000
),
(
    'poi_demo_003',
    'team_alpha_001',
    'usr_cmd_001',
    'usr_cmd_001',
    'محل کمپ شبانه چاتیز',
    'فضای مسطح با دسترسی به مسیر پاکوب مناسب برای برپایی چادرهای اقامتی',
    'CAMPSITE',
    28.5340,
    53.6120,
    0,
    1,
    1729000000000,
    1729000000000
);

-- Upgrade audit_logs table to allow POI audit event types
CREATE TABLE IF NOT EXISTS audit_logs_new (
    id TEXT PRIMARY KEY,
    actor_user_id TEXT,
    event_type TEXT NOT NULL CHECK (event_type IN (
        'AUTH_LOGIN_SUCCESS', 'AUTH_LOGIN_FAILED', 'PERMISSION_DENIED',
        'EMERGENCY_ALERT_DISPATCHED', 'TEAM_CREATED', 'TEAM_UPDATED', 'INCIDENT_RESOLVED',
        'POI_CREATED', 'POI_UPDATED', 'POI_DELETED'
    )),
    resource_type TEXT NOT NULL,
    resource_id TEXT NOT NULL,
    ip_address TEXT NOT NULL,
    details_json TEXT NOT NULL DEFAULT '{}',
    created_at INTEGER NOT NULL
);

INSERT OR IGNORE INTO audit_logs_new SELECT * FROM audit_logs;
DROP TABLE audit_logs;
ALTER TABLE audit_logs_new RENAME TO audit_logs;

CREATE INDEX IF NOT EXISTS idx_audit_logs_event_created ON audit_logs (event_type, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_audit_logs_created_at ON audit_logs (created_at DESC);

