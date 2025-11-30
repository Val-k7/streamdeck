import express from 'express'
import rateLimit from 'express-rate-limit'
import { WebSocketServer, WebSocket } from 'ws'
import fs from 'fs'
import path from 'path'
import http from 'http'
import https from 'https'
import crypto from 'crypto'
import { fileURLToPath } from 'url'
import { handleKeyboard } from './actions/keyboard.js'
import { handleObs } from './actions/obs.js'
import { handleScript } from './actions/scripts.js'
import { handleAudio } from './actions/audio.js'
import Ajv from 'ajv'
import winston from 'winston'
import DailyRotateFile from 'winston-daily-rotate-file'

const __filename = fileURLToPath(import.meta.url)
const __dirname = path.dirname(__filename)

const app = express()
const port = process.env.PORT || 4455
const defaultToken = process.env.DECK_TOKEN || 'change-me'
const handshakeSecret = process.env.HANDSHAKE_SECRET || defaultToken
const tlsKey = process.env.TLS_KEY_PATH
const tlsCert = process.env.TLS_CERT_PATH

const logsDir = path.join(__dirname, 'logs')
fs.mkdirSync(logsDir, { recursive: true })

const logger = winston.createLogger({
  level: 'info',
  format: winston.format.combine(winston.format.timestamp(), winston.format.json()),
  transports: [
    new winston.transports.Console({ level: 'info' }),
    new DailyRotateFile({
      dirname: logsDir,
      filename: 'security-%DATE%.log',
      datePattern: 'YYYY-MM-DD',
      zippedArchive: true,
      maxFiles: '14d',
      maxSize: '5m',
      level: 'info',
    }),
  ],
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

const profileUpdatePayloadSchema = {
  type: 'object',
  additionalProperties: false,
  required: ['kind', 'profile', 'messageId', 'sentAt'],
  properties: {
    kind: { type: 'string', const: 'profile:update' },
    profile: { type: 'object' },
    messageId: { type: 'string', minLength: 1 },
    sentAt: { type: 'integer', minimum: 0 },
    actor: { type: 'string' },
  },
}
const validateProfileUpdatePayload = ajv.compile(profileUpdatePayloadSchema)

const profileSelectPayloadSchema = {
  type: 'object',
  additionalProperties: false,
  required: ['kind', 'profileId', 'messageId', 'sentAt'],
  properties: {
    kind: { type: 'string', const: 'profile:select' },
    profileId: { type: 'string', minLength: 1 },
    messageId: { type: 'string', minLength: 1 },
    sentAt: { type: 'integer', minimum: 0 },
    resetState: { type: 'boolean', default: true },
  },
}
const validateProfileSelectPayload = ajv.compile(profileSelectPayloadSchema)

const mappingsPath = path.join(__dirname, 'config', 'mappings.json')
const mappings = fs.existsSync(mappingsPath)
  ? JSON.parse(fs.readFileSync(mappingsPath, 'utf-8'))
  : {}

const profilesDir = path.join(__dirname, 'profiles')
fs.mkdirSync(profilesDir, { recursive: true })

const profileRegistry = new Map()

function computeChecksum(payload) {
  const hash = crypto.createHash('sha256')
  hash.update(JSON.stringify(payload))
  return hash.digest('hex')
}

function loadProfileFromDisk(id) {
  const file = path.join(profilesDir, `${id}.json`)
  if (!fs.existsSync(file)) return null
  try {
    const parsed = JSON.parse(fs.readFileSync(file, 'utf-8'))
    const version = Number.isInteger(parsed.version) ? parsed.version : 0
    const checksum = parsed.checksum || computeChecksum(parsed)
    const updatedAt = parsed.updatedAt || null
    profileRegistry.set(id, { version, checksum, updatedAt })
    return { profile: parsed, version, checksum, updatedAt }
  } catch (e) {
    logger.warn('Unable to parse profile from disk', { id, error: e.message })
    return null
  }
}

function loadProfileMetadata() {
  const files = fs.readdirSync(profilesDir).filter((file) => file.endsWith('.json'))
  files.forEach((file) => {
    const id = path.basename(file, '.json')
    loadProfileFromDisk(id)
  })
}

loadProfileMetadata()

function getProfileSummary() {
  return [...profileRegistry.entries()].map(([id, meta]) => ({ id, ...meta }))
}

function saveProfile(profile, { source = 'http', actor = 'unknown' } = {}) {
  const existing = profileRegistry.get(profile.id) || loadProfileFromDisk(profile.id)
  const conflict = existing && Number.isInteger(profile.version) && profile.version < existing.version
  const nextVersion = (existing?.version ?? 0) + 1
  const updatedAt = Date.now()
  const normalized = {
    ...profile,
    version: nextVersion,
    updatedAt,
  }
  normalized.checksum = computeChecksum(normalized)
  fs.writeFileSync(path.join(profilesDir, `${profile.id}.json`), JSON.stringify(normalized, null, 2))
  profileRegistry.set(profile.id, { version: normalized.version, checksum: normalized.checksum, updatedAt })

  if (conflict) {
    logger.warn('Profile version conflict resolved with last-writer-wins', {
      id: profile.id,
      providedVersion: profile.version,
      storedVersion: existing.version,
      actor,
      source,
    })
  } else {
    logger.info('Profile saved', { id: profile.id, version: normalized.version, actor, source })
  }

  broadcastProfileUpdate(normalized, { source, conflict, actor })
  return { profile: normalized, conflict, previousVersion: existing?.version ?? 0 }
}

const tokensPath = path.join(__dirname, 'config', 'tokens.json')
const activeTokens = new Set([defaultToken])
function loadTokens() {
  if (fs.existsSync(tokensPath)) {
    try {
      const stored = JSON.parse(fs.readFileSync(tokensPath, 'utf-8'))
      if (Array.isArray(stored)) stored.forEach((t) => activeTokens.add(t))
    } catch (e) {
      logger.warn('Unable to read stored tokens', { error: e.message })
    }
  }
}

function persistTokens() {
  fs.writeFileSync(tokensPath, JSON.stringify([...activeTokens], null, 2))
}

function issueToken() {
  const token = crypto.randomBytes(32).toString('hex')
  activeTokens.add(token)
  persistTokens()
  return token
}

function revokeToken(token) {
  const removed = activeTokens.delete(token)
  persistTokens()
  return removed
}

loadTokens()

const limiter = rateLimit({
  windowMs: 60_000,
  max: 60,
  standardHeaders: true,
  legacyHeaders: false,
})

app.use(express.json({ limit: '512kb' }))
app.use(limiter)
app.get('/health', (_, res) => res.json({ status: 'ok' }))

app.post('/handshake', (req, res) => {
  if (!req.body?.secret) return res.status(400).json({ error: 'secret required' })
  if (req.body.secret !== handshakeSecret) {
    logger.warn('Handshake rejected: invalid secret', { ip: req.ip })
    return res.status(401).json({ error: 'invalid secret' })
  }
  const token = issueToken()
  logger.info('Handshake successful', { ip: req.ip })
  res.json({ token })
})

app.post('/handshake/revoke', (req, res) => {
  const token = req.body?.token
  if (!token) return res.status(400).json({ error: 'token required' })
  if (!revokeToken(token)) {
    logger.warn('Revocation requested for unknown token', { ip: req.ip })
    return res.status(404).json({ error: 'token not found' })
  }
  logger.info('Token revoked', { ip: req.ip })
  res.json({ status: 'revoked' })
})

app.get('/profiles', (_, res) => {
  res.json({ profiles: getProfileSummary() })
})

app.get('/profiles/:id', (req, res) => {
  const file = path.join(profilesDir, `${req.params.id}.json`)
  if (!fs.existsSync(file)) return res.status(404).json({ error: 'Profile not found' })
  res.type('json').send(fs.readFileSync(file, 'utf-8'))
})

const profileSchemaPath = path.join(__dirname, 'config', 'profile.schema.json')
const profileValidator = fs.existsSync(profileSchemaPath)
  ? ajv.compile(JSON.parse(fs.readFileSync(profileSchemaPath, 'utf-8')))
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
  res.json({
    status: 'saved',
    version: profile.version,
    checksum: profile.checksum,
    conflict,
    previousVersion,
  })
})

