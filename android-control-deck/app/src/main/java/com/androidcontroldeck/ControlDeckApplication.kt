package com.androidcontroldeck

import android.app.Application
import com.androidcontroldeck.data.preferences.SettingsRepository
import com.androidcontroldeck.data.preferences.SecurePreferences
import com.androidcontroldeck.data.repository.ProfileRepository
import com.androidcontroldeck.data.storage.ProfileStorage
import com.androidcontroldeck.data.storage.AssetCache
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
    private val networkMonitor by lazy { NetworkStatusMonitor(application, appScope) }
    private val authRepository by lazy { AuthRepository(securePreferences) }
    private val webSocketClient by lazy {
        WebSocketClient(
            scope = appScope,
            networkStatusMonitor = networkMonitor,
        )
    }
    val connectionManager by lazy { ConnectionManager(settingsRepository, webSocketClient, authRepository, appScope) }
    val controlEventSender by lazy { ControlEventSender(webSocketClient) }
    val preferences by lazy { settingsRepository }
}
