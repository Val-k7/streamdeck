import fetch from 'node-fetch'
import { OBSAdvancedManager } from '../plugins/obs/advanced.js'
import { getLogger } from '../utils/Logger.js'

const logger = getLogger()

const OBS_WS_URL = process.env.OBS_WS_URL || 'ws://localhost:4455'
const OBS_WS_PASSWORD = process.env.OBS_WS_PASSWORD || ''

// Client OBS simplifié pour les fonctionnalités avancées
class OBSClient {
  constructor(url, password) {
    this.url = url.replace('ws://', 'http://').replace('wss://', 'https://')
    this.password = password
  }

  async call(requestType, requestData = {}) {
    const response = await fetch(`${this.url}/api`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        ...(this.password && {
          Authorization: `Basic ${Buffer.from(`:${this.password}`).toString('base64')}`,
        }),
      },
      body: JSON.stringify({
        requestType,
        requestData,
      }),
    })

    if (!response.ok) {
      throw new Error(`OBS request failed: ${response.statusText}`)
    }

    const result = await response.json()
    if (result.requestStatus?.code !== 100) {
      throw new Error(
        `OBS request error: ${result.requestStatus?.comment || 'Unknown error'}`
      )
    }

    return result.responseData
  }
}

async function obsRequest(requestType, requestData = {}) {
  const url = OBS_WS_URL.replace('ws://', 'http://').replace('wss://', 'https://')

  // Créer un AbortController pour le timeout (compatible toutes versions Node.js)
  const controller = new AbortController()
  const timeoutId = setTimeout(() => controller.abort(), 10000) // 10 secondes

  try {
    const response = await fetch(`${url}/api`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        ...(OBS_WS_PASSWORD && { Authorization: `Basic ${Buffer.from(`:${OBS_WS_PASSWORD}`).toString('base64')}` }),
      },
      body: JSON.stringify({
        requestType,
        requestData,
      }),
      signal: controller.signal,
    })

    clearTimeout(timeoutId) // Nettoyer le timeout si la requête réussit

    if (!response.ok) {
      // Gestion spécifique des erreurs HTTP
      if (response.status === 404) {
        throw new Error('OBS WebSocket server not found. Is OBS Studio running with WebSocket enabled?')
      }
      if (response.status === 401) {
        throw new Error('OBS WebSocket authentication failed. Check your password.')
      }
      if (response.status === 0 || response.status >= 500) {
        throw new Error(`OBS WebSocket server error: ${response.statusText}. Is OBS Studio running?`)
      }
      throw new Error(`OBS request failed: ${response.statusText}`)
    }

    const result = await response.json()
    if (result.requestStatus?.code !== 100) {
      // Messages d'erreur plus clairs selon le code
      const errorCode = result.requestStatus?.code
      const errorComment = result.requestStatus?.comment || 'Unknown error'

      if (errorCode === 404) {
        throw new Error(`OBS resource not found: ${errorComment}`)
      }
      if (errorCode === 400) {
        throw new Error(`OBS invalid request: ${errorComment}`)
      }
      throw new Error(`OBS request error (code ${errorCode}): ${errorComment}`)
    }

    return result.responseData
  } catch (error) {
    clearTimeout(timeoutId) // Nettoyer le timeout en cas d'erreur

    // Gestion des erreurs de connexion
    if (error.name === 'AbortError' || error.message.includes('timeout') || error.message.includes('aborted')) {
      throw new Error('OBS WebSocket connection timeout. Is OBS Studio running?')
    }
    if (error.message.includes('ECONNREFUSED') || error.message.includes('fetch failed') || error.message.includes('ECONNRESET')) {
      throw new Error('Cannot connect to OBS WebSocket server. Is OBS Studio running with WebSocket enabled on port 4455?')
    }
    // Re-lancer l'erreur si elle est déjà formatée
    throw error
  }
}

export async function handleObs(payload) {
  // Parser le payload si c'est un string JSON
  let parsedPayload = payload
  if (typeof payload === 'string') {
    try {
      parsedPayload = JSON.parse(payload)
    } catch (e) {
      // Si ce n'est pas du JSON, traiter comme nom d'action simple
      parsedPayload = payload
    }
  }

  if (typeof parsedPayload !== 'string' && typeof parsedPayload !== 'object') {
    throw new Error('OBS payload must be a string (action name) or object (action with params)')
  }

  let action
  let params = {}

  if (typeof parsedPayload === 'string') {
    action = parsedPayload
  } else {
    action = parsedPayload.action || parsedPayload.type
    params = parsedPayload.params || parsedPayload.payload || {}
  }

  try {
    switch (action.toUpperCase()) {
      case 'STARTSTREAMING':
      case 'START_STREAMING':
        await obsRequest('StartStream')
        break

      case 'STOPSTREAMING':
      case 'STOP_STREAMING':
        await obsRequest('StopStream')
        break

      case 'START_RECORDING':
        await obsRequest('StartRecord')
        break

      case 'STOP_RECORDING':
        await obsRequest('StopRecord')
        break

      case 'TOGGLE_STREAMING':
        const streamStatus = await obsRequest('GetStreamStatus')
        if (streamStatus.outputActive) {
          await obsRequest('StopStream')
        } else {
          await obsRequest('StartStream')
        }
        break

      case 'TOGGLE_RECORDING':
        const recordStatus = await obsRequest('GetRecordStatus')
        if (recordStatus.outputActive) {
          await obsRequest('StopRecord')
        } else {
          await obsRequest('StartRecord')
        }
        break

      case 'SET_SCENE':
      case 'CHANGE_SCENE':
        if (!params.sceneName) {
          throw new Error('Scene name required for SET_SCENE action')
        }
        await obsRequest('SetCurrentProgramScene', { sceneName: params.sceneName })
        break

      case 'SET_VOLUME':
      case 'OBS_VOLUME':
        if (params.sourceName === undefined && params.inputName === undefined) {
          throw new Error('Source/input name required for SET_VOLUME action')
        }
        const inputName = params.sourceName || params.inputName
        const volumeDb = params.volumeDb !== undefined ? params.volumeDb : params.volume
        if (volumeDb !== undefined) {
          await obsRequest('SetInputVolume', {
            inputName,
            inputVolumeDb: volumeDb,
          })
        } else {
          throw new Error('Volume value required for SET_VOLUME action')
        }
        break

      case 'MUTE':
        if (!params.sourceName && !params.inputName) {
          throw new Error('Source/input name required for MUTE action')
        }
        await obsRequest('ToggleInputMute', {
          inputName: params.sourceName || params.inputName,
        })
        break

      case 'UNMUTE':
        if (!params.sourceName && !params.inputName) {
          throw new Error('Source/input name required for UNMUTE action')
        }
        await obsRequest('SetInputMute', {
          inputName: params.sourceName || params.inputName,
          inputMuted: false,
        })
        break

      default:
        // Essayer les actions avancées (accepte les noms en minuscules)
        try {
          const obsClient = new OBSClient(OBS_WS_URL, OBS_WS_PASSWORD)
          const advancedManager = new OBSAdvancedManager(obsClient)
          const advancedAction = typeof action === 'string' ? action.toLowerCase() : action
          return await advancedManager.handleAdvancedAction(advancedAction, params)
        } catch (advancedError) {
          logger.warn('[obs] Unknown action', { action, error: advancedError.message })
          throw new Error(`Unknown OBS action: ${action}`)
        }
    }
  } catch (error) {
    logger.error('[obs] error', { error: error.message, stack: error.stack })
    throw error
  }
}
