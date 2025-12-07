import { exec } from 'child_process'
import { promisify } from 'util'
import os from 'os'
import fs from 'fs'
import path from 'path'
import { fileURLToPath } from 'url'

const execAsync = promisify(exec)
const platform = os.platform()
const __filename = fileURLToPath(import.meta.url)
const __dirname = path.dirname(__filename)

/**
 * Gestion du presse-papiers Windows
 */

/**
 * Lit le contenu du presse-papiers
 */
export async function readClipboard() {
  if (platform !== 'win32') {
    throw new Error('Clipboard is only supported on Windows')
  }

  try {
    const script = `
      Add-Type -AssemblyName System.Windows.Forms
      $clipboard = [System.Windows.Forms.Clipboard]::GetText()
      Write-Output $clipboard
    `
    const { stdout } = await execAsync(`powershell -NoProfile -ExecutionPolicy Bypass -Command "${script.replace(/\n/g, ' ').replace(/"/g, '\\"')}"`)
    return { success: true, text: stdout.trim() }
  } catch (error) {
    throw new Error(`Failed to read clipboard: ${error.message}`)
  }
}

/**
 * Écrit dans le presse-papiers
 */
export async function writeClipboard(text) {
  if (platform !== 'win32') {
    throw new Error('Clipboard is only supported on Windows')
  }

  try {
    // Échapper les caractères spéciaux
    const escapedText = text.replace(/"/g, '\\"').replace(/\$/g, '`$')

    const script = `
      Add-Type -AssemblyName System.Windows.Forms
      [System.Windows.Forms.Clipboard]::SetText("${escapedText}")
    `
    await execAsync(`powershell -NoProfile -ExecutionPolicy Bypass -Command "${script.replace(/\n/g, ' ')}"`)
    return { success: true, text }
  } catch (error) {
    throw new Error(`Failed to write clipboard: ${error.message}`)
  }
}

/**
 * Efface le presse-papiers
 */
export async function clearClipboard() {
  if (platform !== 'win32') {
    throw new Error('Clipboard is only supported on Windows')
  }

  try {
    const script = `
      Add-Type -AssemblyName System.Windows.Forms
      [System.Windows.Forms.Clipboard]::Clear()
    `
    await execAsync(`powershell -NoProfile -ExecutionPolicy Bypass -Command "${script.replace(/\n/g, ' ').replace(/"/g, '\\"')}"`)
    return { success: true }
  } catch (error) {
    throw new Error(`Failed to clear clipboard: ${error.message}`)
  }
}





