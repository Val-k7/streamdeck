package com.androidcontroldeck.ui.utils

import com.androidcontroldeck.logging.AppLogger
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Gestionnaire d'erreurs centralisé pour l'application Android
 */
class ErrorHandler(
    private val scope: CoroutineScope,
    private val snackbarHostState: SnackbarHostState? = null
) {
    private val errorHistory = mutableListOf<ErrorInfo>()
    private val maxHistorySize = 50

    /**
     * Gère une erreur et affiche un message à l'utilisateur
     */
    fun handleError(
        error: Throwable,
        context: ErrorContext = ErrorContext(),
        showSnackbar: Boolean = true
    ) {
        val errorInfo = ErrorInfo(
            type = classifyError(error),
            message = error.message ?: "Erreur inconnue",
            stackTrace = error.stackTraceToString(),
            context = context,
            timestamp = System.currentTimeMillis()
        )

        // Ajouter à l'historique
        addToHistory(errorInfo)

        // Logger
        logError(error, errorInfo)

        // Afficher un snackbar si demandé
        if (showSnackbar && snackbarHostState != null) {
            scope.launch {
                snackbarHostState.showSnackbar(
                    message = getUserFriendlyMessage(errorInfo),
                    duration = androidx.compose.material3.SnackbarDuration.Short
                )
            }
        }
    }

    /**
     * Classe une erreur selon son type
     */
    private fun classifyError(error: Throwable): ErrorType {
        return when {
            error is java.net.UnknownHostException -> ErrorType.NETWORK
            error is java.net.SocketTimeoutException -> ErrorType.TIMEOUT
            error is java.io.IOException -> ErrorType.IO
            error.message?.contains("authentication", ignoreCase = true) == true -> ErrorType.AUTHENTICATION
            error.message?.contains("permission", ignoreCase = true) == true -> ErrorType.PERMISSION
            error.message?.contains("not found", ignoreCase = true) == true -> ErrorType.NOT_FOUND
            error.message?.contains("validation", ignoreCase = true) == true -> ErrorType.VALIDATION
            else -> ErrorType.UNKNOWN
        }
    }

    /**
     * Ajoute une erreur à l'historique
     */
    private fun addToHistory(errorInfo: ErrorInfo) {
        errorHistory.add(errorInfo)
        if (errorHistory.size > maxHistorySize) {
            errorHistory.removeAt(0)
        }
    }

    /**
     * Log une erreur
     */
    private fun logError(error: Throwable, errorInfo: ErrorInfo) {
        when (errorInfo.type) {
            ErrorType.NETWORK -> AppLogger.w(TAG, "Network error: ${errorInfo.message}", error)
            ErrorType.TIMEOUT -> AppLogger.w(TAG, "Timeout error: ${errorInfo.message}", error)
            ErrorType.AUTHENTICATION -> AppLogger.w(TAG, "Auth error: ${errorInfo.message}", error)
            ErrorType.PERMISSION -> AppLogger.w(TAG, "Permission error: ${errorInfo.message}", error)
            else -> AppLogger.e(TAG, "Error: ${errorInfo.message}", error)
        }
    }

    /**
     * Retourne un message utilisateur-friendly
     */
    private fun getUserFriendlyMessage(errorInfo: ErrorInfo): String {
        return when (errorInfo.type) {
            ErrorType.NETWORK -> "Erreur de connexion réseau"
            ErrorType.TIMEOUT -> "La requête a expiré"
            ErrorType.IO -> "Erreur de lecture/écriture"
            ErrorType.AUTHENTICATION -> "Authentification requise"
            ErrorType.PERMISSION -> "Permissions insuffisantes"
            ErrorType.NOT_FOUND -> "Ressource non trouvée"
            ErrorType.VALIDATION -> "Données invalides"
            ErrorType.UNKNOWN -> "Une erreur inattendue s'est produite"
        }
    }

    /**
     * Obtient l'historique des erreurs
     */
    fun getErrorHistory(): List<ErrorInfo> {
        return errorHistory.toList()
    }

    /**
     * Réinitialise l'historique
     */
    fun clearHistory() {
        errorHistory.clear()
    }

    companion object {
        private const val TAG = "ErrorHandler"
    }
}

/**
 * Type d'erreur
 */
enum class ErrorType {
    NETWORK,
    TIMEOUT,
    IO,
    AUTHENTICATION,
    PERMISSION,
    NOT_FOUND,
    VALIDATION,
    UNKNOWN
}

/**
 * Informations sur une erreur
 */
data class ErrorInfo(
    val type: ErrorType,
    val message: String,
    val stackTrace: String,
    val context: ErrorContext,
    val timestamp: Long
)

/**
 * Contexte d'une erreur
 */
data class ErrorContext(
    val screen: String? = null,
    val action: String? = null,
    val userId: String? = null,
    val additionalData: Map<String, Any> = emptyMap()
)

/**
 * Composable pour obtenir un ErrorHandler
 */
@Composable
fun rememberErrorHandler(
    scope: CoroutineScope = rememberCoroutineScope(),
    snackbarHostState: SnackbarHostState? = null
): ErrorHandler {
    return remember {
        ErrorHandler(scope, snackbarHostState)
    }
}


