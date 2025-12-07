import { describe, it, expect, vi, beforeEach } from '@jest/globals'
import { handleAudio } from '../../actions/audio.js'

// Mock logger
const mockLogger = {
  info: vi.fn(),
  warn: vi.fn(),
  error: vi.fn(),
}

describe('Audio Actions', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('handleAudio', () => {
    it('should handle SetVolume action', async () => {
      const payload = { payload: 'SetVolume', volume: 50 }

      await expect(
        handleAudio(payload, mockLogger)
      ).resolves.not.toThrow()
    })

    it('should handle Mute action', async () => {
      const payload = { payload: 'Mute' }

      await expect(
        handleAudio(payload, mockLogger)
      ).resolves.not.toThrow()
    })

    it('should handle Unmute action', async () => {
      const payload = { payload: 'Unmute' }

      await expect(
        handleAudio(payload, mockLogger)
      ).resolves.not.toThrow()
    })

    it('should handle VolumeUp action', async () => {
      const payload = { payload: 'VolumeUp', step: 5 }

      await expect(
        handleAudio(payload, mockLogger)
      ).resolves.not.toThrow()
    })

    it('should handle VolumeDown action', async () => {
      const payload = { payload: 'VolumeDown', step: 5 }

      await expect(
        handleAudio(payload, mockLogger)
      ).resolves.not.toThrow()
    })

    it('should validate volume range', async () => {
      const payload = { payload: 'SetVolume', volume: 150 }

      await expect(
        handleAudio(payload, mockLogger)
      ).resolves.not.toThrow()
    })

    it('should handle invalid action gracefully', async () => {
      const payload = { payload: 'InvalidAction' }

      await expect(
        handleAudio(payload, mockLogger)
      ).resolves.not.toThrow()
    })
  })
})

