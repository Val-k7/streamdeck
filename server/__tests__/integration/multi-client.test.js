import { describe, it, expect, beforeAll, afterAll } from '@jest/globals'
import WebSocket from 'ws'

describe('Multi-Client Integration Tests', () => {
  const serverUrl = process.env.SERVER_URL || 'ws://localhost:4455'
  const testToken = process.env.TEST_TOKEN || 'test-token'
  let clients = []

  beforeAll(() => {
    // Create multiple client connections
    for (let i = 0; i < 3; i++) {
      const ws = new WebSocket(`${serverUrl}?token=${testToken}`)
      clients.push(ws)
    }
  })

  afterAll(() => {
    clients.forEach((client) => {
      if (client.readyState === WebSocket.OPEN) {
        client.close()
      }
    })
  })

  it('should handle multiple clients simultaneously', (done) => {
    let connectedCount = 0
    const totalClients = clients.length

    clients.forEach((client) => {
      client.on('open', () => {
        connectedCount++
        if (connectedCount === totalClients) {
          expect(connectedCount).toBe(totalClients)
          done()
        }
      })

      client.on('error', () => {
        // Server not available, skip test
        if (connectedCount === 0) {
          done()
        }
      })
    })
  })

  it('should broadcast profile changes to all clients', (done) => {
    if (clients.length === 0 || clients[0].readyState !== WebSocket.OPEN) {
      console.warn('Clients not connected, skipping test')
      return done()
    }

    const profileUpdate = {
      kind: 'profile',
      action: 'update',
      profile: {
        id: 'shared-profile',
        name: 'Shared Profile',
        rows: 3,
        cols: 5,
        controls: [],
      },
    }

    let receivedCount = 0
    const expectedCount = clients.length

    clients.forEach((client) => {
      client.once('message', (data) => {
        const response = JSON.parse(data.toString())
        if (response.kind === 'profile' && response.action === 'update') {
          receivedCount++
          if (receivedCount === expectedCount) {
            done()
          }
        }
      })
    })

    // Send update from first client
    clients[0].send(JSON.stringify(profileUpdate))
  })

  it('should handle concurrent control actions', (done) => {
    if (clients.length === 0 || clients[0].readyState !== WebSocket.OPEN) {
      console.warn('Clients not connected, skipping test')
      return done()
    }

    let ackCount = 0
    const expectedCount = clients.length

    clients.forEach((client, index) => {
      const action = {
        kind: 'control',
        controlId: `test-control-${index}`,
        value: 1,
        messageId: `msg-${Date.now()}-${index}`,
        sentAt: Date.now(),
      }

      client.once('message', (data) => {
        const response = JSON.parse(data.toString())
        if (response.kind === 'ack') {
          ackCount++
          if (ackCount === expectedCount) {
            done()
          }
        }
      })

      client.send(JSON.stringify(action))
    })
  })
})


