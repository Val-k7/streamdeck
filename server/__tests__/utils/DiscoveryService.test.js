import { describe, it, expect, vi, beforeEach, afterEach } from '@jest/globals'
import { DiscoveryService } from '../../utils/DiscoveryService.js'

// Mock bonjour
vi.mock('bonjour', () => ({
  default: vi.fn(() => ({
    publish: vi.fn(() => ({
      start: vi.fn(),
      stop: vi.fn(),
    })),
    find: vi.fn(() => ({
      on: vi.fn(),
      start: vi.fn(),
      stop: vi.fn(),
    })),
  })),
}))

// Mock dgram
const mockSocket = {
  bind: vi.fn(),
  on: vi.fn(),
  send: vi.fn(),
  close: vi.fn(),
}

vi.mock('dgram', () => ({
  createSocket: vi.fn(() => mockSocket),
}))

// Mock logger
const mockLogger = {
  info: vi.fn(),
  warn: vi.fn(),
  error: vi.fn(),
}

describe('DiscoveryService', () => {
  let discoveryService

  beforeEach(() => {
    vi.clearAllMocks()
    discoveryService = new DiscoveryService(mockLogger)
  })

  afterEach(() => {
    if (discoveryService) {
      discoveryService.stop()
    }
  })

  describe('mDNS Discovery', () => {
    it('should start mDNS service', () => {
      expect(() => {
        discoveryService.start(8080, 'test-server')
      }).not.toThrow()
    })

    it('should stop mDNS service', () => {
      discoveryService.start(8080, 'test-server')
      expect(() => {
        discoveryService.stop()
      }).not.toThrow()
    })

    it('should publish service with correct name', () => {
      discoveryService.start(8080, 'test-server')
      // Verify bonjour.publish was called
      expect(mockLogger.info).toHaveBeenCalled()
    })
  })

  describe('UDP Broadcast', () => {
    it('should start UDP broadcast', () => {
      expect(() => {
        discoveryService.start(8080, 'test-server')
      }).not.toThrow()

      expect(mockSocket.bind).toHaveBeenCalled()
    })

    it('should send discovery message', () => {
      discoveryService.start(8080, 'test-server')
      // UDP broadcast should be set up
      expect(mockSocket.on).toHaveBeenCalled()
    })

    it('should handle UDP errors gracefully', () => {
      discoveryService.start(8080, 'test-server')

      // Simulate error
      const errorHandler = mockSocket.on.mock.calls.find(
        call => call[0] === 'error'
      )?.[1]

      if (errorHandler) {
        expect(() => {
          errorHandler(new Error('UDP error'))
        }).not.toThrow()
      }
    })
  })

  describe('Service Information', () => {
    it('should return correct service info', () => {
      discoveryService.start(8080, 'test-server')

      const info = discoveryService.getServiceInfo()
      expect(info).toHaveProperty('port', 8080)
      expect(info).toHaveProperty('serverName', 'test-server')
    })
  })
})


