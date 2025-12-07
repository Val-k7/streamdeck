/**
 * File d'attente d'actions avec priorités et timeouts
 */

class ActionQueue {
  constructor(maxConcurrent = 5, defaultTimeout = 30000) {
    this.queue = []
    this.running = new Map() // actionId -> { startTime, timeout, promise }
    this.maxConcurrent = maxConcurrent
    this.defaultTimeout = defaultTimeout
    this.stats = {
      total: 0,
      completed: 0,
      failed: 0,
      timedOut: 0,
      averageWaitTime: 0,
      averageExecutionTime: 0,
    }
  }

  /**
   * Ajoute une action à la file d'attente
   */
  async enqueue(action, priority = 0, timeout = null) {
    const actionId = `action_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`
    const enqueueTime = Date.now()
    const actionTimeout = timeout || this.defaultTimeout

    return new Promise((resolve, reject) => {
      this.queue.push({
        actionId,
        action,
        priority,
        timeout: actionTimeout,
        enqueueTime,
        resolve,
        reject,
      })

      // Trier par priorité (plus élevé = exécuté en premier)
      this.queue.sort((a, b) => b.priority - a.priority)

      this.stats.total++
      this.processQueue()
    })
  }

  /**
   * Traite la file d'attente
   */
  async processQueue() {
    // Ne pas traiter si on a atteint le maximum de tâches concurrentes
    if (this.running.size >= this.maxConcurrent) {
      return
    }

    // Pas d'actions en attente
    if (this.queue.length === 0) {
      return
    }

    const item = this.queue.shift()
    const waitTime = Date.now() - item.enqueueTime

    // Mettre à jour le temps d'attente moyen
    const totalCompleted = this.stats.completed + this.stats.failed
    this.stats.averageWaitTime =
      (this.stats.averageWaitTime * totalCompleted + waitTime) / (totalCompleted + 1)

    const startTime = Date.now()
    const timeoutId = setTimeout(() => {
      this.running.delete(item.actionId)
      this.stats.timedOut++
      item.reject(new Error(`Action timeout after ${item.timeout}ms`))
      this.processQueue() // Traiter la prochaine action
    }, item.timeout)

    this.running.set(item.actionId, {
      startTime,
      timeout: timeoutId,
      promise: null,
    })

    try {
      const result = await item.action()
      clearTimeout(timeoutId)
      const executionTime = Date.now() - startTime

      // Mettre à jour le temps d'exécution moyen
      const totalCompleted = this.stats.completed + this.stats.failed
      this.stats.averageExecutionTime =
        (this.stats.averageExecutionTime * totalCompleted + executionTime) / (totalCompleted + 1)

      this.stats.completed++
      this.running.delete(item.actionId)
      item.resolve(result)
    } catch (error) {
      clearTimeout(timeoutId)
      this.stats.failed++
      this.running.delete(item.actionId)
      item.reject(error)
    }

    // Traiter la prochaine action
    this.processQueue()
  }

  /**
   * Annule une action en attente
   */
  cancel(actionId) {
    const index = this.queue.findIndex((item) => item.actionId === actionId)
    if (index !== -1) {
      const item = this.queue.splice(index, 1)[0]
      item.reject(new Error('Action cancelled'))
      return true
    }
    return false
  }

  /**
   * Vide la file d'attente
   */
  clear() {
    this.queue.forEach((item) => {
      item.reject(new Error('Queue cleared'))
    })
    this.queue = []
  }

  /**
   * Obtient les statistiques
   */
  getStats() {
    return {
      ...this.stats,
      queued: this.queue.length,
      running: this.running.size,
      successRate:
        this.stats.total > 0
          ? (this.stats.completed / this.stats.total) * 100
          : 0,
    }
  }

  /**
   * Obtient l'état de la file d'attente
   */
  getStatus() {
    return {
      queued: this.queue.length,
      running: this.running.size,
      maxConcurrent: this.maxConcurrent,
      stats: this.getStats(),
    }
  }
}

// Singleton pour le serveur
let globalActionQueue = null

export function getActionQueue(maxConcurrent = 5, defaultTimeout = 30000) {
  if (!globalActionQueue) {
    globalActionQueue = new ActionQueue(maxConcurrent, defaultTimeout)
  }
  return globalActionQueue
}

export function createActionQueue(maxConcurrent = 5, defaultTimeout = 30000) {
  return new ActionQueue(maxConcurrent, defaultTimeout)
}

export default ActionQueue





