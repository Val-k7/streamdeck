/**
 * WebSocket message handling hook
 * Manages pending messages, timeouts, and message routing
 */

import { logger } from "@/lib/logger";
import { WEBSOCKET_CONFIG } from "@/config/websocket";
import { useCallback, useRef } from "react";

interface PendingMessage {
  resolve: () => void;
  reject: (error: Error) => void;
  timeout: ReturnType<typeof setTimeout>;
}

interface UseWebSocketMessagesReturn {
  sendMessage: (ws: WebSocket | null, message: object, timeout?: number) => Promise<void>;
  handleMessage: (data: any) => void;
  registerAckHandler: (handler: (ack: any) => void) => () => void;
  cleanup: () => void;
}

export const useWebSocketMessages = (): UseWebSocketMessagesReturn => {
  const pendingMessagesRef = useRef<Map<string, PendingMessage>>(new Map());
  const ackHandlersRef = useRef<Set<(ack: any) => void>>(new Set());

  const cleanup = useCallback(() => {
    // Reject all pending messages
    pendingMessagesRef.current.forEach(({ reject, timeout }) => {
      clearTimeout(timeout);
      reject(new Error("WebSocket disconnected"));
    });
    pendingMessagesRef.current.clear();
  }, []);

  const sendMessage = useCallback(
    async (
      ws: WebSocket | null,
      message: object,
      timeout = WEBSOCKET_CONFIG.MESSAGE_TIMEOUT_MS
    ): Promise<void> => {
      if (!ws || ws.readyState !== WebSocket.OPEN) {
        throw new Error("WebSocket is not connected");
      }

      const messageId = `${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;
      const fullMessage = {
        ...message,
        messageId,
        sentAt: Date.now(),
      };

      return new Promise<void>((resolve, reject) => {
        const timeoutHandle = setTimeout(() => {
          pendingMessagesRef.current.delete(messageId);
          reject(new Error("Message timeout"));
        }, timeout);

        pendingMessagesRef.current.set(messageId, { resolve, reject, timeout: timeoutHandle });

        try {
          ws.send(JSON.stringify(fullMessage));
          logger.debug("Message sent:", messageId);
        } catch (err) {
          clearTimeout(timeoutHandle);
          pendingMessagesRef.current.delete(messageId);
          reject(err as Error);
        }
      });
    },
    []
  );

  const handleMessage = useCallback((data: any) => {
    // Handle ACK messages
    if (data.type === "ack" || data.type === "profile:select:ack") {
      // Notify registered handlers
      ackHandlersRef.current.forEach((handler) => handler(data));

      // Resolve pending messages
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
  }, []);

  const registerAckHandler = useCallback((handler: (ack: any) => void) => {
    ackHandlersRef.current.add(handler);
    return () => {
      ackHandlersRef.current.delete(handler);
    };
  }, []);

  return {
    sendMessage,
    handleMessage,
    registerAckHandler,
    cleanup,
  };
};
