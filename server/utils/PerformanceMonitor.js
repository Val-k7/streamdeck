import os from 'os'
import { performance } from 'node:perf_hooks'

/**
 * Moniteur de performances pour le serveur
 */

class PerformanceMonitor {
  constructor() {
    this.metrics = {
      requests: {
        total: 0,
        successful: 0,
        failed: 0,
        averageLatency: 0,
        p50: [],
        p95: [],
        p99: [],
      },
      actions: {
        total: 0,
        byType: new Map(),
        averageLatency: 0,
      },
      memory: {
        heapUsed: [],
        heapTotal: [],
        external: [],
        rss: [],
      },
      cpu: {
        usage: [],
        loadAverage: [],
      },
      websocket: {
        messages: 0,
        connections: 0,
        averageMessageLatency: 0,
      },
    }

    this.startTime = Date.now()
    this.sampleInterval = null
    this.startSampling()
  }

  /**
   * Démarre l'échantillonnage périodique
   */
  startSampling(intervalMs = 5000) {
    if (this.sampleInterval) {
      clearInterval(this.sampleInterval)
    }

    this.sampleInterval = setInterval(() => {
      this.sampleMetrics()
    }, intervalMs)
  }

  /**
   * Arrête l'échantillonnage
   */
  stopSampling() {
    if (this.sampleInterval) {
      clearInterval(this.sampleInterval)
      this.sampleInterval = null
    }
  }

  /**
   * Échantillonne les métriques actuelles
   */
  sampleMetrics() {
    const memUsage = process.memoryUsage()
    const cpuUsage = process.cpuUsage()

    // Mémoire
    this.metrics.memory.heapUsed.push({
      timestamp: Date.now(),
      value: memUsage.heapUsed,
    })
    this.metrics.memory.heapTotal.push({
      timestamp: Date.now(),
      value: memUsage.heapTotal,
    })
    this.metrics.memory.external.push({
      timestamp: Date.now(),
      value: memUsage.external,
    })
    this.metrics.memory.rss.push({
      timestamp: Date.now(),
      value: memUsage.rss,
    })

    // Garder seulement les 100 derniers échantillons
    const maxSamples = 100
    Object.keys(this.metrics.memory).forEach((key) => {
      if (this.metrics.memory[key].length > maxSamples) {
        this.metrics.memory[key] = this.metrics.memory[key].slice(-maxSamples)
      }
    })

    // CPU
    const loadAvg = os.loadavg()
    this.metrics.cpu.loadAverage.push({
      timestamp: Date.now(),
      value: loadAvg[0], // 1 minute load average
    })

    if (this.metrics.cpu.loadAverage.length > maxSamples) {
      this.metrics.cpu.loadAverage = this.metrics.cpu.loadAverage.slice(-maxSamples)
    }
  }

  /**
   * Enregistre une requête
   */
  recordRequest(latency, success = true) {
    this.metrics.requests.total++
    if (success) {
      this.metrics.requests.successful++
    } else {
      this.metrics.requests.failed++
    }

    // Mettre à jour la latence moyenne
    const total = this.metrics.requests.total
    this.metrics.requests.averageLatency =
      (this.metrics.requests.averageLatency * (total - 1) + latency) / total

    // Ajouter aux percentiles
    this.metrics.requests.p50.push(latency)
    this.metrics.requests.p95.push(latency)
    this.metrics.requests.p99.push(latency)

    // Garder seulement les 1000 dernières valeurs pour les percentiles
    const maxPercentileSamples = 1000
    if (this.metrics.requests.p50.length > maxPercentileSamples) {
      this.metrics.requests.p50 = this.metrics.requests.p50.slice(-maxPercentileSamples)
      this.metrics.requests.p95 = this.metrics.requests.p95.slice(-maxPercentileSamples)
      this.metrics.requests.p99 = this.metrics.requests.p99.slice(-maxPercentileSamples)
    }
  }

  /**
   * Enregistre une action
   */
  recordAction(actionType, latency, success = true) {
    this.metrics.actions.total++

    if (!this.metrics.actions.byType.has(actionType)) {
      this.metrics.actions.byType.set(actionType, {
        total: 0,
        successful: 0,
        failed: 0,
        averageLatency: 0,
      })
    }

    const actionMetrics = this.metrics.actions.byType.get(actionType)
    actionMetrics.total++
    if (success) {
      actionMetrics.successful++
    } else {
      actionMetrics.failed++
    }

    // Mettre à jour la latence moyenne
    const total = actionMetrics.total
    actionMetrics.averageLatency =
      (actionMetrics.averageLatency * (total - 1) + latency) / total

    // Mettre à jour la latence moyenne globale
    const globalTotal = this.metrics.actions.total
    this.metrics.actions.averageLatency =
      (this.metrics.actions.averageLatency * (globalTotal - 1) + latency) / globalTotal
  }

  /**
   * Enregistre un message WebSocket
   */
  recordWebSocketMessage(latency) {
    this.metrics.websocket.messages++

    const total = this.metrics.websocket.messages
    this.metrics.websocket.averageMessageLatency =
      (this.metrics.websocket.averageMessageLatency * (total - 1) + latency) / total
  }

  /**
   * Calcule un percentile
   */
  calculatePercentile(values, percentile) {
    if (values.length === 0) return 0
    const sorted = [...values].sort((a, b) => a - b)
    const index = Math.ceil((percentile / 100) * sorted.length) - 1
    return sorted[Math.max(0, index)]
  }

