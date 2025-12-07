# Changelog - Control Deck

Toutes les modifications notables de ce projet seront documentées dans ce fichier.

## [Non versionné] - En développement

### Ajouté

#### Application Android
- **UI complète** : Interface utilisateur avec Material Design 3
- **Thèmes** : Support des thèmes clair, sombre et personnalisé
- **Animations** : Animations fluides pour les transitions
- **Haptic Feedback** : Retour haptique pour les interactions
- **Responsive** : Support des différentes tailles d'écran
- **Accessibilité** : Support TalkBack et navigation clavier

#### Création de profils
- **Assistant de création** : Wizard pour guider les nouveaux utilisateurs
- **Templates** : Modèles prédéfinis (Streaming, Audio, Gaming)
- **Validation** : Validation complète avec messages d'erreur clairs
- **Grille avancée** : Snap-to-grid, guides d'alignement

#### Éditeur de profils
- **Undo/Redo** : Système complet d'annulation/rétablissement
- **Prévisualisation** : Aperçu en temps réel
- **Groupes** : Groupement de contrôles
- **Calques** : Gestion du z-index
- **Bibliothèque d'icônes** : Collection d'icônes Material Design
- **Sélecteur de couleurs** : Palette, Hex, RGB
- **Barre d'outils** : Outils d'alignement, distribution, zoom

#### Import/Export
- **Import** : Sélecteur de fichiers avec validation
- **Export** : Options de format, partage direct
- **Fusion** : Fusion intelligente de profils
- **Sauvegardes** : Système automatique et manuel
- **Résolution de conflits** : Options de résolution

#### Connexion
- **Reconnexion automatique** : Stratégie intelligente avec backoff
- **Monitoring** : Indicateurs de latence et qualité
- **Mode hors ligne** : Cache des profils
- **Synchronisation** : Upload/download automatique

#### Serveur
- **Actions intégrées** :
  - Keyboard (toutes plateformes)
  - Audio (Windows avancé, macOS, Linux)
  - OBS Studio (basique et avancé)
  - Media Keys (Windows)
  - System (Windows : lock, sleep, shutdown, windows)
  - Clipboard (Windows)
  - Screenshots (Windows)
  - Processes (Windows)
  - Files (Windows)

#### Système de plugins
- **Architecture complète** : Chargement dynamique, activation/désactivation
- **Sandboxing** : Isolation et sécurité
- **Configuration** : Gestion de configuration par plugin
- **Template** : Template pour créer des plugins
- **Exemples** : Hello World, Webhook

#### Plugins
- **Discord** :
  - Contrôle vocal (mute, deafen)
  - Rich Presence avec presets
  - Messages et réactions (structure)
- **Spotify** :
  - Contrôle de lecture (play/pause, next/previous)
  - Informations de piste (titre, artiste, album)
- **OBS** :
  - Streaming et enregistrement
  - Gestion des scènes
  - Contrôle des sources (visibilité, verrouillage, position)
  - Gestion des filtres
  - Transitions
  - Studio Mode

#### Performance
- **Cache** : Cache pour requêtes fréquentes
- **Queue** : Queue d'actions avec priorités
- **Monitoring** : Monitoring CPU, mémoire, latence

#### Sécurité
- **Tokens** : Gestion sécurisée avec expiration
- **Rate Limiting** : Limitation par client, action, IP
- **Audit Logs** : Logs complets de toutes les actions

#### Documentation
- **Guide utilisateur** : Installation, utilisation, FAQ
- **Guide développeur** : Architecture, API, plugins
- **Guide de contribution** : Standards, workflow
- **Documentation plugins** : Guide de développement

### Modifié

- Amélioration de la gestion des erreurs
- Optimisation des performances
- Amélioration de la robustesse de la connexion

### Sécurité

- Sandboxing des plugins
- Validation des entrées
- Rate limiting
- Audit logging

---

## Format

Le format est basé sur [Keep a Changelog](https://keepachangelog.com/fr/1.0.0/),
et ce projet adhère à [Semantic Versioning](https://semver.org/lang/fr/).

### Types de changements

- **Ajouté** : Nouvelles fonctionnalités
- **Modifié** : Changements dans les fonctionnalités existantes
- **Déprécié** : Fonctionnalités qui seront supprimées
- **Supprimé** : Fonctionnalités supprimées
- **Corrigé** : Corrections de bugs
- **Sécurité** : Vulnérabilités corrigées






