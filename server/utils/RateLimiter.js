/**
 * Rate limiter avancé avec support par client, action et IP
 */

class RateLimiter {
  constructor() {
    this.limits = new Map() // key -> { count, resetAt }
    this.configs = new Map() // identifier -> config
  }

  /**
   * Configure une limite de taux
   */
  configure(identifier, config) {
    this.configs.set(identifier, {
      windowMs: config.windowMs || 60000, // 1 minute par défaut
      max: config.max || 60, // 60 requêtes par défaut
      skipSuccessfulRequests: config.skipSuccessfulRequests || false,
      skipFailedRequests: config.skipFailedRequests || false,
    })
  }

  /**
   * Vérifie si une requête est autorisée
   */
  check(identifier, key, increment = true) {
    const config = this.configs.get(identifier) || {
      windowMs: 60000,
      max: 60,
      skipSuccessfulRequests: false,
      skipFailedRequests: false,
    }

    const fullKey = `${identifier}:${key}`
    const now = Date.now()
    const entry = this.limits.get(fullKey)

    // Nettoyer les entrées expirées
    if (entry && now > entry.resetAt) {
      this.limits.delete(fullKey)
    }

    const current = this.limits.get(fullKey)
    if (!current) {
      if (increment) {
        this.limits.set(fullKey, {
          count: 1,
          resetAt: now + config.windowMs,
        })
      }
      return { allowed: true, remaining: config.max - 1, resetAt: now + config.windowMs }
    }

    if (current.count >= config.max) {
      return {
        allowed: false,
        remaining: 0,
        resetAt: current.resetAt,
        retryAfter: Math.ceil((current.resetAt - now) / 1000),
      }
    }

    if (increment) {
      current.count++
    }

    return {
      allowed: true,
      remaining: config.max - current.count,
      resetAt: current.resetAt,
    }
  }

  /**
   * Réinitialise le compteur pour une clé
   */
  reset(identifier, key) {
    const fullKey = `${identifier}:${key}`
    this.limits.delete(fullKey)
  }

  /**
   * Réinitialise tous les compteurs pour un identifiant
   */
  resetAll(identifier) {
    for (const key of this.limits.keys()) {
      if (key.startsWith(`${identifier}:`)) {
        this.limits.delete(key)
      }
    }
  }

  /**
   * Nettoie les entrées expirées
   */
  cleanup() {
    const now = Date.now()
    for (const [key, entry] of this.limits.entries()) {
      if (now > entry.resetAt) {
        this.limits.delete(key)
      }
    }
  }

  /**
   * Obtient les statistiques
   */
  getStats(identifier = null) {
    const now = Date.now()
    let total = 0
    let active = 0
    let expired = 0

    for (const [key, entry] of this.limits.entries()) {
      if (identifier && !key.startsWith(`${identifier}:`)) {
        continue
      }
      total++
      if (now > entry.resetAt) {
        expired++
      } else {
        active++
      }
    }

    return { total, active, expired }
  }
}

// Singleton pour le serveur
let globalRateLimiter = null

export function getRateLimiter() {
  if (!globalRateLimiter) {
    globalRateLimiter = new RateLimiter()
    // Nettoyage automatique toutes les 5 minutes
    setInterval(() => globalRateLimiter.cleanup(), 5 * 60 * 1000)
  }
  return globalRateLimiter
}

export function createRateLimiter() {
  return new RateLimiter()
}

export default RateLimiter





