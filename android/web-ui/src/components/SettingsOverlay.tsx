import { DiscoveryTab } from "@/components/DiscoveryTab";
import { Button } from "@/components/ui/button";
import {
  Form,
  FormControl,
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
} from "@/components/ui/form";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { useToast } from "@/hooks/use-toast";
import { useConnectionSettings } from "@/hooks/useConnectionSettings";
import { useProfiles } from "@/hooks/useProfiles";
import { useWebSocket } from "@/hooks/useWebSocket";
import { logger } from "@/lib/logger";
import { zodResolver } from "@hookform/resolvers/zod";
import { X } from "lucide-react";
import { useEffect, useState } from "react";
import { useForm } from "react-hook-form";
import * as z from "zod";

type TabValue = "discovery" | "connection" | "profiles" | "plugins";

interface SettingsOverlayProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onConnect?: () => void;
  onProfileSelect?: (profileId: string) => void;
}

const connectionFormSchema = z.object({
  serverIp: z
    .string()
    .min(1, "Server host is required")
    .regex(/^[a-zA-Z0-9.:-]+$/, "Host must be a hostname or IP"),
  serverPort: z
    .string()
    .min(1, "Server Port is required")
    .refine(
      (val) => {
        const port = parseInt(val, 10);
        return !isNaN(port) && port >= 1 && port <= 65535;
      },
      { message: "Port must be between 1 and 65535" }
    ),
  useTls: z.boolean().default(false),
});

type ConnectionFormValues = z.infer<typeof connectionFormSchema>;

