import { useEffect, useRef, useState, useCallback } from "react";
import { logger } from "@/lib/logger";

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
  value: number;
  messageId: string;
  sentAt: number;
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
  error: string | null;
  connect: (config: WebSocketConfig) => void;
  disconnect: () => void;
  sendControl: (controlId: string, value: number, meta?: Record<string, string>) => Promise<void>;
  selectProfile: (profileId: string) => Promise<void>;
  onAck?: (callback: (ack: AckPayload) => void) => void;
  onProfileAck?: (callback: (ack: ProfileAckPayload) => void) => void;
  onControlState?: (callback: (state: ControlStatePayload) => void) => void;
}

// Interface Android disponible via window.Android
declare global {
  interface Window {
    Android?: {
      getConnectionStatus(): string;
      connect(serverIp: string, serverPort: number, useTls: boolean): void;
      disconnect(): void;
      sendControl(controlId: string, value: number, metaJson?: string): void;
      selectProfile(profileId: string): void;
    };
  }
}

export const useWebSocket = (): UseWebSocketReturn => {
  const [status, setStatus] = useState<ConnectionStatus>("offline");
  const [error, setError] = useState<string | null>(null);
  const ackCallbacksRef = useRef<Set<(ack: AckPayload) => void>>(new Set());
  const profileAckCallbacksRef = useRef<Set<(ack: ProfileAckPayload) => void>>(new Set());
  const controlStateCallbacksRef = useRef<Set<(state: ControlStatePayload) => void>>(new Set());
  const statusCheckIntervalRef = useRef<number | null>(null);

  // Vérifier le statut de connexion périodiquement si on est sur Android
  useEffect(() => {
    if (window.Android) {
      statusCheckIntervalRef.current = window.setInterval(() => {
        try {
          const statusJson = window.Android!.getConnectionStatus();
          const statusObj = JSON.parse(statusJson);
          setStatus(statusObj.status as ConnectionStatus);
          setError(statusObj.error || null);
        } catch (e) {
          logger.error("Failed to get connection status from Android:", e);
        }
      }, 1000);
    }

    return () => {
      if (statusCheckIntervalRef.current) {
        clearInterval(statusCheckIntervalRef.current);
      }
    };
  }, []);

  const connect = useCallback((config: WebSocketConfig) => {
    if (window.Android) {
      try {
        // Extraire l'IP et le port de l'URL
        const url = new URL(config.url);
        const serverIp = url.hostname;
        const serverPort = parseInt(url.port || (url.protocol === "wss:" ? "443" : "80"), 10);
        const useTls = url.protocol === "wss:";

        window.Android.connect(serverIp, serverPort, useTls);
        setStatus("connecting");
        setError(null);
      } catch (e) {
        setError(e instanceof Error ? e.message : "Failed to connect");
        setStatus("offline");
      }
    } else {
      setError("Android interface not available");
      setStatus("offline");
    }
  }, []);

  const disconnect = useCallback(() => {
    if (window.Android) {
      window.Android.disconnect();
      setStatus("offline");
      setError(null);
    }
  }, []);

  const sendControl = useCallback(async (controlId: string, value: number, meta?: Record<string, string>) => {
    if (window.Android) {
      const metaJson = meta ? JSON.stringify(meta) : undefined;
      window.Android.sendControl(controlId, value, metaJson);
    } else {
      throw new Error("Android interface not available");
    }
  }, []);

  const selectProfile = useCallback(async (profileId: string) => {
    if (window.Android) {
      window.Android.selectProfile(profileId);
    } else {
      throw new Error("Android interface not available");
    }
  }, []);

  const onAck = useCallback((callback: (ack: AckPayload) => void) => {
    ackCallbacksRef.current.add(callback);
    return () => {
      ackCallbacksRef.current.delete(callback);
    };
  }, []);

  const onProfileAck = useCallback((callback: (ack: ProfileAckPayload) => void) => {
    profileAckCallbacksRef.current.add(callback);
    return () => {
      profileAckCallbacksRef.current.delete(callback);
    };
  }, []);

  const onControlState = useCallback((callback: (state: ControlStatePayload) => void) => {
    controlStateCallbacksRef.current.add(callback);
    return () => {
      controlStateCallbacksRef.current.delete(callback);
    };
  }, []);

  return {
    status,
    error,
    connect,
    disconnect,
    sendControl,
    selectProfile,
    onAck,
    onProfileAck,
    onControlState,
  };
};

