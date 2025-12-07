/**
 * Template de plugin pour Control Deck
 *
 * Ce fichier sert de base pour créer de nouveaux plugins.
 * Copiez ce dossier et modifiez-le selon vos besoins.
 */

export default function createPlugin({ name, version, config, logger, pluginDir }) {
  // État du plugin
  let isInitialized = false
  let pluginState = {}

  /**
   * Appelé lors du chargement du plugin
   */
  async function onLoad() {
    logger.info(`Plugin ${name} v${version} is loading...`)

    // Initialiser le plugin ici
    isInitialized = true
    pluginState = {
      loadedAt: new Date().toISOString(),
      config,
    }

    logger.info(`Plugin ${name} loaded successfully`)
  }

  /**
   * Appelé lors du déchargement du plugin
   */
  async function onUnload() {
    logger.info(`Plugin ${name} is unloading...`)

    // Nettoyer les ressources ici
    isInitialized = false
    pluginState = {}

    logger.info(`Plugin ${name} unloaded successfully`)
  }

  /**
   * Gère les actions du plugin
   *
   * @param {string} action - Nom de l'action
   * @param {object} payload - Données de l'action
   * @returns {Promise<any>} Résultat de l'action
   */
  async function handleAction(action, payload) {
    if (!isInitialized) {
      throw new Error(`Plugin ${name} is not initialized`)
    }

    logger.debug(`Handling action: ${action}`, { payload })

    switch (action) {
      case 'example':
        return handleExample(payload)

      default:
        throw new Error(`Unknown action: ${action}`)
    }
  }

  /**
   * Exemple d'action
   */
  async function handleExample(payload) {
    const { message } = payload || {}

    if (!message) {
      throw new Error('Message is required')
    }

    logger.info(`Example action called with message: ${message}`)

    return {
      success: true,
      message: `Plugin ${name} received: ${message}`,
      timestamp: new Date().toISOString(),
    }
  }

  /**
   * Appelé lors de la mise à jour de la configuration
   */
  function onConfigUpdate(newConfig) {
    logger.info(`Configuration updated for plugin ${name}`)
    pluginState.config = newConfig
    config = newConfig
  }

  /**
   * Retourne l'instance du plugin
   */
  return {
    // Méthodes requises
    onLoad,
    onUnload,
    handleAction,

    // Méthodes optionnelles
    onConfigUpdate,

    // Méthodes utilitaires (optionnelles)
    getState: () => ({ ...pluginState }),
    isInitialized: () => isInitialized,
  }
}





