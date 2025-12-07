package com.androidcontroldeck.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.androidcontroldeck.R
import com.androidcontroldeck.network.ConnectionState
import com.androidcontroldeck.network.WebSocketMetrics
import kotlin.math.roundToInt

/**
 * Moniteur de santé de la connexion avec indicateurs visuels
 */
@Composable
fun ConnectionHealthMonitor(
    connectionState: ConnectionState,
    metrics: WebSocketMetrics,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    val healthStatus = calculateHealthStatus(connectionState, metrics)

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (healthStatus.severity) {
                HealthSeverity.GOOD -> MaterialTheme.colorScheme.primaryContainer
                HealthSeverity.WARNING -> MaterialTheme.colorScheme.tertiaryContainer
                HealthSeverity.CRITICAL -> MaterialTheme.colorScheme.errorContainer
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    HealthIndicator(healthStatus.severity)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = healthStatus.title,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = healthStatus.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (expanded) "Collapse" else "Expand"
                    )
                }
            }

            if (expanded) {
                Spacer(modifier = Modifier.height(16.dp))
                HealthDetails(connectionState, metrics, healthStatus)
            }
        }
    }
}

@Composable
private fun HealthIndicator(severity: HealthSeverity) {
    val color = when (severity) {
        HealthSeverity.GOOD -> Color(0xFF4CAF50)
        HealthSeverity.WARNING -> Color(0xFFFF9800)
        HealthSeverity.CRITICAL -> Color(0xFFF44336)
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "alpha"
    )

    Box(
        modifier = Modifier
            .size(16.dp)
            .background(color.copy(alpha = alpha), RoundedCornerShape(50))
    )
}

@Composable
private fun HealthDetails(
    connectionState: ConnectionState,
    metrics: WebSocketMetrics,
    healthStatus: HealthStatus
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        HealthMetric(
            label = stringResource(R.string.connection_status_latency),
            value = "${metrics.lastAckLatencyMs ?: 0}ms",
            status = when {
                (metrics.lastAckLatencyMs ?: 0) < 50 -> HealthSeverity.GOOD
                (metrics.lastAckLatencyMs ?: 0) < 200 -> HealthSeverity.WARNING
                else -> HealthSeverity.CRITICAL
            }
        )

        HealthMetric(
            label = stringResource(R.string.connection_status_avg_latency),
            value = "${metrics.averageAckLatencyMs.roundToInt()}ms",
            status = when {
                metrics.averageAckLatencyMs < 50 -> HealthSeverity.GOOD
                metrics.averageAckLatencyMs < 200 -> HealthSeverity.WARNING
                else -> HealthSeverity.CRITICAL
            }
        )

        HealthMetric(
            label = stringResource(R.string.connection_status_in_flight),
            value = metrics.inFlightMessages.toString(),
            status = when {
                metrics.inFlightMessages < 5 -> HealthSeverity.GOOD
                metrics.inFlightMessages < 15 -> HealthSeverity.WARNING
                else -> HealthSeverity.CRITICAL
            }
        )

        HealthMetric(
            label = stringResource(R.string.connection_status_reconnects),
            value = metrics.reconnectCount.toString(),
            status = when {
                metrics.reconnectCount == 0L -> HealthSeverity.GOOD
                metrics.reconnectCount < 3L -> HealthSeverity.WARNING
                else -> HealthSeverity.CRITICAL
            }
        )

        if (healthStatus.recommendations.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Divider()
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.health_recommendations),
                style = MaterialTheme.typography.titleSmall
            )
            healthStatus.recommendations.forEach { recommendation ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = recommendation,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun HealthMetric(
    label: String,
    value: String,
    status: HealthSeverity
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(
                        when (status) {
                            HealthSeverity.GOOD -> Color(0xFF4CAF50)
                            HealthSeverity.WARNING -> Color(0xFFFF9800)
                            HealthSeverity.CRITICAL -> Color(0xFFF44336)
                        },
                        RoundedCornerShape(50)
                    )
            )
        }
    }
}

@Composable
private fun calculateHealthStatus(
    connectionState: ConnectionState,
    metrics: WebSocketMetrics
): HealthStatus {
    val recommendations = mutableListOf<String>()

    when (connectionState) {
        is ConnectionState.Connected -> {
            val latency: Int = metrics.lastAckLatencyMs?.toInt() ?: metrics.averageAckLatencyMs.roundToInt()
            val inFlight = metrics.inFlightMessages
            val reconnects = metrics.reconnectCount

            val issues = mutableListOf<String>()

            if (latency > 200) {
                issues.add("high_latency")
                recommendations.add("Vérifiez votre connexion réseau")
            }
            if (inFlight > 15) {
                issues.add("high_inflight")
                recommendations.add("Réduisez la fréquence des actions")
            }
            if (reconnects > 3L) {
                issues.add("high_reconnects")
                recommendations.add("Vérifiez la stabilité de votre connexion")
            }
            if (metrics.droppedAcks > 0) {
                issues.add("dropped_acks")
                recommendations.add("Certains messages ont été perdus")
            }

            return when {
                issues.isEmpty() -> HealthStatus(
                    severity = HealthSeverity.GOOD,
                    title = stringResource(R.string.health_status_excellent),
                    message = stringResource(R.string.health_status_excellent_desc),
                    recommendations = recommendations
                )
                issues.size == 1 -> HealthStatus(
                    severity = HealthSeverity.WARNING,
                    title = stringResource(R.string.health_status_good),
                    message = stringResource(R.string.health_status_good_desc),
                    recommendations = recommendations
                )
                else -> HealthStatus(
                    severity = HealthSeverity.CRITICAL,
                    title = stringResource(R.string.health_status_poor),
                    message = stringResource(R.string.health_status_poor_desc),
                    recommendations = recommendations
                )
            }
        }
        is ConnectionState.Connecting -> {
            return HealthStatus(
                severity = HealthSeverity.WARNING,
                title = stringResource(R.string.health_status_connecting),
                message = stringResource(R.string.health_status_connecting_desc),
                recommendations = listOf("Attente de la connexion...")
            )
        }
        is ConnectionState.Disconnected -> {
            return HealthStatus(
                severity = HealthSeverity.CRITICAL,
                title = stringResource(R.string.health_status_disconnected),
                message = stringResource(R.string.health_status_disconnected_desc),
                recommendations = listOf(
                    "Vérifiez que le serveur est démarré",
                    "Vérifiez votre connexion réseau",
                    "Vérifiez les paramètres de connexion"
                )
            )
        }
    }
}

private data class HealthStatus(
    val severity: HealthSeverity,
    val title: String,
    val message: String,
    val recommendations: List<String> = emptyList()
)

private enum class HealthSeverity {
    GOOD, WARNING, CRITICAL
}


