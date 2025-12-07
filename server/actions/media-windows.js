import { exec } from 'child_process'
import { promisify } from 'util'
import os from 'os'

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

  const script = `
    Add-Type -TypeDefinition @"
      using System;
      using System.Runtime.InteropServices;

      public class MediaKeys {
        [DllImport("user32.dll")]
        public static extern void keybd_event(byte bVk, byte bScan, int dwFlags, int dwExtraInfo);

        public static void SendKey(byte vkCode) {
          keybd_event(vkCode, 0, 0, 0);
          keybd_event(vkCode, 0, 0x0002, 0);
        }
      }
"@
    [MediaKeys]::SendKey(${vkCode})
  `

  try {
    await execAsync(`powershell -NoProfile -ExecutionPolicy Bypass -Command "${script.replace(/\n/g, ' ').replace(/"/g, '\\"')}"`)
    return { success: true, action }
  } catch (error) {
    throw new Error(`Failed to send media key: ${error.message}`)
  }
}





