import react from "@vitejs/plugin-react-swc";
import { componentTagger } from "lovable-tagger";
import path from "path";
import { defineConfig } from "vite";

// Détecter le mode Android AVANT la config
const isAndroidMode =
  process.argv.includes("--mode") && process.argv.includes("android");

// Plugin pour optimiser le HTML pour Android WebView
const fixAndroidAssetsPlugin = {
  name: "fix-android-assets",
  transformIndexHtml(html: string) {
    if (isAndroidMode) {
      // Supprimer crossorigin des assets pour WebView (peut causer des blocages)
      html = html
        .replace(/\s+crossorigin(?=\s|>)/g, "")
        .replace(/\s+crossorigin$/g, "");

      // Réordonner : mettre les <link rel="stylesheet"> AVANT les <script>
      // Cela force le navigateur à charger le CSS avant d'exécuter le JS
      const stylesheetMatch = html.match(/<link\s+rel="stylesheet"[^>]*>/g);
      const scriptMatch = html.match(
        /<script\s+type="module"[^>]*><\/script>/g
      );

      if (stylesheetMatch && scriptMatch) {
        // Supprimer le stylesheet de sa position actuelle
        html = html.replace(/<link\s+rel="stylesheet"[^>]*>\s*/g, "");
        // L'insérer avant le premier script
        html = html.replace(
          scriptMatch[0],
          stylesheetMatch.join("\n    ") + "\n    " + scriptMatch[0]
        );
      }
    }
    return html;
  },
};

// https://vitejs.dev/config/
export default defineConfig(({ mode }) => {
  const isAndroidBuild = mode === "android";

  return {
    server: {
      host: "::",
      port: 8080,
    },
    build: {
      outDir: "dist",
      assetsDir: "assets",
      sourcemap: mode === "development",
      emptyOutDir: true,
      // Base URL : "/" pour tous (chemins absolus /assets/)
      // WebViewAssetLoader intercepte /assets/ et les sert depuis web/assets/
      base: "/",
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
      fixAndroidAssetsPlugin,
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
