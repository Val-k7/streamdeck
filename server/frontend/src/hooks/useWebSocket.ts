import { logger } from "@/lib/logger";
import { useCallback, useEffect, useRef, useState } from "react";

export type ConnectionStatus = "online" | "offline" | "connecting";

interface WebSocketConfig {
  url: string;
  token?: string;
  heartbeatInterval?: number;
  reconnectDelay?: number;
  maxReconnectAttempts?: number;
}

interface ControlPayload {
  kind: "control";
  controlId: string;
  type: string;
  value: number;
  messageId: string;
  sentAt: number;
  meta?: Record<string, unknown>;
}

interface ProfileSelectPayload {
  kind: "profile:select";
  profileId: string;
  messageId: string;
  sentAt: number;
  resetState?: boolean;
}

interface AckPayload {
  type: "ack";
  messageId?: string;
  controlId?: string;
  status: "ok" | "error" | "ignored";
  error?: string;
  receivedAt?: number;
  processedAt?: number;
}

interface ProfileAckPayload {
  type: "ack";
  kind?: "profile:select:ack";
  messageId?: string;
  status: "ok" | "error";
  error?: string;
  profileId?: string;
}

interface ControlStatePayload {
  type: "control:state";
  controlId: string;
  value: number;
}

export interface UseWebSocketReturn {
  status: ConnectionStatus;
  connect: (config: WebSocketConfig) => void;
  disconnect: () => void;
  sendControl: (
    payload: Omit<ControlPayload, "kind" | "messageId" | "sentAt">
  ) => Promise<void>;
  selectProfile: (profileId: string, resetState?: boolean) => Promise<void>;
  onAck: (callback: (ack: AckPayload) => void) => void;
  onProfileAck: (callback: (ack: ProfileAckPayload) => void) => void;
  onControlState: (callback: (state: ControlStatePayload) => void) => void;
  error: string | null;
}

