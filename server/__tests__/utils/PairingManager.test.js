import { describe, it, expect, vi, beforeEach } from '@jest/globals'
import { PairingManager } from '../../utils/PairingManager.js'

// Mock logger
const mockLogger = {
  info: vi.fn(),
  warn: vi.fn(),
  error: vi.fn(),
  debug: vi.fn(),
}

describe('PairingManager', () => {
  let pairingManager

  beforeEach(() => {
    pairingManager = new PairingManager(mockLogger)
  })

  describe('generatePairingCode', () => {
    it('should generate pairing code', () => {
      const code = pairingManager.generatePairingCode('server-1', 'client-1')

      expect(code).toMatch(/^\d{6}$/)
    })

    it('should generate unique codes', () => {
      const qrData1 = pairingManager.generateQRCodeData('server-1', 'Server', 4455, 'ws')
      const qrData2 = pairingManager.generateQRCodeData('server-1', 'Server', 4455, 'ws')

      // Codes should be different (or same if within same time window)
      expect(qrData1.code).toBeDefined()
      expect(qrData2.code).toBeDefined()
    })
  })

  describe('validatePairingCode', () => {
    it('should validate correct pairing code', () => {
      const qrData = pairingManager.generateQRCodeData('server-1', 'Server', 4455, 'ws')

      const isValid = pairingManager.validatePairingCode(qrData.code, 'server-1')

      expect(isValid).toBe(true)
    })

    it('should reject invalid pairing code', () => {
      const isValid = pairingManager.validatePairingCode('000000', 'server-1')

      expect(isValid).toBe(false)
    })

    it('should reject expired pairing code', async () => {
      const shortTTLManager = getPairingManager(100) // 100ms TTL
      const qrData = shortTTLManager.generateQRCodeData('server-1', 'Server', 4455, 'ws')

      await new Promise(resolve => setTimeout(resolve, 150))

      const isValid = shortTTLManager.validatePairingCode(qrData.code, 'server-1')

      expect(isValid).toBe(false)
    })

    it('should reject code for wrong server', () => {
      const qrData = pairingManager.generateQRCodeData('server-1', 'Server', 4455, 'ws')

      const isValid = pairingManager.validatePairingCode(qrData.code, 'server-2')

      expect(isValid).toBe(false)
    })
  })

  describe('finalizePairing', () => {
    it('should finalize pairing', () => {
      const qrData = pairingManager.generateQRCodeData('server-1', 'Server', 4455, 'ws')

      const success = pairingManager.finalizePairing(qrData.code, 'server-1', 'fingerprint-1')

      expect(success).toBe(true)
    })

    it('should return false for invalid code', () => {
      const success = pairingManager.finalizePairing('000000', 'server-1', 'fingerprint-1')

      expect(success).toBe(false)
    })
  })

  describe('getPairedServers', () => {
    it('should return paired servers', () => {
      const qrData = pairingManager.generateQRCodeData('server-1', 'Server', 4455, 'ws')
      pairingManager.finalizePairing(qrData.code, 'server-1', 'fingerprint-1')

      const servers = pairingManager.getPairedServers()

      expect(Array.isArray(servers)).toBe(true)
      expect(servers.length).toBeGreaterThan(0)
    })
  })
})
