import { useState, useEffect, useCallback } from "react";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { useToast } from "@/hooks/use-toast";
import { Loader2, RefreshCw, CheckCircle2 } from "lucide-react";
import { logger } from "@/lib/logger";

interface DiscoveredServer {
  serverId: string;
  serverName?: string;
  host?: string;
  port?: number;
  protocol?: string;
  version?: string;
}

interface PairedServer {
  serverId: string;
  serverName?: string;
  host?: string;
  port?: number;
  protocol?: string;
  pairedAt?: string | number;
  lastSeen?: string | number;
  isAutoConnect?: boolean;
  fingerprint?: string;
}

export const DiscoveryTab = () => {
  const { toast } = useToast();
  const [isDiscovering, setIsDiscovering] = useState(false);
  const [discoveredServers, setDiscoveredServers] = useState<DiscoveredServer[]>([]);
  const [pairedServers, setPairedServers] = useState<PairedServer[]>([]);
  const [pairingCode, setPairingCode] = useState<string>("");
  const [pairingServerId, setPairingServerId] = useState<string | null>(null);
  const [isPairing, setIsPairing] = useState(false);

  const fetchJson = useCallback(async <T,>(url: string, options?: RequestInit): Promise<T> => {
    const response = await fetch(url, {
      headers: { "Content-Type": "application/json" },
      ...options,
    });
    if (!response.ok) {
      throw new Error(`${response.status} ${response.statusText}`);
    }
    return (await response.json()) as T;
  }, []);

  const loadDiscoveredServers = useCallback(async () => {
    setIsDiscovering(true);
    try {
      const data = await fetchJson<DiscoveredServer & { capabilities?: unknown }>("/discovery");
      setDiscoveredServers([
        {
          serverId: data.serverId,
          serverName: data.serverName ?? "Control Deck",
          host: data.host,
          port: data.port,
          protocol: data.protocol,
          version: data.version,
        },
      ]);
    } catch (e) {
      toast({
        variant: "destructive",
        title: "Discovery failed",
        description: e instanceof Error ? e.message : "Unable to reach discovery endpoint",
      });
      logger.error("DiscoveryTab: discovery failed", e);
      setDiscoveredServers([]);
    } finally {
      setIsDiscovering(false);
    }
  }, [fetchJson, toast]);

  const loadPairedServers = useCallback(async () => {
    try {
      const data = await fetchJson<{ servers?: PairedServer[] | Record<string, string> }>(
        "/discovery/pairing/servers"
      );

      if (!data.servers) {
        setPairedServers([]);
        return;
      }

      if (Array.isArray(data.servers)) {
        setPairedServers(data.servers);
      } else {
        const mapped = Object.entries(data.servers).map(([serverId, fingerprint]) => ({
          serverId,
          serverName: serverId,
          protocol: window.location.protocol === "https:" ? "wss" : "ws",
          host: window.location.hostname,
          port: window.location.port ? Number(window.location.port) : undefined,
          lastSeen: Date.now(),
          isAutoConnect: true,
          pairedAt: Date.now(),
          // fingerprint kept for potential future display/use
          fingerprint,
        }));
        setPairedServers(mapped);
      }
    } catch (e) {
      logger.error("DiscoveryTab: failed to load paired servers", e);
      setPairedServers([]);
    }
  }, [fetchJson]);

  useEffect(() => {
    loadDiscoveredServers();
    loadPairedServers();
  }, [loadDiscoveredServers, loadPairedServers]);

  const handleRequestPairing = async (serverId?: string) => {
    setIsPairing(true);
    setPairingServerId(serverId ?? "python-backend");
    try {
      const data = await fetchJson<{ code: string; serverId: string }>("/discovery/pairing/request", {
        method: "POST",
      });
      setPairingCode(data.code);
      setPairingServerId(data.serverId);
      toast({ title: "Pairing code", description: `Code: ${data.code}` });
    } catch (e) {
      toast({
        variant: "destructive",
        title: "Pairing failed",
        description: e instanceof Error ? e.message : "Unable to request pairing code",
      });
      setPairingServerId(null);
      setPairingCode("");
    } finally {
      setIsPairing(false);
    }
  };

  const handleConfirmPairing = async () => {
    if (!pairingServerId || !pairingCode) return;
    setIsPairing(true);
    try {
      const params = new URLSearchParams({ code: pairingCode, serverId: pairingServerId });
      await fetchJson("/discovery/pairing/confirm?" + params.toString(), { method: "POST" });
      toast({ title: "Paired", description: `Server ${pairingServerId} paired` });
      setPairingCode("");
      setPairingServerId(null);
      loadPairedServers();
    } catch (e) {
      toast({
        variant: "destructive",
        title: "Confirmation failed",
        description: e instanceof Error ? e.message : "Unable to confirm pairing",
      });
    } finally {
      setIsPairing(false);
    }
  };

  const isServerPaired = (serverId: string) => pairedServers.some((s) => s.serverId === serverId);

  return (
    <div className="space-y-4">
      <Card>
        <CardHeader>
          <CardTitle className="text-mono text-sm">Server Discovery</CardTitle>
          <CardDescription className="text-mono text-xs">
            Discover the backend serving this UI and manage pairing
          </CardDescription>
        </CardHeader>
        <CardContent className="flex items-center justify-between">
          <div className="flex items-center gap-2 text-sm text-mono">
            {isDiscovering ? (
              <Loader2 className="h-4 w-4 animate-spin" />
            ) : (
              <RefreshCw className="h-4 w-4" />
            )}
            <span>{isDiscovering ? "Discovering..." : `${discoveredServers.length} server(s) found`}</span>
          </div>
          <Button
            onClick={() => {
              loadDiscoveredServers();
              loadPairedServers();
            }}
            size="sm"
            className="text-mono text-xs"
          >
            <RefreshCw className="h-3 w-3 mr-1" />
            Refresh
          </Button>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle className="text-mono text-sm">Discovered Servers</CardTitle>
          <CardDescription className="text-mono text-xs">
            {discoveredServers.length > 0
              ? `${discoveredServers.length} server(s) available`
              : isDiscovering
                ? "Searching for servers..."
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
                        {isPaired && <CheckCircle2 className="h-4 w-4 text-green-500" />}
                      </div>
                      <div className="text-xs text-muted-foreground text-mono mt-1">
                        {server.host}:{server.port} ({server.protocol})
                      </div>
                    </div>
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
                        isPaired ? "Paired" : "Pair"
                      )}
                    </Button>
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
              {isDiscovering ? "Searching for servers on your network..." : "No servers found."}
            </div>
          </CardContent>
        )}
      </Card>

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
                      <span className="text-sm font-medium text-mono">{server.serverName || server.serverId}</span>
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
                </div>
              </Card>
            ))}
          </CardContent>
        ) : (
          <CardContent>
            <div className="text-sm text-muted-foreground text-mono text-center py-4">
              No servers have been paired yet. Request a pairing code to get started.
            </div>
          </CardContent>
        )}
      </Card>
    </div>
  );
};

