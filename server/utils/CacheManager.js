/**
 * Gestionnaire de cache pour optimiser les performances
 */

export class CacheManager {
  constructor(maxSize = 100, ttl = 300000) { // 5 minutes par défaut
    this.cache = new Map()
    this.maxSize = maxSize
    this.ttl = ttl
    this.accessOrder = [] // Pour LRU
  }

  /**
   * Obtient une valeur du cache
   */
  get(key) {
    const entry = this.cache.get(key)
    if (!entry) return null

    // Vérifier expiration
    if (Date.now() > entry.expiresAt) {
      this.cache.delete(key)
      this.accessOrder = this.accessOrder.filter(k => k !== key)
      return null
    }

    // Mettre à jour l'ordre d'accès (LRU)
    this.accessOrder = this.accessOrder.filter(k => k !== key)
    this.accessOrder.push(key)

    return entry.value
  }

  /**
   * Met une valeur dans le cache
   */
  set(key, value, customTtl = null) {
    const ttl = customTtl || this.ttl
    const expiresAt = Date.now() + ttl

    // Si le cache est plein, supprimer le moins récemment utilisé
    if (this.cache.size >= this.maxSize && !this.cache.has(key)) {
      const lruKey = this.accessOrder.shift()
      if (lruKey) {
        this.cache.delete(lruKey)
      }
    }

    this.cache.set(key, { value, expiresAt })
    this.accessOrder = this.accessOrder.filter(k => k !== key)
    this.accessOrder.push(key)
  }

  /**
   * Supprime une valeur du cache
   */
  delete(key) {
    this.cache.delete(key)
    this.accessOrder = this.accessOrder.filter(k => k !== key)
  }

  /**
   * Vide le cache
   */
  clear() {
    this.cache.clear()
    this.accessOrder = []
  }

  /**
   * Obtient la taille du cache
   */
  size() {
    return this.cache.size
  }

  /**
   * Nettoie les entrées expirées
   */
  cleanup() {
    const now = Date.now()
    for (const [key, entry] of this.cache.entries()) {
      if (now > entry.expiresAt) {
        this.cache.delete(key)
        this.accessOrder = this.accessOrder.filter(k => k !== key)
      }
    }
  }
}

/**
 * Instance globale du cache manager
 */
let globalCacheManager = null

export function getCacheManager(maxSize = 100, ttl = 300000) {
  if (!globalCacheManager) {
    globalCacheManager = new CacheManager(maxSize, ttl)

    // Nettoyer le cache toutes les minutes
    setInterval(() => {
      globalCacheManager.cleanup()
    }, 60000)
  }
  return globalCacheManager
}


