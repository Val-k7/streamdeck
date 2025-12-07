import { useState, useEffect, useCallback } from "react";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { useToast } from "@/hooks/use-toast";
import { Loader2, Search, Pause, CheckCircle2, XCircle } from "lucide-react";
import { logger } from "@/lib/logger";

interface DiscoveredServer {
  serverId: string;
  serverName: string;
  host: string;
  port: number;
  protocol: string;
  version: string;
}

interface PairedServer {
  serverId: string;
  serverName: string;
  host: string;
  port: number;
  protocol: string;
  isAutoConnect: boolean;
  pairedAt: number;
  lastSeen: number;
}

// Interface Android disponible via window.Android
declare global {
  interface Window {
    Android?: {
      getDiscoveredServers(): string;
      isDiscovering(): boolean;
      startDiscovery(): void;
      stopDiscovery(): void;
      getPairedServers(): string;
      requestPairingCode(serverId: string): string;
      confirmPairing(serverId: string, code: string): string;
      connectToPairedServer(serverId: string): void;
    };
  }
}

export const DiscoveryTab = () => {
  logger.debug("DiscoveryTab: Component rendering");
  const { toast } = useToast();
  const [isDiscovering, setIsDiscovering] = useState(false);
  const [discoveredServers, setDiscoveredServers] = useState<DiscoveredServer[]>([]);
  const [pairedServers, setPairedServers] = useState<PairedServer[]>([]);
  const [pairingCode, setPairingCode] = useState<string>("");
  const [pairingServerId, setPairingServerId] = useState<string | null>(null);
  const [isPairing, setIsPairing] = useState(false);

  logger.debug("DiscoveryTab: State initialized");

  const loadDiscoveredServers = useCallback(() => {
    if (typeof window !== "undefined" && window.Android) {
      try {
        const serversJson = window.Android.getDiscoveredServers();
        logger.debug("DiscoveryTab: Raw discovered servers JSON:", serversJson);
        const data = JSON.parse(serversJson);
        logger.debug("DiscoveryTab: Parsed discovered data:", data);
        const servers = data.servers || [];
        logger.debug("DiscoveryTab: Loaded discovered servers:", servers.length, servers);
        setDiscoveredServers(servers);
      } catch (e) {
        logger.error("Error loading discovered servers:", e);
        logger.error("Error details:", e instanceof Error ? e.stack : String(e));
      }
    } else {
      logger.warn("DiscoveryTab: Cannot load discovered servers - window.Android not available");
    }
  }, []);

  const loadPairedServers = useCallback(() => {
    if (typeof window !== "undefined" && window.Android) {
      try {
        const serversJson = window.Android.getPairedServers();
        logger.debug("DiscoveryTab: Raw paired servers JSON:", serversJson);
        const data = JSON.parse(serversJson);
        logger.debug("DiscoveryTab: Parsed paired data:", data);
        const servers = data.servers || [];
        logger.debug("DiscoveryTab: Loaded paired servers:", servers.length, servers);
        setPairedServers(servers);
      } catch (e) {
        logger.error("Error loading paired servers:", e);
        logger.error("Error details:", e instanceof Error ? e.stack : String(e));
      }
    } else {
      logger.warn("DiscoveryTab: Cannot load paired servers - window.Android not available");
    }
  }, []);

  const updateDiscoveryState = useCallback(() => {
    if (typeof window !== "undefined" && window.Android) {
      try {
        const discovering = window.Android.isDiscovering();
        setIsDiscovering(discovering);
      } catch (e) {
        logger.error("Error checking discovery state:", e);
      }
    }
  }, []);

  useEffect(() => {
    let interval: NodeJS.Timeout | null = null;
    let retryTimeout: NodeJS.Timeout | null = null;
    let retryCount = 0;
    const maxRetries = 30; // 30 tentatives sur 15 secondes
    const retryInterval = 500; // 500ms entre chaque tentative

    const initBridge = () => {
      const isAvailable = typeof window !== "undefined" && window.Android !== undefined;
      logger.debug(`DiscoveryTab: Attempt ${retryCount + 1}/${maxRetries} - window.Android available?`, isAvailable);

      if (isAvailable) {
        try {
          logger.debug("DiscoveryTab: Android bridge methods:", Object.keys(window.Android || {}));
          // Vérifier que les méthodes essentielles existent
          const requiredMethods = ['getDiscoveredServers', 'getPairedServers', 'isDiscovering'];
          const missingMethods = requiredMethods.filter(m => !window.Android || typeof window.Android[m] !== 'function');

          if (missingMethods.length > 0) {
            logger.warn("DiscoveryTab: Missing bridge methods:", missingMethods);
            if (retryCount < maxRetries) {
              retryCount++;
              retryTimeout = setTimeout(initBridge, retryInterval);
            }
            return;
          }

          // Bridge disponible et complet, initialiser
          logger.debug("DiscoveryTab: Bridge ready, initializing...");
          updateDiscoveryState();
          loadDiscoveredServers();
          loadPairedServers();

          // Rafraîchir périodiquement
          interval = setInterval(() => {
            if (typeof window !== "undefined" && window.Android) {
              updateDiscoveryState();
              loadDiscoveredServers();
              loadPairedServers();
            }
          }, 2000);
        } catch (e) {
          logger.error("DiscoveryTab: Error initializing bridge:", e);
          if (retryCount < maxRetries) {
            retryCount++;
            retryTimeout = setTimeout(initBridge, retryInterval);
          }
        }
      } else {
        // window.Android pas encore disponible, réessayer
        if (retryCount < maxRetries) {
          retryCount++;
          retryTimeout = setTimeout(initBridge, retryInterval);
        } else {
          logger.warn("DiscoveryTab: window.Android is not available after", maxRetries, "attempts. Make sure you're running in the Android app.");
        }
      }
    };

    // Écouter l'événement de bridge prêt
    const handleBridgeReady = () => {
      logger.debug("DiscoveryTab: Bridge ready event received");
      if (retryTimeout) {
        clearTimeout(retryTimeout);
        retryTimeout = null;
      }
      retryCount = 0; // Reset pour permettre une nouvelle tentative immédiate
      initBridge();
    };

    // Démarrer la vérification immédiatement
    initBridge();

    // Écouter l'événement de bridge prêt
    if (typeof window !== "undefined") {
      window.addEventListener('android-bridge-ready', handleBridgeReady);
    }

    return () => {
      if (interval) clearInterval(interval);
      if (retryTimeout) clearTimeout(retryTimeout);
      if (typeof window !== "undefined") {
        window.removeEventListener('android-bridge-ready', handleBridgeReady);
      }
    };
  }, [updateDiscoveryState, loadDiscoveredServers, loadPairedServers]);

  const handleToggleDiscovery = () => {
    if (window.Android) {
      if (isDiscovering) {
        window.Android.stopDiscovery();
      } else {
        window.Android.startDiscovery();
      }
      setTimeout(() => updateDiscoveryState(), 100);
    }
  };

  const handleRequestPairing = async (serverId: string) => {
    if (!window.Android) return;

    setIsPairing(true);
    setPairingServerId(serverId);
    try {
      const responseJson = window.Android.requestPairingCode(serverId);
      const response = JSON.parse(responseJson);
      if (response.error) {
        toast({
          variant: "destructive",
          title: "Pairing failed",
          description: response.error,
        });
        setPairingServerId(null);
      } else {
        setPairingCode(response.code);
        toast({
          title: "Pairing code received",
          description: `Code: ${response.code}`,
        });
      }
    } catch (e) {
      toast({
        variant: "destructive",
        title: "Error",
        description: e instanceof Error ? e.message : "Unknown error",
      });
      setPairingServerId(null);
    } finally {
      setIsPairing(false);
    }
  };

  const handleConfirmPairing = async () => {
    if (!window.Android || !pairingServerId || !pairingCode) return;

    setIsPairing(true);
    try {
      const responseJson = window.Android.confirmPairing(pairingServerId, pairingCode);
      const response = JSON.parse(responseJson);
      if (response.error) {
        toast({
          variant: "destructive",
          title: "Pairing failed",
          description: response.error,
        });
      } else {
        toast({
          title: "Pairing successful",
          description: `Paired with ${response.serverName}`,
        });
        setPairingCode("");
        setPairingServerId(null);
        loadPairedServers();
      }
    } catch (e) {
      toast({
        variant: "destructive",
        title: "Error",
        description: e instanceof Error ? e.message : "Unknown error",
      });
    } finally {
      setIsPairing(false);
    }
  };

  const handleConnect = (serverId: string) => {
    if (window.Android) {
      window.Android.connectToPairedServer(serverId);
      toast({
        title: "Connecting",
        description: "Connecting to server...",
      });
    }
  };

  const isServerPaired = (serverId: string) => {
    return pairedServers.some((s) => s.serverId === serverId);
  };

  // Vérifier si on est dans l'environnement Android
  const isAndroid = typeof window !== "undefined" && window.Android !== undefined;

  return (
    <div className="space-y-4">
      {/* Message de débogage visible - TOUJOURS AFFICHÉ */}
      <div className="text-xs text-mono text-muted-foreground bg-blue-500/10 border border-blue-500/20 p-2 rounded mb-2">
        <strong>DEBUG DiscoveryTab:</strong> Rendered | isAndroid: {String(isAndroid)} | Discovered: {discoveredServers.length} | Paired: {pairedServers.length} | Discovering: {String(isDiscovering)}
      </div>

      {/* Avertissement si le bridge Android n'est pas disponible */}
      {!isAndroid && (
        <Card className="bg-yellow-500/10 border-yellow-500/20">
          <CardHeader>
            <CardTitle className="text-mono text-sm text-yellow-600 dark:text-yellow-400">
              ⚠️ Android Bridge non disponible
            </CardTitle>
            <CardDescription className="text-mono text-xs">
              La découverte automatique n'est disponible que dans l'application Android.
            </CardDescription>
          </CardHeader>
          <CardContent>
            <div className="text-sm text-muted-foreground text-mono">
              Veuillez utiliser l'application mobile pour découvrir et appairer des serveurs.
            </div>
          </CardContent>
        </Card>
      )}

      {/* Discovery Status */}
      <Card>
        <CardHeader>
          <CardTitle className="text-mono text-sm">Server Discovery</CardTitle>
          <CardDescription className="text-mono text-xs">
            Automatically discover servers on your local network
          </CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2">
              {isDiscovering ? (
                <Loader2 className="h-4 w-4 animate-spin" />
              ) : (
                <Pause className="h-4 w-4" />
              )}
              <span className="text-sm text-mono">
                {isDiscovering
                  ? `Discovering... (${discoveredServers.length} found)`
                  : `Stopped (${discoveredServers.length} found)`}
              </span>
            </div>
            <Button
              onClick={handleToggleDiscovery}
              variant={isDiscovering ? "destructive" : "default"}
              size="sm"
              className="text-mono text-xs"
              disabled={!isAndroid}
            >
              {isDiscovering ? (
                <>
                  <Pause className="h-3 w-3 mr-1" />
                  Stop
                </>
              ) : (
                <>
                  <Search className="h-3 w-3 mr-1" />
                  Start
                </>
              )}
            </Button>
          </div>
        </CardContent>
      </Card>

      {/* Discovered Servers */}
      <Card>
        <CardHeader>
          <CardTitle className="text-mono text-sm">Discovered Servers</CardTitle>
          <CardDescription className="text-mono text-xs">
            {discoveredServers.length > 0
              ? `${discoveredServers.length} server(s) found`
              : "No servers discovered yet"}
          </CardDescription>
        </CardHeader>
        {discoveredServers.length > 0 ? (
          <CardContent className="space-y-2">
            {discoveredServers.map((server) => {
              const isPaired = isServerPaired(server.serverId);
              const isPairingThis = pairingServerId === server.serverId;

              return (
                <Card key={server.serverId} className="p-3">
                  <div className="flex items-center justify-between">
                    <div className="flex-1">
                      <div className="flex items-center gap-2">
                        <span className="text-sm font-medium text-mono">{server.serverName}</span>
                        {isPaired && (
                          <CheckCircle2 className="h-4 w-4 text-green-500" />
                        )}
                      </div>
                      <div className="text-xs text-muted-foreground text-mono mt-1">
                        {server.host}:{server.port} ({server.protocol})
                      </div>
                    </div>
                    <div className="flex gap-2">
                      {isPaired ? (
                        <Button
                          onClick={() => handleConnect(server.serverId)}
                          size="sm"
                          variant="outline"
                          className="text-mono text-xs"
                        >
                          Connect
                        </Button>
                      ) : (
                        <Button
                          onClick={() => handleRequestPairing(server.serverId)}
                          size="sm"
                          disabled={isPairingThis}
                          className="text-mono text-xs"
                        >
                          {isPairingThis ? (
                            <>
                              <Loader2 className="h-3 w-3 mr-1 animate-spin" />
                              Pairing...
                            </>
                          ) : (
                            "Pair"
                          )}
                        </Button>
                      )}
                    </div>
                  </div>
                  {isPairingThis && pairingCode && (
                    <div className="mt-3 space-y-2">
                      <Label className="text-mono text-xs">Pairing Code: {pairingCode}</Label>
                      <div className="flex gap-2">
                        <Input
                          value={pairingCode}
                          readOnly
                          className="bg-secondary border-border text-mono text-xs"
                        />
                        <Button
                          onClick={handleConfirmPairing}
                          size="sm"
                          disabled={isPairing}
                          className="text-mono text-xs"
                        >
                          {isPairing ? (
                            <>
                              <Loader2 className="h-3 w-3 mr-1 animate-spin" />
                              Confirming...
                            </>
                          ) : (
                            "Confirm"
                          )}
                        </Button>
                      </div>
                    </div>
                  )}
                </Card>
              );
            })}
          </CardContent>
        ) : (
          <CardContent>
            <div className="text-sm text-muted-foreground text-mono text-center py-4">
              {isDiscovering
                ? "Searching for servers on your network..."
                : "No servers found. Click 'Start' to begin discovery."}
            </div>
          </CardContent>
        )}
      </Card>

      {/* Paired Servers */}
      <Card>
        <CardHeader>
          <CardTitle className="text-mono text-sm">Paired Servers</CardTitle>
          <CardDescription className="text-mono text-xs">
            {pairedServers.length > 0
              ? `${pairedServers.length} server(s) paired`
              : "No servers paired yet"}
          </CardDescription>
        </CardHeader>
        {pairedServers.length > 0 ? (
          <CardContent className="space-y-2">
            {pairedServers.map((server) => (
              <Card key={server.serverId} className="p-3">
                <div className="flex items-center justify-between">
                  <div className="flex-1">
                    <div className="flex items-center gap-2">
                      <span className="text-sm font-medium text-mono">{server.serverName}</span>
                      <CheckCircle2 className="h-4 w-4 text-green-500" />
                    </div>
                    <div className="text-xs text-muted-foreground text-mono mt-1">
                      {server.host}:{server.port} ({server.protocol})
                    </div>
                    {server.isAutoConnect && (
                      <div className="text-xs text-muted-foreground text-mono mt-1">
                        Auto-connect enabled
                      </div>
                    )}
                  </div>
                  <Button
                    onClick={() => handleConnect(server.serverId)}
                    size="sm"
                    variant="outline"
                    className="text-mono text-xs"
                  >
                    Connect
                  </Button>
                </div>
              </Card>
            ))}
          </CardContent>
        ) : (
          <CardContent>
            <div className="text-sm text-muted-foreground text-mono text-center py-4">
              No servers have been paired yet. Discover and pair a server to get started.
            </div>
          </CardContent>
        )}
      </Card>

      {/* Debug info */}
      {process.env.NODE_ENV === 'development' && (
        <Card className="bg-muted/50">
          <CardHeader>
            <CardTitle className="text-mono text-xs">Debug Info</CardTitle>
          </CardHeader>
          <CardContent className="text-xs text-mono space-y-1">
            <div>Android Bridge: {isAndroid ? "✓ Available" : "✗ Not available"}</div>
            <div>Discovering: {isDiscovering ? "Yes" : "No"}</div>
            <div>Discovered Servers: {discoveredServers.length}</div>
            <div>Paired Servers: {pairedServers.length}</div>
            {discoveredServers.length > 0 && (
              <div className="mt-2">
                <div className="font-semibold">Discovered:</div>
                {discoveredServers.map(s => (
                  <div key={s.serverId} className="pl-2">- {s.serverName} ({s.host}:{s.port})</div>
                ))}
              </div>
            )}
            {pairedServers.length > 0 && (
              <div className="mt-2">
                <div className="font-semibold">Paired:</div>
                {pairedServers.map(s => (
                  <div key={s.serverId} className="pl-2">- {s.serverName} ({s.host}:{s.port})</div>
                ))}
              </div>
            )}
          </CardContent>
        </Card>
      )}

      {discoveredServers.length === 0 && pairedServers.length === 0 && !isDiscovering && (
        <div className="text-sm text-muted-foreground text-mono text-center py-8">
          No servers found. Start discovery to find servers on your network.
          {!isAndroid && (
            <div className="mt-2 text-xs text-destructive">
              ⚠️ Android Bridge not available
            </div>
          )}
        </div>
      )}
    </div>
  );
};

