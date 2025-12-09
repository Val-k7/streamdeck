/**
 * Design System Tokens
 * Source of truth pour tous les tokens de design (couleurs, espacement, typographie, etc.)
 * Utilisés par tailwind.config.ts et CSS variables
 */

// ============================================================================
// COLORS - Palette de base avec variations
// ============================================================================
export const colors = {
  // Neutres
  neutral: {
    50: "220 18% 98%",
    100: "220 16% 96%",
    200: "220 14% 88%",
    300: "220 12% 80%",
    400: "220 12% 65%",
    500: "220 12% 55%",
    600: "220 14% 40%",
    700: "220 14% 30%",
    800: "220 14% 22%",
    900: "220 18% 8%", // background dark
    950: "220 18% 6%", // background darker
  },

  // Primary - Cyan/Teal (accent principal)
  primary: {
    50: "188 100% 95%",
    100: "188 100% 90%",
    200: "188 98% 80%",
    300: "188 97% 70%",
    400: "188 96% 60%",
    500: "188 95% 52%", // primary active
    600: "188 90% 45%",
    700: "188 85% 38%",
    800: "188 80% 30%",
    900: "188 75% 20%",
  },

  // Secondary - Variante subtile
  secondary: {
    50: "220 20% 95%",
    100: "220 16% 92%",
    200: "220 14% 85%",
    300: "220 14% 75%",
    400: "220 14% 60%",
    500: "220 14% 50%",
    600: "220 14% 40%",
    700: "220 14% 30%",
    800: "220 14% 18%", // secondary main
    900: "220 14% 12%",
  },

  // Accent - Orange/Amber (secondary action)
  accent: {
    50: "45 100% 96%",
    100: "45 97% 92%",
    200: "45 97% 84%",
    300: "45 96% 74%",
    400: "45 95% 64%",
    500: "45 94% 55%",
    600: "45 93% 45%",
    700: "45 92% 35%",
    800: "45 90% 25%",
    900: "45 88% 15%",
  },

  // Destructive - Red (danger/warning)
  destructive: {
    50: "15 100% 95%",
    100: "15 96% 92%",
    200: "15 94% 84%",
    300: "15 92% 74%",
    400: "15 91% 64%",
    500: "15 90% 58%", // destructive active
    600: "15 89% 48%",
    700: "15 88% 38%",
    800: "15 87% 28%",
    900: "15 85% 18%",
  },

  // Success - Green
  success: {
    50: "142 100% 96%",
    100: "142 96% 92%",
    200: "142 94% 84%",
    300: "142 92% 72%",
    400: "142 84% 60%",
    500: "142 76% 48%", // success active
    600: "142 71% 38%",
    700: "142 65% 30%",
    800: "142 60% 22%",
    900: "142 55% 14%",
  },

  // Warning - Yellow/Orange
  warning: {
    50: "38 100% 96%",
    100: "38 97% 92%",
    200: "38 97% 84%",
    300: "38 96% 72%",
    400: "38 95% 60%",
    500: "38 94% 52%",
    600: "38 93% 42%",
    700: "38 92% 32%",
    800: "38 91% 22%",
    900: "38 89% 12%",
  },

  // Info - Blue
  info: {
    50: "210 100% 97%",
    100: "210 96% 94%",
    200: "210 94% 88%",
    300: "210 92% 80%",
    400: "210 90% 68%",
    500: "210 88% 56%",
    600: "210 86% 44%",
    700: "210 84% 32%",
    800: "210 82% 20%",
    900: "210 80% 10%",
  },
} as const;

// ============================================================================
// SPACING - Système d'espacement cohérent (basé sur 4px)
// ============================================================================
export const spacing = {
  xs: "0.25rem", // 4px
  sm: "0.5rem", // 8px
  md: "1rem", // 16px
  lg: "1.5rem", // 24px
  xl: "2rem", // 32px
  "2xl": "2.5rem", // 40px
  "3xl": "3rem", // 48px
  "4xl": "4rem", // 64px
  "5xl": "5rem", // 80px
} as const;

// ============================================================================
// TYPOGRAPHY - Système typographique cohérent
// ============================================================================
export const typography = {
  // Tailles de texte (rem -> px)
  sizes: {
    xs: { size: "0.75rem", lineHeight: "1rem" }, // 12px
    sm: { size: "0.875rem", lineHeight: "1.25rem" }, // 14px
    base: { size: "1rem", lineHeight: "1.5rem" }, // 16px
    lg: { size: "1.125rem", lineHeight: "1.75rem" }, // 18px
    xl: { size: "1.25rem", lineHeight: "1.75rem" }, // 20px
    "2xl": { size: "1.5rem", lineHeight: "2rem" }, // 24px
    "3xl": { size: "1.875rem", lineHeight: "2.25rem" }, // 30px
    "4xl": { size: "2.25rem", lineHeight: "2.5rem" }, // 36px
  },

  // Poids de police
  weights: {
    light: 300,
    normal: 400,
    medium: 500,
    semibold: 600,
    bold: 700,
    extrabold: 800,
  },

  // Familles de police
  fonts: {
    sans: '-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif',
    mono: '"SF Mono", "Monaco", "Inconsolata", "Roboto Mono", monospace',
  },
} as const;

// ============================================================================
// BORDER RADIUS - Arrondi cohérent
// ============================================================================
export const borderRadius = {
  none: "0",
  xs: "0.25rem", // 4px
  sm: "0.375rem", // 6px - standard Tailwind
  md: "0.5rem", // 8px
  lg: "0.75rem", // 12px
  xl: "1rem", // 16px
  "2xl": "1.5rem", // 24px
  full: "9999px", // Fully rounded
} as const;

