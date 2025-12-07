import { exec } from 'child_process'
import { promisify } from 'util'
import os from 'os'
import fs from 'fs'
import path from 'path'

const execAsync = promisify(exec)
const platform = os.platform()

/**
 * Contrôle des media keys Windows
 */
export async function handleMediaKeys(action, payload) {
  if (platform !== 'win32') {
    throw new Error('Media keys control is only supported on Windows')
  }

  const mediaKeyCodes = {
    play_pause: 0xB3,  // VK_MEDIA_PLAY_PAUSE
    stop: 0xB2,        // VK_MEDIA_STOP
    next: 0xB0,        // VK_MEDIA_NEXT_TRACK
    previous: 0xB1,    // VK_MEDIA_PREV_TRACK
    volume_up: 0xAF,   // VK_VOLUME_UP
    volume_down: 0xAE, // VK_VOLUME_DOWN
    volume_mute: 0xAD  // VK_VOLUME_MUTE
  }

  const vkCode = mediaKeyCodes[action.toLowerCase()]
  if (!vkCode) {
    throw new Error(`Unknown media key action: ${action}`)
  }

  // Utiliser [System.Windows.Forms.SendKeys] ou directement keybd_event via rundll32
  // Approche simple: utiliser un script PowerShell avec fichier temporaire
  const csharpCode = `
using System;
using System.Runtime.InteropServices;

public class MediaKeys {
  [DllImport("user32.dll")]
  public static extern void keybd_event(byte bVk, byte bScan, int dwFlags, int dwExtraInfo);

  public static void Main() {
    keybd_event(${vkCode}, 0, 0, 0);
    keybd_event(${vkCode}, 0, 0x0002, 0);
  }
}
`.trim()

  const tempFile = path.join(os.tmpdir(), `mediakey_${Date.now()}.cs`)
  
  try {
    fs.writeFileSync(tempFile, csharpCode)
    
    // Compiler et exécuter avec csc.exe ou utiliser Add-Type avec fichier
    const script = `
$code = Get-Content -Path '${tempFile.replace(/\\/g, '\\\\')}' -Raw
Add-Type -TypeDefinition $code -Language CSharp
[MediaKeys]::Main()
`
    await execAsync(`powershell -NoProfile -ExecutionPolicy Bypass -Command "${script.replace(/\n/g, '; ').replace(/"/g, '\\"')}"`)
    
    // Cleanup
    try { fs.unlinkSync(tempFile) } catch {}
    
    return { success: true, action }
  } catch (error) {
    try { fs.unlinkSync(tempFile) } catch {}
    throw new Error(`Failed to send media key: ${error.message}`)
  }
}





