import express from 'express'
import rateLimit from 'express-rate-limit'
import { WebSocketServer, WebSocket } from 'ws'
import fs from 'fs'
import path from 'path'
import http from 'http'
import https from 'https'
import crypto from 'crypto'
import { performance } from 'node:perf_hooks'
import { fileURLToPath } from 'url'
import { handleKeyboard } from './actions/keyboard.js'
import { handleObs } from './actions/obs.js'
import { handleScript } from './actions/scripts.js'
import { handleAudio } from './actions/audio.js'
import { handleMediaKeys } from './actions/media-windows.js'
import * as SystemWindows from './actions/system-windows.js'
import * as ClipboardWindows from './actions/clipboard-windows.js'
import * as ScreenshotWindows from './actions/screenshot-windows.js'
import * as ProcessWindows from './actions/processes.js'
import * as FilesWindows from './actions/files-windows.js'
import { PluginManager } from './plugins/PluginManager.js'
import { getTokenManager } from './utils/TokenManager.js'
import { getRateLimiter } from './utils/RateLimiter.js'
import { getAuditLogger } from './utils/AuditLogger.js'
import { getActionQueue } from './utils/ActionQueue.js'
import { getPerformanceMonitor } from './utils/PerformanceMonitor.js'
import { ErrorHandler } from './utils/ErrorHandler.js'
import { getHealthChecker } from './utils/HealthChecker.js'
import { getGlobalEventEmitter } from './utils/EventEmitter.js'
import { getCacheManager } from './utils/CacheManager.js'
import Ajv from 'ajv'
import winston from 'winston'
import DailyRotateFile from 'winston-daily-rotate-file'
import os from 'os'
import bonjour from 'bonjour'
import { v4 as uuidv4 } from 'uuid'
import { DiscoveryService } from './utils/DiscoveryService.js'
import { getPairingManager } from './utils/PairingManager.js'

const __filename = fileURLToPath(import.meta.url)
const __dirname = path.dirname(__filename)

const runtimeBase = process.env.DECK_DATA_DIR
  ? path.resolve(process.env.DECK_DATA_DIR)
  : process.pkg
    ? path.dirname(process.execPath)
    : __dirname
const templateBase = __dirname
const configDir = path.join(runtimeBase, 'config')
const profilesDir = path.join(runtimeBase, 'profiles')
const logsDir = path.join(runtimeBase, 'logs')
const pluginsDir = path.join(runtimeBase, 'plugins')
const pluginsConfigDir = path.join(configDir, 'plugins')

// Note: L'UI web n'est plus servie par le serveur
// Elle doit être servie séparément via un serveur web dédié

fs.mkdirSync(configDir, { recursive: true })
fs.mkdirSync(profilesDir, { recursive: true })
fs.mkdirSync(logsDir, { recursive: true })
fs.mkdirSync(pluginsDir, { recursive: true })
fs.mkdirSync(pluginsConfigDir, { recursive: true })

function readJsonSafe(filePath, fallback = {}) {
  try {
    return JSON.parse(fs.readFileSync(filePath, 'utf-8'))
  } catch (e) {
    logger.warn('Unable to parse JSON file', { file: filePath, error: e.message })
    return fallback
  }
}

function ensureSeedFile(seedPath, targetPath) {
  if (!fs.existsSync(seedPath)) return
  if (fs.existsSync(targetPath)) return
  fs.mkdirSync(path.dirname(targetPath), { recursive: true })
  fs.copyFileSync(seedPath, targetPath)
}

const configPath = path.join(configDir, 'server.config.json')
const configSeedPath = path.join(templateBase, 'config', 'server.config.sample.json')
ensureSeedFile(configSeedPath, configPath)
const config = fs.existsSync(configPath) ? readJsonSafe(configPath, {}) : {}

const port = Number(process.env.PORT) || config.port || 4455
const host = process.env.HOST || config.host || '0.0.0.0' // Écouter sur toutes les interfaces par défaut

// Générer un token sécurisé par défaut si aucun n'est fourni
function generateSecureToken() {
  return crypto.randomBytes(32).toString('hex')
}

const defaultToken = process.env.DECK_TOKEN || config.defaultToken || (() => {
  const generated = generateSecureToken()
  logger.warn('No default token provided, generated secure token. Please set DECK_TOKEN or defaultToken in config for production.', {
    generated: generated.substring(0, 8) + '...'
  })
  return generated
})()
const handshakeSecret = process.env.HANDSHAKE_SECRET || config.handshakeSecret || defaultToken
const tlsKey = process.env.TLS_KEY_PATH
const tlsCert = process.env.TLS_CERT_PATH
const logLevel = process.env.LOG_LEVEL || 'info'
const serverStartedAt = Date.now()

// Identifiant unique du serveur (généré une fois et stocké)
const serverIdPath = path.join(configDir, 'server.id')
let serverId = null
if (fs.existsSync(serverIdPath)) {
  try {
    serverId = fs.readFileSync(serverIdPath, 'utf-8').trim()
  } catch (e) {
    logger.warn('Unable to read server ID', { error: e.message })
  }
}
if (!serverId) {
  serverId = uuidv4()
  try {
    fs.writeFileSync(serverIdPath, serverId, 'utf-8')
  } catch (e) {
    logger.warn('Unable to save server ID', { error: e.message })
  }
}

// Nom du serveur pour mDNS (utilise le hostname ou un nom par défaut)
const serverName = process.env.SERVER_NAME || config.serverName || `ControlDeck-${os.hostname()}`.replace(/[^a-zA-Z0-9-]/g, '-')

const app = express()

const logger = winston.createLogger({
  level: logLevel,
  format: winston.format.combine(winston.format.timestamp(), winston.format.json()),
  transports: [
    new winston.transports.Console({ level: logLevel }),
    new DailyRotateFile({
      dirname: logsDir,
      filename: 'security-%DATE%.log',
      datePattern: 'YYYY-MM-DD',
      zippedArchive: true,
      maxFiles: '14d',
      maxSize: '5m',
      level: logLevel,
    }),
  ],
})

// Initialiser le rate limiter, l'audit logger, la file d'actions, le moniteur de performance et le cache
const rateLimiter = getRateLimiter()
const auditLogger = getAuditLogger()
const actionQueue = getActionQueue(5, 30000) // 5 actions concurrentes max, 30s timeout
const performanceMonitor = getPerformanceMonitor()
const cacheManager = getCacheManager(100, 300000) // Cache de 100 entrées, TTL 5 minutes
const errorHandler = new ErrorHandler(logger)
const healthChecker = getHealthChecker(logger)
const eventEmitter = getGlobalEventEmitter()
const pairingManager = getPairingManager(logger)

// Démarrer les vérifications de santé périodiques
healthChecker.startPeriodicChecks()

// Configurer les limites de taux
rateLimiter.configure('ip', { windowMs: 60000, max: 100 }) // 100 requêtes/minute par IP
rateLimiter.configure('client', { windowMs: 60000, max: 200 }) // 200 requêtes/minute par client
rateLimiter.configure('action', { windowMs: 1000, max: 15 }) // 15 actions/seconde par action

// Initialiser le gestionnaire de plugins
const pluginManager = new PluginManager(pluginsDir, pluginsConfigDir, logger)

// Charger les plugins au démarrage
pluginManager.loadAllPlugins().catch((error) => {
  logger.error('Failed to load plugins:', error)
  auditLogger.logError(error, { context: 'plugin_loading' })
})

