package com.androidcontroldeck.ui.utils

import android.content.Context
import com.androidcontroldeck.logging.AppLogger
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.androidcontroldeck.data.repository.ProfileRepository
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * Outils de débogage pour l'application
 */
object DebugTools {
    private const val TAG = "DebugTools"
    private val debugLogs = mutableListOf<DebugLogEntry>()
    private const val MAX_LOG_ENTRIES = 100

    /**
     * Log un message de débogage
     */
    fun log(level: LogLevel, category: String, message: String, data: Any? = null) {
        val entry = DebugLogEntry(
            timestamp = System.currentTimeMillis(),
            level = level,
            category = category,
            message = message,
            data = data?.toString()
        )

        debugLogs.add(entry)
        if (debugLogs.size > MAX_LOG_ENTRIES) {
            debugLogs.removeAt(0)
        }

        // Logger aussi dans Logcat
        when (level) {
            LogLevel.DEBUG -> AppLogger.d(TAG, "[$category] $message")
            LogLevel.INFO -> AppLogger.i(TAG, "[$category] $message")
            LogLevel.WARN -> AppLogger.w(TAG, "[$category] $message")
            LogLevel.ERROR -> AppLogger.e(TAG, "[$category] $message")
        }
    }

    /**
     * Obtient les logs de débogage
     */
    fun getLogs(): List<DebugLogEntry> {
        return debugLogs.toList()
    }

    /**
     * Efface les logs
     */
    fun clearLogs() {
        debugLogs.clear()
    }

    /**
     * Exporte les logs vers un fichier
     */
    fun exportLogs(context: Context): File? {
        return try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val file = File(context.cacheDir, "debug_logs_$timestamp.txt")
            file.writeText(
                debugLogs.joinToString("\n") { entry ->
                    "${entry.timestamp} [${entry.level}] [${entry.category}] ${entry.message}${if (entry.data != null) " | Data: ${entry.data}" else ""}"
                }
            )
            file
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to export logs", e)
            null
        }
    }

    /**
     * Obtient les statistiques de débogage
     */
    fun getStats(): DebugStats {
        val byLevel = debugLogs.groupBy { it.level }
        val byCategory = debugLogs.groupBy { it.category }

        return DebugStats(
            totalLogs = debugLogs.size,
            byLevel = byLevel.mapValues { it.value.size },
            byCategory = byCategory.mapValues { it.value.size },
            oldestLog = debugLogs.firstOrNull()?.timestamp,
            newestLog = debugLogs.lastOrNull()?.timestamp
        )
    }
}

/**
 * Niveau de log
 */
enum class LogLevel {
    DEBUG,
    INFO,
    WARN,
    ERROR
}

/**
 * Entrée de log de débogage
 */
data class DebugLogEntry(
    val timestamp: Long,
    val level: LogLevel,
    val category: String,
    val message: String,
    val data: String? = null
)

/**
 * Statistiques de débogage
 */
data class DebugStats(
    val totalLogs: Int,
    val byLevel: Map<LogLevel, Int>,
    val byCategory: Map<String, Int>,
    val oldestLog: Long?,
    val newestLog: Long?
)

/**
 * Composable pour afficher les outils de débogage
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugToolsPanel(
    profileRepository: ProfileRepository? = null,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var selectedTab by remember { mutableStateOf(0) }
    var logs by remember { mutableStateOf(DebugTools.getLogs()) }
    var stats by remember { mutableStateOf(DebugTools.getStats()) }

    LaunchedEffect(Unit) {
        // Rafraîchir les logs périodiquement
        while (true) {
            kotlinx.coroutines.delay(1000)
            logs = DebugTools.getLogs()
            stats = DebugTools.getStats()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Outils de débogage") },
        text = {
            Column(modifier = Modifier.fillMaxWidth().heightIn(max = 500.dp)) {
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Logs") }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Stats") }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("Actions") }
                    )
                }

                Spacer(Modifier.height(8.dp))

                when (selectedTab) {
                    0 -> LogsTab(logs = logs)
                    1 -> StatsTab(stats = stats)
                    2 -> ActionsTab(
                        profileRepository = profileRepository,
                        onExportLogs = {
                            coroutineScope.launch {
                                val file = DebugTools.exportLogs(context)
                                if (file != null) {
                                    // Partager le fichier
                                    val sendIntent = android.content.Intent().apply {
                                        action = android.content.Intent.ACTION_SEND
                                        putExtra(android.content.Intent.EXTRA_STREAM, android.net.Uri.fromFile(file))
                                        type = "text/plain"
                                    }
                                    context.startActivity(android.content.Intent.createChooser(sendIntent, "Partager les logs"))
                                }
                            }
                        },
                        onClearLogs = {
                            DebugTools.clearLogs()
                            logs = DebugTools.getLogs()
                            stats = DebugTools.getStats()
                        }
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Fermer")
            }
        }
    )
}

@Composable
private fun LogsTab(logs: List<DebugLogEntry>) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(logs.reversed()) { entry ->
            val color = when (entry.level) {
                LogLevel.DEBUG -> MaterialTheme.colorScheme.primary
                LogLevel.INFO -> MaterialTheme.colorScheme.secondary
                LogLevel.WARN -> MaterialTheme.colorScheme.tertiary
                LogLevel.ERROR -> MaterialTheme.colorScheme.error
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f))
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(entry.timestamp)),
                            style = MaterialTheme.typography.labelSmall
                        )
                        Text(
                            text = "[${entry.level}]",
                            style = MaterialTheme.typography.labelSmall,
                            color = color
                        )
                    }
                    Text(
                        text = "[${entry.category}] ${entry.message}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    if (entry.data != null) {
                        Text(
                            text = entry.data,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatsTab(stats: DebugStats) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        StatCard("Total logs", stats.totalLogs.toString())

        Text("Par niveau", style = MaterialTheme.typography.titleSmall)
        stats.byLevel.forEach { (level, count) ->
            StatCard(level.name, count.toString())
        }

        Text("Par catégorie", style = MaterialTheme.typography.titleSmall)
        stats.byCategory.forEach { (category, count) ->
            StatCard(category, count.toString())
        }
    }
}

@Composable
private fun StatCard(label: String, value: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, style = MaterialTheme.typography.bodyMedium)
            Text(text = value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ActionsTab(
    profileRepository: ProfileRepository?,
    onExportLogs: () -> Unit,
    onClearLogs: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(
            onClick = onExportLogs,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Download, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Exporter les logs")
        }

        Button(
            onClick = onClearLogs,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors()
        ) {
            Icon(Icons.Default.Delete, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Effacer les logs")
        }
    }
}

