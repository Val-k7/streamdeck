package com.androidcontroldeck.ui.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.androidcontroldeck.R

/**
 * Gestionnaire de notifications pour l'application
 */
class NotificationManager(private val context: Context) {
    private val notificationManager = NotificationManagerCompat.from(context)

    companion object {
        private const val CHANNEL_ID_CONNECTION = "connection_status"
        private const val CHANNEL_ID_ACTIONS = "actions"
        private const val CHANNEL_ID_PROFILES = "profiles"
        private const val CHANNEL_ID_GENERAL = "general"

        private const val NOTIFICATION_ID_CONNECTION = 1
        private const val NOTIFICATION_ID_ACTION = 1000
        private const val NOTIFICATION_ID_PROFILE = 2000
    }

    init {
        createNotificationChannels()
    }

    /**
     * Crée les canaux de notification (Android 8.0+)
     */
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channels = listOf(
                NotificationChannel(
                    CHANNEL_ID_CONNECTION,
                    "Statut de connexion",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Notifications sur l'état de la connexion au serveur"
                },
                NotificationChannel(
                    CHANNEL_ID_ACTIONS,
                    "Actions",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Notifications sur les actions exécutées"
                },
                NotificationChannel(
                    CHANNEL_ID_PROFILES,
                    "Profils",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Notifications sur les profils"
                },
                NotificationChannel(
                    CHANNEL_ID_GENERAL,
                    "Général",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Notifications générales"
                }
            )

            val systemNotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            channels.forEach { systemNotificationManager.createNotificationChannel(it) }
        }
    }

    /**
     * Affiche une notification de statut de connexion
     */
    fun showConnectionStatus(connected: Boolean, serverIp: String? = null) {
        val title = if (connected) "Connecté au serveur" else "Déconnecté du serveur"
        val text = if (connected && serverIp != null) {
            "Connecté à $serverIp"
        } else if (!connected) {
            "En attente de connexion..."
        } else {
            "Statut de connexion"
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_CONNECTION)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(connected)
            .setAutoCancel(!connected)
            .build()

        notificationManager.notify(NOTIFICATION_ID_CONNECTION, notification)
    }

    /**
     * Affiche une notification d'action réussie
     */
    fun showActionSuccess(actionName: String, details: String? = null) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID_ACTIONS)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Action exécutée")
            .setContentText("$actionName${if (details != null) ": $details" else ""}")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(
            NOTIFICATION_ID_ACTION + System.currentTimeMillis().toInt() % 1000,
            notification
        )
    }

    /**
     * Affiche une notification d'erreur d'action
     */
    fun showActionError(actionName: String, errorMessage: String) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID_ACTIONS)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("Erreur d'action")
            .setContentText("$actionName: $errorMessage")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(
            NOTIFICATION_ID_ACTION + System.currentTimeMillis().toInt() % 1000,
            notification
        )
    }

    /**
     * Affiche une notification de profil
     */
    fun showProfileNotification(
        title: String,
        message: String,
        type: ProfileNotificationType = ProfileNotificationType.INFO
    ) {
        val icon = when (type) {
            ProfileNotificationType.SUCCESS -> android.R.drawable.ic_dialog_info
            ProfileNotificationType.ERROR -> android.R.drawable.ic_dialog_alert
            ProfileNotificationType.INFO -> android.R.drawable.ic_dialog_info
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_PROFILES)
            .setSmallIcon(icon)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(
            NOTIFICATION_ID_PROFILE + System.currentTimeMillis().toInt() % 1000,
            notification
        )
    }

    /**
     * Affiche une notification générale
     */
    fun showGeneralNotification(
        title: String,
        message: String,
        priority: Int = NotificationCompat.PRIORITY_DEFAULT
    ) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID_GENERAL)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(priority)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(
            System.currentTimeMillis().toInt() % 10000,
            notification
        )
    }

    /**
     * Supprime la notification de connexion
     */
    fun dismissConnectionNotification() {
        notificationManager.cancel(NOTIFICATION_ID_CONNECTION)
    }

    /**
     * Supprime toutes les notifications
     */
    fun dismissAll() {
        notificationManager.cancelAll()
    }
}

enum class ProfileNotificationType {
    SUCCESS,
    ERROR,
    INFO
}