const ajv = new Ajv({ allErrors: true, strict: true })
const controlPayloadSchema = {
  type: 'object',
  additionalProperties: false,
  required: ['controlId', 'type', 'value', 'messageId', 'sentAt'],
  properties: {
    kind: { type: 'string', const: 'control', default: 'control' },
    controlId: { type: 'string', minLength: 1 },
    type: { type: 'string', minLength: 1 },
    value: { type: 'number' },
    meta: {
      type: 'object',
      additionalProperties: { type: 'string' },
      default: {},
    },
    messageId: { type: 'string', minLength: 1 },
    sentAt: { type: 'integer', minimum: 0 },
  },
}
const validateControlPayload = ajv.compile(controlPayloadSchema)

const mappingSeedSource = path.join(templateBase, 'config', 'mappings', 'default.json')
const mappingSeedTarget = path.join(configDir, 'mappings', 'default.json')
ensureSeedFile(mappingSeedSource, mappingSeedTarget)

function resolveConfigPath(value) {
  if (!value) return null
  return path.isAbsolute(value) ? value : path.join(configDir, value)
}

const mappingCandidates = [process.env.MAPPINGS_FILE, config.mappingsFile, 'mappings/default.json', 'mappings.json']
const resolvedMappingsPath = mappingCandidates
  .map(resolveConfigPath)
  .filter(Boolean)
  .find((candidate) => fs.existsSync(candidate))
const mappingsPath = resolvedMappingsPath ?? resolveConfigPath(config.mappingsFile ?? 'mappings/default.json')
const mappings = fs.existsSync(mappingsPath) ? readJsonSafe(mappingsPath, {}) : {}
if (!fs.existsSync(mappingsPath)) {
  logger.warn('Mapping file not found, using empty configuration', { mappingsPath })
}

// Utiliser le TokenManager amélioré
const tokenManager = getTokenManager(24 * 60 * 60 * 1000) // 24 heures TTL

// Compatibilité avec l'ancien système
const tokensPath = path.join(configDir, 'tokens.json')
const activeTokens = new Set([defaultToken])

function loadTokens() {
  if (fs.existsSync(tokensPath)) {
    try {
      const stored = JSON.parse(fs.readFileSync(tokensPath, 'utf-8'))
      if (Array.isArray(stored)) {
        stored.forEach((t) => {
          activeTokens.add(t)
          // Ajouter au TokenManager aussi
          if (t !== defaultToken) {
            tokenManager.issueToken(null, {}, 24 * 60 * 60 * 1000)
          }
        })
      }
    } catch (e) {
      logger.warn('Unable to read stored tokens', { error: e.message })
    }
  }
}

function persistTokens() {
  // Persister les tokens actifs du TokenManager
  const activeTokenList = Array.from(activeTokens)
  fs.writeFileSync(tokensPath, JSON.stringify(activeTokenList, null, 2))
}

function issueToken(clientId = null) {
  const tokenData = tokenManager.issueToken(clientId)
  activeTokens.add(tokenData.token) // Compatibilité avec l'ancien code
  persistTokens()
  return tokenData
}

function revokeToken(token) {
  const removed = activeTokens.delete(token)
  tokenManager.revokeToken(token)
  persistTokens()
  return removed
}

// Vérifier la validité d'un token avec le TokenManager
function isValidToken(token) {
  if (!token) return false
  // Vérifier d'abord avec le TokenManager
  if (tokenManager.isValid(token)) {
    return true
  }
  // Fallback sur l'ancien système pour compatibilité
  return activeTokens.has(token)
}

loadTokens()

// Nettoyage périodique des tokens expirés
setInterval(() => {
  tokenManager.cleanup()
  // Synchroniser avec activeTokens
  for (const token of activeTokens) {
    if (!tokenManager.isValid(token) && token !== defaultToken) {
      activeTokens.delete(token)
    }
  }
  persistTokens()
}, 60 * 60 * 1000) // Toutes les heures

const clientSessions = new Map()

/**
 * Crée les profils par défaut s'ils n'existent pas
 */
