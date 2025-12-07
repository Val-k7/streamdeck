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
import android.util.Log
import androidx.webkit.WebViewAssetLoader
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import com.androidcontroldeck.BuildConfig
import com.androidcontroldeck.AppContainer
import com.androidcontroldeck.data.model.Profile
import com.androidcontroldeck.network.ConnectionState
import com.androidcontroldeck.logging.AppLogger
import com.androidcontroldeck.ui.navigation.Destination
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import org.json.JSONArray
import org.json.JSONObject

/**
 * Interface JavaScript pour la communication entre l'UI React et Android
 */
class AndroidBridge(
    private val container: AppContainer,
    private val scope: CoroutineScope,
    private val navController: NavController?
) {
    private val json = Json { ignoreUnknownKeys = true }

    @JavascriptInterface
    fun getConnectionSettings(): String {
        val settings = container.connectionManager.settings.value
        return JSONObject().apply {
            put("serverIp", settings.serverIp)
            put("serverPort", settings.serverPort)
            put("useTls", settings.useTls)
            put("token", container.securePreferences.getAuthToken() ?: "")
        }.toString()
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
        return JSONObject().apply {
            put("status", when (state) {
                is ConnectionState.Connected -> "online"
                is ConnectionState.Connecting -> "connecting"
                is ConnectionState.Disconnected -> "offline"
            })
            put("error", if (state is ConnectionState.Disconnected) state.reason else null)
        }.toString()
    }

    @JavascriptInterface
    fun connect(serverIp: String, serverPort: Int, useTls: Boolean) {
        scope.launch(Dispatchers.IO) {
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
            container.connectionManager.disconnect()
        }
    }

    @JavascriptInterface
    fun getProfiles(): String {
        val profiles = container.profileRepository.profiles.value
        val uniqueProfiles = profiles.distinctBy { it.id }
        return JSONObject().apply {
            put("profiles", JSONArray().apply {
                uniqueProfiles.forEach { profile ->
                    put(JSONObject().apply {
                        put("id", profile.id)
                        put("name", profile.name)
                        put("version", profile.version)
                    })
                }
            })
        }.toString()
    }

    @JavascriptInterface
    fun getProfile(profileId: String): String {
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
            val profile = container.profileRepository.profiles.value.find { it.id == profileId }
            if (profile != null) {
                container.profileRepository.selectProfile(profileId)
            }
        }
    }

    @JavascriptInterface
    fun sendControl(controlId: String, value: Double, metaJson: String?) {
        scope.launch(Dispatchers.IO) {
            val meta = metaJson?.let {
                try {
                    JSONObject(it).let { json ->
                        json.keys().asSequence().associateWith { key -> json.getString(key) }
                    }
                } catch (e: Exception) {
                    emptyMap()
                }
            } ?: emptyMap()
            container.controlEventSender.send(controlId, "BUTTON", value.toFloat(), meta)
        }
    }

    @JavascriptInterface
    fun getDiscoveredServers(): String {
        val servers = container.serverDiscoveryManager.discoveredServers.value
        AppLogger.d("AndroidBridge", "getDiscoveredServers: returning ${servers.size} servers")
        return JSONObject().apply {
            put("servers", JSONArray().apply {
                servers.forEach { server ->
                    put(JSONObject().apply {
                        put("serverId", server.serverId)
                        put("serverName", server.serverName)
                        put("host", server.host)
                        put("port", server.port)
                        put("protocol", server.protocol)
                        put("version", server.version)
                    })
                }
            })
        }.toString()
    }

    @JavascriptInterface
    fun isDiscovering(): Boolean {
        return container.serverDiscoveryManager.isDiscovering.value
    }

    @JavascriptInterface
    fun startDiscovery() {
        scope.launch(Dispatchers.IO) {
            container.serverDiscoveryManager.startDiscovery()
        }
    }

    @JavascriptInterface
    fun stopDiscovery() {
        scope.launch(Dispatchers.IO) {
            container.serverDiscoveryManager.stopDiscovery()
        }
    }

    @JavascriptInterface
    fun getPairedServers(): String {
        val servers = runBlocking {
            container.pairedServersRepository.pairedServers.first()
        }
        AppLogger.d("AndroidBridge", "getPairedServers: returning ${servers.size} servers")
        return JSONObject().apply {
            put("servers", JSONArray().apply {
                servers.forEach { server ->
                    put(JSONObject().apply {
                        put("serverId", server.serverId)
                        put("serverName", server.serverName)
                        put("host", server.host)
                        put("port", server.port)
                        put("protocol", server.protocol)
                        put("isAutoConnect", server.isAutoConnect)
                        put("pairedAt", server.pairedAt)
                        put("lastSeen", server.lastSeen)
                    })
                }
            })
        }.toString()
    }

    @JavascriptInterface
    fun requestPairingCode(serverId: String): String {
        return runBlocking {
            try {
                val discoveredServers = container.serverDiscoveryManager.discoveredServers.value
                AppLogger.d("AndroidBridge", "requestPairingCode: looking for serverId=$serverId in ${discoveredServers.size} servers")
                val server = discoveredServers.firstOrNull { it.serverId == serverId }
                if (server == null) {
                    AppLogger.w("AndroidBridge", "requestPairingCode: server not found")
                    return@runBlocking JSONObject().apply {
                        put("error", "Server not found")
                    }.toString()
                }
                AppLogger.d("AndroidBridge", "requestPairingCode: requesting code for ${server.serverName}")
                val response = container.pairingManager.requestPairingCode(server)
                if (response != null) {
                    AppLogger.d("AndroidBridge", "requestPairingCode: code received")
                    JSONObject().apply {
                        put("code", response.code)
                        put("expiresAt", response.expiresAt)
                    }.toString()
                } else {
                    AppLogger.w("AndroidBridge", "requestPairingCode: failed to get code")
                    JSONObject().apply {
                        put("error", "Failed to get pairing code")
                    }.toString()
                }
            } catch (e: Exception) {
                AppLogger.e("AndroidBridge", "requestPairingCode: error", e)
                JSONObject().apply {
                    put("error", e.message ?: "Unknown error")
                }.toString()
            }
        }
    }

    @JavascriptInterface
    fun confirmPairing(serverId: String, code: String): String {
        return runBlocking {
            try {
                val discoveredServers = container.serverDiscoveryManager.discoveredServers.value
                AppLogger.d("AndroidBridge", "confirmPairing: looking for serverId=$serverId in ${discoveredServers.size} servers")
                val server = discoveredServers.firstOrNull { it.serverId == serverId }
                if (server == null) {
                    AppLogger.w("AndroidBridge", "confirmPairing: server not found")
                    return@runBlocking JSONObject().apply {
                        put("error", "Server not found")
                    }.toString()
                }
                AppLogger.d("AndroidBridge", "confirmPairing: confirming pairing for ${server.serverName} with code=$code")
                val pairedServer = container.pairingManager.confirmPairing(server, code)
                if (pairedServer != null) {
                    AppLogger.d("AndroidBridge", "confirmPairing: pairing successful, saving server")
                    container.pairedServersRepository.addOrUpdateServer(pairedServer)
                    JSONObject().apply {
                        put("success", true)
                        put("serverId", pairedServer.serverId)
                        put("serverName", pairedServer.serverName)
                    }.toString()
                } else {
                    AppLogger.w("AndroidBridge", "confirmPairing: pairing failed")
                    JSONObject().apply {
                        put("error", "Pairing failed")
                    }.toString()
                }
            } catch (e: Exception) {
                AppLogger.e("AndroidBridge", "confirmPairing: error", e)
                JSONObject().apply {
                    put("error", e.message ?: "Unknown error")
                }.toString()
            }
        }
    }

    @JavascriptInterface
    fun connectToPairedServer(serverId: String) {
        scope.launch(Dispatchers.IO) {
            val pairedServers = runBlocking {
                container.pairedServersRepository.pairedServers.first()
            }
            AppLogger.d("AndroidBridge", "connectToPairedServer: looking for serverId=$serverId in ${pairedServers.size} servers")
            val server = pairedServers.firstOrNull { it.serverId == serverId }
            if (server != null) {
                AppLogger.d("AndroidBridge", "connectToPairedServer: connecting to ${server.serverName}")
                container.connectionManager.connectToPairedServer(server)
            } else {
                AppLogger.w("AndroidBridge", "connectToPairedServer: server not found")
            }
        }
    }

    @JavascriptInterface
    fun openSettings() {
        scope.launch(Dispatchers.Main) {
            navController?.navigate(Destination.Settings.route) {
                popUpTo(Destination.WebView.route) { inclusive = false }
            }
        }
    }

    private fun serializeProfile(profile: Profile): String {
        return JSONObject().apply {
            put("id", profile.id)
            put("name", profile.name)
            put("rows", profile.rows)
            put("cols", profile.cols)
            put("version", profile.version)
            put("controls", JSONArray().apply {
                profile.controls.forEach { control ->
                    put(JSONObject().apply {
                        put("id", control.id)
                        put("type", control.type.name)
                        put("row", control.row)
                        put("col", control.col)
                        put("rowSpan", control.rowSpan)
                        put("colSpan", control.colSpan)
                        put("label", control.label ?: "")
                        put("colorHex", control.colorHex ?: "")
                        control.action?.let { action ->
                          put("action", JSONObject().apply {
                              put("type", action.type.name)
                              put("payload", action.payload ?: "")
                          })
                        }
                    })
                }
            })
        }.toString()
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
        WebViewAssetLoader.Builder()
            .setDomain("appassets.androidplatform.net")
            .addPathHandler("/assets/", object : WebViewAssetLoader.PathHandler {
                override fun handle(path: String): android.webkit.WebResourceResponse? {
                    return try {
                        val cleanPath = path.removePrefix("/").removePrefix("assets/")
                        val assetPath = "web/assets/$cleanPath"
                        AppLogger.d("WebViewAssetLoader", "Loading asset: $assetPath (original path: $path)")
                        val inputStream = context.assets.open(assetPath)
                        val mimeType = when {
                            cleanPath.endsWith(".js") -> "application/javascript"
                            cleanPath.endsWith(".css") -> "text/css"
                            cleanPath.endsWith(".html") -> "text/html"
                            cleanPath.endsWith(".png") -> "image/png"
                            cleanPath.endsWith(".jpg") || cleanPath.endsWith(".jpeg") -> "image/jpeg"
                            cleanPath.endsWith(".svg") -> "image/svg+xml"
                            else -> "application/octet-stream"
                        }
                        AppLogger.d("WebViewAssetLoader", "Asset loaded successfully: $assetPath (MIME: $mimeType)")
                        android.webkit.WebResourceResponse(mimeType, "UTF-8", inputStream)
                    } catch (e: Exception) {
                        AppLogger.e("WebViewAssetLoader", "Failed to load asset: web/assets/$path", e)
                        try {
                            val assets = context.assets.list("web/assets")
                            AppLogger.d("WebViewAssetLoader", "Available assets in web/assets/: ${assets?.joinToString(", ")}")
                        } catch (listError: Exception) {
                            AppLogger.e("WebViewAssetLoader", "Failed to list assets", listError)
                        }
                        null
                    }
                }
            })
            .addPathHandler("/", object : WebViewAssetLoader.PathHandler {
                override fun handle(path: String): android.webkit.WebResourceResponse? {
                    return try {
                        val cleanPath = path.removePrefix("/")
                        val filePath = if (cleanPath.isEmpty() || cleanPath == "index.html") "web/index.html" else "web/$cleanPath"
                        val inputStream = context.assets.open(filePath)
                        val mimeType = when {
                            path.endsWith(".js") -> "application/javascript"
                            path.endsWith(".css") -> "text/css"
                            path.endsWith(".html") -> "text/html"
                            path.endsWith(".png") -> "image/png"
                            path.endsWith(".jpg") || path.endsWith(".jpeg") -> "image/jpeg"
                            path.endsWith(".svg") -> "image/svg+xml"
                            else -> "text/html"
                        }
                        android.webkit.WebResourceResponse(mimeType, "UTF-8", inputStream)
                    } catch (e: Exception) {
                        AppLogger.e("WebViewAssetLoader", "Failed to load asset: web/$path", e)
                        null
                    }
                }
            })
            .build()
    }

    val webView = remember {
        WebView(context).apply {
          settings.apply {
              javaScriptEnabled = true
              domStorageEnabled = true
              allowFileAccess = true
              allowContentAccess = true
              // Autoriser complètement le mixed content (HTTP/WS) depuis appassets (HTTPS) pour les usages LAN/debug
              mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
              setSupportZoom(false)
              builtInZoomControls = false
              displayZoomControls = false
          }

          val bridge = AndroidBridge(container, kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main), navController)
          addJavascriptInterface(bridge, "Android")

          webViewClient = object : WebViewClient() {
              override fun shouldInterceptRequest(
                  view: WebView?,
                  request: WebResourceRequest?
              ): android.webkit.WebResourceResponse? {
                  val uri = request?.url
                  if (uri != null) {
                      AppLogger.d("WebViewAssetLoader", "Intercepting request: ${uri.toString()}")
                      val response = assetLoader.shouldInterceptRequest(uri)
                      if (response != null) {
                          AppLogger.d("WebViewAssetLoader", "AssetLoader handled request: ${uri.toString()}")
                      } else {
                          AppLogger.w("WebViewAssetLoader", "AssetLoader did not handle request: ${uri.toString()}")
                      }
                      return response ?: super.shouldInterceptRequest(view, request)
                  } else {
                      return super.shouldInterceptRequest(view, request)
                  }
              }

              override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                  return false
              }

              override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                  super.onPageStarted(view, url, favicon)
              }

              override fun onPageFinished(view: WebView?, url: String?) {
                  super.onPageFinished(view, url)
                  AppLogger.d("WebViewScreen", "Page finished loading, Android bridge should be available")
                  view?.evaluateJavascript("""
                        if (typeof window !== 'undefined' && window.Android) {
                            console.log('Android bridge is ready');
                            window.dispatchEvent(new Event('android-bridge-ready'));
                        }
                    """.trimIndent(), null)
              }
          }

          webChromeClient = object : WebChromeClient() {
              override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                  consoleMessage?.let {
                      val tag = "WebViewConsole"
                      val msg = "[${it.sourceId()}:${it.lineNumber()}] ${it.message()}"
                      when (it.messageLevel()) {
                          ConsoleMessage.MessageLevel.ERROR -> Log.e(tag, msg)
                          ConsoleMessage.MessageLevel.WARNING -> Log.w(tag, msg)
                          ConsoleMessage.MessageLevel.LOG,
                          ConsoleMessage.MessageLevel.TIP,
                          ConsoleMessage.MessageLevel.DEBUG -> Log.d(tag, msg)
                      }
                  }
                  return super.onConsoleMessage(consoleMessage)
              }
          }
        }
    }

    DisposableEffect(Unit) {
        webView.loadUrl("https://appassets.androidplatform.net/index.html")
        onDispose {
            webView.destroy()
        }
    }

    AndroidView(
        factory = { webView },
        modifier = modifier.fillMaxSize()
    )
}
