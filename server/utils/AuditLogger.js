import winston from 'winston'
import DailyRotateFile from 'winston-daily-rotate-file'
import path from 'path'
import { fileURLToPath } from 'url'
import fs from 'fs'

const __filename = fileURLToPath(import.meta.url)
const __dirname = path.dirname(__filename)

/**
 * Logger d'audit pour enregistrer toutes les actions, tentatives d'accès et erreurs
 */

class AuditLogger {
  constructor(logDir = null) {
    const auditLogDir = logDir || path.join(process.env.DECK_DATA_DIR || __dirname, 'logs', 'audit')
    fs.mkdirSync(auditLogDir, { recursive: true })

    this.logger = winston.createLogger({
      level: 'info',
      format: winston.format.combine(
        winston.format.timestamp({ format: 'YYYY-MM-DD HH:mm:ss' }),
        winston.format.errors({ stack: true }),
        winston.format.json()
      ),
      transports: [
        new DailyRotateFile({
          filename: path.join(auditLogDir, 'audit-%DATE%.log'),
          datePattern: 'YYYY-MM-DD',
          maxSize: '20m',
          maxFiles: '30d',
          zippedArchive: true,
        }),
        // Garder aussi un fichier de log actuel pour accès rapide
        new winston.transports.File({
          filename: path.join(auditLogDir, 'audit-current.log'),
          maxsize: 10 * 1024 * 1024, // 10MB
          maxFiles: 1,
        }),
      ],
    })
  }

  /**
   * Enregistre une action
   */
  logAction(action, details = {}) {
    this.logger.info('action', {
      type: 'action',
      action,
      ...details,
      timestamp: new Date().toISOString(),
    })
  }

  /**
   * Enregistre une tentative d'accès
   */
  logAccessAttempt(ip, success, reason = null, details = {}) {
    this.logger.info('access_attempt', {
      type: 'access_attempt',
      ip,
      success,
      reason,
      ...details,
      timestamp: new Date().toISOString(),
    })
  }

  /**
   * Enregistre une erreur
   */
  logError(error, context = {}) {
    this.logger.error('error', {
      type: 'error',
      error: error.message,
      stack: error.stack,
      ...context,
      timestamp: new Date().toISOString(),
    })
  }

  /**
   * Enregistre une authentification
   */
  logAuth(action, clientId, ip, success, reason = null) {
    this.logger.info('auth', {
      type: 'auth',
      action, // 'login', 'logout', 'token_issue', 'token_revoke', 'token_rotate'
      clientId,
      ip,
      success,
      reason,
      timestamp: new Date().toISOString(),
    })
  }

  /**
   * Enregistre une modification de profil
   */
  logProfileChange(action, profileId, clientId, details = {}) {
    this.logger.info('profile_change', {
      type: 'profile_change',
      action, // 'create', 'update', 'delete', 'import', 'export'
      profileId,
      clientId,
      ...details,
      timestamp: new Date().toISOString(),
    })
  }

  /**
   * Enregistre une action de contrôle
   */
  logControlAction(controlId, action, clientId, details = {}) {
    this.logger.info('control_action', {
      type: 'control_action',
      controlId,
      action,
      clientId,
      ...details,
      timestamp: new Date().toISOString(),
    })
  }

  /**
   * Enregistre une action de plugin
   */
  logPluginAction(pluginId, action, clientId, success, error = null) {
    this.logger.info('plugin_action', {
      type: 'plugin_action',
      pluginId,
      action,
      clientId,
      success,
      error: error?.message,
      timestamp: new Date().toISOString(),
    })
  }

  /**
   * Enregistre un changement de configuration
   */
  logConfigChange(configType, clientId, oldValue, newValue) {
    this.logger.info('config_change', {
      type: 'config_change',
      configType,
      clientId,
      oldValue,
      newValue,
      timestamp: new Date().toISOString(),
    })
  }

  /**
   * Enregistre un événement système
   */
  logSystemEvent(event, details = {}) {
    this.logger.info('system_event', {
      type: 'system_event',
      event,
      ...details,
      timestamp: new Date().toISOString(),
    })
  }

  /**
   * Recherche dans les logs d'audit
   */
  async searchLogs(query = {}) {
    // Cette fonction pourrait être implémentée pour rechercher dans les logs
    // Pour l'instant, on retourne juste une indication
    return {
      message: 'Log search not yet implemented. Logs are stored in the audit log files.',
      query,
    }
  }
}

// Singleton pour le serveur
let globalAuditLogger = null

export function getAuditLogger(logDir = null) {
  if (!globalAuditLogger) {
    globalAuditLogger = new AuditLogger(logDir)
  }
  return globalAuditLogger
}

export function createAuditLogger(logDir = null) {
  return new AuditLogger(logDir)
}

export default AuditLogger





