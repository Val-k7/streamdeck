import { exec } from 'child_process'
import { promisify } from 'util'
import os from 'os'

const execAsync = promisify(exec)
const platform = os.platform()

/**
 * Plugin Spotify pour Control Deck
 *
 * Utilise les Media Keys du système pour contrôler Spotify
 * Optionnellement, peut utiliser l'API Spotify Web (nécessite authentification)
 */
export default function createSpotifyPlugin({ name, version, config, logger, pluginDir }) {
  return {
    async onLoad() {
      logger.info(`Spotify plugin v${version} initialized`)
      logger.info(`Using media keys: ${config.useMediaKeys}`)
      if (config.apiEnabled) {
        logger.info('Spotify API enabled (client ID configured)')
      }
    },

    async onUnload() {
      logger.info('Spotify plugin unloaded')
    },

    async handleAction(action, payload) {
      switch (action) {
        case 'play_pause':
          return await handlePlayPause(payload, config, logger)

        case 'next':
          return await handleNext(config, logger)

        case 'previous':
          return await handlePrevious(config, logger)

        case 'volume':
          return await handleVolume(payload, config, logger)

        case 'shuffle':
          return await handleShuffle(payload, config, logger)

        case 'repeat':
          return await handleRepeat(payload, config, logger)

        case 'get_info':
          return await handleGetInfo(config, logger)

        default:
          throw new Error(`Unknown Spotify action: ${action}`)
      }
    },

    onConfigUpdate(newConfig) {
      logger.info('Spotify plugin config updated')
      config = newConfig
    }
  }

  /**
   * Gère play/pause
   */
  async function handlePlayPause(payload, config, logger) {
    try {
      if (config.useMediaKeys) {
        await sendMediaKey('play_pause', platform)
        logger.info('Spotify play/pause via media keys')
      } else {
        // Utiliser l'API Spotify si configurée
        throw new Error('Spotify API not yet implemented. Enable useMediaKeys in config.')
      }

      return { success: true, action: 'play_pause' }
    } catch (error) {
      logger.error('Error handling Spotify play/pause:', error)
      throw new Error(`Failed to play/pause Spotify: ${error.message}`)
    }
  }

  /**
   * Gère next track
   */
  async function handleNext(config, logger) {
    try {
      if (config.useMediaKeys) {
        await sendMediaKey('next', platform)
        logger.info('Spotify next track via media keys')
      } else {
        throw new Error('Spotify API not yet implemented')
      }

      return { success: true, action: 'next' }
    } catch (error) {
      logger.error('Error handling Spotify next:', error)
      throw new Error(`Failed to skip to next track: ${error.message}`)
    }
  }

  /**
   * Gère previous track
   */
  async function handlePrevious(config, logger) {
    try {
      if (config.useMediaKeys) {
        await sendMediaKey('previous', platform)
        logger.info('Spotify previous track via media keys')
      } else {
        throw new Error('Spotify API not yet implemented')
      }

      return { success: true, action: 'previous' }
    } catch (error) {
      logger.error('Error handling Spotify previous:', error)
      throw new Error(`Failed to skip to previous track: ${error.message}`)
    }
  }

  /**
   * Gère le volume
   */
  async function handleVolume(payload, config, logger) {
    try {
      const volume = payload?.volume
      if (volume === undefined || volume < 0 || volume > 100) {
        throw new Error('Volume must be between 0 and 100')
      }

      // Le volume Spotify est généralement contrôlé via le volume système
      // ou via l'API Spotify
      if (config.apiEnabled) {
        // TODO: Utiliser l'API Spotify pour contrôler le volume
        logger.info(`Spotify volume set to ${volume}% (API not yet implemented)`)
      } else {
        logger.warn('Spotify volume control requires API. Using system volume instead.')
      }

      return { success: true, action: 'volume', volume }
    } catch (error) {
      logger.error('Error handling Spotify volume:', error)
      throw new Error(`Failed to set Spotify volume: ${error.message}`)
    }
  }

  /**
   * Gère shuffle
   */
  async function handleShuffle(payload, config, logger) {
    try {
      const enabled = payload?.enabled ?? true

      if (config.apiEnabled) {
        // TODO: Utiliser l'API Spotify
        logger.info(`Spotify shuffle ${enabled ? 'enabled' : 'disabled'} (API not yet implemented)`)
      } else {
        throw new Error('Spotify shuffle requires API. Enable apiEnabled in config.')
      }

      return { success: true, action: 'shuffle', enabled }
    } catch (error) {
      logger.error('Error handling Spotify shuffle:', error)
      throw new Error(`Failed to toggle shuffle: ${error.message}`)
    }
  }

  /**
   * Gère repeat
   */
  async function handleRepeat(payload, config, logger) {
    try {
      const mode = payload?.mode || 'off'

      if (config.apiEnabled) {
        // TODO: Utiliser l'API Spotify
        logger.info(`Spotify repeat mode set to ${mode} (API not yet implemented)`)
      } else {
        throw new Error('Spotify repeat requires API. Enable apiEnabled in config.')
      }

      return { success: true, action: 'repeat', mode }
    } catch (error) {
      logger.error('Error handling Spotify repeat:', error)
      throw new Error(`Failed to set repeat mode: ${error.message}`)
    }
  }

  /**
   * Obtient les informations de la piste actuelle
   */
  async function handleGetInfo(config, logger) {
    try {
      const { SpotifyTrackInfo } = await import('./track-info.js')
      const trackInfo = new SpotifyTrackInfo(logger)

      const track = await trackInfo.getCurrentTrack()

      // Formater les informations pour l'affichage
      const formatted = {
        display: trackInfo.formatTrackInfo(track),
        position: trackInfo.formatPosition(track),
      }

      return {
        success: true,
        action: 'get_info',
        track: {
          title: track.title,
          artist: track.artist,
          album: track.album,
          duration: track.duration,
          position: track.position,
          isPlaying: track.isPlaying,
          artworkUrl: track.artworkUrl,
        },
        formatted,
      }
    } catch (error) {
      logger.error('Error getting Spotify info:', error)
      throw new Error(`Failed to get track info: ${error.message}`)
    }
  }

  /**
   * Envoie une media key selon la plateforme
   */
  async function sendMediaKey(key, platform) {
    if (platform === 'win32') {
      // Windows: Utiliser SendKeys ou Virtual-Key Codes
      const keyMap = {
        play_pause: 0xB3, // VK_MEDIA_PLAY_PAUSE
        next: 0xB0,       // VK_MEDIA_NEXT_TRACK
        previous: 0xB1    // VK_MEDIA_PREV_TRACK
      }

      const vkCode = keyMap[key]
      if (!vkCode) {
        throw new Error(`Unknown media key: ${key}`)
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

      await execAsync(`powershell -Command "${script.replace(/\n/g, ' ').replace(/"/g, '\\"')}"`)
    } else if (platform === 'darwin') {
      // macOS: Utiliser osascript
      const scriptMap = {
        play_pause: 'tell application "System Events" to key code 49', // Space
        next: 'tell application "System Events" to key code 124',      // Right arrow
        previous: 'tell application "System Events" to key code 123'   // Left arrow
      }

      const script = scriptMap[key]
      if (!script) {
        throw new Error(`Unknown media key: ${key}`)
      }

      await execAsync(`osascript -e '${script}'`)
    } else if (platform === 'linux') {
      // Linux: Utiliser xdotool ou playerctl
      try {
        // Essayer playerctl d'abord (plus fiable pour Spotify)
        const playerctlMap = {
          play_pause: 'play-pause',
          next: 'next',
          previous: 'previous'
        }
        const playerctlCmd = playerctlMap[key]
        if (playerctlCmd) {
          await execAsync(`playerctl ${playerctlCmd} spotify`)
          return
        }
      } catch {
        // Fallback sur xdotool
      }

      // Fallback: utiliser xdotool avec les media keys
      const xdotoolMap = {
        play_pause: 'XF86AudioPlay',
        next: 'XF86AudioNext',
        previous: 'XF86AudioPrev'
      }

      const keyName = xdotoolMap[key]
      if (!keyName) {
        throw new Error(`Unknown media key: ${key}`)
      }

      await execAsync(`xdotool key ${keyName}`)
    } else {
      throw new Error(`Unsupported platform: ${platform}`)
    }
  }
}

