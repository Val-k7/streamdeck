import fs from 'fs'
import path from 'path'
import { fileURLToPath } from 'url'

const __filename = fileURLToPath(import.meta.url)
const __dirname = path.dirname(__filename)

/**
 * Gestionnaire de configuration pour les plugins
 */
export class PluginConfigManager {
  constructor(pluginsConfigDir, logger) {
    this.pluginsConfigDir = pluginsConfigDir
    this.logger = logger
    this.configCache = new Map()

    // S'assurer que le répertoire existe
    if (!fs.existsSync(pluginsConfigDir)) {
      fs.mkdirSync(pluginsConfigDir, { recursive: true })
    }
  }

  /**
   * Charge la configuration d'un plugin
   */
  loadConfig(pluginId) {
    // Vérifier le cache
    if (this.configCache.has(pluginId)) {
      return this.configCache.get(pluginId)
    }

    const configPath = path.join(this.pluginsConfigDir, `${pluginId}.json`)

    if (!fs.existsSync(configPath)) {
      // Retourner une configuration par défaut
      const defaultConfig = this.getDefaultConfig(pluginId)
      this.configCache.set(pluginId, defaultConfig)
      return defaultConfig
    }

    try {
      const configData = fs.readFileSync(configPath, 'utf-8')
      const config = JSON.parse(configData)
      this.configCache.set(pluginId, config)
      return config
    } catch (error) {
      this.logger?.error(`Failed to load config for plugin ${pluginId}`, { error: error.message })
      const defaultConfig = this.getDefaultConfig(pluginId)
      this.configCache.set(pluginId, defaultConfig)
      return defaultConfig
    }
  }

  /**
   * Sauvegarde la configuration d'un plugin
   */
  saveConfig(pluginId, config) {
    const configPath = path.join(this.pluginsConfigDir, `${pluginId}.json`)

    try {
      // Valider la configuration
      const validatedConfig = this.validateConfig(pluginId, config)

      // Sauvegarder
      fs.writeFileSync(configPath, JSON.stringify(validatedConfig, null, 2), 'utf-8')

      // Mettre à jour le cache
      this.configCache.set(pluginId, validatedConfig)

      this.logger?.info(`Config saved for plugin ${pluginId}`)
      return validatedConfig
    } catch (error) {
      this.logger?.error(`Failed to save config for plugin ${pluginId}`, { error: error.message })
      throw error
    }
  }

  /**
   * Met à jour une partie de la configuration
   */
  updateConfig(pluginId, updates) {
    const currentConfig = this.loadConfig(pluginId)
    const updatedConfig = { ...currentConfig, ...updates }
    return this.saveConfig(pluginId, updatedConfig)
  }

  /**
   * Supprime la configuration d'un plugin
   */
  deleteConfig(pluginId) {
    const configPath = path.join(this.pluginsConfigDir, `${pluginId}.json`)

    if (fs.existsSync(configPath)) {
      try {
        fs.unlinkSync(configPath)
        this.configCache.delete(pluginId)
        this.logger?.info(`Config deleted for plugin ${pluginId}`)
        return true
      } catch (error) {
        this.logger?.error(`Failed to delete config for plugin ${pluginId}`, { error: error.message })
        return false
      }
    }

    return true
  }

  /**
   * Obtient le schéma de configuration pour un plugin
   */
  getConfigSchema(pluginId) {
    // Par défaut, retourner un schéma générique
    // Les plugins peuvent surcharger cette méthode
    return {
      type: 'object',
      properties: {},
      required: []
    }
  }

  /**
   * Valide une configuration selon le schéma
   */
  validateConfig(pluginId, config) {
    const schema = this.getConfigSchema(pluginId)
    const validated = { ...config }

    // Validation basique
    if (schema.required) {
      for (const field of schema.required) {
        if (!(field in validated)) {
          throw new Error(`Required field missing: ${field}`)
        }
      }
    }

    // Validation des types
    if (schema.properties) {
      for (const [field, fieldSchema] of Object.entries(schema.properties)) {
        if (field in validated) {
          const value = validated[field]
          const expectedType = fieldSchema.type

          if (expectedType === 'string' && typeof value !== 'string') {
            throw new Error(`Field ${field} must be a string`)
          } else if (expectedType === 'number' && typeof value !== 'number') {
            throw new Error(`Field ${field} must be a number`)
          } else if (expectedType === 'boolean' && typeof value !== 'boolean') {
            throw new Error(`Field ${field} must be a boolean`)
          } else if (expectedType === 'object' && typeof value !== 'object') {
            throw new Error(`Field ${field} must be an object`)
          } else if (expectedType === 'array' && !Array.isArray(value)) {
            throw new Error(`Field ${field} must be an array`)
          }
        }
      }
    }

    return validated
  }

  /**
   * Obtient une configuration par défaut pour un plugin
   */
  getDefaultConfig(pluginId) {
    // Configurations par défaut pour les plugins connus
    const defaultConfigs = {
      discord: {
        enabled: true,
        token: '',
        clientId: '',
        guildId: '',
        channelId: '',
        autoReconnect: true,
        reconnectDelay: 5000
      },
      spotify: {
        enabled: true,
        clientId: '',
        clientSecret: '',
        redirectUri: 'http://localhost:3000/callback',
        accessToken: '',
        refreshToken: '',
        autoRefresh: true
      },
      obs: {
        enabled: true,
        host: 'localhost',
        port: 4455,
        password: '',
        autoReconnect: true,
        reconnectDelay: 5000
      }
    }

    return defaultConfigs[pluginId] || {}
  }

  /**
   * Réinitialise la configuration d'un plugin aux valeurs par défaut
   */
  resetConfig(pluginId) {
    const defaultConfig = this.getDefaultConfig(pluginId)
    return this.saveConfig(pluginId, defaultConfig)
  }

  /**
   * Liste toutes les configurations
   */
  listConfigs() {
    const configs = {}

    try {
      const files = fs.readdirSync(this.pluginsConfigDir)
      for (const file of files) {
        if (file.endsWith('.json')) {
          const pluginId = path.basename(file, '.json')
          configs[pluginId] = this.loadConfig(pluginId)
        }
      }
    } catch (error) {
      this.logger?.error('Failed to list configs', { error: error.message })
    }

    return configs
  }

  /**
   * Vide le cache de configuration
   */
  clearCache() {
    this.configCache.clear()
  }

  /**
   * Recharge la configuration d'un plugin depuis le disque
   */
  reloadConfig(pluginId) {
    this.configCache.delete(pluginId)
    return this.loadConfig(pluginId)
  }
}





