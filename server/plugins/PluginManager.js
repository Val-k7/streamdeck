import fs from 'fs'
import path from 'path'
import { fileURLToPath } from 'url'
import { PluginConfigManager } from './PluginConfigManager.js'
import { PluginSandbox } from './PluginSandbox.js'
import { createRequire } from 'module'

const __filename = fileURLToPath(import.meta.url)
const __dirname = path.dirname(__filename)

const require = createRequire(import.meta.url)

/**
 * Gestionnaire de plugins pour le serveur Control Deck
 */
export class PluginManager {
  constructor(pluginsDir, configDir, logger) {
    this.pluginsDir = pluginsDir
    this.configDir = configDir
    this.logger = logger || console
    this.plugins = new Map()
    this.pluginConfigs = new Map()
    this.configManager = new PluginConfigManager(configDir, logger)
    this.sandboxes = new Map() // pluginName -> PluginSandbox

    // Créer les répertoires si nécessaire
    if (!fs.existsSync(pluginsDir)) {
      fs.mkdirSync(pluginsDir, { recursive: true })
    }
    if (!fs.existsSync(configDir)) {
      fs.mkdirSync(configDir, { recursive: true })
    }
  }

  /**
   * Charge tous les plugins disponibles
   */
  async loadAllPlugins() {
    if (!fs.existsSync(this.pluginsDir)) {
      this.logger.warn(`Plugins directory does not exist: ${this.pluginsDir}`)
      return
    }

    const pluginDirs = fs.readdirSync(this.pluginsDir, { withFileTypes: true })
      .filter(dirent => dirent.isDirectory())
      .map(dirent => dirent.name)

    for (const pluginName of pluginDirs) {
      try {
        await this.loadPlugin(pluginName)
      } catch (error) {
        this.logger.error(`Failed to load plugin ${pluginName}:`, error)
      }
    }

    this.logger.info(`Loaded ${this.plugins.size} plugin(s)`)
  }

  /**
   * Charge un plugin spécifique
   */
  async loadPlugin(pluginName) {
    const pluginPath = path.join(this.pluginsDir, pluginName)
    const pluginManifestPath = path.join(pluginPath, 'plugin.json')
    const pluginMainPath = path.join(pluginPath, 'index.js')

    if (!fs.existsSync(pluginManifestPath)) {
      throw new Error(`Plugin manifest not found: ${pluginManifestPath}`)
    }

    if (!fs.existsSync(pluginMainPath)) {
      throw new Error(`Plugin main file not found: ${pluginMainPath}`)
    }

    // Charger le manifest
    const manifest = JSON.parse(fs.readFileSync(pluginManifestPath, 'utf-8'))

    // Valider le manifest
    this.validateManifest(manifest)

    // Charger la configuration du plugin
    const config = this.loadPluginConfig(pluginName, manifest)

    // Charger le module du plugin
    const pluginModule = await import(`file://${pluginMainPath}`)

    if (!pluginModule.default || typeof pluginModule.default !== 'function') {
      throw new Error(`Plugin ${pluginName} does not export a default function`)
    }

    // Créer le sandbox pour ce plugin
    const sandbox = new PluginSandbox(pluginName, this.logger)
    this.sandboxes.set(pluginName, sandbox)

    // Créer l'instance du plugin dans le sandbox
    const pluginInstance = pluginModule.default({
      name: pluginName,
      version: manifest.version,
      config,
      logger: this.logger.child({ plugin: pluginName }),
      pluginDir: pluginPath,
      sandbox, // Passer le sandbox au plugin
    })

    // Valider l'instance du plugin
    this.validatePluginInstance(pluginInstance, manifest)

    // Initialiser le plugin
    if (pluginInstance.onLoad) {
      await pluginInstance.onLoad()
    }

    // Enregistrer le plugin
    this.plugins.set(pluginName, {
      name: pluginName,
      manifest,
      instance: pluginInstance,
      config,
      enabled: config.enabled !== false,
      sandbox,
    })

    this.logger.info(`Plugin loaded: ${pluginName} v${manifest.version}`)
  }

  /**
   * Décharge un plugin
   */
  async unloadPlugin(pluginName) {
    const plugin = this.plugins.get(pluginName)
    if (!plugin) {
      throw new Error(`Plugin not found: ${pluginName}`)
    }

    try {
      if (plugin.instance.onUnload) {
        await plugin.instance.onUnload()
      }
    } catch (error) {
      this.logger.error(`Error unloading plugin ${pluginName}:`, error)
    }

    this.plugins.delete(pluginName)
    this.sandboxes.delete(pluginName)
    this.logger.info(`Plugin unloaded: ${pluginName}`)
  }

  /**
   * Active un plugin
   */
  enablePlugin(pluginName) {
    const plugin = this.plugins.get(pluginName)
    if (!plugin) {
      throw new Error(`Plugin not found: ${pluginName}`)
    }

    plugin.enabled = true
    this.savePluginConfig(pluginName, plugin.config)
    this.logger.info(`Plugin enabled: ${pluginName}`)
  }

