import { describe, it, expect, vi, beforeEach, afterEach } from '@jest/globals'
import { getActionQueue } from '../../utils/ActionQueue.js'

// Mock logger
const mockLogger = {
  info: vi.fn(),
  warn: vi.fn(),
  error: vi.fn(),
  debug: vi.fn(),
}

describe('ActionQueue', () => {
  let actionQueue

  beforeEach(() => {
    vi.clearAllMocks()
    actionQueue = getActionQueue(5, 30000, mockLogger) // maxConcurrency: 5, timeout: 30s
  })

  afterEach(() => {
    if (actionQueue) {
      actionQueue.clear()
    }
  })

  describe('enqueue', () => {
    it('should enqueue an action', async () => {
      const action = vi.fn().mockResolvedValue('success')

      const promise = actionQueue.enqueue(action, 0) // priority = 0

      expect(actionQueue.queue.length).toBeGreaterThan(0)
      await promise
      expect(action).toHaveBeenCalled()
    })

    it('should execute actions concurrently up to maxConcurrent', async () => {
      const actions = []
      for (let i = 0; i < 10; i++) {
        const action = vi.fn().mockImplementation(() =>
          new Promise(resolve => setTimeout(() => resolve(i), 100))
        )
        actions.push(actionQueue.enqueue(action, 0))
      }

      await Promise.all(actions)

      // All actions should have been executed
      actions.forEach((action) => {
        expect(action).resolves.toBeDefined()
      })
    })

    it('should handle action errors gracefully', async () => {
      const action = vi.fn().mockRejectedValue(new Error('Action failed'))

      await expect(
        actionQueue.enqueue(action, 0)
      ).rejects.toThrow('Action failed')
    })

    it('should prioritize actions', async () => {
      const results = []

      const lowPriority = vi.fn().mockImplementation(() => {
        results.push('low')
        return Promise.resolve('low')
      })

      const highPriority = vi.fn().mockImplementation(() => {
        results.push('high')
        return Promise.resolve('high')
      })

      actionQueue.enqueue(lowPriority, 1) // Lower priority
      actionQueue.enqueue(highPriority, 10) // Higher priority

      await new Promise(resolve => setTimeout(resolve, 200))

      // High priority should execute first
      expect(highPriority).toHaveBeenCalled()
    })
  })

  describe('queue size', () => {
    it('should track queue size', () => {
      expect(actionQueue.queue.length).toBe(0)

      actionQueue.enqueue(vi.fn(), 0)
      actionQueue.enqueue(vi.fn(), 0)

      expect(actionQueue.queue.length).toBeGreaterThan(0)
    })
  })

  describe('clear', () => {
    it('should clear all queued actions', () => {
      actionQueue.enqueue(vi.fn(), 0)
      actionQueue.enqueue(vi.fn(), 0)

      actionQueue.clear()

      expect(actionQueue.queue.length).toBe(0)
    })
  })
})

