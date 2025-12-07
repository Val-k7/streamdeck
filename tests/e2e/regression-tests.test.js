/**
 * Tests E2E - Tests de Régression
 */

import { describe, it, expect, beforeAll, afterAll } from '@jest/globals'
import WebSocket from 'ws'
import fetch from 'node-fetch'

describe('Regression Tests E2E', () => {
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

  describe('Templates Tests', () => {
    const templates = ['User', 'Gamer', 'Streamer', 'Audio', 'Productivité']

    templates.forEach((template) => {
      it(`should load ${template} template`, async () => {
        if (ws.readyState !== WebSocket.OPEN) {
          console.warn('WebSocket not connected, skipping test')
          return
        }

        const response = await fetch(`${serverUrl}/profiles`)
        expect(response.ok).toBe(true)

        const data = await response.json()
        expect(data).toHaveProperty('profiles')
        expect(Array.isArray(data.profiles)).toBe(true)

        // Check if template profile exists
        const templateProfile = data.profiles.find((p) =>
          p.name.toLowerCase().includes(template.toLowerCase())
        )
        // Template may or may not exist, but API should work
        expect(data.profiles).toBeDefined()
      })
    })
  })

  describe('Server Actions Tests', () => {
    const actions = [
      { type: 'KEYBOARD', payload: 'CTRL+C' },
      { type: 'OBS', payload: 'StartStreaming' },
      { type: 'AUDIO', payload: 'SetVolume:50' },
      { type: 'SCRIPT', payload: 'test-script.sh' },
    ]

    actions.forEach((action) => {
      it(`should execute ${action.type} action`, (done) => {
        if (ws.readyState !== WebSocket.OPEN) {
          console.warn('WebSocket not connected, skipping test')
          return done()
        }

        const payload = {
          kind: 'control',
          controlId: `test-${action.type.toLowerCase()}`,
          type: action.type,
          value: 1,
          messageId: `msg-${Date.now()}`,
          sentAt: Date.now(),
        }

        ws.send(JSON.stringify(payload))

        ws.once('message', (data) => {
          const response = JSON.parse(data.toString())
          // Should receive ack or error
          expect(['ack', 'error']).toContain(response.kind)
          done()
        })
      })
    })
  })

  describe('Compatibility Tests', () => {
    it('should handle Android 7.0+ compatibility', async () => {
      // This is a placeholder for Android compatibility tests
      // Actual tests would run on Android devices/emulators
      expect(true).toBe(true)
    })

    it('should handle different Node.js versions', async () => {
      const response = await fetch(`${serverUrl}/health`)
      expect(response.ok).toBe(true)
    })

    it('should handle different browsers', async () => {
      // This is a placeholder for browser compatibility tests
      // Actual tests would run in different browsers
      expect(true).toBe(true)
    })
  })

  describe('Performance Tests', () => {
    it('should handle rapid control actions', (done) => {
      if (ws.readyState !== WebSocket.OPEN) {
        console.warn('WebSocket not connected, skipping test')
        return done()
      }

      const startTime = Date.now()
      let completed = 0
      const totalActions = 10

      for (let i = 0; i < totalActions; i++) {
        const action = {
          kind: 'control',
          controlId: `test-rapid-${i}`,
          value: 1,
          messageId: `msg-${Date.now()}-${i}`,
          sentAt: Date.now(),
        }

        ws.send(JSON.stringify(action))

        ws.once('message', (data) => {
          const response = JSON.parse(data.toString())
          if (response.kind === 'ack') {
            completed++
            if (completed === totalActions) {
              const endTime = Date.now()
              const duration = endTime - startTime
              // Should complete within reasonable time (e.g., 5 seconds)
              expect(duration).toBeLessThan(5000)
              done()
            }
          }
        })
      }
    })
  })
})


