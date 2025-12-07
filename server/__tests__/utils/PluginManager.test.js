import { describe, it, expect, vi, beforeEach } from '@jest/globals'
import { PluginManager } from '../../plugins/PluginManager.js'
import { promises as fs } from 'fs'
import path from 'path'

// Mock fs
vi.mock('fs', () => ({
  promises: {
    readdir: vi.fn(),
    readFile: vi.fn(),
    stat: vi.fn(),
  },
}))

// Mock logger
const mockLogger = {
  info: vi.fn(),
  warn: vi.fn(),
  error: vi.fn(),
}

describe('PluginManager', () => {
  let pluginManager

  beforeEach(() => {
    vi.clearAllMocks()
    pluginManager = new PluginManager(mockLogger)
  })

  describe('loadPlugins', () => {
    it('should load plugins from directory', async () => {
      const mockPlugins = ['plugin1.js', 'plugin2.js']

      fs.promises.readdir = vi.fn().mockResolvedValue(mockPlugins)
      fs.promises.stat = vi.fn().mockResolvedValue({ isFile: () => true })
      fs.promises.readFile = vi.fn().mockResolvedValue('module.exports = {}')

      await pluginManager.loadPlugins('/plugins')

      expect(fs.promises.readdir).toHaveBeenCalled()
      expect(mockLogger.info).toHaveBeenCalled()
    })

    it('should handle empty plugin directory', async () => {
      fs.promises.readdir = vi.fn().mockResolvedValue([])

      await pluginManager.loadPlugins('/plugins')

      expect(fs.promises.readdir).toHaveBeenCalled()
    })

    it('should handle invalid plugin files', async () => {
      const mockPlugins = ['invalid.js']

      fs.promises.readdir = vi.fn().mockResolvedValue(mockPlugins)
      fs.promises.stat = vi.fn().mockResolvedValue({ isFile: () => true })
      fs.promises.readFile = vi.fn().mockResolvedValue('invalid syntax')

      await expect(
        pluginManager.loadPlugins('/plugins')
      ).resolves.not.toThrow()

      expect(mockLogger.error).toHaveBeenCalled()
    })

    it('should handle directory read error', async () => {
      fs.promises.readdir = vi.fn().mockRejectedValue(new Error('Permission denied'))

      await expect(
        pluginManager.loadPlugins('/plugins')
      ).resolves.not.toThrow()

      expect(mockLogger.error).toHaveBeenCalled()
    })
  })

  describe('getPlugin', () => {
    it('should return plugin if loaded', async () => {
      const mockPlugin = { name: 'test-plugin', execute: vi.fn() }
      pluginManager.plugins.set('test-plugin', mockPlugin)

      const plugin = pluginManager.getPlugin('test-plugin')
      expect(plugin).toBe(mockPlugin)
    })

    it('should return null if plugin not found', () => {
      const plugin = pluginManager.getPlugin('nonexistent')
      expect(plugin).toBeNull()
    })
  })

  describe('executePlugin', () => {
    it('should execute plugin action', async () => {
      const mockPlugin = {
        name: 'test-plugin',
        execute: vi.fn().mockResolvedValue({ success: true }),
      }
      pluginManager.plugins.set('test-plugin', mockPlugin)

      const result = await pluginManager.executePlugin('test-plugin', {})

      expect(mockPlugin.execute).toHaveBeenCalled()
      expect(result).toEqual({ success: true })
    })

    it('should handle plugin execution error', async () => {
      const mockPlugin = {
        name: 'test-plugin',
        execute: vi.fn().mockRejectedValue(new Error('Plugin error')),
      }
      pluginManager.plugins.set('test-plugin', mockPlugin)

      await expect(
        pluginManager.executePlugin('test-plugin', {})
      ).resolves.not.toThrow()

      expect(mockLogger.error).toHaveBeenCalled()
    })

    it('should handle nonexistent plugin', async () => {
      await expect(
        pluginManager.executePlugin('nonexistent', {})
      ).resolves.not.toThrow()

      expect(mockLogger.warn).toHaveBeenCalled()
    })
  })
})

