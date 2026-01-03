/**
 * WebSocket configuration constants
 */

export const WEBSOCKET_CONFIG = {
  // Timeouts
  MESSAGE_TIMEOUT_MS: 5000,
  HEARTBEAT_INTERVAL_MS: 15000,

  // Reconnection
  MAX_RECONNECT_ATTEMPTS: 6,
  RECONNECT_BASE_DELAY_MS: 1000, // Exponential backoff base

  // Message size
  MAX_MESSAGE_SIZE: 102400, // 100KB
} as const;

export const WEBSOCKET_CLOSE_CODES = {
  NORMAL: 1000,
  UNAUTHORIZED: 4001,
  MESSAGE_TOO_BIG: 1009,
  RATE_LIMITED: 4029,
} as const;

export const MESSAGE_TYPES = {
  ACK: "ack",
  ERROR: "error",
  PROFILE_SELECT: "profile:select",
  PROFILE_SELECT_ACK: "profile:select:ack",
  CONTROL_STATE: "control:state",
} as const;

export const STATUS_VALUES = {
  OK: "ok",
  ERROR: "error",
  IGNORED: "ignored",
} as const;
