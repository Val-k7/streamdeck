/**
 * Validateur de configuration pour le serveur
 *
 * Valide la configuration du serveur et des plugins
 */

import Ajv from 'ajv'
import addFormats from 'ajv-formats'

const ajv = new Ajv({ allErrors: true, strict: false })
addFormats(ajv)

/**
 * Schéma de configuration du serveur
 */
const serverConfigSchema = {
  type: 'object',
  properties: {
    port: {
      type: 'integer',
      minimum: 1,
      maximum: 65535,
      default: 3000,
    },
    host: {
      type: 'string',
      default: '0.0.0.0',
    },
    ssl: {
      type: 'object',
      properties: {
        enabled: { type: 'boolean', default: false },
        key: { type: 'string' },
        cert: { type: 'string' },
      },
      required: ['enabled'],
    },
    authentication: {
      type: 'object',
      properties: {
        enabled: { type: 'boolean', default: false },
        tokenTtl: { type: 'integer', minimum: 1, default: 86400000 },
      },
      required: ['enabled'],
    },
    rateLimiting: {
      type: 'object',
      properties: {
        enabled: { type: 'boolean', default: true },
        windowMs: { type: 'integer', minimum: 1000, default: 60000 },
        maxRequests: { type: 'integer', minimum: 1, default: 100 },
      },
      required: ['enabled'],
    },
    logging: {
      type: 'object',
      properties: {
        level: {
          type: 'string',
          enum: ['error', 'warn', 'info', 'debug'],
          default: 'info',
        },
        file: { type: 'string' },
        console: { type: 'boolean', default: true },
      },
    },
    plugins: {
      type: 'object',
      properties: {
        directory: { type: 'string', default: './plugins' },
        autoLoad: { type: 'boolean', default: true },
      },
    },
  },
  required: ['port'],
  additionalProperties: false,
}

/**
 * Validateur de configuration
 */
export class ConfigValidator {
  constructor() {
    this.validator = ajv.compile(serverConfigSchema)
  }

  /**
   * Valide la configuration du serveur
   *
   * @param {object} config - Configuration à valider
   * @returns {object} { valid: boolean, errors: array, normalized: object }
   */
  validateServerConfig(config) {
    const valid = this.validator(config)

    if (!valid) {
      return {
        valid: false,
        errors: this.validator.errors,
        normalized: null,
      }
    }

    // Normaliser la configuration avec les valeurs par défaut
    const normalized = this.normalizeConfig(config)

    return {
      valid: true,
      errors: [],
      normalized,
    }
  }

  /**
   * Normalise la configuration avec les valeurs par défaut
   */
  normalizeConfig(config) {
    return {
      port: config.port || 3000,
      host: config.host || '0.0.0.0',
      ssl: {
        enabled: config.ssl?.enabled || false,
        key: config.ssl?.key,
        cert: config.ssl?.cert,
      },
      authentication: {
        enabled: config.authentication?.enabled || false,
        tokenTtl: config.authentication?.tokenTtl || 86400000,
      },
      rateLimiting: {
        enabled: config.rateLimiting?.enabled !== false,
        windowMs: config.rateLimiting?.windowMs || 60000,
        maxRequests: config.rateLimiting?.maxRequests || 100,
      },
      logging: {
        level: config.logging?.level || 'info',
        file: config.logging?.file,
        console: config.logging?.console !== false,
      },
      plugins: {
        directory: config.plugins?.directory || './plugins',
        autoLoad: config.plugins?.autoLoad !== false,
      },
    }
  }

  /**
   * Valide la configuration d'un plugin
   *
   * @param {object} pluginConfig - Configuration du plugin
   * @param {object} schema - Schéma de validation (JSON Schema)
   * @returns {object} { valid: boolean, errors: array }
   */
  validatePluginConfig(pluginConfig, schema) {
    if (!schema) {
      return { valid: true, errors: [] }
    }

    const validate = ajv.compile(schema)
    const valid = validate(pluginConfig)

    return {
      valid,
      errors: validate.errors || [],
    }
  }

  /**
   * Valide une URL
   */
  validateUrl(url) {
    try {
      new URL(url)
      return { valid: true, error: null }
    } catch (error) {
      return { valid: false, error: error.message }
    }
  }

  /**
   * Valide un port
   */
  validatePort(port) {
    const numPort = parseInt(port, 10)
    if (isNaN(numPort) || numPort < 1 || numPort > 65535) {
      return { valid: false, error: 'Port must be between 1 and 65535' }
    }
    return { valid: true, error: null }
  }

  /**
   * Valide une adresse IP
   */
  validateIp(ip) {
    const ipv4Regex = /^(\d{1,3}\.){3}\d{1,3}$/
    const ipv6Regex = /^([0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}$/

    if (ip === 'localhost' || ip === '0.0.0.0') {
      return { valid: true, error: null }
    }

    if (ipv4Regex.test(ip)) {
      const parts = ip.split('.')
      const valid = parts.every((part) => {
        const num = parseInt(part, 10)
        return num >= 0 && num <= 255
      })
      return { valid, error: valid ? null : 'Invalid IPv4 address' }
    }

    if (ipv6Regex.test(ip)) {
      return { valid: true, error: null }
    }

    return { valid: false, error: 'Invalid IP address format' }
  }
}

/**
 * Instance singleton
 */
let instance = null

export function getConfigValidator() {
  if (!instance) {
    instance = new ConfigValidator()
  }
  return instance
}





