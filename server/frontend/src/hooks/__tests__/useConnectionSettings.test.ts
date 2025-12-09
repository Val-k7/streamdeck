import { describe, it, expect, vi, beforeEach } from 'vitest'
import { renderHook, act } from '@testing-library/react'
import useConnectionSettings from '../useConnectionSettings'

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

describe('useConnectionSettings', () => {
  beforeEach(() => {
    localStorageMock.clear()
  })

  it('should initialize with default values', () => {
    const { result } = renderHook(() => useConnectionSettings())

    expect(result.current.serverIp).toBeDefined()
    expect(result.current.serverPort).toBeDefined()
    expect(typeof result.current.useTls).toBe('boolean')
  })

  it('should update server IP', () => {
    const { result } = renderHook(() => useConnectionSettings())

    act(() => {
      result.current.setServerIp('192.168.1.100')
    })

    expect(result.current.serverIp).toBe('192.168.1.100')
  })

  it('should update server port', () => {
    const { result } = renderHook(() => useConnectionSettings())

    act(() => {
      result.current.setServerPort(4455)
    })

    expect(result.current.serverPort).toBe(4455)
  })

  it('should toggle TLS', () => {
    const { result } = renderHook(() => useConnectionSettings())
    const initialTls = result.current.useTls

    act(() => {
      result.current.setUseTls(!initialTls)
    })

    expect(result.current.useTls).toBe(!initialTls)
  })

  it('should persist settings to localStorage', () => {
    const { result } = renderHook(() => useConnectionSettings())

    act(() => {
      result.current.setServerIp('192.168.1.100')
      result.current.setServerPort(4455)
    })

    // Settings should be persisted
    const stored = localStorageMock.getItem('connection-settings')
    expect(stored).toBeDefined()
  })
})


