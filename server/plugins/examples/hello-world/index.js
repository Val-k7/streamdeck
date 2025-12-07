/**
 * Plugin Hello World - Exemple simple
 *
 * Ce plugin démontre les fonctionnalités de base d'un plugin Control Deck.
 */

export default function createPlugin({ name, version, config, logger, pluginDir }) {
  logger.info(`Hello World plugin v${version} initializing...`)

  async function onLoad() {
    logger.info(`Hello World plugin loaded with greeting: ${config.greeting || 'Hello'}`)
  }

  async function onUnload() {
    logger.info('Hello World plugin unloaded')
  }

  async function handleAction(action, payload) {
    switch (action) {
      case 'hello':
        const greeting = config.greeting || 'Hello'
        const name = payload?.name || 'World'
        const message = `${greeting}, ${name}!`

        logger.info(message)

        return {
          success: true,
          message,
          timestamp: new Date().toISOString(),
        }

      default:
        throw new Error(`Unknown action: ${action}`)
    }
  }

  return {
    onLoad,
    onUnload,
    handleAction,
  }
}
