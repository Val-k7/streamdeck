package com.androidcontroldeck.ui.utils

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Moniteur de performance pour l'application Android
 */
object PerformanceMonitor {
    private const val TAG = "PerformanceMonitor"
    private val _metrics = MutableStateFlow(PerformanceMetrics())
    val metrics: StateFlow<PerformanceMetrics> = _metrics.asStateFlow()

    private val actionTimings = mutableMapOf<String, MutableList<Long>>()
    private const val MAX_TIMINGS_PER_ACTION = 100

    /**
     * Enregistre le temps d'exécution d'une action
     */
    fun recordAction(action: String, durationMs: Long) {
        val timings = actionTimings.getOrPut(action) { mutableListOf() }
        timings.add(durationMs)
        if (timings.size > MAX_TIMINGS_PER_ACTION) {
            timings.removeAt(0)
        }

        updateMetrics()
        DebugTools.log(LogLevel.DEBUG, "Performance", "Action '$action' took ${durationMs}ms")
    }

    /**
     * Mesure le temps d'exécution d'une fonction
     */
    inline fun <T> measure(action: String, block: () -> T): T {
        val startTime = System.currentTimeMillis()
        return try {
            block()
        } finally {
            val duration = System.currentTimeMillis() - startTime
            recordAction(action, duration)
        }
    }

    /**
     * Met à jour les métriques
     */
    private fun updateMetrics() {
        val totalActions = actionTimings.values.sumOf { it.size }
        val totalDuration = actionTimings.values.flatten().sum()
        val averageDuration = if (totalActions > 0) totalDuration / totalActions else 0L

        val actionStats = actionTimings.mapValues { (_, timings) ->
            ActionStats(
                count = timings.size,
                averageMs = timings.average().toLong(),
                minMs = timings.minOrNull() ?: 0L,
                maxMs = timings.maxOrNull() ?: 0L,
                p50Ms = timings.sorted()[timings.size / 2],
                p95Ms = timings.sorted()[(timings.size * 95 / 100).coerceAtMost(timings.size - 1)]
            )
        }

        _metrics.value = PerformanceMetrics(
            totalActions = totalActions,
            averageDurationMs = averageDuration,
            actionStats = actionStats
        )
    }

    /**
     * Réinitialise les métriques
     */
    fun reset() {
        actionTimings.clear()
        _metrics.value = PerformanceMetrics()
        DebugTools.log(LogLevel.INFO, "Performance", "Metrics reset")
    }

    /**
     * Obtient les statistiques d'une action spécifique
     */
    fun getActionStats(action: String): ActionStats? {
        val timings = actionTimings[action] ?: return null
        return ActionStats(
            count = timings.size,
            averageMs = timings.average().toLong(),
            minMs = timings.minOrNull() ?: 0L,
            maxMs = timings.maxOrNull() ?: 0L,
            p50Ms = timings.sorted()[timings.size / 2],
            p95Ms = timings.sorted()[(timings.size * 95 / 100).coerceAtMost(timings.size - 1)]
        )
    }
}

/**
 * Métriques de performance
 */
data class PerformanceMetrics(
    val totalActions: Int = 0,
    val averageDurationMs: Long = 0,
    val actionStats: Map<String, ActionStats> = emptyMap()
)

/**
 * Statistiques d'une action
 */
data class ActionStats(
    val count: Int,
    val averageMs: Long,
    val minMs: Long,
    val maxMs: Long,
    val p50Ms: Long,
    val p95Ms: Long
)





