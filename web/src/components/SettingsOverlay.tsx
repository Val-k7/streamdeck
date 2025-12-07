import { useEffect } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import * as z from "zod";
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogDescription } from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Form, FormControl, FormField, FormItem, FormLabel, FormMessage } from "@/components/ui/form";
import { Input } from "@/components/ui/input";
import { useConnectionSettings } from "@/hooks/useConnectionSettings";
import { useProfiles } from "@/hooks/useProfiles";
import { useWebSocket } from "@/hooks/useWebSocket";
import { useToast } from "@/hooks/use-toast";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Label } from "@/components/ui/label";
import { DiscoveryTab } from "@/components/DiscoveryTab";
import { logger } from "@/lib/logger";

interface SettingsOverlayProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onConnect?: () => void;
  onProfileSelect?: (profileId: string) => void;
}

const connectionFormSchema = z.object({
  // Autoriser hostname ou IP (publique ou locale) pour le debug distant
  serverIp: z
    .string()
    .min(1, "Server host is required")
    .regex(/^[a-zA-Z0-9.:-]+$/, "Host must be a hostname or IP"),
  serverPort: z.string().min(1, "Server Port is required").refine(
    (val) => {
      const port = parseInt(val, 10);
      return !isNaN(port) && port >= 1 && port <= 65535;
    },
    { message: "Port must be between 1 and 65535" }
  ),
  useTls: z.boolean().default(false),
});

type ConnectionFormValues = z.infer<typeof connectionFormSchema>;

