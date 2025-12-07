import { exec } from 'child_process'
import { promisify } from 'util'
import os from 'os'
import fs from 'fs'
import { getLogger } from '../utils/Logger.js'

const logger = getLogger()

const execAsync = promisify(exec)

const platform = os.platform()

function parseKeyCombo(combo) {
  const parts = combo.split('+').map((s) => s.trim().toUpperCase())
  const modifiers = parts.slice(0, -1)
  const key = parts[parts.length - 1]
  return { modifiers, key }
}

async function sendKeyWindows(key, modifiers = []) {
  const modMap = {
    CTRL: '^',
    SHIFT: '+',
    ALT: '%',
    WIN: '#',
  }
  const modStr = modifiers.map((m) => modMap[m] || '').join('')
  const keyMap = {
    ENTER: 'ENTER',
    SPACE: 'SPACE',
    TAB: 'TAB',
    ESC: 'ESC',
    UP: 'UP',
    DOWN: 'DOWN',
    LEFT: 'LEFT',
    RIGHT: 'RIGHT',
    F1: 'F1',
    F2: 'F2',
    F3: 'F3',
    F4: 'F4',
    F5: 'F5',
    F6: 'F6',
    F7: 'F7',
    F8: 'F8',
    F9: 'F9',
    F10: 'F10',
    F11: 'F11',
    F12: 'F12',
  }
  const keyStr = keyMap[key] || key
  // Échapper les guillemets doubles dans la chaîne pour VBScript
  // Dans VBScript, les guillemets doubles sont échappés en les doublant
  const escapedKeyStr = keyStr.replace(/"/g, '""')
  const escapedModStr = modStr.replace(/"/g, '""')
  const vbsScript = `Set WshShell = CreateObject("WScript.Shell")\nWshShell.SendKeys "${escapedModStr}{${escapedKeyStr}}"`
  const tempFile = os.tmpdir() + `\\key_${Date.now()}.vbs`
  fs.writeFileSync(tempFile, vbsScript, 'utf-8')
  try {
    await execAsync(`cscript //nologo "${tempFile}"`)
  } finally {
    try {
      fs.unlinkSync(tempFile)
    } catch (e) {
      // Ignore cleanup errors
    }
  }
}

async function sendKeyMacOS(key, modifiers = []) {
  const modMap = {
    CTRL: '^',
    SHIFT: '+',
    ALT: '~',
    CMD: '@',
  }
  const modStr = modifiers.map((m) => modMap[m] || '').join('')
  const keyMap = {
    ENTER: '\\r',
    SPACE: ' ',
    TAB: '\\t',
    ESC: '\\e',
  }
  const keyStr = keyMap[key] || key.toLowerCase()
  await execAsync(`osascript -e 'tell application "System Events" to keystroke "${keyStr}" using {${modStr}}}'`)
}

async function sendKeyLinux(key, modifiers = []) {
  const modFlags = {
    CTRL: 'ctrl',
    SHIFT: 'shift',
    ALT: 'alt',
    WIN: 'super',
  }
  const flags = modifiers.map((m) => modFlags[m] || '').filter(Boolean)
  const keyMap = {
    ENTER: 'Return',
    SPACE: 'space',
    TAB: 'Tab',
    ESC: 'Escape',
  }
  const keyStr = keyMap[key] || key
  const xdotoolCmd = `xdotool key ${flags.length > 0 ? flags.map((f) => `${f}+`).join('') : ''}${keyStr}`
  await execAsync(xdotoolCmd)
}

export async function handleKeyboard(payload) {
  if (typeof payload !== 'string') {
    throw new Error('Keyboard payload must be a string (e.g., "CTRL+SHIFT+S")')
  }

  const { modifiers, key } = parseKeyCombo(payload)

  try {
    if (platform === 'win32') {
      await sendKeyWindows(key, modifiers)
    } else if (platform === 'darwin') {
      await sendKeyMacOS(key, modifiers)
    } else if (platform === 'linux') {
      await sendKeyLinux(key, modifiers)
    } else {
      throw new Error(`Unsupported platform: ${platform}`)
    }
  } catch (error) {
    logger.error('[keyboard] error', { error: error.message, stack: error.stack })
    throw error
  }
}
