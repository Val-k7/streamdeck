#!/usr/bin/env node

/**
 * Script de validation de configuration pour Control Deck Server
 *
 * Usage: node scripts/validate-config.js
 */

import fs from 'fs'
import path from 'path'
import { fileURLToPath } from 'url'
import { getConfigValidator } from '../utils/ConfigValidator.js'

const __filename = fileURLToPath(import.meta.url)
const __dirname = path.dirname(__filename)
const rootDir = path.resolve(__dirname, '..')

const colors = {
  reset: '\x1b[0m',
  green: '\x1b[32m',
  red: '\x1b[31m',
  yellow: '\x1b[33m',
  blue: '\x1b[34m',
}

function log(message, color = 'reset') {
  console.log(`${colors[color]}${message}${colors.reset}`)
}

function validateConfig() {
  log('🔍 Validating Control Deck Server configuration...', 'blue')
  console.log('')

  const validator = getConfigValidator()
  let hasErrors = false

  // Valider server.config.json
  const configPath = path.join(rootDir, 'config', 'server.config.json')
  if (fs.existsSync(configPath)) {
    log('📋 Validating server.config.json...', 'blue')
    try {
      const config = JSON.parse(fs.readFileSync(configPath, 'utf-8'))
      const result = validator.validateServerConfig(config)

      if (result.valid) {
        log('  ✅ Configuration is valid', 'green')
      } else {
        log('  ❌ Configuration has errors:', 'red')
        result.errors.forEach((error) => {
          log(`    - ${error.instancePath || 'root'}: ${error.message}`, 'red')
        })
        hasErrors = true
      }
    } catch (error) {
      log(`  ❌ Failed to parse config: ${error.message}`, 'red')
      hasErrors = true
    }
  } else {
    log('  ⚠️  server.config.json not found (using defaults)', 'yellow')
  }

  console.log('')

  // Valider .env (si présent)
  const envPath = path.join(rootDir, '.env')
  if (fs.existsSync(envPath)) {
    log('🔐 Validating .env file...', 'blue')
    const envContent = fs.readFileSync(envPath, 'utf-8')
    const lines = envContent.split('\n').filter((line) => line.trim() && !line.startsWith('#'))

    let envErrors = 0
    lines.forEach((line) => {
      if (line.includes('=')) {
        const [key, value] = line.split('=').map((s) => s.trim())
        if (key && !value) {
          log(`  ⚠️  ${key} has no value`, 'yellow')
        }
        // Validation spécifique
        if (key === 'PORT') {
          const portResult = validator.validatePort(parseInt(value, 10))
          if (!portResult.valid) {
            log(`  ❌ PORT: ${portResult.error}`, 'red')
            envErrors++
          }
        }
        if (key === 'HOST') {
          // Validation basique de l'host
          if (value && value !== 'localhost' && value !== '0.0.0.0') {
            const ipResult = validator.validateIp(value)
            if (!ipResult.valid) {
              log(`  ⚠️  HOST: ${ipResult.error}`, 'yellow')
            }
          }
        }
      }
    })

    if (envErrors === 0) {
      log('  ✅ .env file is valid', 'green')
    } else {
      hasErrors = true
    }
  } else {
    log('  ⚠️  .env file not found', 'yellow')
  }

  console.log('')

  // Vérifier les répertoires nécessaires
  log('📁 Checking required directories...', 'blue')
  const requiredDirs = ['config', 'profiles', 'plugins', 'logs']
  let dirErrors = 0

  requiredDirs.forEach((dir) => {
    const dirPath = path.join(rootDir, dir)
    if (fs.existsSync(dirPath)) {
      log(`  ✅ ${dir}/ exists`, 'green')
    } else {
      log(`  ⚠️  ${dir}/ does not exist (will be created on startup)`, 'yellow')
    }
  })

  console.log('')

  // Résumé
  if (hasErrors) {
    log('❌ Configuration validation failed', 'red')
    process.exit(1)
  } else {
    log('✅ Configuration validation passed!', 'green')
    process.exit(0)
  }
}

validateConfig()





