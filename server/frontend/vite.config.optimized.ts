/**
 * Optimized Vite configuration for production builds
 * Includes code splitting, tree shaking, and compression
 */

import react from "@vitejs/plugin-react";
import path from "path";
import { defineConfig } from "vite";
import { visualizer } from "rollup-plugin-visualizer";
import { compression } from "vite-plugin-compression2";

export default defineConfig({
  plugins: [
    react(),

    // Gzip compression for production
    compression({
      algorithm: "gzip",
      threshold: 10240, // Only compress files > 10KB
      deleteOriginalAssets: false,
    }),

    // Brotli compression (better than gzip)
    compression({
      algorithm: "brotliCompress",
      threshold: 10240,
      deleteOriginalAssets: false,
    }),

    // Bundle size visualization
    visualizer({
      filename: "./dist/stats.html",
      open: false,
      gzipSize: true,
      brotliSize: true,
    }),
  ],

  resolve: {
    alias: {
      "@": path.resolve(__dirname, "./src"),
    },
  },

  build: {
    // Output directory
    outDir: "dist",
    assetsDir: "assets",

    // Source maps for debugging (disable in production for smaller size)
    sourcemap: process.env.NODE_ENV !== "production",

    // Minification
    minify: "terser",
    terserOptions: {
      compress: {
        drop_console: process.env.NODE_ENV === "production", // Remove console.log
        drop_debugger: true,
        pure_funcs: process.env.NODE_ENV === "production" ? ["console.log", "console.debug"] : [],
      },
      format: {
        comments: false, // Remove comments
      },
    },

    // Chunk size warnings
    chunkSizeWarningLimit: 500, // KB

    // Manual chunk splitting for better caching
    rollupOptions: {
      output: {
        // Split vendor code
        manualChunks: (id) => {
          // React core
          if (id.includes("node_modules/react") || id.includes("node_modules/react-dom")) {
            return "react-vendor";
          }

          // Radix UI components (large library)
          if (id.includes("node_modules/@radix-ui")) {
            return "radix-ui";
          }

          // React Query
          if (id.includes("node_modules/@tanstack/react-query")) {
            return "react-query";
          }

          // Lucide icons
          if (id.includes("node_modules/lucide-react")) {
            return "icons";
          }

          // Other node_modules
          if (id.includes("node_modules")) {
            return "vendor";
          }
        },

        // Asset naming
        assetFileNames: (assetInfo) => {
          const info = assetInfo.name?.split(".");
          const ext = info?.[info.length - 1];

          if (/png|jpe?g|svg|gif|tiff|bmp|ico/i.test(ext || "")) {
            return `assets/images/[name]-[hash][extname]`;
          } else if (/woff|woff2|eot|ttf|otf/i.test(ext || "")) {
            return `assets/fonts/[name]-[hash][extname]`;
          }

          return `assets/[name]-[hash][extname]`;
        },

        // Chunk naming
        chunkFileNames: "assets/js/[name]-[hash].js",
        entryFileNames: "assets/js/[name]-[hash].js",
      },
    },

    // CSS code splitting
    cssCodeSplit: true,

    // Report compressed size
    reportCompressedSize: true,

    // Target modern browsers for smaller bundle
    target: "es2020",
  },

  // Optimize dependencies
  optimizeDeps: {
    include: [
      "react",
      "react-dom",
      "@tanstack/react-query",
    ],
    exclude: ["@storybook/test"],
  },

  // Server configuration for development
  server: {
    port: 3000,
    strictPort: false,
    host: true,
    open: false,
  },

  // Preview server configuration
  preview: {
    port: 4173,
    strictPort: false,
    host: true,
  },
});
