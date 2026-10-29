-- Migration: 0002_seed_demo_data.sql
-- Synthetic Seed Data for Development and Testing Only

INSERT OR IGNORE INTO users (id, username, password_hash, role, callsign, is_active, created_at, updated_at) VALUES
('usr_owner_001', 'owner_demo', 'pbkdf2_sha256$100000$tactical_salt_2026$d28d64f188a0c41972d60c5c430594548d080bdbb0bf4da2d13fa48c167d115e', 'OWNER', 'Alpha-Base (متولی اصلی)', 1, 1729000000000, 1729000000000),
('usr_cmd_001', 'commander_demo', 'pbkdf2_sha256$100000$tactical_salt_2026$d28d64f188a0c41972d60c5c430594548d080bdbb0bf4da2d13fa48c167d115e', 'COMMANDER', 'Eagle-1 (فرمانده)', 1, 1729000000000, 1729000000000),
('usr_lead_001', 'patrol_lead_demo', 'pbkdf2_sha256$100000$tactical_salt_2026$d28d64f188a0c41972d60c5c430594548d080bdbb0bf4da2d13fa48c167d115e', 'TEAM_LEAD', 'Falcon-Lead (سرتیم آلفا)', 1, 1729000000000, 1729000000000);

INSERT OR IGNORE INTO teams (id, name, lead_user_id, vehicle_type, vehicle_plate, equipment_summary, is_active, created_at, updated_at) VALUES
('team_alpha_001', 'گشت آلفا (واکنش سریع)', 'usr_lead_001', 'تویوتا هایلوکس', '۴۴-ب-۱۲', 'بی‌سیم فرکانس اختصاصی، جعبه امداد تاکتیکی', 1, 1729000000000, 1729000000000);

INSERT OR IGNORE INTO team_members (id, team_id, full_name, callsign, role_in_team, created_at) VALUES
('mem_001', 'team_alpha_001', 'رضا کریمی', 'البرز ۱', 'راننده و اپراتور فنی', 1729000000000),
('mem_002', 'team_alpha_001', 'محمد حسینی', 'سهند ۲', 'دیده‌بان و سرتیم', 1729000000000);
