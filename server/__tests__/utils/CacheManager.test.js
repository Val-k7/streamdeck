import { describe, it, expect, vi, beforeEach } from '@jest/globals'
import { CacheManager, getCacheManager } from '../../utils/CacheManager.js'

describe('CacheManager', () => {
  let cache

  beforeEach(() => {
    cache = new CacheManager(5, 1000) // maxSize: 5, ttl: 1s
  })

  describe('get and set', () => {
    it('should store and retrieve values', () => {
      cache.set('key1', 'value1')

      expect(cache.get('key1')).toBe('value1')
    })

    it('should return null for non-existent keys', () => {
      expect(cache.get('nonexistent')).toBeNull()
    })

    it('should expire values after TTL', async () => {
      cache.set('key1', 'value1', 100) // 100ms TTL

      expect(cache.get('key1')).toBe('value1')

      await new Promise(resolve => setTimeout(resolve, 150))

      expect(cache.get('key1')).toBeNull()
    })

    it('should use custom TTL when provided', async () => {
      cache.set('key1', 'value1', 200) // 200ms TTL

      await new Promise(resolve => setTimeout(resolve, 150))
      expect(cache.get('key1')).toBe('value1')

      await new Promise(resolve => setTimeout(resolve, 100))
      expect(cache.get('key1')).toBeNull()
    })
  })

  describe('LRU eviction', () => {
    it('should evict least recently used when full', () => {
      // Fill cache to max size
      for (let i = 0; i < 5; i++) {
        cache.set(`key${i}`, `value${i}`)
      }

      expect(cache.size()).toBe(5)

      // Add one more - should evict least recently used
      cache.set('key5', 'value5')

      expect(cache.size()).toBe(5)
      expect(cache.get('key0')).toBeNull() // First key should be evicted
      expect(cache.get('key5')).toBe('value5')
    })

    it('should update access order on get', () => {
      cache.set('key1', 'value1')
      cache.set('key2', 'value2')
      cache.set('key3', 'value3')

      // Access key1 to make it most recently used
      cache.get('key1')

      // Fill cache
      cache.set('key4', 'value4')
      cache.set('key5', 'value5')

      // key2 should be evicted (least recently used)
      expect(cache.get('key2')).toBeNull()
      expect(cache.get('key1')).toBe('value1') // Should still be there
    })
  })

  describe('delete', () => {
    it('should delete values', () => {
      cache.set('key1', 'value1')
      cache.delete('key1')

      expect(cache.get('key1')).toBeNull()
    })
  })

  describe('clear', () => {
    it('should clear all values', () => {
      cache.set('key1', 'value1')
      cache.set('key2', 'value2')

      cache.clear()

      expect(cache.size()).toBe(0)
      expect(cache.get('key1')).toBeNull()
      expect(cache.get('key2')).toBeNull()
    })
  })

  describe('cleanup', () => {
    it('should remove expired entries', async () => {
      cache.set('key1', 'value1', 50) // Expires quickly
      cache.set('key2', 'value2', 1000) // Expires later

      await new Promise(resolve => setTimeout(resolve, 100))

      cache.cleanup()

      expect(cache.get('key1')).toBeNull()
      expect(cache.get('key2')).toBe('value2')
    })
  })

  describe('getCacheManager', () => {
    it('should return singleton instance', () => {
      const instance1 = getCacheManager()
      const instance2 = getCacheManager()

      expect(instance1).toBe(instance2)
    })
  })
})