// ============================================================================
// SHADOWS - Ombres subtiles pour profondeur
// ============================================================================
export const shadows = {
  none: "none",
  xs: "0 1px 2px 0 rgba(0, 0, 0, 0.05)",
  sm: "0 1px 2px 0 rgba(0, 0, 0, 0.1)",
  base: "0 1px 3px 0 rgba(0, 0, 0, 0.1), 0 1px 2px 0 rgba(0, 0, 0, 0.06)",
  md: "0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -1px rgba(0, 0, 0, 0.06)",
  lg: "0 10px 15px -3px rgba(0, 0, 0, 0.1), 0 4px 6px -2px rgba(0, 0, 0, 0.05)",
  xl: "0 20px 25px -5px rgba(0, 0, 0, 0.1), 0 10px 10px -5px rgba(0, 0, 0, 0.04)",
} as const;

// ============================================================================
// BREAKPOINTS - Points de rupture responsifs
// ============================================================================
export const breakpoints = {
  xs: "0px", // Mobile
  sm: "640px", // Small devices
  md: "768px", // Tablets (portrait)
  lg: "1024px", // Tablets (landscape) / Small desktop
  xl: "1280px", // Desktop
  "2xl": "1536px", // Large desktop
} as const;

// ============================================================================
// COMPONENT SIZES - Tailles de composants
// ============================================================================
export const componentSizes = {
  // Boutons
  button: {
    xs: { padding: "0.25rem 0.5rem", fontSize: "0.75rem" },
    sm: { padding: "0.375rem 0.75rem", fontSize: "0.875rem" },
    md: { padding: "0.5rem 1rem", fontSize: "1rem" },
    lg: { padding: "0.75rem 1.5rem", fontSize: "1.125rem" },
    xl: { padding: "1rem 1.5rem", fontSize: "1.25rem" },
  },

  // Pad de contrôle (grille)
  pad: {
    xs: { width: "60px", height: "60px" }, // Mobile minimal
    sm: { width: "80px", height: "80px" }, // Mobile standard
    md: { width: "120px", height: "120px" }, // Tablet
    lg: { width: "160px", height: "160px" }, // Desktop
    xl: { width: "200px", height: "200px" }, // Large desktop
  },

  // Espacements internes des pads
  padPadding: {
    xs: spacing.xs, // 4px
    sm: spacing.sm, // 8px
    md: spacing.md, // 16px
    lg: spacing.lg, // 24px
  },
} as const;

// ============================================================================
// ANIMATION & TRANSITION
// ============================================================================
export const animation = {
  durations: {
    fast: "75ms",
    base: "150ms",
    slow: "250ms",
    slower: "350ms",
  },

  easings: {
    linear: "linear",
    in: "cubic-bezier(0.4, 0, 1, 1)",
    out: "cubic-bezier(0, 0, 0.2, 1)",
    inOut: "cubic-bezier(0.4, 0, 0.2, 1)",
  },
} as const;

// ============================================================================
// PAD STATES - États des pads de contrôle
// ============================================================================
export const padStates = {
  colors: {
    active: colors.primary[500], // 188 95% 52%
    inactive: colors.neutral[800], // 220 14% 18%
    hover: colors.neutral[700], // 220 14% 24% (légèrement plus clair)
    pressed: colors.primary[600], // 188 80% 45%
  },

  glows: {
    default: "0 0 20px hsl(var(--glow) / 0.5), 0 0 40px hsl(var(--glow) / 0.2)",
    hover: "0 0 10px hsl(var(--glow) / 0.3)",
    active: "0 0 30px hsl(var(--glow) / 0.7), 0 0 60px hsl(var(--glow) / 0.4)",
  },
} as const;

// ============================================================================
// CONNECTION STATES - États de connexion
// ============================================================================
export const connectionStates = {
  online: colors.success[500], // 142 76% 48%
  offline: colors.destructive[500], // 15 90% 58%
  connecting: colors.warning[500], // 38 94% 52%
} as const;

// ============================================================================
// GRID SYSTEM - Configuration de grille responsive
// ============================================================================
export const gridSystem = {
  // Nombre de colonnes par breakpoint
  columns: {
    xs: 2, // Mobile
    sm: 3, // Mobile landscape
    md: 4, // Tablet
    lg: 6, // Desktop
    xl: 8, // Large desktop
  },

  // Espacement entre pads
  gap: {
    xs: spacing.xs, // 4px
    sm: spacing.sm, // 8px
    md: spacing.md, // 16px
    lg: spacing.lg, // 24px
    xl: spacing.xl, // 32px
  },

  // Padding des conteneurs
  containerPadding: {
    xs: spacing.sm, // 8px
    sm: spacing.md, // 16px
    md: spacing.lg, // 24px
    lg: spacing.xl, // 32px
    xl: "3rem", // 48px
  },
} as const;

// ============================================================================
// ZINDEX - Ordre de superposition
// ============================================================================
export const zIndex = {
  base: 0,
  dropdown: 1000,
  sticky: 1020,
  fixed: 1030,
  backdrop: 1040,
  offcanvas: 1050,
  modal: 1060,
  popover: 1070,
  tooltip: 1080,
} as const;

export const tokens = {
  colors,
  spacing,
  typography,
  borderRadius,
  shadows,
  breakpoints,
  componentSizes,
  animation,
  padStates,
  connectionStates,
  gridSystem,
  zIndex,
} as const;

export default tokens;
