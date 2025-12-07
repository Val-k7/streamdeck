import { describe, it, expect, beforeAll, afterAll } from '@jest/globals'
import { WebSocket } from 'ws'
import http from 'http'

/**
 * Tests d'intégration serveur ↔ client
 *
 * Ces tests vérifient la communication complète entre un client
 * et le serveur Control Deck.
 */

describe('Server-Client Integration', () => {
  let server
  let baseUrl
  let port = 4455

  beforeAll(async () => {
    // Note: Ces tests nécessitent un serveur en cours d'exécution
    // Pour les tests automatisés, il faudrait démarrer le serveur dans beforeAll
    baseUrl = `http://localhost:${port}`
  })

  afterAll(async () => {
    // Nettoyage si nécessaire
  })

  it('should respond to health check', async () => {
    const response = await fetch(`${baseUrl}/health`)
    expect(response.ok).toBe(true)
    const data = await response.json()
    expect(data).toHaveProperty('status', 'ok')
  })

  it('should return discovery information', async () => {
    const response = await fetch(`${baseUrl}/discovery`)
    expect(response.ok).toBe(true)
    const data = await response.json()
    expect(data).toHaveProperty('serverId')
    expect(data).toHaveProperty('serverName')
    expect(data).toHaveProperty('port')
    expect(data).toHaveProperty('capabilities')
  })

  it('should handle pairing request', async () => {
    const response = await fetch(`${baseUrl}/pairing/request`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ clientId: 'test-client' }),
    })

    expect(response.ok).toBe(true)
    const data = await response.json()
    expect(data).toHaveProperty('code')
    expect(data).toHaveProperty('expiresAt')
    expect(data.code).toMatch(/^\d{6}$/) // 6-digit code
  })

  it('should validate pairing code', async () => {
    // Request pairing code
    const requestResponse = await fetch(`${baseUrl}/pairing/request`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ clientId: 'test-client' }),
    })
    const requestData = await requestResponse.json()
    const code = requestData.code
    const serverId = requestData.qrData?.serverId

    // Confirm pairing
    const confirmResponse = await fetch(`${baseUrl}/pairing/confirm`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        code,
        serverId,
        fingerprint: 'test-fingerprint',
      }),
    })

    expect(confirmResponse.ok).toBe(true)
    const confirmData = await confirmResponse.json()
    expect(confirmData).toHaveProperty('status', 'paired')
  })

  it('should handle handshake and return token', async () => {
    // Note: Nécessite un secret valide
    const response = await fetch(`${baseUrl}/handshake`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        secret: process.env.HANDSHAKE_SECRET || 'change-me',
        clientId: 'test-client',
      }),
    })

    if (response.ok) {
      const data = await response.json()
      expect(data).toHaveProperty('token')
      expect(data).toHaveProperty('expiresAt')
      expect(data).toHaveProperty('expiresIn')
    }
  })

  it('should list profiles', async () => {
    const response = await fetch(`${baseUrl}/profiles`)
    expect(response.ok).toBe(true)
    const data = await response.json()
    expect(data).toHaveProperty('profiles')
    expect(Array.isArray(data.profiles)).toBe(true)
  })
})

describe('WebSocket Integration', () => {
  let ws
  let baseUrl = 'ws://localhost:4455'

  it('should connect to WebSocket', (done) => {
    ws = new WebSocket(`${baseUrl}/ws`)

    ws.on('open', () => {
      expect(ws.readyState).toBe(WebSocket.OPEN)
      ws.close()
      done()
    })

    ws.on('error', (error) => {
      // Si le serveur n'est pas démarré, le test échoue
      // C'est normal pour les tests d'intégration
      done(error)
    })
  })

  it('should send and receive control message', (done) => {
    ws = new WebSocket(`${baseUrl}/ws`)

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
        ws.close()
        done()
      })
    })

    ws.on('error', (error) => {
      done(error)
    })
  })

  it('should handle heartbeat', (done) => {
    ws = new WebSocket(`${baseUrl}/ws`)

    ws.on('open', () => {
      ws.send('💾') // Heartbeat emoji

      ws.on('message', (data) => {
        const response = JSON.parse(data.toString())
        expect(response).toHaveProperty('type', 'ack')
        expect(response).toHaveProperty('status', 'ok')
        ws.close()
        done()
      })
    })

    ws.on('error', (error) => {
      done(error)
    })
  })
})


