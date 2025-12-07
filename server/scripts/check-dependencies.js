#!/usr/bin/env node

/**
 * Script de vérification des dépendances pour Control Deck Server
 *
 * Usage: node scripts/check-dependencies.js
 */

import fs from 'fs'
import { execSync } from 'child_process'

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

function checkDependencies() {
  log('🔍 Checking Control Deck Server dependencies...', 'blue')
  console.log('')

  // Vérifier Node.js
  log('📦 Checking Node.js...', 'blue')
  try {
    const nodeVersion = execSync('node -v', { encoding: 'utf-8' }).trim()
    const majorVersion = parseInt(nodeVersion.replace('v', '').split('.')[0])

    if (majorVersion >= 18) {
      log(`  ✅ Node.js ${nodeVersion}`, 'green')
    } else {
      log(`  ❌ Node.js ${nodeVersion} (requires 18+)`, 'red')
      return false
    }
  } catch (error) {
    log('  ❌ Node.js not found', 'red')
    return false
  }

  // Vérifier npm
  log('📦 Checking npm...', 'blue')
  try {
    const npmVersion = execSync('npm -v', { encoding: 'utf-8' }).trim()
    log(`  ✅ npm ${npmVersion}`, 'green')
  } catch (error) {
    log('  ❌ npm not found', 'red')
    return false
  }

  // Vérifier package.json
  log('📋 Checking package.json...', 'blue')
  if (fs.existsSync('package.json')) {
    const packageJson = JSON.parse(fs.readFileSync('package.json', 'utf-8'))
    log(`  ✅ package.json found (version: ${packageJson.version})`, 'green')
  } else {
    log('  ❌ package.json not found', 'red')
    return false
  }

  // Vérifier node_modules
  log('📦 Checking installed dependencies...', 'blue')
  if (fs.existsSync('node_modules')) {
    const packageJson = JSON.parse(fs.readFileSync('package.json', 'utf-8'))
    const requiredDeps = Object.keys(packageJson.dependencies || {})
    let missingDeps = []

    requiredDeps.forEach((dep) => {
      if (!fs.existsSync(`node_modules/${dep}`)) {
        missingDeps.push(dep)
      }
    })

    if (missingDeps.length === 0) {
      log(`  ✅ All dependencies installed (${requiredDeps.length} packages)`, 'green')
    } else {
      log(`  ⚠️  Missing dependencies: ${missingDeps.join(', ')}`, 'yellow')
      log('  Run: npm install', 'yellow')
      return false
    }
  } else {
    log('  ⚠️  node_modules not found', 'yellow')
    log('  Run: npm install', 'yellow')
    return false
  }

  // Vérifier les répertoires nécessaires
  log('📁 Checking required directories...', 'blue')
  const requiredDirs = ['config', 'profiles', 'plugins', 'logs']
  let missingDirs = []

  requiredDirs.forEach((dir) => {
    if (!fs.existsSync(dir)) {
      missingDirs.push(dir)
    }
  })

  if (missingDirs.length === 0) {
    log('  ✅ All required directories exist', 'green')
  } else {
    log(`  ⚠️  Missing directories: ${missingDirs.join(', ')}`, 'yellow')
    log('  These will be created on startup', 'yellow')
  }

  console.log('')
  log('✅ Dependency check complete!', 'green')
  return true
}

const success = checkDependencies()
process.exit(success ? 0 : 1)