  /**
   * Désactive un plugin
   */
  disablePlugin(pluginName) {
    const plugin = this.plugins.get(pluginName)
    if (!plugin) {
      throw new Error(`Plugin not found: ${pluginName}`)
    }

    plugin.enabled = false
    this.savePluginConfig(pluginName, plugin.config)
    this.logger.info(`Plugin disabled: ${pluginName}`)
  }

  /**
   * Exécute une action via un plugin avec sandboxing
   */
  async executeAction(pluginName, action, payload) {
    const plugin = this.plugins.get(pluginName)
    if (!plugin) {
      throw new Error(`Plugin not found: ${pluginName}`)
    }

    if (!plugin.enabled) {
      throw new Error(`Plugin is disabled: ${pluginName}`)
    }

    if (!plugin.instance.handleAction) {
      throw new Error(`Plugin does not support actions: ${pluginName}`)
    }

    const sandbox = plugin.sandbox

    try {
      // Valider les entrées
      const actionSchema = plugin.manifest.actions?.find((a) => a.name === action)
      if (actionSchema) {
        payload = sandbox.validateInput(payload || {}, this.buildValidationSchema(actionSchema.parameters))
      }

      // Vérifier l'utilisation de la mémoire
      sandbox.checkMemoryUsage()

      // Exécuter l'action dans le sandbox
      const wrappedAction = sandbox.wrapFunction(plugin.instance.handleAction.bind(plugin.instance), {
        timeout: sandbox.timeout,
      })

      return await wrappedAction(action, payload)
    } catch (error) {
      this.logger.error(`Error executing action in plugin ${pluginName}:`, error)
      throw error
    }
  }

  /**
   * Construit un schéma de validation à partir des paramètres d'action
   */
  buildValidationSchema(parameters) {
    if (!parameters) return null

    const schema = {}
    for (const [key, param] of Object.entries(parameters)) {
      schema[key] = {
        type: param.type || 'string',
        required: param.required || false,
        min: param.min,
        max: param.max,
        maxLength: param.maxLength,
        maxItems: param.maxItems,
        items: param.items ? this.buildValidationSchema(param.items) : undefined,
        properties: param.properties ? this.buildValidationSchema(param.properties) : undefined,
      }
    }
    return schema
  }

  /**
   * Liste tous les plugins
   */
  listPlugins() {
    return Array.from(this.plugins.values()).map(plugin => ({
      name: plugin.name,
      version: plugin.manifest.version,
      enabled: plugin.enabled,
      description: plugin.manifest.description,
      author: plugin.manifest.author
    }))
  }

  /**
   * Valide le manifest d'un plugin
   */
  validateManifest(manifest) {
    const required = ['name', 'version', 'description', 'main']
    for (const field of required) {
      if (!manifest[field]) {
        throw new Error(`Plugin manifest missing required field: ${field}`)
      }
    }

    // Valider la version
    if (!/^\d+\.\d+\.\d+/.test(manifest.version)) {
      throw new Error(`Invalid plugin version format: ${manifest.version}`)
    }
  }

  /**
   * Valide l'instance d'un plugin
   */
  validatePluginInstance(instance, manifest) {
    if (!instance.handleAction && !manifest.actions) {
      this.logger.warn(`Plugin ${manifest.name} does not export handleAction`)
    }
  }

  /**
   * Charge la configuration d'un plugin
   */
  loadPluginConfig(pluginName, manifest) {
    const defaultConfig = manifest.config || {}
    const savedConfig = this.configManager.loadConfig(pluginName)
    return { ...defaultConfig, ...savedConfig }
  }

  /**
   * Sauvegarde la configuration d'un plugin
   */
  savePluginConfig(pluginName, config) {
    this.configManager.saveConfig(pluginName, config)
  }

  /**
   * Obtient la configuration d'un plugin
   */
  getPluginConfig(pluginName) {
    const plugin = this.plugins.get(pluginName)
    return plugin ? plugin.config : null
  }

  /**
   * Met à jour la configuration d'un plugin
   */
  updatePluginConfig(pluginName, updates) {
    const plugin = this.plugins.get(pluginName)
    if (!plugin) {
      throw new Error(`Plugin not found: ${pluginName}`)
    }

    const updatedConfig = this.configManager.updateConfig(pluginName, updates)
    plugin.config = updatedConfig

    // Notifier le plugin du changement de configuration
    if (plugin.instance.onConfigUpdate) {
      plugin.instance.onConfigUpdate(updatedConfig)
    }

    return updatedConfig
  }

  /**
   * Réinitialise la configuration d'un plugin
   */
  resetPluginConfig(pluginName) {
    const plugin = this.plugins.get(pluginName)
    if (!plugin) {
      throw new Error(`Plugin not found: ${pluginName}`)
    }

    const defaultConfig = this.configManager.resetConfig(pluginName)
    plugin.config = defaultConfig

    // Notifier le plugin du changement de configuration
    if (plugin.instance.onConfigUpdate) {
      plugin.instance.onConfigUpdate(defaultConfig)
    }

    return defaultConfig
  }

  /**
   * Obtient le gestionnaire de configuration
   */
  getConfigManager() {
    return this.configManager
  }
}

