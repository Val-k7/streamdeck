import { describe, it, expect, vi, beforeEach } from '@jest/globals'
import { getHealthChecker } from '../../utils/HealthChecker.js'
import os from 'os'

// Mock logger
const mockLogger = {
  info: vi.fn(),
  warn: vi.fn(),
  error: vi.fn(),
}

// Mock os
vi.mock('os', () => ({
  default: {
    totalmem: vi.fn(() => 8 * 1024 * 1024 * 1024), // 8GB
    freemem: vi.fn(() => 4 * 1024 * 1024 * 1024), // 4GB
    loadavg: vi.fn(() => [0.5, 0.6, 0.7]),
    cpus: vi.fn(() => [
      { model: 'CPU 1', speed: 2400 },
      { model: 'CPU 2', speed: 2400 },
    ]),
    platform: vi.fn(() => 'linux'),
    uptime: vi.fn(() => 3600), // 1 hour
  },
}))

describe('HealthChecker', () => {
  let healthChecker

  beforeEach(() => {
    vi.clearAllMocks()
    healthChecker = getHealthChecker(mockLogger)
  })

  describe('registerCheck', () => {
    it('should register a health check', () => {
      const checkFn = vi.fn().mockResolvedValue({ healthy: true })

      healthChecker.registerCheck('test-check', checkFn, false)

      expect(healthChecker.checks.has('test-check')).toBe(true)
    })

    it('should register critical check', () => {
      const checkFn = vi.fn().mockResolvedValue({ healthy: true })

      healthChecker.registerCheck('critical-check', checkFn, true)

      const check = healthChecker.checks.get('critical-check')
      expect(check.critical).toBe(true)
    })
  })

  describe('runChecks', () => {
    it('should run all registered checks', async () => {
      const checkFn = vi.fn().mockResolvedValue({ healthy: true })
      healthChecker.registerCheck('test-check', checkFn)

      const results = await healthChecker.runChecks()

      expect(results).toHaveProperty('test-check')
      expect(checkFn).toHaveBeenCalled()
    })

    it('should handle check failures', async () => {
      const checkFn = vi.fn().mockResolvedValue({ healthy: false, message: 'Failed' })
      healthChecker.registerCheck('failing-check', checkFn)

      const results = await healthChecker.runChecks()

      expect(results['failing-check']).toHaveProperty('healthy', false)
    })

    it('should handle check errors', async () => {
      const checkFn = vi.fn().mockRejectedValue(new Error('Check error'))
      healthChecker.registerCheck('error-check', checkFn)

      const results = await healthChecker.runChecks()

      expect(results['error-check']).toHaveProperty('healthy', false)
    })

    it('should determine overall health', async () => {
      const healthyCheck = vi.fn().mockResolvedValue({ healthy: true })
      const unhealthyCheck = vi.fn().mockResolvedValue({ healthy: false })

      healthChecker.registerCheck('healthy', healthyCheck)
      healthChecker.registerCheck('unhealthy', unhealthyCheck)

      const results = await healthChecker.runChecks()

      expect(results).toHaveProperty('overall')
      expect(['healthy', 'degraded', 'unhealthy']).toContain(results.overall)
    })
  })

  describe('getStatus', () => {
    it('should return current health status', async () => {
      const checkFn = vi.fn().mockResolvedValue({ healthy: true })
      healthChecker.registerCheck('test-check', checkFn)

      await healthChecker.runChecks()
      const status = healthChecker.getStatus()

      expect(status).toHaveProperty('overall')
      expect(status).toHaveProperty('checks')
    })
  })
})

