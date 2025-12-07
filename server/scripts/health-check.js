#!/usr/bin/env node

/**
 * Script de vérification de santé pour Control Deck Server
 *
 * Usage: node scripts/health-check.js [options]
 * Options:
 *   --url: URL du serveur (défaut: http://localhost:4455)
 *   --timeout: Timeout en ms (défaut: 5000)
 */

import https from 'https'
import http from 'http'

const args = process.argv.slice(2)
const urlArg = args.find(arg => arg.startsWith('--url='))
const timeoutArg = args.find(arg => arg.startsWith('--timeout='))

const serverUrl = urlArg ? urlArg.split('=')[1] : 'http://localhost:4455'
const timeout = timeoutArg ? parseInt(timeoutArg.split('=')[1]) : 5000

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

async function checkHealth() {
  log('🔍 Checking Control Deck Server health...', 'blue')
  log(`📍 Server: ${serverUrl}`, 'blue')
  log(`⏱️  Timeout: ${timeout}ms\n`, 'blue')

  try {
    const url = new URL(serverUrl)
    const client = url.protocol === 'https:' ? https : http

    const healthCheck = new Promise((resolve, reject) => {
      const req = client.get(`${serverUrl}/health`, { timeout }, (res) => {
        let data = ''

        res.on('data', (chunk) => {
          data += chunk
        })

        res.on('end', () => {
          try {
            const json = JSON.parse(data)
            resolve({ status: res.statusCode, data: json })
          } catch (e) {
            resolve({ status: res.statusCode, data: data })
          }
        })
      })

      req.on('error', (error) => {
        reject(error)
      })

      req.on('timeout', () => {
        req.destroy()
        reject(new Error('Request timeout'))
      })
    })

    const result = await healthCheck

    if (result.status === 200) {
      log('✅ Server is healthy!', 'green')
      if (result.data.healthy !== undefined) {
        log(`   Health status: ${result.data.healthy ? 'Healthy' : 'Unhealthy'}`, result.data.healthy ? 'green' : 'yellow')
      }
      if (result.data.checks) {
        log('\n📊 Health checks:', 'blue')
        for (const [check, status] of Object.entries(result.data.checks)) {
          const icon = status.healthy ? '✅' : '❌'
          const color = status.healthy ? 'green' : 'red'
          log(`   ${icon} ${check}: ${status.message || (status.healthy ? 'OK' : 'Failed')}`, color)
          if (status.duration) {
            log(`      Duration: ${status.duration.toFixed(2)}ms`, 'blue')
          }
        }
      }
      process.exit(0)
    } else {
      log(`❌ Server returned status ${result.status}`, 'red')
      process.exit(1)
    }
  } catch (error) {
    log(`❌ Health check failed: ${error.message}`, 'red')
    log('\n💡 Make sure the server is running:', 'yellow')
    log('   npm start', 'yellow')
    log('   or', 'yellow')
    log('   ./scripts/start.sh', 'yellow')
    process.exit(1)
  }
}

checkHealth()