  /**
   * Obtient un rapport de performance
   */
  getReport() {
    const memUsage = process.memoryUsage()
    const uptime = Date.now() - this.startTime

    return {
      uptime: {
        seconds: Math.floor(uptime / 1000),
        formatted: this.formatUptime(uptime),
      },
      requests: {
        total: this.metrics.requests.total,
        successful: this.metrics.requests.successful,
        failed: this.metrics.requests.failed,
        successRate:
          this.metrics.requests.total > 0
            ? (this.metrics.requests.successful / this.metrics.requests.total) * 100
            : 0,
        averageLatency: Math.round(this.metrics.requests.averageLatency),
        p50: this.calculatePercentile(this.metrics.requests.p50, 50),
        p95: this.calculatePercentile(this.metrics.requests.p95, 95),
        p99: this.calculatePercentile(this.metrics.requests.p99, 99),
      },
      actions: {
        total: this.metrics.actions.total,
        averageLatency: Math.round(this.metrics.actions.averageLatency),
        byType: Object.fromEntries(
          Array.from(this.metrics.actions.byType.entries()).map(([type, metrics]) => [
            type,
            {
              total: metrics.total,
              successful: metrics.successful,
              failed: metrics.failed,
              successRate:
                metrics.total > 0 ? (metrics.successful / metrics.total) * 100 : 0,
              averageLatency: Math.round(metrics.averageLatency),
            },
          ])
        ),
      },
      memory: {
        current: {
          heapUsed: this.formatBytes(memUsage.heapUsed),
          heapTotal: this.formatBytes(memUsage.heapTotal),
          external: this.formatBytes(memUsage.external),
          rss: this.formatBytes(memUsage.rss),
        },
        heapUsed: {
          current: memUsage.heapUsed,
          average: this.calculateAverage(this.metrics.memory.heapUsed),
          max: Math.max(...this.metrics.memory.heapUsed.map((s) => s.value), 0),
          min: Math.min(...this.metrics.memory.heapUsed.map((s) => s.value), 0),
        },
        heapTotal: {
          current: memUsage.heapTotal,
          average: this.calculateAverage(this.metrics.memory.heapTotal),
          max: Math.max(...this.metrics.memory.heapTotal.map((s) => s.value), 0),
          min: Math.min(...this.metrics.memory.heapTotal.map((s) => s.value), 0),
        },
      },
      cpu: {
        loadAverage: {
          current: os.loadavg()[0],
          average: this.calculateAverage(this.metrics.cpu.loadAverage),
          max: Math.max(...this.metrics.cpu.loadAverage.map((s) => s.value), 0),
          min: Math.min(...this.metrics.cpu.loadAverage.map((s) => s.value), 0),
        },
        cores: os.cpus().length,
      },
      websocket: {
        messages: this.metrics.websocket.messages,
        averageMessageLatency: Math.round(this.metrics.websocket.averageMessageLatency),
      },
    }
  }

  /**
   * Calcule la moyenne d'une série de valeurs
   */
  calculateAverage(samples) {
    if (samples.length === 0) return 0
    const sum = samples.reduce((acc, s) => acc + s.value, 0)
    return sum / samples.length
  }

  /**
   * Formate les bytes en format lisible
   */
  formatBytes(bytes) {
    if (bytes === 0) return '0 B'
    const k = 1024
    const sizes = ['B', 'KB', 'MB', 'GB']
    const i = Math.floor(Math.log(bytes) / Math.log(k))
    return Math.round((bytes / Math.pow(k, i)) * 100) / 100 + ' ' + sizes[i]
  }

  /**
   * Formate le temps de fonctionnement
   */
  formatUptime(ms) {
    const seconds = Math.floor(ms / 1000)
    const minutes = Math.floor(seconds / 60)
    const hours = Math.floor(minutes / 60)
    const days = Math.floor(hours / 24)

    if (days > 0) {
      return `${days}d ${hours % 24}h ${minutes % 60}m`
    } else if (hours > 0) {
      return `${hours}h ${minutes % 60}m ${seconds % 60}s`
    } else if (minutes > 0) {
      return `${minutes}m ${seconds % 60}s`
    } else {
      return `${seconds}s`
    }
  }

  /**
   * Réinitialise les métriques
   */
  reset() {
    this.metrics = {
      requests: {
        total: 0,
        successful: 0,
        failed: 0,
        averageLatency: 0,
        p50: [],
        p95: [],
        p99: [],
      },
      actions: {
        total: 0,
        byType: new Map(),
        averageLatency: 0,
      },
      memory: {
        heapUsed: [],
        heapTotal: [],
        external: [],
        rss: [],
      },
      cpu: {
        usage: [],
        loadAverage: [],
      },
      websocket: {
        messages: 0,
        connections: 0,
        averageMessageLatency: 0,
      },
    }
    this.startTime = Date.now()
  }
}

// Singleton pour le serveur
let globalPerformanceMonitor = null

export function getPerformanceMonitor() {
  if (!globalPerformanceMonitor) {
    globalPerformanceMonitor = new PerformanceMonitor()
  }
  return globalPerformanceMonitor
}

export function createPerformanceMonitor() {
  return new PerformanceMonitor()
}

export default PerformanceMonitor

