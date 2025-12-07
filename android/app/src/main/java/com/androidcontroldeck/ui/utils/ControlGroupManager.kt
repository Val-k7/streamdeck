package com.androidcontroldeck.ui.utils

import com.androidcontroldeck.data.model.Control

/**
 * Gestionnaire de groupes de contrôles pour l'éditeur
 */
object ControlGroupManager {

    /**
     * Représente un groupe de contrôles
     */
    data class ControlGroup(
        val id: String,
        val name: String,
        val controlIds: Set<String>,
        val color: String? = null
    )

    /**
     * Crée un groupe à partir d'une liste de contrôles
     */
    fun createGroup(controlIds: List<String>, name: String = "Groupe ${System.currentTimeMillis()}"): ControlGroup {
        return ControlGroup(
            id = "group_${System.currentTimeMillis()}",
            name = name,
            controlIds = controlIds.toSet()
        )
    }

    /**
     * Ajoute un contrôle à un groupe
     */
    fun addControlToGroup(group: ControlGroup, controlId: String): ControlGroup {
        return group.copy(controlIds = group.controlIds + controlId)
    }

    /**
     * Retire un contrôle d'un groupe
     */
    fun removeControlFromGroup(group: ControlGroup, controlId: String): ControlGroup {
        return group.copy(controlIds = group.controlIds - controlId)
    }

    /**
     * Vérifie si un contrôle appartient à un groupe
     */
    fun isControlInGroup(controlId: String, group: ControlGroup): Boolean {
        return controlId in group.controlIds
    }

    /**
     * Trouve tous les groupes contenant un contrôle
     */
    fun findGroupsForControl(controlId: String, groups: List<ControlGroup>): List<ControlGroup> {
        return groups.filter { controlId in it.controlIds }
    }

    /**
     * Déplace tous les contrôles d'un groupe
     */
    fun moveGroup(
        group: ControlGroup,
        controls: List<Control>,
        deltaRow: Int,
        deltaCol: Int,
        maxRows: Int,
        maxCols: Int
    ): List<Control> {
        val groupControls = controls.filter { it.id in group.controlIds }
        val otherControls = controls.filter { it.id !in group.controlIds }

        val movedControls = groupControls.map { control ->
            val newRow = (control.row + deltaRow).coerceIn(0, maxRows - control.rowSpan)
            val newCol = (control.col + deltaCol).coerceIn(0, maxCols - control.colSpan)

            // Vérifier les collisions avec les autres contrôles
            val hasCollision = otherControls.any { other ->
                rectanglesOverlap(
                    newRow, newCol, control.rowSpan, control.colSpan,
                    other.row, other.col, other.rowSpan, other.colSpan
                )
            }

            if (!hasCollision) {
                control.copy(row = newRow, col = newCol)
            } else {
                control // Garder la position originale en cas de collision
            }
        }

        return otherControls + movedControls
    }

    /**
     * Duplique un groupe de contrôles
     */
    fun duplicateGroup(
        group: ControlGroup,
        controls: List<Control>,
        profileId: String? = null,
        offsetRow: Int = 1,
        offsetCol: Int = 1
    ): Pair<ControlGroup, List<Control>> {
        val groupControls = controls.filter { it.id in group.controlIds }
        val otherControls = controls.filter { it.id !in group.controlIds }

        // Dupliquer les contrôles avec décalage
        val duplicatedControls = groupControls.map { control ->
            val newId = generateControlId(control.id, profileId)
            control.copy(
                id = newId,
                row = control.row + offsetRow,
                col = control.col + offsetCol,
                label = "${control.label} (Copy)"
            )
        }

        val newGroup = ControlGroup(
            id = "group_${System.currentTimeMillis()}",
            name = "${group.name} (Copy)",
            controlIds = duplicatedControls.map { it.id }.toSet(),
            color = group.color
        )

        return Pair(newGroup, otherControls + duplicatedControls)
    }

    /**
     * Supprime un groupe (supprime les contrôles ou les retire du groupe)
     */
    fun removeGroup(
        group: ControlGroup,
        controls: List<Control>,
        deleteControls: Boolean = false
    ): List<Control> {
        return if (deleteControls) {
            controls.filter { it.id !in group.controlIds }
        } else {
            controls // Garder les contrôles, juste retirer le groupe
        }
    }

    /**
     * Fusionne deux groupes
     */
    fun mergeGroups(group1: ControlGroup, group2: ControlGroup, newName: String? = null): ControlGroup {
        return ControlGroup(
            id = group1.id, // Garder l'ID du premier groupe
            name = newName ?: "${group1.name} + ${group2.name}",
            controlIds = group1.controlIds + group2.controlIds,
            color = group1.color ?: group2.color
        )
    }

    /**
     * Vérifie si deux groupes se chevauchent
     */
    fun groupsOverlap(group1: ControlGroup, group2: ControlGroup): Boolean {
        return group1.controlIds.intersect(group2.controlIds).isNotEmpty()
    }

    /**
     * Calcule les limites d'un groupe (bounding box)
     */
    fun getGroupBounds(group: ControlGroup, controls: List<Control>): Bounds? {
        val groupControls = controls.filter { it.id in group.controlIds }
        if (groupControls.isEmpty()) return null

        val minRow = groupControls.minOfOrNull { it.row } ?: return null
        val maxRow = groupControls.maxOfOrNull { it.row + it.rowSpan } ?: return null
        val minCol = groupControls.minOfOrNull { it.col } ?: return null
        val maxCol = groupControls.maxOfOrNull { it.col + it.colSpan } ?: return null

        return Bounds(
            row = minRow,
            col = minCol,
            rowSpan = maxRow - minRow,
            colSpan = maxCol - minCol
        )
    }

    /**
     * Sélectionne tous les contrôles dans une région
     */
    fun selectControlsInRegion(
        controls: List<Control>,
        startRow: Int,
        startCol: Int,
        endRow: Int,
        endCol: Int
    ): List<String> {
        val minRow = minOf(startRow, endRow)
        val maxRow = maxOf(startRow, endRow)
        val minCol = minOf(startCol, endCol)
        val maxCol = maxOf(startCol, endCol)

        return controls.filter { control ->
            // Vérifier si le contrôle intersecte avec la région
            !(control.row + control.rowSpan <= minRow ||
              control.row >= maxRow ||
              control.col + control.colSpan <= minCol ||
              control.col >= maxCol)
        }.map { it.id }
    }

    /**
     * Crée un groupe à partir d'une sélection rectangulaire
     */
    fun createGroupFromSelection(
        selectedControlIds: List<String>,
        name: String = "Sélection"
    ): ControlGroup {
        return createGroup(selectedControlIds, name)
    }

    data class Bounds(
        val row: Int,
        val col: Int,
        val rowSpan: Int,
        val colSpan: Int
    )

    private fun rectanglesOverlap(
        r1: Int, c1: Int, h1: Int, w1: Int,
        r2: Int, c2: Int, h2: Int, w2: Int
    ): Boolean {
        return !(r1 + h1 <= r2 || r2 + h2 <= r1 || c1 + w1 <= c2 || c2 + w2 <= c1)
    }

    private fun generateControlId(baseId: String, profileId: String?): String {
        val prefix = profileId?.let { "${it}_" } ?: ""
        val sanitized = baseId.lowercase()
            .replace(Regex("[^a-z0-9_-]"), "_")
            .take(30)
        val timestamp = System.currentTimeMillis() % 100000
        return "${prefix}${sanitized}_${timestamp}"
    }
}

