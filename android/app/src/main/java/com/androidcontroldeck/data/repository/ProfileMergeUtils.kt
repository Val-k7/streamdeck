package com.androidcontroldeck.data.repository

import com.androidcontroldeck.data.model.Control
import com.androidcontroldeck.data.model.Profile

/**
 * Utilitaires pour fusionner des profils
 */
object ProfileMergeUtils {

    /**
     * Fusionne deux profils avec différentes stratégies
     */
    fun mergeProfiles(
        baseProfile: Profile,
        importedProfile: Profile,
        strategy: MergeStrategy
    ): Profile {
        return when (strategy) {
            MergeStrategy.REPLACE -> importedProfile.copy(id = baseProfile.id)

            MergeStrategy.KEEP_BASE -> baseProfile

            MergeStrategy.MERGE_CONTROLS -> {
                mergeControls(baseProfile, importedProfile)
            }

            MergeStrategy.MERGE_SMART -> {
                mergeSmart(baseProfile, importedProfile)
            }
        }
    }

    /**
     * Fusion simple : ajoute les contrôles de l'import qui n'existent pas dans le profil de base
     */
    private fun mergeControls(baseProfile: Profile, importedProfile: Profile): Profile {
        val baseControlIds = baseProfile.controls.map { it.id }.toSet()

        // Ajouter les contrôles qui n'existent pas déjà
        val newControls = importedProfile.controls
            .filter { it.id !in baseControlIds }
            .map { control ->
                // Ajuster la position si nécessaire pour éviter les collisions
                adjustControlPosition(control, baseProfile.controls, baseProfile.rows, baseProfile.cols)
            }

        val mergedControls = baseProfile.controls + newControls

        return baseProfile.copy(
            controls = mergedControls,
            version = maxOf(baseProfile.version, importedProfile.version) + 1
        )
    }

    /**
     * Fusion intelligente : combine les meilleurs aspects des deux profils
     */
    private fun mergeSmart(baseProfile: Profile, importedProfile: Profile): Profile {
        val mergedControls = mutableListOf<Control>()
        val baseControlIds = baseProfile.controls.map { it.id }.toSet()

        // 1. Conserver tous les contrôles de la base
        mergedControls.addAll(baseProfile.controls)

        // 2. Pour les contrôles avec le même ID, garder le plus récent (ou celui avec la version la plus élevée)
        val importedMap = importedProfile.controls.associateBy { it.id }
        baseProfile.controls.forEach { baseControl ->
            importedMap[baseControl.id]?.let { importedControl ->
                // Utiliser le contrôle importé si sa version est plus récente
                if (importedProfile.version > baseProfile.version) {
                    mergedControls.remove(baseControl)
                    mergedControls.add(importedControl)
                }
            }
        }

        // 3. Ajouter les nouveaux contrôles de l'import avec ajustement de position
        importedProfile.controls
            .filter { it.id !in baseControlIds }
            .forEach { control ->
                val adjustedControl = adjustControlPosition(
                    control,
                    mergedControls,
                    baseProfile.rows,
                    baseProfile.cols
                )
                mergedControls.add(adjustedControl)
            }

        // 4. Prendre le plus grand grid size
        val maxRows = maxOf(baseProfile.rows, importedProfile.rows)
        val maxCols = maxOf(baseProfile.cols, importedProfile.cols)

        return baseProfile.copy(
            rows = maxRows,
            cols = maxCols,
            controls = mergedControls,
            version = maxOf(baseProfile.version, importedProfile.version) + 1,
            name = "${baseProfile.name} (Merged)"
        )
    }

