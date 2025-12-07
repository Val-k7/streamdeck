/**
 * Gestionnaire d'erreurs centralisé pour le serveur
 *
 * Fournit une gestion cohérente des erreurs avec logging, classification et réponses standardisées
 */

export class ErrorHandler {
  constructor(logger) {
    this.logger = logger
    this.errorCounts = new Map() // type -> count
    this.errorHistory = [] // Dernières erreurs
    this.maxHistorySize = 100
  }

  /**
   * Gère une erreur et retourne une réponse standardisée
   *
   * @param {Error} error - L'erreur à gérer
   * @param {object} context - Contexte supplémentaire
   * @param {string} context.action - Action qui a échoué
   * @param {string} context.clientId - ID du client
   * @param {object} context.payload - Payload de la requête
   * @returns {object} Réponse d'erreur standardisée
   */
  handleError(error, context = {}) {
    const errorType = this.classifyError(error)
    const errorId = this.generateErrorId()

    // Incrémenter le compteur
    this.errorCounts.set(errorType, (this.errorCounts.get(errorType) || 0) + 1)

    // Ajouter à l'historique
    this.addToHistory({
      id: errorId,
      type: errorType,
      message: error.message,
      stack: error.stack,
      context,
      timestamp: new Date().toISOString(),
    })

    // Logger selon le type
    this.logError(error, errorType, context)

    // Retourner une réponse standardisée
    return this.createErrorResponse(error, errorType, errorId, context)
  }

  /**
   * Classe une erreur selon son type
   */
  classifyError(error) {
    if (error.name === 'ValidationError') return 'validation'
    if (error.name === 'AuthenticationError') return 'authentication'
    if (error.name === 'AuthorizationError') return 'authorization'
    if (error.name === 'RateLimitError') return 'rate_limit'
    if (error.name === 'TimeoutError') return 'timeout'
    if (error.name === 'NetworkError') return 'network'
    if (error.name === 'PluginError') return 'plugin'
    if (error.message?.includes('not found')) return 'not_found'
    if (error.message?.includes('permission')) return 'permission'
    if (error.message?.includes('timeout')) return 'timeout'
    return 'unknown'
  }

  /**
   * Génère un ID unique pour l'erreur
   */
  generateErrorId() {
    return `err_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`
  }

  /**
   * Ajoute une erreur à l'historique
   */
  addToHistory(errorInfo) {
    this.errorHistory.push(errorInfo)
    if (this.errorHistory.length > this.maxHistorySize) {
      this.errorHistory.shift()
    }
  }

  /**
   * Log une erreur selon son type
   */
  logError(error, errorType, context) {
    const logData = {
      type: errorType,
      message: error.message,
      action: context.action,
      clientId: context.clientId,
    }

    switch (errorType) {
      case 'validation':
        this.logger.warn('Validation error', logData)
        break
      case 'authentication':
      case 'authorization':
        this.logger.warn('Auth error', logData)
        break
      case 'rate_limit':
        this.logger.warn('Rate limit exceeded', logData)
        break
      case 'timeout':
        this.logger.warn('Timeout error', logData)
        break
      case 'network':
        this.logger.error('Network error', logData)
        break
      case 'plugin':
        this.logger.error('Plugin error', logData)
        break
      default:
        this.logger.error('Unknown error', { ...logData, stack: error.stack })
    }
  }

  /**
   * Crée une réponse d'erreur standardisée
   */
  createErrorResponse(error, errorType, errorId, context) {
    const baseResponse = {
      success: false,
      error: {
        id: errorId,
        type: errorType,
        message: this.getUserFriendlyMessage(error, errorType),
        timestamp: new Date().toISOString(),
      },
    }

    // Ajouter des détails selon le type
    switch (errorType) {
      case 'validation':
        baseResponse.error.details = error.details || {}
        baseResponse.error.field = error.field
        break
      case 'rate_limit':
        baseResponse.error.retryAfter = error.retryAfter
        break
      case 'timeout':
        baseResponse.error.timeout = error.timeout
        break
    }

    // En mode développement, ajouter la stack
    if (process.env.NODE_ENV === 'development') {
      baseResponse.error.stack = error.stack
      baseResponse.error.originalMessage = error.message
    }

    return baseResponse
  }

  /**
   * Retourne un message utilisateur-friendly
   */
  getUserFriendlyMessage(error, errorType) {
    const messages = {
      validation: 'Les données fournies sont invalides',
      authentication: 'Authentification requise',
      authorization: 'Accès non autorisé',
      rate_limit: 'Trop de requêtes, veuillez réessayer plus tard',
      timeout: 'La requête a expiré',
      network: 'Erreur de connexion réseau',
      plugin: 'Erreur dans le plugin',
      not_found: 'Ressource non trouvée',
      permission: 'Permissions insuffisantes',
      unknown: 'Une erreur inattendue s\'est produite',
    }

    return messages[errorType] || messages.unknown
  }

  /**
   * Obtient les statistiques d'erreurs
   */
  getErrorStats() {
    return {
      counts: Object.fromEntries(this.errorCounts),
      recent: this.errorHistory.slice(-10),
      total: this.errorHistory.length,
    }
  }

  /**
   * Réinitialise les statistiques
   */
  resetStats() {
    this.errorCounts.clear()
    this.errorHistory = []
  }

  /**
   * Crée une erreur typée
   */
  static createError(type, message, details = {}) {
    const error = new Error(message)
    error.name = `${type}Error`
    error.type = type
    Object.assign(error, details)
    return error
  }

  /**
   * Wrapper pour async functions avec gestion d'erreur
   */
  async wrapAsync(fn, context = {}) {
    try {
      return await fn()
    } catch (error) {
      return this.handleError(error, context)
    }
  }
}

/**
 * Erreurs personnalisées
 */
export class ValidationError extends Error {
  constructor(message, field, details = {}) {
    super(message)
    this.name = 'ValidationError'
    this.field = field
    this.details = details
  }
}

export class AuthenticationError extends Error {
  constructor(message = 'Authentication required') {
    super(message)
    this.name = 'AuthenticationError'
  }
}

export class AuthorizationError extends Error {
  constructor(message = 'Access denied') {
    super(message)
    this.name = 'AuthorizationError'
  }
}

export class RateLimitError extends Error {
  constructor(message = 'Rate limit exceeded', retryAfter = 60) {
    super(message)
    this.name = 'RateLimitError'
    this.retryAfter = retryAfter
  }
}

export class TimeoutError extends Error {
  constructor(message = 'Operation timeout', timeout = 30000) {
    super(message)
    this.name = 'TimeoutError'
    this.timeout = timeout
  }
}

export class PluginError extends Error {
  constructor(pluginName, message, details = {}) {
    super(`Plugin ${pluginName}: ${message}`)
    this.name = 'PluginError'
    this.pluginName = pluginName
    this.details = details
  }
}





