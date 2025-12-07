import { handleObs } from '../../actions/obs.js'

export default function createObsPlugin({ version, config, logger }) {
  const host = config?.host || 'localhost'
  const port = config?.port || 4455
  const password = config?.password || ''
  const obsUrl = `ws://${host}:${port}`

  function applyEnv() {
    process.env.OBS_WS_URL = obsUrl
    process.env.OBS_WS_PASSWORD = password
  }

  return {
    async onLoad() {
      applyEnv()
      logger.info(`OBS plugin v${version} initialized`, {
        url: obsUrl,
        autoReconnect: config?.autoReconnect !== false,
      })
    },

    async onUnload() {
      logger.info('OBS plugin unloaded')
    },

    async handleAction(action, payload = {}) {
      const resolved = action || ''
      return await handleObs({ action: resolved, params: payload })
    },
  }
}