export const useWebSocket = (): UseWebSocketReturn => {
  const [status, setStatus] = useState<ConnectionStatus>("offline");
  const [error, setError] = useState<string | null>(null);
  const wsRef = useRef<WebSocket | null>(null);
  const configRef = useRef<WebSocketConfig | null>(null);
  const heartbeatIntervalRef = useRef<ReturnType<typeof setInterval> | null>(
    null
  );
  const reconnectTimeoutRef = useRef<ReturnType<typeof setTimeout> | null>(
    null
  );
  const reconnectAttemptsRef = useRef(0);
  const pendingMessagesRef = useRef<
    Map<
      string,
      {
        resolve: () => void;
        reject: (error: Error) => void;
        timeout: ReturnType<typeof setTimeout>;
      }
    >
  >(new Map());
  const ackCallbacksRef = useRef<Set<(ack: AckPayload) => void>>(new Set());
  const profileAckCallbacksRef = useRef<Set<(ack: ProfileAckPayload) => void>>(
    new Set()
  );
  const controlStateCallbacksRef = useRef<
    Set<(state: ControlStatePayload) => void>
  >(new Set());
  const statusCheckIntervalRef = useRef<number | null>(null);



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
    // Rejeter tous les messages en attente
    pendingMessagesRef.current.forEach(({ reject, timeout }) => {
      clearTimeout(timeout);
      reject(new Error("WebSocket disconnected"));
    });
    pendingMessagesRef.current.clear();
  }, []);

  const startHeartbeat = useCallback(() => {
    if (heartbeatIntervalRef.current) {
      clearInterval(heartbeatIntervalRef.current);
    }
    const interval = configRef.current?.heartbeatInterval || 15000;
    heartbeatIntervalRef.current = setInterval(() => {
      if (wsRef.current?.readyState === WebSocket.OPEN) {
        // Keepalive payload kept simple to avoid server-side parse errors
        wsRef.current.send("ping");
      }
    }, interval);
  }, []);

  const connect = useCallback(
    (config: WebSocketConfig) => {
      logger.debug("useWebSocket: connecting", { url: config.url });

      // Connexion via WebSocket natif du navigateur
      cleanup();
      configRef.current = config;
      setStatus("connecting");
      setError(null);
      reconnectAttemptsRef.current = 0;

      try {
        logger.debug("useWebSocket: connect via WebSocket", config.url);
        const ws = new WebSocket(config.url);
        wsRef.current = ws;

        ws.onopen = () => {
          setStatus("online");
          setError(null);
          reconnectAttemptsRef.current = 0;
          startHeartbeat();
        };

        ws.onmessage = (event) => {
          try {
            // Ignorer les messages de heartbeat en texte brut
            if (event.data === "pong") {
              return;
            }

            const data = JSON.parse(event.data);

            // Gérer les ACK
            if (data.type === "ack" || data.type === "profile:select:ack") {
              if (data.type === "profile:select:ack") {
                profileAckCallbacksRef.current.forEach((callback) =>
                  callback(data as ProfileAckPayload)
                );
              } else {
                ackCallbacksRef.current.forEach((callback) =>
                  callback(data as AckPayload)
                );
              }

              // Résoudre les messages en attente
              if (data.messageId) {
                const pending = pendingMessagesRef.current.get(data.messageId);
                if (pending) {
                  clearTimeout(pending.timeout);
                  if (data.status === "ok") {
                    pending.resolve();
                  } else {
                    pending.reject(new Error(data.error || "Action failed"));
                  }
                  pendingMessagesRef.current.delete(data.messageId);
                }
              }
            }

            // Gérer les mises à jour d'état des contrôles
            if (data.type === "control:state") {
              controlStateCallbacksRef.current.forEach((callback) =>
                callback(data as ControlStatePayload)
              );
            }
          } catch (e) {
            logger.error("Failed to parse WebSocket message:", e);
          }
        };

        ws.onerror = (event) => {
          logger.error("WebSocket error:", event);
          setError("Connection error");
          // Ne pas fermer la connexion ici, laisser onclose gérer
        };

        ws.onclose = (event) => {
          setStatus("offline");
          wsRef.current = null;

          if (heartbeatIntervalRef.current) {
            clearInterval(heartbeatIntervalRef.current);
            heartbeatIntervalRef.current = null;
          }

          // Tentative de reconnexion automatique
          if (
            configRef.current &&
            reconnectAttemptsRef.current <
              (configRef.current.maxReconnectAttempts || 6)
          ) {
            const delay =
              (configRef.current.reconnectDelay || 1000) *
              Math.pow(2, reconnectAttemptsRef.current);
            reconnectAttemptsRef.current++;

            reconnectTimeoutRef.current = setTimeout(() => {
              if (configRef.current) {
                connect(configRef.current);
              }
            }, delay);
          } else {
            setError("Connection lost. Please reconnect manually.");
          }
        };
      } catch (e) {
        setStatus("offline");
        setError(e instanceof Error ? e.message : "Failed to connect");
      }
    },
    [cleanup, startHeartbeat]
  );

  const disconnect = useCallback(() => {
    cleanup();
    setStatus("offline");
    setError(null);
    reconnectAttemptsRef.current = 0;
  }, [cleanup]);

  const sendControl = useCallback(
    async (payload: Omit<ControlPayload, "kind" | "messageId" | "sentAt">) => {

      // Fallback sur WebSocket standard
      if (wsRef.current?.readyState !== WebSocket.OPEN) {
        throw new Error("WebSocket is not connected");
      }

      const messageId = `${Date.now()}_${Math.random()
        .toString(36)
        .substr(2, 9)}`;
      const fullPayload: ControlPayload = {
        kind: "control",
        ...payload,
        messageId,
        sentAt: Date.now(),
      };

      return new Promise<void>((resolve, reject) => {
        const timeout = setTimeout(() => {
          pendingMessagesRef.current.delete(messageId);
          reject(new Error("Action timeout"));
        }, 5000);

        pendingMessagesRef.current.set(messageId, { resolve, reject, timeout });
        wsRef.current?.send(JSON.stringify(fullPayload));
      });
    },
    []
  );

  const selectProfile = useCallback(
    async (profileId: string, resetState = true) => {
      if (wsRef.current?.readyState !== WebSocket.OPEN) {
        throw new Error("WebSocket is not connected");
      }

      const messageId = `${Date.now()}_${Math.random()
        .toString(36)
        .substr(2, 9)}`;
      const payload: ProfileSelectPayload = {
        kind: "profile:select",
        profileId,
        messageId,
        sentAt: Date.now(),
        resetState,
      };

      return new Promise<void>((resolve, reject) => {
        const timeout = setTimeout(() => {
          pendingMessagesRef.current.delete(messageId);
          reject(new Error("Profile selection timeout"));
        }, 5000);

        pendingMessagesRef.current.set(messageId, { resolve, reject, timeout });
        wsRef.current?.send(JSON.stringify(payload));
      });
    },
    []
  );

  const onAck = useCallback((callback: (ack: AckPayload) => void) => {
    ackCallbacksRef.current.add(callback);
    return () => {
      ackCallbacksRef.current.delete(callback);
    };
  }, []);

  const onProfileAck = useCallback(
    (callback: (ack: ProfileAckPayload) => void) => {
      profileAckCallbacksRef.current.add(callback);
      return () => {
        profileAckCallbacksRef.current.delete(callback);
      };
    },
    []
  );

  const onControlState = useCallback(
    (callback: (state: ControlStatePayload) => void) => {
      controlStateCallbacksRef.current.add(callback);
      return () => {
        controlStateCallbacksRef.current.delete(callback);
      };
    },
    []
  );

  useEffect(() => {
    return () => {
      cleanup();
    };
  }, [cleanup]);

  return {
    status,
    connect,
    disconnect,
    sendControl,
    selectProfile,
    onAck,
    onProfileAck,
    onControlState,
    error,
  };
};
