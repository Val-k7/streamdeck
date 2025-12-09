/**
 * Tests E2E pour la gestion des profils
 */

import { describe, it, expect } from '@jest/globals'
import fetch from 'node-fetch'

const serverUrl = process.env.SERVER_URL || 'http://localhost:4455'

describe('Profile Management E2E', () => {
  const profileId = 'e2e-test-profile'

  it('should create a new profile', async () => {
    const response = await fetch(`${serverUrl}/profiles/${profileId}`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        id: profileId,
        name: 'E2E Test Profile',
        rows: 3,
        cols: 5,
        controls: [],
      }),
    })

    expect(response.ok).toBe(true)
  })

  it('should load existing profiles', async () => {
    const response = await fetch(`${serverUrl}/profiles/`)
    expect(response.ok).toBe(true)
    const payload = await response.json()
    expect(Array.isArray(payload.profiles)).toBe(true)
  })

  it('should update a profile', async () => {
    const response = await fetch(`${serverUrl}/profiles/${profileId}`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        id: profileId,
        name: 'Updated E2E Test Profile',
        rows: 3,
        cols: 5,
        controls: [],
      }),
    })

    expect(response.ok).toBe(true)
  })

  it('should delete a profile', async () => {
    const response = await fetch(`${serverUrl}/profiles/${profileId}`, {
      method: 'DELETE',
    })

    expect(response.ok).toBe(true)
  })
})


