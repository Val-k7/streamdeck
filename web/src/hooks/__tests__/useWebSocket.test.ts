import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { renderHook, waitFor } from '@testing-library/react'
import useWebSocket from '../useWebSocket'

// Mock WebSocket
const mockWebSocket = {
  send: vi.fn(),
  close: vi.fn(),
  addEventListener: vi.fn(),
  removeEventListener: vi.fn(),
  readyState: 1, // OPEN
  CONNECTING: 0,
  OPEN: 1,
  CLOSING: 2,
  CLOSED: 3,
}

global.WebSocket = vi.fn(() => mockWebSocket as any) as any

describe('useWebSocket', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('should initialize with disconnected state', () => {
    const { result } = renderHook(() => useWebSocket('ws://localhost:4455/ws'))
    
    expect(result.current.isConnected).toBe(false)
    expect(result.current.connectionState).toBe('disconnected')
  })

  it('should connect to WebSocket', async () => {
    const { result } = renderHook(() => useWebSocket('ws://localhost:4455/ws'))
    
    // Simulate WebSocket connection
    await waitFor(() => {
      // WebSocket should be created
      expect(global.WebSocket).toHaveBeenCalledWith('ws://localhost:4455/ws')
    })
  })

  it('should send message when connected', () => {
    const { result } = renderHook(() => useWebSocket('ws://localhost:4455/ws'))
    
    // Simulate connection
    mockWebSocket.readyState = 1 // OPEN
    
    result.current.sendMessage('test-control', 1.0, {})
    
    expect(mockWebSocket.send).toHaveBeenCalled()
  })

  it('should handle disconnection', () => {
    const { result } = renderHook(() => useWebSocket('ws://localhost:4455/ws'))
    
    result.current.disconnect()
    
    expect(mockWebSocket.close).toHaveBeenCalled()
  })
})


