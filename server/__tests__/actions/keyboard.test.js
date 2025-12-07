import { describe, it, expect, beforeEach, jest } from '@jest/globals'
import { handleKeyboard } from '../../actions/keyboard.js'
import { exec } from 'child_process'
import { promisify } from 'util'
import os from 'os'

// Mock child_process
jest.mock('child_process')
const execAsync = promisify(exec)

describe('handleKeyboard', () => {
  beforeEach(() => {
    jest.clearAllMocks()
  })

  it('should throw error for non-string payload', async () => {
    await expect(handleKeyboard({ action: 'test' })).rejects.toThrow(
      'Keyboard payload must be a string'
    )
  })

  it('should parse key combination correctly', async () => {
    if (os.platform() === 'win32') {
      execAsync.mockResolvedValue({ stdout: '', stderr: '' })

      await handleKeyboard('CTRL+SHIFT+S')

      expect(execAsync).toHaveBeenCalled()
    }
  })

  it('should handle single key', async () => {
    if (os.platform() === 'win32') {
      execAsync.mockResolvedValue({ stdout: '', stderr: '' })

      await handleKeyboard('ENTER')

      expect(execAsync).toHaveBeenCalled()
    }
  })

  it('should throw error for unsupported platform', async () => {
    const originalPlatform = os.platform
    Object.defineProperty(os, 'platform', {
      value: () => 'unsupported',
      configurable: true
    })

    await expect(handleKeyboard('CTRL+S')).rejects.toThrow(
      'Unsupported platform'
    )

    Object.defineProperty(os, 'platform', {
      value: originalPlatform,
      configurable: true
    })
  })
})


