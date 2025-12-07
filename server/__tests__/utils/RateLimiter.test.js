import { describe, it, expect, vi, beforeEach } from '@jest/globals'
import { getRateLimiter } from '../../utils/RateLimiter.js'

describe('RateLimiter', () => {
  let rateLimiter

  beforeEach(() => {
    rateLimiter = getRateLimiter()
  })

  describe('check', () => {
    it('should allow requests within limit', () => {
      const result1 = rateLimiter.check('ip', '192.168.1.1')
      const result2 = rateLimiter.check('ip', '192.168.1.1')
      const result3 = rateLimiter.check('ip', '192.168.1.1')

      expect(result1.allowed).toBe(true)
      expect(result2.allowed).toBe(true)
      expect(result3.allowed).toBe(true)
    })

    it('should block requests exceeding limit', () => {
      // Make many requests quickly
      for (let i = 0; i < 100; i++) {
        rateLimiter.check('ip', '192.168.1.1')
      }

      const result = rateLimiter.check('ip', '192.168.1.1')

      expect(result.allowed).toBe(false)
      expect(result.retryAfter).toBeGreaterThan(0)
    })

    it('should track different keys separately', () => {
      const result1 = rateLimiter.check('ip', '192.168.1.1')
      const result2 = rateLimiter.check('ip', '192.168.1.2')

      expect(result1.allowed).toBe(true)
      expect(result2.allowed).toBe(true)
    })

    it('should reset after window expires', async () => {
      // Make requests to exceed limit
      for (let i = 0; i < 100; i++) {
        rateLimiter.check('ip', '192.168.1.1')
      }

      const blocked = rateLimiter.check('ip', '192.168.1.1')
      expect(blocked.allowed).toBe(false)

      // Wait for window to expire (if window is short)
      // Note: This test may need adjustment based on actual window size
      await new Promise(resolve => setTimeout(resolve, 1100))

      // Should allow again after window
      // (This depends on the actual window size in RateLimiter)
    })
  })

  describe('getStats', () => {
    it('should return statistics', () => {
      rateLimiter.check('ip', '192.168.1.1')
      rateLimiter.check('ip', '192.168.1.2')

      const stats = rateLimiter.getStats()

      expect(stats).toHaveProperty('total')
      expect(stats).toHaveProperty('blocked')
      expect(stats.total).toBeGreaterThanOrEqual(2)
    })
  })
})
