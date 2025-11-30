🎛️ Android Control Deck

Un Stream Deck amélioré pour Android — avec faders, boutons avancés et profils entièrement personnalisables.

Android Control Deck transforme votre smartphone ou tablette Android en surface de contrôle polyvalente : lancement d’actions, commandes OBS, raccourcis, faders, encodeurs virtuels, profils multiples… le tout via réseau local.

✨ Fonctionnalités
🧩 Types de contrôleurs

Boutons

Momentary (appui maintenu)

Toggle (bascule ON/OFF)

Faders / Sliders

Idéal pour volume, transitions, paramètres continus

Encodeurs / “Knobs” virtuels

Rotation virtuelle avec glissement

Pads et boutons colorés

Pour scènes OBS, cues audio, macros, etc.

🗂️ Profils entièrement personnalisables

Mise en page libre sous forme de grille

Taille adaptable (col/row)

Personnalisation complète :

couleur, icône, label

action associée

dimensions du widget (1×1, 2×1, 2×2…)

Import/export en JSON

Interface d’édition simple et intégrée

🔌 Communication réseau

WebSocket en LAN (Wi-Fi)

Temps réel, faible latence

Compatible Windows, macOS, Linux

🖥️ Serveur PC

Fourni en Node.js ou Python, permettant :

Exécution de scripts (bash, batch, python…)

Raccourcis clavier

Contrôle OBS (via OBS-WebSocket)

Contrôle audio, logiciels, macros, etc.

🚀 Installation
💡 Installation rapide (serveur PC)

1. Cloner le repo puis aller dans le dossier serveur :
   - `cd android-control-deck/server`
2. Lancer l'installation selon votre OS :
   - Linux / macOS : `./scripts/install.sh`
   - Windows (PowerShell admin) : `./scripts/install.ps1`
3. Suivre l'assistant `npm run setup` pour choisir le port, le token et le fichier de mapping (validation intégrée).
4. Le service est créé automatiquement (systemd/launchd/SC). Vérifiez le statut ou démarrez manuellement via `npm start` si besoin.
5. Pour une version autonome, générez les exécutables avec `npm run package` (dossier `server/dist/`), livrés avec la config par défaut et le dossier `config/mappings`.

📱 Côté Android

Cloner ce repo

Ouvrir dans Android Studio (Arctic Fox ou +)

Lancer l’app sur un appareil Android

Dans “Paramètres”, entrer l’adresse IP du serveur PC

🖥️ Côté PC

Installer Node.js (ou Python >3.9)

Aller dans le dossier /server

Installer les dépendances :

npm install
# ou
pip install -r requirements.txt


Lancer le serveur :

npm start
# ou
python server.py

📡 Fonctionnement général
1. Android Deck ⇆ Serveur PC via WebSocket

Chaque interaction envoie un message JSON comme :

{
  "controlId": "btn_start",
  "type": "BUTTON",
  "value": 1
}

2. Le serveur exécute une action associée :

Exemple de configuration sur PC :

{
  "btn_start": {
    "action": "keyboard",
    "payload": "CTRL+SHIFT+S"
  },
  "fader_vol": {
    "action": "obs_set_volume",
    "payload": "mic"
  }
}

🧱 Architecture du projet
📱 Côté Android
android-control-deck/
│
├── app/
│   ├── data/
│   │   ├── model/     # Control, Profile, Action
│   │   ├── storage/   # JSON, Room DB
│   │
│   ├── network/       # WebSocket client
│   ├── ui/
│   │   ├── components/ # ButtonView, FaderView, KnobView...
│   │   ├── screens/    # ProfileScreen, EditorScreen...
│   │
│   ├── util/          # Helpers
│
└── server/             # Serveur Node.js ou Python

🖥️ Côté Serveur
server/
│
├── index.js / server.py
├── actions/
│   ├── keyboard.js
│   ├── obs.js
│   ├── scripts.js
│
└── config/
    └── mappings.json

🧩 Format des profils

Les profils sont stockés en JSON sous cette forme :

{
  "id": "default_profile",
  "name": "My Deck",
  "rows": 3,
  "cols": 5,
  "controls": [
    {
      "id": "btn_obs_start",
      "type": "BUTTON",
      "row": 0,
      "col": 0,
      "label": "Start Stream",
      "colorHex": "#FF5722",
      "action": {
        "type": "obs",
        "payload": "StartStreaming"
      }
    },
    {
      "id": "fader_audio",
      "type": "FADER",
      "row": 1,
      "col": 0,
      "minValue": 0,
      "maxValue": 100,
      "action": {
        "type": "obs_volume",
        "payload": "Mic/Aux"
      }
    }
  ]
}

🛠️ Développement
Construire l'UI (Jetpack Compose)

L’interface est entièrement dynamique et générée selon le JSON.

Exemple d’affichage d’un profil :

ProfileScreen(
    profile = currentProfile,
    onControlEvent = { control, value ->
        websocket.send(control.id, value)
    }
)


Exemple d’un fader :

Slider(
    value = sliderValue,
    onValueChange = {
        sliderValue = it
        onControlEvent(control, it)
    }
)

🧪 Roadmap
⏳ Version Beta

 Boutons et faders 100% fonctionnels

 Multi-profils

 Éditeur de layout sur Android

 Serveur PC avec actions clavier + scripts

🚀 Version 1.0

 Actions OBS complètes

 Encodeurs virtuels

 Import / Export de profils

 Thèmes personnalisés

 Éditeur visuel côté Web

♿ Accessibilité (Android)

- Contrôles Compose avec cible tactile minimale de 48dp, contour de focus visible, rôle TalkBack/VoiceOver et `contentDescription`.
- Navigation clavier/d-pad prise en charge via `focusOrder`, `FocusRequester` et raccourcis flèche/centre.
- Préférences d’accessibilité dans `SettingsScreen` (animations réduites, haptique, police agrandie).
- Couleurs à contraste élevé par défaut pour satisfaire AA/AAA et labels/états exposés dans la hiérarchie de sémantique.
- Vérifier avec l’Inspecteur d’accessibilité Android Studio et corriger tout élément non conforme avant livraison.

🤝 Contributions

Les PRs sont les bienvenues !
Guidelines à venir sous /CONTRIBUTING.md.

📜 Licence

MIT — usage libre, modification et redistribution autorisés.
