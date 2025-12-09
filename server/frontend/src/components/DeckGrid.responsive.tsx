/**
 * DeckGrid - Grille responsive de pads de contrôle
 * Refactorisé pour s'adapter fluellement à tous les écrans
 * Utilise le système de design tokens pour cohérence
 *
 * Responsive design:
 * xs (mobile): 2 colonnes
 * sm (mobile landscape): 3 colonnes
 * md (tablet): 4 colonnes
 * lg (desktop): 6 colonnes
 * xl (large desktop): 8 colonnes
 */

import { PadConfig } from "@/hooks/useDeckStorage";
import { logger } from "@/lib/logger";
import { cn } from "@/lib/utils";
import { useState } from "react";
import { ControlPad } from "./ControlPad";
import { PadEditor, getIconByName } from "./PadEditor";

// ============================================================================
// DEFAULT PROFILES
// ============================================================================

export const defaultProfileDecks: Record<string, PadConfig[]> = {
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

  music: [
    {
      id: "1",
      type: "fader",
      size: "1x2",
      label: "Master",
      color: "primary",
      defaultValue: 75,
    },
    {
      id: "2",
      type: "fader",
      size: "1x2",
      label: "Bass",
      color: "accent",
      defaultValue: 50,
    },
    {
      id: "3",
      type: "fader",
      size: "1x2",
      label: "Mid",
      color: "accent",
      defaultValue: 50,
    },
    {
      id: "4",
      type: "fader",
      size: "1x2",
      label: "Treble",
      color: "accent",
      defaultValue: 50,
    },
    {
      id: "5",
      type: "encoder",
      size: "2x2",
      label: "Tempo",
      color: "primary",
      defaultValue: 120,
    },
  ],

  gaming: [
    {
      id: "1",
      type: "button",
      size: "2x1",
      label: "Start Stream",
      iconName: "Play",
      color: "primary",
    },
    {
      id: "2",
      type: "toggle",
      size: "1x1",
      label: "Mic",
      iconName: "Mic",
      color: "destructive",
    },
    {
      id: "3",
      type: "toggle",
      size: "1x1",
      label: "Cam",
      iconName: "Video",
      color: "accent",
    },
    {
      id: "4",
      type: "fader",
      size: "2x1",
      label: "Game Volume",
      color: "primary",
      defaultValue: 75,
    },
    {
      id: "5",
      type: "fader",
      size: "2x1",
      label: "Chat Volume",
      color: "primary",
      defaultValue: 60,
    },
  ],
};

// ============================================================================
// RESPONSIVE GRID LAYOUT
// ============================================================================

// Type pour les états des pads (toggle: boolean, fader: number)
type PadState = boolean | number | undefined;

interface DeckGridProps {
  profileId: string;
  pads: PadConfig[];
  states: Record<string, PadState>;
  onPadPress: (padId: string) => void;
  onPadEdit: (padId: string) => void;
  onPadValueChange: (padId: string, value: number) => void;
  editorPadId?: string;
  onEditorClose: () => void;
}

export const DeckGrid = ({
  profileId,
  pads,
  states,
  onPadPress,
  onPadEdit,
  onPadValueChange,
  editorPadId,
  onEditorClose,
}: DeckGridProps) => {
  const [contextMenuPadId, setContextMenuPadId] = useState<string | null>(null);

  // ========================================================================
  // RESPONSIVE GRID CLASSES
  // ========================================================================
  const gridClasses = cn(
    "grid w-full h-full",
    // Nombre de colonnes responsive
    "grid-cols-2", // xs: 2 colonnes (mobile)
    "xs:grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-6 xl:grid-cols-8",
    // Auto-rows avec taille adaptative
    "auto-rows-[60px]", // xs: 60px
    "xs:auto-rows-[60px] sm:auto-rows-[80px] md:auto-rows-[100px] lg:auto-rows-[120px] xl:auto-rows-[150px]",
    // Gap responsive
    "gap-1 xs:gap-1 sm:gap-2 md:gap-3 lg:gap-4 xl:gap-4",
    // Padding
    "p-2 xs:p-2 sm:p-3 md:p-4 lg:p-6"
  );

  // ========================================================================
  // RENDER
  // ========================================================================

  return (
    <div className={gridClasses}>
      {pads.map((pad) => {
        const padState = states[pad.id] || {};
        const Icon = pad.iconName ? getIconByName(pad.iconName) : undefined;

        return (
          <ControlPad
            key={pad.id}
            id={pad.id}
            type={pad.type}
            size={pad.size}
            label={pad.label}
            icon={Icon}
            imageUrl={pad.imageUrl}
            color={pad.color}
            isActive={padState.isActive ?? pad.defaultActive ?? false}
            value={padState.value ?? pad.defaultValue ?? 0}
            onPress={() => {
              if (pad.type === "button") {
                onPadPress(pad.id);
              }
            }}
            onLongPress={() => {
              logger.info(`Long press on pad ${pad.id}`);
              onPadEdit(pad.id);
            }}
            onValueChange={(value) => {
              if (pad.type === "fader" || pad.type === "encoder") {
                onPadValueChange(pad.id, value);
              }
            }}
          />
        );
      })}

      {editorPadId && (
        <PadEditor
          open={true}
          pad={pads.find((p) => p.id === editorPadId)!}
          onClose={onEditorClose}
          onSave={(updatedPad) => {
            // Handle pad save
          }}
        />
      )}
    </div>
  );
};

export default DeckGrid;
