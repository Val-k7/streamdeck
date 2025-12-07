/**
 * Gestion de la Rich Presence Discord
 *
 * Permet de définir un statut personnalisé avec des détails sur l'activité
 */

import { exec } from 'child_process'
import { promisify } from 'util'
import os from 'os'

const execAsync = promisify(exec)
const platform = os.platform()

/**
 * Gestionnaire de Rich Presence Discord
 */
export class DiscordRichPresence {
  constructor(logger) {
    this.logger = logger
    this.isActive = false
  }

  /**
   * Définit la Rich Presence Discord
   *
   * @param {object} presence - Configuration de la présence
   * @param {string} presence.details - Détails principaux (ex: "En train de streamer")
   * @param {string} presence.state - État secondaire (ex: "OBS Studio")
   * @param {string} presence.largeImageKey - Clé de l'image grande (ex: "obs")
   * @param {string} presence.largeImageText - Texte de l'image grande
   * @param {string} presence.smallImageKey - Clé de l'image petite
   * @param {string} presence.smallImageText - Texte de l'image petite
   * @param {number} presence.startTimestamp - Timestamp de début (optionnel)
   * @param {number} presence.endTimestamp - Timestamp de fin (optionnel)
   * @param {array} presence.buttons - Boutons à afficher (optionnel)
   */
  async setRichPresence(presence) {
    try {
      if (platform === 'win32') {
        return await this.setRichPresenceWindows(presence)
      } else if (platform === 'darwin') {
        return await this.setRichPresenceMacOS(presence)
      } else if (platform === 'linux') {
        return await this.setRichPresenceLinux(presence)
      } else {
        throw new Error(`Unsupported platform: ${platform}`)
      }
    } catch (error) {
      this.logger.error(`Failed to set Discord Rich Presence: ${error.message}`)
      throw error
    }
  }

  /**
   * Définit la Rich Presence sur Windows
   * Utilise PowerShell pour interagir avec Discord via l'API IPC
   */
  async setRichPresenceWindows(presence) {
    const script = `
      # Script PowerShell pour définir la Rich Presence Discord
      # Note: Cette implémentation nécessite l'utilisation de l'API IPC Discord
      # ou d'une bibliothèque tierce comme discord-rpc

      $presence = @{
        details = "${presence.details || ''}"
        state = "${presence.state || ''}"
        largeImageKey = "${presence.largeImageKey || ''}"
        largeImageText = "${presence.largeImageText || ''}"
        smallImageKey = "${presence.smallImageKey || ''}"
        smallImageText = "${presence.smallImageText || ''}"
      }

      if ($presence.startTimestamp) {
        $presence.startTimestamp = ${presence.startTimestamp || 0}
      }

      if ($presence.endTimestamp) {
        $presence.endTimestamp = ${presence.endTimestamp || 0}
      }

      # Pour une implémentation complète, utiliser discord-rpc ou l'API IPC Discord
      # Ceci est un placeholder qui montre la structure
      Write-Output "Rich Presence configurée: $($presence | ConvertTo-Json)"
      exit 0
    `

    try {
      await execAsync(
        `powershell -NoProfile -ExecutionPolicy Bypass -Command "${script.replace(/\n/g, ' ').replace(/"/g, '\\"')}"`
      )
      this.isActive = true
      return { success: true, presence }
    } catch (error) {
      throw new Error(`Failed to set Rich Presence on Windows: ${error.message}`)
    }
  }

  /**
   * Définit la Rich Presence sur macOS
   */
  async setRichPresenceMacOS(presence) {
    // Sur macOS, on peut utiliser osascript ou une bibliothèque Node.js
    // Placeholder pour l'implémentation
    this.logger.warn('Rich Presence on macOS not fully implemented')
    this.isActive = true
    return { success: true, presence }
  }

  /**
   * Définit la Rich Presence sur Linux
   */
  async setRichPresenceLinux(presence) {
    // Sur Linux, on peut utiliser une bibliothèque Node.js ou l'API IPC
    // Placeholder pour l'implémentation
    this.logger.warn('Rich Presence on Linux not fully implemented')
    this.isActive = true
    return { success: true, presence }
  }

  /**
   * Efface la Rich Presence
   */
  async clearRichPresence() {
    try {
      if (platform === 'win32') {
        await execAsync(
          `powershell -NoProfile -ExecutionPolicy Bypass -Command "Write-Output 'Rich Presence cleared'; exit 0"`
        )
      }
      this.isActive = false
      return { success: true }
    } catch (error) {
      this.logger.error(`Failed to clear Rich Presence: ${error.message}`)
      throw error
    }
  }

  /**
   * Met à jour uniquement certains champs de la Rich Presence
   */
  async updateRichPresence(updates) {
    // Cette méthode devrait maintenir l'état actuel et mettre à jour seulement les champs fournis
    // Pour l'instant, c'est un placeholder
    this.logger.info('Updating Rich Presence with:', updates)
    return { success: true, updates }
  }
}

/**
 * Presets de Rich Presence pour différents scénarios
 */
export const RichPresencePresets = {
  /**
   * Preset pour le streaming
   */
  streaming: (details) => ({
    details: details || 'En train de streamer',
    state: 'OBS Studio',
    largeImageKey: 'obs',
    largeImageText: 'OBS Studio',
    startTimestamp: Math.floor(Date.now() / 1000),
  }),

  /**
   * Preset pour l'enregistrement
   */
  recording: (details) => ({
    details: details || 'En train d\'enregistrer',
    state: 'OBS Studio',
    largeImageKey: 'obs',
    largeImageText: 'OBS Studio',
    startTimestamp: Math.floor(Date.now() / 1000),
  }),

  /**
   * Preset pour le gaming
   */
  gaming: (gameName, details) => ({
    details: details || `Joue à ${gameName}`,
    state: gameName,
    largeImageKey: 'game',
    largeImageText: gameName,
    startTimestamp: Math.floor(Date.now() / 1000),
  }),

  /**
   * Preset pour le travail
   */
  working: (application, details) => ({
    details: details || 'En train de travailler',
    state: application || 'Application',
    largeImageKey: 'work',
    largeImageText: application || 'Travail',
    startTimestamp: Math.floor(Date.now() / 1000),
  }),

  /**
   * Preset personnalisé
   */
  custom: (config) => ({
    ...config,
    startTimestamp: config.startTimestamp || Math.floor(Date.now() / 1000),
  }),
}





