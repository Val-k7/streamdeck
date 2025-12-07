import { describe, it, expect, vi, beforeEach } from '@jest/globals'
import * as ProcessWindows from '../../actions/processes.js'

// Mock logger
const mockLogger = {
  info: vi.fn(),
  warn: vi.fn(),
  error: vi.fn(),
}

// Mock child_process
vi.mock('child_process', () => ({
  exec: vi.fn((cmd, callback) => {
    callback(null, { stdout: 'process output', stderr: '' })
  }),
}))

describe('Process Actions', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('listProcessesWindows', () => {
    it('should list running processes', async () => {
      const { exec } = await import('child_process')
      exec.mockImplementationOnce((cmd, callback) => {
        callback(null, {
          stdout: JSON.stringify([{ Id: 1, ProcessName: 'test', CPU: 0, WorkingSet: 1000 }]),
          stderr: ''
        })
      })

      const processes = await ProcessWindows.listProcessesWindows()

      expect(Array.isArray(processes)).toBe(true)
    })

    it('should handle list error', async () => {
      const { exec } = await import('child_process')
      exec.mockImplementationOnce((cmd, callback) => {
        callback(new Error('List failed'), { stdout: '', stderr: 'error' })
      })

      await expect(
        ProcessWindows.listProcessesWindows()
      ).rejects.toThrow()
    })
  })

  describe('startProcessWindows', () => {
    it('should start a process', async () => {
      const { exec } = await import('child_process')
      exec.mockImplementationOnce((cmd, callback) => {
        callback(null, { stdout: '', stderr: '' })
      })

      await expect(
        ProcessWindows.startProcessWindows('notepad.exe')
      ).resolves.not.toThrow()
    })

    it('should start process with arguments', async () => {
      await expect(
        ProcessWindows.startProcessWindows('notepad.exe', ['file.txt'])
      ).resolves.not.toThrow()
    })
  })

  describe('stopProcessWindows', () => {
    it('should stop a process', async () => {
      await expect(
        ProcessWindows.stopProcessWindows('notepad.exe')
      ).resolves.not.toThrow()
    })
  })
})

