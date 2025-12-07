package com.androidcontroldeck.data.repository

import com.androidcontroldeck.data.model.Control
import com.androidcontroldeck.data.model.Profile
import java.util.*

/**
 * Utilitaires pour la duplication de profils et de contrôles
 */
object ProfileDuplicationUtils {

    /**
     * Duplique un profil avec un nouveau nom et ID
     */
    fun duplicateProfile(profile: Profile, newName: String? = null): Profile {
        val newId = generateUniqueId(profile.id, "copy")
        val duplicateName = newName ?: "${profile.name} (Copy)"

        // Dupliquer tous les contrôles avec de nouveaux IDs
        val duplicatedControls = profile.controls.map { control ->
            duplicateControl(control, newId)
        }

        return profile.copy(
            id = newId,
            name = duplicateName,
            controls = duplicatedControls,
            version = 1 // Réinitialiser la version pour un nouveau profil
        )
    }

    /**
     * Duplique un contrôle avec un nouvel ID
     */
    fun duplicateControl(control: Control, profileId: String? = null): Control {
        val newId = generateControlId(control.id, profileId)

        return control.copy(
            id = newId,
            label = if (control.label.isNotEmpty()) "${control.label} (Copy)" else control.label
        )
    }

    /**
     * Duplique plusieurs contrôles
     */
    fun duplicateControls(controls: List<Control>, profileId: String? = null): List<Control> {
        return controls.map { duplicateControl(it, profileId) }
    }

    /**
     * Génère un ID unique basé sur un ID existant
     */
    private fun generateUniqueId(baseId: String, suffix: String = "copy"): String {
        val sanitized = baseId.lowercase()
            .replace(Regex("[^a-z0-9_-]"), "_")
            .take(40) // Limiter la longueur

        return if (sanitized.endsWith("_$suffix")) {
            "${sanitized}_${System.currentTimeMillis() % 10000}"
        } else {
            "${sanitized}_$suffix"
        }
    }

    /**
     * Génère un ID unique pour un contrôle
     */
    private fun generateControlId(baseId: String, profileId: String?): String {
        val prefix = profileId?.let { "${it}_" } ?: ""
        val sanitized = baseId.lowercase()
            .replace(Regex("[^a-z0-9_-]"), "_")
            .take(30)

        // Ajouter un timestamp pour garantir l'unicité
        val timestamp = System.currentTimeMillis() % 100000
        return "${prefix}${sanitized}_${timestamp}"
    }

    /**
     * Vérifie si un ID de profil est déjà utilisé
     */
    fun isProfileIdAvailable(id: String, existingProfiles: List<Profile>): Boolean {
        return existingProfiles.none { it.id == id }
    }

    /**
     * Génère un ID disponible pour un profil
     */
    fun generateAvailableProfileId(baseId: String, existingProfiles: List<Profile>): String {
        var candidate = baseId
        var counter = 1

        while (!isProfileIdAvailable(candidate, existingProfiles)) {
            candidate = "${baseId}_${counter}"
            counter++
        }

        return candidate
    }

    /**
     * Duplique un profil avec décalage des contrôles dans la grille
     */
    fun duplicateProfileWithOffset(
        profile: Profile,
        rowOffset: Int = 0,
        colOffset: Int = 0,
        newName: String? = null
    ): Profile {
        val duplicated = duplicateProfile(profile, newName)

        // Décaler tous les contrôles
        val offsetControls = duplicated.controls.map { control ->
            val newRow = (control.row + rowOffset).coerceIn(0, profile.rows - control.rowSpan)
            val newCol = (control.col + colOffset).coerceIn(0, profile.cols - control.colSpan)

            control.copy(row = newRow, col = newCol)
        }

        return duplicated.copy(controls = offsetControls)
    }

    /**
     * Duplique un contrôle avec décalage dans la grille
     */
    fun duplicateControlWithOffset(
        control: Control,
        rowOffset: Int = 0,
        colOffset: Int = 0,
        maxRows: Int = Int.MAX_VALUE,
        maxCols: Int = Int.MAX_VALUE,
        profileId: String? = null
    ): Control {
        val duplicated = duplicateControl(control, profileId)

        val newRow = (control.row + rowOffset).coerceIn(0, maxRows - control.rowSpan)
        val newCol = (control.col + colOffset).coerceIn(0, maxCols - control.colSpan)

        return duplicated.copy(row = newRow, col = newCol)
    }

    /**
     * Trouve une position libre dans la grille pour un nouveau contrôle
     */
    fun findFreePosition(
        control: Control,
        existingControls: List<Control>,
        maxRows: Int,
        maxCols: Int
    ): Pair<Int, Int> {
        // Chercher une position libre proche de la position actuelle
        for (offset in 1 until maxRows + maxCols) {
            for (rowOffset in -offset..offset) {
                for (colOffset in -offset..offset) {
                    val newRow = (control.row + rowOffset).coerceIn(0, maxRows - control.rowSpan)
                    val newCol = (control.col + colOffset).coerceIn(0, maxCols - control.colSpan)

                    // Vérifier si cette position est libre
                    val conflicts = existingControls.any { other ->
                        rectanglesIntersect(
                            newRow, newCol, control.rowSpan, control.colSpan,
                            other.row, other.col, other.rowSpan, other.colSpan
                        )
                    }

                    if (!conflicts) {
                        return Pair(newRow, newCol)
                    }
                }
            }
        }

        // Si aucune position libre trouvée, placer en (0, 0)
        return Pair(0, 0)
    }

    private fun rectanglesIntersect(
        r1: Int, c1: Int, h1: Int, w1: Int,
        r2: Int, c2: Int, h2: Int, w2: Int
    ): Boolean {
        return !(r1 + h1 <= r2 || r2 + h2 <= r1 || c1 + w1 <= c2 || c2 + w2 <= c1)
    }
}

