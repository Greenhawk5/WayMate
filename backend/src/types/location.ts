/**
 * Location Sharing WebSocket Protocol Types & Contracts (Version 1)
 */

export const LOCATION_PROTOCOL_VERSION = 1;

export type ClientLocationMessageType =
  | 'LOCATION_UPDATE'
  | 'SHARING_STARTED'
  | 'SHARING_STOPPED'
  | 'PING';

export type ServerLocationMessageType =
  | 'LOCATION_UPDATE'
  | 'SHARING_STARTED'
  | 'SHARING_STOPPED'
  | 'MEMBER_STATE'
  | 'INITIAL_ROSTER'
  | 'PONG'
  | 'ERROR';

export interface BaseLocationMessage {
  type: ClientLocationMessageType | ServerLocationMessageType;
  version: number;
}

/**
 * Incoming client location update payload.
 */
export interface ClientLocationUpdateMessage extends BaseLocationMessage {
  type: 'LOCATION_UPDATE';
  version: 1;
  seq: number;
  latitude: number;
  longitude: number;
  accuracy: number;
  altitude?: number | null;
  speed?: number | null;
  bearing?: number | null;
  timestamp: number;
}

export interface ClientSharingStartedMessage extends BaseLocationMessage {
  type: 'SHARING_STARTED';
  version: 1;
  seq: number;
  timestamp: number;
}

export interface ClientSharingStoppedMessage extends BaseLocationMessage {
  type: 'SHARING_STOPPED';
  version: 1;
  seq: number;
  timestamp: number;
}

export interface ClientPingMessage extends BaseLocationMessage {
  type: 'PING';
  version: 1;
  timestamp: number;
}

export type ClientMessage =
  | ClientLocationUpdateMessage
  | ClientSharingStartedMessage
  | ClientSharingStoppedMessage
  | ClientPingMessage;

/**
 * Outgoing server broadcast messages.
 */
export interface ServerLocationUpdateMessage extends BaseLocationMessage {
  type: 'LOCATION_UPDATE';
  version: 1;
  seq: number;
  userId: string;
  callsign: string;
  teamId: string;
  latitude: number;
  longitude: number;
  accuracy: number;
  altitude?: number | null;
  speed?: number | null;
  bearing?: number | null;
  timestamp: number;
  serverTimestamp: number;
}

export interface ServerSharingStartedMessage extends BaseLocationMessage {
  type: 'SHARING_STARTED';
  version: 1;
  userId: string;
  callsign: string;
  teamId: string;
  timestamp: number;
  serverTimestamp: number;
}

export interface ServerSharingStoppedMessage extends BaseLocationMessage {
  type: 'SHARING_STOPPED';
  version: 1;
  userId: string;
  callsign: string;
  teamId: string;
  timestamp: number;
  serverTimestamp: number;
}

export interface ActiveSharerSummary {
  userId: string;
  callsign: string;
  latitude: number;
  longitude: number;
  accuracy: number;
  altitude?: number | null;
  speed?: number | null;
  bearing?: number | null;
  timestamp: number;
}

export interface ServerInitialRosterMessage extends BaseLocationMessage {
  type: 'INITIAL_ROSTER';
  version: 1;
  teamId: string;
  activeSharers: ActiveSharerSummary[];
  serverTimestamp: number;
}

export interface ServerMemberStateMessage extends BaseLocationMessage {
  type: 'MEMBER_STATE';
  version: 1;
  userId: string;
  callsign: string;
  teamId: string;
  status: 'CONNECTED' | 'DISCONNECTED';
  isSharing: boolean;
  serverTimestamp: number;
}

export interface ServerPongMessage extends BaseLocationMessage {
  type: 'PONG';
  version: 1;
  timestamp: number;
  serverTimestamp: number;
}

export interface ServerErrorMessage extends BaseLocationMessage {
  type: 'ERROR';
  version: 1;
  code:
    | 'INVALID_MESSAGE'
    | 'UNSUPPORTED_VERSION'
    | 'INVALID_COORDINATES'
    | 'OUTDATED_TIMESTAMP'
    | 'RATE_LIMITED'
    | 'UNAUTHORIZED';
  message: string;
  timestamp: number;
}

export type ServerMessage =
  | ServerLocationUpdateMessage
  | ServerSharingStartedMessage
  | ServerSharingStoppedMessage
  | ServerInitialRosterMessage
  | ServerMemberStateMessage
  | ServerPongMessage
  | ServerErrorMessage;

/**
 * In-memory connection context for a connected WebSocket client.
 */
export interface ConnectedClient {
  ws: WebSocket;
  userId: string;
  callsign: string;
  teamId: string;
  isSharing: boolean;
  lastLocation?: ActiveSharerSummary;
  lastSeenSeq: number;
  lastSeenTimestamp: number;
  windowStart: number;
  messageCountInWindow: number;
}
