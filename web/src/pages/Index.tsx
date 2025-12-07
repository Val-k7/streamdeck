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
import { AnimatePresence, motion } from "framer-motion";
import { ChevronLeft, ChevronRight } from "lucide-react";
import { useCallback, useEffect, useState } from "react";
import { logger } from "@/lib/logger";

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
    if (settings.serverIp && settings.serverPort && isValidIpAddress(settings.serverIp)) {
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
  }, [settings.serverIp, settings.serverPort, settings.useTls, isValidIpAddress]);

  // Charger les profils au démarrage
  useEffect(() => {
    if (profiles.profiles.length > 0 && !activeProfileId) {
      setActiveProfileId(profiles.profiles[0].id);
      // Charger le premier profil
      profiles.loadProfile(profiles.profiles[0].id);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [profiles.profiles, activeProfileId]);

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
    (direction: number) => {
      if (profiles.profiles.length === 0) return;
      const currentIndex = profiles.profiles.findIndex(
        (p) => p.id === activeProfileId
      );
      const newIndex =
        (currentIndex + direction + profiles.profiles.length) %
        profiles.profiles.length;
      setSlideDirection(direction);
      const newProfileId = profiles.profiles[newIndex].id;
      setActiveProfileId(newProfileId);
      profiles.loadProfile(newProfileId);
      setActivePage((prev) => ({
        ...prev,
        [newProfileId]: prev[newProfileId] || 0,
      }));
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

  const handleProfileChange = (profileId: string) => {
    if (!activeProfileId) return;
    const currentIndex = profiles.profiles.findIndex(
      (p) => p.id === activeProfileId
    );
    const newIndex = profiles.profiles.findIndex((p) => p.id === profileId);
    setSlideDirection(newIndex > currentIndex ? 1 : -1);
    setActiveProfileId(profileId);
    profiles.loadProfile(profileId);
    setActivePage((prev) => ({ ...prev, [profileId]: prev[profileId] || 0 }));
  };

  // Convertir les contrôles du profil serveur en format PadConfig
  const convertProfileToDecks = useCallback(() => {
    if (!profiles.selectedProfile) return {};

    const padConfigs = profiles.selectedProfile.controls.map((control) => {
      const colSpan = control.colSpan || 1;
      const rowSpan = control.rowSpan || 1;
      const size = `${colSpan}x${rowSpan}` as "1x1" | "2x1" | "1x2" | "2x2";

      return {
        id: control.id,
        type: control.type.toLowerCase() as
          | "button"
          | "toggle"
          | "fader"
          | "encoder",
        size,
        label: control.label || "",
        color: "primary" as const,
        defaultActive: false,
        defaultValue: 0,
      };
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
  const uniqueProfiles = profiles.profiles.filter((profile, index, self) =>
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
      <SettingsOverlay
        open={settingsOpen}
        onOpenChange={setSettingsOpen}
        onConnect={() => {
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
          setActiveProfileId(profileId);
          profiles.loadProfile(profileId);
        }}
      />
    </div>
  );
};

export default Index;
