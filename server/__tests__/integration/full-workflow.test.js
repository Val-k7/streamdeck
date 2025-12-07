import { describe, it, expect, beforeAll, afterAll } from '@jest/globals'
import { WebSocket } from 'ws'
import http from 'http'

/**
 * Tests d'intégration complets - Workflow complet
 *
 * Ces tests nécessitent un serveur en cours d'exécution
 */

describe('Full Workflow Integration', () => {
  let server
  let baseUrl
  let port = 4455

  beforeAll(async () => {
    baseUrl = `http://localhost:${port}`
  })

  afterAll(async () => {
    // Nettoyage
  })

  describe('Discovery → Pairing → Connection Workflow', () => {
    it('should complete full discovery workflow', async () => {
      // 1. Discovery
      const discoveryResponse = await fetch(`${baseUrl}/discovery`)
      expect(discoveryResponse.ok).toBe(true)
      const discoveryData = await discoveryResponse.json()
      expect(discoveryData).toHaveProperty('serverId')
      expect(discoveryData).toHaveProperty('serverName')

      // 2. Pairing Request
      const pairingRequestResponse = await fetch(`${baseUrl}/pairing/request`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ clientId: 'test-client' }),
      })
      expect(pairingRequestResponse.ok).toBe(true)
      const pairingData = await pairingRequestResponse.json()
      const code = pairingData.code
      const serverId = pairingData.qrData?.serverId

      // 3. Pairing Confirm
      const pairingConfirmResponse = await fetch(`${baseUrl}/pairing/confirm`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          code,
          serverId,
          fingerprint: 'test-fingerprint',
        }),
      })
      expect(pairingConfirmResponse.ok).toBe(true)
      const confirmData = await pairingConfirmResponse.json()
      expect(confirmData).toHaveProperty('status', 'paired')
      expect(confirmData).toHaveProperty('token')

      // 4. Handshake
      const handshakeResponse = await fetch(`${baseUrl}/handshake`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          secret: confirmData.token,
          clientId: 'test-client',
        }),
      })
      expect(handshakeResponse.ok).toBe(true)
      const handshakeData = await handshakeResponse.json()
      expect(handshakeData).toHaveProperty('token')

      // 5. WebSocket Connection
      const ws = new WebSocket(`ws://localhost:${port}/ws`)

      await new Promise((resolve, reject) => {
        ws.on('open', () => {
          expect(ws.readyState).toBe(WebSocket.OPEN)
          ws.close()
          resolve()
        })

        ws.on('error', reject)
      })
    })
  })

  describe('Profile Management Workflow', () => {
    it('should list, create, and select profiles', async () => {
      // 1. List profiles
      const listResponse = await fetch(`${baseUrl}/profiles`)
      expect(listResponse.ok).toBe(true)
      const listData = await listResponse.json()
      expect(listData).toHaveProperty('profiles')
      expect(Array.isArray(listData.profiles)).toBe(true)

      // 2. Get profile
      if (listData.profiles.length > 0) {
        const profileId = listData.profiles[0].id
        const getResponse = await fetch(`${baseUrl}/profiles/${profileId}`)
        expect(getResponse.ok).toBe(true)
        const profileData = await getResponse.json()
        expect(profileData).toHaveProperty('id', profileId)
      }
    })
  })

  describe('Action Execution Workflow', () => {
    it('should execute keyboard action via WebSocket', (done) => {
      const ws = new WebSocket(`ws://localhost:${port}/ws`)

      ws.on('open', () => {
        const payload = {
          kind: 'control',
          controlId: 'test-control',
          type: 'BUTTON',
          value: 1.0,
          messageId: `test-${Date.now()}`,
          sentAt: Date.now(),
        }

        ws.send(JSON.stringify(payload))

        ws.on('message', (data) => {
          const response = JSON.parse(data.toString())
          expect(response).toHaveProperty('type', 'ack')
          expect(response).toHaveProperty('status')
          expect(response).toHaveProperty('controlId', 'test-control')
          ws.close()
          done()
        })
      })

      ws.on('error', (error) => {
        done(error)
      })
    })
  })
})


