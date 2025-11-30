# Plan de tests utilisateurs

## Objectifs
- Vérifier que le tutoriel interactif est compris et utilisé pour configurer le réseau.
- Valider l'utilisabilité du centre d'aide/FAQ et du flux de feedback avec capture de logs optionnelle.
- Mesurer la fluidité de navigation (profil → paramètres → support) et la perception de la latence réseau.

## Personas ciblés
1. Nouvel utilisateur (aucune expérience réseau) avec accès Wi-Fi domestique.
2. Utilisateur avancé (admin/ingé son) habitué aux pare-feux/ports réseau.
3. Utilisateur mobile (VPN ou réseau d'entreprise) pour tester les blocages potentiels.

## Scénarios principaux
1. **Première connexion** : lancement de l'app → lecture de l'overlay → vérification réseau → ouverture des paramètres → connexion au serveur.
2. **Assistance** : consultation de la FAQ et des conseils pare-feu → ouverture du lien de support.
3. **Feedback** : envoi d'un rapport avec note + commentaire, avec et sans logs.
4. **Fiabilité réseau** : déconnexion/reconnexion Wi-Fi ou VPN pendant l'overlay puis dans le centre d'aide pour vérifier les messages d'état.

## Indicateurs à mesurer
- Temps pour compléter l'onboarding (objectif : < 3 minutes).
- Nombre de retours à la FAQ avant de réussir la connexion.
- Clarté du feedback (note + commentaire) et volonté d'inclure les logs.
- Perception de la latence après ajustement des paramètres.

## Script de session (30–35 min)
1. **Brief (3 min)** : présenter l'objectif (connexion et prise en main), rappeler la capture volontaire de logs.
2. **Onboarding (8 min)** : laisser l'utilisateur suivre l'overlay, vérifier le réseau et ouvrir les paramètres ; noter les blocages.
3. **Aide (8 min)** : demander de résoudre un problème simulé de port bloqué via le centre d'aide et d'ouvrir un lien de support.
4. **Feedback (6 min)** : demander une note et un commentaire, d'abord sans logs puis avec logs activés.
5. **Variations réseau (5 min)** : couper/restaurer le Wi-Fi ou activer un VPN et observer les messages d'état.
6. **Debrief (5 min)** : recueillir les points de friction et propositions d'amélioration.

## Logistique
- Appareil : smartphone Android connecté au même réseau que le serveur de test.
- Enregistrement : capture d'écran et notes d'observation ; logs supplémentaires via l'option intégrée.
- Questionnaire post-session : SUS abrégé + question ouverte sur la clarté des messages réseau.
