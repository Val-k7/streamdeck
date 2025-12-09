import { describe, it, expect, beforeEach, vi } from 'vitest'
import { renderHook, act } from '@testing-library/react'
import useDeckStorage from '../useDeckStorage'

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

describe('useDeckStorage', () => {
  beforeEach(() => {
    localStorageMock.clear()
  })

  it('should initialize with empty pads', () => {
    const { result } = renderHook(() => useDeckStorage('test-profile'))
    expect(result.current.pads).toEqual([])
  })

  it('should add a pad', () => {
    const { result } = renderHook(() => useDeckStorage('test-profile'))
    
    act(() => {
      result.current.addPad({
        id: 'pad-1',
        type: 'button',
        size: '1x1',
        label: 'Test',
        color: 'primary',
      })
    })

    expect(result.current.pads).toHaveLength(1)
    expect(result.current.pads[0].id).toBe('pad-1')
  })

  it('should update a pad', () => {
    const { result } = renderHook(() => useDeckStorage('test-profile'))
    
    act(() => {
      result.current.addPad({
        id: 'pad-1',
        type: 'button',
        size: '1x1',
        label: 'Test',
        color: 'primary',
      })
    })

    act(() => {
      result.current.updatePad('pad-1', { label: 'Updated' })
    })

    expect(result.current.pads[0].label).toBe('Updated')
  })

  it('should delete a pad', () => {
    const { result } = renderHook(() => useDeckStorage('test-profile'))
    
    act(() => {
      result.current.addPad({
        id: 'pad-1',
        type: 'button',
        size: '1x1',
        label: 'Test',
        color: 'primary',
      })
    })

    act(() => {
      result.current.deletePad('pad-1')
    })

    expect(result.current.pads).toHaveLength(0)
  })

  it('should clear all pads', () => {
    const { result } = renderHook(() => useDeckStorage('test-profile'))
    
    act(() => {
      result.current.addPad({
        id: 'pad-1',
        type: 'button',
        size: '1x1',
        label: 'Test',
        color: 'primary',
      })
      result.current.addPad({
        id: 'pad-2',
        type: 'button',
        size: '1x1',
        label: 'Test 2',
        color: 'primary',
      })
    })

    act(() => {
      result.current.clearPads()
    })

    expect(result.current.pads).toHaveLength(0)
  })
})


