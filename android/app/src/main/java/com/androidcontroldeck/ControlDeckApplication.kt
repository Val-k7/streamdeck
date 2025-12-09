package com.androidcontroldeck

import android.app.Application
import android.webkit.WebView
import com.androidcontroldeck.data.diagnostics.DiagnosticsReporter
import com.androidcontroldeck.data.feedback.FeedbackRepository
import com.androidcontroldeck.data.preferences.PairedServersRepository
import com.androidcontroldeck.data.preferences.SecurePreferences
import com.androidcontroldeck.data.preferences.SettingsRepository
import com.androidcontroldeck.data.repository.ProfileRepository
import com.androidcontroldeck.data.storage.AssetCache
import com.androidcontroldeck.data.storage.PendingActionStorage
import com.androidcontroldeck.data.storage.ProfileBackupManager
import com.androidcontroldeck.data.storage.ProfileStorage
import com.androidcontroldeck.logging.DiagnosticsRepository
import com.androidcontroldeck.logging.UnifiedLogger
import com.androidcontroldeck.network.AuthRepository
import com.androidcontroldeck.network.ConnectionManager
import com.androidcontroldeck.network.ControlEventSender
import com.androidcontroldeck.network.NetworkStatusMonitor
import com.androidcontroldeck.network.PairingManager
import com.androidcontroldeck.network.ServerDiscoveryManager
import com.androidcontroldeck.network.WebSocketClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class ControlDeckApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        // Activer le debug WebView pour que l'app apparaisse dans chrome://inspect
        WebView.setWebContentsDebuggingEnabled(true)
        container = AppContainer(this)
    }
}

class AppContainer(private val application: Application) {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val assetCache by lazy { AssetCache(application) }
    private val profileStorage by lazy { ProfileStorage(application) }
    val webSocketClient by lazy { WebSocketClient(scope = appScope, networkStatusMonitor = networkMonitor, logger = logger) }
    val profileRepository by lazy {
        ProfileRepository(
                profileStorage,
                scope = appScope,
                stringProvider = application::getString,
                webSocketClient = webSocketClient
        )
    }
    val profileBackupManager by lazy { ProfileBackupManager(application) }

    val securePreferences by lazy { SecurePreferences(application) }
    private val settingsRepository by lazy { SettingsRepository(application) }
    val networkMonitor by lazy { NetworkStatusMonitor(application, appScope) }
    private val authRepository by lazy { AuthRepository(securePreferences) }
    val logger by lazy { UnifiedLogger(application, appScope) }
    val pairedServersRepository by lazy { PairedServersRepository(application) }
    val serverDiscoveryManager by lazy { ServerDiscoveryManager(application, logger, appScope) }
    val pairingManager by lazy {
        PairingManager(
                okhttp3.OkHttpClient.Builder()
                        .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                        .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                        .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                        .build(),
                logger
        )
    }
    private val pendingActionStorage by lazy { PendingActionStorage(application) }
    val diagnosticsRepository by lazy {
        DiagnosticsRepository(application, appScope, webSocketClient, logger)
    }
    val connectionManager by lazy {
        ConnectionManager(settingsRepository, webSocketClient)
    }
    val controlEventSender by lazy {
        ControlEventSender(
                webSocketClient = webSocketClient,
                logger = logger,
                storage = pendingActionStorage,
                scope = appScope
        )
    }
    val preferences by lazy { settingsRepository }

    val diagnosticsReporter by lazy {
        DiagnosticsReporter(
                settingsRepository = settingsRepository,
                securePreferences = securePreferences,
                networkStatusMonitor = networkMonitor,
                connectionManager = connectionManager
        )
    }
    val feedbackRepository by lazy { FeedbackRepository() }
}
