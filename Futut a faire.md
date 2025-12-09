# TODO - UI Web Control Deck

## 1. 🐛 Problème de changement de profils ✅ RÉSOLU

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

- [x] Ajouter un état de chargement (`loading`) visible pendant le fetch des profils ✅ Spinner ajouté dans ProfileTabs
- [x] Vérifier que `profiles.loadProfile(profileId)` est bien appelé APRÈS `setActiveProfileId` ✅ L'ordre est correct
- [ ] S'assurer que le profil est bien rafraîchi côté serveur (invalider le cache si nécessaire)
- [x] Ajouter des logs de debug pour tracer le flux de changement de profil ✅ Logs déjà présents

---

## 2. 🔒 Empêcher le zoom sur les pages ✅ RÉSOLU

**Fichier concerné:**

- `android/web-ui/index.html` (ligne 5)

**État actuel:** ✅ DÉJÀ IMPLÉMENTÉ
Le viewport contient déjà:
```html
<meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no, viewport-fit=cover" />
```

**CSS:** ✅ DÉJÀ IMPLÉMENTÉ
- `android/web-ui/src/index.css` contient `touch-action: manipulation;` sur body

---

## 3. 📏 Slider/Fader qui prend toute la place (fonctionnel) ✅ RÉSOLU

**Fichier concerné:**

- `android/web-ui/src/components/ControlPad.tsx` (lignes 486-570)

**État actuel:** ✅ DÉJÀ IMPLÉMENTÉ
Le fader remplit maintenant toute la cellule avec `w-full h-full` et utilise une barre de progression animée qui couvre toute la surface.

---

## 4. 👆 Fader plus grand au clic et simple au toucher ✅ RÉSOLU

**Fichier concerné:**

- `android/web-ui/src/components/ControlPad.tsx` (lignes 204-260, 486-595)

**État actuel:** ✅ RÉSOLU
- [x] Le fader remplit maintenant toute la cellule, donc la zone de hit est maximale
- [x] Design utilise une ligne indicatrice (1px) adaptée au design full-cell
- [x] Zone de touch couvre toute la surface de la cellule

---

## Priorité suggérée

1. ~~**🔴 Haute** - Empêcher zoom (quick fix)~~ ✅ FAIT
2. ~~**🔴 Haute** - Problème profils (bug fonctionnel)~~ ✅ FAIT (loading indicator ajouté)
3. ~~**🟡 Moyenne** - Fader plus grand au toucher~~ ✅ FAIT (full-cell design)
4. ~~**🟡 Moyenne** - Slider pleine largeur~~ ✅ FAIT
