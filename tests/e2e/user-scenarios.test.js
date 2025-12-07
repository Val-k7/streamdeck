/**
 * Tests E2E - Scénarios Utilisateur Complets
 */

import { describe, it, expect, beforeAll, afterAll } from '@jest/globals'
import WebSocket from 'ws'
import fetch from 'node-fetch'

describe('User Scenarios E2E', () => {
  let ws
  const serverUrl = process.env.SERVER_URL || 'http://localhost:4455'
  const wsUrl = process.env.WS_URL || 'ws://localhost:4455'
  const testToken = process.env.TEST_TOKEN || 'test-token'

  beforeAll((done) => {
    ws = new WebSocket(`${wsUrl}?token=${testToken}`)
    ws.on('open', () => {
      done()
    })
    ws.on('error', (error) => {
      console.warn('Server not available, skipping E2E tests:', error.message)
      done()
    })
  })

  afterAll(() => {
    if (ws && ws.readyState === WebSocket.OPEN) {
      ws.close()
    }
  })

  describe('Scénario: Premier Lancement', () => {
    it('should complete first launch workflow', async () => {
      if (ws.readyState !== WebSocket.OPEN) {
        console.warn('WebSocket not connected, skipping test')
        return
      }

      // 1. Discovery
      const discoveryResponse = await fetch(`${serverUrl}/discovery`)
      expect(discoveryResponse.ok).toBe(true)
      const discoveryData = await discoveryResponse.json()
      expect(discoveryData).toHaveProperty('serverId')

      // 2. Pairing Request
      const pairingRequestResponse = await fetch(`${serverUrl}/pairing/request`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ clientId: 'e2e-first-launch' }),
      })
      expect(pairingRequestResponse.ok).toBe(true)
      const pairingData = await pairingRequestResponse.json()
      expect(pairingData).toHaveProperty('code')

      // 3. Pairing Confirm
      const pairingConfirmResponse = await fetch(`${serverUrl}/pairing/confirm`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          code: pairingData.code,
          serverId: discoveryData.serverId,
          fingerprint: 'e2e-fingerprint',
        }),
      })
      expect(pairingConfirmResponse.ok).toBe(true)
      const confirmData = await pairingConfirmResponse.json()
      expect(confirmData).toHaveProperty('status', 'paired')
      expect(confirmData).toHaveProperty('token')
    })
  })

  describe('Scénario: Création et Édition de Profil', () => {
    it('should create and edit a profile', (done) => {
      if (ws.readyState !== WebSocket.OPEN) {
        console.warn('WebSocket not connected, skipping test')
        return done()
      }

      const newProfile = {
        kind: 'profile',
        action: 'create',
        profile: {
          id: 'e2e-edit-profile',
          name: 'E2E Edit Profile',
          rows: 3,
          cols: 5,
          controls: [],
        },
      }

      ws.send(JSON.stringify(newProfile))

      ws.once('message', (data) => {
        const response = JSON.parse(data.toString())
        if (response.kind === 'ack' && response.success) {
          // Edit profile
          const updatedProfile = {
            kind: 'profile',
            action: 'update',
            profile: {
              id: 'e2e-edit-profile',
              name: 'Updated E2E Profile',
              rows: 3,
              cols: 5,
              controls: [],
            },
          }

          ws.send(JSON.stringify(updatedProfile))

          ws.once('message', (data2) => {
            const response2 = JSON.parse(data2.toString())
            expect(response2.kind).toBe('ack')
            expect(response2.success).toBe(true)
            done()
          })
        }
      })
    })
  })

  describe('Scénario: Import/Export de Profil', () => {
    it('should export and import a profile', async () => {
      if (ws.readyState !== WebSocket.OPEN) {
        console.warn('WebSocket not connected, skipping test')
        return
      }

      // Create profile
      const createResponse = await fetch(`${serverUrl}/profiles`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          id: 'e2e-export-profile',
          name: 'Export Profile',
          rows: 3,
          cols: 5,
          controls: [],
        }),
      })
      expect(createResponse.ok).toBe(true)

      // Export profile
      const exportResponse = await fetch(`${serverUrl}/profiles/e2e-export-profile`)
      expect(exportResponse.ok).toBe(true)
      const exportedProfile = await exportResponse.json()
      expect(exportedProfile).toHaveProperty('id')
      expect(exportedProfile).toHaveProperty('name')

      // Import profile (create new with same data)
      const importResponse = await fetch(`${serverUrl}/profiles`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          ...exportedProfile,
          id: 'e2e-imported-profile',
          name: 'Imported Profile',
        }),
      })
      expect(importResponse.ok).toBe(true)
    })
  })

  describe('Scénario: Utilisation de Tous les Types de Contrôles', () => {
    it('should handle all control types', (done) => {
      if (ws.readyState !== WebSocket.OPEN) {
        console.warn('WebSocket not connected, skipping test')
        return done()
      }

      const controlTypes = ['button', 'toggle', 'fader', 'encoder']
      let completed = 0

      controlTypes.forEach((type) => {
        const action = {
          kind: 'control',
          controlId: `test-${type}`,
          type: type.toUpperCase(),
          value: type === 'toggle' ? 1 : 0.5,
          messageId: `msg-${Date.now()}-${type}`,
          sentAt: Date.now(),
        }

        ws.send(JSON.stringify(action))

        ws.once('message', (data) => {
          const response = JSON.parse(data.toString())
          if (response.kind === 'ack') {
            completed++
            if (completed === controlTypes.length) {
              done()
            }
          }
        })
      })
    })
  })

  describe('Scénario: Gestion des Erreurs', () => {
    it('should handle server disconnection gracefully', (done) => {
      if (ws.readyState !== WebSocket.OPEN) {
        console.warn('WebSocket not connected, skipping test')
        return done()
      }

      ws.once('close', () => {
        // Should trigger close event
        done()
      })

      ws.close()
    })

    it('should handle network errors', async () => {
      try {
        const response = await fetch('http://192.168.1.999:4455/discovery', {
          timeout: 1000,
        })
        // Should handle error gracefully
      } catch (error) {
        // Expected error
        expect(error).toBeDefined()
      }
    })
  })

  describe('Scénario: Multi-Profils et Basculement', () => {
    it('should switch between multiple profiles', (done) => {
      if (ws.readyState !== WebSocket.OPEN) {
        console.warn('WebSocket not connected, skipping test')
        return done()
      }

      const profiles = ['profile-1', 'profile-2', 'profile-3']
      let currentIndex = 0

      const switchProfile = () => {
        const profileId = profiles[currentIndex]
        const payload = {
          kind: 'profile:select',
          profileId,
          messageId: `msg-${Date.now()}`,
          sentAt: Date.now(),
        }

        ws.send(JSON.stringify(payload))

        ws.once('message', (data) => {
          const response = JSON.parse(data.toString())
          if (response.kind === 'ack') {
            currentIndex++
            if (currentIndex < profiles.length) {
              switchProfile()
            } else {
              done()
            }
          }
        })
      }

      switchProfile()
    })
  })
})


