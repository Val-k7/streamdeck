package com.androidcontroldeck.logging

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.androidcontroldeck.BuildConfig
import com.androidcontroldeck.network.WebSocketClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

data class NetworkFailure(val message: String, val at: Long = System.currentTimeMillis())

data class DiagnosticsState(
    val versionName: String = BuildConfig.VERSION_NAME,
    val averageLatencyMs: Double? = null,
    val lastNetworkFailure: NetworkFailure? = null,
)

class DiagnosticsRepository(
    private val context: Context,
    private val scope: CoroutineScope,
    private val webSocketClient: WebSocketClient,
    private val logger: UnifiedLogger,
) {
    private val _manualNetworkFailure = MutableStateFlow<NetworkFailure?>(null)

    val diagnostics: StateFlow<DiagnosticsState> = combine(
        webSocketClient.metrics,
        webSocketClient.lastFailure,
        _manualNetworkFailure,
    ) { metrics, wsFailure, manualFailure ->
        DiagnosticsState(
            averageLatencyMs = metrics.averageAckLatencyMs.takeIf { metrics.ackCount > 0 },
            lastNetworkFailure = manualFailure ?: wsFailure,
        )
    }.stateIn(scope, SharingStarted.Eagerly, DiagnosticsState())

    fun recordNetworkFailure(message: String, throwable: Throwable? = null) {
        scope.launch {
            val failure = NetworkFailure(message)
            _manualNetworkFailure.emit(failure)
            logger.logNetworkError(message, throwable)
        }
    }

    fun clearManualNetworkFailure() {
        scope.launch { _manualNetworkFailure.emit(null) }
    }

    fun exportLogArchive(): Pair<File, Uri> {
        val archive = logger.exportCompressedLogs()
        val uri = FileProvider.getUriForFile(
            context,
            "${BuildConfig.APPLICATION_ID}.fileprovider",
            archive
        )
        return archive to uri
    }
}