let server
if (tlsKey && tlsCert && fs.existsSync(tlsKey) && fs.existsSync(tlsCert)) {
  const credentials = {
    key: fs.readFileSync(tlsKey),
    cert: fs.readFileSync(tlsCert),
  }
  server = https.createServer(credentials, app)
  server.listen(port, () => {
    logger.info(`Server listening with TLS on https://localhost:${port}`)
  })
} else {
  server = http.createServer(app)
  server.listen(port, () => {
    logger.info(`Server listening on http://localhost:${port}`)
  })
}

let wss
function broadcastProfileUpdate(profile, meta = {}) {
  if (!wss) return
  const payload = JSON.stringify({
    type: 'profile:update',
    profile,
    broadcastAt: Date.now(),
    ...meta,
  })

  wss.clients.forEach((client) => {
    if (client.readyState === WebSocket.OPEN) {
      client.send(payload)
    }
  })
}

const clientSessions = new Map()

wss = new WebSocketServer({
  server,
  path: '/ws',
  perMessageDeflate: {
    serverNoContextTakeover: true,
    clientNoContextTakeover: true,
    threshold: 512,
  },
})

wss.on('connection', (ws, req) => {
  const auth = req.headers['authorization']
  const token = auth?.toString().replace('Bearer ', '')
  const authRequired = activeTokens.size > 0

  if (authRequired && (!token || !activeTokens.has(token))) {
    logger.warn('Connection refused: invalid token', { ip: req.socket.remoteAddress })
    ws.close(4001, 'invalid token')
    return
  }

  const clientId = crypto.randomUUID()
  logger.info('Client connected', { ip: req.socket.remoteAddress, clientId })

  clientSessions.set(ws, { clientId, activeProfileId: null, controlStates: {} })

  ws.send(
    JSON.stringify({
      type: 'welcome',
      clientId,
      profiles: getProfileSummary(),
      activeProfileId: null,
    })
  )

  const recentMessages = []

  ws.on('message', async (message) => {
    const receivedAt = Date.now()
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
      payload = JSON.parse(message.toString())
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

      const mapping = payload.controlId ? mappings[payload.controlId] : undefined
      if (session) {
        session.controlStates[payload.controlId] = payload.value
      }

      if (!mapping) {
        ws.send(JSON.stringify({ ...ack, status: 'ignored' }))
        return
      }

      try {
        switch (mapping.action) {
          case 'keyboard':
            await handleKeyboard(mapping.payload)
            break
          case 'obs':
            await handleObs(mapping.payload)
            break
          case 'script':
            await handleScript(mapping.payload)
            break
          case 'audio':
            await handleAudio(mapping.payload)
            break
          default:
            console.warn('Unknown action', mapping.action)
        }
        ws.send(JSON.stringify({ ...ack, status: 'ok', processedAt: Date.now() }))
      } catch (e) {
        console.error('Unable to execute action', e)
        logger.error('Action execution error', { error: e.message })
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

      const profileData = loadProfileFromDisk(payload.profileId)
      if (!profileData) {
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

      if (session) {
        session.activeProfileId = payload.profileId
        if (payload.resetState !== false) {
          session.controlStates = {}
        }
      }

      ws.send(
        JSON.stringify({
          type: 'profile:select:ack',
          status: 'ok',
          profileId: payload.profileId,
          receivedAt,
          profile: profileData.profile,
        })
      )
      return
    }

    if (kind === 'profile:update') {
      if (!validateProfileUpdatePayload(payload)) {
        logger.warn('Profile update rejected by schema', { errors: validateProfileUpdatePayload.errors })
        ws.send(JSON.stringify({ type: 'profile:update:ack', status: 'error', error: 'invalid payload', receivedAt }))
        return
      }

      if (!profileValidator) {
        ws.send(JSON.stringify({ type: 'profile:update:ack', status: 'error', error: 'profile validator unavailable' }))
        return
      }

      if (!profileValidator(payload.profile)) {
        ws.send(
          JSON.stringify({
            type: 'profile:update:ack',
            status: 'error',
            error: 'invalid profile',
            details: profileValidator.errors,
          })
        )
        return
      }

      const { profile: saved, conflict, previousVersion } = saveProfile(payload.profile, {
        source: 'websocket',
        actor: session?.clientId ?? 'ws',
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

    ws.send(JSON.stringify({ type: 'error', error: 'unknown message kind', kind }))
  })

  ws.on('close', () => {
    console.log('Client disconnected', clientSessions.get(ws)?.clientId ?? '')
    clientSessions.delete(ws)
  })
})
