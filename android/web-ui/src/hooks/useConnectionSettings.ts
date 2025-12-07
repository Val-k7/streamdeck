import { useState, useEffect, useCallback } from "react";
import { logger } from "@/lib/logger";

export interface ConnectionSettings {
  serverIp: string;
  serverPort: number;
  useTls: boolean;
  token?: string;
}

// Interface Android disponible via window.Android
declare global {
  interface Window {
    Android?: {
      getConnectionSettings(): string;
      saveConnectionSettings(settingsJson: string): void;
    };
  }
}

const STORAGE_KEY = "control-deck-connection-settings";

const defaultSettings: ConnectionSettings = {
  serverIp: "",
  serverPort: 4455,
  useTls: false,
};

export const useConnectionSettings = () => {
  const [settings, setSettings] = useState<ConnectionSettings>(defaultSettings);

  useEffect(() => {
    // Charger les paramètres depuis Android si disponible
    if (window.Android) {
      try {
        const settingsJson = window.Android.getConnectionSettings();
        const parsed = JSON.parse(settingsJson);
        setSettings({ ...defaultSettings, ...parsed });
      } catch (e) {
        logger.error("Failed to load connection settings from Android:", e);
      }
    } else {
      // Fallback sur localStorage pour le web
      try {
        const stored = localStorage.getItem(STORAGE_KEY);
        if (stored) {
          const parsed = JSON.parse(stored);
          setSettings({ ...defaultSettings, ...parsed });
        }
      } catch (e) {
        logger.error("Failed to load connection settings:", e);
      }
    }
  }, []);

  const saveSettings = useCallback((newSettings: Partial<ConnectionSettings>) => {
    setSettings((prevSettings) => {
      const updated = { ...prevSettings, ...newSettings };
      if (window.Android) {
        try {
          window.Android.saveConnectionSettings(JSON.stringify(updated));
        } catch (e) {
          logger.error("Failed to save connection settings to Android:", e);
        }
      } else {
        // Fallback sur localStorage pour le web
        try {
          localStorage.setItem(STORAGE_KEY, JSON.stringify(updated));
        } catch (e) {
          logger.error("Failed to save connection settings:", e);
        }
      }
      return updated;
    });
  }, []);

  const getWebSocketUrl = useCallback((settings: ConnectionSettings): string => {
    if (!settings.serverIp?.trim()) return "";
    const scheme = settings.useTls ? "wss" : "ws";
    return `${scheme}://${settings.serverIp}:${settings.serverPort}/ws`;
  }, []);

  const getHttpUrl = useCallback((settings: ConnectionSettings): string => {
    if (!settings.serverIp?.trim()) return "";
    const scheme = settings.useTls ? "https" : "http";
    return `${scheme}://${settings.serverIp}:${settings.serverPort}`;
  }, []);

  return {
    settings,
    saveSettings,
    getWebSocketUrl,
    getHttpUrl,
  };
};
