# TODO - UI Web Control Deck

## 1. 🐛 Problème de changement de profils

**Fichiers concernés:**

- `android/web-ui/src/pages/Index.tsx` (lignes 100-115, 175-206)
- `android/web-ui/src/hooks/useProfiles.ts`
- `android/web-ui/src/components/ProfileTabs.tsx`

**Problème identifié:**
Le code charge bien les profils depuis le serveur (`useProfiles`), mais il y a plusieurs problèmes potentiels :

1. **Dédoublonnage incomplet** (Index.tsx ligne 249-252): Le filtrage des profils dupliqués pourrait masquer des profils valides
2. **Race condition** entre `setActiveProfileId` et `profiles.loadProfile()` dans `handleProfileChange`
3. **Fallback sur profils par défaut** quand `profileTabs.length === 0` pourrait être déclenché à tort si le chargement est lent

**Actions à faire:**

- [ ] Ajouter un état de chargement (`loading`) visible pendant le fetch des profils
- [ ] Vérifier que `profiles.loadProfile(profileId)` est bien appelé APRÈS `setActiveProfileId`
- [ ] S'assurer que le profil est bien rafraîchi côté serveur (invalider le cache si nécessaire)
- [ ] Ajouter des logs de debug pour tracer le flux de changement de profil

---

## 2. 🔒 Empêcher le zoom sur les pages

**Fichier concerné:**

- `android/web-ui/index.html` (ligne 5)

**Problème:**
Le meta viewport actuel est:

```html
<meta name="viewport" content="width=device-width, initial-scale=1.0" />
```

**Solution:**
Modifier la balise viewport pour désactiver le zoom:

```html
<meta
  name="viewport"
  content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no"
/>
```

**Fichiers additionnels à modifier (CSS):**

- `android/web-ui/src/index.css` ou `android/web-ui/src/App.css`
- Ajouter: `touch-action: manipulation;` sur le body pour éviter le double-tap zoom

---

## 3. 📏 Slider/Fader qui prend toute la place (fonctionnel)

**Fichier concerné:**

- `android/web-ui/src/components/ControlPad.tsx` (lignes 486-570)

**Analyse actuelle:**
Le fader utilise une `minWidth/minHeight` dynamique basée sur `sliderThickness` (ligne 488):

```typescript
const sliderThickness =
  containerWidth > 0
    ? Math.max(6, Math.min(20, Math.round(containerWidth * 0.08)))
    : 10;
```

**Problèmes:**

- Le fader vertical (ligne 553-570) a une largeur fixe trop petite
- Le slider ne profite pas de toute la largeur disponible

**Actions à faire:**

- [ ] Augmenter `minWidth` du fader track vertical pour qu'il occupe plus d'espace
- [ ] Utiliser `w-full` ou une largeur en pourcentage plutôt que `minWidth` fixe
- [ ] Ajuster le ratio `0.08` à `0.15` ou plus pour des sliders plus épais

---

## 4. 👆 Fader plus grand au clic et simple au toucher

**Fichier concerné:**

- `android/web-ui/src/components/ControlPad.tsx` (lignes 204-260, 486-595)

**Analyse actuelle:**

- Le knob/thumb du fader grandit au dragging (`scale: isDragging ? 1.2 : 1`)
- La zone de touch est limitée à la track visible

**Actions à faire:**

- [ ] Agrandir la zone de hit (touch target) avec un padding invisible plus large
- [ ] Augmenter le scale du thumb quand actif (`isDragging ? 1.5 : 1`) pour feedback visuel
- [ ] Ajouter un état `isFocused` pour agrandir le fader dès le premier touch (pas seulement pendant le drag)
- [ ] Augmenter la taille du thumb (actuellement `sliderThickness * 1.2`, passer à `* 1.5` ou `* 2`)
- [ ] Ajouter une zone de sécurité autour du fader pour éviter les clics accidentels

**Suggestion d'amélioration UX:**

```typescript
// Zone de hit élargie (ajouter padding transparent autour de la track)
className = "... p-4 -m-4"; // Padding interne avec marge négative
```

---

## Priorité suggérée

1. **🔴 Haute** - Empêcher zoom (quick fix)
2. **🔴 Haute** - Problème profils (bug fonctionnel)
3. **🟡 Moyenne** - Fader plus grand au toucher
4. **🟡 Moyenne** - Slider pleine largeur
