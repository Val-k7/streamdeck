import { exec } from 'child_process'
import { promisify } from 'util'
import os from 'os'

const execAsync = promisify(exec)
const platform = os.platform()

/**
 * Plugin Discord pour Control Deck
 *
 * Utilise l'API Discord RPC pour contrôler Discord
 * Note: Nécessite discord-rpc ou un wrapper similaire
 */
export default function createDiscordPlugin({ name, version, config, logger, pluginDir }) {
  let discordRpc = null
  let isConnected = false

  return {
    async onLoad() {
      logger.info(`Discord plugin v${version} initialized`)

      // TODO: Initialiser Discord RPC si configuré
      // Pour l'instant, on utilise des méthodes alternatives

      if (config.autoConnect) {
        // Tenter de se connecter à Discord
        await connectToDiscord()
      }
    },

    async onUnload() {
      logger.info('Discord plugin unloaded')
      if (isConnected && discordRpc) {
        await disconnectFromDiscord()
      }
    },

    async handleAction(action, payload) {
      switch (action) {
        case 'mute':
          return await handleMute(payload, logger)

        case 'deafen':
          return await handleDeafen(payload, logger)

        case 'rich_presence':
          return await handleRichPresence(payload, logger)

        case 'send_message':
          return await handleSendMessage(payload, logger)

        case 'add_reaction':
          return await handleAddReaction(payload, logger)

        case 'remove_reaction':
          return await handleRemoveReaction(payload, logger)

        case 'edit_message':
          return await handleEditMessage(payload, logger)

        case 'delete_message':
          return await handleDeleteMessage(payload, logger)

        default:
          throw new Error(`Unknown Discord action: ${action}`)
      }
    },

    onConfigUpdate(newConfig) {
      logger.info('Discord plugin config updated')
      config = newConfig
    }
  }

  /**
   * Se connecter à Discord RPC
   */
  async function connectToDiscord() {
    try {
      // TODO: Implémenter la connexion Discord RPC
      // Cela nécessite une bibliothèque comme discord-rpc ou discord.js
      isConnected = true
      logger.info('Connected to Discord RPC')
    } catch (error) {
      logger.warn('Failed to connect to Discord RPC:', error.message)
      isConnected = false
    }
  }

  /**
   * Se déconnecter de Discord RPC
   */
  async function disconnectFromDiscord() {
    try {
      // TODO: Implémenter la déconnexion
      isConnected = false
      logger.info('Disconnected from Discord RPC')
    } catch (error) {
      logger.error('Error disconnecting from Discord RPC:', error)
    }
  }

  /**
   * Gère l'action mute/unmute
   */
  async function handleMute(payload, logger) {
    try {
      if (platform === 'win32') {
        // Windows: Utiliser les raccourcis clavier Discord (Ctrl+Shift+M par défaut)
        const toggle = payload?.toggle ?? false
        const mute = payload?.mute ?? null

        if (toggle || mute === null) {
          // Toggle mute
          await execAsync('powershell -Command "Add-Type -AssemblyName System.Windows.Forms; [System.Windows.Forms.SendKeys]::SendWait(\'^+M\')"')
        } else if (mute) {
          // Mute
          // Note: Discord n'a pas de raccourci direct pour "mute seulement", on toggle
          await execAsync('powershell -Command "Add-Type -AssemblyName System.Windows.Forms; [System.Windows.Forms.SendKeys]::SendWait(\'^+M\')"')
        } else {
          // Unmute
          await execAsync('powershell -Command "Add-Type -AssemblyName System.Windows.Forms; [System.Windows.Forms.SendKeys]::SendWait(\'^+M\')"')
        }
      } else if (platform === 'darwin') {
        // macOS: Utiliser AppleScript ou raccourcis clavier
        await execAsync(`osascript -e 'tell application "System Events" to keystroke "m" using {control down, shift down}'`)
      } else if (platform === 'linux') {
        // Linux: Utiliser xdotool
        await execAsync('xdotool key ctrl+shift+m')
      }

      logger.info(`Discord mute action: toggle=${payload?.toggle}, mute=${payload?.mute}`)
      return { success: true, action: 'mute' }
    } catch (error) {
      logger.error('Error handling Discord mute action:', error)
      throw new Error(`Failed to mute Discord: ${error.message}`)
    }
  }

  /**
   * Gère l'action deafen/undeafen
   */
  async function handleDeafen(payload, logger) {
    try {
      if (platform === 'win32') {
        // Windows: Raccourci Ctrl+Shift+D
        await execAsync('powershell -Command "Add-Type -AssemblyName System.Windows.Forms; [System.Windows.Forms.SendKeys]::SendWait(\'^+D\')"')
      } else if (platform === 'darwin') {
        await execAsync(`osascript -e 'tell application "System Events" to keystroke "d" using {control down, shift down}'`)
      } else if (platform === 'linux') {
        await execAsync('xdotool key ctrl+shift+d')
      }

      logger.info(`Discord deafen action: toggle=${payload?.toggle}, deafen=${payload?.deafen}`)
      return { success: true, action: 'deafen' }
    } catch (error) {
      logger.error('Error handling Discord deafen action:', error)
      throw new Error(`Failed to deafen Discord: ${error.message}`)
    }
  }

  /**
   * Gère la Rich Presence Discord
   *
   * Note: Cela nécessite Discord RPC actif
   */
  async function handleRichPresence(payload, logger) {
    try {
      const { DiscordRichPresence, RichPresencePresets } = await import('./rich-presence.js')
      const richPresence = new DiscordRichPresence(logger)

      // Si un preset est spécifié, l'utiliser
      if (payload?.preset) {
        const presetName = payload.preset
        const presetConfig = payload.presetConfig || {}

        let presence
        switch (presetName) {
          case 'streaming':
            presence = RichPresencePresets.streaming(presetConfig.details)
            break
          case 'recording':
            presence = RichPresencePresets.recording(presetConfig.details)
            break
          case 'gaming':
            presence = RichPresencePresets.gaming(presetConfig.gameName, presetConfig.details)
            break
          case 'working':
            presence = RichPresencePresets.working(presetConfig.application, presetConfig.details)
            break
          case 'custom':
            presence = RichPresencePresets.custom(presetConfig)
            break
          default:
            throw new Error(`Unknown preset: ${presetName}`)
        }

        return await richPresence.setRichPresence(presence)
      }

      // Sinon, utiliser les paramètres directs
      const presence = {
        details: payload?.details || null,
        state: payload?.state || null,
        largeImageKey: payload?.largeImageKey || null,
        largeImageText: payload?.largeImageText || null,
        smallImageKey: payload?.smallImageKey || null,
        smallImageText: payload?.smallImageText || null,
        startTimestamp: payload?.startTimestamp || null,
        endTimestamp: payload?.endTimestamp || null,
        buttons: payload?.buttons || null,
      }

      return await richPresence.setRichPresence(presence)
    } catch (error) {
      logger.error('Error handling Discord Rich Presence:', error)
      throw new Error(`Failed to update Rich Presence: ${error.message}`)
    }
  }

  /**
   * Gère l'envoi de messages Discord
   */
  async function handleSendMessage(payload, logger) {
    try {
      const { DiscordMessageManager } = await import('./messages.js')
      const messageManager = new DiscordMessageManager(logger)

      return await messageManager.sendMessage({
        channelId: payload?.channelId,
        content: payload?.content,
        embed: payload?.embed,
        components: payload?.components,
        useClipboard: payload?.useClipboard || false,
      })
    } catch (error) {
      logger.error('Error handling Discord send message:', error)
      throw new Error(`Failed to send message: ${error.message}`)
    }
  }

  /**
   * Gère l'ajout de réactions
   */
  async function handleAddReaction(payload, logger) {
    try {
      const { DiscordMessageManager } = await import('./messages.js')
      const messageManager = new DiscordMessageManager(logger)

      return await messageManager.addReaction({
        messageId: payload?.messageId,
        channelId: payload?.channelId,
        emoji: payload?.emoji,
      })
    } catch (error) {
      logger.error('Error handling Discord add reaction:', error)
      throw new Error(`Failed to add reaction: ${error.message}`)
    }
  }

  /**
   * Gère la suppression de réactions
   */
  async function handleRemoveReaction(payload, logger) {
    try {
      const { DiscordMessageManager } = await import('./messages.js')
      const messageManager = new DiscordMessageManager(logger)

      return await messageManager.removeReaction({
        messageId: payload?.messageId,
        channelId: payload?.channelId,
        emoji: payload?.emoji,
      })
    } catch (error) {
      logger.error('Error handling Discord remove reaction:', error)
      throw new Error(`Failed to remove reaction: ${error.message}`)
    }
  }

  /**
   * Gère l'édition de messages
   */
  async function handleEditMessage(payload, logger) {
    try {
      const { DiscordMessageManager } = await import('./messages.js')
      const messageManager = new DiscordMessageManager(logger)

      return await messageManager.editMessage({
        messageId: payload?.messageId,
        channelId: payload?.channelId,
        content: payload?.content,
        embed: payload?.embed,
      })
    } catch (error) {
      logger.error('Error handling Discord edit message:', error)
      throw new Error(`Failed to edit message: ${error.message}`)
    }
  }

  /**
   * Gère la suppression de messages
   */
  async function handleDeleteMessage(payload, logger) {
    try {
      const { DiscordMessageManager } = await import('./messages.js')
      const messageManager = new DiscordMessageManager(logger)

      return await messageManager.deleteMessage({
        messageId: payload?.messageId,
        channelId: payload?.channelId,
      })
    } catch (error) {
      logger.error('Error handling Discord delete message:', error)
      throw new Error(`Failed to delete message: ${error.message}`)
    }
  }
}

