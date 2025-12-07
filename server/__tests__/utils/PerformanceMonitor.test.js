import { describe, it, expect, vi, beforeEach } from '@jest/globals'
import { getPerformanceMonitor } from '../../utils/PerformanceMonitor.js'

// Mock logger
const mockLogger = {
  info: vi.fn(),
  warn: vi.fn(),
  error: vi.fn(),
}

describe('PerformanceMonitor', () => {
  let monitor

  beforeEach(() => {
    vi.clearAllMocks()
    monitor = getPerformanceMonitor(mockLogger)
  })

  describe('recordRequest', () => {
    it('should record request metrics', () => {
      monitor.recordRequest(100, true)

      const report = monitor.getReport()
      expect(report.metrics.requests.total).toBe(1)
      expect(report.metrics.requests.successful).toBe(1)
    })

    it('should track failed requests', () => {
      monitor.recordRequest(50, false)

      const report = monitor.getReport()
      expect(report.metrics.requests.failed).toBe(1)
    })

    it('should calculate average latency', () => {
      monitor.recordRequest(100, true)
      monitor.recordRequest(200, true)
      monitor.recordRequest(150, true)

      const report = monitor.getReport()
      expect(report.metrics.requests.averageLatency).toBe(150)
    })
  })

  describe('recordAction', () => {
    it('should record action execution', () => {
      monitor.recordAction('test-action', 100)

      const report = monitor.getReport()
      expect(report.metrics.actions.total).toBe(1)
      expect(report.metrics.actions.byType.has('test-action')).toBe(true)
    })
  })

  describe('getReport', () => {
    it('should return complete performance report', () => {
      monitor.recordRequest(100, true)
      monitor.recordAction('action-1', 50)

      const report = monitor.getReport()

      expect(report).toHaveProperty('metrics')
      expect(report.metrics).toHaveProperty('requests')
      expect(report.metrics).toHaveProperty('actions')
      expect(report.metrics).toHaveProperty('memory')
      expect(report.metrics).toHaveProperty('cpu')
    })
  })

  describe('reset', () => {
    it('should reset all metrics', () => {
      monitor.recordRequest(100, true)
      monitor.recordAction('test-action', 50)

      monitor.reset()

      const report = monitor.getReport()
      expect(report.metrics.requests.total).toBe(0)
      expect(report.metrics.actions.total).toBe(0)
    })
  })
})

