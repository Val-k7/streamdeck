/**
 * Logger avancé avec niveaux, contexte et formatage
 *
 * Wrapper autour de Winston avec fonctionnalités supplémentaires
 */

import winston from 'winston'
import DailyRotateFile from 'winston-daily-rotate-file'
import path from 'path'
import { fileURLToPath } from 'url'

const __filename = fileURLToPath(import.meta.url)
const __dirname = path.dirname(__filename)

export class Logger {
  constructor(options = {}) {
    const {
      level = 'info',
      logDir = path.join(process.env.DECK_DATA_DIR || __dirname, 'logs'),
      console = true,
      file = true,
      maxFiles = '14d',
      maxSize = '20m',
    } = options

    const transports = []

    if (console) {
      transports.push(
        new winston.transports.Console({
          format: winston.format.combine(
            winston.format.colorize(),
            winston.format.timestamp({ format: 'YYYY-MM-DD HH:mm:ss' }),
            winston.format.printf(({ timestamp, level, message, ...meta }) => {
              const metaStr = Object.keys(meta).length
                ? ` ${JSON.stringify(meta)}`
                : ''
              return `${timestamp} [${level}]: ${message}${metaStr}`
            })
          ),
        })
      )
    }

    if (file) {
      // Log général
      transports.push(
        new DailyRotateFile({
          dirname: logDir,
          filename: 'app-%DATE%.log',
          datePattern: 'YYYY-MM-DD',
          maxSize,
          maxFiles,
          zippedArchive: true,
          format: winston.format.combine(
            winston.format.timestamp(),
            winston.format.json()
          ),
        })
      )

      // Log d'erreurs séparé
      transports.push(
        new DailyRotateFile({
          dirname: logDir,
          filename: 'error-%DATE%.log',
          datePattern: 'YYYY-MM-DD',
          level: 'error',
          maxSize,
          maxFiles,
          zippedArchive: true,
          format: winston.format.combine(
            winston.format.timestamp(),
            winston.format.json()
          ),
        })
      )
    }

    this.winston = winston.createLogger({
      level,
      format: winston.format.combine(
        winston.format.timestamp(),
        winston.format.errors({ stack: true }),
        winston.format.json()
      ),
      transports,
    })

    this.context = {}
  }

  /**
   * Ajoute un contexte permanent
   */
  setContext(key, value) {
    this.context[key] = value
    return this
  }

  /**
   * Supprime un contexte
   */
  removeContext(key) {
    delete this.context[key]
    return this
  }

  /**
   * Crée un child logger avec un contexte supplémentaire
   */
  child(additionalContext = {}) {
    const childLogger = new Logger({
      level: this.winston.level,
      logDir: null, // Pas de fichiers supplémentaires
      console: false,
      file: false,
    })
    childLogger.winston = this.winston
    childLogger.context = { ...this.context, ...additionalContext }
    return childLogger
  }

  /**
   * Log avec contexte
   */
  _log(level, message, meta = {}) {
    const fullMeta = { ...this.context, ...meta }
    this.winston[level](message, fullMeta)
  }

  error(message, meta) {
    this._log('error', message, meta)
  }

  warn(message, meta) {
    this._log('warn', message, meta)
  }

  info(message, meta) {
    this._log('info', message, meta)
  }

  debug(message, meta) {
    this._log('debug', message, meta)
  }

  verbose(message, meta) {
    this._log('verbose', message, meta)
  }

  /**
   * Log une requête HTTP
   */
  logRequest(req, res, responseTime) {
    const meta = {
      method: req.method,
      url: req.url,
      statusCode: res.statusCode,
      responseTime,
      ip: req.ip || req.socket.remoteAddress,
      userAgent: req.get('user-agent'),
    }

    if (res.statusCode >= 500) {
      this.error('HTTP Request', meta)
    } else if (res.statusCode >= 400) {
      this.warn('HTTP Request', meta)
    } else {
      this.info('HTTP Request', meta)
    }
  }

  /**
   * Log une action
   */
  logAction(action, payload, result, duration) {
    const meta = {
      action,
      payload: this.sanitizePayload(payload),
      success: result?.success !== false,
      duration,
    }

    if (result?.error) {
      this.error('Action executed', { ...meta, error: result.error })
    } else {
      this.info('Action executed', meta)
    }
  }

  /**
   * Sanitise un payload pour le logging (enlève les données sensibles)
   */
  sanitizePayload(payload) {
    if (!payload || typeof payload !== 'object') {
      return payload
    }

    const sensitiveKeys = ['password', 'token', 'secret', 'key', 'apiKey', 'auth']
    const sanitized = { ...payload }

    for (const key in sanitized) {
      if (sensitiveKeys.some((sk) => key.toLowerCase().includes(sk.toLowerCase()))) {
        sanitized[key] = '***REDACTED***'
      } else if (typeof sanitized[key] === 'object') {
        sanitized[key] = this.sanitizePayload(sanitized[key])
      }
    }

    return sanitized
  }

  /**
   * Log une erreur avec stack trace
   */
  logError(error, context = {}) {
    this.error('Error occurred', {
      ...context,
      error: {
        message: error.message,
        stack: error.stack,
        name: error.name,
      },
    })
  }
}

/**
 * Instance singleton
 */
let defaultLogger = null

export function getLogger(options = {}) {
  if (!defaultLogger) {
    defaultLogger = new Logger(options)
  }
  return defaultLogger
}

export function createLogger(options = {}) {
  return new Logger(options)
}





