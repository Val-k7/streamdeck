# Version Desktop de Débogage - Control Deck

## Vue d'ensemble

Une version desktop de l'application Control Deck a été créée pour permettre le débogage et les tests directement sur PC, sans avoir besoin d'un émulateur Android ou d'un appareil physique.

## Structure créée

### Module `desktopApp`
- **Point d'entrée** : `DesktopMain.kt` - Lance l'application desktop
- **Application** : `DesktopApp.kt` - Interface utilisateur principale
- **Container** : `DesktopAppContainer.kt` - Gestion des dépendances

### Implémentations Desktop

#### Storage (`desktop/storage/`)
- `DesktopProfileStorage.kt` - Stockage des profils (fichiers JSON)
- `DesktopAssetCache.kt` - Cache des assets/icons
- `DesktopPendingActionStorage.kt` - Actions en attente
- `DesktopProfileBackupManager.kt` - Sauvegardes de profils

#### Preferences (`desktop/preferences/`)
- `DesktopSecurePreferences.kt` - Préférences chiffrées (AES)
- `DesktopSettingsRepository.kt` - Paramètres de l'application
- `DesktopPairedServersRepository.kt` - Serveurs appairés

#### Network (`desktop/network/`)
- `DesktopWebSocketClient.kt` - Client WebSocket
- `DesktopNetworkStatusMonitor.kt` - Monitoring réseau
- `DesktopAuthRepository.kt` - Authentification
- `DesktopServerDiscoveryManager.kt` - Découverte de serveurs (UDP)
- `DesktopPairingManager.kt` - Appairage
- `DesktopConnectionManager.kt` - Gestion de connexion
- `DesktopControlEventSender.kt` - Envoi d'événements

#### Logging (`desktop/logging/`)
- `DesktopLogger.kt` - Logger (SLF4J/Logback)
- `DesktopDiagnosticsRepository.kt` - Diagnostics

## Utilisation

### Lancement rapide

**Windows:**
```powershell
cd android
.\scripts\run-desktop.ps1
```

**Linux/macOS:**
```bash
cd android
./scripts/run-desktop.sh
```

### Build manuel

```bash
cd android
./gradlew :desktopApp:build
./gradlew :desktopApp:run
```

## Données

Toutes les données sont stockées dans `~/.controldeck/` :
- `profiles/` - Profils (fichiers JSON individuels)
- `settings.properties` - Paramètres
- `secure_prefs.properties` - Préférences chiffrées
- `paired_servers.json` - Serveurs appairés
- `pending_actions.properties` - Actions en attente
- `cache/` - Cache
- `backups/` - Sauvegardes
- `icons/` - Icônes personnalisées

## Limitations connues

1. **Découverte de serveurs** : Implémentation basique (UDP multicast à compléter)
2. **Pairing** : Implémentation simplifiée
3. **AssetCache** : Conversion BufferedImage -> ImageBitmap à finaliser
4. **Notifications** : Non disponible sur desktop
5. **Certaines fonctionnalités Android** : Nécessitent des adaptations

## Prochaines étapes

Pour finaliser la version desktop :

1. **Compléter les implémentations réseau** :
   - UDP multicast pour la découverte
   - Logique de pairing complète

2. **Finaliser AssetCache** :
   - Conversion BufferedImage -> ImageBitmap
   - Chargement des icônes depuis les assets

3. **Tests** :
   - Tester la connexion au serveur
   - Tester la synchronisation des profils
   - Tester les actions de contrôle

4. **Optimisations** :
   - Améliorer les performances
   - Gérer les erreurs réseau
   - Ajouter des logs de débogage

## Notes de développement

- Les composants UI sont partagés avec l'app Android via `:core:ui`
- Les implémentations desktop remplacent les dépendances Android
- Le code est organisé de manière similaire à l'app Android pour faciliter la maintenance


