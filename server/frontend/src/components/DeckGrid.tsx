import { PadConfig } from "@/hooks/useDeckStorage";
import { logger } from "@/lib/logger";
import {
  Circle,
  Headphones,
  Layers,
  LucideIcon,
  MessageSquare,
  Mic,
  MicOff,
  Monitor,
  Music,
  Play,
  Radio,
  RotateCcw,
  Square,
  Video,
  Volume2,
  Wifi,
  Zap,
} from "lucide-react";
import { useState } from "react";
import { ControlPad } from "./ControlPad";
import { PadEditor, getIconByName } from "./PadEditor";

// Default profile decks (used when no localStorage data exists)
const defaultProfileDecks: Record<string, PadConfig[]> = {
  streaming: [
    {
      id: "1",
      type: "toggle",
      size: "1x1",
      label: "Mic",
      iconName: "Mic",
      color: "destructive",
      defaultActive: true,
    },
    {
      id: "2",
      type: "toggle",
      size: "1x1",
      label: "Cam",
      iconName: "Video",
      color: "accent",
    },
    {
      id: "3",
      type: "fader",
      size: "1x2",
      label: "Master",
      color: "primary",
      defaultValue: 75,
    },
    {
      id: "4",
      type: "button",
      size: "1x1",
      label: "Scene 1",
      iconName: "Monitor",
      color: "primary",
    },
    {
      id: "5",
      type: "encoder",
      size: "2x2",
      label: "Pan",
      color: "accent",
      defaultValue: 50,
    },
    {
      id: "6",
      type: "toggle",
      size: "2x1",
      label: "Go Live",
      iconName: "Circle",
      color: "destructive",
    },
    {
      id: "7",
      type: "fader",
      size: "2x1",
      label: "Music Volume",
      color: "accent",
      defaultValue: 45,
    },
    {
      id: "8",
      type: "button",
      size: "1x1",
      label: "Chat",
      iconName: "MessageSquare",
      color: "muted",
    },
    {
      id: "9",
      type: "button",
      size: "1x1",
      label: "Alert",
      iconName: "Zap",
      color: "accent",
    },
  ],
  audio: [
    {
      id: "1",
      type: "fader",
      size: "1x2",
      label: "CH 1",
      color: "primary",
      defaultValue: 80,
    },
    {
      id: "2",
      type: "fader",
      size: "1x2",
      label: "CH 2",
      color: "primary",
      defaultValue: 65,
    },
    {
      id: "3",
      type: "encoder",
      size: "2x2",
      label: "EQ Low",
      color: "accent",
      defaultValue: 50,
    },
    {
      id: "4",
      type: "fader",
      size: "1x2",
      label: "Master",
      color: "destructive",
      defaultValue: 90,
    },
    {
      id: "5",
      type: "toggle",
      size: "1x1",
      label: "Mute 1",
      iconName: "MicOff",
      color: "muted",
    },
    {
      id: "6",
      type: "toggle",
      size: "1x1",
      label: "Mute 2",
      iconName: "MicOff",
      color: "muted",
    },
    {
      id: "7",
      type: "encoder",
      size: "1x1",
      label: "Rev",
      color: "primary",
      defaultValue: 30,
    },
    {
      id: "8",
      type: "toggle",
      size: "1x1",
      label: "Mute M",
      iconName: "Volume2",
      color: "destructive",
    },
    {
      id: "9",
      type: "fader",
      size: "2x1",
      label: "FX Send",
      color: "accent",
      defaultValue: 30,
    },
    {
      id: "10",
      type: "button",
      size: "1x1",
      label: "Solo",
      iconName: "Headphones",
      color: "accent",
    },
    {
      id: "11",
      type: "button",
      size: "1x1",
      label: "Rec",
      iconName: "Circle",
      color: "destructive",
    },
  ],
  macros: [
    {
      id: "1",
      type: "button",
      size: "2x2",
      label: "Start Stream",
      iconName: "Play",
      color: "primary",
    },
    {
      id: "2",
      type: "button",
      size: "1x1",
      label: "Stop",
      iconName: "Square",
      color: "destructive",
    },
    {
      id: "3",
      type: "button",
      size: "1x1",
      label: "Music",
      iconName: "Music",
      color: "accent",
    },
    {
      id: "4",
      type: "encoder",
      size: "2x2",
      label: "Speed",
      color: "primary",
      defaultValue: 50,
    },
    {
      id: "5",
      type: "button",
      size: "1x1",
      label: "Network",
      iconName: "Wifi",
      color: "primary",
    },
    {
      id: "6",
      type: "fader",
      size: "2x1",
      label: "Transition",
      color: "primary",
      defaultValue: 50,
    },
    {
      id: "7",
      type: "toggle",
      size: "1x1",
      label: "Auto",
      iconName: "Zap",
      color: "accent",
      defaultActive: true,
    },
    {
      id: "8",
      type: "toggle",
      size: "1x1",
      label: "Lock",
      iconName: "Layers",
      color: "muted",
    },
  ],
};

// Icon mapping for default pads
const iconMap: Record<string, LucideIcon> = {
  Mic: Mic,
  MicOff: MicOff,
  Video: Video,
  Monitor: Monitor,
  Volume2: Volume2,
  Play: Play,
  Square: Square,
  Circle: Circle,
  Layers: Layers,
  MessageSquare: MessageSquare,
  Music: Music,
  Headphones: Headphones,
  Radio: Radio,
  Wifi: Wifi,
  Zap: Zap,
  RotateCcw: RotateCcw,
};

