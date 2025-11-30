package com.androidcontroldeck

import android.app.Application
import com.androidcontroldeck.data.preferences.SettingsRepository
import com.androidcontroldeck.data.preferences.SecurePreferences
import com.androidcontroldeck.data.repository.ProfileRepository
import com.androidcontroldeck.data.storage.ProfileStorage
import com.androidcontroldeck.data.storage.AssetCache
import com.androidcontroldeck.logging.DiagnosticsRepository
import com.androidcontroldeck.logging.UnifiedLogger
import com.androidcontroldeck.network.AuthRepository
import com.androidcontroldeck.network.ConnectionManager
import com.androidcontroldeck.network.ControlEventSender
import com.androidcontroldeck.network.NetworkStatusMonitor
import com.androidcontroldeck.network.WebSocketClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class ControlDeckApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

class AppContainer(private val application: Application) {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val assetCache by lazy { AssetCache(application) }
    private val profileStorage by lazy { ProfileStorage(application) }
    val profileRepository by lazy { ProfileRepository(profileStorage, scope = appScope, stringProvider = application::getString) }

    val securePreferences by lazy { SecurePreferences(application) }
    private val settingsRepository by lazy { SettingsRepository(application) }
    val networkMonitor by lazy { NetworkStatusMonitor(application, appScope) }
    private val authRepository by lazy { AuthRepository(securePreferences) }
    val logger by lazy { UnifiedLogger(application, appScope) }
    private val webSocketClient by lazy {
        WebSocketClient(
            scope = appScope,
            networkStatusMonitor = networkMonitor,
            logger = logger,
        )
    }
    val diagnosticsRepository by lazy {
        DiagnosticsRepository(
            application,
            appScope,
            webSocketClient,
            logger
        )
    }
    val connectionManager by lazy {
        ConnectionManager(
            settingsRepository,
            webSocketClient,
            authRepository,
            diagnosticsRepository,
            logger,
            appScope
        )
    }
    val controlEventSender by lazy { ControlEventSender(webSocketClient, logger) }
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
