import { exec } from 'child_process'
import { promisify } from 'util'
import os from 'os'
import fs from 'fs'
import path from 'path'

const execAsync = promisify(exec)
const platform = os.platform()

/**
 * Contrôle système Windows
 */

/**
 * Verrouille la session Windows
 */
export async function lockSystem() {
  if (platform !== 'win32') {
    throw new Error('System lock is only supported on Windows')
  }

  try {
    await execAsync('rundll32.exe user32.dll,LockWorkStation')
    return { success: true, action: 'lock' }
  } catch (error) {
    throw new Error(`Failed to lock system: ${error.message}`)
  }
}

/**
 * Met en veille le système
 */
export async function sleepSystem() {
  if (platform !== 'win32') {
    throw new Error('System sleep is only supported on Windows')
  }

  try {
    await execAsync('rundll32.exe powrprof.dll,SetSuspendState 0,1,0')
    return { success: true, action: 'sleep' }
  } catch (error) {
    throw new Error(`Failed to sleep system: ${error.message}`)
  }
}

/**
 * Arrête le système
 */
export async function shutdownSystem(delaySeconds = 60) {
  if (platform !== 'win32') {
    throw new Error('System shutdown is only supported on Windows')
  }

  try {
    await execAsync(`shutdown /s /t ${delaySeconds}`)
    return { success: true, action: 'shutdown', delaySeconds }
  } catch (error) {
    throw new Error(`Failed to shutdown system: ${error.message}`)
  }
}

/**
 * Redémarre le système
 */
export async function restartSystem(delaySeconds = 60) {
  if (platform !== 'win32') {
    throw new Error('System restart is only supported on Windows')
  }

  try {
    await execAsync(`shutdown /r /t ${delaySeconds}`)
    return { success: true, action: 'restart', delaySeconds }
  } catch (error) {
    throw new Error(`Failed to restart system: ${error.message}`)
  }
}

/**
 * Annule un arrêt/redémarrage programmé
 */
export async function cancelShutdown() {
  if (platform !== 'win32') {
    throw new Error('Cancel shutdown is only supported on Windows')
  }

  try {
    await execAsync('shutdown /a')
    return { success: true, action: 'cancel_shutdown' }
  } catch (error) {
    throw new Error(`Failed to cancel shutdown: ${error.message}`)
  }
}

/**
 * Gère les fenêtres Windows
 */
export async function manageWindow(action, windowTitle) {
  if (platform !== 'win32') {
    throw new Error('Window management is only supported on Windows')
  }

  try {
    switch (action) {
      case 'minimize':
        await execAsync(`powershell -Command "(Get-Process | Where-Object {$_.MainWindowTitle -like '*${windowTitle}*'}) | ForEach-Object { (New-Object -ComObject Shell.Application).Windows() | Where-Object {$_.HWND -eq $_.MainWindowHandle} | ForEach-Object {$_.Document.Application.Quit()} }"`)
        break
      case 'maximize':
        await execAsync(`powershell -Command "$wshell = New-Object -ComObject wscript.shell; $wshell.SendKeys('%{ENTER}')"`)
        break
      case 'close':
        await execAsync(`taskkill /FI "WINDOWTITLE eq ${windowTitle}*" /T /F`)
        break
      default:
        throw new Error(`Unknown window action: ${action}`)
    }
    return { success: true, action, windowTitle }
  } catch (error) {
    throw new Error(`Failed to manage window: ${error.message}`)
  }
}

/**
 * Contrôle la luminosité de l'écran
 * @param {number} brightness - Valeur de luminosité entre 0 et 100
 */
export async function setBrightness(brightness) {
  if (platform !== 'win32') {
    throw new Error('Brightness control is only supported on Windows')
  }

  if (brightness < 0 || brightness > 100) {
    throw new Error('Brightness must be between 0 and 100')
  }

  try {
    // Utiliser PowerShell avec WMI pour ajuster la luminosité
    // Séparer les commandes avec des point-virgules pour éviter les problèmes de parsing
    const script = `$brightness = ${brightness}; (Get-WmiObject -Namespace root/WMI -Class WmiMonitorBrightnessMethods).WmiSetBrightness(1, $brightness)`
    await execAsync(`powershell -NoProfile -ExecutionPolicy Bypass -Command "${script.replace(/"/g, '\\"')}"`)
    return { success: true, action: 'brightness', brightness }
  } catch (error) {
    throw new Error(`Failed to set brightness: ${error.message}`)
  }
}

/**
 * Ouvre le menu des applications Windows (Action Center ou menu Démarrer)
 */
export async function openApps() {
  if (platform !== 'win32') {
    throw new Error('Open apps is only supported on Windows')
  }

  try {
    // Simuler WIN+A pour ouvrir le Action Center (Windows 10/11)
    // Utiliser un fichier temporaire pour éviter les problèmes de here-string
    const csharpCode = `
using System;
using System.Runtime.InteropServices;

public class Keyboard {
  [DllImport("user32.dll")]
  public static extern void keybd_event(byte bVk, byte bScan, int dwFlags, int dwExtraInfo);

  public static void Main() {
    const byte VK_LWIN = 0x5B;
    const byte VK_A = 0x41;
    const int KEYEVENTF_KEYUP = 0x0002;

    keybd_event(VK_LWIN, 0, 0, 0);
    keybd_event(VK_A, 0, 0, 0);
    keybd_event(VK_A, 0, KEYEVENTF_KEYUP, 0);
    keybd_event(VK_LWIN, 0, KEYEVENTF_KEYUP, 0);
  }
}
`.trim()

    const tempFile = path.join(os.tmpdir(), `keyboard_${Date.now()}.cs`)
    fs.writeFileSync(tempFile, csharpCode)
    
    const script = `$code = Get-Content -Path '${tempFile.replace(/\\/g, '\\\\')}' -Raw; Add-Type -TypeDefinition $code -Language CSharp; [Keyboard]::Main()`
    await execAsync(`powershell -NoProfile -ExecutionPolicy Bypass -Command "${script.replace(/"/g, '\\"')}"`)
    
    try { fs.unlinkSync(tempFile) } catch {}
    
    return { success: true, action: 'open_apps' }
  } catch (error) {
    throw new Error(`Failed to open apps menu: ${error.message}`)
  }
}



