import { describe, it, expect, vi, beforeEach } from '@jest/globals'
import { handleObs } from '../../actions/obs.js'

// Mock WebSocket client
const mockWebSocketClient = {
  send: vi.fn(),
  close: vi.fn(),
}

// Mock logger
const mockLogger = {
  info: vi.fn(),
  warn: vi.fn(),
  error: vi.fn(),
}

describe('OBS Actions', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('handleObs', () => {
    it('should handle StartStreaming action', async () => {
      const payload = { payload: 'StartStreaming' }

      // Mock fetch for OBS API
      global.fetch = vi.fn().mockResolvedValue({
        ok: true,
        json: async () => ({
          requestStatus: { code: 100 },
          responseData: {},
        }),
      })

      await expect(
        handleObs(payload, mockLogger)
      ).resolves.not.toThrow()
    })

    it('should handle StopStreaming action', async () => {
      const payload = { payload: 'StopStreaming' }

      global.fetch = vi.fn().mockResolvedValue({
        ok: true,
        json: async () => ({
          requestStatus: { code: 100 },
          responseData: {},
        }),
      })

      await expect(
        handleObs(payload, mockLogger)
      ).resolves.not.toThrow()
    })

    it('should handle SceneSwitch action', async () => {
      const payload = { payload: 'SceneSwitch', sceneName: 'Scene 1' }

      global.fetch = vi.fn().mockResolvedValue({
        ok: true,
        json: async () => ({
          requestStatus: { code: 100 },
          responseData: {},
        }),
      })

      await expect(
        handleObs(payload, mockLogger)
      ).resolves.not.toThrow()
    })

    it('should handle OBS API error', async () => {
      const payload = { payload: 'StartStreaming' }

      global.fetch = vi.fn().mockResolvedValue({
        ok: true,
        json: async () => ({
          requestStatus: { code: 400, comment: 'Error' },
        }),
      })

      await expect(
        handleObs(payload, mockLogger)
      ).resolves.not.toThrow()

      expect(mockLogger.error).toHaveBeenCalled()
    })

    it('should handle network error', async () => {
      const payload = { payload: 'StartStreaming' }

      global.fetch = vi.fn().mockRejectedValue(new Error('Network error'))

      await expect(
        handleObs(payload, mockLogger)
      ).resolves.not.toThrow()

      expect(mockLogger.error).toHaveBeenCalled()
    })
  })
})

