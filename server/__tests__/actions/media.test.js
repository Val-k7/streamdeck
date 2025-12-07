import { describe, it, expect, vi, beforeEach } from '@jest/globals'
import { handleMediaKeys } from '../../actions/media-windows.js'

// Mock logger
const mockLogger = {
  info: vi.fn(),
  warn: vi.fn(),
  error: vi.fn(),
}

// Mock child_process
vi.mock('child_process', () => ({
  exec: vi.fn((cmd, callback) => {
    callback(null, { stdout: '', stderr: '' })
  }),
}))

describe('Media Actions', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('handleMediaKeys', () => {
    it('should handle PlayPause action', async () => {
      const payload = { payload: 'PlayPause' }

      await expect(
        handleMediaKeys(payload, mockLogger)
      ).resolves.not.toThrow()
    })

    it('should handle NextTrack action', async () => {
      const payload = { payload: 'NextTrack' }

      await expect(
        handleMediaKeys(payload, mockLogger)
      ).resolves.not.toThrow()
    })

    it('should handle PreviousTrack action', async () => {
      const payload = { payload: 'PreviousTrack' }

      await expect(
        handleMediaKeys(payload, mockLogger)
      ).resolves.not.toThrow()
    })

    it('should handle VolumeUp action', async () => {
      const payload = { payload: 'VolumeUp' }

      await expect(
        handleMediaKeys(payload, mockLogger)
      ).resolves.not.toThrow()
    })

    it('should handle VolumeDown action', async () => {
      const payload = { payload: 'VolumeDown' }

      await expect(
        handleMediaKeys(payload, mockLogger)
      ).resolves.not.toThrow()
    })

    it('should handle Mute action', async () => {
      const payload = { payload: 'Mute' }

      await expect(
        handleMediaKeys(payload, mockLogger)
      ).resolves.not.toThrow()
    })

    it('should handle invalid action gracefully', async () => {
      const payload = { payload: 'InvalidAction' }

      await expect(
        handleMediaKeys(payload, mockLogger)
      ).resolves.not.toThrow()
    })
  })
})


