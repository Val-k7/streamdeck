/**
 * Tests End-to-End - Scénarios de base
 *
 * Ces tests nécessitent un serveur en cours d'exécution et un client Android simulé
 */

import { describe, it, expect, beforeAll, afterAll } from '@jest/globals'
import { WebSocket } from 'ws'
import fetch from 'node-fetch'

const SERVER_URL = process.env.SERVER_URL || 'http://localhost:4455'
const WS_URL = process.env.WS_URL || 'ws://localhost:4455'

describe('E2E - Scénarios Utilisateur', () => {
  let token = null

  beforeAll(async () => {
    // Obtenir un token pour les tests
    try {
      const response = await fetch(`${SERVER_URL}/handshake`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          secret: process.env.HANDSHAKE_SECRET || 'change-me',
          clientId: 'e2e-test-client',
        }),
      })
      if (response.ok) {
        const data = await response.json()
        token = data.token
      }
    } catch (e) {
      console.warn('Could not get token for E2E tests:', e.message)
    }
  })

  describe('Scénario 1: Premier Lancement', () => {
    it('should discover server', async () => {
      const response = await fetch(`${SERVER_URL}/discovery`)
      expect(response.ok).toBe(true)

      const data = await response.json()
      expect(data).toHaveProperty('serverId')
      expect(data).toHaveProperty('serverName')
    })

    it('should request pairing code', async () => {
      const response = await fetch(`${SERVER_URL}/pairing/request`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ clientId: 'e2e-client' }),
      })

      expect(response.ok).toBe(true)
      const data = await response.json()
      expect(data).toHaveProperty('code')
      expect(data.code).toMatch(/^\d{6}$/)
    })

    it('should complete pairing and connect', async () => {
      // Request pairing
      const requestResponse = await fetch(`${SERVER_URL}/pairing/request`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ clientId: 'e2e-client' }),
      })
      const requestData = await requestResponse.json()
      const code = requestData.code
      const serverId = requestData.qrData?.serverId

      // Confirm pairing
      const confirmResponse = await fetch(`${SERVER_URL}/pairing/confirm`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          code,
          serverId,
          fingerprint: 'e2e-fingerprint',
        }),
      })

      expect(confirmResponse.ok).toBe(true)
      const confirmData = await confirmResponse.json()
      expect(confirmData).toHaveProperty('status', 'paired')
      expect(confirmData).toHaveProperty('token')
    })
  })

  describe('Scénario 2: Utilisation Basique', () => {
    it('should list available profiles', async () => {
      const response = await fetch(`${SERVER_URL}/profiles`)
      expect(response.ok).toBe(true)

      const data = await response.json()
      expect(data).toHaveProperty('profiles')
      expect(Array.isArray(data.profiles)).toBe(true)
    })

    it('should select a profile via WebSocket', (done) => {
      const ws = new WebSocket(`${WS_URL}/ws`)

      ws.on('open', () => {
        const payload = {
          kind: 'profile:select',
          profileId: 'default',
          messageId: `e2e-${Date.now()}`,
          sentAt: Date.now(),
        }

        ws.send(JSON.stringify(payload))

        ws.on('message', (data) => {
          const response = JSON.parse(data.toString())
          if (response.type === 'ack' && response.kind === 'profile:select:ack') {
            expect(response.status).toBe('ok')
            ws.close()
            done()
          }
        })
      })

      ws.on('error', (error) => {
        done(error)
      })
    })

    it('should execute a button action', (done) => {
      const ws = new WebSocket(`${WS_URL}/ws`)

      ws.on('open', () => {
        const payload = {
          kind: 'control',
          controlId: 'test-button',
          type: 'BUTTON',
          value: 1.0,
          messageId: `e2e-${Date.now()}`,
          sentAt: Date.now(),
        }

        ws.send(JSON.stringify(payload))

        ws.on('message', (data) => {
          const response = JSON.parse(data.toString())
          if (response.type === 'ack' && response.controlId === 'test-button') {
            expect(response.status).toBe('ok')
            ws.close()
            done()
          }
        })
      })

      ws.on('error', (error) => {
        done(error)
      })
    })
  })

  describe('Scénario 3: Gestion d\'Erreurs', () => {
    it('should handle server disconnection', (done) => {
      const ws = new WebSocket(`${WS_URL}/ws`)

      ws.on('open', () => {
        ws.close()

        // Should trigger close event
        ws.on('close', () => {
          done()
        })
      })

      ws.on('error', (error) => {
        done(error)
      })
    })

    it('should handle invalid action gracefully', (done) => {
      const ws = new WebSocket(`${WS_URL}/ws`)

      ws.on('open', () => {
        const payload = {
          kind: 'control',
          controlId: 'invalid',
          type: 'INVALID_TYPE',
          value: 1.0,
          messageId: `e2e-${Date.now()}`,
          sentAt: Date.now(),
        }

        ws.send(JSON.stringify(payload))

        ws.on('message', (data) => {
          const response = JSON.parse(data.toString())
          if (response.type === 'ack' && response.controlId === 'invalid') {
            // Should return error status
            expect(['error', 'ignored']).toContain(response.status)
            ws.close()
            done()
          }
        })
      })

      ws.on('error', (error) => {
        done(error)
      })
    })
  })
})