function ensureDefaultProfiles() {
  const defaultProfileNames = ['Utilisateur', 'Gamer', 'Streamer', 'Discord']
  const existingProfiles = getProfileSummaryWithoutDefaults()
  const existingNames = new Set(existingProfiles.map(p => p.name))
  const missingProfiles = defaultProfileNames.filter(name => !existingNames.has(name))

  if (missingProfiles.length === 0) {
    return // Tous les profils par défaut existent déjà
  }

  logger.info('Creating default profiles', { missing: missingProfiles })

  // Template Utilisateur
  if (missingProfiles.includes('Utilisateur')) {
    const utilisateurProfile = {
      id: `profile_default_user_${Date.now()}`,
      name: 'Utilisateur',
      rows: 3,
      cols: 4,
      version: 1,
      controls: [
        { id: 'btn_play_pause', type: 'BUTTON', row: 0, col: 0, label: 'Play/Pause', colorHex: '#1DB954', action: { type: 'CUSTOM', payload: '{"action": "media", "payload": {"action": "play_pause"}}' } },
        { id: 'btn_prev', type: 'BUTTON', row: 0, col: 1, label: 'Previous', colorHex: '#1DB954', action: { type: 'CUSTOM', payload: '{"action": "media", "payload": {"action": "previous"}}' } },
        { id: 'btn_next', type: 'BUTTON', row: 0, col: 2, label: 'Next', colorHex: '#1DB954', action: { type: 'CUSTOM', payload: '{"action": "media", "payload": {"action": "next"}}' } },
        { id: 'toggle_mute', type: 'TOGGLE', row: 0, col: 3, label: 'Mute', colorHex: '#F44336', action: { type: 'AUDIO', payload: '{"action": "MUTE"}' } },
        { id: 'fader_volume', type: 'FADER', row: 1, col: 0, colSpan: 4, label: 'Volume', action: { type: 'AUDIO', payload: '{"action": "SET_VOLUME", "volume": 50}' } },
        { id: 'btn_screenshot', type: 'BUTTON', row: 2, col: 0, label: 'Screenshot', colorHex: '#E91E63', action: { type: 'CUSTOM', payload: '{"action": "screenshot", "payload": {"type": "full"}}' } },
        { id: 'btn_lock', type: 'BUTTON', row: 2, col: 1, label: 'Lock', colorHex: '#607D8B', action: { type: 'CUSTOM', payload: '{"action": "system", "payload": {"action": "lock"}}' } },
        { id: 'btn_brightness', type: 'BUTTON', row: 2, col: 2, label: 'Brightness', colorHex: '#FFC107', action: { type: 'CUSTOM', payload: '{"action": "system", "payload": {"action": "brightness"}}' } },
        { id: 'btn_apps', type: 'BUTTON', row: 2, col: 3, label: 'Apps', colorHex: '#2196F3', action: { type: 'CUSTOM', payload: '{"action": "system", "payload": {"action": "open_apps"}}' } }
      ]
    }
    saveProfile(utilisateurProfile, { source: 'server', actor: 'system' })
  }

  // Template Gamer
  if (missingProfiles.includes('Gamer')) {
    const gamerProfile = {
      id: `profile_default_gaming_${Date.now()}`,
      name: 'Gamer',
      rows: 2,
      cols: 5,
      version: 1,
      controls: [
        { id: 'btn_save', type: 'BUTTON', row: 0, col: 0, label: 'Save', colorHex: '#4CAF50', action: { type: 'KEYBOARD', payload: 'CTRL+S' } },
        { id: 'btn_load', type: 'BUTTON', row: 0, col: 1, label: 'Load', colorHex: '#2196F3', action: { type: 'KEYBOARD', payload: 'CTRL+L' } },
        { id: 'btn_quick_save', type: 'BUTTON', row: 0, col: 2, label: 'Quick Save', colorHex: '#FF9800', action: { type: 'KEYBOARD', payload: 'F5' } },
        { id: 'btn_pause', type: 'BUTTON', row: 0, col: 3, label: 'Pause', colorHex: '#9E9E9E', action: { type: 'KEYBOARD', payload: 'ESC' } },
        { id: 'btn_discord_mute', type: 'BUTTON', row: 1, col: 0, label: 'Discord Mute', colorHex: '#5865F2', action: { type: 'CUSTOM', payload: '{"plugin": "discord", "action": "mute", "payload": {"toggle": true}}' } },
        { id: 'btn_discord_deafen', type: 'BUTTON', row: 1, col: 1, label: 'Discord Deafen', colorHex: '#5865F2', action: { type: 'CUSTOM', payload: '{"plugin": "discord", "action": "deafen", "payload": {"toggle": true}}' } },
        { id: 'btn_media_play', type: 'BUTTON', row: 1, col: 2, label: 'Media Play', colorHex: '#1DB954', action: { type: 'CUSTOM', payload: '{"action": "media", "payload": {"action": "play_pause"}}' } },
        { id: 'btn_media_next', type: 'BUTTON', row: 1, col: 3, label: 'Media Next', colorHex: '#1DB954', action: { type: 'CUSTOM', payload: '{"action": "media", "payload": {"action": "next"}}' } },
        { id: 'fader_game_volume', type: 'FADER', row: 1, col: 4, label: 'Game Volume', action: { type: 'AUDIO', payload: '{"action": "SET_APPLICATION_VOLUME", "application": "Game", "volume": 50}' } }
      ]
    }
    saveProfile(gamerProfile, { source: 'server', actor: 'system' })
  }

  // Template Streamer (simplifié)
  if (missingProfiles.includes('Streamer')) {
    const streamerProfile = {
      id: `profile_default_streaming_${Date.now()}`,
      name: 'Streamer',
      rows: 3,
      cols: 5,
      version: 1,
      controls: [
        { id: 'btn_start_stream', type: 'BUTTON', row: 0, col: 0, label: 'Start Stream', colorHex: '#FF5722', action: { type: 'OBS', payload: 'StartStreaming' } },
        { id: 'btn_stop_stream', type: 'BUTTON', row: 0, col: 1, label: 'Stop Stream', colorHex: '#F44336', action: { type: 'OBS', payload: 'StopStreaming' } },
        { id: 'btn_record', type: 'BUTTON', row: 0, col: 2, label: 'Record', colorHex: '#E91E63', action: { type: 'OBS', payload: 'ToggleRecording' } }
      ]
    }
    saveProfile(streamerProfile, { source: 'server', actor: 'system' })
  }

  // Template Discord (simplifié)
  if (missingProfiles.includes('Discord')) {
    const discordProfile = {
      id: `profile_default_discord_${Date.now()}`,
      name: 'Discord',
      rows: 3,
      cols: 4,
      version: 1,
      controls: [
        { id: 'btn_discord_mute', type: 'BUTTON', row: 0, col: 0, label: 'Mute', colorHex: '#5865F2', action: { type: 'CUSTOM', payload: '{"plugin": "discord", "action": "mute", "payload": {"toggle": true}}' } },
        { id: 'btn_discord_deafen', type: 'BUTTON', row: 0, col: 1, label: 'Deafen', colorHex: '#5865F2', action: { type: 'CUSTOM', payload: '{"plugin": "discord", "action": "deafen", "payload": {"toggle": true}}' } },
        { id: 'btn_discord_leave', type: 'BUTTON', row: 0, col: 2, label: 'Leave', colorHex: '#F44336', action: { type: 'CUSTOM', payload: '{"plugin": "discord", "action": "leave_voice", "payload": {}}' } },
        { id: 'btn_discord_join', type: 'BUTTON', row: 0, col: 3, label: 'Join', colorHex: '#4CAF50', action: { type: 'CUSTOM', payload: '{"plugin": "discord", "action": "join_voice", "payload": {"channelId": "default"}}' } }
      ]
    }
    saveProfile(discordProfile, { source: 'server', actor: 'system' })
  }
}

function getProfileSummaryWithoutDefaults() {
  const summaries = []
  if (!fs.existsSync(profilesDir)) return summaries
  const files = fs.readdirSync(profilesDir).filter((f) => f.endsWith('.json'))
  for (const file of files) {
    try {
      const filePath = path.join(profilesDir, file)
      const content = fs.readFileSync(filePath, 'utf-8')
      const profile = JSON.parse(content)
      summaries.push({
        id: profile.id,
        name: profile.name || profile.id,
        version: profile.version || 0,
        checksum: profile.checksum || null,
      })
    } catch (e) {
      logger.warn('Unable to read profile summary', { file, error: e.message })
    }
  }
  return summaries
}

function getProfileSummary() {
  // S'assurer que les profils par défaut existent
  ensureDefaultProfiles()
  // Retourner tous les profils (y compris ceux qui viennent d'être créés)
  return getProfileSummaryWithoutDefaults()
}

function loadProfileFromDisk(profileId) {
  const file = path.join(profilesDir, `${profileId}.json`)
  if (!fs.existsSync(file)) return null
  try {
    const content = fs.readFileSync(file, 'utf-8')
    const profile = JSON.parse(content)
    return { profile }
  } catch (e) {
    logger.error('Unable to load profile', { profileId, error: e.message })
    return null
  }
}

/**
 * Convertit un profil en mappings pour le serveur
 * Chaque contrôle avec une action devient un mapping controlId -> {action, payload}
 */
function createMappingsFromProfile(profile) {
  const profileMappings = {}
  if (!profile || !profile.controls || !Array.isArray(profile.controls)) {
    return profileMappings
  }

  for (const control of profile.controls) {
    if (!control.id || !control.action) {
      continue
    }

    const action = control.action
    let actionType = action.type || null
    let payload = action.payload || null

    // Convertir ActionType en string d'action pour le serveur
    switch (actionType) {
      case 'KEYBOARD':
        actionType = 'keyboard'
        break
      case 'OBS':
        actionType = 'obs'
        break
      case 'AUDIO':
        actionType = 'audio'
        break
      case 'SCRIPT':
        actionType = 'script'
        break
      case 'MEDIA':
        actionType = 'media'
        break
      case 'SYSTEM':
        actionType = 'system'
        break
      case 'WINDOW':
        actionType = 'window'
        break
      case 'CLIPBOARD':
        actionType = 'clipboard'
        break
      case 'SCREENSHOT':
        actionType = 'screenshot'
        break
      case 'PROCESS':
        actionType = 'process'
        break
      case 'FILE':
        actionType = 'file'
        break
      case 'CUSTOM':
        // Pour CUSTOM, le payload peut contenir un JSON avec plugin:action ou action:payload
        try {
          const customPayload = payload ? JSON.parse(payload) : {}
          if (customPayload.plugin) {
            // Format plugin:action (ex: discord:mute)
            actionType = `${customPayload.plugin}:${customPayload.action || 'execute'}`
            // Préserver la structure complète du payload pour les plugins
            payload = JSON.stringify(customPayload.payload || customPayload)
          } else if (customPayload.action) {
            // Format action directe (ex: media, system, clipboard, screenshot, etc.)
            actionType = customPayload.action
            // Si le payload contient un sous-objet payload, l'utiliser, sinon utiliser tout l'objet
            if (customPayload.payload) {
              payload = JSON.stringify(customPayload.payload)
            } else {
              // Retirer 'action' du payload pour éviter la duplication
              const { action: _, ...restPayload } = customPayload
              payload = JSON.stringify(restPayload)
            }
          } else {
            // Fallback: utiliser le payload tel quel
            actionType = 'custom'
          }
        } catch (e) {
          // Si le parsing échoue, traiter comme string simple
          logger.debug('Failed to parse CUSTOM payload, treating as string', { error: e.message, payload })
          actionType = 'custom'
        }
        break
      default:
        // Fallback: tenter d'inférer à partir du payload JSON (ancien format sans type)
        try {
          const guessed = payload ? JSON.parse(payload) : {}
          if (guessed.plugin) {
            actionType = `${guessed.plugin}:${guessed.action || 'execute'}`
            payload = JSON.stringify(guessed.payload || guessed)
          } else if (guessed.action) {
            actionType = guessed.action
            payload = guessed.payload ? JSON.stringify(guessed.payload) : JSON.stringify(guessed)
          } else {
            actionType = 'custom'
          }
          logger.warn('Inferred missing action type from payload', { controlId: control.id, inferred: actionType })
        } catch (e) {
          logger.warn('Unknown action type in profile', { controlId: control.id, actionType: action.type })
          continue
        }
    }

    profileMappings[control.id] = {
      action: actionType,
      payload: payload,
    }
  }

  logger.debug('Created mappings from profile', {
    profileId: profile.id,
    mappingsCount: Object.keys(profileMappings).length
  })

  return profileMappings
}

