import { useState, useEffect, useCallback } from "react";
import { logger } from "@/lib/logger";

export interface ConnectionSettings {
  serverIp: string;
  serverPort: number;
  useTls: boolean;
  token?: string;
}

const STORAGE_KEY = "control-deck-connection-settings";

const getDefaultSettings = (): ConnectionSettings => {
  if (typeof window !== "undefined" && window.location.protocol !== "file:") {
    const isHttps = window.location.protocol === "https:";
    const host = window.location.hostname;
    const port = window.location.port
      ? parseInt(window.location.port, 10)
      : isHttps
        ? 443
        : 80;

    return {
      serverIp: host,
      serverPort: port,
      useTls: isHttps,
    };
  }

  return {
    serverIp: "",
    serverPort: 4455,
    useTls: false,
  };
};

export const useConnectionSettings = () => {
  const [settings, setSettings] = useState<ConnectionSettings>(getDefaultSettings);

  useEffect(() => {
    try {
      const stored = localStorage.getItem(STORAGE_KEY);
      if (stored) {
        const parsed = JSON.parse(stored);
        setSettings({ ...getDefaultSettings(), ...parsed });
      } else {
        const autoDetected = getDefaultSettings();
        if (autoDetected.serverIp) {
          logger.info("Auto-detected server from origin:", autoDetected);
          setSettings(autoDetected);
        }
      }
    } catch (e) {
      logger.error("Failed to load connection settings:", e);
    }
  }, []);

  const saveSettings = useCallback((newSettings: Partial<ConnectionSettings>) => {
    setSettings((prevSettings) => {
      const updated = { ...prevSettings, ...newSettings };
      try {
        localStorage.setItem(STORAGE_KEY, JSON.stringify(updated));
      } catch (e) {
        logger.error("Failed to save connection settings:", e);
      }
      return updated;
    });
  }, []);

  const getWebSocketUrl = useCallback((settings: ConnectionSettings): string => {
    const hasServer = !!settings.serverIp?.trim();
    const scheme = settings.useTls ? "wss" : "ws";

    if (typeof window !== "undefined") {
      const sameHost = hasServer && settings.serverIp === window.location.hostname;
      const samePort = hasServer && `${settings.serverPort}` === (window.location.port || (window.location.protocol === "https:" ? "443" : "80"));

      // If targeting the current origin, prefer relative path to survive proxies/reverse-proxy setups
      if (!hasServer || (sameHost && samePort)) {
        return "/ws";
      }
    }

    if (!hasServer) return "";
    return `${scheme}://${settings.serverIp}:${settings.serverPort}/ws`;
  }, []);

  const getHttpUrl = useCallback((settings: ConnectionSettings): string => {
    const hasServer = !!settings.serverIp?.trim();
    const scheme = settings.useTls ? "https" : "http";

    if (typeof window !== "undefined") {
      const sameHost = hasServer && settings.serverIp === window.location.hostname;
      const samePort = hasServer && `${settings.serverPort}` === (window.location.port || (window.location.protocol === "https:" ? "443" : "80"));

      // For same-origin, return empty to allow relative fetches
      if (!hasServer || (sameHost && samePort)) {
        return "";
      }
    }

    if (!hasServer) return "";
    return `${scheme}://${settings.serverIp}:${settings.serverPort}`;
  }, []);

  return {
    settings,
    saveSettings,
    getWebSocketUrl,
    getHttpUrl,
  };
};
