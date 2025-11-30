import express from 'express'
import { WebSocketServer } from 'ws'
import fs from 'fs'
import path from 'path'
import { fileURLToPath } from 'url'
import { handleKeyboard } from './actions/keyboard.js'
import { handleObs } from './actions/obs.js'
import { handleScript } from './actions/scripts.js'
import { handleAudio } from './actions/audio.js'

const __filename = fileURLToPath(import.meta.url)
const __dirname = path.dirname(__filename)

const app = express()
const port = process.env.PORT || 4455
const token = process.env.DECK_TOKEN || 'change-me'

const mappingsPath = path.join(__dirname, 'config', 'mappings.json')
const mappings = fs.existsSync(mappingsPath)
  ? JSON.parse(fs.readFileSync(mappingsPath, 'utf-8'))
  : {}

const profilesDir = path.join(__dirname, 'profiles')
fs.mkdirSync(profilesDir, { recursive: true })

app.use(express.json())
app.get('/health', (_, res) => res.json({ status: 'ok' }))

app.get('/profiles', (_, res) => {
  const files = fs.readdirSync(profilesDir).filter((file) => file.endsWith('.json'))
  res.json({ profiles: files })
})

app.get('/profiles/:id', (req, res) => {
  const file = path.join(profilesDir, `${req.params.id}.json`)
  if (!fs.existsSync(file)) return res.status(404).json({ error: 'Profile not found' })
  res.type('json').send(fs.readFileSync(file, 'utf-8'))
})

app.post('/profiles/:id', (req, res) => {
  const payload = JSON.stringify(req.body, null, 2)
  fs.writeFileSync(path.join(profilesDir, `${req.params.id}.json`), payload)
  res.json({ status: 'saved' })
})

const server = app.listen(port, () => {
  console.log(`Server listening on http://localhost:${port}`)
})

const wss = new WebSocketServer({ server, path: '/ws' })

wss.on('connection', (ws, req) => {
  const auth = req.headers['authorization']
  if (auth && auth !== `Bearer ${token}`) {
    ws.close(4001, 'invalid token')
    return
  }

  ws.on('message', async (message) => {
    try {
      const payload = JSON.parse(message.toString())
      const mapping = mappings[payload.controlId]
      if (!mapping) return

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
    } catch (e) {
      console.error('Unable to parse message', e)
    }
  })

  ws.on('close', () => console.log('Client disconnected'))
})
