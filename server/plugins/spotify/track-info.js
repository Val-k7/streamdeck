/**
 * Gestion des informations de piste Spotify
 *
 * Permet de récupérer les informations sur la piste actuellement jouée
 */

import { exec } from 'child_process'
import { promisify } from 'util'
import os from 'os'

const execAsync = promisify(exec)
const platform = os.platform()

/**
 * Gestionnaire d'informations de piste Spotify
 */
export class SpotifyTrackInfo {
  constructor(logger) {
    this.logger = logger
    this.cache = new Map()
    this.cacheTimeout = 5000 // 5 secondes
  }

  /**
   * Récupère les informations de la piste actuellement jouée
   *
   * @returns {Promise<object>} Informations de la piste
   * @property {string} title - Titre de la piste
   * @property {string} artist - Artiste
   * @property {string} album - Album
   * @property {number} duration - Durée en secondes
   * @property {number} position - Position actuelle en secondes
   * @property {boolean} isPlaying - Si la piste est en cours de lecture
   * @property {string} artworkUrl - URL de l'image de l'album
   */
  async getCurrentTrack() {
    // Vérifier le cache
    const cached = this.cache.get('currentTrack')
    if (cached && Date.now() - cached.timestamp < this.cacheTimeout) {
      return cached.data
    }

    try {
      let trackInfo

      if (platform === 'win32') {
        trackInfo = await this.getCurrentTrackWindows()
      } else if (platform === 'darwin') {
        trackInfo = await this.getCurrentTrackMacOS()
      } else if (platform === 'linux') {
        trackInfo = await this.getCurrentTrackLinux()
      } else {
        throw new Error(`Unsupported platform: ${platform}`)
      }

      // Mettre en cache
      this.cache.set('currentTrack', {
        data: trackInfo,
        timestamp: Date.now(),
      })

      return trackInfo
    } catch (error) {
      this.logger.error(`Failed to get Spotify track info: ${error.message}`)
      throw error
    }
  }

  /**
   * Récupère les informations sur Windows
   * Utilise l'API Spotify Web ou PowerShell pour interagir avec Spotify
   */
  async getCurrentTrackWindows() {
    const script = `
      # Script PowerShell pour récupérer les infos Spotify
      # Note: Nécessite Spotify Web API ou une bibliothèque tierce

      try {
        # Essayer d'utiliser l'API Spotify Web
        # Ou utiliser une bibliothèque comme spotify-web-api-node

        # Placeholder: structure de données
        $trackInfo = @{
          title = "Unknown"
          artist = "Unknown"
          album = "Unknown"
          duration = 0
          position = 0
          isPlaying = $false
          artworkUrl = ""
        }

        # Pour une implémentation complète, utiliser:
        # - Spotify Web API avec OAuth
        # - Bibliothèque Node.js spotify-web-api-node
        # - Ou interagir avec l'application Spotify via COM (Windows)

        $trackInfo | ConvertTo-Json -Compress
        exit 0
      } catch {
        Write-Error $_.Exception.Message
        exit 1
      }
    `

    try {
      const { stdout } = await execAsync(
        `powershell -NoProfile -ExecutionPolicy Bypass -Command "${script.replace(/\n/g, ' ').replace(/"/g, '\\"')}"`
      )
      return JSON.parse(stdout.trim())
    } catch (error) {
      // Retourner des valeurs par défaut en cas d'erreur
      return {
        title: 'Unknown',
        artist: 'Unknown',
        album: 'Unknown',
        duration: 0,
        position: 0,
        isPlaying: false,
        artworkUrl: '',
      }
    }
  }

  /**
   * Récupère les informations sur macOS
   * Utilise AppleScript pour interagir avec Spotify
   */
  async getCurrentTrackMacOS() {
    const script = `
      tell application "Spotify"
        if it is running then
          set trackName to name of current track
          set artistName to artist of current track
          set albumName to album of current track
          set trackDuration to duration of current track
          set trackPosition to player position
          set isPlaying to player state is playing

          return "{\\"title\\":\\"" & trackName & "\\",\\"artist\\":\\"" & artistName & "\\",\\"album\\":\\"" & albumName & "\\",\\"duration\\":" & (trackDuration / 1000) & ",\\"position\\":" & trackPosition & ",\\"isPlaying\\":" & isPlaying & "}"
        else
          return "{\\"title\\":\\"Unknown\\",\\"artist\\":\\"Unknown\\",\\"album\\":\\"Unknown\\",\\"duration\\":0,\\"position\\":0,\\"isPlaying\\":false}"
        end if
      end tell
    `

    try {
      const { stdout } = await execAsync(`osascript -e '${script}'`)
      return JSON.parse(stdout.trim())
    } catch (error) {
      return {
        title: 'Unknown',
        artist: 'Unknown',
        album: 'Unknown',
        duration: 0,
        position: 0,
        isPlaying: false,
        artworkUrl: '',
      }
    }
  }

  /**
   * Récupère les informations sur Linux
   * Utilise playerctl ou dbus pour interagir avec Spotify
   */
  async getCurrentTrackLinux() {
    try {
      // Essayer d'utiliser playerctl
      const { stdout: title } = await execAsync("playerctl -p spotify metadata title 2>/dev/null || echo 'Unknown'")
      const { stdout: artist } = await execAsync("playerctl -p spotify metadata artist 2>/dev/null || echo 'Unknown'")
      const { stdout: album } = await execAsync("playerctl -p spotify metadata album 2>/dev/null || echo 'Unknown'")
      const { stdout: duration } = await execAsync("playerctl -p spotify metadata mpris:length 2>/dev/null || echo '0'")
      const { stdout: position } = await execAsync("playerctl -p spotify position 2>/dev/null || echo '0'")
      const { stdout: status } = await execAsync("playerctl -p spotify status 2>/dev/null || echo 'Stopped'")

      return {
        title: title.trim(),
        artist: artist.trim(),
        album: album.trim(),
        duration: Math.floor(parseInt(duration.trim()) / 1000000), // Convertir de microsecondes en secondes
        position: parseFloat(position.trim()),
        isPlaying: status.trim() === 'Playing',
        artworkUrl: '', // playerctl ne fournit pas l'URL de l'image
      }
    } catch (error) {
      return {
        title: 'Unknown',
        artist: 'Unknown',
        album: 'Unknown',
        duration: 0,
        position: 0,
        isPlaying: false,
        artworkUrl: '',
      }
    }
  }

  /**
   * Vide le cache
   */
  clearCache() {
    this.cache.clear()
  }

  /**
   * Formate les informations de piste pour l'affichage
   */
  formatTrackInfo(trackInfo) {
    if (!trackInfo || trackInfo.title === 'Unknown') {
      return 'Aucune piste en cours'
    }

    const artist = trackInfo.artist || 'Artiste inconnu'
    const title = trackInfo.title || 'Titre inconnu'
    const album = trackInfo.album ? ` - ${trackInfo.album}` : ''

    return `${artist} - ${title}${album}`
  }

  /**
   * Formate la durée en format lisible (MM:SS)
   */
  formatDuration(seconds) {
    const mins = Math.floor(seconds / 60)
    const secs = Math.floor(seconds % 60)
    return `${mins}:${secs.toString().padStart(2, '0')}`
  }

  /**
   * Formate la position et la durée (position/duration)
   */
  formatPosition(trackInfo) {
    if (!trackInfo || trackInfo.duration === 0) {
      return ''
    }

    const position = this.formatDuration(trackInfo.position)
    const duration = this.formatDuration(trackInfo.duration)
    return `${position} / ${duration}`
  }
}





