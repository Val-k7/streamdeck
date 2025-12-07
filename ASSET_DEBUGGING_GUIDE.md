# Guide de Diagnostic - CSS/Icônes Manquants en Build USB Android

Ce guide aide à diagnostiquer et corriger les problèmes de CSS/icônes manquants lors du build USB via Android Studio.

## 🔍 Diagnostic Rapide

### 1. Vérifier les logs Android Studio

Lorsque vous branchez l'appareil en USB et lancez l'app:

```bash
# Dans Android Studio: Logcat (View > Tool Windows > Logcat)
# Filter par: WebViewAssetLoader
# Chercher les messages suivants:
```

#### ✓ OK - Ce que vous devriez voir:

```
D  WebViewAssetLoader: Asset loaded successfully: web/assets/index-abc123.css (MIME: text/css; charset=UTF-8)
D  WebViewAssetLoader: Found CSS files: index-abc123.css
D  WebViewAssetLoader: Found SVG files: Play.svg, Volume2.svg, ...
```

#### ❌ PROBLÈME - Ce que vous verrez en cas d'erreur:

```
E  WebViewAssetLoader: ❌ CSS NOT FOUND: https://appassets.androidplatform.net/assets/index-abc123.css - Styling will be missing!
E  WebViewAssetLoader: ❌ CRITICAL: web/assets directory is EMPTY or does not exist!
W  WebViewAssetLoader: ⚠️ NO CSS FILES FOUND in assets! This may cause styling issues.
```

### 2. Chrome DevTools (USB Debugging)

Connectez l'appareil et ouvrez `chrome://inspect` dans Chrome sur votre PC.

Dans la console DevTools de l'app:

```javascript
// Vérifier les feuilles de style chargées
console.log(
  "Loaded stylesheets:",
  Array.from(document.styleSheets).map((ss) => ss.href)
);

// Vérifier les règles CSS appliquées
console.log("Computed style for body:", window.getComputedStyle(document.body));
```

## 📋 Problèmes Courants et Solutions

### Problème 1: "NO CSS FILES FOUND"

**Cause**: Les fichiers CSS ne sont pas copiés dans `android/app/src/main/assets/web/`.

**Solution**:

```powershell
# Sur Windows (PowerShell), dans la racine du projet:
cd android/scripts
./copy-ui-assets.ps1 -Build

# OU si vous avez déjà un build web:
./copy-ui-assets.ps1
```

Vérifiez ensuite:

```powershell
Get-ChildItem -Path "android/app/src/main/assets/web/assets/" -Filter "*.css"
```

### Problème 2: "NO SVG FILES FOUND" (Icônes manquantes)

**Cause**: Les fichiers d'icônes SVG ne sont pas dans le répertoire assets.

**Vérification**:

```powershell
# Chercher les fichiers SVG
Get-ChildItem -Path "android/app/src/main/assets/web/" -Filter "*.svg" -Recurse

# Lister tous les assets disponibles
Get-ChildItem -Path "android/app/src/main/assets/web/assets/" | Select-Object Name
```

### Problème 3: Chemins absolus au lieu de relatifs

**Cause**: `index.html` contient `/assets/` au lieu de `./assets/`.

**Vérification**:

```powershell
# Chercher les chemins absolus dans index.html
Select-String -Path "android/app/src/main/assets/web/index.html" -Pattern 'href="/|src="/'
```

**Solution automatique**:

```powershell
# Le script fix-ui-assets.ps1 corrige automatiquement cela
./fix-ui-assets.ps1
```

**Solution manuelle**:
Remplacer dans `index.html`:

- `href="/assets/` → `href="./assets/`
- `src="/assets/` → `src="./assets/`

### Problème 4: MIME types incorrects

**Cause**: Android WebView reçoit un mauvais type MIME pour CSS/SVG.

**Correction appliquée** dans `WebViewScreen.kt`:

```kotlin
// Avant:
cleanPath.endsWith(".css") -> "text/css"

// Après:
cleanPath.endsWith(".css") -> "text/css; charset=UTF-8"
cleanPath.endsWith(".svg") -> "image/svg+xml; charset=UTF-8"
```

## 🛠️ Processus de Build Complet

Pour une build complète et correcte:

```powershell
# 1. Build l'UI React/Vite
cd web
npm run build

# 2. Copier les assets dans Android
cd ../android/scripts
./copy-ui-assets.ps1

# 3. Vérifier les fichiers
Write-Host "Vérification des assets..."
Get-ChildItem -Path "../app/src/main/assets/web/" -Recurse | Select-Object FullName

# 4. Builder l'APK Android depuis Android Studio
# OU en ligne de commande:
cd ..
./gradlew assembleDebug
```

## 📊 Structure Attendue

Après un build réussi, vous devriez avoir:

```
android/app/src/main/assets/web/
├── index.html                    (avec chemins ./assets/)
├── assets/
│   ├── index-abc123.css         (fichier CSS généré par Vite)
│   ├── main-def456.js           (bundle JavaScript principal)
│   └── ... (autres chunks)
├── images/
│   └── ... (images de l'app)
└── ... (autres fichiers)
```

## 🔧 Améliorations Apportées

### WebViewScreen.kt

1. ✓ MIME types améliorés avec charset explicite
2. ✓ Support des fonts (WOFF, TTF, EOT)
3. ✓ Support des images modernes (WebP, GIF)
4. ✓ Logs de diagnostic enrichis pour CSS/SVG manquants
5. ✓ Détection des répertoires vides
6. ✓ Fallback SPA pour les routes React Router

### fix-ui-assets.ps1

1. ✓ Vérification des fichiers CSS présents
2. ✓ Comptage des assets par type (SVG, JS, images)
3. ✓ Diagnostic des chemins absolus vs relatifs
4. ✓ Correction des URLs de polices dans les CSS
5. ✓ Messages visuels avec couleurs et symboles

## 🧪 Test de Vérification

Après le build, testez en ouvrant l'app et en cherchant dans Logcat:

```bash
# Filtre Logcat:
WebViewAssetLoader|WebViewScreen|WebDevTools

# Vous devriez voir:
D  WebViewAssetLoader: ✓ AssetLoader handled request: https://appassets.androidplatform.net/assets/index-abc123.css
I  WebViewScreen: ✓ Page finished loading from: https://appassets.androidplatform.net/
D  WebViewScreen: Stylesheet verification result: ["https://appassets.androidplatform.net/assets/index-abc123.css", ...]
```

## 📞 Support

Si vous avez toujours des problèmes:

1. **Vérifiez les logs complets** en exportant Logcat
2. **Nettoyez et rebuildiez**:
   ```bash
   ./gradlew clean
   ./gradlew assembleDebug
   ```
3. **Réexécutez le script de copie**:
   ```powershell
   ./android/scripts/copy-ui-assets.ps1 -Build
   ```
4. **Videz le cache Android**:
   ```bash
   ./gradlew cleanBuildCache
   ```
