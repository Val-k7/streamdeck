/**
 * Plugin Webhook - Envoie des requêtes HTTP
 *
 * Ce plugin permet d'envoyer des requêtes HTTP vers des webhooks.
 */

import https from 'https'
import http from 'http'
import { URL } from 'url'

export default function createPlugin({ name, version, config, logger, pluginDir }) {
  logger.info(`Webhook plugin v${version} initializing...`)

  async function onLoad() {
    logger.info('Webhook plugin loaded')
  }

  async function onUnload() {
    logger.info('Webhook plugin unloaded')
  }

  async function handleAction(action, payload) {
    switch (action) {
      case 'send':
        return await sendWebhook(payload)

      default:
        throw new Error(`Unknown action: ${action}`)
    }
  }

  async function sendWebhook(payload) {
    const { url, method = config.defaultMethod || 'POST', body } = payload || {}

    if (!url) {
      throw new Error('URL is required')
    }

    try {
      const parsedUrl = new URL(url)
      const isHttps = parsedUrl.protocol === 'https:'
      const client = isHttps ? https : http

      const requestBody = body ? JSON.stringify(body) : ''
      const options = {
        hostname: parsedUrl.hostname,
        port: parsedUrl.port || (isHttps ? 443 : 80),
        path: parsedUrl.pathname + parsedUrl.search,
        method: method.toUpperCase(),
        headers: {
          'Content-Type': 'application/json',
          'Content-Length': Buffer.byteLength(requestBody),
        },
        timeout: config.timeout || 5000,
      }

      return new Promise((resolve, reject) => {
        const req = client.request(options, (res) => {
          let data = ''

          res.on('data', (chunk) => {
            data += chunk
          })

          res.on('end', () => {
            logger.info(`Webhook sent to ${url}: ${res.statusCode}`)
            resolve({
              success: res.statusCode >= 200 && res.statusCode < 300,
              statusCode: res.statusCode,
              response: data,
            })
          })
        })

        req.on('error', (error) => {
          logger.error(`Webhook error: ${error.message}`)
          reject(error)
        })

        req.on('timeout', () => {
          req.destroy()
          reject(new Error('Request timeout'))
        })

        if (requestBody) {
          req.write(requestBody)
        }

        req.end()
      })
    } catch (error) {
      logger.error(`Failed to send webhook: ${error.message}`)
      throw error
    }
  }

  return {
    onLoad,
    onUnload,
    handleAction,
  }
}





