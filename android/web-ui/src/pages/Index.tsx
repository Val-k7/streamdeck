import { ConnectionIndicator } from "@/components/ConnectionIndicator";
import { DeckGrid, defaultProfileDecks } from "@/components/DeckGrid";
import { ProfileTabs } from "@/components/ProfileTabs";
import { SettingsButton } from "@/components/SettingsButton";
import { SettingsOverlay } from "@/components/SettingsOverlay";
import { Button } from "@/components/ui/button";
import { useConnectionSettings } from "@/hooks/useConnectionSettings";
import { useDeckStorage } from "@/hooks/useDeckStorage";
import { useProfiles } from "@/hooks/useProfiles";
import { useSwipeGesture } from "@/hooks/useSwipeGesture";
import { useWebSocket } from "@/hooks/useWebSocket";
import { logger } from "@/lib/logger";
import { AnimatePresence, motion } from "framer-motion";
import { ChevronLeft, ChevronRight } from "lucide-react";
import { useCallback, useEffect, useState } from "react";

// Type pour window.Android
declare global {
  interface Window {
    Android?: unknown;
  }
}

const Index = () => {
  const { settings, getHttpUrl, getWebSocketUrl } = useConnectionSettings();
  const ws = useWebSocket();
  const profiles = useProfiles({
    serverUrl: getHttpUrl(settings),
    token: settings.token,
  });

  const [settingsOpen, setSettingsOpen] = useState(false);
  const [activeProfileId, setActiveProfileId] = useState<string | null>(null);
  const [activePage, setActivePage] = useState<Record<string, number>>({});
  const [slideDirection, setSlideDirection] = useState(0);

  // Utiliser un state réactif pour le bridge Android car il peut être injecté après le rendu initial
  const [isAndroidBridge, setIsAndroidBridge] = useState(() => {
    const hasAndroid = typeof window !== "undefined" && !!window.Android;
    console.log("[Index] Initial isAndroidBridge check:", hasAndroid);
    return hasAndroid;
  });

  // Écouter l'événement de disponibilité du bridge Android
  useEffect(() => {
    console.log("[Index] Setting up Android bridge listener");

    const handleBridgeReady = () => {
      console.log("[Index] android-bridge-ready event received!");
      logger.info("Index: Android bridge ready event received");
      setIsAndroidBridge(true);
    };

    // Vérifier si le bridge est déjà disponible
    if (typeof window !== "undefined" && window.Android) {
      console.log("[Index] Android bridge already available");
      setIsAndroidBridge(true);
    }

    window.addEventListener("android-bridge-ready", handleBridgeReady);
    return () => {
      window.removeEventListener("android-bridge-ready", handleBridgeReady);
    };
  }, []);

  const {
    decks,
    states,
    isLoaded,
    saveDecks,
    updatePad,
    updatePadState,
    updateFaderValue,
  } = useDeckStorage();

  // Validation assouplie: accepter hostname ou IP (publique ou locale) pour debug distant
  const isValidIpAddress = useCallback((ip: string): boolean => {
    if (!ip || ip.trim() === "") return false;
    const trimmed = ip.trim();
    const hostPattern = /^[a-zA-Z0-9.:-]+$/;
    return hostPattern.test(trimmed);
  }, []);

  // Connexion WebSocket automatique
  useEffect(() => {
    if (
      settings.serverIp &&
      settings.serverPort &&
      isValidIpAddress(settings.serverIp)
    ) {
      const wsUrl = getWebSocketUrl(settings);
      ws.connect({
        url: wsUrl,
        token: settings.token,
        heartbeatInterval: 15000,
        reconnectDelay: 1000,
        maxReconnectAttempts: 6,
      });
    }
    return () => {
      ws.disconnect();
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [
    settings.serverIp,
    settings.serverPort,
    settings.useTls,
    isValidIpAddress,
  ]);

  // Debug: Log profile state changes
  useEffect(() => {
    logger.debug("Index: profiles state changed", {
      count: profiles.profiles.length,
      profiles: profiles.profiles.map((p) => ({ id: p.id, name: p.name })),
      loading: profiles.loading,
      error: profiles.error,
      selectedProfile: profiles.selectedProfile?.id,
    });
  }, [
    profiles.profiles,
    profiles.loading,
    profiles.error,
    profiles.selectedProfile,
  ]);

  // Charger les profils au démarrage
  useEffect(() => {
    if (profiles.profiles.length > 0 && !activeProfileId && !profiles.loading) {
      const firstProfileId = profiles.profiles[0].id;
      logger.debug("Index: initializing with first profile", firstProfileId);
      setActiveProfileId(firstProfileId);
      // Charger le premier profil
      profiles.loadProfile(firstProfileId).catch((err) => {
        logger.error("Index: failed to load initial profile", err);
      });
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [profiles.profiles, activeProfileId, profiles.loading]);

  // Sélectionner un profil via WebSocket
  useEffect(() => {
    if (activeProfileId && ws.status === "online") {
      ws.selectProfile(activeProfileId, true).catch((err) => {
        logger.error("Failed to select profile:", err);
      });
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [activeProfileId, ws.status]);

  // Calculer le nombre de pages pour un profil (basé sur le nombre de contrôles)
  const getProfilePages = useCallback(
    (profileId: string): number => {
      if (
        profiles.selectedProfile?.id === profileId &&
        profiles.selectedProfile
      ) {
        const controlsPerPage = 16; // 4x4 grid
        return Math.ceil(
          profiles.selectedProfile.controls.length / controlsPerPage
        );
      }
      // Fallback sur les decks locaux
      const profileDecks =
        decks[profileId] || defaultProfileDecks[profileId] || [];
      return Math.ceil(profileDecks.length / 16);
    },
    [profiles.selectedProfile, decks]
  );

  const navigateProfile = useCallback(
    async (direction: number) => {
      if (profiles.profiles.length === 0) return;
      const currentIndex = profiles.profiles.findIndex(
        (p) => p.id === activeProfileId
      );
      const newIndex =
        (currentIndex + direction + profiles.profiles.length) %
        profiles.profiles.length;
      setSlideDirection(direction);
      const newProfileId = profiles.profiles[newIndex].id;

      try {
        await profiles.loadProfile(newProfileId);
        setActiveProfileId(newProfileId);
        setActivePage((prev) => ({
          ...prev,
          [newProfileId]: prev[newProfileId] || 0,
        }));
      } catch (err) {
        logger.error("Index: failed to navigate to profile", newProfileId, err);
        setActiveProfileId(newProfileId);
      }
    },
    [activeProfileId, profiles]
  );

  const navigatePage = useCallback(
    (direction: number) => {
      if (!activeProfileId) return;
      const currentPage = activePage[activeProfileId] || 0;
      const maxPages = getProfilePages(activeProfileId);
      const newPage = Math.max(
        0,
        Math.min(maxPages - 1, currentPage + direction)
      );
      setActivePage((prev) => ({ ...prev, [activeProfileId]: newPage }));
    },
    [activeProfileId, activePage, getProfilePages]
  );

  const swipeHandlers = useSwipeGesture({
    onSwipeLeft: () => navigateProfile(1),
    onSwipeRight: () => navigateProfile(-1),
    threshold: 60,
  });

  const handleProfileChange = useCallback(
    async (profileId: string) => {
      // Permettre le changement même si activeProfileId est null
      logger.debug("Index: change profile", profileId, "from", activeProfileId);

      if (activeProfileId) {
        const currentIndex = profiles.profiles.findIndex(
          (p) => p.id === activeProfileId
        );
        const newIndex = profiles.profiles.findIndex((p) => p.id === profileId);
        setSlideDirection(newIndex > currentIndex ? 1 : -1);
      } else {
        setSlideDirection(0);
      }

      // D'abord charger le profil, puis mettre à jour l'ID actif
      try {
        await profiles.loadProfile(profileId);
        setActiveProfileId(profileId);
        setActivePage((prev) => ({
          ...prev,
          [profileId]: prev[profileId] || 0,
        }));
        logger.debug("Index: profile loaded successfully", profileId);
      } catch (err) {
        logger.error("Index: failed to load profile", profileId, err);
        // Mettre à jour l'ID quand même pour permettre un retry
        setActiveProfileId(profileId);
      }
    },
    [activeProfileId, profiles]
  );

  // Convertir les contrôles du profil serveur en format PadConfig
  const convertProfileToDecks = useCallback(() => {
    if (!profiles.selectedProfile) return {};

    // Mapper les types serveur/Android vers les types web UI supportés
    const mapControlType = (
      serverType: string
    ): "button" | "toggle" | "fader" | "encoder" => {
      const type = serverType.toLowerCase();
      switch (type) {
        case "button":
        case "pad": // Android PAD → button
          return "button";
        case "toggle":
          return "toggle";
        case "fader":
          return "fader";
        case "knob": // Android KNOB → encoder
        case "encoder":
          return "encoder";
        default:
          logger.warn(
            `Unknown control type "${serverType}", defaulting to button`
          );
          return "button";
      }
    };

    const padConfigs = profiles.selectedProfile.controls.map((control) => {
      const colSpan = control.colSpan || 1;
      const rowSpan = control.rowSpan || 1;
      const size = `${colSpan}x${rowSpan}` as "1x1" | "2x1" | "1x2" | "2x2";

      // Mapper colorHex vers une couleur de pad supportée
      const mapColor = (
        colorHex?: string
      ): "primary" | "accent" | "destructive" | "muted" => {
        if (!colorHex) return "primary";
        const hex = colorHex.toLowerCase();
        // Rouge/Orange = destructive
        if (
          hex.startsWith("#f44") ||
          hex.startsWith("#e91") ||
          hex.startsWith("#ff5")
        ) {
          return "destructive";
        }
        // Gris/Foncé = muted
        if (
          hex.startsWith("#666") ||
          hex.startsWith("#888") ||
          hex.startsWith("#999")
        ) {
          return "muted";
        }
        return "primary";
      };

      return {
        id: control.id,
        type: mapControlType(control.type),
        size,
        label: control.label || "",
        color: mapColor(control.colorHex),
        colorHex: control.colorHex,
        defaultActive: false,
        defaultValue: 0,
        action: control.action,
      };
    });

    logger.debug("convertProfileToDecks:", {
      profileId: profiles.selectedProfile.id,
      controlCount: profiles.selectedProfile.controls.length,
      padConfigsCount: padConfigs.length,
    });

    return { [profiles.selectedProfile.id]: padConfigs };
  }, [profiles.selectedProfile]);

  // Utiliser les profils du serveur ou les decks locaux
  const effectiveDecks = profiles.selectedProfile
    ? convertProfileToDecks()
    : Object.keys(decks).length > 0
    ? decks
    : defaultProfileDecks;

  const currentPage = activeProfileId ? activePage[activeProfileId] || 0 : 0;
  const maxPages = activeProfileId ? getProfilePages(activeProfileId) : 1;
  const canGoPrevPage = currentPage > 0;
  const canGoNextPage = currentPage < maxPages - 1;

  // Convertir les profils du serveur en format pour ProfileTabs
  // Dédupliquer par ID pour éviter les doublons
  const uniqueProfiles = profiles.profiles.filter(
    (profile, index, self) =>
      index === self.findIndex((p) => p.id === profile.id)
  );
  const profileTabs = uniqueProfiles.map((p) => ({
    id: p.id,
    label: p.name,
  }));

  // Si aucun profil n'est disponible, utiliser les profils par défaut
  const displayProfiles =
    profileTabs.length > 0
      ? profileTabs
      : [
          { id: "streaming", label: "Stream" },
          { id: "audio", label: "Audio" },
          { id: "macros", label: "Macros" },
        ];

  const displayActiveProfile =
    activeProfileId || displayProfiles[0]?.id || "streaming";

  return (
    <>
      {/* Modal Settings - Rendu en dehors du conteneur overflow-hidden */}
      {settingsOpen && (
        <SettingsOverlay
          open={settingsOpen}
          onOpenChange={(open) => {
            console.log(
              "[Index] settings overlay onOpenChange:",
              open,
              "isAndroidBridge:",
              isAndroidBridge
            );
            logger.info("Index: settings overlay change", open);
            setSettingsOpen(open);
          }}
          onConnect={() => {
            logger.debug("Index: onConnect from settings overlay");
            if (settings.serverIp && settings.serverPort) {
              const wsUrl = getWebSocketUrl(settings);
              ws.connect({
                url: wsUrl,
                token: settings.token,
                heartbeatInterval: 15000,
                reconnectDelay: 1000,
                maxReconnectAttempts: 6,
              });
            }
          }}
          onProfileSelect={(profileId) => {
            logger.debug(
              "Index: onProfileSelect from settings overlay",
              profileId
            );
            setActiveProfileId(profileId);
            profiles.loadProfile(profileId);
          }}
        />
      )}

      <div
        className="fixed inset-0 bg-background overflow-hidden"
        {...swipeHandlers}
      >
        <ConnectionIndicator status={ws.status} />
        <SettingsButton onClick={() => setSettingsOpen(true)} />

        {/* Navigation entre pages */}
        {maxPages > 1 && (
          <div className="absolute top-20 left-0 right-0 flex justify-center gap-2 z-10">
            <Button
              variant="ghost"
              size="icon"
              onClick={() => navigatePage(-1)}
              disabled={!canGoPrevPage}
              className="bg-card/80 backdrop-blur-md"
            >
              <ChevronLeft className="h-4 w-4" />
            </Button>
            <div className="px-3 py-1 bg-card/80 backdrop-blur-md rounded-full text-xs text-mono">
              Page {currentPage + 1} / {maxPages}
            </div>
            <Button
              variant="ghost"
              size="icon"
              onClick={() => navigatePage(1)}
              disabled={!canGoNextPage}
              className="bg-card/80 backdrop-blur-md"
            >
              <ChevronRight className="h-4 w-4" />
            </Button>
          </div>
        )}

        <AnimatePresence mode="wait" initial={false}>
          <motion.div
            key={displayActiveProfile}
            className="w-full h-full"
            initial={{ x: slideDirection * 100 + "%", opacity: 0 }}
            animate={{ x: 0, opacity: 1 }}
            exit={{ x: slideDirection * -100 + "%", opacity: 0 }}
            transition={{ type: "spring", stiffness: 300, damping: 30 }}
          >
            <DeckGrid
              activeProfile={displayActiveProfile}
              decks={effectiveDecks}
              states={states}
              onUpdatePad={updatePad}
              onUpdatePadState={updatePadState}
              onUpdateFaderValue={updateFaderValue}
              currentPage={currentPage}
              websocket={ws}
              selectedProfileId={profiles.selectedProfile?.id}
            />
          </motion.div>
        </AnimatePresence>

        <ProfileTabs
          profiles={displayProfiles}
          activeProfile={displayActiveProfile}
          onProfileChange={handleProfileChange}
        />
        <div className="fixed top-2 left-2 px-3 py-2 bg-card/80 border border-border/60 rounded text-xs text-mono space-y-1 pointer-events-none">
          <div>Android bridge: {isAndroidBridge ? "available" : "missing"}</div>
          <div>WS status: {ws.status}</div>
          {ws.error && (
            <div className="text-destructive">Error: {ws.error}</div>
          )}
        </div>
      </div>
    </>
  );
};

export default Index;