interface DeckGridProps {
  activeProfile: string;
  decks: Record<string, PadConfig[]>;
  onUpdatePad: (
    profileId: string,
    padId: string,
    updates: Partial<PadConfig>
  ) => void;
  onUpdatePadState: (
    profileId: string,
    padId: string,
    isActive: boolean
  ) => void;
  onUpdateFaderValue: (profileId: string, padId: string, value: number) => void;
  states: Record<
    string,
    { padStates: Record<string, boolean>; faderValues: Record<string, number> }
  >;
  currentPage?: number;
  websocket?: {
    status: "online" | "offline" | "connecting";
    sendControl: (payload: {
      controlId: string;
      value: number;
      meta?: Record<string, string>;
    }) => Promise<void>;
  };
  selectedProfileId?: string | null;
  cols?: number; // Nombre de colonnes (1-16, défaut: 4)
  enableScroll?: boolean; // Activer le scroll infini au lieu de la pagination
}

export const DeckGrid = ({
  activeProfile,
  decks,
  onUpdatePad,
  onUpdatePadState,
  onUpdateFaderValue,
  states,
  currentPage = 0,
  websocket,
  selectedProfileId,
  cols = 4,
  enableScroll = true,
}: DeckGridProps) => {
  const [editingPad, setEditingPad] = useState<PadConfig | null>(null);
  const [editorOpen, setEditorOpen] = useState(false);

  const allPads =
    decks[activeProfile] || defaultProfileDecks[activeProfile] || [];

  // Si scroll activé, afficher tous les pads, sinon paginer
  const effectiveCols = Math.min(16, Math.max(1, cols)); // Limiter entre 1 et 16
  const controlsPerPage = enableScroll ? allPads.length : effectiveCols * 4; // 4 lignes si pagination
  const startIndex = enableScroll ? 0 : currentPage * controlsPerPage;
  const endIndex = enableScroll ? allPads.length : startIndex + controlsPerPage;
  const pads = allPads.slice(startIndex, endIndex);

  const profileState = states[activeProfile] || {
    padStates: {},
    faderValues: {},
  };

  const handlePadPress = async (pad: PadConfig) => {
    logger.debug("DeckGrid: pad pressed", pad.id, pad.type, pad.action);

    // Envoyer l'action au serveur via WebSocket
    if (websocket?.status === "online" && websocket.sendControl) {
      try {
        await websocket.sendControl({
          controlId: pad.id,
          type: pad.type || "button",
          value:
            pad.type === "toggle"
              ? profileState.padStates[pad.id]
                ? 0
                : 1
              : 1,
          meta: pad.action ? { actionType: pad.action.type } : undefined,
        });
        logger.debug("DeckGrid: control sent successfully", pad.id);
      } catch (err) {
        logger.error("DeckGrid: failed to send control", pad.id, err);
      }
    } else {
      logger.warn(
        "DeckGrid: WebSocket not connected, cannot send control",
        pad.id
      );
    }

    // Mettre à jour l'état local pour les toggles
    if (pad.type === "toggle") {
      const currentState =
        profileState.padStates[pad.id] ?? pad.defaultActive ?? false;
      onUpdatePadState(activeProfile, pad.id, !currentState);
    }
  };

  const handleLongPress = (pad: PadConfig) => {
    setEditingPad(pad);
    setEditorOpen(true);
  };

  const handleFaderChange = (padId: string, value: number) => {
    onUpdateFaderValue(activeProfile, padId, value);
  };

  const handleSavePad = (updates: Partial<PadConfig>) => {
    if (editingPad) {
      onUpdatePad(activeProfile, editingPad.id, updates);
    }
  };

  const getIcon = (pad: PadConfig) => {
    if (pad.iconName) {
      const icon = getIconByName(pad.iconName) || iconMap[pad.iconName];
      if (!icon) {
        logger.warn(`Icon not found for iconName: ${pad.iconName}, pad:`, pad);
      } else {
        logger.debug(`Icon found for pad ${pad.id}: ${pad.iconName}`, icon);
      }
      return icon;
    }
    logger.debug(`No iconName for pad ${pad.id}:`, pad);
    return undefined;
  };

  return (
    <>
      <div
        className={`w-full h-full p-4 pb-20 ${
          enableScroll ? "overflow-y-auto" : "overflow-hidden"
        }`}
      >
        <div
          className="grid auto-rows-fr gap-3 isolate"
          style={{
            gridTemplateColumns: `repeat(${effectiveCols}, minmax(0, 1fr))`,
            gridAutoFlow: "dense",
            minHeight: enableScroll ? "auto" : "100%",
          }}
        >
          {pads.map((pad) => (
            <ControlPad
              key={pad.id}
              id={pad.id}
              type={pad.type}
              size={pad.size}
              label={pad.label}
              icon={getIcon(pad)}
              imageUrl={pad.imageUrl}
              color={pad.color}
              isActive={
                profileState.padStates[pad.id] ?? pad.defaultActive ?? false
              }
              value={profileState.faderValues[pad.id] ?? pad.defaultValue ?? 0}
              onPress={() => handlePadPress(pad)}
              onLongPress={() => handleLongPress(pad)}
              onValueChange={(value) => handleFaderChange(pad.id, value)}
            />
          ))}
        </div>
      </div>

      <PadEditor
        pad={editingPad}
        open={editorOpen}
        onClose={() => {
          setEditorOpen(false);
          setEditingPad(null);
        }}
        onSave={handleSavePad}
      />
    </>
  );
};

export { defaultProfileDecks };
