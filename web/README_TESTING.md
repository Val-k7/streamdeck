# Guide de Tests - Control Deck Web

Ce document décrit comment exécuter et écrire des tests pour l'application web React.

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

### Interface UI pour les tests
```bash
npm run test:ui
```

## Structure des Tests

Les tests sont organisés dans des fichiers `__tests__/` à côté des composants :

```
web/src/
├── components/
│   ├── __tests__/
│   │   ├── ControlPad.test.tsx
│   │   ├── DeckGrid.test.tsx
│   │   └── ConnectionIndicator.test.tsx
│   ├── ControlPad.tsx
│   └── ...
├── hooks/
│   ├── __tests__/
│   │   └── useWebSocket.test.ts
│   └── useWebSocket.ts
└── test/
    ├── setup.ts
    └── utils/
        └── test-utils.tsx
```

## Écriture de Tests

### Exemple de test de composant

```typescript
import { describe, it, expect } from 'vitest'
import { render, screen, fireEvent } from '../../test/utils/test-utils'
import MyComponent from '../MyComponent'

describe('MyComponent', () => {
  it('should render correctly', () => {
    render(<MyComponent />)
    expect(screen.getByText('Hello')).toBeInTheDocument()
  })

  it('should handle user interaction', () => {
    const handleClick = vi.fn()
    render(<MyComponent onClick={handleClick} />)
    
    fireEvent.click(screen.getByRole('button'))
    expect(handleClick).toHaveBeenCalledTimes(1)
  })
})
```

### Exemple de test de hook

```typescript
import { describe, it, expect } from 'vitest'
import { renderHook } from '@testing-library/react'
import useMyHook from '../useMyHook'

describe('useMyHook', () => {
  it('should return initial value', () => {
    const { result } = renderHook(() => useMyHook())
    expect(result.current.value).toBe(0)
  })
})
```

## Mocking

### Mocker un hook

```typescript
vi.mock('../hooks/useWebSocket', () => ({
  default: () => ({
    isConnected: true,
    sendMessage: vi.fn(),
  }),
}))
```

### Mocker un module

```typescript
vi.mock('../lib/api', () => ({
  fetchData: vi.fn().mockResolvedValue({ data: 'test' }),
}))
```

## Configuration Vitest

La configuration est dans `vitest.config.ts` :

```typescript
export default defineConfig({
  test: {
    globals: true,
    environment: 'jsdom',
    setupFiles: ['./src/test/setup.ts'],
    coverage: {
      thresholds: {
        lines: 70,
        functions: 70,
        branches: 70,
        statements: 70,
      },
    },
  },
})
```

## Couverture de Code

L'objectif est d'atteindre **70% de couverture** pour les composants critiques :

- Composants UI : 70% minimum
- Hooks : 70% minimum
- Utilitaires : 80% minimum

### Vérifier la couverture

```bash
npm run test:coverage
```

Le rapport de couverture sera généré dans `coverage/`.

## Bonnes Pratiques

1. **Tester le comportement, pas l'implémentation**
2. **Utiliser des queries accessibles** (getByRole, getByLabelText)
3. **Tester les cas d'erreur**
4. **Tester les interactions utilisateur**
5. **Isoler les tests** (chaque test doit être indépendant)
6. **Nommer les tests de manière descriptive**

## Utilitaires de Test

### test-utils.tsx

Fournit une fonction `render` personnalisée avec tous les providers nécessaires (QueryClient, etc.).

### setup.ts

Configure l'environnement de test :
- Jest DOM matchers
- Mocks globaux (WebSocket, matchMedia)
- Cleanup automatique

## Dépendances de Test

- **Vitest** : Framework de test (compatible Vite)
- **@testing-library/react** : Utilitaires pour tester React
- **@testing-library/jest-dom** : Matchers DOM
- **@testing-library/user-event** : Simulation d'événements utilisateur
- **jsdom** : Environnement DOM pour les tests
- **@vitest/coverage-v8** : Couverture de code

## Troubleshooting

### Erreur "Cannot find module"

Vérifiez que les alias de chemin sont correctement configurés dans `vitest.config.ts`.

### Tests qui échouent de manière intermittente

Utilisez `waitFor` pour les opérations asynchrones :

```typescript
await waitFor(() => {
  expect(screen.getByText('Loaded')).toBeInTheDocument()
})
```

### Mocking ne fonctionne pas

Assurez-vous que le mock est défini avant l'import du module à tester.


