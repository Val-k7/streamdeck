/**
 * Tests E2E - Scénarios Utilisateur Complets
 */

import { describe, it, expect, beforeAll, afterAll } from '@jest/globals'
import WebSocket from 'ws'
import fetch from 'node-fetch'

const serverUrl = process.env.SERVER_URL || 'http://localhost:4455'
const wsUrl = process.env.WS_URL || 'ws://localhost:4455'
const handshakeSecret = process.env.HANDSHAKE_SECRET

describe('User Scenarios E2E', () => {
  let ws
  let token

  beforeAll(async () => {
    if (!handshakeSecret) {
      console.warn('HANDSHAKE_SECRET not provided, WS tests will be skipped')
      return
    }

    const response = await fetch(
      `${serverUrl}/tokens/handshake?secret=${encodeURIComponent(handshakeSecret)}&clientId=e2e-user-scenarios`,
      { method: 'POST' }
    )
    if (response.ok) {
      token = (await response.json()).token
      ws = new WebSocket(`${wsUrl}/ws`, {
        headers: { Authorization: `Bearer ${token}` },
      })
      await new Promise((resolve) => {
        ws.on('open', resolve)
        ws.on('error', resolve)
      })
    }
  })

  afterAll(() => {
    if (ws && ws.readyState === WebSocket.OPEN) {
      ws.close()
    }
  })

  describe('Scénario: Premier Lancement', () => {
    it('should complete first launch workflow', async () => {
      const discoveryResponse = await fetch(`${serverUrl}/discovery/`)
      expect(discoveryResponse.ok).toBe(true)
      const discoveryData = await discoveryResponse.json()
      expect(discoveryData).toHaveProperty('serverId')

      const pairingRequestResponse = await fetch(`${serverUrl}/discovery/pairing/request`, {
        method: 'POST',
      })
      expect(pairingRequestResponse.ok).toBe(true)
      const pairingData = await pairingRequestResponse.json()
      expect(pairingData).toHaveProperty('code')

      const pairingConfirmResponse = await fetch(
        `${serverUrl}/discovery/pairing/confirm?code=${pairingData.code}&serverId=${discoveryData.serverId}`,
        { method: 'POST' }
      )
      expect(pairingConfirmResponse.ok).toBe(true)
      const confirmData = await pairingConfirmResponse.json()
      expect(confirmData).toHaveProperty('status', 'paired')
    })
  })

  describe('Scénario: Création et Édition de Profil', () => {
    it('should create and update profile via REST', async () => {
      const payload = {
        id: 'e2e-edit-profile',
        name: 'E2E Edit Profile',
        rows: 3,
        cols: 5,
        controls: [],
      }

      const save = await fetch(`${serverUrl}/profiles/e2e-edit-profile`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload),
      })
      expect(save.ok).toBe(true)

      const updated = await fetch(`${serverUrl}/profiles/e2e-edit-profile`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ ...payload, name: 'Updated E2E Profile' }),
      })
      expect(updated.ok).toBe(true)
    })
  })

  describe('Scénario: Import/Export de Profil', () => {
    it('should export and import a profile', async () => {
      const createResponse = await fetch(`${serverUrl}/profiles/e2e-export-profile`, {
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

      const exportResponse = await fetch(`${serverUrl}/profiles/e2e-export-profile`)
      expect(exportResponse.ok).toBe(true)
      const exportedProfile = await exportResponse.json()
      expect(exportedProfile).toHaveProperty('id')

      const importResponse = await fetch(`${serverUrl}/profiles/e2e-imported-profile`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ ...exportedProfile, id: 'e2e-imported-profile', name: 'Imported Profile' }),
      })
      expect(importResponse.ok).toBe(true)
    })
  })

  describe('Scénario: Actions WebSocket simples', () => {
    const wsTest = token ? it : it.skip

    wsTest('should send processes action', (done) => {
      const action = {
        action: 'processes',
        payload: { limit: 1 },
        messageId: `msg-${Date.now()}`,
      }

      ws.send(JSON.stringify(action))

      ws.once('message', (data) => {
        const response = JSON.parse(data.toString())
        expect(response.type).toBe('ack')
        done()
      })
    })
  })
})


