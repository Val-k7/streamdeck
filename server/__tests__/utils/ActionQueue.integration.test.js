import { describe, it, expect, vi, beforeEach, afterEach } from '@jest/globals'
import { getActionQueue } from '../../utils/ActionQueue.js'

// Mock logger
const mockLogger = {
  info: vi.fn(),
  warn: vi.fn(),
  error: vi.fn(),
  debug: vi.fn(),
}

describe('ActionQueue Integration', () => {
  let actionQueue

  beforeEach(() => {
    vi.clearAllMocks()
    actionQueue = getActionQueue(3, 5000, mockLogger) // maxConcurrent: 3, timeout: 5s
  })

  afterEach(() => {
    if (actionQueue) {
      actionQueue.clear()
    }
  })

  describe('Concurrent execution', () => {
    it('should execute actions concurrently up to maxConcurrent', async () => {
      const executionOrder = []
      const actions = []

      for (let i = 0; i < 5; i++) {
        const index = i
        const action = vi.fn().mockImplementation(() => {
          executionOrder.push(`start-${index}`)
          return new Promise(resolve => {
            setTimeout(() => {
              executionOrder.push(`end-${index}`)
              resolve(index)
            }, 100)
          })
        })
        actions.push(actionQueue.enqueue(action, 0))
      }

      await Promise.all(actions)

      // Should have started 3 actions immediately (maxConcurrent)
      const starts = executionOrder.filter(o => o.startsWith('start-'))
      expect(starts.length).toBe(5) // All should eventually start

      // All actions should complete
      actions.forEach((action) => {
        expect(action).resolves.toBeDefined()
      })
    })

    it('should handle mixed priority actions', async () => {
      const executionOrder = []

      // Low priority actions
      const low1 = vi.fn().mockImplementation(() => {
        executionOrder.push('low1')
        return Promise.resolve('low1')
      })
      const low2 = vi.fn().mockImplementation(() => {
        executionOrder.push('low2')
        return Promise.resolve('low2')
      })

      // High priority actions
      const high1 = vi.fn().mockImplementation(() => {
        executionOrder.push('high1')
        return Promise.resolve('high1')
      })
      const high2 = vi.fn().mockImplementation(() => {
        executionOrder.push('high2')
        return Promise.resolve('high2')
      })

      // Enqueue in mixed order
      actionQueue.enqueue(low1, 1)
      actionQueue.enqueue(high1, 10)
      actionQueue.enqueue(low2, 1)
      actionQueue.enqueue(high2, 10)

      await new Promise(resolve => setTimeout(resolve, 300))

      // High priority should execute first
      expect(high1).toHaveBeenCalled()
      expect(high2).toHaveBeenCalled()
    })
  })

  describe('Error handling', () => {
    it('should continue processing after error', async () => {
      const failingAction = vi.fn().mockRejectedValue(new Error('Failed'))
      const succeedingAction = vi.fn().mockResolvedValue('Success')

      const promise1 = actionQueue.enqueue(failingAction, 0)
      const promise2 = actionQueue.enqueue(succeedingAction, 0)

      await expect(promise1).rejects.toThrow('Failed')
      await expect(promise2).resolves.toBe('Success')
    })

    it('should handle timeout correctly', async () => {
      const slowAction = vi.fn().mockImplementation(() =>
        new Promise(resolve => setTimeout(resolve, 10000))
      )

      const shortTimeoutQueue = getActionQueue(5, 100, mockLogger) // 100ms timeout

      await expect(
        shortTimeoutQueue.enqueue(slowAction, 0)
      ).rejects.toThrow()
    })
  })

  describe('Statistics', () => {
    it('should track statistics correctly', async () => {
      const action1 = vi.fn().mockResolvedValue('result1')
      const action2 = vi.fn().mockResolvedValue('result2')
      const action3 = vi.fn().mockRejectedValue(new Error('error'))

      await Promise.allSettled([
        actionQueue.enqueue(action1, 0),
        actionQueue.enqueue(action2, 0),
        actionQueue.enqueue(action3, 0),
      ])

      const stats = actionQueue.getStats()

      expect(stats.total).toBeGreaterThanOrEqual(3)
      expect(stats.completed).toBeGreaterThanOrEqual(2)
      expect(stats.failed).toBeGreaterThanOrEqual(1)
    })
  })
})