export const SettingsOverlay = ({ open, onOpenChange, onConnect, onProfileSelect }: SettingsOverlayProps) => {
  const { settings, saveSettings, getWebSocketUrl, getHttpUrl } = useConnectionSettings();
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
      form.reset({
        serverIp: settings.serverIp,
        serverPort: settings.serverPort.toString(),
        useTls: settings.useTls,
      });
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [open, settings.serverIp, settings.serverPort, settings.useTls]);

  const onSubmit = async (values: ConnectionFormValues) => {
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
      toast({
        variant: "destructive",
        title: "Connection failed",
        description: error instanceof Error ? error.message : "Failed to connect",
      });
    }
  };

  const handleDisconnect = () => {
    ws.disconnect();
    toast({
      title: "Disconnected",
      description: "Connection closed",
    });
  };

  const handleProfileSelect = async (profileId: string) => {
    try {
      await ws.selectProfile(profileId);
      await profiles.selectProfile(profileId);
      toast({
        title: "Profile selected",
        description: `Switched to profile: ${profiles.profiles.find(p => p.id === profileId)?.name || profileId}`,
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

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent
        className="sm:max-w-[640px] w-[95vw] max-h-[90vh] bg-card text-foreground border border-border p-0 overflow-hidden"
        style={{ display: "flex", flexDirection: "column" }}
      >
        <DialogHeader className="flex-shrink-0">
          <DialogTitle className="text-mono text-foreground">Control Deck Settings</DialogTitle>
          <DialogDescription className="text-sm text-muted-foreground">
            Manage your server connection, profiles, and application settings.
          </DialogDescription>
        </DialogHeader>

        <Tabs defaultValue="discovery" className="w-full flex flex-col flex-1 min-h-0 overflow-hidden">
          <TabsList className="grid w-full grid-cols-4 bg-muted text-foreground flex-shrink-0 rounded-none border-b">
            <TabsTrigger value="discovery" className="text-mono text-xs rounded-none data-[state=active]:bg-background data-[state=active]:border-b-2 data-[state=active]:border-primary">
              Discovery
            </TabsTrigger>
            <TabsTrigger value="connection" className="text-mono text-xs rounded-none data-[state=active]:bg-background data-[state=active]:border-b-2 data-[state=active]:border-primary">Connection</TabsTrigger>
            <TabsTrigger value="profiles" className="text-mono text-xs rounded-none data-[state=active]:bg-background data-[state=active]:border-b-2 data-[state=active]:border-primary">Profiles</TabsTrigger>
            <TabsTrigger value="plugins" className="text-mono text-xs rounded-none data-[state=active]:bg-background data-[state=active]:border-b-2 data-[state=active]:border-primary">Plugins</TabsTrigger>
          </TabsList>

          <div className="flex-1 min-h-0 overflow-hidden flex flex-col">
            <TabsContent value="discovery" className="flex-1 min-h-0 overflow-y-auto p-6 mt-0">
            <div className="text-xs text-mono text-muted-foreground mb-2">
              Debug: Discovery tab loaded
            </div>
            {(() => {
              try {
                return <DiscoveryTab />;
              } catch (error) {
                logger.error("Error in DiscoveryTab:", error);
                return (
                  <div className="space-y-4">
                    <div className="text-sm text-destructive text-mono text-center py-4 border border-destructive rounded p-4">
                      Erreur lors du chargement de l'onglet Discovery: {error instanceof Error ? error.message : String(error)}
                    </div>
                    <div className="text-xs text-mono text-muted-foreground">
                      Stack: {error instanceof Error ? error.stack : "N/A"}
                    </div>
                  </div>
                );
              }
            })()}
            </TabsContent>

            <TabsContent value="connection" className="flex-1 min-h-0 overflow-y-auto p-6 mt-0">
            <Form {...form}>
              <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4">
                <FormField
                  control={form.control}
                  name="serverIp"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel className="text-mono text-sm">Server Host / IP</FormLabel>
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
                      <FormLabel className="text-mono text-sm">Server Port</FormLabel>
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
                <div className="flex gap-2">
                  {ws.status === "online" ? (
                    <Button
                      type="button"
                      onClick={handleDisconnect}
                      className="flex-1 bg-destructive text-destructive-foreground hover:bg-destructive/90"
                    >
                      Disconnect
                    </Button>
                  ) : (
                    <Button
                      type="submit"
                      disabled={ws.status === "connecting" || !form.formState.isValid}
                      className="flex-1 bg-primary text-primary-foreground hover:bg-primary/90"
                    >
                      {ws.status === "connecting" ? "Connecting..." : "Connect"}
                    </Button>
                  )}
                </div>
                {ws.status === "online" && (
                  <div className="text-xs text-mono text-muted-foreground text-center">
                    Connected to {form.watch("serverIp")}:{form.watch("serverPort")}
                  </div>
                )}
                {ws.error && (
                  <div className="text-xs text-mono text-destructive text-center">
                    {ws.error}
                  </div>
                )}
              </form>
            </Form>
            </TabsContent>

            <TabsContent value="profiles" className="flex-1 min-h-0 overflow-y-auto p-6 mt-0">
            {profiles.loading ? (
              <div className="text-sm text-muted-foreground text-mono text-center py-8">
                Loading profiles...
              </div>
            ) : profiles.error ? (
              <div className="text-sm text-destructive text-mono text-center py-8">
                {profiles.error}
              </div>
            ) : profiles.profiles.length === 0 ? (
              <div className="text-sm text-muted-foreground text-mono text-center py-8">
                No profiles available
              </div>
            ) : (
              <div className="space-y-2">
                <Label className="text-mono text-sm">Select Profile</Label>
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
                  <div className="text-xs text-mono text-muted-foreground mt-2">
                    Grid: {profiles.selectedProfile.cols}x{profiles.selectedProfile.rows} |
                    Controls: {profiles.selectedProfile.controls.length}
                  </div>
                )}
              </div>
            )}
            </TabsContent>

            <TabsContent value="plugins" className="flex-1 min-h-0 overflow-y-auto p-6 mt-0">
              <div className="text-sm text-muted-foreground text-mono text-center py-8">
                Plugin configuration coming soon
              </div>
            </TabsContent>
          </div>
        </Tabs>
      </DialogContent>
    </Dialog>
  );
};
