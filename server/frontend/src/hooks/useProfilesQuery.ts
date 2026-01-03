/**
 * React Query-based profiles hook
 * Replaces the old useProfiles with caching and automatic refetching
 */

import { logger } from "@/lib/logger";
import { queryKeys } from "@/lib/queryClient";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

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

interface UseProfilesQueryConfig {
  serverUrl: string;
  token?: string;
}

// API Functions
const fetchProfiles = async (
  serverUrl: string,
  token?: string
): Promise<ProfileSummary[]> => {
  const baseUrl = serverUrl.replace(/\/$/, "");
  const url = baseUrl ? `${baseUrl}/profiles` : "/profiles";

  const headers: HeadersInit = {
    "Content-Type": "application/json",
  };

  if (token) {
    headers["Authorization"] = `Bearer ${token}`;
    headers["X-Deck-Token"] = token;
  }

  const response = await fetch(url, { headers });

  if (!response.ok) {
    throw new Error(`Failed to load profiles: ${response.statusText}`);
  }

  const data = await response.json();
  return data.profiles || [];
};

const fetchProfile = async (
  profileId: string,
  serverUrl: string,
  token?: string
): Promise<Profile> => {
  const baseUrl = serverUrl.replace(/\/$/, "");
  const url = baseUrl ? `${baseUrl}/profiles/${profileId}` : `/profiles/${profileId}`;

  const headers: HeadersInit = {
    "Content-Type": "application/json",
  };

  if (token) {
    headers["Authorization"] = `Bearer ${token}`;
    headers["X-Deck-Token"] = token;
  }

  const response = await fetch(url, { headers });

  if (!response.ok) {
    throw new Error(`Failed to load profile: ${response.statusText}`);
  }

  return await response.json();
};

const saveProfile = async (
  profile: Profile,
  serverUrl: string,
  token?: string
): Promise<Profile> => {
  const baseUrl = serverUrl.replace(/\/$/, "");
  const url = baseUrl ? `${baseUrl}/profiles/${profile.id}` : `/profiles/${profile.id}`;

  const headers: HeadersInit = {
    "Content-Type": "application/json",
  };

  if (token) {
    headers["Authorization"] = `Bearer ${token}`;
    headers["X-Deck-Token"] = token;
  }

  const response = await fetch(url, {
    method: "PUT",
    headers,
    body: JSON.stringify(profile),
  });

  if (!response.ok) {
    throw new Error(`Failed to save profile: ${response.statusText}`);
  }

  return await response.json();
};

// Hooks
export const useProfilesList = (config: UseProfilesQueryConfig) => {
  return useQuery({
    queryKey: queryKeys.profiles.list({ serverUrl: config.serverUrl }),
    queryFn: () => fetchProfiles(config.serverUrl, config.token),
    staleTime: 5 * 60 * 1000, // 5 minutes
    retry: 3,
  });
};

export const useProfile = (profileId: string | null, config: UseProfilesQueryConfig) => {
  return useQuery({
    queryKey: queryKeys.profiles.detail(profileId || ""),
    queryFn: () => fetchProfile(profileId!, config.serverUrl, config.token),
    enabled: !!profileId, // Only fetch if profileId is provided
    staleTime: 10 * 60 * 1000, // 10 minutes (profiles change less frequently)
    retry: 2,
  });
};

export const useSaveProfile = (config: UseProfilesQueryConfig) => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (profile: Profile) => saveProfile(profile, config.serverUrl, config.token),
    onSuccess: (savedProfile) => {
      // Invalidate and refetch profiles list
      queryClient.invalidateQueries({ queryKey: queryKeys.profiles.lists() });

      // Update the specific profile in cache
      queryClient.setQueryData(
        queryKeys.profiles.detail(savedProfile.id),
        savedProfile
      );

      logger.info("Profile saved successfully:", savedProfile.id);
    },
    onError: (error) => {
      logger.error("Failed to save profile:", error);
    },
  });
};

// Prefetch helper for better UX
export const usePrefetchProfile = (config: UseProfilesQueryConfig) => {
  const queryClient = useQueryClient();

  return (profileId: string) => {
    queryClient.prefetchQuery({
      queryKey: queryKeys.profiles.detail(profileId),
      queryFn: () => fetchProfile(profileId, config.serverUrl, config.token),
      staleTime: 10 * 60 * 1000,
    });
  };
};
