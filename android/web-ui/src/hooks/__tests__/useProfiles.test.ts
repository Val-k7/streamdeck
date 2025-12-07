import { describe, it, expect, vi, beforeEach } from 'vitest'
import { renderHook, act, waitFor } from '@testing-library/react'
import useProfiles from '../useProfiles'

// Mock localStorage
const localStorageMock = (() => {
  let store: Record<string, string> = {}
  return {
    getItem: (key: string) => store[key] || null,
    setItem: (key: string, value: string) => {
      store[key] = value
    },
    removeItem: (key: string) => {
      delete store[key]
    },
    clear: () => {
      store = {}
    },
  }
})()

Object.defineProperty(window, 'localStorage', {
  value: localStorageMock,
})

// Mock useWebSocket
vi.mock('../useWebSocket', () => ({
  default: () => ({
    selectProfile: vi.fn(),
    status: 'online',
  }),
}))

describe('useProfiles', () => {
  beforeEach(() => {
    localStorageMock.clear()
  })

  it('should initialize with default profiles', () => {
    const { result } = renderHook(() => useProfiles())

    expect(result.current.profiles).toBeDefined()
    expect(Array.isArray(result.current.profiles)).toBe(true)
  })

  it('should select a profile', async () => {
    const { result } = renderHook(() => useProfiles())

    await act(async () => {
      result.current.selectProfile('profile-1')
    })

    await waitFor(() => {
      expect(result.current.activeProfile?.id).toBe('profile-1')
    })
  })

  it('should handle profile selection error', async () => {
    const { result } = renderHook(() => useProfiles())

    await act(async () => {
      result.current.selectProfile('nonexistent')
    })

    // Should handle error gracefully
    expect(result.current.activeProfile).toBeDefined()
  })
})


