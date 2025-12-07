package com.androidcontroldeck.ui.components

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.webkit.ConsoleMessage
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import androidx.webkit.WebViewAssetLoader
import com.androidcontroldeck.AppContainer
import com.androidcontroldeck.BuildConfig
import com.androidcontroldeck.data.model.Profile
import com.androidcontroldeck.logging.AppLogger
import com.androidcontroldeck.network.ConnectionState
import com.androidcontroldeck.ui.navigation.Destination
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.json.JSONArray
import org.json.JSONObject

/** Interface JavaScript pour la communication entre l'UI React et Android */
class AndroidBridge(
        private val container: AppContainer,
        private val scope: CoroutineScope,
        private val navController: NavController?
) {
    private val json = Json { ignoreUnknownKeys = true }

    @JavascriptInterface
    fun getConnectionSettings(): String {
        val settings = container.connectionManager.settings.value
        return JSONObject()
                .apply {
                    put("serverIp", settings.serverIp)
                    put("serverPort", settings.serverPort)
                    put("useTls", settings.useTls)
                    put("token", container.securePreferences.getAuthToken() ?: "")
                }
                .toString()
    }

    @JavascriptInterface
    fun saveConnectionSettings(settingsJson: String) {
        scope.launch(Dispatchers.IO) {
            try {
                val json = JSONObject(settingsJson)
                container.preferences.update { current ->
                    current.copy(
                            serverIp = json.optString("serverIp", current.serverIp),
                            serverPort = json.optInt("serverPort", current.serverPort),
                            useTls = json.optBoolean("useTls", current.useTls)
                    )
                }
            } catch (e: Exception) {
                AppLogger.e("AndroidBridge", "Error saving settings", e)
            }
        }
    }

    @JavascriptInterface
    fun getConnectionStatus(): String {
        val state = container.connectionManager.state.value
        return JSONObject()
                .apply {
                    put(
                            "status",
                            when (state) {
                                is ConnectionState.Connected -> "online"
                                is ConnectionState.Connecting -> "connecting"
                                is ConnectionState.Disconnected -> "offline"
                            }
                    )
                    put("error", if (state is ConnectionState.Disconnected) state.reason else null)
                }
                .toString()
    }

    @JavascriptInterface
    fun connect(serverIp: String, serverPort: Int, useTls: Boolean) {
        scope.launch(Dispatchers.IO) {
            AppLogger.d(
                    "AndroidBridge",
                    "connect(serverIp=$serverIp, port=$serverPort, tls=$useTls)"
            )
            if (serverIp.isBlank()) {
                AppLogger.w("WebViewScreen", "Cannot connect: server IP is empty")
                return@launch
            }

            // Autoriser les IP ou hostnames publics/privés pour le debug distant
            container.preferences.update { current ->
                current.copy(serverIp = serverIp.trim(), serverPort = serverPort, useTls = useTls)
            }
            container.connectionManager.connect()
        }
    }

    @JavascriptInterface
    fun disconnect() {
        scope.launch(Dispatchers.IO) {
            AppLogger.d("AndroidBridge", "disconnect()")
            container.connectionManager.disconnect()
        }
    }

    @JavascriptInterface
    fun getProfiles(): String {
        AppLogger.d("AndroidBridge", "getProfiles() called")
        val profiles = container.profileRepository.profiles.value
        val uniqueProfiles = profiles.distinctBy { it.id }
        return JSONObject()
                .apply {
                    put(
                            "profiles",
                            JSONArray().apply {
                                uniqueProfiles.forEach { profile ->
                                    put(
                                            JSONObject().apply {
                                                put("id", profile.id)
                                                put("name", profile.name)
                                                put("version", profile.version)
                                            }
                                    )
                                }
                            }
                    )
                }
                .toString()
    }

    @JavascriptInterface
    fun getProfile(profileId: String): String {
        AppLogger.d("AndroidBridge", "getProfile($profileId)")
        val profile = container.profileRepository.profiles.value.find { it.id == profileId }
        return if (profile != null) {
            serializeProfile(profile)
        } else {
            JSONObject().apply { put("error", "Profile not found") }.toString()
        }
    }

    @JavascriptInterface
    fun selectProfile(profileId: String) {
        scope.launch(Dispatchers.IO) {
            AppLogger.d("AndroidBridge", "selectProfile($profileId)")
            val profile = container.profileRepository.profiles.value.find { it.id == profileId }
            if (profile != null) {
                container.profileRepository.selectProfile(profileId)
            }
        }
    }

    @JavascriptInterface
    fun sendControl(controlId: String, value: Double, metaJson: String?) {
        scope.launch(Dispatchers.IO) {
            AppLogger.d(
                    "AndroidBridge",
                    "sendControl(controlId=$controlId, value=$value, metaJson=$metaJson)"
            )
            val meta =
                    metaJson?.let {
                        try {
                            JSONObject(it).let { json ->
                                json.keys().asSequence().associateWith { key ->
                                    json.getString(key)
                                }
                            }
                        } catch (e: Exception) {
                            emptyMap()
                        }
                    }
                            ?: emptyMap()
            container.controlEventSender.send(controlId, "BUTTON", value.toFloat(), meta)
        }
    }

    @JavascriptInterface
    fun getDiscoveredServers(): String {
        AppLogger.d("AndroidBridge", "getDiscoveredServers()")
        val servers = container.serverDiscoveryManager.discoveredServers.value
        AppLogger.d("AndroidBridge", "getDiscoveredServers: returning ${servers.size} servers")
        return JSONObject()
                .apply {
                    put(
                            "servers",
                            JSONArray().apply {
                                servers.forEach { server ->
                                    put(
                                            JSONObject().apply {
                                                put("serverId", server.serverId)
                                                put("serverName", server.serverName)
                                                put("host", server.host)
                                                put("port", server.port)
                                                put("protocol", server.protocol)
                                                put("version", server.version)
                                            }
                                    )
                                }
                            }
                    )
                }
                .toString()
    }

    @JavascriptInterface
    fun isDiscovering(): Boolean {
        AppLogger.d("AndroidBridge", "isDiscovering()")
        return container.serverDiscoveryManager.isDiscovering.value
    }

    @JavascriptInterface
    fun startDiscovery() {
        scope.launch(Dispatchers.IO) {
            AppLogger.d("AndroidBridge", "startDiscovery()")
            container.serverDiscoveryManager.startDiscovery()
        }
    }

    @JavascriptInterface
    fun stopDiscovery() {
        scope.launch(Dispatchers.IO) {
            AppLogger.d("AndroidBridge", "stopDiscovery()")
            container.serverDiscoveryManager.stopDiscovery()
        }
    }

    @JavascriptInterface
    fun log(level: String, message: String, source: String? = null, line: Int = -1) {
        val tag = "WebDevTools"
        val formatted = buildString {
            append("[${level.uppercase()}]")
            source?.takeIf { it.isNotBlank() }?.let {
                append(" [").append(it).append(':').append(line).append(']')
            }
            append(' ').append(message)
        }
        when (level.lowercase()) {
            "error" -> AppLogger.e(tag, formatted)
            "warn", "warning" -> AppLogger.w(tag, formatted)
            "info" -> AppLogger.i(tag, formatted)
            "debug", "log" -> AppLogger.d(tag, formatted)
            else -> AppLogger.d(tag, formatted)
        }
    }

    @JavascriptInterface
    fun getPairedServers(): String {
        AppLogger.d("AndroidBridge", "getPairedServers()")
        val servers = runBlocking { container.pairedServersRepository.pairedServers.first() }
        AppLogger.d("AndroidBridge", "getPairedServers: returning ${servers.size} servers")
        return JSONObject()
                .apply {
                    put(
                            "servers",
                            JSONArray().apply {
                                servers.forEach { server ->
                                    put(
                                            JSONObject().apply {
                                                put("serverId", server.serverId)
                                                put("serverName", server.serverName)
                                                put("host", server.host)
                                                put("port", server.port)
                                                put("protocol", server.protocol)
                                                put("isAutoConnect", server.isAutoConnect)
                                                put("pairedAt", server.pairedAt)
                                                put("lastSeen", server.lastSeen)
                                            }
                                    )
                                }
                            }
                    )
                }
                .toString()
    }

    @JavascriptInterface
    fun requestPairingCode(serverId: String): String {
        AppLogger.d("AndroidBridge", "requestPairingCode(serverId=$serverId)")
        return runBlocking {
            try {
                val discoveredServers = container.serverDiscoveryManager.discoveredServers.value
                AppLogger.d(
                        "AndroidBridge",
                        "requestPairingCode: looking for serverId=$serverId in ${discoveredServers.size} servers"
                )
                val server = discoveredServers.firstOrNull { it.serverId == serverId }
                if (server == null) {
                    AppLogger.w("AndroidBridge", "requestPairingCode: server not found")
                    return@runBlocking JSONObject()
                            .apply { put("error", "Server not found") }
                            .toString()
                }
                AppLogger.d(
                        "AndroidBridge",
                        "requestPairingCode: requesting code for ${server.serverName}"
                )
                val response = container.pairingManager.requestPairingCode(server)
                if (response != null) {
                    AppLogger.d("AndroidBridge", "requestPairingCode: code received")
                    JSONObject()
                            .apply {
                                put("code", response.code)
                                put("expiresAt", response.expiresAt)
                            }
                            .toString()
                } else {
                    AppLogger.w("AndroidBridge", "requestPairingCode: failed to get code")
                    JSONObject().apply { put("error", "Failed to get pairing code") }.toString()
                }
            } catch (e: Exception) {
                AppLogger.e("AndroidBridge", "requestPairingCode: error", e)
                JSONObject().apply { put("error", e.message ?: "Unknown error") }.toString()
            }
        }
    }

    @JavascriptInterface
    fun confirmPairing(serverId: String, code: String): String {
        AppLogger.d("AndroidBridge", "confirmPairing(serverId=$serverId, code=$code)")
        return runBlocking {
            try {
                val discoveredServers = container.serverDiscoveryManager.discoveredServers.value
                AppLogger.d(
                        "AndroidBridge",
                        "confirmPairing: looking for serverId=$serverId in ${discoveredServers.size} servers"
                )
                val server = discoveredServers.firstOrNull { it.serverId == serverId }
                if (server == null) {
                    AppLogger.w("AndroidBridge", "confirmPairing: server not found")
                    return@runBlocking JSONObject()
                            .apply { put("error", "Server not found") }
                            .toString()
                }
                AppLogger.d(
                        "AndroidBridge",
                        "confirmPairing: confirming pairing for ${server.serverName} with code=$code"
                )
                val pairedServer = container.pairingManager.confirmPairing(server, code)
                if (pairedServer != null) {
                    AppLogger.d(
                            "AndroidBridge",
                            "confirmPairing: pairing successful, saving server"
                    )
                    container.pairedServersRepository.addOrUpdateServer(pairedServer)
                    JSONObject()
                            .apply {
                                put("success", true)
                                put("serverId", pairedServer.serverId)
                                put("serverName", pairedServer.serverName)
                            }
                            .toString()
                } else {
                    AppLogger.w("AndroidBridge", "confirmPairing: pairing failed")
                    JSONObject().apply { put("error", "Pairing failed") }.toString()
                }
            } catch (e: Exception) {
                AppLogger.e("AndroidBridge", "confirmPairing: error", e)
                JSONObject().apply { put("error", e.message ?: "Unknown error") }.toString()
            }
        }
    }

    @JavascriptInterface
    fun connectToPairedServer(serverId: String) {
        scope.launch(Dispatchers.IO) {
            AppLogger.d("AndroidBridge", "connectToPairedServer(serverId=$serverId)")
            val pairedServers = runBlocking {
                container.pairedServersRepository.pairedServers.first()
            }
            AppLogger.d(
                    "AndroidBridge",
                    "connectToPairedServer: looking for serverId=$serverId in ${pairedServers.size} servers"
            )
            val server = pairedServers.firstOrNull { it.serverId == serverId }
            if (server != null) {
                AppLogger.d(
                        "AndroidBridge",
                        "connectToPairedServer: connecting to ${server.serverName}"
                )
                container.connectionManager.connectToPairedServer(server)
            } else {
                AppLogger.w("AndroidBridge", "connectToPairedServer: server not found")
            }
        }
    }

    @JavascriptInterface
    fun openSettings() {
        AppLogger.d(
                "AndroidBridge",
                "openSettings() called, navController=${navController != null}"
        )
        scope.launch(Dispatchers.Main) {
            try {
                if (navController == null) {
                    AppLogger.e(
                            "AndroidBridge",
                            "openSettings(): navController is null, cannot navigate"
                    )
                    return@launch
                }
                AppLogger.d("AndroidBridge", "openSettings(): navigating to Settings route")
                navController.navigate(Destination.Settings.route) {
                    popUpTo(Destination.WebView.route) { inclusive = false }
                }
                AppLogger.d("AndroidBridge", "openSettings(): navigation completed")
            } catch (e: Exception) {
                AppLogger.e("AndroidBridge", "openSettings(): navigation failed", e)
            }
        }
    }

    private fun serializeProfile(profile: Profile): String {
        return JSONObject()
                .apply {
                    put("id", profile.id)
                    put("name", profile.name)
                    put("rows", profile.rows)
                    put("cols", profile.cols)
                    put("version", profile.version)
                    put(
                            "controls",
                            JSONArray().apply {
                                profile.controls.forEach { control ->
                                    put(
                                            JSONObject().apply {
                                                put("id", control.id)
                                                put("type", control.type.name)
                                                put("row", control.row)
                                                put("col", control.col)
                                                put("rowSpan", control.rowSpan)
                                                put("colSpan", control.colSpan)
                                                put("label", control.label ?: "")
                                                put("colorHex", control.colorHex ?: "")
                                                control.action?.let { action ->
                                                    put(
                                                            "action",
                                                            JSONObject().apply {
                                                                put("type", action.type.name)
                                                                put("payload", action.payload ?: "")
                                                            }
                                                    )
                                                }
                                            }
                                    )
                                }
                            }
                    )
                }
                .toString()
    }
}

