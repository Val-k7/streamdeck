import { useState, useEffect, useCallback } from "react";
import { logger } from "@/lib/logger";

export interface Profile {
  id: string;
  name: string;
  rows: number;
  cols: number;
  version?: number;
  checksum?: string;
  controls: Control[];
}

export interface Control {
  id: string;
  type: string;
  row: number;
  col: number;
  rowSpan?: number;
  colSpan?: number;
  label?: string;
  colorHex?: string;
  action?: {
    type: string;
    payload?: Record<string, unknown>;
  };
}

interface ProfileSummary {
  id: string;
  name: string;
  version?: number;
}

interface UseProfilesConfig {
  serverUrl: string;
  token?: string;
}

export interface UseProfilesReturn {
  profiles: ProfileSummary[];
  selectedProfile: Profile | null;
  loading: boolean;
  error: string | null;
  loadProfiles: () => Promise<void>;
  loadProfile: (profileId: string) => Promise<void>;
  selectProfile: (profileId: string) => Promise<void>;
}

// Interface Android disponible via window.Android
declare global {
  interface Window {
    Android?: {
      getProfiles(): string;
      getProfile(profileId: string): string;
      selectProfile(profileId: string): void;
    };
  }
}

export const useProfiles = (config: UseProfilesConfig): UseProfilesReturn => {
  const [profiles, setProfiles] = useState<ProfileSummary[]>([]);
  const [selectedProfile, setSelectedProfile] = useState<Profile | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const loadProfiles = useCallback(async () => {
    // Utiliser l'interface Android si disponible
    if (window.Android) {
      setLoading(true);
      setError(null);
      try {
        const profilesJson = window.Android.getProfiles();
        const data = JSON.parse(profilesJson);
        setProfiles(data.profiles || []);
      } catch (e) {
        const message = e instanceof Error ? e.message : "Failed to load profiles";
        setError(message);
        logger.error("Error loading profiles from Android:", e);
      } finally {
        setLoading(false);
      }
      return;
    }

    // Fallback sur HTTP pour le web
    if (!config.serverUrl) {
      setError("Server URL not configured");
      return;
    }

    setLoading(true);
    setError(null);

    try {
      const url = `${config.serverUrl}/profiles`;
      const headers: HeadersInit = {
        "Content-Type": "application/json",
      };

      if (config.token) {
        headers["Authorization"] = `Bearer ${config.token}`;
      }

      const response = await fetch(url, { headers });

      if (!response.ok) {
        throw new Error(`Failed to load profiles: ${response.statusText}`);
      }

      const data = await response.json();
      setProfiles(data.profiles || []);
    } catch (e) {
      const message = e instanceof Error ? e.message : "Failed to load profiles";
      setError(message);
      logger.error("Error loading profiles:", e);
    } finally {
      setLoading(false);
    }
  }, [config.serverUrl, config.token]);

  const loadProfile = useCallback(async (profileId: string) => {
    // Utiliser l'interface Android si disponible
    if (window.Android) {
      setLoading(true);
      setError(null);
      try {
        const profileJson = window.Android.getProfile(profileId);
        const profileData = JSON.parse(profileJson);
        if (profileData.error) {
          throw new Error(profileData.error);
        }
        setSelectedProfile(profileData as Profile);
      } catch (e) {
        const message = e instanceof Error ? e.message : "Failed to load profile";
        setError(message);
        logger.error("Error loading profile from Android:", e);
        setSelectedProfile(null);
      } finally {
        setLoading(false);
      }
      return;
    }

    // Fallback sur HTTP pour le web
    if (!config.serverUrl) {
      setError("Server URL not configured");
      return;
    }

    setLoading(true);
    setError(null);

    try {
      const url = `${config.serverUrl}/profiles/${profileId}`;
      const headers: HeadersInit = {
        "Content-Type": "application/json",
      };

      if (config.token) {
        headers["Authorization"] = `Bearer ${config.token}`;
      }

      const response = await fetch(url, { headers });

      if (!response.ok) {
        throw new Error(`Failed to load profile: ${response.statusText}`);
      }

      const profile: Profile = await response.json();
      setSelectedProfile(profile);
    } catch (e) {
      const message = e instanceof Error ? e.message : "Failed to load profile";
      setError(message);
      logger.error("Error loading profile:", e);
      setSelectedProfile(null);
    } finally {
      setLoading(false);
    }
  }, [config.serverUrl, config.token]);

  const selectProfile = useCallback(async (profileId: string) => {
    if (window.Android) {
      window.Android.selectProfile(profileId);
      await loadProfile(profileId);
    } else {
      await loadProfile(profileId);
    }
  }, [loadProfile]);

  // Charger les profils au démarrage - via Android bridge OU serveur HTTP
  useEffect(() => {
    // Si Android bridge est disponible, charger immédiatement
    if (window.Android) {
      logger.debug("useProfiles: Loading profiles via Android bridge");
      loadProfiles();
    } else if (config.serverUrl) {
      // Sinon, charger via HTTP si l'URL est configurée
      logger.debug("useProfiles: Loading profiles via HTTP", config.serverUrl);
      loadProfiles();
    }
  }, [config.serverUrl, loadProfiles]);

  return {
    profiles,
    selectedProfile,
    loading,
    error,
    loadProfiles,
    loadProfile,
    selectProfile,
  };
};

