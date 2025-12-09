import { logger } from "@/lib/logger";
import { useCallback, useEffect, useState } from "react";

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
  icon?: string;
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

export const useProfiles = (config: UseProfilesConfig): UseProfilesReturn => {
  const [profiles, setProfiles] = useState<ProfileSummary[]>([]);
  const [selectedProfile, setSelectedProfile] = useState<Profile | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const loadProfiles = useCallback(async () => {
    const baseUrl = (config.serverUrl || "").replace(/\/$/, "");

    setLoading(true);
    setError(null);

    try {
      const url = baseUrl ? `${baseUrl}/profiles` : "/profiles";
      const headers: HeadersInit = {
        "Content-Type": "application/json",
      };

      if (config.token) {
        headers["Authorization"] = `Bearer ${config.token}`;
        headers["X-Deck-Token"] = config.token;
      }

      const response = await fetch(url, { headers });

      if (!response.ok) {
        throw new Error(`Failed to load profiles: ${response.statusText}`);
      }

      const data = await response.json();
      setProfiles(data.profiles || []);
    } catch (e) {
      const message =
        e instanceof Error ? e.message : "Failed to load profiles";
      setError(message);
      logger.error("Error loading profiles:", e);
    } finally {
      setLoading(false);
    }
  }, [config.serverUrl, config.token]);

  const loadProfile = useCallback(
    async (profileId: string) => {
      const baseUrl = (config.serverUrl || "").replace(/\/$/, "");

      setLoading(true);
      setError(null);

      try {
        const url = baseUrl
          ? `${baseUrl}/profiles/${profileId}`
          : `/profiles/${profileId}`;
        const headers: HeadersInit = {
          "Content-Type": "application/json",
        };

        if (config.token) {
          headers["Authorization"] = `Bearer ${config.token}`;
          headers["X-Deck-Token"] = config.token;
        }

        const response = await fetch(url, { headers });

        if (!response.ok) {
          throw new Error(`Failed to load profile: ${response.statusText}`);
        }

        const profile: Profile = await response.json();
        setSelectedProfile(profile);
      } catch (e) {
        const message =
          e instanceof Error ? e.message : "Failed to load profile";
        setError(message);
        logger.error("Error loading profile:", e);
        setSelectedProfile(null);
      } finally {
        setLoading(false);
      }
    },
    [config.serverUrl, config.token]
  );

  const selectProfile = useCallback(
    async (profileId: string) => {
      await loadProfile(profileId);
    },
    [loadProfile]
  );

  // Charger les profils au démarrage si l'URL est configurée
  useEffect(() => {
    if (config.serverUrl) {
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
