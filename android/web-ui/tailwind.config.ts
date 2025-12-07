import type { Config } from "tailwindcss";
import { tokens } from "./src/styles/tokens";

export default {
  darkMode: ["class"],
  content: [
    "./pages/**/*.{ts,tsx}",
    "./components/**/*.{ts,tsx}",
    "./app/**/*.{ts,tsx}",
    "./src/**/*.{ts,tsx}",
  ],
  prefix: "",
  theme: {
    // Utiliser les breakpoints des tokens
    screens: {
      xs: tokens.breakpoints.xs,
      sm: tokens.breakpoints.sm,
      md: tokens.breakpoints.md,
      lg: tokens.breakpoints.lg,
      xl: tokens.breakpoints.xl,
      "2xl": tokens.breakpoints["2xl"],
    },

    container: {
      center: true,
      padding: "2rem",
      screens: {
        "2xl": "1400px",
      },
    },

    extend: {
      colors: {
        // CSS variables fallback pour les couleurs système
        border: "hsl(var(--border))",
        input: "hsl(var(--input))",
        ring: "hsl(var(--ring))",
        background: "hsl(var(--background))",
        foreground: "hsl(var(--foreground))",
        primary: {
          DEFAULT: "hsl(var(--primary))",
          foreground: "hsl(var(--primary-foreground))",
        },
        secondary: {
          DEFAULT: "hsl(var(--secondary))",
          foreground: "hsl(var(--secondary-foreground))",
        },
        destructive: {
          DEFAULT: "hsl(var(--destructive))",
          foreground: "hsl(var(--destructive-foreground))",
        },
        muted: {
          DEFAULT: "hsl(var(--muted))",
          foreground: "hsl(var(--muted-foreground))",
        },
        accent: {
          DEFAULT: "hsl(var(--accent))",
          foreground: "hsl(var(--accent-foreground))",
        },
        popover: {
          DEFAULT: "hsl(var(--popover))",
          foreground: "hsl(var(--popover-foreground))",
        },
        card: {
          DEFAULT: "hsl(var(--card))",
          foreground: "hsl(var(--card-foreground))",
        },
        pad: {
          active: "hsl(var(--pad-active))",
          inactive: "hsl(var(--pad-inactive))",
          hover: "hsl(var(--pad-hover))",
          pressed: "hsl(var(--pad-pressed))",
        },
        glow: {
          DEFAULT: "hsl(var(--glow))",
          strong: "hsl(var(--glow-strong))",
        },
        connection: {
          online: "hsl(var(--connection-online))",
          offline: "hsl(var(--connection-offline))",
          connecting: "hsl(var(--connection-connecting))",
        },
      },

      // Espacement depuis les tokens
      spacing: Object.fromEntries(
        Object.entries(tokens.spacing).map(([key, value]) => [key, value])
      ),

      // Tailles de police depuis les tokens
      fontSize: Object.fromEntries(
        Object.entries(tokens.typography.sizes).map(([key, value]) => [
          key,
          [value.size, value.lineHeight],
        ])
      ),

      // Poids de police depuis les tokens
      fontWeight: tokens.typography.weights,

      // Border radius depuis les tokens
      borderRadius: tokens.borderRadius,

      // Ombres depuis les tokens
      boxShadow: tokens.shadows,

      keyframes: {
        "accordion-down": {
          from: {
            height: "0",
          },
          to: {
            height: "var(--radix-accordion-content-height)",
          },
        },
        "accordion-up": {
          from: {
            height: "var(--radix-accordion-content-height)",
          },
          to: {
            height: "0",
          },
        },
        // Animations personnalisées pour pads
        "pad-press": {
          "0%": { transform: "scale(1)" },
          "50%": { transform: "scale(0.95)" },
          "100%": { transform: "scale(1)" },
        },
        "pad-glow": {
          "0%, 100%": {
            boxShadow:
              "0 0 10px hsl(var(--glow) / 0.3), 0 0 20px hsl(var(--glow) / 0.1)",
          },
          "50%": {
            boxShadow:
              "0 0 20px hsl(var(--glow) / 0.5), 0 0 40px hsl(var(--glow) / 0.2)",
          },
        },
      },

      animation: {
        "accordion-down": "accordion-down 0.2s ease-out",
        "accordion-up": "accordion-up 0.2s ease-out",
        "pad-press": "pad-press 150ms cubic-bezier(0.4, 0, 0.2, 1)",
        "pad-glow": "pad-glow 2s cubic-bezier(0.4, 0, 0.6, 1) infinite",
      },
    },
  },
  // eslint-disable-next-line @typescript-eslint/no-require-imports
  plugins: [require("tailwindcss-animate")],
} satisfies Config;
