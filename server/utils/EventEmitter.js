/**
 * Système d'événements pour le serveur
 *
 * Permet d'émettre et d'écouter des événements de manière découplée
 */

import { EventEmitter as NodeEventEmitter } from 'events'

/**
 * Gestionnaire d'événements étendu
 */
export class EventEmitter extends NodeEventEmitter {
  constructor() {
    super()
    this.maxListeners = 50
    this.eventHistory = new Map() // event -> [last 10 events]
    this.maxHistorySize = 10
  }

  /**
   * Émet un événement avec historique
   */
  emit(event, ...args) {
    // Ajouter à l'historique
    if (!this.eventHistory.has(event)) {
      this.eventHistory.set(event, [])
    }
    const history = this.eventHistory.get(event)
    history.push({
      timestamp: Date.now(),
      args: args.map((arg) => this.serializeArg(arg)),
    })
    if (history.length > this.maxHistorySize) {
      history.shift()
    }

    // Émettre l'événement
    return super.emit(event, ...args)
  }

  /**
   * Sérialise un argument pour l'historique
   */
  serializeArg(arg) {
    if (arg === null || arg === undefined) return arg
    if (typeof arg === 'function') return '[Function]'
    if (typeof arg === 'object') {
      try {
        return JSON.stringify(arg)
      } catch {
        return '[Object]'
      }
    }
    return arg
  }

  /**
   * Obtient l'historique d'un événement
   */
  getEventHistory(event) {
    return this.eventHistory.get(event) || []
  }

  /**
   * Obtient tous les événements émis récemment
   */
  getAllHistory() {
    const all = {}
    for (const [event, history] of this.eventHistory.entries()) {
      all[event] = history
    }
    return all
  }

  /**
   * Réinitialise l'historique
   */
  clearHistory() {
    this.eventHistory.clear()
  }

  /**
   * Écoute un événement une seule fois avec timeout
   */
  onceWithTimeout(event, timeoutMs, onEvent, onTimeout) {
    let timeout
    const listener = (...args) => {
      clearTimeout(timeout)
      this.removeListener(event, listener)
      onEvent(...args)
    }

    timeout = setTimeout(() => {
      this.removeListener(event, listener)
      if (onTimeout) {
        onTimeout()
      }
    }, timeoutMs)

    this.once(event, listener)
  }

  /**
   * Écoute plusieurs événements
   */
  onAny(events, callback) {
    const listeners = new Map()
    events.forEach((event) => {
      const listener = (...args) => {
        callback(event, ...args)
      }
      listeners.set(event, listener)
      this.on(event, listener)
    })

    // Retourner une fonction pour supprimer tous les listeners
    return () => {
      listeners.forEach((listener, event) => {
        this.removeListener(event, listener)
      })
    }
  }
}

/**
 * Instance globale d'EventEmitter
 */
let globalEventEmitter = null

export function getGlobalEventEmitter() {
  if (!globalEventEmitter) {
    globalEventEmitter = new EventEmitter()
  }
  return globalEventEmitter
}





