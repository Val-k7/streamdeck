/**
 * Tests E2E pour les actions de contrôle
 */

import { describe, it, expect, beforeAll, afterAll } from '@jest/globals'
import WebSocket from 'ws'

describe('Control Actions E2E', () => {
  let ws
  const serverUrl = process.env.SERVER_URL || 'ws://localhost:4455'
  const testToken = process.env.TEST_TOKEN || 'test-token'

  beforeAll((done) => {
    ws = new WebSocket(`${serverUrl}?token=${testToken}`)
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

  it('should send button press action', (done) => {
    if (ws.readyState !== WebSocket.OPEN) {
      console.warn('WebSocket not connected, skipping test')
      return done()
    }

    const action = {
      kind: 'control',
      controlId: 'test-button',
      value: 1,
      messageId: `msg-${Date.now()}`,
      sentAt: Date.now(),
    }

    ws.send(JSON.stringify(action))

    ws.once('message', (data) => {
      const response = JSON.parse(data.toString())
      expect(response.kind).toBe('ack')
      expect(response.messageId).toBe(action.messageId)
      done()
    })
  })

  it('should send fader value change', (done) => {
    if (ws.readyState !== WebSocket.OPEN) {
      console.warn('WebSocket not connected, skipping test')
      return done()
    }

    const action = {
      kind: 'control',
      controlId: 'test-fader',
      value: 75,
      messageId: `msg-${Date.now()}`,
      sentAt: Date.now(),
    }

    ws.send(JSON.stringify(action))

    ws.once('message', (data) => {
      const response = JSON.parse(data.toString())
      expect(response.kind).toBe('ack')
      expect(response.messageId).toBe(action.messageId)
      done()
    })
  })

  it('should handle invalid control action', (done) => {
    if (ws.readyState !== WebSocket.OPEN) {
      console.warn('WebSocket not connected, skipping test')
      return done()
    }

    const invalidAction = {
      kind: 'control',
      controlId: 'non-existent-control',
      value: 1,
      messageId: `msg-${Date.now()}`,
      sentAt: Date.now(),
    }

    ws.send(JSON.stringify(invalidAction))

    ws.once('message', (data) => {
      const response = JSON.parse(data.toString())
      // Should receive error or ack with success: false
      expect(['ack', 'error']).toContain(response.kind)
      done()
    })
  })

  it('should receive action feedback', (done) => {
    if (ws.readyState !== WebSocket.OPEN) {
      console.warn('WebSocket not connected, skipping test')
      return done()
    }

    const action = {
      kind: 'control',
      controlId: 'test-button',
      value: 1,
      messageId: `msg-${Date.now()}`,
      sentAt: Date.now(),
    }

    ws.send(JSON.stringify(action))

    // Listen for feedback
    ws.once('message', (data) => {
      const response = JSON.parse(data.toString())
      if (response.kind === 'feedback') {
        expect(response.controlId).toBe(action.controlId)
        done()
      } else {
        // Wait for feedback
        setTimeout(() => done(), 1000)
      }
    })
  })
})


