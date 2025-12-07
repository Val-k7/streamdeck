package com.androidcontroldeck.ui.utils

import com.androidcontroldeck.data.model.Control

/**
 * Utilitaires pour le snap-to-grid et l'alignement dans l'éditeur
 */
object GridSnapHelper {

    /**
     * Aligne une position à la grille
     */
    fun snapToGrid(
        row: Float,
        col: Float,
        gridSize: Float = 1f
    ): Pair<Int, Int> {
        val snappedRow = (row / gridSize).toInt().coerceAtLeast(0)
        val snappedCol = (col / gridSize).toInt().coerceAtLeast(0)
        return Pair(snappedRow, snappedCol)
    }

    /**
     * Vérifie si deux positions sont alignées sur la même grille
     */
    fun areAligned(
        row1: Int,
        col1: Int,
        row2: Int,
        col2: Int
    ): Boolean {
        return row1 == row2 || col1 == col2
    }

    /**
     * Aligne un contrôle à la grille
     */
    fun snapControlToGrid(control: Control, gridSize: Int = 1): Control {
        return control.copy(
            row = (control.row / gridSize) * gridSize,
            col = (control.col / gridSize) * gridSize
        )
    }

    /**
     * Vérifie si un contrôle est aligné sur la grille
     */
    fun isControlAligned(control: Control, gridSize: Int = 1): Boolean {
        return control.row % gridSize == 0 && control.col % gridSize == 0
    }

    /**
     * Aligne plusieurs contrôles à la grille
     */
    fun snapControlsToGrid(controls: List<Control>, gridSize: Int = 1): List<Control> {
        return controls.map { snapControlToGrid(it, gridSize) }
    }

    /**
     * Calcule la distance minimale pour aligner un contrôle avec d'autres
     */
    fun calculateSnapDistance(
        control: Control,
        otherControls: List<Control>,
        snapThreshold: Int = 2
    ): Pair<Int, Int>? {
        var minDistance = snapThreshold
        var bestOffset: Pair<Int, Int>? = null

        for (other in otherControls) {
            if (other.id == control.id) continue

            // Snap horizontal
            val colDistance = other.col - control.col
            if (kotlin.math.abs(colDistance) < minDistance) {
                minDistance = kotlin.math.abs(colDistance)
                bestOffset = Pair(0, colDistance)
            }

            // Snap vertical
            val rowDistance = other.row - control.row
            if (kotlin.math.abs(rowDistance) < minDistance) {
                minDistance = kotlin.math.abs(rowDistance)
                bestOffset = Pair(rowDistance, 0)
            }

            // Snap aux bords
            val rightEdgeDistance = (other.col + other.colSpan) - control.col
            if (kotlin.math.abs(rightEdgeDistance) < minDistance) {
                minDistance = kotlin.math.abs(rightEdgeDistance)
                bestOffset = Pair(0, rightEdgeDistance)
            }

            val bottomEdgeDistance = (other.row + other.rowSpan) - control.row
            if (kotlin.math.abs(bottomEdgeDistance) < minDistance) {
                minDistance = kotlin.math.abs(bottomEdgeDistance)
                bestOffset = Pair(bottomEdgeDistance, 0)
            }
        }

        return bestOffset
    }

    /**
     * Applique le snap automatique à un contrôle lors du déplacement
     */
    fun applySnap(
        control: Control,
        newRow: Int,
        newCol: Int,
        otherControls: List<Control>,
        snapThreshold: Int = 2,
        snapEnabled: Boolean = true
    ): Pair<Int, Int> {
        if (!snapEnabled) return Pair(newRow, newCol)

        val movedControl = control.copy(row = newRow, col = newCol)
        val snapOffset = calculateSnapDistance(movedControl, otherControls, snapThreshold)

        return if (snapOffset != null) {
            Pair(newRow + snapOffset.first, newCol + snapOffset.second)
        } else {
            Pair(newRow, newCol)
        }
    }

    /**
     * Génère des guides d'alignement visuels
     */
    data class AlignmentGuide(
        val type: GuideType,
        val position: Int,
        val length: Int
    )

    enum class GuideType {
        VERTICAL,
        HORIZONTAL
    }

    /**
     * Calcule les guides d'alignement pour un contrôle
     */
    fun calculateAlignmentGuides(
        control: Control,
        otherControls: List<Control>
    ): List<AlignmentGuide> {
        val guides = mutableListOf<AlignmentGuide>()

        for (other in otherControls) {
            if (other.id == control.id) continue

            // Guide vertical pour alignement des colonnes
            if (control.col == other.col || control.col == (other.col + other.colSpan) ||
                (control.col + control.colSpan) == other.col) {
                guides.add(
                    AlignmentGuide(
                        type = GuideType.VERTICAL,
                        position = other.col,
                        length = other.rowSpan
                    )
                )
            }

            // Guide horizontal pour alignement des lignes
            if (control.row == other.row || control.row == (other.row + other.rowSpan) ||
                (control.row + control.rowSpan) == other.row) {
                guides.add(
                    AlignmentGuide(
                        type = GuideType.HORIZONTAL,
                        position = other.row,
                        length = other.colSpan
                    )
                )
            }
        }

        return guides.distinct()
    }

    /**
     * Vérifie si une position est dans les limites de la grille
     */
    fun isWithinBounds(
        row: Int,
        col: Int,
        rowSpan: Int,
        colSpan: Int,
        maxRows: Int,
        maxCols: Int
    ): Boolean {
        return row >= 0 && col >= 0 &&
                row + rowSpan <= maxRows &&
                col + colSpan <= maxCols
    }

    /**
     * Contraindre un contrôle dans les limites de la grille
     */
    fun constrainToBounds(
        control: Control,
        maxRows: Int,
        maxCols: Int
    ): Control {
        val constrainedRow = control.row.coerceIn(0, maxRows - control.rowSpan)
        val constrainedCol = control.col.coerceIn(0, maxCols - control.colSpan)

        return control.copy(
            row = constrainedRow,
            col = constrainedCol,
            rowSpan = control.rowSpan.coerceAtMost(maxRows - constrainedRow),
            colSpan = control.colSpan.coerceAtMost(maxCols - constrainedCol)
        )
    }
}





