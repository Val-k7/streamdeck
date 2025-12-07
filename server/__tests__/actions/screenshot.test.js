import { describe, it, expect, vi, beforeEach } from '@jest/globals'
import * as ScreenshotWindows from '../../actions/screenshot-windows.js'

// Mock logger
const mockLogger = {
  info: vi.fn(),
  warn: vi.fn(),
  error: vi.fn(),
}

// Mock fs
vi.mock('fs', () => ({
  promises: {
    writeFile: vi.fn().mockResolvedValue(undefined),
    mkdir: vi.fn().mockResolvedValue(undefined),
  },
  existsSync: vi.fn().mockReturnValue(true),
}))

// Mock child_process
vi.mock('child_process', () => ({
  exec: vi.fn((cmd, callback) => {
    callback(null, { stdout: '', stderr: '' })
  }),
}))

describe('Screenshot Actions', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('captureFullScreen', () => {
    it('should capture full screen', async () => {
      const result = await ScreenshotWindows.captureFullScreen('/tmp/screenshot.png')

      expect(result).toHaveProperty('success', true)
      expect(result).toHaveProperty('path')
    })

    it('should use default path if not provided', async () => {
      const result = await ScreenshotWindows.captureFullScreen()

      expect(result).toHaveProperty('success', true)
      expect(result).toHaveProperty('path')
    })

    it('should handle screenshot error', async () => {
      const { exec } = await import('child_process')
      exec.mockImplementationOnce((cmd, callback) => {
        callback(new Error('Screenshot failed'), { stdout: '', stderr: 'error' })
      })

      await expect(
        ScreenshotWindows.captureFullScreen('/tmp/test.png')
      ).rejects.toThrow()
    })
  })
})