function saveProfile(profileData, options = {}) {
  const { source = 'unknown', actor = 'unknown' } = options
  const file = path.join(profilesDir, `${profileData.id}.json`)
  const existing = fs.existsSync(file) ? loadProfileFromDisk(profileData.id) : null
  const previousVersion = existing?.profile?.version || 0
  const conflict = existing && profileData.version && profileData.version <= previousVersion

  const version = conflict ? previousVersion + 1 : (profileData.version || previousVersion + 1)
  const profileToSave = {
    ...profileData,
    version,
    checksum: crypto.createHash('sha256').update(JSON.stringify(profileData)).digest('hex'),
  }

  fs.writeFileSync(file, JSON.stringify(profileToSave, null, 2), 'utf-8')
  logger.info('Profile saved', { id: profileData.id, version, source, actor, conflict })

  return {
    profile: profileToSave,
    conflict,
    previousVersion: existing?.profile?.version || null,
  }
}

const profileSelectPayloadSchema = {
  type: 'object',
  additionalProperties: false,
  required: ['profileId', 'messageId', 'sentAt'],
  properties: {
    kind: { type: 'string', const: 'profile:select', default: 'profile:select' },
    profileId: { type: 'string', minLength: 1 },
    resetState: { type: 'boolean', default: true },
    messageId: { type: 'string', minLength: 1 },
    sentAt: { type: 'integer', minimum: 0 },
    profile: { type: 'object' }, // Profil optionnel envoyé par le client
  },
}
const validateProfileSelectPayload = ajv.compile(profileSelectPayloadSchema)

const limiter = rateLimit({
  windowMs: 60_000,
  max: 60,
  standardHeaders: true,
  legacyHeaders: false,
})

app.use(express.json({ limit: '512kb' }))
app.use(limiter)

// Note: L'UI web n'est plus servie par le serveur
// Elle doit être servie séparément via un serveur web dédié ou localement

app.get('/health', (_, res) => res.json({ status: 'ok' }))

// Endpoint de découverte pour les clients
app.get('/discovery', (req, res) => {
  const ip = req.ip || req.socket.remoteAddress || req.headers['x-forwarded-for'] || 'unknown'

  // Retourner les informations de découverte
  res.json({
    serverId,
    serverName,
    port,
    protocol: tlsKey && tlsCert ? 'wss' : 'ws',
    host: host === '0.0.0.0' ? req.headers.host?.split(':')[0] || 'localhost' : host,
    version: '1.0.0',
    capabilities: {
      tls: !!(tlsKey && tlsCert),
      websocket: true,
      profiles: true,
      plugins: true,
    },
  })
})

// Endpoint pour générer un code de pairing
app.post('/pairing/request', (req, res) => {
  const ip = req.ip || req.socket.remoteAddress
  const clientId = req.body?.clientId || null

  // Rate limiting
  const rateLimitCheck = rateLimiter.check('ip', ip)
  if (!rateLimitCheck.allowed) {
    return res.status(429).json({
      error: 'Too many requests',
      retryAfter: rateLimitCheck.retryAfter,
    })
  }

  // Générer un code de pairing
  const qrData = pairingManager.generateQRCodeData(
    serverId,
    serverName,
    port,
    tlsKey && tlsCert ? 'wss' : 'ws'
  )

  auditLogger.logSystemEvent('pairing_request', {
    ip,
    clientId,
    serverId,
    code: qrData.code,
  })

  res.json({
    code: qrData.code,
    expiresAt: qrData.expiresAt,
    qrData, // Données pour génération QR code côté client
  })
})

// Endpoint pour finaliser le pairing
app.post('/pairing/confirm', (req, res) => {
  const ip = req.ip || req.socket.remoteAddress
  const { code, serverId: providedServerId, fingerprint } = req.body || {}

  if (!code || !providedServerId) {
    return res.status(400).json({ error: 'code and serverId required' })
  }

  // Valider le code de pairing
  if (!pairingManager.validatePairingCode(code, providedServerId)) {
    auditLogger.logAccessAttempt(ip, false, 'invalid_pairing_code', {
      endpoint: '/pairing/confirm',
    })
    return res.status(401).json({ error: 'Invalid or expired pairing code' })
  }

  // Finaliser le pairing
  const success = pairingManager.finalizePairing(code, providedServerId, fingerprint)

  if (success) {
    auditLogger.logSystemEvent('pairing_confirmed', {
      ip,
      serverId: providedServerId,
      fingerprint,
    })
    res.json({
      status: 'paired',
      serverId: providedServerId,
      message: 'Server paired successfully',
    })
  } else {
    res.status(400).json({ error: 'Failed to finalize pairing' })
  }
})

// Endpoint pour obtenir les informations d'un serveur appairé
app.get('/pairing/servers', (req, res) => {
  const servers = pairingManager.getPairedServers()
  res.json({ servers })
})

app.post('/handshake', (req, res) => {
  const ip = req.ip || req.socket.remoteAddress

  // Rate limiting par IP
  const rateLimitCheck = rateLimiter.check('ip', ip)
  if (!rateLimitCheck.allowed) {
    auditLogger.logAccessAttempt(ip, false, 'rate_limit_exceeded', { endpoint: '/handshake' })
    return res.status(429).json({
      error: 'Too many requests',
      retryAfter: rateLimitCheck.retryAfter
    })
  }

  if (!req.body?.secret) {
    auditLogger.logAccessAttempt(ip, false, 'missing_secret', { endpoint: '/handshake' })
    return res.status(400).json({ error: 'secret required' })
  }

  if (req.body.secret !== handshakeSecret) {
    auditLogger.logAccessAttempt(ip, false, 'invalid_secret', { endpoint: '/handshake' })
    logger.warn('Handshake rejected: invalid secret', { ip })
    return res.status(401).json({ error: 'invalid secret' })
  }

  const clientId = req.body?.clientId || null
  const { token, expiresAt, expiresIn } = issueToken(clientId)

  auditLogger.logAuth('token_issue', clientId, ip, true)
  logger.info('Handshake successful', { ip, clientId })
  res.json({ token, expiresAt, expiresIn })
})

