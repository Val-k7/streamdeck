import { useState, useEffect } from "react";
import { motion, AnimatePresence } from "framer-motion";
import { X, Check } from "lucide-react";
import { PadConfig } from "@/hooks/useDeckStorage";
import {
  Mic, Video, Monitor, Volume2, Play, Square, Circle,
  Layers, MessageSquare, Music, Headphones, Radio, Wifi,
  Zap, Settings, Camera, Power, SkipForward, SkipBack,
  Pause, RefreshCw, RotateCcw
} from "lucide-react";

const AVAILABLE_ICONS = [
  { name: "Mic", icon: Mic },
  { name: "Video", icon: Video },
  { name: "Monitor", icon: Monitor },
  { name: "Volume2", icon: Volume2 },
  { name: "Play", icon: Play },
  { name: "Square", icon: Square },
  { name: "Circle", icon: Circle },
  { name: "Layers", icon: Layers },
  { name: "MessageSquare", icon: MessageSquare },
  { name: "Music", icon: Music },
  { name: "Headphones", icon: Headphones },
  { name: "Radio", icon: Radio },
  { name: "Wifi", icon: Wifi },
  { name: "Zap", icon: Zap },
  { name: "Settings", icon: Settings },
  { name: "Camera", icon: Camera },
  { name: "Power", icon: Power },
  { name: "SkipForward", icon: SkipForward },
  { name: "SkipBack", icon: SkipBack },
  { name: "Pause", icon: Pause },
  { name: "RefreshCw", icon: RefreshCw },
  { name: "RotateCcw", icon: RotateCcw },
];

const COLORS = ["primary", "accent", "destructive", "muted"] as const;
const SIZES = ["1x1", "2x1", "1x2", "2x2"] as const;
const TYPES = ["button", "toggle", "fader", "encoder"] as const;

interface PadEditorProps {
  pad: PadConfig | null;
  open: boolean;
  onClose: () => void;
  onSave: (updates: Partial<PadConfig>) => void;
}

