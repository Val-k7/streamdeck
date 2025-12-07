import { useState, useEffect, useCallback } from "react";

export interface PadConfig {
  id: string;
  type: "button" | "toggle" | "fader" | "encoder";
  size: "1x1" | "2x1" | "1x2" | "2x2";
  label: string;
  iconName?: string;
  imageUrl?: string;
  color?: "primary" | "accent" | "destructive" | "muted";
  colorHex?: string;
  defaultActive?: boolean;
  defaultValue?: number;
  action?: {
    type: string;
    payload?: Record<string, unknown>;
  };
}

export interface DeckState {
  padStates: Record<string, boolean>;
  faderValues: Record<string, number>;
}

const STORAGE_KEY = "control-deck-config";
const STATE_STORAGE_KEY = "control-deck-state";

export const useDeckStorage = () => {
  const [decks, setDecks] = useState<Record<string, PadConfig[]>>({});
  const [states, setStates] = useState<Record<string, DeckState>>({});
  const [isLoaded, setIsLoaded] = useState(false);

  // Load from localStorage on mount
  useEffect(() => {
    try {
      const savedConfig = localStorage.getItem(STORAGE_KEY);
      const savedStates = localStorage.getItem(STATE_STORAGE_KEY);

      if (savedConfig) {
        setDecks(JSON.parse(savedConfig));
      }
      if (savedStates) {
        setStates(JSON.parse(savedStates));
      }
    } catch (e) {
      logger.error("Failed to load deck config from storage:", e);
    }
    setIsLoaded(true);
  }, []);

  // Save decks to localStorage
  const saveDecks = useCallback((newDecks: Record<string, PadConfig[]>) => {
    setDecks(newDecks);
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(newDecks));
    } catch (e) {
      logger.error("Failed to save deck config:", e);
    }
  }, []);

  // Save states to localStorage
  const saveStates = useCallback((newStates: Record<string, DeckState>) => {
    setStates(newStates);
    try {
      localStorage.setItem(STATE_STORAGE_KEY, JSON.stringify(newStates));
    } catch (e) {
      logger.error("Failed to save deck state:", e);
    }
  }, []);

  // Update a single pad in a profile
  const updatePad = useCallback((profileId: string, padId: string, updates: Partial<PadConfig>) => {
    setDecks((prev) => {
      const profilePads = prev[profileId] || [];
      const newPads = profilePads.map((pad) =>
        pad.id === padId ? { ...pad, ...updates } : pad
      );
      const newDecks = { ...prev, [profileId]: newPads };
      localStorage.setItem(STORAGE_KEY, JSON.stringify(newDecks));
      return newDecks;
    });
  }, []);

  // Update pad state
  const updatePadState = useCallback((profileId: string, padId: string, isActive: boolean) => {
    setStates((prev) => {
      const profileState = prev[profileId] || { padStates: {}, faderValues: {} };
      const newStates = {
        ...prev,
        [profileId]: {
          ...profileState,
          padStates: { ...profileState.padStates, [padId]: isActive },
        },
      };
      localStorage.setItem(STATE_STORAGE_KEY, JSON.stringify(newStates));
      return newStates;
    });
  }, []);

  // Update fader value
  const updateFaderValue = useCallback((profileId: string, padId: string, value: number) => {
    setStates((prev) => {
      const profileState = prev[profileId] || { padStates: {}, faderValues: {} };
      const newStates = {
        ...prev,
        [profileId]: {
          ...profileState,
          faderValues: { ...profileState.faderValues, [padId]: value },
        },
      };
      localStorage.setItem(STATE_STORAGE_KEY, JSON.stringify(newStates));
      return newStates;
    });
  }, []);

  return {
    decks,
    states,
    isLoaded,
    saveDecks,
    saveStates,
    updatePad,
    updatePadState,
    updateFaderValue,
  };
};
