/**
 * WebSocket connection management hook
 * Handles connection lifecycle, reconnection, and heartbeat
 */

import { logger } from "@/lib/logger";
import { WEBSOCKET_CONFIG } from "@/config/websocket";
import { useCallback, useEffect, useRef, useState } from "react";

export type ConnectionStatus = "online" | "offline" | "connecting";

interface WebSocketConfig {
  url: string;
  token?: string;
  heartbeatInterval?: number;
  reconnectDelay?: number;
  maxReconnectAttempts?: number;
}

interface UseWebSocketConnectionReturn {
  status: ConnectionStatus;
  ws: WebSocket | null;
  connect: (config: WebSocketConfig) => void;
  disconnect: () => void;
  error: string | null;
}

export const useWebSocketConnection = (): UseWebSocketConnectionReturn => {
  const [status, setStatus] = useState<ConnectionStatus>("offline");
  const [error, setError] = useState<string | null>(null);
  const wsRef = useRef<WebSocket | null>(null);
  const configRef = useRef<WebSocketConfig | null>(null);
  const heartbeatIntervalRef = useRef<ReturnType<typeof setInterval> | null>(null);
  const reconnectTimeoutRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const reconnectAttemptsRef = useRef(0);

  const cleanup = useCallback(() => {
    if (heartbeatIntervalRef.current) {
      clearInterval(heartbeatIntervalRef.current);
      heartbeatIntervalRef.current = null;
    }
    if (reconnectTimeoutRef.current) {
      clearTimeout(reconnectTimeoutRef.current);
      reconnectTimeoutRef.current = null;
    }
    if (wsRef.current) {
      wsRef.current.close();
      wsRef.current = null;
    }
  }, []);

  const startHeartbeat = useCallback(() => {
    if (heartbeatIntervalRef.current) {
      clearInterval(heartbeatIntervalRef.current);
    }
    const interval = configRef.current?.heartbeatInterval || WEBSOCKET_CONFIG.HEARTBEAT_INTERVAL_MS;
    heartbeatIntervalRef.current = setInterval(() => {
      if (wsRef.current?.readyState === WebSocket.OPEN) {
        wsRef.current.send("ping");
      }
    }, interval);
  }, []);

  const connect = useCallback(
    (config: WebSocketConfig) => {
      logger.debug("useWebSocketConnection: connecting", { url: config.url });

      cleanup();
      configRef.current = config;
      setStatus("connecting");
      setError(null);
      reconnectAttemptsRef.current = 0;

      try {
        let urlToUse = config.url;
        if (config.token) {
          const sep = config.url.includes("?") ? "&" : "?";
          urlToUse = `${config.url}${sep}token=${encodeURIComponent(config.token)}`;
        }

        const ws = new WebSocket(urlToUse);
        wsRef.current = ws;

        ws.onopen = () => {
          setStatus("online");
          setError(null);
          reconnectAttemptsRef.current = 0;
          startHeartbeat();
          logger.info("WebSocket connected");
        };

        ws.onerror = (event) => {
          logger.error("WebSocket error:", event);
          setError("Connection error");
        };

        ws.onclose = (event) => {
          setStatus("offline");
          wsRef.current = null;

          if (heartbeatIntervalRef.current) {
            clearInterval(heartbeatIntervalRef.current);
            heartbeatIntervalRef.current = null;
          }

          // Auto-reconnect logic
          if (
            configRef.current &&
            reconnectAttemptsRef.current <
              (configRef.current.maxReconnectAttempts || WEBSOCKET_CONFIG.MAX_RECONNECT_ATTEMPTS)
          ) {
            const delay =
              (configRef.current.reconnectDelay || WEBSOCKET_CONFIG.RECONNECT_BASE_DELAY_MS) *
              Math.pow(2, reconnectAttemptsRef.current);
            reconnectAttemptsRef.current++;

            logger.info(
              `Reconnecting in ${delay}ms (attempt ${reconnectAttemptsRef.current})`
            );

            reconnectTimeoutRef.current = setTimeout(() => {
              if (configRef.current) {
                connect(configRef.current);
              }
            }, delay);
          } else {
            setError("Connection lost. Please reconnect manually.");
            logger.warn("Max reconnect attempts reached");
          }
        };
      } catch (e) {
        setStatus("offline");
        setError(e instanceof Error ? e.message : "Failed to connect");
        logger.error("Connection failed:", e);
      }
    },
    [cleanup, startHeartbeat]
  );

  const disconnect = useCallback(() => {
    cleanup();
    setStatus("offline");
    setError(null);
    reconnectAttemptsRef.current = 0;
    logger.info("WebSocket disconnected");
  }, [cleanup]);

  useEffect(() => {
    return () => {
      cleanup();
    };
  }, [cleanup]);

  return {
    status,
    ws: wsRef.current,
    connect,
    disconnect,
    error,
  };
};
