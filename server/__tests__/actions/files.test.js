import { describe, it, expect, vi, beforeEach } from '@jest/globals'
import * as FilesWindows from '../../actions/files-windows.js'

// Mock logger
const mockLogger = {
  info: vi.fn(),
  warn: vi.fn(),
  error: vi.fn(),
}

// Mock fs
vi.mock('fs', () => ({
  promises: {
    readFile: vi.fn().mockResolvedValue('file content'),
    writeFile: vi.fn().mockResolvedValue(undefined),
    readdir: vi.fn().mockResolvedValue(['file1.txt', 'file2.txt']),
    mkdir: vi.fn().mockResolvedValue(undefined),
    stat: vi.fn().mockResolvedValue({ isFile: () => true, isDirectory: () => false }),
  },
  existsSync: vi.fn().mockReturnValue(true),
}))

// Mock path
vi.mock('path', () => ({
  default: {
    join: vi.fn((...args) => args.join('/')),
    resolve: vi.fn((...args) => args.join('/')),
    dirname: vi.fn((p) => p.split('/').slice(0, -1).join('/')),
  },
}))

describe('Files Actions', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('openFileWindows', () => {
    it('should open a file', async () => {
      const result = await FilesWindows.openFileWindows('/path/to/file.txt')

      expect(result).toHaveProperty('success', true)
      expect(result).toHaveProperty('filePath')
    })

    it('should handle file not found', async () => {
      const { existsSync } = await import('fs')
      existsSync.mockReturnValueOnce(false)

      await expect(
        FilesWindows.openFileWindows('/nonexistent/file.txt')
      ).rejects.toThrow()
    })
  })

  describe('createFileWindows', () => {
    it('should create a file', async () => {
      const result = await FilesWindows.createFileWindows('/path/to/file.txt', 'content')

      expect(result).toHaveProperty('success', true)
      expect(result).toHaveProperty('filePath')
    })

    it('should create file with empty content', async () => {
      await expect(
        FilesWindows.createFileWindows('/path/to/file.txt')
      ).resolves.not.toThrow()
    })
  })

  describe('readFileWindows', () => {
    it('should read a file', async () => {
      const result = await FilesWindows.readFileWindows('/path/to/file.txt')

      expect(result).toHaveProperty('success', true)
      expect(result).toHaveProperty('content')
    })

    it('should handle file read error', async () => {
      const { readFile } = await import('fs/promises')
      readFile.mockRejectedValueOnce(new Error('Read failed'))

      await expect(
        FilesWindows.readFileWindows('/invalid/file.txt')
      ).rejects.toThrow()
    })
  })

  describe('writeFileWindows', () => {
    it('should write to a file', async () => {
      await expect(
        FilesWindows.writeFileWindows('/path/to/file.txt', 'content')
      ).resolves.not.toThrow()
    })
  })
})

