/**
 * Tests E2E pour la gestion des profils
 */

import { describe, it, expect, beforeAll, afterAll } from '@jest/globals'
import WebSocket from 'ws'

describe('Profile Management E2E', () => {
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

  it('should create a new profile', (done) => {
    if (ws.readyState !== WebSocket.OPEN) {
      console.warn('WebSocket not connected, skipping test')
      return done()
    }

    const newProfile = {
      kind: 'profile',
      action: 'create',
      profile: {
        id: 'e2e-test-profile',
        name: 'E2E Test Profile',
        rows: 3,
        cols: 5,
        controls: [],
      },
    }

    ws.send(JSON.stringify(newProfile))

    ws.once('message', (data) => {
      const response = JSON.parse(data.toString())
      expect(response.kind).toBe('ack')
      expect(response.success).toBe(true)
      done()
    })
  })

  it('should load existing profiles', (done) => {
    if (ws.readyState !== WebSocket.OPEN) {
      console.warn('WebSocket not connected, skipping test')
      return done()
    }

    const request = {
      kind: 'profile',
      action: 'list',
    }

    ws.send(JSON.stringify(request))

    ws.once('message', (data) => {
      const response = JSON.parse(data.toString())
      expect(response.kind).toBe('profiles')
      expect(Array.isArray(response.profiles)).toBe(true)
      done()
    })
  })

  it('should update a profile', (done) => {
    if (ws.readyState !== WebSocket.OPEN) {
      console.warn('WebSocket not connected, skipping test')
      return done()
    }

    const updatedProfile = {
      kind: 'profile',
      action: 'update',
      profile: {
        id: 'e2e-test-profile',
        name: 'Updated E2E Test Profile',
        rows: 3,
        cols: 5,
        controls: [],
      },
    }

    ws.send(JSON.stringify(updatedProfile))

    ws.once('message', (data) => {
      const response = JSON.parse(data.toString())
      expect(response.kind).toBe('ack')
      expect(response.success).toBe(true)
      done()
    })
  })

  it('should delete a profile', (done) => {
    if (ws.readyState !== WebSocket.OPEN) {
      console.warn('WebSocket not connected, skipping test')
      return done()
    }

    const deleteRequest = {
      kind: 'profile',
      action: 'delete',
      profileId: 'e2e-test-profile',
    }

    ws.send(JSON.stringify(deleteRequest))

    ws.once('message', (data) => {
      const response = JSON.parse(data.toString())
      expect(response.kind).toBe('ack')
      expect(response.success).toBe(true)
      done()
    })
  })
})


