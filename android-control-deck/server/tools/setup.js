#!/usr/bin/env node
import fs from 'fs'
import path from 'path'
import { fileURLToPath } from 'url'
import { createInterface } from 'readline/promises'
import { stdin as input, stdout as output } from 'process'
import crypto from 'crypto'

const __filename = fileURLToPath(import.meta.url)
const __dirname = path.dirname(__filename)

const configDir = path.join(__dirname, 'config')
const mappingsDir = path.join(configDir, 'mappings')
fs.mkdirSync(mappingsDir, { recursive: true })

const configPath = path.join(configDir, 'server.config.json')
const defaultConfigPath = path.join(configDir, 'server.config.sample.json')
const defaultMappingPath = path.join(mappingsDir, 'default.json')

function readExistingConfig() {
  try {
    if (fs.existsSync(configPath)) {
      return JSON.parse(fs.readFileSync(configPath, 'utf-8'))
    }
  } catch (error) {
    console.warn('Impossible de lire la configuration existante :', error.message)
  }
  try {
    if (fs.existsSync(defaultConfigPath)) {
      return JSON.parse(fs.readFileSync(defaultConfigPath, 'utf-8'))
    }
  } catch (error) {
    console.warn('Impossible de lire le modèle de configuration :', error.message)
  }
  return { port: 4455, defaultToken: 'change-me', handshakeSecret: 'change-me', mappingsFile: 'mappings/default.json' }
}

function saveDefaultMapping(targetPath) {
  if (fs.existsSync(targetPath)) return
  try {
    const template = fs.existsSync(defaultMappingPath)
      ? fs.readFileSync(defaultMappingPath, 'utf-8')
      : JSON.stringify({
          example: {
            action: 'keyboard',
            payload: 'CTRL+ALT+S',
          },
        }, null, 2)
    fs.mkdirSync(path.dirname(targetPath), { recursive: true })
    fs.writeFileSync(targetPath, template)
    console.log(`Mapping initial créé : ${targetPath}`)
  } catch (error) {
    console.warn('Impossible de créer le fichier de mapping par défaut :', error.message)
  }
}

async function askQuestion(rl, question, validate, defaultValue) {
  while (true) {
    const suffix = defaultValue !== undefined ? ` [${defaultValue}]` : ''
    const answer = (await rl.question(`${question}${suffix}: `)).trim()
    const value = answer === '' ? defaultValue : answer
    const validation = validate(value)
    if (validation.valid) return validation.value
    console.log(`\n${validation.message}\n`)
  }
}

function normalizePort(value) {
  const num = Number(value)
  if (Number.isInteger(num) && num >= 1 && num <= 65535) {
    return { valid: true, value: num }
  }
  return { valid: false, message: 'Le port doit être un entier entre 1 et 65535.' }
}

function normalizeToken(value) {
  if (typeof value !== 'string' || value.trim() === '') {
    return { valid: false, message: 'Le token ne peut pas être vide.' }
  }
  if (value.length < 12) {
    return { valid: false, message: 'Le token doit contenir au moins 12 caractères.' }
  }
  if (/\s/.test(value)) {
    return { valid: false, message: 'Le token ne doit pas contenir d\'espaces.' }
  }
  return { valid: true, value }
}

function normalizeMappingName(value) {
  if (typeof value !== 'string' || value.trim() === '') {
    return { valid: false, message: 'Le nom de mapping est requis.' }
  }
  const safeName = value.replace(/[^a-zA-Z0-9_-]/g, '') || 'mappings'
  return { valid: true, value: safeName }
}

async function main() {
  const existing = readExistingConfig()
  const rl = createInterface({ input, output })
  console.log('Assistant de configuration Android Control Deck')
  console.log('Appuyez sur Entrée pour conserver la valeur proposée.')

  const port = await askQuestion(rl, 'Port du serveur', normalizePort, existing.port ?? 4455)

  const suggestedToken = existing.defaultToken && existing.defaultToken !== 'change-me'
    ? existing.defaultToken
    : crypto.randomBytes(16).toString('hex')
  const defaultToken = await askQuestion(rl, 'Token d\'accès API', normalizeToken, suggestedToken)

  const suggestedHandshake = existing.handshakeSecret && existing.handshakeSecret !== 'change-me'
    ? existing.handshakeSecret
    : defaultToken
  const handshakeSecret = await askQuestion(rl, 'Secret de handshake (client mobile)', normalizeToken, suggestedHandshake)

  const currentMappingName = existing.mappingsFile?.split('/').pop()?.replace(/\.json$/, '') ?? 'default'
  const mappingName = await askQuestion(rl, 'Nom du fichier de mapping (sans extension)', normalizeMappingName, currentMappingName)
  const mappingFile = `mappings/${mappingName}.json`

  rl.close()

  const nextConfig = {
    port,
    defaultToken,
    handshakeSecret,
    mappingsFile: mappingFile,
  }

  fs.writeFileSync(configPath, JSON.stringify(nextConfig, null, 2))
  console.log(`Configuration sauvegardée dans ${configPath}`)

  const targetMappingPath = path.join(configDir, mappingFile)
  saveDefaultMapping(targetMappingPath)
}

main().catch((error) => {
  console.error('Configuration interrompue :', error.message)
  process.exit(1)
})
