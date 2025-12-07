/**
 * Kill any process listening on the Control Deck server port before starting.
 * Default port: 4455 (can be overridden via PORT env var).
 */

import { execSync } from 'child_process'

const port = parseInt(process.env.PORT || '4455', 10)

function getPids() {
  try {
    if (process.platform === 'win32') {
      // netstat -ano | findstr :4455
      const output = execSync(`netstat -ano | findstr :${port}`, { encoding: 'utf-8' })
      const lines = output.split(/\r?\n/).filter(Boolean)
      const pids = lines
        .map((line) => line.trim().split(/\s+/).pop())
        .filter((pid) => pid && /^\d+$/.test(pid))
      return Array.from(new Set(pids))
    }

    // Try lsof first (macOS/Linux)
    const lsof = execSync(`lsof -i :${port} -t`, { encoding: 'utf-8' })
    const pids = lsof
      .split(/\r?\n/)
      .filter((pid) => pid && /^\d+$/.test(pid))
    if (pids.length) return Array.from(new Set(pids))
  } catch (err) {
    // Ignore and try fallback
  }

  try {
    // Fallback to fuser on Linux
    const fuser = execSync(`fuser ${port}/tcp`, { encoding: 'utf-8' })
    const pids = fuser
      .split(/\s+/)
      .filter((pid) => pid && /^\d+$/.test(pid))
    return Array.from(new Set(pids))
  } catch (err) {
    return []
  }
}

function killPid(pid) {
  if (Number(pid) === process.pid) return
  try {
    process.kill(Number(pid), 'SIGTERM')
  } catch (err) {
    // If SIGTERM fails, force kill with platform-specific command
    if (process.platform === 'win32') {
      execSync(`taskkill /PID ${pid} /F`)
    } else {
      execSync(`kill -9 ${pid}`)
    }
  }
}

const pids = getPids()
if (!pids.length) {
  console.log(`[kill-port] No process found on port ${port}`)
  process.exit(0)
}

console.log(`[kill-port] Killing ${pids.length} process(es) on port ${port}: ${pids.join(', ')}`)
pids.forEach(killPid)
