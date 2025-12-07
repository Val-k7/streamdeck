import { describe, it, expect, vi, beforeEach } from '@jest/globals'
import * as ClipboardWindows from '../../actions/clipboard-windows.js'

// Mock logger
const mockLogger = {
  info: vi.fn(),
  warn: vi.fn(),
  error: vi.fn(),
}

// Mock child_process
vi.mock('child_process', () => ({
  exec: vi.fn((cmd, callback) => {
    callback(null, { stdout: 'test output', stderr: '' })
  }),
}))

describe('Clipboard Actions', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('readClipboard', () => {
    it('should read text from clipboard', async () => {
      const result = await ClipboardWindows.readClipboard()

      expect(result).toHaveProperty('success', true)
      expect(result).toHaveProperty('text')
    })

    it('should handle clipboard read error', async () => {
      const { exec } = await import('child_process')
      exec.mockImplementationOnce((cmd, callback) => {
        callback(new Error('Clipboard error'), { stdout: '', stderr: 'error' })
      })

      await expect(
        ClipboardWindows.readClipboard()
      ).rejects.toThrow()
    })
  })

  describe('writeClipboard', () => {
    it('should write text to clipboard', async () => {
      await expect(
        ClipboardWindows.writeClipboard('Test text')
      ).resolves.not.toThrow()
    })

    it('should handle clipboard write error', async () => {
      const { exec } = await import('child_process')
      exec.mockImplementationOnce((cmd, callback) => {
        callback(new Error('Clipboard error'), { stdout: '', stderr: 'error' })
      })

      await expect(
        ClipboardWindows.writeClipboard('Test')
      ).rejects.toThrow()
    })
  })
})

