/**
 * ControlPad - Composant réutilisable pour les pads de contrôle
 * Refactorisé pour responsivité, performance et maintenabilité
 * Support: button, toggle, fader (horizontal/vertical), encoder
 * Responsif sur mobile, tablet et desktop
 */

import { cn } from "@/lib/utils";
import { motion } from "framer-motion";
import { LucideIcon } from "lucide-react";
import { useEffect, useRef, useState } from "react";

type PadType = "button" | "toggle" | "fader" | "encoder";
type PadSize = "1x1" | "2x1" | "1x2" | "2x2";
type PadColor = "primary" | "accent" | "destructive" | "muted";

interface ControlPadProps {
  id: string;
  type: PadType;
  size: PadSize;
  label: string;
  icon?: LucideIcon;
  imageUrl?: string;
  color?: PadColor;
  isActive?: boolean;
  value?: number;
  onPress?: () => void;
  onLongPress?: () => void;
  onValueChange?: (value: number) => void;
}

// ============================================================================
// SIZE CLASSES - Tailles responsives des pads
// ============================================================================
const sizeClasses = {
  "1x1": "col-span-1 row-span-1",
  "2x1": "col-span-2 row-span-1 xs:col-span-1 sm:col-span-2",
  "1x2": "col-span-1 row-span-2 xs:row-span-1 sm:row-span-2",
  "2x2":
    "col-span-2 row-span-2 xs:col-span-1 xs:row-span-1 sm:col-span-2 sm:row-span-2",
} as const;

// ============================================================================
// COLOR UTILITIES
// ============================================================================
const colorColorMap: Record<PadColor, string> = {
  primary: "hsl(var(--primary))",
  accent: "hsl(var(--primary))", // Same as primary for consistency
  destructive: "hsl(var(--destructive))",
  muted: "hsl(var(--muted-foreground))",
};

const colorBackgroundMap: Record<PadColor, string> = {
  primary: "bg-primary/20 border-primary/60 text-primary",
  accent: "bg-primary/20 border-primary/60 text-primary",
  destructive: "bg-destructive/20 border-destructive/60 text-destructive",
  muted: "bg-muted border-muted-foreground/30 text-muted-foreground",
};

const getColorClasses = (color: PadColor, isActive: boolean, type: PadType) => {
  if (!isActive && type !== "fader" && type !== "encoder") {
    return "bg-secondary border-border/60 text-foreground";
  }
  return colorBackgroundMap[color];
};

const getHoverColor = (color: PadColor) => {
  const hoverMap: Record<PadColor, { border: string; glow: string }> = {
    primary: {
      border: "hsla(188, 95%, 52%, 0.6)",
      glow: "0 0 20px hsla(188, 95%, 52%, 0.15)",
    },
    accent: {
      border: "hsla(188, 95%, 52%, 0.6)",
      glow: "0 0 20px hsla(188, 95%, 52%, 0.15)",
    },
    destructive: {
      border: "hsla(15, 90%, 58%, 0.6)",
      glow: "0 0 20px hsla(15, 90%, 58%, 0.15)",
    },
    muted: {
      border: "hsla(210, 12%, 60%, 0.6)",
      glow: "0 0 20px hsla(210, 12%, 60%, 0.1)",
    },
  };
  return hoverMap[color];
};