export const SettingsOverlay = ({
  open,
  onOpenChange,
  onConnect,
  onProfileSelect,
}: SettingsOverlayProps) => {
  const { settings, saveSettings, getWebSocketUrl, getHttpUrl } =
    useConnectionSettings();
  const { toast } = useToast();
  const ws = useWebSocket();
  const profiles = useProfiles({
    serverUrl: getHttpUrl(settings),
    token: settings.token,
  });

  const form = useForm<ConnectionFormValues>({
    resolver: zodResolver(connectionFormSchema),
    defaultValues: {
      serverIp: settings.serverIp,
      serverPort: settings.serverPort.toString(),
      useTls: settings.useTls,
    },
    mode: "onChange",
  });

  // Réinitialiser le formulaire quand le dialog s'ouvre ou que settings change
  useEffect(() => {
    if (open) {
      logger.debug("SettingsOverlay: open, resetting form", settings);
      form.reset({
        serverIp: settings.serverIp,
        serverPort: settings.serverPort.toString(),
        useTls: settings.useTls,
      });
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [open, settings.serverIp, settings.serverPort, settings.useTls]);

  const onSubmit = async (values: ConnectionFormValues) => {
    logger.debug("SettingsOverlay: submit connection", values);
    const port = parseInt(values.serverPort, 10);

    saveSettings({
      serverIp: values.serverIp.trim(),
      serverPort: port,
      useTls: values.useTls,
    });

    const updatedSettings = {
      ...settings,
      serverIp: values.serverIp.trim(),
      serverPort: port,
      useTls: values.useTls,
    };

    try {
      const wsUrl = getWebSocketUrl(updatedSettings);
      logger.debug("SettingsOverlay: connect via WS URL", wsUrl);
      ws.connect({
        url: wsUrl,
        token: updatedSettings.token,
        heartbeatInterval: 15000,
        reconnectDelay: 1000,
        maxReconnectAttempts: 6,
      });

      toast({
        title: "Connecting...",
        description: `Connecting to ${values.serverIp}:${port}`,
      });

      onConnect?.();
    } catch (error) {
      logger.error("SettingsOverlay: connection failed", error);
      toast({
        variant: "destructive",
        title: "Connection failed",
        description:
          error instanceof Error ? error.message : "Failed to connect",
      });
    }
  };

  const handleDisconnect = () => {
    logger.debug("SettingsOverlay: disconnect");
    ws.disconnect();
    toast({
      title: "Disconnected",
      description: "Connection closed",
    });
  };

  const handleProfileSelect = async (profileId: string) => {
    logger.debug("SettingsOverlay: handleProfileSelect", profileId);
    try {
      await ws.selectProfile(profileId);
      await profiles.selectProfile(profileId);
      toast({
        title: "Profile selected",
        description: `Switched to profile: ${
          profiles.profiles.find((p) => p.id === profileId)?.name || profileId
        }`,
      });
      onProfileSelect?.(profileId);
    } catch (error) {
      toast({
        variant: "destructive",
        title: "Failed to select profile",
        description: error instanceof Error ? error.message : "Unknown error",
      });
    }
  };

  const [activeTab, setActiveTab] = useState<TabValue>("discovery");

  const tabs: { value: TabValue; label: string }[] = [
    { value: "discovery", label: "Discovery" },
    { value: "connection", label: "Connection" },
    { value: "profiles", label: "Profiles" },
    { value: "plugins", label: "Plugins" },
  ];

  // Ne rien afficher si fermé
  if (!open) return null;

  console.log(
    "[SettingsOverlay] Rendering modal, open:",
    open,
    "activeTab:",
    activeTab
  );

  // Utiliser une approche sans transform pour Android WebView
  // Le conteneur principal utilise flexbox pour centrer le modal
  const modalContent = (
    <div
      style={{
        position: "fixed",
        top: 0,
        left: 0,
        right: 0,
        bottom: 0,
        width: "100%",
        height: "100%",
        display: "flex",
        alignItems: "center",
        justifyContent: "center",
        backgroundColor: "rgba(0, 0, 0, 0.85)",
        zIndex: 9999,
      }}
      onClick={(e) => {
        // Fermer seulement si on clique sur le backdrop (pas sur le modal)
        if (e.target === e.currentTarget) {
          onOpenChange(false);
        }
      }}
    >
      {/* Modal Container - centré via flexbox parent */}
      <div
        style={{
          width: "90%",
          maxWidth: "672px",
          height: "500px",
          backgroundColor: "#1a1d24",
          border: "2px solid #00d4ff",
          borderRadius: "8px",
          boxShadow: "0 25px 50px -12px rgba(0, 0, 0, 0.5)",
          display: "flex",
          flexDirection: "column" as const,
          overflow: "hidden",
        }}
        onClick={(e) => e.stopPropagation()}
      >
        {/* Header */}
        <div
          style={{
            flexShrink: 0,
            padding: "12px 16px",
            borderBottom: "1px solid #333",
            backgroundColor: "#1e2128",
          }}
        >
          <div
            style={{
              display: "flex",
              alignItems: "flex-start",
              justifyContent: "space-between",
              gap: "16px",
            }}
          >
            <div style={{ minWidth: 0 }}>
              <h2
                style={{
                  color: "#ffffff",
                  fontSize: "18px",
                  fontWeight: 600,
                  margin: 0,
                }}
              >
                Control Deck Settings
              </h2>
              <p
                style={{
                  color: "#888888",
                  fontSize: "14px",
                  marginTop: "4px",
                  margin: 0,
                }}
              >
                Manage your server connection and settings
              </p>
            </div>
            <button
              onClick={() => onOpenChange(false)}
              style={{
                flexShrink: 0,
                padding: "4px",
                background: "transparent",
                border: "none",
                borderRadius: "4px",
                cursor: "pointer",
                color: "#ffffff",
              }}
            >
              <X size={20} />
            </button>
          </div>
        </div>

        {/* Tabs */}
        <div
          style={{
            flexShrink: 0,
            display: "flex",
            overflowX: "auto",
            borderBottom: "1px solid #333",
            backgroundColor: "#15181e",
          }}
        >
          {tabs.map((tab) => (
            <button
              key={tab.value}
              onClick={() => setActiveTab(tab.value)}
              style={{
                flexShrink: 0,
                padding: "8px 16px",
                fontSize: "14px",
                fontWeight: 500,
                fontFamily: "monospace",
                border: "none",
                borderBottom:
                  activeTab === tab.value
                    ? "2px solid #00d4ff"
                    : "2px solid transparent",
                color: activeTab === tab.value ? "#ffffff" : "#888888",
                backgroundColor:
                  activeTab === tab.value
                    ? "rgba(0,212,255,0.1)"
                    : "transparent",
                cursor: "pointer",
                whiteSpace: "nowrap",
              }}
            >
              {tab.label}
            </button>
          ))}
        </div>

        {/* Content */}
        <div
          style={{
            flex: 1,
            overflowY: "auto",
            padding: "16px",
            backgroundColor: "#1a1d24",
            color: "#ffffff",
          }}
        >
          {activeTab === "discovery" && <DiscoveryTab />}{" "}
          {activeTab === "connection" && (
            <Form {...form}>
              <form
                onSubmit={form.handleSubmit(onSubmit)}
                className="space-y-4"
              >
                <FormField
                  control={form.control}
                  name="serverIp"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel className="text-mono text-sm">
                        Server Host / IP
                      </FormLabel>
                      <FormControl>
                        <Input
                          placeholder="my.server.com or 203.0.113.10"
                          className="bg-secondary border-border text-mono"
                          autoComplete="off"
                          {...field}
                        />
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  )}
                />
                <FormField
                  control={form.control}
                  name="serverPort"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel className="text-mono text-sm">
                        Server Port
                      </FormLabel>
                      <FormControl>
                        <Input
                          type="number"
                          placeholder="4455"
                          className="bg-secondary border-border text-mono"
                          autoComplete="off"
                          {...field}
                        />
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  )}
                />
                <FormField
                  control={form.control}
                  name="useTls"
                  render={({ field }) => (
                    <FormItem className="flex flex-row items-center space-x-2 space-y-0">
                      <FormControl>
                        <input
                          type="checkbox"
                          checked={field.value}
                          onChange={field.onChange}
                          className="rounded border-border"
                        />
                      </FormControl>
                      <FormLabel className="text-mono text-sm cursor-pointer">
                        Use TLS (WSS)
                      </FormLabel>
                    </FormItem>
                  )}
                />
                <div className="flex gap-1.5 xs:gap-2">
                  {ws.status === "online" ? (
                    <Button
                      type="button"
                      onClick={handleDisconnect}
                      className="flex-1 bg-destructive text-destructive-foreground hover:bg-destructive/90 text-xs xs:text-sm"
                    >
                      Disconnect
                    </Button>
                  ) : (
                    <Button
                      type="submit"
                      disabled={
                        ws.status === "connecting" || !form.formState.isValid
                      }
                      className="flex-1 bg-primary text-primary-foreground hover:bg-primary/90 text-xs xs:text-sm"
                    >
                      {ws.status === "connecting" ? "Connecting..." : "Connect"}
                    </Button>
                  )}
                </div>
                {ws.status === "online" && (
                  <div className="text-[10px] xs:text-xs text-mono text-muted-foreground text-center">
                    Connected to {form.watch("serverIp")}:
                    {form.watch("serverPort")}
                  </div>
                )}
                {ws.error && (
                  <div className="text-[10px] xs:text-xs text-mono text-destructive text-center">
                    {ws.error}
                  </div>
                )}
              </form>
            </Form>
          )}
          {activeTab === "profiles" && (
            <>
              {profiles.loading ? (
                <div className="text-xs xs:text-sm text-muted-foreground text-mono text-center py-8">
                  Loading profiles...
                </div>
              ) : profiles.error ? (
                <div className="text-xs xs:text-sm text-destructive text-mono text-center py-8">
                  {profiles.error}
                </div>
              ) : profiles.profiles.length === 0 ? (
                <div className="text-xs xs:text-sm text-muted-foreground text-mono text-center py-8">
                  No profiles available
                </div>
              ) : (
                <div className="space-y-2">
                  <Label className="text-mono text-xs xs:text-sm">
                    Select Profile
                  </Label>
                  <Select
                    value={profiles.selectedProfile?.id || ""}
                    onValueChange={handleProfileSelect}
                  >
                    <SelectTrigger className="bg-secondary border-border text-mono">
                      <SelectValue placeholder="Select a profile" />
                    </SelectTrigger>
                    <SelectContent>
                      {profiles.profiles.map((profile) => (
                        <SelectItem key={profile.id} value={profile.id}>
                          {profile.name}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                  {profiles.selectedProfile && (
                    <div className="text-[10px] xs:text-xs text-mono text-muted-foreground mt-2">
                      Grid: {profiles.selectedProfile.cols}x
                      {profiles.selectedProfile.rows} | Controls:{" "}
                      {profiles.selectedProfile.controls.length}
                    </div>
                  )}
                </div>
              )}
            </>
          )}
          {activeTab === "plugins" && (
            <div className="text-xs xs:text-sm text-muted-foreground text-mono text-center py-8">
              Plugin configuration coming soon
            </div>
          )}
        </div>
      </div>
    </div>
  );

  // Retourner le contenu directement sans portal
  return modalContent;
};
