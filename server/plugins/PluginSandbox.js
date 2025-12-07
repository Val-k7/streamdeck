import { EventEmitter } from 'events'
import vm from 'vm'

/**
 * Sandbox pour isoler et sécuriser l'exécution des plugins
 */
export class PluginSandbox extends EventEmitter {
  constructor(pluginName, logger) {
    super()
    this.pluginName = pluginName
    this.logger = logger
    this.isolated = false
    this.timeout = 30000 // 30 secondes par défaut
    this.memoryLimit = 50 * 1024 * 1024 // 50 MB par défaut
    this.errorCount = 0
    this.maxErrors = 10
    this.lastError = null
    this.startTime = null
  }

  /**
   * Exécute du code dans un contexte isolé
   */
  executeIsolated(code, context = {}) {
    if (this.errorCount >= this.maxErrors) {
      throw new Error(`Plugin ${this.pluginName} has exceeded maximum error count`)
    }

    this.startTime = Date.now()

    try {
      // Créer un contexte isolé
      const sandboxContext = {
        console: {
          log: (...args) => this.logger?.info(`[${this.pluginName}]`, ...args),
          error: (...args) => this.logger?.error(`[${this.pluginName}]`, ...args),
          warn: (...args) => this.logger?.warn(`[${this.pluginName}]`, ...args),
          info: (...args) => this.logger?.info(`[${this.pluginName}]`, ...args),
        },
        setTimeout: (fn, delay) => {
          // Limiter les timeouts
          if (delay > 60000) {
            throw new Error('Timeout delay exceeds maximum (60s)')
          }
          return setTimeout(fn, Math.min(delay, 60000))
        },
        setInterval: (fn, delay) => {
          // Limiter les intervals
          if (delay > 60000) {
            throw new Error('Interval delay exceeds maximum (60s)')
          }
          return setInterval(fn, Math.min(delay, 60000))
        },
        clearTimeout,
        clearInterval,
        Buffer: Buffer,
        process: {
          env: {},
          version: process.version,
          platform: process.platform,
          arch: process.arch,
        },
        ...context,
      }

      // Créer le script avec timeout
      const script = new vm.Script(code, {
        timeout: this.timeout,
        displayErrors: true,
      })

      // Exécuter dans le contexte isolé
      const result = script.runInNewContext(sandboxContext, {
        timeout: this.timeout,
        displayErrors: true,
      })

      this.errorCount = 0 // Réinitialiser le compteur en cas de succès
      return result
    } catch (error) {
      this.errorCount++
      this.lastError = error
      this.emit('error', error)

      if (this.errorCount >= this.maxErrors) {
        this.emit('maxErrorsReached', this.errorCount)
      }

      throw new Error(`Plugin ${this.pluginName} execution error: ${error.message}`)
    }
  }

  /**
   * Enveloppe une fonction pour l'exécuter de manière sécurisée
   */
  wrapFunction(fn, options = {}) {
    const timeout = options.timeout || this.timeout
    const maxRetries = options.maxRetries || 3

    return async (...args) => {
      let lastError = null

      for (let attempt = 0; attempt < maxRetries; attempt++) {
        try {
          // Exécuter avec timeout
          const result = await Promise.race([
            Promise.resolve(fn(...args)),
            new Promise((_, reject) =>
              setTimeout(() => reject(new Error('Function timeout')), timeout)
            ),
          ])

          return result
        } catch (error) {
          lastError = error
          this.logger?.warn(
            `Plugin ${this.pluginName} function error (attempt ${attempt + 1}/${maxRetries}):`,
            error.message
          )

          if (attempt < maxRetries - 1) {
            // Attendre avant de réessayer
            await new Promise((resolve) => setTimeout(resolve, 1000 * (attempt + 1)))
          }
        }
      }

      this.errorCount++
      throw new Error(
        `Plugin ${this.pluginName} function failed after ${maxRetries} attempts: ${lastError?.message}`
      )
    }
  }

  /**
   * Valide les entrées pour prévenir les injections
   */
  validateInput(input, schema) {
    if (!schema) return input

    const validated = {}

    for (const [key, validator] of Object.entries(schema)) {
      if (key in input) {
        const value = input[key]

        if (validator.type === 'string') {
          if (typeof value !== 'string') {
            throw new Error(`Invalid type for ${key}: expected string`)
          }
          if (validator.maxLength && value.length > validator.maxLength) {
            throw new Error(`Value too long for ${key}: max ${validator.maxLength} characters`)
          }
          // Sanitize strings
          validated[key] = this.sanitizeString(value)
        } else if (validator.type === 'number') {
          if (typeof value !== 'number') {
            throw new Error(`Invalid type for ${key}: expected number`)
          }
          if (validator.min !== undefined && value < validator.min) {
            throw new Error(`Value too small for ${key}: min ${validator.min}`)
          }
          if (validator.max !== undefined && value > validator.max) {
            throw new Error(`Value too large for ${key}: max ${validator.max}`)
          }
          validated[key] = value
        } else if (validator.type === 'boolean') {
          validated[key] = Boolean(value)
        } else if (validator.type === 'array') {
          if (!Array.isArray(value)) {
            throw new Error(`Invalid type for ${key}: expected array`)
          }
          if (validator.maxItems && value.length > validator.maxItems) {
            throw new Error(`Array too large for ${key}: max ${validator.maxItems} items`)
          }
          validated[key] = value.map((item) => this.validateInput(item, validator.items))
        } else if (validator.type === 'object') {
          if (typeof value !== 'object' || Array.isArray(value)) {
            throw new Error(`Invalid type for ${key}: expected object`)
          }
          validated[key] = this.validateInput(value, validator.properties)
        } else {
          validated[key] = value
        }
      } else if (validator.required) {
        throw new Error(`Required field missing: ${key}`)
      }
    }

    return validated
  }

  /**
   * Nettoie une chaîne pour prévenir les injections
   */
  sanitizeString(str) {
    if (typeof str !== 'string') return str

    // Retirer les caractères dangereux
    return str
      .replace(/[<>]/g, '') // Retirer < et >
      .replace(/javascript:/gi, '') // Retirer javascript:
      .replace(/on\w+=/gi, '') // Retirer les handlers d'événements
      .trim()
  }

  /**
   * Limite l'utilisation de la mémoire
   */
  checkMemoryUsage() {
    const usage = process.memoryUsage()
    const heapUsed = usage.heapUsed

    if (heapUsed > this.memoryLimit) {
      throw new Error(
        `Memory limit exceeded: ${(heapUsed / 1024 / 1024).toFixed(2)}MB > ${(this.memoryLimit / 1024 / 1024).toFixed(2)}MB`
      )
    }
  }

  /**
   * Réinitialise le sandbox
   */
  reset() {
    this.errorCount = 0
    this.lastError = null
    this.startTime = null
  }

  /**
   * Obtient les statistiques du sandbox
   */
  getStats() {
    return {
      pluginName: this.pluginName,
      errorCount: this.errorCount,
      maxErrors: this.maxErrors,
      lastError: this.lastError?.message,
      timeout: this.timeout,
      memoryLimit: this.memoryLimit,
      isolated: this.isolated,
    }
  }
}





