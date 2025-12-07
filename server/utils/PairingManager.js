/**
 * Gestionnaire de pairing sécurisé pour Control Deck
 * Gère les codes de pairing temporaires et la validation mutuelle
 */

import crypto from 'crypto'
import winston from 'winston'

export class PairingManager {
  constructor(logger) {
    this.logger = logger || winston.createLogger({ silent: true })
    this.activePairings = new Map() // pairingCode -> { serverId, expiresAt, clientId }
    this.pairedServers = new Map() // serverId -> { pairedAt, fingerprint, lastSeen }
    this.pairingCodeTTL = 5 * 60 * 1000 // 5 minutes
  }

  /**
   * Générer un code de pairing temporaire
   * @param {string} serverId - ID du serveur
   * @param {string} clientId - ID du client (optionnel)
   * @returns {string} Code de pairing à 6 chiffres
   */
  generatePairingCode(serverId, clientId = null) {
    // Générer un code à 6 chiffres
    const code = Math.floor(100000 + Math.random() * 900000).toString()
    const expiresAt = Date.now() + this.pairingCodeTTL

    this.activePairings.set(code, {
      serverId,
      clientId,
      expiresAt,
      createdAt: Date.now(),
    })

    this.logger.info('Pairing code generated', {
      code,
      serverId,
      clientId,
      expiresAt: new Date(expiresAt).toISOString(),
    })

    // Nettoyer les codes expirés
    this.cleanupExpiredPairings()

    return code
  }

  /**
   * Valider un code de pairing
   * @param {string} code - Code de pairing
   * @param {string} serverId - ID du serveur
   * @returns {boolean} True si le code est valide
   */
  validatePairingCode(code, serverId) {
    const pairing = this.activePairings.get(code)

    if (!pairing) {
      this.logger.warn('Invalid pairing code', { code, serverId })
      return false
    }

    if (pairing.serverId !== serverId) {
      this.logger.warn('Pairing code server ID mismatch', { code, serverId, expected: pairing.serverId })
      return false
    }

    if (Date.now() > pairing.expiresAt) {
      this.logger.warn('Pairing code expired', { code, serverId, expiresAt: pairing.expiresAt })
      this.activePairings.delete(code)
      return false
    }

    return true
  }

  /**
   * Finaliser le pairing et enregistrer le serveur appairé
   * @param {string} code - Code de pairing
   * @param {string} serverId - ID du serveur
   * @param {string} fingerprint - Fingerprint du certificat serveur (optionnel)
   * @returns {boolean} True si le pairing a été finalisé avec succès
   */
  finalizePairing(code, serverId, fingerprint = null) {
    if (!this.validatePairingCode(code, serverId)) {
      return false
    }

    const pairing = this.activePairings.get(code)

    // Enregistrer le serveur appairé
    this.pairedServers.set(serverId, {
      pairedAt: Date.now(),
      fingerprint,
      lastSeen: Date.now(),
      clientId: pairing.clientId,
    })

    // Supprimer le code de pairing utilisé
    this.activePairings.delete(code)

    this.logger.info('Pairing finalized', {
      serverId,
      clientId: pairing.clientId,
      fingerprint,
    })

    return true
  }

  /**
   * Vérifier si un serveur est appairé
   * @param {string} serverId - ID du serveur
   * @returns {boolean} True si le serveur est appairé
   */
  isServerPaired(serverId) {
    return this.pairedServers.has(serverId)
  }

  /**
   * Obtenir les informations d'un serveur appairé
   * @param {string} serverId - ID du serveur
   * @returns {Object|null} Informations du serveur ou null
   */
  getPairedServer(serverId) {
    return this.pairedServers.get(serverId) || null
  }

  /**
   * Mettre à jour la dernière vue d'un serveur
   * @param {string} serverId - ID du serveur
   */
  updateLastSeen(serverId) {
    const server = this.pairedServers.get(serverId)
    if (server) {
      server.lastSeen = Date.now()
    }
  }

  /**
   * Supprimer un serveur appairé
   * @param {string} serverId - ID du serveur
   */
  removePairedServer(serverId) {
    this.pairedServers.delete(serverId)
    this.logger.info('Paired server removed', { serverId })
  }

  /**
   * Obtenir tous les serveurs appairés
   * @returns {Array} Liste des serveurs appairés
   */
  getPairedServers() {
    return Array.from(this.pairedServers.entries()).map(([serverId, info]) => ({
      serverId,
      ...info,
    }))
  }

  /**
   * Nettoyer les codes de pairing expirés
   */
  cleanupExpiredPairings() {
    const now = Date.now()
    for (const [code, pairing] of this.activePairings.entries()) {
      if (now > pairing.expiresAt) {
        this.activePairings.delete(code)
        this.logger.debug('Expired pairing code removed', { code })
      }
    }
  }

  /**
   * Générer un QR code pour le pairing (retourne les données pour génération)
   * @param {string} serverId - ID du serveur
   * @param {string} serverName - Nom du serveur
   * @param {number} port - Port du serveur
   * @param {string} protocol - Protocole (ws/wss)
   * @returns {Object} Données pour génération QR code
   */
  generateQRCodeData(serverId, serverName, port, protocol) {
    const code = this.generatePairingCode(serverId)
    return {
      type: 'control-deck-pairing',
      serverId,
      serverName,
      port,
      protocol,
      code,
      expiresAt: Date.now() + this.pairingCodeTTL,
    }
  }
}

// Instance singleton
let pairingManagerInstance = null

export function getPairingManager(logger) {
  if (!pairingManagerInstance) {
    pairingManagerInstance = new PairingManager(logger)
  }
  return pairingManagerInstance
}


