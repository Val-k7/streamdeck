import react from "@vitejs/plugin-react-swc";
import { componentTagger } from "lovable-tagger";
import path from "path";
import { defineConfig } from "vite";

// https://vitejs.dev/config/
export default defineConfig(({ mode }) => {
  const isDev = mode === "development";
  const outDir = "../static";

  return {
    base: "/",
    server: {
      host: "::",
      port: 8080,
      proxy: {
        "/api": {
          target: "http://localhost:4455",
          changeOrigin: true,
          rewrite: (path) => path.replace(/^\/api/, ""),
        },
        "/ws": {
          target: "ws://localhost:4455",
          ws: true,
        },
      },
    },
    build: {
      outDir,
      assetsDir: "assets",
      sourcemap: isDev,
      emptyOutDir: true,
      // Optimisations
      minify: "esbuild",
      rollupOptions: {
        output: {
          // Code splitting pour réduire la taille du bundle principal
          manualChunks: {
            // Séparer les dépendances volumineuses
            vendor: ["react", "react-dom", "react-router-dom"],
            hooks: ["react-hook-form", "zod", "@hookform/resolvers"],
            ui: [
              "@radix-ui/react-dialog",
              "@radix-ui/react-tabs",
              "@radix-ui/react-select",
            ],
          },
        },
      },
      // Augmenter la limite d'avertissement de taille de chunk
      chunkSizeWarningLimit: 1000,
    },
    plugins: [
      react(),
      mode === "development" && componentTagger(),
    ].filter(Boolean),
    resolve: {
      alias: {
        "@": path.resolve(__dirname, "./src"),
      },
    },
  };
});
