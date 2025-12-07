import { describe, it, expect, vi, beforeEach } from 'vitest'
import { renderHook, act } from '@testing-library/react'
import { useSwipeGesture } from '../useSwipeGesture'

describe('useSwipeGesture', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('should detect swipe left', () => {
    const onSwipeLeft = vi.fn()
    const { result } = renderHook(() => useSwipeGesture({ onSwipeLeft }))

    // Simulate swipe left
    act(() => {
      // Mock touch events
      const touchStart = new TouchEvent('touchstart', {
        touches: [{ clientX: 100, clientY: 50 } as Touch],
      })
      const touchEnd = new TouchEvent('touchend', {
        changedTouches: [{ clientX: 50, clientY: 50 } as Touch],
      })

      // Trigger handlers
      result.current.onTouchStart(touchStart)
      result.current.onTouchEnd(touchEnd)
    })

    expect(onSwipeLeft).toHaveBeenCalled()
  })

  it('should detect swipe right', () => {
    const onSwipeRight = vi.fn()
    const { result } = renderHook(() => useSwipeGesture({ onSwipeRight }))

    // Simulate swipe right
    act(() => {
      const touchStart = new TouchEvent('touchstart', {
        touches: [{ clientX: 50, clientY: 50 } as Touch],
      })
      const touchEnd = new TouchEvent('touchend', {
        changedTouches: [{ clientX: 100, clientY: 50 } as Touch],
      })

      result.current.onTouchStart(touchStart)
      result.current.onTouchEnd(touchEnd)
    })

    expect(onSwipeRight).toHaveBeenCalled()
  })

  it('should not trigger on small movements', () => {
    const onSwipeLeft = vi.fn()
    const { result } = renderHook(() => useSwipeGesture({ onSwipeLeft }))

    act(() => {
      const touchStart = new TouchEvent('touchstart', {
        touches: [{ clientX: 100, clientY: 50 } as Touch],
      })
      const touchEnd = new TouchEvent('touchend', {
        changedTouches: [{ clientX: 95, clientY: 50 } as Touch], // Small movement
      })

      result.current.onTouchStart(touchStart)
      result.current.onTouchEnd(touchEnd)
    })

    expect(onSwipeLeft).not.toHaveBeenCalled()
  })
})


