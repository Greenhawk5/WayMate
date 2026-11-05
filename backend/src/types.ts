export * from './types/location.js';

/**
 * Environment bindings injected by Cloudflare Workers runtime.
 */
export interface Bindings {
  DB: D1Database;
  ENVIRONMENT: string;
  API_VERSION: string;
  JWT_SECRET?: string;
  LOCATION_SESSIONS?: DurableObjectNamespace;
}

/**
 * Standard authenticated user session payload stored in Hono context.
 */
export interface AuthUser {
  id: string;
  username: string;
  role: 'OWNER' | 'COMMANDER' | 'TEAM_LEAD';
  callsign: string;
}

export interface Variables {
  user?: AuthUser;
  requestId: string;
}

/**
 * Common API Response Envelopes
 */
export interface ApiResponse<T> {
  success: true;
  data: T;
  meta: {
    timestamp: number;
    request_id: string;
  };
}

export interface PaginatedResponse<T> {
  success: true;
  data: T[];
  pagination: {
    offset: number;
    limit: number;
    total: number;
    has_more: boolean;
  };
  meta: {
    timestamp: number;
    request_id: string;
  };
}

export interface ApiFieldError {
  field: string;
  issue: string;
}

export interface ApiErrorResponse {
  success: false;
  error: {
    code: string;
    message: string;
    details?: ApiFieldError[];
    timestamp: number;
    request_id: string;
  };
}

/**
 * Database Row Types
 */
export interface DbUser {
  id: string;
  username: string;
  password_hash: string;
  role: 'OWNER' | 'COMMANDER' | 'TEAM_LEAD';
  callsign: string;
  is_active: number;
  created_at: number;
  updated_at: number;
}

export interface DbTeam {
  id: string;
  name: string;
  lead_user_id: string;
  vehicle_type: string;
  vehicle_plate: string;
  equipment_summary: string;
  is_active: number;
  created_at: number;
  updated_at: number;
}

export interface DbTeamMember {
  id: string;
  team_id: string;
  full_name: string;
  callsign: string;
  role_in_team: string;
  created_at: number;
}

export interface DbTelemetryPing {
  id: string;
  team_id: string;
  user_id: string;
  latitude: number;
  longitude: number;
  accuracy_meters: number;
  speed_mps: number;
  heading_degrees: number;
  operational_status: 'NORMAL' | 'PURSUING' | 'ENGAGED';
  recorded_at: number;
  received_at: number;
}

export interface DbIncident {
  id: string;
  team_id: string;
  reporter_user_id: string;
  title: string;
  description: string;
  incident_type: 'SUSPICIOUS_PERSON' | 'VEHICLE_TRESPASS' | 'ROAD_HAZARD' | 'SECURITY_BREACH' | 'QUICK_ALERT' | 'OTHER';
  severity: 'ROUTINE' | 'WARNING' | 'URGENT';
  latitude: number;
  longitude: number;
  status: 'OPEN' | 'IN_PROGRESS' | 'RESOLVED';
  created_at: number;
  resolved_at: number | null;
}

export interface DbTacticalMessage {
  id: string;
  sender_user_id: string;
  target_team_id: string | null;
  body: string;
  is_urgent: number;
  created_at: number;
  acknowledged_at: number | null;
}

export interface DbAuditLog {
  id: string;
  actor_user_id: string | null;
  event_type: string;
  resource_type: string;
  resource_id: string;
  ip_address: string;
  details_json: string;
  created_at: number;
}

export type PointCategory =
  | 'START_POINT'
  | 'DESTINATION'
  | 'CAMPSITE'
  | 'REST_AREA'
  | 'WATER_SOURCE'
  | 'GENERAL';

export interface DbPointOfInterest {
  id: string;
  team_id: string;
  created_by_user_id: string;
  updated_by_user_id: string;
  title: string;
  description: string;
  category: PointCategory;
  latitude: number;
  longitude: number;
  is_deleted: number;
  version: number;
  created_at: number;
  updated_at: number;
}