app.post('/handshake/revoke', (req, res) => {
  const ip = req.ip || req.socket.remoteAddress
  const token = req.body?.token

  if (!token) {
    auditLogger.logAccessAttempt(ip, false, 'missing_token', { endpoint: '/handshake/revoke' })
    return res.status(400).json({ error: 'token required' })
  }

  if (!isValidToken(token)) {
    auditLogger.logAuth('token_revoke', null, ip, false, 'invalid_token')
    logger.warn('Revocation requested for invalid token', { ip })
    return res.status(404).json({ error: 'token not found or already invalid' })
  }

  const tokenInfo = tokenManager.getTokenInfo(token)
  revokeToken(token)
  auditLogger.logAuth('token_revoke', tokenInfo?.clientId || null, ip, true)
  logger.info('Token revoked', { ip: req.ip })
  res.json({ status: 'revoked' })
})

app.get('/profiles', (_, res) => {
  // Utiliser le cache pour optimiser les requêtes fréquentes
  const cacheKey = 'profiles:summary'
  let cached = cacheManager.get(cacheKey)

  if (!cached) {
    const profiles = getProfileSummary()
    cacheManager.set(cacheKey, profiles, 60000) // Cache 1 minute pour les profils
    cached = profiles
  }

  res.json({ profiles: cached })
})

app.get('/profiles/:id', (req, res) => {
  const profileId = req.params.id
  const cacheKey = `profile:${profileId}`

  // Vérifier le cache
  let cached = cacheManager.get(cacheKey)
  if (cached) {
    res.type('json').send(cached)
    return
  }

  const file = path.join(profilesDir, `${profileId}.json`)
  if (!fs.existsSync(file)) {
    return res.status(404).json({ error: 'Profile not found' })
  }

  const content = fs.readFileSync(file, 'utf-8')
  cacheManager.set(cacheKey, content, 300000) // Cache 5 minutes
  res.type('json').send(content)
})

const profileSchemaCandidate = fs.existsSync(path.join(configDir, 'profile.schema.json'))
  ? path.join(configDir, 'profile.schema.json')
  : path.join(templateBase, 'config', 'profile.schema.json')
const profileValidator = fs.existsSync(profileSchemaCandidate)
  ? ajv.compile(JSON.parse(fs.readFileSync(profileSchemaCandidate, 'utf-8')))
  : null

app.post('/profiles/:id', (req, res) => {
  if (!profileValidator) {
    return res.status(500).json({ error: 'profile validator unavailable' })
  }

  if (!profileValidator(req.body)) {
    logger.warn('Profile validation failed', { id: req.params.id, errors: profileValidator.errors })
    return res.status(400).json({ error: 'invalid profile', details: profileValidator.errors })
  }

  if (req.body.id !== req.params.id) {
    logger.warn('Profile validation failed: id mismatch', { requested: req.params.id, provided: req.body.id })
    return res.status(400).json({ error: 'id mismatch' })
  }

  const { profile, conflict, previousVersion } = saveProfile(req.body, { source: 'http' })

  // Invalider le cache pour ce profil et la liste des profils
  cacheManager.delete(`profile:${profile.id}`)
  cacheManager.delete('profiles:summary')

  res.json({
    status: 'saved',
    version: profile.version,
    checksum: profile.checksum,
    conflict,
    previousVersion,
  })
})

let server
let wss
let totalConnections = 0
let discoveryService = null

if (tlsKey && tlsCert && fs.existsSync(tlsKey) && fs.existsSync(tlsCert)) {
  const credentials = {
    key: fs.readFileSync(tlsKey),
    cert: fs.readFileSync(tlsCert),
  }
  server = https.createServer(credentials, app)
  server.listen(port, host, () => {
    logger.info(`Server listening with TLS on https://${host === '0.0.0.0' ? 'all interfaces' : host}:${port}`)
    // Initialiser les profils par défaut au démarrage
    ensureDefaultProfiles()
    // Démarrer le service de découverte
    startDiscoveryService()
  })
} else {
  server = http.createServer(app)
  server.listen(port, host, () => {
    logger.info(`Server listening on http://${host === '0.0.0.0' ? 'all interfaces' : host}:${port}`)
    // Initialiser les profils par défaut au démarrage
    ensureDefaultProfiles()
    // Démarrer le service de découverte
    startDiscoveryService()
  })
}

// Fonction pour démarrer le service de découverte
function startDiscoveryService() {
  try {
    const protocol = tlsKey && tlsCert ? 'wss' : 'ws'
    discoveryService = new DiscoveryService(serverId, serverName, port, protocol, logger)
    discoveryService.start()

    // Nettoyer à l'arrêt
    process.on('SIGINT', () => {
      if (discoveryService) {
        discoveryService.stop()
      }
    })
    process.on('SIGTERM', () => {
      if (discoveryService) {
        discoveryService.stop()
      }
    })
  } catch (error) {
    logger.warn('Failed to start discovery service', { error: error.message })
    // Continuer sans découverte si cela échoue
  }
}

wss = new WebSocketServer({
  server,
  path: '/ws',
  perMessageDeflate: {
    serverNoContextTakeover: true,
    clientNoContextTakeover: true,
    threshold: 512,
  },
})

app.get('/diagnostics', (req, res) => {
  res.json({
    status: 'ok',
    logLevel: logger.level,
    uptimeSeconds: Math.round((Date.now() - serverStartedAt) / 1000),
    activeWebsocketConnections: wss?.clients?.size ?? 0,
    totalConnections,
    plugins: pluginManager.listPlugins(),
    tokens: tokenManager.getStats(),
    rateLimiter: rateLimiter.getStats(),
    actionQueue: actionQueue.getStatus(),
    performance: performanceMonitor.getReport(),
  })
})

// Endpoint dédié pour les métriques de performance
app.get('/performance', (req, res) => {
  res.json(performanceMonitor.getReport())
})

// Endpoint pour les statistiques d'erreurs
app.get('/errors', (req, res) => {
  res.json(errorHandler.getErrorStats())
})

// API pour la gestion des tokens
app.get('/tokens/info', (req, res) => {
  const token = req.headers['authorization']?.toString().replace('Bearer ', '')
  if (!token) {
    return res.status(401).json({ error: 'token required' })
  }
  const info = tokenManager.getTokenInfo(token)
  if (!info) {
    return res.status(404).json({ error: 'token not found or invalid' })
  }
  res.json(info)
})

app.post('/tokens/rotate', (req, res) => {
  const oldToken = req.headers['authorization']?.toString().replace('Bearer ', '')
  if (!oldToken) {
    return res.status(401).json({ error: 'token required' })
  }
  try {
    const newToken = tokenManager.rotateToken(oldToken, req.body?.clientId, req.body?.metadata || {})
    res.json(newToken)
  } catch (error) {
    res.status(400).json({ error: error.message })
  }
})

app.post('/tokens/revoke', (req, res) => {
  const token = req.body?.token || req.headers['authorization']?.toString().replace('Bearer ', '')
  if (!token) {
    return res.status(400).json({ error: 'token required' })
  }
  revokeToken(token)
  res.json({ status: 'revoked' })
})

// API pour gérer les plugins
app.get('/plugins', (req, res) => {
  res.json({ plugins: pluginManager.listPlugins() })
})

app.post('/plugins/:name/enable', (req, res) => {
  try {
    pluginManager.enablePlugin(req.params.name)
    res.json({ status: 'enabled' })
  } catch (error) {
    res.status(404).json({ error: error.message })
  }
})

app.post('/plugins/:name/disable', (req, res) => {
  try {
    pluginManager.disablePlugin(req.params.name)
    res.json({ status: 'disabled' })
  } catch (error) {
    res.status(404).json({ error: error.message })
  }
})