export const PadEditor = ({ pad, open, onClose, onSave }: PadEditorProps) => {
  const [label, setLabel] = useState("");
  const [type, setType] = useState<PadConfig["type"]>("button");
  const [size, setSize] = useState<PadConfig["size"]>("1x1");
  const [color, setColor] = useState<PadConfig["color"]>("primary");
  const [iconName, setIconName] = useState<string | undefined>();
  const [imageUrl, setImageUrl] = useState<string | undefined>();

  useEffect(() => {
    if (pad) {
      setLabel(pad.label);
      setType(pad.type);
      setSize(pad.size);
      setColor(pad.color || "primary");
      setIconName(pad.iconName);
      setImageUrl(pad.imageUrl);
    }
  }, [pad]);

  const handleSave = () => {
    onSave({ label, type, size, color, iconName, imageUrl });
    onClose();
  };

  return (
    <AnimatePresence>
      {open && (
        <motion.div
          className="fixed inset-0 z-50 flex items-end justify-center"
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          exit={{ opacity: 0 }}
        >
          <motion.div
            className="absolute inset-0 bg-background/80 backdrop-blur-sm"
            onClick={onClose}
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
          />
          <motion.div
            className="relative w-full max-w-lg bg-card border border-border rounded-t-2xl p-6 max-h-[80vh] overflow-y-auto"
            initial={{ y: "100%" }}
            animate={{ y: 0 }}
            exit={{ y: "100%" }}
            transition={{ type: "spring", damping: 25, stiffness: 300 }}
          >
            <div className="flex items-center justify-between mb-6">
              <h2 className="text-lg font-semibold text-mono uppercase tracking-wide">Edit Pad</h2>
              <div className="flex gap-2">
                <button
                  onClick={onClose}
                  className="p-2 rounded-lg bg-muted hover:bg-muted/80 transition-colors"
                >
                  <X className="w-5 h-5" />
                </button>
                <button
                  onClick={handleSave}
                  className="p-2 rounded-lg bg-primary text-primary-foreground hover:bg-primary/90 transition-colors"
                >
                  <Check className="w-5 h-5" />
                </button>
              </div>
            </div>

            {/* Label */}
            <div className="mb-5">
              <label className="block text-xs text-mono uppercase tracking-wide text-muted-foreground mb-2">
                Label
              </label>
              <input
                type="text"
                value={label}
                onChange={(e) => setLabel(e.target.value)}
                className="w-full px-4 py-3 bg-muted border border-border rounded-lg text-foreground text-mono focus:outline-none focus:ring-2 focus:ring-primary"
                maxLength={12}
              />
            </div>

            {/* Type */}
            <div className="mb-5">
              <label className="block text-xs text-mono uppercase tracking-wide text-muted-foreground mb-2">
                Type
              </label>
              <div className="grid grid-cols-4 gap-2">
                {TYPES.map((t) => (
                  <button
                    key={t}
                    onClick={() => setType(t)}
                    className={`px-3 py-2 rounded-lg text-xs text-mono uppercase tracking-wide transition-colors ${
                      type === t
                        ? "bg-primary text-primary-foreground"
                        : "bg-muted text-muted-foreground hover:bg-muted/80"
                    }`}
                  >
                    {t}
                  </button>
                ))}
              </div>
            </div>

            {/* Size */}
            <div className="mb-5">
              <label className="block text-xs text-mono uppercase tracking-wide text-muted-foreground mb-2">
                Size
              </label>
              <div className="grid grid-cols-4 gap-2">
                {SIZES.map((s) => (
                  <button
                    key={s}
                    onClick={() => setSize(s)}
                    className={`px-3 py-2 rounded-lg text-xs text-mono uppercase tracking-wide transition-colors ${
                      size === s
                        ? "bg-primary text-primary-foreground"
                        : "bg-muted text-muted-foreground hover:bg-muted/80"
                    }`}
                  >
                    {s}
                  </button>
                ))}
              </div>
            </div>

            {/* Color */}
            <div className="mb-5">
              <label className="block text-xs text-mono uppercase tracking-wide text-muted-foreground mb-2">
                Color
              </label>
              <div className="grid grid-cols-4 gap-2">
                {COLORS.map((c) => (
                  <button
                    key={c}
                    onClick={() => setColor(c)}
                    className={`px-3 py-2 rounded-lg text-xs text-mono uppercase tracking-wide transition-colors border-2 ${
                      color === c
                        ? c === "primary"
                          ? "bg-primary/20 border-primary text-primary"
                          : c === "accent"
                          ? "bg-primary/20 border-primary text-primary"
                          : c === "destructive"
                          ? "bg-destructive/20 border-destructive text-destructive"
                          : "bg-muted border-muted-foreground/30 text-muted-foreground"
                        : "bg-muted border-transparent text-muted-foreground hover:bg-muted/80"
                    }`}
                  >
                    {c}
                  </button>
                ))}
              </div>
            </div>

            {/* Icon */}
            {(type === "button" || type === "toggle") && (
              <div className="mb-5">
                <label className="block text-xs text-mono uppercase tracking-wide text-muted-foreground mb-2">
                  Icon
                </label>
                <div className="grid grid-cols-6 gap-2 max-h-40 overflow-y-auto p-1 mb-3">
                  {AVAILABLE_ICONS.map(({ name, icon: IconComponent }) => (
                    <button
                      key={name}
                      onClick={() => {
                        setIconName(name);
                        setImageUrl(undefined);
                      }}
                      className={`p-3 rounded-lg transition-colors flex items-center justify-center ${
                        iconName === name && !imageUrl
                          ? "bg-primary text-primary-foreground"
                          : "bg-muted text-muted-foreground hover:bg-muted/80"
                      }`}
                    >
                      <IconComponent className="w-5 h-5" />
                    </button>
                  ))}
                </div>
                <label className="block text-xs text-mono uppercase tracking-wide text-muted-foreground mb-2">
                  Image URL (alternative to icon)
                </label>
                <input
                  type="text"
                  value={imageUrl || ""}
                  onChange={(e) => {
                    setImageUrl(e.target.value || undefined);
                    if (e.target.value) setIconName(undefined);
                  }}
                  placeholder="https://example.com/image.png"
                  className="w-full px-4 py-3 bg-muted border border-border rounded-lg text-foreground text-mono text-sm focus:outline-none focus:ring-2 focus:ring-primary"
                />
                {imageUrl && (
                  <div className="mt-2 p-2 bg-muted rounded-lg">
                    <img
                      src={imageUrl}
                      alt="Preview"
                      className="w-full h-20 object-contain rounded"
                      onError={(e) => {
                        const target = e.target as HTMLImageElement;
                        target.style.display = 'none';
                        const parent = target.parentElement;
                        if (parent) {
                          parent.innerHTML = '<p class="text-xs text-destructive">Failed to load image</p>';
                        }
                      }}
                    />
                  </div>
                )}
              </div>
            )}
          </motion.div>
        </motion.div>
      )}
    </AnimatePresence>
  );
};

// Helper to get icon component from name
export const getIconByName = (name?: string) => {
  if (!name) return undefined;
  const found = AVAILABLE_ICONS.find((i) => i.name === name);
  return found?.icon;
};
