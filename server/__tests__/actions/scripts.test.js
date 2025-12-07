import { describe, it, expect, vi, beforeEach, afterEach } from '@jest/globals'
import { handleScript } from '../../actions/scripts.js'
import { promises as fs } from 'fs'
import { exec } from 'child_process'
import { promisify } from 'util'

const execAsync = promisify(exec)

// Mock logger
const mockLogger = {
  info: vi.fn(),
  warn: vi.fn(),
  error: vi.fn(),
}

// Mock fs
vi.mock('fs', () => ({
  promises: {
    access: vi.fn(),
    readFile: vi.fn(),
  },
}))

// Mock child_process
vi.mock('child_process', () => ({
  exec: vi.fn(),
}))

describe('Script Actions', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  describe('handleScript', () => {
    it('should execute a valid script command', async () => {
      const command = 'echo "test"'

      exec.mockImplementation((cmd, callback) => {
        callback(null, { stdout: 'test\n', stderr: '' })
      })

      const result = await handleScript(command)

      expect(result).toBe('test\n')
      expect(exec).toHaveBeenCalledWith(command, expect.any(Function))
    })

    it('should handle script execution error', async () => {
      const command = 'invalid-command'

      exec.mockImplementation((cmd, callback) => {
        callback(new Error('Command not found'), { stdout: '', stderr: 'error' })
      })

      await expect(
        handleScript(command)
      ).rejects.toThrow()

      expect(mockLogger.error).toHaveBeenCalled()
    })

    it('should handle stderr output', async () => {
      const command = 'echo "test" >&2'

      exec.mockImplementation((cmd, callback) => {
        callback(null, { stdout: '', stderr: 'test\n' })
      })

      await expect(
        handleScript(command)
      ).resolves.toBe('')

      expect(mockLogger.warn).toHaveBeenCalled()
    })
  })
})