app.get('/plugins/:name/config', (req, res) => {
  try {
    const config = pluginManager.getPluginConfig(req.params.name)
    res.json({ config })
  } catch (error) {
    res.status(404).json({ error: error.message })
  }
})

app.post('/plugins/:name/config', (req, res) => {
  try {
    pluginManager.updatePluginConfig(req.params.name, req.body)
    res.json({ status: 'updated' })
  } catch (error) {
    res.status(404).json({ error: error.message })
  }
})

// Note: L'UI web n'est plus servie par le serveur
// Toutes les routes non-API retournent 404
app.get('*', (req, res) => {
  // Ne pas servir l'UI pour les routes API ou WebSocket
  if (req.path.startsWith('/api/') || req.path.startsWith('/ws')) {
    return res.status(404).json({ error: 'Not found' })
  }
  // Retourner 404 pour toutes les autres routes
  res.status(404).json({ error: 'Not found', message: 'UI web is not served by this server. Please use a separate web server.' })
})

wss.on('connection', (ws, req) => {
  const auth = req.headers['authorization']
  const token = auth?.toString().replace('Bearer ', '')
  const authRequired = activeTokens.size > 0 || tokenManager.getStats().active > 0

  if (authRequired && (!token || !isValidToken(token))) {
    auditLogger.logAccessAttempt(req.socket.remoteAddress, false, 'invalid_token', {
      endpoint: 'websocket',
      hasToken: !!token,
    })
    logger.warn('Connection refused: invalid token', { ip: req.socket.remoteAddress })
    ws.close(4001, 'invalid token')
    return
  }

  const tokenInfo = token ? tokenManager.getTokenInfo(token) : null
  auditLogger.logSystemEvent('websocket_connect', {
    ip: req.socket.remoteAddress,
    clientId: tokenInfo?.clientId,
    authenticated: !!token && isValidToken(token),
  })

  logger.info('Client connected', { ip: req.socket.remoteAddress })
  totalConnections += 1

  const clientId = `client_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`
  const session = {
    clientId,
    activeProfileId: null,
    controlStates: {},
    profileMappings: {},
    connectedAt: Date.now(),
  }
  clientSessions.set(ws, session)

  const recentMessages = []

  ws.on('message', async (message) => {
    const receivedAt = Date.now()
    const messageStr = message.toString()

    // Gérer le heartbeat (emoji 💾) - exclure du rate limiting
    if (messageStr === '💾' || messageStr === '\uD83D\uDCBE') {
      // Heartbeat - juste répondre avec un ack
      ws.send(JSON.stringify({
        type: 'ack',
        status: 'ok',
        receivedAt: Date.now(),
      }))
      return
    }

    // Rate limiting pour les autres messages
    const now = Date.now()
    recentMessages.push(now)
    const windowStart = now - 1000
    while (recentMessages.length && recentMessages[0] < windowStart) recentMessages.shift()
    if (recentMessages.length > 15) {
      logger.warn('Rate limit exceeded on websocket', { ip: req.socket.remoteAddress })
      ws.send(
        JSON.stringify({
          type: 'ack',
          status: 'error',
          error: 'rate limit exceeded',
          receivedAt,
        })
      )
      return
    }

    let payload
    try {
      payload = JSON.parse(messageStr)
    } catch (e) {
      logger.warn('Unable to parse message', { error: e.message })
      return
    }

    const kind = payload.kind || 'control'
    const session = clientSessions.get(ws)

    if (kind === 'control') {
      if (!validateControlPayload(payload)) {
        logger.warn('Payload rejected by schema', { errors: validateControlPayload.errors })
        ws.send(
          JSON.stringify({
            type: 'ack',
            status: 'error',
            error: 'invalid payload',
            receivedAt,
          })
        )
        return
      }

      const ack = {
        type: 'ack',
        messageId: payload.messageId,
        controlId: payload.controlId,
        receivedAt,
      }

      // Chercher le mapping dans les mappings de session (profil actif) ou dans les mappings globaux
      let mapping = null
      if (session && session.profileMappings && payload.controlId) {
        mapping = session.profileMappings[payload.controlId]
      }
      if (!mapping && payload.controlId) {
        mapping = mappings[payload.controlId]
      }

      if (session) {
        session.controlStates[payload.controlId] = payload.value
      }

      if (!mapping) {
        logger.debug('No mapping found for control', { controlId: payload.controlId, activeProfile: session?.activeProfileId })
        ws.send(JSON.stringify({ ...ack, status: 'ignored', error: 'no mapping found' }))
        return
      }

      try {
        // Rate limiting par action
        const actionCheck = rateLimiter.check('action', `${mapping.action}:${payload.controlId}`)
        if (!actionCheck.allowed) {
          auditLogger.logControlAction(
            payload.controlId,
            mapping.action,
            session?.clientId || 'unknown',
            false,
            { error: 'rate_limit_exceeded' }
          )
          ws.send(JSON.stringify({
            ...ack,
            status: 'error',
            error: 'rate limit exceeded for this action',
            retryAfter: actionCheck.retryAfter,
          }))
          return
        }

        // Mesurer la latence de l'action
        const actionStartTime = performance.now()

        // Vérifier si l'action est gérée par un plugin
        const actionParts = mapping.action.split(':')
        if (actionParts.length === 2) {
          // Format: plugin:action (ex: discord:mute)
          const [pluginName, pluginAction] = actionParts
          try {
            await pluginManager.executeAction(pluginName, pluginAction, mapping.payload)

            auditLogger.logPluginAction(
              pluginName,
              pluginAction,
              session?.clientId || 'unknown',
              true
            )
            auditLogger.logControlAction(
              payload.controlId,
              mapping.action,
              session?.clientId || 'unknown',
              true,
              { value: payload.value }
            )

            ws.send(JSON.stringify({ ...ack, status: 'ok', processedAt: Date.now() }))
            return
          } catch (pluginError) {
            auditLogger.logPluginAction(
              pluginName,
              pluginAction,
              session?.clientId || 'unknown',
              false,
              pluginError
            )
            logger.warn(`Plugin action failed, trying built-in actions: ${pluginError.message}`)
            // Continuer avec les actions intégrées
          }
        }

        // Parser et injecter les valeurs dynamiques dans le payload
        let actionPayload = mapping.payload
        if (typeof actionPayload === 'string') {
          try {
            actionPayload = JSON.parse(actionPayload)
          } catch (e) {
            // Garder comme string si ce n'est pas du JSON
          }
        }

        // Injecter la valeur dynamique (payload.value) dans le payload de l'action
        if (typeof actionPayload === 'object' && actionPayload !== null) {
          // Pour les actions AUDIO
          if (mapping.action === 'audio') {
            if (actionPayload.action === 'SET_VOLUME' || actionPayload.action === 'SET_APPLICATION_VOLUME' || actionPayload.action === 'SET_DEVICE_VOLUME') {
              // Convertir value (0-1) en pourcentage (0-100)
              const volumePercent = Math.round(payload.value * 100)
              actionPayload.volume = volumePercent
            } else if (actionPayload.action === 'SET_BALANCE') {
              // Convertir value (0-1) en balance (-100 à 100)
              // value 0 = -100 (gauche), 0.5 = 0 (centre), 1 = 100 (droite)
              const balance = Math.round((payload.value - 0.5) * 200)
              actionPayload.balance = balance
            } else if (actionPayload.action === 'MUTE' || actionPayload.action === 'UNMUTE') {
              // Pour les toggles : utiliser value > 0.5 pour true/false
              actionPayload.mute = payload.value > 0.5
            }
          }
          // Pour les actions OBS
          else if (mapping.action === 'obs') {
            if (actionPayload.action === 'SET_VOLUME' && actionPayload.params) {
              // Convertir value (0-1) en dB (exemple: 0-1 → -100dB à 0dB)
              // OBS utilise généralement -60dB à 0dB, donc on ajuste
              const volumeDb = (payload.value * 60) - 60 // 0-1 → -60dB à 0dB
              actionPayload.params.volumeDb = volumeDb
            }
          }
          // Pour les actions MEDIA, SYSTEM, SCREENSHOT, etc.
          else if (mapping.action === 'media' && actionPayload.payload) {
            // Le payload est déjà structuré, pas besoin d'injection
          } else if (mapping.action === 'system' && actionPayload.payload) {
            // Injecter la valeur si nécessaire (ex: brightness)
            if (actionPayload.payload.action === 'brightness') {
              const brightness = Math.round(payload.value * 100)
              actionPayload.payload.brightness = brightness
            }
          }
        }

        // Actions intégrées
        switch (mapping.action) {
          case 'keyboard':
            await handleKeyboard(mapping.payload)
            break
          case 'obs':
            await handleObs(actionPayload)
            break
          case 'script':
            await handleScript(mapping.payload)
            break
          case 'audio':
            await handleAudio(actionPayload)
            break
          case 'media':
            if (os.platform() === 'win32') {
              // Parser le payload si nécessaire
              let mediaPayload = actionPayload
              if (typeof mediaPayload === 'object' && mediaPayload.payload) {
                mediaPayload = mediaPayload.payload
              }
              const mediaAction = mediaPayload?.action || mediaPayload || 'play_pause'
              await handleMediaKeys(mediaAction, mediaPayload)
            } else {
              throw new Error('Media keys are only supported on Windows')
            }
            break
          case 'system':
            if (os.platform() !== 'win32') {
              throw new Error('System actions are only supported on Windows')
            }
            // Parser le payload si nécessaire
            let systemPayload = actionPayload
            if (typeof systemPayload === 'object' && systemPayload.payload) {
              systemPayload = systemPayload.payload
            }
            const systemAction = systemPayload?.action || 'lock'
            switch (systemAction) {
              case 'lock':
                await SystemWindows.lockSystem()
                break
              case 'sleep':
                await SystemWindows.sleepSystem()
                break
              case 'shutdown':
                await SystemWindows.shutdownSystem(systemPayload?.delaySeconds)
                break
              case 'restart':
                await SystemWindows.restartSystem(systemPayload?.delaySeconds)
                break
              case 'cancel_shutdown':
                await SystemWindows.cancelShutdown()
                break
              case 'brightness':
                if (systemPayload?.brightness !== undefined) {
                  await SystemWindows.setBrightness(systemPayload.brightness)
                } else {
                  throw new Error('Brightness value required')
                }
                break
              case 'open_apps':
                await SystemWindows.openApps()
                break
              default:
                throw new Error(`Unknown system action: ${systemAction}`)
            }
            break
          case 'window':
            if (os.platform() !== 'win32') {
              throw new Error('Window management is only supported on Windows')
            }
            // Parser le payload si nécessaire
            let windowPayload = actionPayload
            if (typeof windowPayload === 'object' && windowPayload.payload) {
              windowPayload = windowPayload.payload
            }
            await SystemWindows.manageWindow(windowPayload?.action || actionPayload?.action, windowPayload?.windowTitle || actionPayload?.windowTitle)
            break
          case 'clipboard':
            if (os.platform() !== 'win32') {
              throw new Error('Clipboard is only supported on Windows')
            }
            // Parser le payload si nécessaire
            let clipboardPayload = actionPayload
            if (typeof clipboardPayload === 'object' && clipboardPayload.payload) {
              clipboardPayload = clipboardPayload.payload
            }
            const clipboardAction = clipboardPayload?.action || 'read'
            switch (clipboardAction) {
              case 'read':
                return await ClipboardWindows.readClipboard()
              case 'write':
                if (!clipboardPayload?.text) throw new Error('Text required for clipboard write')
                await ClipboardWindows.writeClipboard(clipboardPayload.text)
                break
              case 'clear':
                await ClipboardWindows.clearClipboard()
                break
              default:
                throw new Error(`Unknown clipboard action: ${clipboardAction}`)
            }
            break
          case 'screenshot':
            if (os.platform() !== 'win32') {
              throw new Error('Screenshot is only supported on Windows')
            }
            // Parser le payload si nécessaire
            let screenshotPayload = actionPayload
            if (typeof screenshotPayload === 'object' && screenshotPayload.payload) {
              screenshotPayload = screenshotPayload.payload
            }
            const screenshotType = screenshotPayload?.type || 'full'
            switch (screenshotType) {
              case 'full':
                await ScreenshotWindows.captureFullScreen(screenshotPayload?.outputPath)
                break
              case 'window':
                if (!screenshotPayload?.windowTitle) throw new Error('Window title required')
                await ScreenshotWindows.captureWindow(screenshotPayload.windowTitle, screenshotPayload?.outputPath)
                break
              default:
                throw new Error(`Unknown screenshot type: ${screenshotType}`)
            }
            break
          case 'process':
            if (os.platform() !== 'win32') {
              throw new Error('Process management is only supported on Windows')
            }
            // Parser le payload si nécessaire
            let processPayload = actionPayload
            if (typeof processPayload === 'object' && processPayload.payload) {
              processPayload = processPayload.payload
            }
            const processAction = processPayload?.action || actionPayload?.action || 'list'
            switch (processAction) {
              case 'list':
                return await ProcessWindows.listProcessesWindows()
              case 'start':
                if (!processPayload?.command && !actionPayload?.command) throw new Error('Command required for process start')
                await ProcessWindows.startProcessWindows(
                  processPayload?.command || actionPayload?.command,
                  processPayload?.args || actionPayload?.args || [],
                  processPayload?.options || actionPayload?.options || {}
                )
                break
              case 'stop':
                if (!processPayload?.processId && !processPayload?.processName && !actionPayload?.processId && !actionPayload?.processName) {
                  throw new Error('processId or processName required for process stop')
                }
                if (processPayload?.processId || actionPayload?.processId) {
                  await ProcessWindows.stopProcessWindows(processPayload?.processId || actionPayload?.processId)
                } else {
                  await ProcessWindows.stopProcessByNameWindows(
                    processPayload?.processName || actionPayload?.processName,
                    (processPayload?.force || actionPayload?.force) || false
                  )
                }
                break
              case 'info':
                if (!processPayload?.processId && !actionPayload?.processId) throw new Error('processId required for process info')
                return await ProcessWindows.getProcessInfoWindows(processPayload?.processId || actionPayload?.processId)
              case 'isRunning':
                if (!processPayload?.processName && !actionPayload?.processName) throw new Error('processName required')
                return await ProcessWindows.isProcessRunningWindows(processPayload?.processName || actionPayload?.processName)
              default:
                throw new Error(`Unknown process action: ${processAction}`)
            }
            break
          case 'file':
            if (os.platform() !== 'win32') {
              throw new Error('File operations are only supported on Windows')
            }
            // Parser le payload si nécessaire
            let filePayload = actionPayload
            if (typeof filePayload === 'object' && filePayload.payload) {
              filePayload = filePayload.payload
            }
            const fileAction = filePayload?.action || actionPayload?.action || 'read'
            switch (fileAction) {
              case 'open':
                if (!filePayload?.filePath && !actionPayload?.filePath) throw new Error('filePath required for file open')
                await FilesWindows.openFileWindows(filePayload?.filePath || actionPayload?.filePath)
                break
              case 'create':
                if (!filePayload?.filePath && !actionPayload?.filePath) throw new Error('filePath required for file create')
                await FilesWindows.createFileWindows(
                  filePayload?.filePath || actionPayload?.filePath,
                  (filePayload?.content || actionPayload?.content) || ''
                )
                break
              case 'delete':
                if (!filePayload?.filePath && !actionPayload?.filePath) throw new Error('filePath required for file delete')
                await FilesWindows.deleteFileWindows(filePayload?.filePath || actionPayload?.filePath)
                break
              case 'list':
                if (!filePayload?.directoryPath && !actionPayload?.directoryPath) throw new Error('directoryPath required for file list')
                return await FilesWindows.listFilesWindows(
                  filePayload?.directoryPath || actionPayload?.directoryPath,
                  filePayload?.options || actionPayload?.options || {}
                )
              case 'read':
                if (!filePayload?.filePath && !actionPayload?.filePath) throw new Error('filePath required for file read')
                return await FilesWindows.readFileWindows(
                  filePayload?.filePath || actionPayload?.filePath,
                  (filePayload?.encoding || actionPayload?.encoding) || 'utf-8'
                )
              case 'write':
                if (!filePayload?.filePath && !actionPayload?.filePath) throw new Error('filePath required for file write')
                if (filePayload?.content === undefined && actionPayload?.content === undefined) throw new Error('content required for file write')
                await FilesWindows.writeFileWindows(
                  filePayload?.filePath || actionPayload?.filePath,
                  filePayload?.content || actionPayload?.content,
                  (filePayload?.encoding || actionPayload?.encoding) || 'utf-8'
                )
                break
              case 'exists':
                if (!filePayload?.filePath && !actionPayload?.filePath) throw new Error('filePath required for file exists')
                return await FilesWindows.fileExistsWindows(filePayload?.filePath || actionPayload?.filePath)
              case 'info':
                if (!filePayload?.filePath && !actionPayload?.filePath) throw new Error('filePath required for file info')
                return await FilesWindows.getFileInfoWindows(filePayload?.filePath || actionPayload?.filePath)
              case 'mkdir':
                if (!filePayload?.directoryPath && !actionPayload?.directoryPath) throw new Error('directoryPath required for mkdir')
                await FilesWindows.createDirectoryWindows(filePayload?.directoryPath || actionPayload?.directoryPath)
                break
              case 'rmdir':
                if (!filePayload?.directoryPath && !actionPayload?.directoryPath) throw new Error('directoryPath required for rmdir')
                await FilesWindows.deleteDirectoryWindows(
                  filePayload?.directoryPath || actionPayload?.directoryPath,
                  (filePayload?.recursive || actionPayload?.recursive) || false
                )
                break
              default:
                throw new Error(`Unknown file action: ${fileAction}`)
            }
            break
          default:
            logger.warn('Unknown action', { action: mapping.action })
        }

        const actionLatency = performance.now() - actionStartTime

        // Enregistrer les métriques de performance
        performanceMonitor.recordAction(mapping.action, actionLatency, true)
        performanceMonitor.recordWebSocketMessage(actionLatency)

        auditLogger.logControlAction(
          payload.controlId,
          mapping.action,
          session?.clientId || 'unknown',
          true,
          { value: payload.value, latency: actionLatency }
        )

        ws.send(JSON.stringify({ ...ack, status: 'ok', processedAt: Date.now() }))
      } catch (e) {
        logger.error('Unable to execute action', { error: e.message, stack: e.stack })
        ws.send(
          JSON.stringify({
            ...ack,
            status: 'error',
            processedAt: Date.now(),
            error: e?.message ?? 'unknown error',
          })
        )
      }
      return
    }

    if (kind === 'profile:select') {
      if (!validateProfileSelectPayload(payload)) {
        logger.warn('Profile selection rejected by schema', { errors: validateProfileSelectPayload.errors })
        ws.send(JSON.stringify({ type: 'profile:select:ack', status: 'error', error: 'invalid payload', receivedAt }))
        return
      }

      const incomingProfile = payload.profile
      const diskProfileData = loadProfileFromDisk(payload.profileId)
      const diskProfile = diskProfileData?.profile

      // Choisir le profil le plus récent entre le disque et le payload
      let selectedProfile = diskProfile
      if (incomingProfile) {
        const incomingVersion = incomingProfile.version || 0
        const diskVersion = diskProfile?.version || 0
        const incomingChecksum = incomingProfile.checksum
        const diskChecksum = diskProfile?.checksum
        const isNewer =
          incomingVersion > diskVersion ||
          (incomingVersion === diskVersion && incomingChecksum && incomingChecksum !== diskChecksum)

        if (!diskProfile || isNewer) {
          selectedProfile = incomingProfile
          saveProfile(incomingProfile, { source: 'websocket', actor: session?.clientId ?? 'ws' })
          logger.info('Profile payload accepted (newer than disk)', {
            profileId: incomingProfile.id,
            incomingVersion,
            diskVersion,
          })
        }
      }

      if (!selectedProfile) {
        ws.send(
          JSON.stringify({
            type: 'profile:select:ack',
            status: 'error',
            error: 'profile not found',
            receivedAt,
            profileId: payload.profileId,
          })
        )
        return
      }

      // Convertir le profil en mappings et les activer pour cette session
      const profileMappings = createMappingsFromProfile(selectedProfile)
      if (session) {
        session.activeProfileId = payload.profileId
        session.profileMappings = profileMappings
        if (payload.resetState !== false) {
          session.controlStates = {}
        }
      }

      // Activer les mappings pour cette session (fusionner avec les mappings globaux)
      Object.assign(mappings, profileMappings)
      logger.info('Profile mappings activated', {
        profileId: payload.profileId,
        mappingsCount: Object.keys(profileMappings).length
      })

      ws.send(
        JSON.stringify({
          type: 'profile:select:ack',
          status: 'ok',
          profileId: payload.profileId,
          receivedAt,
          profile: selectedProfile,
          mappingsCount: Object.keys(profileMappings).length,
        })
      )
      return
    }

    if (kind === 'profile:update') {
      if (!profileValidator) {
        ws.send(JSON.stringify({ type: 'profile:update:ack', status: 'error', error: 'profile validator unavailable', receivedAt }))
        return
      }

      if (!profileValidator(payload.profile)) {
        ws.send(
          JSON.stringify({
            type: 'profile:update:ack',
            status: 'error',
            error: 'invalid profile',
            details: profileValidator.errors,
            receivedAt,
          })
        )
        return
      }

      const { profile: saved, conflict, previousVersion } = saveProfile(payload.profile, {
        source: 'websocket',
        actor: session?.clientId ?? 'ws',
      })

      auditLogger.logProfileChange('update', saved.id, session?.clientId ?? 'unknown', {
        version: saved.version,
        conflict,
        previousVersion,
        controlsCount: saved.controls?.length || 0
      })

      ws.send(
        JSON.stringify({
          type: 'profile:update:ack',
          status: 'ok',
          profileId: saved.id,
          version: saved.version,
          conflict,
          previousVersion,
          receivedAt,
        })
      )
      return
    }

    logger.debug('Control payload received', {
      controlId: payload.controlId,
      type: payload.type,
      meta: payload.meta,
    })

    ws.send(JSON.stringify({ type: 'error', error: 'unknown message kind', kind }))
  })

  ws.on('close', () => {
    clientSessions.delete(ws)
    logger.info('Client disconnected', { ip: req.socket.remoteAddress })
  })
  ws.on('error', (error) => {
    clientSessions.delete(ws)
    logger.error('Websocket error', { error: error.message })
  })
})
