package com.androidcontroldeck.ui.utils

import android.util.Patterns
import java.net.InetAddress
import java.util.regex.Pattern

/**
 * Validateur de configuration pour l'application Android
 */
object ConfigValidator {
    /**
     * Valide une adresse IP réseau locale
     * Rejette localhost et 127.0.0.1, n'accepte que les adresses réseau locales
     */
    fun validateIp(ip: String): ValidationResult {
        if (ip.isBlank()) {
            return ValidationResult(false, "L'adresse IP ne peut pas être vide")
        }

        val trimmed = ip.trim().lowercase()

        // Rejeter explicitement localhost et 127.0.0.1
        if (trimmed == "localhost" || trimmed == "127.0.0.1" || trimmed.startsWith("127.")) {
            return ValidationResult(false, "L'adresse IP doit être une adresse réseau locale (192.168.x.x, 10.x.x.x, ou 172.16-31.x.x), pas localhost")
        }

        // IPv4
        if (Patterns.IP_ADDRESS.matcher(trimmed).matches()) {
            // Vérifier que c'est une adresse réseau locale
            val parts = trimmed.split(".")
            if (parts.size == 4) {
                val first = parts[0].toIntOrNull()
                val second = parts[1].toIntOrNull()

                if (first != null && second != null) {
                    val isLocalNetwork = when {
                        // 192.168.0.0/16
                        first == 192 && second == 168 -> true
                        // 10.0.0.0/8
                        first == 10 -> true
                        // 172.16.0.0/12 (172.16.0.0 à 172.31.255.255)
                        first == 172 && second in 16..31 -> true
                        else -> false
                    }

                    if (isLocalNetwork) {
                        return ValidationResult(true, null)
                    } else {
                        return ValidationResult(false, "L'adresse IP doit être une adresse réseau locale (192.168.x.x, 10.x.x.x, ou 172.16-31.x.x)")
                    }
                }
            }
        }

        // IPv6 (simplifié) - rejeter pour l'instant car plus complexe à valider
        if (trimmed.contains(":")) {
            return ValidationResult(false, "Les adresses IPv6 ne sont pas supportées pour l'instant")
        }

        return ValidationResult(false, "Format d'adresse IP invalide")
    }

    /**
     * Valide un port
     */
    fun validatePort(port: Int): ValidationResult {
        return when {
            port < 1 -> ValidationResult(false, "Le port doit être supérieur à 0")
            port > 65535 -> ValidationResult(false, "Le port doit être inférieur à 65536")
            else -> ValidationResult(true, null)
        }
    }

    /**
     * Valide une URL
     */
    fun validateUrl(url: String): ValidationResult {
        if (url.isBlank()) {
            return ValidationResult(false, "L'URL ne peut pas être vide")
        }

        return try {
            java.net.URL(url)
            ValidationResult(true, null)
        } catch (e: Exception) {
            ValidationResult(false, "Format d'URL invalide")
        }
    }

    /**
     * Valide un nom de profil
     */
    fun validateProfileName(name: String): ValidationResult {
        return when {
            name.isBlank() -> ValidationResult(false, "Le nom du profil ne peut pas être vide")
            name.length > 50 -> ValidationResult(false, "Le nom du profil est trop long (max 50 caractères)")
            !name.matches(Regex("^[a-zA-Z0-9 _-]+$")) -> ValidationResult(false, "Le nom contient des caractères invalides")
            else -> ValidationResult(true, null)
        }
    }

    /**
     * Valide les dimensions d'une grille
     */
    fun validateGridDimensions(rows: Int, cols: Int): ValidationResult {
        return when {
            rows < 1 -> ValidationResult(false, "Le nombre de lignes doit être supérieur à 0")
            rows > 20 -> ValidationResult(false, "Le nombre de lignes est trop élevé (max 20)")
            cols < 1 -> ValidationResult(false, "Le nombre de colonnes doit être supérieur à 0")
            cols > 20 -> ValidationResult(false, "Le nombre de colonnes est trop élevé (max 20)")
            else -> ValidationResult(true, null)
        }
    }

    /**
     * Valide un label de contrôle
     */
    fun validateControlLabel(label: String): ValidationResult {
        return when {
            label.isBlank() -> ValidationResult(false, "Le label ne peut pas être vide")
            label.length > 30 -> ValidationResult(false, "Le label est trop long (max 30 caractères)")
            else -> ValidationResult(true, null)
        }
    }

    /**
     * Valide une couleur hex
     */
    fun validateHexColor(color: String): ValidationResult {
        val hexPattern = Pattern.compile("^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$")
        return if (hexPattern.matcher(color).matches()) {
            ValidationResult(true, null)
        } else {
            ValidationResult(false, "Format de couleur invalide (ex: #FF0000)")
        }
    }
}

/**
 * Résultat de validation
 */
data class ValidationResult(
    val isValid: Boolean,
    val errorMessage: String?
)