// ============================================================================
// MAIN COMPONENT
// ============================================================================
export const ControlPad = ({
  id,
  type,
  size,
  label,
  icon: Icon,
  imageUrl,
  color = "primary",
  isActive = false,
  value = 0,
  onPress,
  onLongPress,
  onValueChange,
}: ControlPadProps) => {
  // State Management
  const [isPressed, setIsPressed] = useState(false);
  const [longPressTimer, setLongPressTimer] = useState<ReturnType<
    typeof setTimeout
  > | null>(null);
  const [longPressTriggered, setLongPressTriggered] = useState(false);
  const [currentValue, setCurrentValue] = useState(value);
  const [isDragging, setIsDragging] = useState(false);
  const [isHorizontal, setIsHorizontal] = useState(false);
  const [containerWidth, setContainerWidth] = useState(0);

  // Refs
  const containerRef = useRef<HTMLDivElement | null>(null);
  const faderTrackRef = useRef<HTMLDivElement | null>(null);
  const encoderRef = useRef<HTMLDivElement | null>(null);
  const lastAngleRef = useRef<number | null>(null);

  // ========================================================================
  // EFFECTS
  // ========================================================================

  // Synchronize value from props
  useEffect(() => {
    setCurrentValue(value);
  }, [value]);

  // Detect fader orientation based on rendered dimensions
  useEffect(() => {
    if (type !== "fader" || !containerRef.current) return;

    const observer = new ResizeObserver((entries) => {
      for (const entry of entries) {
        const { width, height } = entry.contentRect;
        setIsHorizontal(width > height * 1.2);
        setContainerWidth(width);
      }
    });

    observer.observe(containerRef.current);

    // Initialize immediately
    if (containerRef.current) {
      const rect = containerRef.current.getBoundingClientRect();
      setContainerWidth(rect.width);
      setIsHorizontal(rect.width > rect.height * 1.2);
    }

    return () => observer.disconnect();
  }, [type]);

  // ========================================================================
  // BUTTON / TOGGLE HANDLERS
  // ========================================================================

  const handlePressStart = () => {
    if (type === "fader" || type === "encoder") return;
    setIsPressed(true);
    setLongPressTriggered(false);
    const timer = setTimeout(() => {
      setLongPressTriggered(true);
      onLongPress?.();
    }, 500);
    setLongPressTimer(timer);
  };

  const handlePressEnd = () => {
    if (type === "fader" || type === "encoder") return;
    setIsPressed(false);
    if (longPressTimer) {
      clearTimeout(longPressTimer);
      setLongPressTimer(null);
      if (!longPressTriggered) {
        onPress?.();
      }
    }
  };

  // ========================================================================
  // FADER HANDLERS
  // ========================================================================

  const updateFaderValue = (clientX: number, clientY: number) => {
    if (!faderTrackRef.current) return;
    const rect = faderTrackRef.current.getBoundingClientRect();

    let relative: number;
    if (isHorizontal) {
      relative = (clientX - rect.left) / rect.width;
    } else {
      relative = (rect.bottom - clientY) / rect.height;
    }

    const clamped = Math.min(1, Math.max(0, relative));
    const percent = Math.round(clamped * 100);
    setCurrentValue(percent);
    onValueChange?.(percent);
  };

  const handleFaderPointerDown: React.PointerEventHandler<HTMLDivElement> = (
    event
  ) => {
    event.preventDefault();
    event.stopPropagation();
    setIsDragging(true);
    (event.currentTarget as HTMLDivElement).setPointerCapture(event.pointerId);
    updateFaderValue(event.clientX, event.clientY);
  };

  const handleFaderTouchStart: React.TouchEventHandler<HTMLDivElement> = (
    event
  ) => {
    event.preventDefault();
    event.stopPropagation();
    setIsDragging(true);
    const touch = event.touches[0];
    updateFaderValue(touch.clientX, touch.clientY);
  };

  const handleFaderTouchMove: React.TouchEventHandler<HTMLDivElement> = (
    event
  ) => {
    if (!isDragging) return;
    event.preventDefault();
    event.stopPropagation();
    const touch = event.touches[0];
    updateFaderValue(touch.clientX, touch.clientY);
  };

  const handleFaderTouchEnd: React.TouchEventHandler<HTMLDivElement> = (
    event
  ) => {
    if (!isDragging) return;
    event.preventDefault();
    event.stopPropagation();
    setIsDragging(false);
  };

  const handleFaderPointerMove: React.PointerEventHandler<HTMLDivElement> = (
    event
  ) => {
    if (!isDragging) return;
    updateFaderValue(event.clientX, event.clientY);
  };

  const handleFaderPointerUp: React.PointerEventHandler<HTMLDivElement> = (
    event
  ) => {
    if (!isDragging) return;
    setIsDragging(false);
    try {
      (event.currentTarget as HTMLDivElement).releasePointerCapture(
        event.pointerId
      );
    } catch {
      // ignore
    }
  };

  // Encoder rotation logic
  const updateEncoderValue = (clientX: number, clientY: number) => {
    if (!encoderRef.current) return;
    const rect = encoderRef.current.getBoundingClientRect();
    const centerX = rect.left + rect.width / 2;
    const centerY = rect.top + rect.height / 2;

    const angle =
      Math.atan2(clientY - centerY, clientX - centerX) * (180 / Math.PI);

    if (lastAngleRef.current !== null) {
      let delta = angle - lastAngleRef.current;
      if (delta > 180) delta -= 360;
      if (delta < -180) delta += 360;

      const sensitivity = 0.5;
      const newValue = Math.min(
        100,
        Math.max(0, currentValue + delta * sensitivity)
      );
      setCurrentValue(Math.round(newValue));
      onValueChange?.(Math.round(newValue));
    }
    lastAngleRef.current = angle;
  };

  const handleEncoderPointerDown: React.PointerEventHandler<HTMLDivElement> = (
    event
  ) => {
    event.preventDefault();
    event.stopPropagation();
    setIsDragging(true);
    lastAngleRef.current = null;
    (event.currentTarget as HTMLDivElement).setPointerCapture(event.pointerId);
  };

  const handleEncoderPointerMove: React.PointerEventHandler<HTMLDivElement> = (
    event
  ) => {
    if (!isDragging) return;
    updateEncoderValue(event.clientX, event.clientY);
  };

  const handleEncoderPointerUp: React.PointerEventHandler<HTMLDivElement> = (
    event
  ) => {
    if (!isDragging) return;
    setIsDragging(false);
    lastAngleRef.current = null;
    try {
      (event.currentTarget as HTMLDivElement).releasePointerCapture(
        event.pointerId
      );
    } catch {
      // ignore
    }
  };

  const handleEncoderTouchStart: React.TouchEventHandler<HTMLDivElement> = (
    event
  ) => {
    event.preventDefault();
    event.stopPropagation();
    setIsDragging(true);
    lastAngleRef.current = null;
    const touch = event.touches[0];
    updateEncoderValue(touch.clientX, touch.clientY);
  };

  const handleEncoderTouchMove: React.TouchEventHandler<HTMLDivElement> = (
    event
  ) => {
    if (!isDragging) return;
    event.preventDefault();
    event.stopPropagation();
    const touch = event.touches[0];
    updateEncoderValue(touch.clientX, touch.clientY);
  };

  const handleEncoderTouchEnd: React.TouchEventHandler<HTMLDivElement> = (
    event
  ) => {
    if (!isDragging) return;
    event.preventDefault();
    event.stopPropagation();
    setIsDragging(false);
    lastAngleRef.current = null;
  };

  const getColorClasses = () => {
    if (!isActive && type !== "fader" && type !== "encoder") {
      return "bg-secondary border-border/60 text-foreground";
    }

    switch (color) {
      case "destructive":
        return "bg-destructive/20 border-destructive/60 text-destructive";
      case "accent":
        return "bg-primary/20 border-primary/60 text-primary";
      case "muted":
        return "bg-muted border-muted-foreground/30 text-muted-foreground";
      default:
        return "bg-primary/20 border-primary/60 text-primary";
    }
  };

  const getColorHsl = () => {
    switch (color) {
      case "destructive":
        return "hsl(var(--destructive))";
      case "muted":
        return "hsl(var(--muted-foreground))";
      default:
        return "hsl(var(--primary))";
    }
  };

  const getHoverColor = () => {
    switch (color) {
      case "destructive":
        return {
          border: "hsla(15, 90%, 58%, 0.6)",
          glow: "0 0 20px hsla(15, 90%, 58%, 0.15)",
        };
      case "muted":
        return {
          border: "hsla(210, 12%, 60%, 0.6)",
          glow: "0 0 20px hsla(210, 12%, 60%, 0.1)",
        };
      default:
        // primary / accent
        return {
          border: "hsla(188, 95%, 52%, 0.6)",
          glow: "0 0 20px hsla(188, 95%, 52%, 0.15)",
        };
    }
  };

  const renderEncoderContent = () => {
    const strokeColor = getColorHsl();
    const radius = 36;
    const circumference = 2 * Math.PI * radius;
    const strokeDashoffset =
      circumference - (currentValue / 100) * circumference * 0.75;

    return (
      <div className="w-full h-full flex flex-col items-center justify-center p-2 xs:p-2 sm:p-3 gap-1">
        <span className="text-[10px] xs:text-xs text-mono font-medium uppercase tracking-wide text-muted-foreground truncate">
          {label}
        </span>
        <div
          ref={encoderRef}
          className="relative touch-none cursor-grab active:cursor-grabbing flex-1 flex items-center justify-center min-h-0"
          onPointerDown={handleEncoderPointerDown}
          onPointerMove={handleEncoderPointerMove}
          onPointerUp={handleEncoderPointerUp}
          onPointerCancel={handleEncoderPointerUp}
          onTouchStart={handleEncoderTouchStart}
          onTouchMove={handleEncoderTouchMove}
          onTouchEnd={handleEncoderTouchEnd}
          onTouchCancel={handleEncoderTouchEnd}
        >
          <svg
            width="70"
            height="70"
            viewBox="0 0 90 90"
            className="transform -rotate-135 xs:w-20 xs:h-20 sm:w-24 sm:h-24 w-16 h-16"
            style={{ flexShrink: 0 }}
          >
            {/* Background track */}
            <circle
              cx="45"
              cy="45"
              r={radius}
              fill="none"
              stroke="hsl(var(--muted))"
              strokeWidth="6"
              strokeLinecap="round"
              strokeDasharray={`${circumference * 0.75} ${
                circumference * 0.25
              }`}
            />
            {/* Value arc */}
            <motion.circle
              cx="45"
              cy="45"
              r={radius}
              fill="none"
              stroke={strokeColor}
              strokeWidth="6"
              strokeLinecap="round"
              strokeDasharray={`${circumference * 0.75} ${
                circumference * 0.25
              }`}
              initial={{ strokeDashoffset: circumference }}
              animate={{ strokeDashoffset }}
              transition={{ type: "spring", stiffness: 300, damping: 30 }}
              style={{ filter: `drop-shadow(0 0 6px ${strokeColor})` }}
            />
          </svg>
          {/* Center knob */}
          <div className="absolute inset-0 flex items-center justify-center">
            <motion.div
              className="rounded-full bg-secondary border-2 border-border flex items-center justify-center w-10 h-10 xs:w-12 xs:h-12 sm:w-14 sm:h-14"
              animate={{
                rotate: (currentValue / 100) * 270 - 135,
                scale: isDragging ? 1.1 : 1,
              }}
              transition={{ type: "spring", stiffness: 300, damping: 25 }}
            >
              <div className="w-1 h-3 xs:h-4 bg-primary rounded-full -translate-y-1" />
            </motion.div>
          </div>
        </div>
        <span
          className="text-xs xs:text-sm text-mono font-bold leading-none"
          style={{ color: strokeColor }}
        >
          {currentValue}%
        </span>
      </div>
    );
  };

  const renderFaderContent = () => {
    // Fader simplifié : toute la zone est cliquable, pas de thumb
    // La barre de progression remplit visuellement la case

    if (isHorizontal) {
      return (
        <div
          ref={faderTrackRef}
          className="w-full h-full relative touch-none cursor-pointer overflow-hidden"
          onPointerDown={handleFaderPointerDown}
          onPointerMove={handleFaderPointerMove}
          onPointerUp={handleFaderPointerUp}
          onPointerCancel={handleFaderPointerUp}
          onTouchStart={handleFaderTouchStart}
          onTouchMove={handleFaderTouchMove}
          onTouchEnd={handleFaderTouchEnd}
          onTouchCancel={handleFaderTouchEnd}
        >
          {/* Barre de progression qui remplit la case */}
          <motion.div
            className="absolute top-0 left-0 bottom-0 bg-gradient-to-r from-primary/40 to-primary"
            initial={{ width: 0 }}
            animate={{ width: `${currentValue}%` }}
            transition={{ type: "spring", stiffness: 300, damping: 30 }}
          />
          {/* Label et valeur superposés */}
          <div className="absolute inset-0 flex items-center justify-between px-3 pointer-events-none">
            <span className="text-xs text-mono font-medium uppercase tracking-wide text-foreground drop-shadow-md truncate">
              {label}
            </span>
            <span className="text-sm text-mono font-bold text-foreground drop-shadow-md">
              {currentValue}%
            </span>
          </div>
          {/* Ligne indicatrice de position */}
          <motion.div
            className="absolute top-0 bottom-0 w-0.5 bg-foreground/80 shadow-lg"
            style={{ left: `${currentValue}%` }}
            animate={{ opacity: isDragging ? 1 : 0.6 }}
          />
        </div>
      );
    }

    // Vertical fader - remplit toute la case
    return (
      <div
        ref={faderTrackRef}
        className="w-full h-full relative touch-none cursor-pointer overflow-hidden"
        onPointerDown={handleFaderPointerDown}
        onPointerMove={handleFaderPointerMove}
        onPointerUp={handleFaderPointerUp}
        onPointerCancel={handleFaderPointerUp}
        onTouchStart={handleFaderTouchStart}
        onTouchMove={handleFaderTouchMove}
        onTouchEnd={handleFaderTouchEnd}
        onTouchCancel={handleFaderTouchEnd}
      >
        {/* Barre de progression qui remplit depuis le bas */}
        <motion.div
          className="absolute bottom-0 left-0 right-0 bg-gradient-to-t from-primary/40 to-primary"
          initial={{ height: 0 }}
          animate={{ height: `${currentValue}%` }}
          transition={{ type: "spring", stiffness: 300, damping: 30 }}
        />
        {/* Label en haut */}
        <div className="absolute top-2 left-0 right-0 flex justify-center pointer-events-none">
          <span className="text-[10px] xs:text-xs text-mono font-medium uppercase tracking-wide text-foreground drop-shadow-md truncate px-2">
            {label}
          </span>
        </div>
        {/* Valeur en bas */}
        <div className="absolute bottom-2 left-0 right-0 flex justify-center pointer-events-none">
          <span className="text-sm text-mono font-bold text-foreground drop-shadow-md">
            {currentValue}%
          </span>
        </div>
        {/* Ligne indicatrice de position */}
        <motion.div
          className="absolute left-0 right-0 h-0.5 bg-foreground/80 shadow-lg"
          style={{ bottom: `${currentValue}%` }}
          animate={{ opacity: isDragging ? 1 : 0.6 }}
        />
      </div>
    );
  };

  const renderPadContent = () => {
    if (type === "encoder") return renderEncoderContent();
    if (type === "fader") return renderFaderContent();

    return (
      <div className="w-full h-full flex flex-col items-center justify-center gap-1 p-2 xs:p-2 sm:p-3">
        {imageUrl ? (
          <img
            src={imageUrl}
            alt={label}
            className={cn(
              "w-full h-full object-contain flex-1 min-h-0",
              !isActive && "opacity-70"
            )}
            onError={(e) => {
              const target = e.target as HTMLImageElement;
              target.style.display = "none";
            }}
          />
        ) : null}
        {!imageUrl && Icon ? (
          <div className="flex items-center justify-center flex-1 min-h-0">
            <Icon
              className={cn(
                "w-5 h-5 xs:w-6 xs:h-6 sm:w-7 sm:h-7 flex-shrink-0",
                !isActive && "opacity-70"
              )}
              strokeWidth={1.5}
            />
          </div>
        ) : null}
        <span className="text-[9px] xs:text-[10px] sm:text-xs text-mono font-medium uppercase tracking-wider text-center leading-tight line-clamp-1 px-1">
          {label}
        </span>
        {type === "toggle" && (
          <div
            className={cn(
              "w-1.5 h-1.5 xs:w-2 xs:h-2 rounded-full transition-all",
              isActive
                ? "bg-primary shadow-[0_0_8px_hsl(var(--primary))]"
                : "bg-muted-foreground/30"
            )}
          />
        )}
      </div>
    );
  };

  return (
    <motion.div
      ref={containerRef}
      className={cn("relative min-h-0", sizeClasses[size])}
      initial={{ opacity: 0, scale: 0.95 }}
      animate={{ opacity: 1, scale: 1 }}
      transition={{ duration: 0.2 }}
    >
      <motion.button
        onMouseDown={handlePressStart}
        onMouseUp={handlePressEnd}
        onMouseLeave={handlePressEnd}
        onTouchStart={(e) => {
          if (type === "fader" || type === "encoder") {
            e.stopPropagation();
          } else {
            handlePressStart();
          }
        }}
        onTouchEnd={(e) => {
          if (type === "fader" || type === "encoder") {
            e.stopPropagation();
          } else {
            handlePressEnd();
          }
        }}
        className={cn(
          "w-full h-full rounded-lg xs:rounded-xl border-2 relative overflow-hidden flex",
          "backdrop-blur-sm transition-colors duration-150",
          type === "fader" || type === "encoder"
            ? "bg-secondary border-border/60 text-foreground"
            : getColorClasses(),
          isPressed && "brightness-125"
        )}
        whileHover={{
          borderColor: getHoverColor().border,
          boxShadow: getHoverColor().glow,
        }}
        whileTap={
          type !== "fader" && type !== "encoder" ? { scale: 0.96 } : undefined
        }
        transition={{ duration: 0.15 }}
      >
        {isActive && type !== "fader" && type !== "encoder" && (
          <motion.div
            className="absolute inset-0 bg-gradient-to-br from-primary/10 via-transparent to-transparent pointer-events-none"
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            transition={{ duration: 0.2 }}
          />
        )}
        {renderPadContent()}
      </motion.button>
    </motion.div>
  );
};

export default ControlPad;
