# 🧪 Guide de Test - Interface Web Control Deck

## Prérequis

1. Serveur Node.js démarré et accessible
2. Navigateur moderne (Chrome, Firefox, Edge, Safari)
3. Profils disponibles sur le serveur

## Tests de Connexion WebSocket

### Test 1 : Connexion basique
1. Ouvrir l'interface web
2. Cliquer sur le bouton "Settings"
3. Aller dans l'onglet "Connection"
4. Entrer l'IP du serveur (ex: `localhost` ou `192.168.1.100`)
5. Entrer le port (ex: `8080`)
6. Cliquer sur "Connect"
7. **Résultat attendu** : L'indicateur de connexion passe à "online" (vert)

### Test 2 : Connexion avec TLS
1. Répéter les étapes 1-4
2. Cocher "Use TLS (WSS)"
3. Cliquer sur "Connect"
4. **Résultat attendu** : Connexion sécurisée établie

### Test 3 : Gestion des erreurs
1. Entrer une IP invalide (ex: `999.999.999.999`)
2. Cliquer sur "Connect"
3. **Résultat attendu** : Message d'erreur affiché, connexion échoue

### Test 4 : Reconnexion automatique
1. Se connecter au serveur
2. Arrêter le serveur
3. **Résultat attendu** : Tentative de reconnexion automatique avec backoff exponentiel
4. Redémarrer le serveur
5. **Résultat attendu** : Reconnexion réussie automatiquement

## Tests de Gestion des Profils

### Test 5 : Chargement des profils
1. Se connecter au serveur
2. Aller dans l'onglet "Profiles"
3. **Résultat attendu** : Liste des profils disponibles affichée

### Test 6 : Sélection de profil
1. Sélectionner un profil dans la liste déroulante
2. **Résultat attendu** :
   - Le profil est chargé
   - Les pads s'affichent selon la configuration du profil
   - Toast de confirmation affiché

### Test 7 : Affichage des pads
1. Sélectionner un profil avec différents types de contrôles
2. **Résultat attendu** :
   - Les boutons s'affichent correctement
   - Les toggles s'affichent avec leur état
   - Les faders s'affichent avec leur valeur
   - Les icônes sont correctement mappées

## Tests d'Envoi d'Actions

### Test 8 : Action bouton
1. Sélectionner un profil avec un bouton
2. Cliquer sur le bouton
3. **Résultat attendu** :
   - Feedback visuel (succès/erreur)
   - Action exécutée côté serveur
   - Toast de confirmation si succès

### Test 9 : Action toggle
1. Sélectionner un profil avec un toggle
2. Cliquer sur le toggle
3. **Résultat attendu** :
   - L'état du toggle change (on/off)
   - Action envoyée au serveur avec valeur 0 ou 1
   - Feedback visuel

### Test 10 : Action fader
1. Sélectionner un profil avec un fader
2. Faire glisser le fader
3. **Résultat attendu** :
   - La valeur du fader change en temps réel
   - Actions envoyées au serveur avec valeurs 0-1
   - Feedback visuel

### Test 11 : Long press
1. Maintenir appuyé sur un pad
2. **Résultat attendu** : Action long press déclenchée (si configurée)

## Tests de Feedback Visuel

### Test 12 : Feedback succès
1. Exécuter une action qui réussit
2. **Résultat attendu** :
   - Indicateur visuel de succès sur le pad
   - Toast de confirmation
   - Feedback disparaît après 3 secondes

### Test 13 : Feedback erreur
1. Exécuter une action qui échoue (ex: serveur arrêté)
2. **Résultat attendu** :
   - Indicateur visuel d'erreur sur le pad
   - Toast d'erreur avec message détaillé
   - Feedback disparaît après 5 secondes

## Tests de Compatibilité

### Test 14 : Navigateurs
Tester sur :
- Chrome (dernière version)
- Firefox (dernière version)
- Edge (dernière version)
- Safari (si disponible)

**Résultat attendu** : Fonctionnalités identiques sur tous les navigateurs

### Test 15 : Tailles d'écran
Tester sur :
- Desktop (1920x1080)
- Laptop (1366x768)
- Tablette (768x1024)
- Mobile (375x667)

**Résultat attendu** : Interface responsive et utilisable sur toutes les tailles

### Test 16 : Interactions tactiles
1. Sur un appareil tactile, tester :
   - Tap sur les pads
   - Long press
   - Glisser sur les faders
2. **Résultat attendu** : Toutes les interactions fonctionnent correctement

## Tests d'Accessibilité

### Test 17 : Navigation au clavier
1. Utiliser Tab pour naviguer entre les pads
2. Utiliser Enter/Espace pour activer
3. **Résultat attendu** : Navigation clavier fonctionnelle

### Test 18 : Lecteurs d'écran
1. Activer un lecteur d'écran (NVDA, JAWS, VoiceOver)
2. Naviguer dans l'interface
3. **Résultat attendu** : Les labels et rôles sont correctement annoncés

## Tests de Performance

### Test 19 : Chargement initial
1. Ouvrir l'interface web
2. Mesurer le temps de chargement
3. **Résultat attendu** : Chargement < 2 secondes

### Test 20 : Réactivité
1. Exécuter plusieurs actions rapidement
2. **Résultat attendu** : Pas de lag, toutes les actions sont traitées

## Checklist de Test Rapide

- [ ] Connexion WebSocket fonctionne
- [ ] Déconnexion fonctionne
- [ ] Reconnexion automatique fonctionne
- [ ] Chargement des profils fonctionne
- [ ] Sélection de profil fonctionne
- [ ] Affichage des pads fonctionne
- [ ] Actions bouton fonctionnent
- [ ] Actions toggle fonctionnent
- [ ] Actions fader fonctionnent
- [ ] Feedback visuel fonctionne
- [ ] Toasts fonctionnent
- [ ] Tooltips fonctionnent
- [ ] Responsive fonctionne
- [ ] Accessibilité fonctionne

## Problèmes Connus

Aucun problème connu actuellement.

## Notes

- Les tests nécessitent un serveur en cours d'exécution
- Certains tests nécessitent OBS Studio pour les actions OBS
- Les tests d'accessibilité nécessitent des outils spécialisés

