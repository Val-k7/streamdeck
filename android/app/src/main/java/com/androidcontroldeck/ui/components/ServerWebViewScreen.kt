package com.androidcontroldeck.ui.components

import android.annotation.SuppressLint
import android.content.Context
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import com.androidcontroldeck.AppContainer
import com.androidcontroldeck.BuildConfig
import com.androidcontroldeck.data.preferences.SettingsState
import com.androidcontroldeck.logging.AppLogger
import com.androidcontroldeck.network.model.DiscoveredServer
import com.androidcontroldeck.ui.navigation.Destination

/**
 * Écran principal simplifié : découverte de serveurs + WebView
 *
 * Architecture :
 * - État "Découverte" : liste les serveurs trouvés via mDNS/UDP
 * - État "Connecté" : charge l'URL du serveur dans une WebView
 * - La Web UI (React) est servie par le serveur Node.js
 * - Communication via HTTP/WebSocket direct (pas de bridge complexe)
 */
@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterialApi::class)
@Composable
fun ServerWebViewScreen(
    container: AppContainer,
    navController: NavController? = null,
    settings: SettingsState,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    // État de découverte
    val discoveredServers by container.serverDiscoveryManager.discoveredServers.collectAsState()
    val isDiscovering by container.serverDiscoveryManager.isDiscovering.collectAsState()
    val pairedServers by
            container.pairedServersRepository.pairedServers.collectAsState(initial = emptyList())

    val configuredUrl = remember(settings) {
        val ip = settings.serverIp.trim()
        if (ip.isNotEmpty()) {
            val protocol = if (settings.useTls) "https" else "http"
            "$protocol://$ip:${settings.serverPort}"
        } else {
            null
        }
    }

    // Serveur sélectionné pour afficher dans la WebView
    var selectedServerUrl by remember { mutableStateOf<String?>(null) }
    var webViewError by remember { mutableStateOf<String?>(null) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var isRefreshing by remember { mutableStateOf(false) }
    val pullRefreshState = rememberPullRefreshState(
            refreshing = isRefreshing,
            onRefresh = {
                isRefreshing = true
                webViewRef?.reload()
                container.serverDiscoveryManager.startDiscovery()
            }
    )

    LaunchedEffect(configuredUrl) {
        if (selectedServerUrl == null && configuredUrl != null) {
            selectedServerUrl = configuredUrl
        }
    }

    // Démarrer la découverte au lancement
    LaunchedEffect(Unit) {
        if (BuildConfig.DEBUG) {
            WebView.setWebContentsDebuggingEnabled(true)
        }
        container.serverDiscoveryManager.startDiscovery()
    }

    // Auto-connexion au premier serveur appairé trouvé
    LaunchedEffect(discoveredServers, pairedServers) {
        if (selectedServerUrl != null) return@LaunchedEffect

        // Chercher un serveur appairé avec auto-connect parmi les découverts
        val autoConnectServer =
                discoveredServers.firstOrNull { discovered ->
                    pairedServers.any { paired ->
                        paired.serverId == discovered.serverId && paired.isAutoConnect
                    }
                }

        if (autoConnectServer != null) {
            AppLogger.d("ServerWebView", "Auto-connecting to ${autoConnectServer.serverName}")
            val protocol = if (autoConnectServer.protocol == "https") "https" else "http"
            selectedServerUrl = "$protocol://${autoConnectServer.host}:${autoConnectServer.port}"
            container.serverDiscoveryManager.stopDiscovery()
        }
    }

    // Cleanup à la destruction
    DisposableEffect(Unit) { onDispose { container.serverDiscoveryManager.stopDiscovery() } }

    if (selectedServerUrl != null && webViewError == null) {
        Box(modifier = modifier.fillMaxSize().pullRefresh(pullRefreshState)) {
            ServerWebView(
                    serverUrl = selectedServerUrl!!,
                    context = context,
                    onError = { error ->
                        webViewError = error
                        selectedServerUrl = null
                    },
                    onWebViewReady = { created -> webViewRef = created },
                    onPageLoaded = { isRefreshing = false },
                    modifier = Modifier.fillMaxSize()
            )
            PullRefreshIndicator(
                    refreshing = isRefreshing,
                    state = pullRefreshState,
                    modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    } else {
        // Afficher l'écran de découverte
        ServerDiscoveryScreen(
                discoveredServers = discoveredServers,
                pairedServers = pairedServers.map { it.serverId }.toSet(),
                isDiscovering = isDiscovering,
                error = webViewError,
                configuredUrl = configuredUrl,
                onServerSelected = { server ->
                    webViewError = null
                    val protocol = if (server.protocol == "https") "https" else "http"
                    selectedServerUrl = "$protocol://${server.host}:${server.port}"
                    container.serverDiscoveryManager.stopDiscovery()
                },
                onUseConfiguredUrl = {
                    webViewError = null
                    selectedServerUrl = configuredUrl
                },
                onRefresh = {
                    webViewError = null
                    container.serverDiscoveryManager.startDiscovery()
                },
                onOpenSettings = { navController?.navigate(Destination.Settings.route) },
                modifier = modifier
        )
    }
}

@Composable
private fun ServerDiscoveryScreen(
        discoveredServers: List<DiscoveredServer>,
        pairedServers: Set<String>,
        isDiscovering: Boolean,
        error: String?,
    configuredUrl: String?,
        onServerSelected: (DiscoveredServer) -> Unit,
    onUseConfiguredUrl: () -> Unit,
        onRefresh: () -> Unit,
        onOpenSettings: () -> Unit,
        modifier: Modifier = Modifier
) {
    Scaffold(
            topBar = {
                TopAppBar(
                        title = { Text("Control Deck") },
                        actions = {
                            IconButton(onClick = onRefresh) {
                                Icon(Icons.Default.Refresh, contentDescription = "Actualiser")
                            }
                            IconButton(onClick = onOpenSettings) {
                                Icon(Icons.Default.Settings, contentDescription = "Paramètres")
                            }
                        }
                )
            },
            modifier = modifier
    ) { padding ->
        Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Erreur éventuelle
            error?.let {
                Card(
                        colors =
                                CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.errorContainer
                                ),
                        modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                            text = it,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(16.dp)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            configuredUrl?.let { url ->
                Text(
                        text = "URL configurée : $url",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = onUseConfiguredUrl, modifier = Modifier.fillMaxWidth()) {
                    Text("Ouvrir l'URL configurée")
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Titre
            Text(
                    text =
                            if (isDiscovering) "Recherche de serveurs..."
                            else "Serveurs disponibles",
                    style = MaterialTheme.typography.titleLarge
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Indicateur de chargement
            if (isDiscovering) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Liste des serveurs
            if (discoveredServers.isEmpty() && !isDiscovering) {
                Text(
                        text =
                                "Aucun serveur trouvé.\nVérifiez que le serveur est démarré sur votre réseau local.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                discoveredServers.forEach { server ->
                    val isPaired = pairedServers.contains(server.serverId)
                    ServerCard(
                            server = server,
                            isPaired = isPaired,
                            onClick = { onServerSelected(server) }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun ServerCard(server: DiscoveredServer, isPaired: Boolean, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = server.serverName, style = MaterialTheme.typography.titleMedium)
                Text(
                        text = "${server.host}:${server.port}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (isPaired) {
                Badge { Text("Appairé") }
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun ServerWebView(
    serverUrl: String,
    context: Context,
    onError: (String) -> Unit,
    onWebViewReady: (WebView) -> Unit,
    onPageLoaded: () -> Unit,
    modifier: Modifier = Modifier
) {
    val webView = remember {
        WebView(context).apply {
            clearCache(true)

            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                cacheMode = WebSettings.LOAD_NO_CACHE
                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                setSupportZoom(false)
                builtInZoomControls = false
                displayZoomControls = false
            }

            webViewClient =
                    object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            AppLogger.i("ServerWebView", "Page loaded: $url")
                            onPageLoaded()
                        }

                        override fun onReceivedError(
                                view: WebView?,
                                errorCode: Int,
                                description: String?,
                                failingUrl: String?
                        ) {
                            super.onReceivedError(view, errorCode, description, failingUrl)
                            AppLogger.e(
                                    "ServerWebView",
                                    "Error loading $failingUrl: $description (code: $errorCode)"
                            )
                            if (failingUrl == serverUrl) {
                                onPageLoaded()
                                onError("Impossible de se connecter au serveur: $description")
                            }
                        }
                    }

            webChromeClient =
                    object : WebChromeClient() {
                        override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                            consoleMessage?.let { msg ->
                                val level = msg.messageLevel()
                                val formatted = "[${level.name}] ${msg.message()}"
                                when (level) {
                                    ConsoleMessage.MessageLevel.ERROR ->
                                            AppLogger.e("WebDevTools", formatted)
                                    ConsoleMessage.MessageLevel.WARNING ->
                                            AppLogger.w("WebDevTools", formatted)
                                    else -> AppLogger.d("WebDevTools", formatted)
                                }
                            }
                            return true
                        }
                    }
        }
    }

    LaunchedEffect(webView) { onWebViewReady(webView) }

    DisposableEffect(serverUrl) {
        AppLogger.i("ServerWebView", "Loading server URL: $serverUrl")
        webView.loadUrl(serverUrl)
        onDispose {
            onPageLoaded()
            webView.destroy()
        }
    }

    AndroidView(factory = { webView }, modifier = modifier.fillMaxSize())
}
