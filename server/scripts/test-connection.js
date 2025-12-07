#!/usr/bin/env node

/**
 * Script de test de connexion pour Control Deck Server
 *
 * Usage: node scripts/test-connection.js [options]
 * Options:
 *   --url: URL du serveur (défaut: http://localhost:4455)
 *   --token: Token d'authentification (optionnel)
 */

import https from 'https'
import http from 'http'

const args = process.argv.slice(2)
const urlArg = args.find(arg => arg.startsWith('--url='))
const tokenArg = args.find(arg => arg.startsWith('--token='))

const serverUrl = urlArg ? urlArg.split('=')[1] : 'http://localhost:4455'
const token = tokenArg ? tokenArg.split('=')[1] : null

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

async function testConnection() {
  log('🔌 Testing Control Deck Server connection...', 'blue')
  log(`📍 Server: ${serverUrl}`, 'blue')
  if (token) {
    log(`🔑 Token: ${token.substring(0, 8)}...`, 'blue')
  }
  console.log('')

  const url = new URL(serverUrl)
  const client = url.protocol === 'https:' ? https : http

  // Test 1: Health endpoint
  log('1️⃣ Testing /health endpoint...', 'blue')
  try {
    const healthCheck = new Promise((resolve, reject) => {
      const headers = {}
      if (token) {
        headers['Authorization'] = `Bearer ${token}`
      }

      const req = client.get(`${serverUrl}/health`, { headers, timeout: 5000 }, (res) => {
        let data = ''
        res.on('data', (chunk) => { data += chunk })
        res.on('end', () => {
          try {
            resolve({ status: res.statusCode, data: JSON.parse(data) })
          } catch {
            resolve({ status: res.statusCode, data })
          }
        })
      })

      req.on('error', reject)
      req.on('timeout', () => {
        req.destroy()
        reject(new Error('Request timeout'))
      })
    })

    const result = await healthCheck
    if (result.status === 200) {
      log('  ✅ Health check passed', 'green')
      if (result.data.healthy !== undefined) {
        log(`     Status: ${result.data.healthy ? 'Healthy' : 'Unhealthy'}`, result.data.healthy ? 'green' : 'yellow')
      }
    } else {
      log(`  ⚠️  Health check returned status ${result.status}`, 'yellow')
    }
  } catch (error) {
    log(`  ❌ Health check failed: ${error.message}`, 'red')
    return false
  }

  // Test 2: Diagnostics endpoint
  log('\n2️⃣ Testing /diagnostics endpoint...', 'blue')
  try {
    const diagnosticsCheck = new Promise((resolve, reject) => {
      const headers = {}
      if (token) {
        headers['Authorization'] = `Bearer ${token}`
      }

      const req = client.get(`${serverUrl}/diagnostics`, { headers, timeout: 5000 }, (res) => {
        let data = ''
        res.on('data', (chunk) => { data += chunk })
        res.on('end', () => {
          try {
            resolve({ status: res.statusCode, data: JSON.parse(data) })
          } catch {
            resolve({ status: res.statusCode, data })
          }
        })
      })

      req.on('error', reject)
      req.on('timeout', () => {
        req.destroy()
        reject(new Error('Request timeout'))
      })
    })

    const result = await diagnosticsCheck
    if (result.status === 200) {
      log('  ✅ Diagnostics endpoint accessible', 'green')
      if (result.data.uptimeSeconds) {
        log(`     Uptime: ${result.data.uptimeSeconds}s`, 'blue')
      }
      if (result.data.activeWebsocketConnections !== undefined) {
        log(`     Active WebSocket connections: ${result.data.activeWebsocketConnections}`, 'blue')
      }
    } else {
      log(`  ⚠️  Diagnostics returned status ${result.status}`, 'yellow')
    }
  } catch (error) {
    log(`  ❌ Diagnostics check failed: ${error.message}`, 'red')
  }

  // Test 3: WebSocket connection (si possible)
  log('\n3️⃣ Testing WebSocket connection...', 'blue')
  try {
    // Note: WebSocket test nécessiterait le module 'ws' côté client
    log('  ⚠️  WebSocket test requires client implementation', 'yellow')
    log('  💡 Use the Android app to test WebSocket connection', 'yellow')
  } catch (error) {
    log(`  ❌ WebSocket test failed: ${error.message}`, 'red')
  }

  console.log('')
  log('✅ Connection test complete!', 'green')
  log('\n💡 Tips:', 'blue')
  log('  - Make sure the server is running: npm start', 'blue')
  log('  - Check firewall settings if connection fails', 'blue')
  log('  - Verify the server URL and port', 'blue')

  return true
}

testConnection().catch((error) => {
  log(`\n❌ Connection test failed: ${error.message}`, 'red')
  process.exit(1)
})





