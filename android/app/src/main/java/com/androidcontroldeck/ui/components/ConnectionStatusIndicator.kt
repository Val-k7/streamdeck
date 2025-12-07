package com.androidcontroldeck.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.androidcontroldeck.R
import com.androidcontroldeck.network.ConnectionState
import com.androidcontroldeck.network.WebSocketMetrics
import kotlinx.coroutines.delay
import kotlin.math.max
import kotlin.math.min

/**
 * Indicateur de statut de connexion avec informations détaillées
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectionStatusIndicator(
    connectionState: ConnectionState,
    metrics: WebSocketMetrics,
    modifier: Modifier = Modifier,
    showDetails: Boolean = false,
    onToggleDetails: (() -> Unit)? = null
) {
    val statusColor = when (connectionState) {
        is ConnectionState.Connected -> Color(0xFF4CAF50) // Vert
        is ConnectionState.Connecting -> Color(0xFFFF9800) // Orange
        is ConnectionState.Disconnected -> Color(0xFFF44336) // Rouge
    }

    val statusText = when (connectionState) {
        is ConnectionState.Connected -> "Connecté"
        is ConnectionState.Connecting -> "Connexion..."
        is ConnectionState.Disconnected -> "Déconnecté"
    }

    val statusIcon = when (connectionState) {
        is ConnectionState.Connected -> Icons.Default.Wifi
        is ConnectionState.Connecting -> Icons.Default.Sync
        is ConnectionState.Disconnected -> Icons.Default.WifiOff
    }

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        onClick = { onToggleDetails?.invoke() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Indicateur de statut avec animation
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Pulsation animée pour l'indicateur de statut
                StatusIndicatorDot(
                    color = statusColor,
                    isActive = connectionState is ConnectionState.Connected || connectionState is ConnectionState.Connecting
                )

                Column {
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )

                    if (connectionState is ConnectionState.Connected && metrics.lastAckLatencyMs != null) {
                        val latency = metrics.lastAckLatencyMs
                        val latencyColor = when {
                            latency < 50 -> Color(0xFF4CAF50) // Excellent
                            latency < 100 -> Color(0xFF8BC34A) // Bon
                            latency < 200 -> Color(0xFFFFC107) // Moyen
                            else -> Color(0xFFFF5722) // Mauvais
                        }

                        Text(
                            text = "Latence: ${latency}ms",
                            style = MaterialTheme.typography.bodySmall,
                            color = latencyColor
                        )
                    }
                }
            }

            Icon(
                imageVector = statusIcon,
                contentDescription = null,
                tint = statusColor,
                modifier = Modifier.size(24.dp)
            )
        }

        // Détails étendus
        if (showDetails && connectionState is ConnectionState.Connected) {
            Divider(modifier = Modifier.padding(horizontal = 12.dp))
            ConnectionDetails(
                metrics = metrics,
                modifier = Modifier.padding(12.dp)
            )
        }
    }
}

@Composable
private fun StatusIndicatorDot(
    color: Color,
    isActive: Boolean
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")

    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isActive) 1.2f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isActive) 0.7f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Box(
        modifier = Modifier
            .size(12.dp)
            .clip(CircleShape)
            .background(color.copy(alpha = alpha))
            .then(Modifier.size((12 * scale).dp))
    )
}

@Composable
private fun ConnectionDetails(
    metrics: WebSocketMetrics,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Statistiques de connexion",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )

        // Métriques principales
        MetricRow(
            label = "Latence moyenne",
            value = if (metrics.averageAckLatencyMs > 0) {
                "${metrics.averageAckLatencyMs.toInt()}ms"
            } else {
                "N/A"
            }
        )

        MetricRow(
            label = "Dernière latence",
            value = metrics.lastAckLatencyMs?.let { "${it}ms" } ?: "N/A"
        )

        MetricRow(
            label = "Messages envoyés",
            value = "${metrics.ackCount}"
        )

        MetricRow(
            label = "Messages perdus",
            value = "${metrics.droppedAcks}",
            color = if (metrics.droppedAcks > 0) MaterialTheme.colorScheme.error else null
        )

        MetricRow(
            label = "En attente",
            value = "${metrics.inFlightMessages}"
        )

        MetricRow(
            label = "Reconnexions",
            value = "${metrics.reconnectCount}",
            color = if (metrics.reconnectCount > 0) MaterialTheme.colorScheme.error else null
        )

        MetricRow(
            label = "Heartbeats",
            value = "${metrics.heartbeatCount}"
        )

        metrics.lastHeartbeatAtMs?.let {
            MetricRow(
                label = "Dernier heartbeat",
                value = "${(System.currentTimeMillis() - it) / 1000}s"
            )
        }

        metrics.lastReconnectDelayMs?.let {
            MetricRow(
                label = "Dernier délai reconnexion",
                value = "${it}ms"
            )
        }
    }
}

@Composable
private fun MetricRow(
    label: String,
    value: String,
    color: Color? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = color ?: MaterialTheme.colorScheme.onSurface
        )
    }
}

/**
 * Histogramme simple pour afficher la latence
 */
@Composable
fun LatencyHistoryChart(
    latencies: List<Long>,
    modifier: Modifier = Modifier
) {
    if (latencies.isEmpty()) return

    val maxLatency = latencies.maxOrNull() ?: 100L
    val minLatency = latencies.minOrNull() ?: 0L

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Historique de latence",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                latencies.takeLast(30).forEach { latency ->
                    val normalizedHeight = if (maxLatency > 0) {
                        ((latency - minLatency).toFloat() / (maxLatency - minLatency).toFloat())
                            .coerceIn(0f, 1f)
                    } else {
                        0.5f
                    }

                    val barColor = when {
                        latency < 50 -> Color(0xFF4CAF50)
                        latency < 100 -> Color(0xFF8BC34A)
                        latency < 200 -> Color(0xFFFFC107)
                        else -> Color(0xFFFF5722)
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(normalizedHeight)
                            .clip(RoundedCornerShape(2.dp))
                            .background(barColor)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Min: ${minLatency}ms",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Max: ${maxLatency}ms",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

