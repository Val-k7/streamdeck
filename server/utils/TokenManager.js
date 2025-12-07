/**
 * Gestionnaire de tokens avec expiration, rotation et révocation
 */

import crypto from 'crypto'

class TokenManager {
  constructor(defaultTtl = 24 * 60 * 60 * 1000) {
    // defaultTtl: 24 heures par défaut
    this.tokens = new Map() // token -> { issuedAt, expiresAt, clientId, metadata }
    this.revokedTokens = new Set() // Tokens révoqués
    this.defaultTtl = defaultTtl
    this.cleanupInterval = setInterval(() => this.cleanup(), 60 * 60 * 1000) // Nettoyage toutes les heures
  }

  /**
   * Génère un nouveau token
   */
  issueToken(clientId = null, metadata = {}, customTtl = null) {
    const token = this.generateToken()
    const now = Date.now()
    const ttl = customTtl || this.defaultTtl
    const expiresAt = now + ttl

    this.tokens.set(token, {
      issuedAt: now,
      expiresAt,
      clientId,
      metadata,
      lastUsed: now,
    })

    return {
      token,
      expiresAt,
      expiresIn: ttl,
    }
  }

  /**
   * Vérifie si un token est valide
   */
  isValid(token) {
    if (!token) return false

    // Vérifier si le token est révoqué
    if (this.revokedTokens.has(token)) {
      return false
    }

    const tokenData = this.tokens.get(token)
    if (!tokenData) {
      return false
    }

    // Vérifier l'expiration
    if (Date.now() > tokenData.expiresAt) {
      this.tokens.delete(token)
      return false
    }

    // Mettre à jour la dernière utilisation
    tokenData.lastUsed = Date.now()

    return true
  }

  /**
   * Révoke un token
   */
  revokeToken(token) {
    if (this.tokens.has(token)) {
      this.tokens.delete(token)
    }
    this.revokedTokens.add(token)
  }

  /**
   * Révoke tous les tokens d'un client
   */
  revokeClientTokens(clientId) {
    for (const [token, data] of this.tokens.entries()) {
      if (data.clientId === clientId) {
        this.tokens.delete(token)
        this.revokedTokens.add(token)
      }
    }
  }

  /**
   * Révoke tous les tokens expirés
   */
  revokeExpiredTokens() {
    const now = Date.now()
    for (const [token, data] of this.tokens.entries()) {
      if (now > data.expiresAt) {
        this.tokens.delete(token)
        this.revokedTokens.add(token)
      }
    }
  }

  /**
   * Renouvelle un token (rotation)
   */
  rotateToken(oldToken, clientId = null, metadata = {}) {
    if (!this.isValid(oldToken)) {
      throw new Error('Invalid token for rotation')
    }

    const oldTokenData = this.tokens.get(oldToken)
    const newToken = this.issueToken(
      clientId || oldTokenData.clientId,
      { ...oldTokenData.metadata, ...metadata },
      oldTokenData.expiresAt - Date.now()
    )

    // Révoker l'ancien token
    this.revokeToken(oldToken)

    return newToken
  }

  /**
   * Obtient les informations sur un token
   */
  getTokenInfo(token) {
    if (!this.isValid(token)) {
      return null
    }

    const data = this.tokens.get(token)
    return {
      issuedAt: data.issuedAt,
      expiresAt: data.expiresAt,
      clientId: data.clientId,
      metadata: data.metadata,
      lastUsed: data.lastUsed,
      expiresIn: data.expiresAt - Date.now(),
    }
  }

  /**
   * Nettoie les tokens expirés et les anciennes révocations
   */
  cleanup() {
    const now = Date.now()

    // Supprimer les tokens expirés
    for (const [token, data] of this.tokens.entries()) {
      if (now > data.expiresAt) {
        this.tokens.delete(token)
      }
    }

    // Nettoyer les anciennes révocations (garder seulement les 1000 dernières)
    if (this.revokedTokens.size > 1000) {
      const tokensArray = Array.from(this.revokedTokens)
      this.revokedTokens = new Set(tokensArray.slice(-1000))
    }
  }

  /**
   * Génère un token aléatoire sécurisé
   */
  generateToken() {
    return crypto.randomBytes(32).toString('hex')
  }

  /**
   * Obtient les statistiques
   */
  getStats() {
    const now = Date.now()
    let active = 0
    let expired = 0

    for (const data of this.tokens.values()) {
      if (now > data.expiresAt) {
        expired++
      } else {
        active++
      }
    }

    return {
      active,
      expired,
      revoked: this.revokedTokens.size,
      total: this.tokens.size,
    }
  }

  /**
   * Ferme le gestionnaire (arrête le nettoyage automatique)
   */
  close() {
    if (this.cleanupInterval) {
      clearInterval(this.cleanupInterval)
      this.cleanupInterval = null
    }
    this.tokens.clear()
    this.revokedTokens.clear()
  }
}

// Singleton pour le serveur
let globalTokenManager = null

export function getTokenManager(defaultTtl = 24 * 60 * 60 * 1000) {
  if (!globalTokenManager) {
    globalTokenManager = new TokenManager(defaultTtl)
  }
  return globalTokenManager
}

export function createTokenManager(defaultTtl = 24 * 60 * 60 * 1000) {
  return new TokenManager(defaultTtl)
}

export default TokenManager


