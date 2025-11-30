package com.androidcontroldeck

import android.app.Application
import com.androidcontroldeck.data.preferences.SettingsRepository
import com.androidcontroldeck.data.repository.ProfileRepository
import com.androidcontroldeck.data.storage.ProfileStorage
import com.androidcontroldeck.network.ConnectionManager
import com.androidcontroldeck.network.ControlEventSender
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

    private val profileStorage by lazy { ProfileStorage(application) }
    val profileRepository by lazy { ProfileRepository(profileStorage, scope = appScope) }

    private val settingsRepository by lazy { SettingsRepository(application) }
    private val webSocketClient by lazy { WebSocketClient(scope = appScope) }
    val connectionManager by lazy { ConnectionManager(settingsRepository, webSocketClient, appScope) }
    val controlEventSender by lazy { ControlEventSender(webSocketClient) }
    val preferences by lazy { settingsRepository }
}
