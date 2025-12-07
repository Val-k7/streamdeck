/**
 * Service de découverte réseau pour Control Deck Server
 * Implémente mDNS/Bonjour et broadcast UDP comme fallback
 */

import dgram from 'dgram'
import bonjour from 'bonjour'
import winston from 'winston'

export class DiscoveryService {
  constructor(serverId, serverName, port, protocol, logger) {
    this.serverId = serverId
    this.serverName = serverName
    this.port = port
    this.protocol = protocol
    this.logger = logger || winston.createLogger({ silent: true })

    this.bonjourInstance = null
    this.bonjourService = null
    this.udpSocket = null
    this.isRunning = false
  }

  /**
   * Démarrer les services de découverte
   */
  start() {
    if (this.isRunning) {
      this.logger.warn('Discovery service already running')
      return
    }

    this.startBonjour()
    this.startUDPBroadcast()
    this.isRunning = true
    this.logger.info('Discovery service started', {
      serverId: this.serverId,
      serverName: this.serverName,
      port: this.port,
    })
  }

  /**
   * Arrêter les services de découverte
   */
  stop() {
    if (!this.isRunning) {
      return
    }

    this.stopBonjour()
    this.stopUDPBroadcast()
    this.isRunning = false
    this.logger.info('Discovery service stopped')
  }

  /**
   * Démarrer le service Bonjour/mDNS
   */
  startBonjour() {
    try {
      this.bonjourInstance = bonjour()
      // Spécifier explicitement le type complet avec préfixe _ et suffixe ._tcp
      // pour correspondre exactement à ce qu'Android NSD recherche
      this.bonjourService = this.bonjourInstance.publish({
        name: this.serverName,
        type: '_control-deck._tcp',
        port: this.port,
        txt: {
          id: this.serverId,
          version: '1.0.0',
          port: this.port.toString(),
          protocol: this.protocol,
        },
      })

      this.logger.info('Bonjour/mDNS service published', {
        name: this.serverName,
        type: '_control-deck._tcp',
        port: this.port,
        serverId: this.serverId,
      })
    } catch (error) {
      this.logger.warn('Failed to start Bonjour/mDNS service', { error: error.message })
    }
  }

  /**
   * Arrêter le service Bonjour/mDNS
   */
  stopBonjour() {
    if (this.bonjourInstance) {
      try {
        // Capturer la référence avant le callback asynchrone
        const bonjourInstance = this.bonjourInstance
        this.bonjourInstance = null
        this.bonjourService = null

        bonjourInstance.unpublishAll(() => {
          // Vérifier que l'instance existe toujours avant de la détruire
          if (bonjourInstance) {
            try {
              bonjourInstance.destroy()
            } catch (error) {
              // Ignorer les erreurs si l'instance est déjà détruite
              this.logger.debug('Error destroying Bonjour instance', { error: error.message })
            }
          }
        })
      } catch (error) {
        this.logger.warn('Error stopping Bonjour service', { error: error.message })
        // S'assurer que les références sont nettoyées même en cas d'erreur
        this.bonjourInstance = null
        this.bonjourService = null
      }
    }
  }

  /**
   * Démarrer le broadcast UDP (fallback si mDNS indisponible)
   */
  startUDPBroadcast() {
    try {
      this.udpSocket = dgram.createSocket('udp4')

      // Message de découverte à envoyer
      const discoveryMessage = JSON.stringify({
        type: 'control-deck-discovery',
        serverId: this.serverId,
        serverName: this.serverName,
        port: this.port,
        protocol: this.protocol,
        version: '1.0.0',
      })

      // Envoyer un broadcast toutes les 5 secondes
      const broadcastInterval = setInterval(() => {
        if (!this.isRunning) {
          clearInterval(broadcastInterval)
          return
        }

        const message = Buffer.from(discoveryMessage)
        // Broadcast sur le port 5353 (mDNS) et un port personnalisé 4456
        const broadcastPorts = [5353, 4456]

        broadcastPorts.forEach((broadcastPort) => {
          try {
            this.udpSocket.setBroadcast(true)
            this.udpSocket.send(
              message,
              0,
              message.length,
              broadcastPort,
              '255.255.255.255',
              (err) => {
                if (err) {
                  this.logger.debug('UDP broadcast error', { port: broadcastPort, error: err.message })
                }
              }
            )
          } catch (error) {
            this.logger.debug('UDP broadcast exception', { port: broadcastPort, error: error.message })
          }
        })
      }, 5000)

      // Stocker l'interval pour le nettoyer plus tard
      this.broadcastInterval = broadcastInterval

      // Écouter les requêtes de découverte
      this.udpSocket.on('message', (msg, rinfo) => {
        try {
          const request = JSON.parse(msg.toString())
          if (request.type === 'control-deck-discovery-request') {
            this.logger.info('UDP discovery request received', {
              from: `${rinfo.address}:${rinfo.port}`,
              message: msg.toString(),
            })

            // Répondre avec les informations du serveur
            const response = JSON.stringify({
              type: 'control-deck-discovery-response',
              serverId: this.serverId,
              serverName: this.serverName,
              port: this.port,
              protocol: this.protocol,
              version: '1.0.0',
            })

            // Répondre sur le port 4457 (port d'écoute de l'application Android)
            // ou sur le port d'origine si c'est différent
            const responsePort = rinfo.port === 4457 ? 4457 : rinfo.port

            this.udpSocket.send(
              Buffer.from(response),
              0,
              response.length,
              responsePort,
              rinfo.address,
              (err) => {
                if (err) {
                  this.logger.debug('UDP response error', {
                    error: err.message,
                    to: `${rinfo.address}:${responsePort}`,
                  })
                } else {
                  this.logger.debug('UDP discovery response sent', {
                    to: `${rinfo.address}:${responsePort}`,
                    serverName: this.serverName,
                  })
                }
              }
            )
          }
        } catch (error) {
          // Ignorer les messages non valides
          this.logger.debug('Invalid UDP message received', { error: error.message })
        }
      })

      this.udpSocket.on('error', (err) => {
        this.logger.warn('UDP socket error', { error: err.message })
      })

      this.udpSocket.bind(4456, () => {
        this.logger.info('UDP broadcast service started on port 4456')
      })
    } catch (error) {
      this.logger.warn('Failed to start UDP broadcast service', { error: error.message })
    }
  }

  /**
   * Arrêter le broadcast UDP
   */
  stopUDPBroadcast() {
    if (this.broadcastInterval) {
      clearInterval(this.broadcastInterval)
      this.broadcastInterval = null
    }

    if (this.udpSocket) {
      try {
        this.udpSocket.close()
        this.udpSocket = null
      } catch (error) {
        this.logger.warn('Error closing UDP socket', { error: error.message })
      }
    }
  }
}

