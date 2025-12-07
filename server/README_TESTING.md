# Guide de Tests - Control Deck Server

Ce document décrit comment exécuter et écrire des tests pour le serveur Control Deck.

## Installation des Dépendances de Test

```bash
npm install
```

Les dépendances de test sont installées automatiquement avec `npm install`.

## Exécution des Tests

### Tous les tests
```bash
npm test
```

### Tests en mode watch
```bash
npm run test:watch
```

### Tests avec couverture
```bash
npm run test:coverage
```

## Structure des Tests

Les tests sont organisés dans le répertoire `__tests__/` :

```
server/
├── __tests__/
│   ├── utils/
│   │   ├── TokenManager.test.js
│   │   ├── RateLimiter.test.js
│   │   └── PairingManager.test.js
│   ├── actions/
│   │   ├── keyboard.test.js
│   │   └── ...
│   └── plugins/
│       └── ...
```

## Écriture de Tests

### Exemple de test

```javascript
import { describe, it, expect, beforeEach } from '@jest/globals'
import { getTokenManager } from '../../utils/TokenManager.js'

describe('TokenManager', () => {
  let tokenManager

  beforeEach(() => {
    tokenManager = getTokenManager(24 * 60 * 60 * 1000)
  })

  it('should issue a new token', () => {
    const tokenData = tokenManager.issueToken('client-1')
    expect(tokenData).toHaveProperty('token')
    expect(tokenData.token).toBeTruthy()
  })
})
```

## Couverture de Code

L'objectif est d'atteindre **80% de couverture** pour les modules critiques :

- `utils/` : 80% minimum
- `actions/` : 80% minimum
- `plugins/` : 70% minimum

### Vérifier la couverture

```bash
npm run test:coverage
```

Le rapport de couverture sera généré dans `coverage/`.

## Tests d'Intégration

Les tests d'intégration vérifient le comportement de plusieurs composants ensemble.

### Exemple de test d'intégration

```javascript
import { describe, it, expect } from '@jest/globals'
import request from 'supertest'
import app from '../index.js'

describe('API Integration', () => {
  it('should return health status', async () => {
    const response = await request(app)
      .get('/health')
      .expect(200)

    expect(response.body).toHaveProperty('status', 'ok')
  })
})
```

## Mocking

Utilisez `jest.mock()` pour mocker les dépendances externes :

```javascript
jest.mock('child_process')
jest.mock('fs')
```

## Tests Asynchrones

Utilisez `async/await` pour les tests asynchrones :

```javascript
it('should handle async operation', async () => {
  const result = await asyncFunction()
  expect(result).toBeDefined()
})
```

## Bonnes Pratiques

1. **Un test = une assertion principale**
2. **Nommer les tests de manière descriptive**
3. **Utiliser `beforeEach` pour la configuration**
4. **Nettoyer après chaque test**
5. **Tester les cas d'erreur**
6. **Tester les cas limites**

## Dépendances de Test

- **Jest** : Framework de test
- **Supertest** : Tests HTTP
- **@jest/globals** : Types pour Jest en ESM

## Configuration Jest

La configuration Jest est dans `package.json` :

```json
{
  "jest": {
    "testEnvironment": "node",
    "testMatch": ["**/__tests__/**/*.js"],
    "collectCoverageFrom": ["utils/**/*.js", "actions/**/*.js"]
  }
}
```

## Troubleshooting

### Erreur "Cannot use import statement outside a module"

Assurez-vous que `package.json` contient `"type": "module"` et utilisez `--experimental-vm-modules` avec Jest.

### Tests qui échouent de manière intermittente

Vérifiez les timeouts et les opérations asynchrones. Utilisez `jest.setTimeout()` si nécessaire.

### Mocking ne fonctionne pas

Assurez-vous que le mock est défini avant l'import du module à tester.