    /**
     * Ajuste la position d'un contrôle pour éviter les collisions
     */
    private fun adjustControlPosition(
        control: Control,
        existingControls: List<Control>,
        maxRows: Int,
        maxCols: Int
    ): Control {
        var adjustedControl = control
        var attempts = 0
        val maxAttempts = maxRows * maxCols

        while (hasCollision(adjustedControl, existingControls) && attempts < maxAttempts) {
            // Essayer de décaler vers la droite
            if (adjustedControl.col + adjustedControl.colSpan < maxCols) {
                adjustedControl = adjustedControl.copy(col = adjustedControl.col + 1)
            }
            // Sinon, décaler vers le bas
            else if (adjustedControl.row + adjustedControl.rowSpan < maxRows) {
                adjustedControl = adjustedControl.copy(
                    row = adjustedControl.row + 1,
                    col = 0
                )
            }
            // Sinon, réduire la taille si possible
            else {
                val newRowSpan = adjustedControl.rowSpan.coerceAtMost(maxRows - adjustedControl.row)
                val newColSpan = adjustedControl.colSpan.coerceAtMost(maxCols - adjustedControl.col)
                adjustedControl = adjustedControl.copy(
                    rowSpan = newRowSpan,
                    colSpan = newColSpan
                )
            }
            attempts++
        }

        // Si toujours en collision, placer en (0, 0) avec taille minimale
        if (hasCollision(adjustedControl, existingControls)) {
            adjustedControl = adjustedControl.copy(
                row = 0,
                col = 0,
                rowSpan = 1,
                colSpan = 1
            )
        }

        return adjustedControl
    }

    /**
     * Vérifie si un contrôle entre en collision avec d'autres
     */
    private fun hasCollision(control: Control, existingControls: List<Control>): Boolean {
        return existingControls.any { other ->
            if (other.id == control.id) return@any false
            rectanglesOverlap(
                control.row, control.col, control.rowSpan, control.colSpan,
                other.row, other.col, other.rowSpan, other.colSpan
            )
        }
    }

    /**
     * Vérifie si deux rectangles se chevauchent
     */
    private fun rectanglesOverlap(
        r1: Int, c1: Int, h1: Int, w1: Int,
        r2: Int, c2: Int, h2: Int, w2: Int
    ): Boolean {
        return !(r1 + h1 <= r2 || r2 + h2 <= r1 || c1 + w1 <= c2 || c2 + w2 <= c1)
    }

    /**
     * Génère un rapport de fusion
     */
    data class MergeReport(
        val baseControlsCount: Int,
        val importedControlsCount: Int,
        val mergedControlsCount: Int,
        val newControlsAdded: Int,
        val controlsReplaced: Int,
        val collisionsResolved: Int,
        val gridSizeChanged: Boolean
    )

    /**
     * Calcule un rapport de fusion avant de réellement fusionner
     */
    fun generateMergeReport(
        baseProfile: Profile,
        importedProfile: Profile,
        strategy: MergeStrategy
    ): MergeReport {
        val baseControlsIds = baseProfile.controls.map { it.id }.toSet()
        val importedControlsIds = importedProfile.controls.map { it.id }.toSet()

        val newControls = importedProfile.controls.filter { it.id !in baseControlsIds }
        val replacedControls = baseProfile.controls.filter { it.id in importedControlsIds }

        val collisions = newControls.count { control ->
            hasCollision(control, baseProfile.controls)
        }

        val gridSizeChanged = baseProfile.rows != importedProfile.rows ||
                baseProfile.cols != importedProfile.cols

        val mergedControlsCount = when (strategy) {
            MergeStrategy.REPLACE -> importedProfile.controls.size
            MergeStrategy.KEEP_BASE -> baseProfile.controls.size
            MergeStrategy.MERGE_CONTROLS -> baseProfile.controls.size + newControls.size
            MergeStrategy.MERGE_SMART -> {
                baseProfile.controls.size + newControls.size - replacedControls.size
            }
        }

        return MergeReport(
            baseControlsCount = baseProfile.controls.size,
            importedControlsCount = importedProfile.controls.size,
            mergedControlsCount = mergedControlsCount,
            newControlsAdded = newControls.size,
            controlsReplaced = replacedControls.size,
            collisionsResolved = collisions,
            gridSizeChanged = gridSizeChanged
        )
    }
}

/**
 * Stratégies de fusion de profils
 */
enum class MergeStrategy {
    /**
     * Remplace complètement le profil de base par l'importé
     */
    REPLACE,

    /**
     * Garde uniquement le profil de base
     */
    KEEP_BASE,

    /**
     * Fusion simple : ajoute les nouveaux contrôles
     */
    MERGE_CONTROLS,

    /**
     * Fusion intelligente : combine les meilleurs aspects
     */
    MERGE_SMART
}