private const val DEVTOOLS_LOG_TAG = "WebDevTools"

private fun logDevToolsMessage(message: ConsoleMessage) {
    val level = message.messageLevel()
    val rawSource = message.sourceId().orEmpty()
    val source = rawSource.takeIf { it.isNotBlank() }?.substringAfterLast('/') ?: "inline"
    val formatted = buildString {
        append("[${level.name}] [$source:${message.lineNumber()}] ${message.message()}")
        if (rawSource.isNotBlank() && rawSource != source) {
            append(" | source=").append(rawSource)
        }
    }
    when (level) {
        ConsoleMessage.MessageLevel.ERROR -> AppLogger.e(DEVTOOLS_LOG_TAG, formatted)
        ConsoleMessage.MessageLevel.WARNING -> AppLogger.w(DEVTOOLS_LOG_TAG, formatted)
        ConsoleMessage.MessageLevel.LOG -> AppLogger.d(DEVTOOLS_LOG_TAG, formatted)
        ConsoleMessage.MessageLevel.TIP -> AppLogger.d(DEVTOOLS_LOG_TAG, formatted)
        ConsoleMessage.MessageLevel.DEBUG -> AppLogger.d(DEVTOOLS_LOG_TAG, formatted)
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebViewScreen(
        container: AppContainer,
        navController: NavController? = null,
        modifier: Modifier = Modifier
) {
    // Active le débogage WebView en debug pour chrome://inspect
    LaunchedEffect(Unit) {
        if (BuildConfig.DEBUG) {
            WebView.setWebContentsDebuggingEnabled(true)
        }
    }

    val context = LocalContext.current
    val assetLoader = remember {
        // Custom path handler qui gère tous les chemins depuis web/
        val webAssetsHandler =
                object : WebViewAssetLoader.PathHandler {
                    override fun handle(path: String): android.webkit.WebResourceResponse? {
                        return try {
                            val cleanPath = path.removePrefix("/")
                            val filePath =
                                    when {
                                        cleanPath.isEmpty() -> "web/index.html"
                                        cleanPath == "index.html" -> "web/index.html"
                                        else -> "web/$cleanPath"
                                    }
                            AppLogger.d(
                                    "WebViewAssetLoader",
                                    "Loading: $filePath (request path: $path)"
                            )
                            val inputStream = context.assets.open(filePath)
                            val mimeType =
                                    when {
                                        filePath.endsWith(".js") -> "application/javascript"
                                        filePath.endsWith(".css") -> "text/css"
                                        filePath.endsWith(".html") -> "text/html"
                                        filePath.endsWith(".png") -> "image/png"
                                        filePath.endsWith(".jpg") || filePath.endsWith(".jpeg") ->
                                                "image/jpeg"
                                        filePath.endsWith(".svg") -> "image/svg+xml"
                                        filePath.endsWith(".woff") -> "font/woff"
                                        filePath.endsWith(".woff2") -> "font/woff2"
                                        filePath.endsWith(".ttf") -> "font/ttf"
                                        filePath.endsWith(".ico") -> "image/x-icon"
                                        filePath.endsWith(".json") -> "application/json"
                                        filePath.endsWith(".txt") -> "text/plain"
                                        else -> "application/octet-stream"
                                    }
                            AppLogger.d(
                                    "WebViewAssetLoader",
                                    "✓ Loaded: $filePath (MIME: $mimeType)"
                            )
                            android.webkit.WebResourceResponse(mimeType, "UTF-8", inputStream)
                        } catch (e: Exception) {
                            AppLogger.e("WebViewAssetLoader", "Failed to load: web/$path", e)
                            null
                        }
                    }
                }

        WebViewAssetLoader.Builder()
                .setDomain("appassets.androidplatform.net")
                .setHttpAllowed(false)
                .addPathHandler("/", webAssetsHandler)
                .build()
    }

    val webView = remember {
        WebView(context).apply {
            // Clear cache to ensure fresh load during development
            clearCache(true)

            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                allowFileAccess = true
                allowContentAccess = true
                // Force no cache to debug CSS loading issue
                cacheMode = WebSettings.LOAD_NO_CACHE
                // Autoriser complètement le mixed content (HTTP/WS) depuis appassets (HTTPS) pour
                // les usages LAN/debug
                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                setSupportZoom(false)
                builtInZoomControls = false
                displayZoomControls = false
            }

            val bridge =
                    AndroidBridge(
                            container,
                            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main),
                            navController
                    )
            addJavascriptInterface(bridge, "Android")

            webViewClient =
                    object : WebViewClient() {
                        override fun shouldInterceptRequest(
                                view: WebView?,
                                request: WebResourceRequest?
                        ): android.webkit.WebResourceResponse? {
                            val uri = request?.url
                            if (uri != null) {
                                AppLogger.d(
                                        "WebViewAssetLoader",
                                        "Intercepting request: ${uri.toString()}"
                                )
                                val response = assetLoader.shouldInterceptRequest(uri)
                                if (response != null) {
                                    AppLogger.d(
                                            "WebViewAssetLoader",
                                            "✓ AssetLoader handled request: ${uri.toString()}"
                                    )
                                } else {
                                    // Diagnostic: est-ce un fichier CSS/SVG non trouvé?
                                    if (uri.toString().endsWith(".css")) {
                                        AppLogger.e(
                                                "WebViewAssetLoader",
                                                "❌ CSS NOT FOUND: ${uri.toString()} - Styling will be missing!"
                                        )
                                    } else if (uri.toString().endsWith(".svg")) {
                                        AppLogger.w(
                                                "WebViewAssetLoader",
                                                "⚠️ SVG NOT FOUND: ${uri.toString()} - Icon may not render"
                                        )
                                    } else {
                                        AppLogger.w(
                                                "WebViewAssetLoader",
                                                "⚠️ AssetLoader did not handle request: ${uri.toString()}"
                                        )
                                    }
                                }
                                return response ?: super.shouldInterceptRequest(view, request)
                            } else {
                                return super.shouldInterceptRequest(view, request)
                            }
                        }

                        override fun shouldOverrideUrlLoading(
                                view: WebView?,
                                request: WebResourceRequest?
                        ): Boolean {
                            return false
                        }

                        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                            super.onPageStarted(view, url, favicon)
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            AppLogger.i(
                                    "WebViewScreen",
                                    "✓ Page finished loading from: $url, Android bridge should be available"
                            )
                            // Log pour vérifier que les styles sont appliqués
                            view?.evaluateJavascript(
                                    """
                        (function() {
                            const styleSheets = document.styleSheets;
                            const styles = Array.from(styleSheets).map(ss => ss.href || 'inline').join(', ');
                            console.log('DEBUG: Loaded stylesheets: ' + (styles || 'NONE'));
                            // Log the actual HEAD content to diagnose
                            console.log('DEBUG: HEAD innerHTML: ' + document.head.innerHTML.substring(0, 500));
                            // Log all link tags
                            const links = document.querySelectorAll('link');
                            console.log('DEBUG: Found ' + links.length + ' link tags');
                            links.forEach((link, i) => {
                                console.log('DEBUG: Link ' + i + ': rel=' + link.rel + ' href=' + link.href);
                            });
                        })();
                    """.trimIndent()
                            ) { result ->
                                AppLogger.d(
                                        "WebViewScreen",
                                        "Stylesheet verification result: $result"
                                )
                            }
                            view?.evaluateJavascript(
                                    """
                        (function() {
                            if (typeof window === 'undefined') return;
                            if (window.__deckConsolePatched) {
                                window.dispatchEvent(new Event('android-bridge-ready'));
                                return;
                            }
                            window.__deckConsolePatched = true;
                            const original = {
                                log: console.log,
                                warn: console.warn,
                                error: console.error,
                                info: console.info,
                                debug: console.debug
                            };
                            const send = function(level, msg, source, line) {
                                try {
                                    if (window.Android && typeof window.Android.log === 'function') {
                                        window.Android.log(level, String(msg), source || '', line || -1);
                                    }
                                } catch (e) {}
                            };
                            ['log','warn','error','info','debug'].forEach(function(level) {
                                const fn = original[level] || console.log;
                                console[level] = function() {
                                    try {
                                        var parts = [];
                                        for (var i=0;i<arguments.length;i++) {
                                            var a = arguments[i];
                                            if (typeof a === 'object') {
                                                try { parts.push(JSON.stringify(a)); }
                                                catch (_) { parts.push(String(a)); }
                                            } else {
                                                parts.push(String(a));
                                            }
                                        }
                                        send(level, parts.join(' '), '', -1);
                                    } catch (_) {}
                                    return fn.apply(console, arguments);
                                };
                            });
                            window.addEventListener('error', function(event) {
                                try {
                                    var src = event.filename || '';
                                    var msg = event.message || 'JS error';
                                    var line = event.lineno || -1;
                                    if (event.error && event.error.stack) {
                                        msg += ' | ' + event.error.stack;
                                    }
                                    send('error', msg, src, line);
                                } catch (_) {}
                            });
                            window.addEventListener('unhandledrejection', function(event) {
                                try {
                                    var reason = event.reason;
                                    var msg = 'Unhandled promise rejection';
                                    if (reason) {
                                        if (reason.stack) msg += ': ' + reason.stack;
                                        else msg += ': ' + reason;
                                    }
                                    send('error', msg, '', -1);
                                } catch (_) {}
                            });
                            if (window.Android) {
                                console.log('Android bridge is ready');
                                window.dispatchEvent(new Event('android-bridge-ready'));
                            }
                        })();
                    """.trimIndent(),
                                    null
                            )
                        }
                    }

            webChromeClient =
                    object : WebChromeClient() {
                        override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                            consoleMessage?.let { logDevToolsMessage(it) }
                            return true
                        }
                    }
        }
    }

    DisposableEffect(Unit) {
        webView.loadUrl("https://appassets.androidplatform.net/index.html")
        onDispose { webView.destroy() }
    }

    AndroidView(factory = { webView }, modifier = modifier.fillMaxSize())
}
