import { describe, it, expect, vi, beforeEach, afterEach } from '@jest/globals'
import { getTokenManager } from '../../utils/TokenManager.js'

describe('TokenManager', () => {
  let tokenManager

  beforeEach(() => {
    tokenManager = getTokenManager(3600000) // 1 hour TTL
  })

  afterEach(() => {
    if (tokenManager) {
      tokenManager.cleanup()
    }
  })

  describe('issueToken', () => {
    it('should issue a new token', () => {
      const tokenData = tokenManager.issueToken('client-1')

      expect(tokenData).toHaveProperty('token')
      expect(tokenData).toHaveProperty('expiresAt')
      expect(tokenData).toHaveProperty('expiresIn')
      expect(tokenData.clientId).toBe('client-1')
    })

    it('should issue token with custom TTL', () => {
      const tokenData = tokenManager.issueToken('client-1', {}, 7200000) // 2 hours

      expect(tokenData.expiresIn).toBe(7200000)
    })

    it('should issue token with metadata', () => {
      const metadata = { ip: '192.168.1.1', userAgent: 'test' }
      const tokenData = tokenManager.issueToken('client-1', metadata)

      expect(tokenData.metadata).toEqual(metadata)
    })
  })

  describe('isValid', () => {
    it('should validate valid token', () => {
      const tokenData = tokenManager.issueToken('client-1')

      expect(tokenManager.isValid(tokenData.token)).toBe(true)
    })

    it('should reject invalid token', () => {
      expect(tokenManager.isValid('invalid-token')).toBe(false)
    })

    it('should reject expired token', async () => {
      const shortTTLManager = getTokenManager(100) // 100ms TTL
      const tokenData = shortTTLManager.issueToken('client-1')

      await new Promise(resolve => setTimeout(resolve, 150))

      expect(shortTTLManager.isValid(tokenData.token)).toBe(false)
    })
  })

  describe('revokeToken', () => {
    it('should revoke a token', () => {
      const tokenData = tokenManager.issueToken('client-1')

      const revoked = tokenManager.revokeToken(tokenData.token)

      expect(revoked).toBe(true)
      expect(tokenManager.isValid(tokenData.token)).toBe(false)
    })

    it('should return false for non-existent token', () => {
      const revoked = tokenManager.revokeToken('non-existent-token')

      expect(revoked).toBe(false)
    })
  })

  describe('getTokenInfo', () => {
    it('should get token information', () => {
      const tokenData = tokenManager.issueToken('client-1', { ip: '192.168.1.1' })

      const info = tokenManager.getTokenInfo(tokenData.token)

      expect(info).toHaveProperty('clientId', 'client-1')
      expect(info).toHaveProperty('metadata')
      expect(info).toHaveProperty('issuedAt')
      expect(info).toHaveProperty('expiresAt')
    })

    it('should return null for invalid token', () => {
      const info = tokenManager.getTokenInfo('invalid-token')

      expect(info).toBeNull()
    })
  })

  describe('rotateToken', () => {
    it('should rotate a token', () => {
      const oldTokenData = tokenManager.issueToken('client-1')

      const newTokenData = tokenManager.rotateToken(oldTokenData.token, 'client-1')

      expect(newTokenData).toHaveProperty('token')
      expect(newTokenData.token).not.toBe(oldTokenData.token)
      expect(tokenManager.isValid(oldTokenData.token)).toBe(false)
      expect(tokenManager.isValid(newTokenData.token)).toBe(true)
    })

    it('should throw error for invalid token', () => {
      expect(() => {
        tokenManager.rotateToken('invalid-token', 'client-1')
      }).toThrow()
    })
  })

  describe('cleanup', () => {
    it('should remove expired tokens', async () => {
      const shortTTLManager = getTokenManager(100) // 100ms TTL
      const tokenData = shortTTLManager.issueToken('client-1')

      await new Promise(resolve => setTimeout(resolve, 150))

      shortTTLManager.cleanup()

      expect(shortTTLManager.isValid(tokenData.token)).toBe(false)
    })
  })

  describe('getStats', () => {
    it('should return statistics', () => {
      tokenManager.issueToken('client-1')
      tokenManager.issueToken('client-2')

      const stats = tokenManager.getStats()

      expect(stats).toHaveProperty('total')
      expect(stats).toHaveProperty('active')
      expect(stats.active).toBeGreaterThanOrEqual(2)
    })
  })
})
