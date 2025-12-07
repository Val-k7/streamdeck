/**
 * Vérificateur de santé du serveur
 *
 * Vérifie l'état de santé du serveur et de ses composants
 */

import os from 'os'
import { performance } from 'perf_hooks'

export class HealthChecker {
  constructor(logger) {
    this.logger = logger
    this.checks = new Map()
    this.lastCheck = null
    this.checkInterval = 30000 // 30 secondes
    this.intervalId = null
  }

  /**
   * Enregistre une vérification de santé
   */
  registerCheck(name, checkFn, critical = false) {
    this.checks.set(name, {
      fn: checkFn,
      critical,
      lastResult: null,
      lastCheck: null,
    })
  }

  /**
   * Exécute toutes les vérifications
   */
  async runChecks() {
    const results = {}
    let overallHealthy = true

    for (const [name, check] of this.checks.entries()) {
      try {
        const startTime = performance.now()
        const result = await Promise.race([
          check.fn(),
          new Promise((_, reject) =>
            setTimeout(() => reject(new Error('Check timeout')), 5000)
          ),
        ])
        const duration = performance.now() - startTime

        check.lastResult = {
          healthy: result.healthy !== false,
          message: result.message,
          data: result.data,
          duration,
        }
        check.lastCheck = Date.now()

        results[name] = check.lastResult

        if (!check.lastResult.healthy && check.critical) {
          overallHealthy = false
        }
      } catch (error) {
        check.lastResult = {
          healthy: false,
          message: error.message,
          duration: 0,
        }
        check.lastCheck = Date.now()

        results[name] = check.lastResult

        if (check.critical) {
          overallHealthy = false
        }

        this.logger.warn(`Health check failed: ${name}`, { error: error.message })
      }
    }

    this.lastCheck = Date.now()

    return {
      healthy: overallHealthy,
      timestamp: this.lastCheck,
      checks: results,
    }
  }

  /**
   * Obtient le rapport de santé
   */
  async getHealthReport() {
    if (!this.lastCheck || Date.now() - this.lastCheck > this.checkInterval) {
      return await this.runChecks()
    }

    // Retourner les résultats en cache
    const results = {}
    for (const [name, check] of this.checks.entries()) {
      results[name] = check.lastResult
    }

    return {
      healthy: Array.from(this.checks.values()).every(
        (check) => !check.critical || (check.lastResult?.healthy !== false)
      ),
      timestamp: this.lastCheck,
      checks: results,
    }
  }

  /**
   * Démarre les vérifications périodiques
   */
  startPeriodicChecks() {
    if (this.intervalId) {
      return
    }

    this.intervalId = setInterval(async () => {
      await this.runChecks()
    }, this.checkInterval)

    // Exécuter immédiatement
    this.runChecks()
  }

  /**
   * Arrête les vérifications périodiques
   */
  stopPeriodicChecks() {
    if (this.intervalId) {
      clearInterval(this.intervalId)
      this.intervalId = null
    }
  }

  /**
   * Vérifications système par défaut
   */
  registerDefaultChecks() {
    // Vérification de la mémoire
    this.registerCheck(
      'memory',
      async () => {
        const usage = process.memoryUsage()
        const totalMemory = os.totalmem()
        const freeMemory = os.freemem()
        const usedMemory = totalMemory - freeMemory
        const memoryUsagePercent = (usedMemory / totalMemory) * 100

        return {
          healthy: memoryUsagePercent < 90,
          message:
            memoryUsagePercent < 90
              ? 'Mémoire disponible'
              : 'Utilisation mémoire élevée',
          data: {
            heapUsed: usage.heapUsed,
            heapTotal: usage.heapTotal,
            rss: usage.rss,
            systemMemoryPercent: memoryUsagePercent,
          },
        }
      },
      true
    )

    // Vérification du CPU
    this.registerCheck(
      'cpu',
      async () => {
        const cpus = os.cpus()
        const loadAvg = os.loadavg()

        return {
          healthy: loadAvg[0] < cpus.length * 2,
          message: loadAvg[0] < cpus.length * 2 ? 'CPU OK' : 'Charge CPU élevée',
          data: {
            cores: cpus.length,
            loadAverage: loadAvg,
          },
        }
      },
      false
    )

    // Vérification de l'espace disque
    this.registerCheck(
      'disk',
      async () => {
        // Note: Nécessite une bibliothèque comme 'diskusage' pour une vérification complète
        // Pour l'instant, on retourne toujours OK
        return {
          healthy: true,
          message: 'Espace disque OK',
          data: {},
        }
      },
      false
    )
  }
}

/**
 * Instance singleton
 */
let instance = null

export function getHealthChecker(logger) {
  if (!instance) {
    instance = new HealthChecker(logger)
    instance.registerDefaultChecks()
  }
  return instance
}





