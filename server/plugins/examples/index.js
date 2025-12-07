import helloPlugin from './hello-world/index.js'
import webhookPlugin from './webhook/index.js'

export default function createExamplesPlugin({ version, config, logger, pluginDir }) {
  const hello = helloPlugin({
    name: 'hello-world',
    version,
    config: config?.hello || {},
    logger: logger.child({ plugin: 'examples:hello' }),
    pluginDir: `${pluginDir}/hello-world`,
  })

  const webhook = webhookPlugin({
    name: 'webhook',
    version,
    config: config?.webhook || {},
    logger: logger.child({ plugin: 'examples:webhook' }),
    pluginDir: `${pluginDir}/webhook`,
  })

  return {
    async onLoad() {
      await hello.onLoad?.()
      await webhook.onLoad?.()
      logger.info('Examples plugin loaded (hello-world, webhook)')
    },

    async onUnload() {
      await hello.onUnload?.()
      await webhook.onUnload?.()
    },

    async handleAction(action, payload) {
      if (action === 'hello') return await hello.handleAction('hello', payload)
      if (['webhook_send', 'send', 'webhook'].includes(action)) {
        return await webhook.handleAction('send', payload)
      }
      throw new Error(`Unknown examples action: ${action}`)
    },
  }
}

