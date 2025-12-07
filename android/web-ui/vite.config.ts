import react from "@vitejs/plugin-react-swc";
import { componentTagger } from "lovable-tagger";
import path from "path";
import { defineConfig } from "vite";

// Output direct dans les assets Android
const ANDROID_ASSETS_DIR = "../app/src/main/assets/web";

// Plugin pour optimiser le HTML pour Android WebView
const fixAndroidAssetsPlugin = {
  name: "fix-android-assets",
  transformIndexHtml(html: string) {
    // Toujours appliquer les optimisations pour WebView
    // Supprimer crossorigin des assets (peut causer des blocages)
    html = html
      .replace(/\s+crossorigin(?=\s|>)/g, "")
      .replace(/\s+crossorigin$/g, "");

    // Réordonner : mettre les <link rel="stylesheet"> AVANT les <script>
    // Cela force le navigateur à charger le CSS avant d'exécuter le JS
    const stylesheetMatch = html.match(/<link\s+rel="stylesheet"[^>]*>/g);
    const scriptMatch = html.match(/<script\s+type="module"[^>]*><\/script>/g);

    if (stylesheetMatch && scriptMatch) {
      // Supprimer le stylesheet de sa position actuelle
      html = html.replace(/<link\s+rel="stylesheet"[^>]*>\s*/g, "");
      // L'insérer avant le premier script
      html = html.replace(
        scriptMatch[0],
        stylesheetMatch.join("\n    ") + "\n    " + scriptMatch[0]
      );
    }
    return html;
  },
};

// https://vitejs.dev/config/
export default defineConfig(({ mode }) => {
  const isDev = mode === "development";

  return {
    // Chemins absolus pour WebView Android (WebViewAssetLoader intercepte /assets/)
    base: "/",
    server: {
      host: "::",
      port: 8080,
    },
    build: {
      // Output direct dans les assets Android
      outDir: ANDROID_ASSETS_DIR,
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
