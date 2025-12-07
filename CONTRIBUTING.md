# Guide de contribution - Control Deck

Merci de votre intérêt pour contribuer à Control Deck ! Ce document fournit des directives pour contribuer au projet.

## Table des matières

1. [Code de conduite](#code-de-conduite)
2. [Comment contribuer](#comment-contribuer)
3. [Processus de développement](#processus-de-développement)
4. [Standards de code](#standards-de-code)
5. [Tests](#tests)
6. [Documentation](#documentation)
7. [Soumission de pull requests](#soumission-de-pull-requests)

## Code de conduite

En participant à ce projet, vous acceptez de respecter notre code de conduite. Soyez respectueux, inclusif et professionnel.

## Comment contribuer

### Signaler un bug

1. Vérifiez que le bug n'a pas déjà été signalé dans les issues
2. Créez une nouvelle issue avec :
   - Un titre clair et descriptif
   - Une description détaillée du problème
   - Les étapes pour reproduire
   - Le comportement attendu vs le comportement actuel
   - Votre environnement (OS, version, etc.)

### Proposer une fonctionnalité

1. Vérifiez que la fonctionnalité n'a pas déjà été proposée
2. Créez une nouvelle issue avec :
   - Un titre clair et descriptif
   - Une description détaillée de la fonctionnalité
   - Le cas d'usage et la valeur ajoutée
   - Des exemples d'utilisation si possible

### Contribuer au code

1. Fork le projet
2. Créez une branche pour votre fonctionnalité (`git checkout -b feature/ma-fonctionnalite`)
3. Committez vos changements (`git commit -m 'Ajout de ma fonctionnalité'`)
4. Push vers la branche (`git push origin feature/ma-fonctionnalite`)
5. Ouvrez une Pull Request

## Processus de développement

### Structure du projet

```
android/
├── app/                    # Application Android
│   └── src/main/java/     # Code Kotlin
└── docs/                   # Documentation

server/                     # Serveur Node.js (à la racine)
├── actions/               # Actions intégrées
├── plugins/                # Système de plugins
└── utils/                  # Utilitaires

web/                       # Interface web React (à la racine)
├── src/                   # Code source
└── dist/                  # Build de production
```

### Workflow Git

1. **Branches** :
   - `main` : Code stable et testé
   - `develop` : Branche de développement
   - `feature/*` : Nouvelles fonctionnalités
   - `fix/*` : Corrections de bugs
   - `docs/*` : Documentation

2. **Commits** :
   - Utilisez des messages clairs et descriptifs
   - Format : `type: description`
   - Types : `feat`, `fix`, `docs`, `style`, `refactor`, `test`, `chore`

### Développement local

#### Serveur

```bash
cd server
npm install
npm run dev
```

#### Application Android

1. Ouvrez le projet dans Android Studio
2. Configurez un émulateur ou connectez un appareil
3. Lancez l'application

## Standards de code

### JavaScript/Node.js

- Utilisez ES6+ (modules, async/await, etc.)
- Suivez les conventions ESLint
- Commentez le code complexe
- Utilisez des noms de variables descriptifs
- Gestion d'erreurs appropriée

### Kotlin/Android

- Suivez les conventions Kotlin
- Utilisez les fonctionnalités modernes (coroutines, Flow, Compose)
- Respectez les guidelines Material Design
- Gestion d'erreurs avec try-catch appropriés
- Documentation KDoc pour les fonctions publiques

### Formatage

- JavaScript : Prettier avec configuration par défaut
- Kotlin : ktlint avec configuration par défaut
- Formatage automatique avant commit

## Tests

### Tests unitaires

- Écrivez des tests pour toutes les nouvelles fonctionnalités
- Maintenez une couverture de code > 80%
- Utilisez des noms de tests descriptifs

### Tests d'intégration

- Testez les interactions entre composants
- Testez les scénarios d'utilisation réels
- Vérifiez la compatibilité cross-platform

### Exécution des tests

```bash
# Serveur
cd server
npm test

# Android
# Exécutez les tests dans Android Studio
```

## Documentation

### Code

- Documentez toutes les fonctions publiques
- Ajoutez des commentaires pour le code complexe
- Maintenez la documentation à jour

### Utilisateur

- Mettez à jour le README si nécessaire
- Ajoutez des exemples d'utilisation
- Documentez les changements breaking

### Développeur

- Documentez l'architecture
- Expliquez les décisions de design
- Fournissez des guides pour les plugins

## Soumission de pull requests

### Avant de soumettre

1. ✅ Votre code compile sans erreurs
2. ✅ Les tests passent
3. ✅ Le code est formaté
4. ✅ La documentation est à jour
5. ✅ Vous avez testé manuellement

### Template de PR

```markdown
## Description
Brève description des changements

## Type de changement
- [ ] Bug fix
- [ ] Nouvelle fonctionnalité
- [ ] Breaking change
- [ ] Documentation

## Tests
Description des tests effectués

## Checklist
- [ ] Code compilé sans erreurs
- [ ] Tests passent
- [ ] Documentation mise à jour
- [ ] Pas de warnings
```

### Review process

1. Un mainteneur examinera votre PR
2. Des commentaires peuvent être demandés
3. Une fois approuvée, la PR sera mergée

## Questions ?

N'hésitez pas à :
- Ouvrir une issue pour poser une question
- Rejoindre les discussions
- Contacter les mainteneurs

Merci de contribuer à Control Deck ! 🎉
