package com.androidcontroldeck.ui.utils

import com.androidcontroldeck.data.model.Profile

/**
 * Gestionnaire d'historique undo/redo pour l'éditeur de profils
 */
class UndoRedoManager<T>(maxHistorySize: Int = 50) {
    private val undoStack = mutableListOf<T>()
    private val redoStack = mutableListOf<T>()
    private var maxSize = maxHistorySize

    /**
     * Ajoute un état à l'historique
     */
    fun push(state: T) {
        undoStack.add(state)
        redoStack.clear()

        // Limiter la taille de l'historique
        if (undoStack.size > maxSize) {
            undoStack.removeAt(0)
        }
    }

    /**
     * Annule la dernière action
     */
    fun undo(currentState: T): T? {
        if (undoStack.isEmpty()) return null

        val previousState = undoStack.removeAt(undoStack.size - 1)
        redoStack.add(currentState)

        return previousState
    }

    /**
     * Refait la dernière action annulée
     */
    fun redo(currentState: T): T? {
        if (redoStack.isEmpty()) return null

        val nextState = redoStack.removeAt(redoStack.size - 1)
        undoStack.add(currentState)

        return nextState
    }

    /**
     * Vérifie si undo est disponible
     */
    fun canUndo(): Boolean = undoStack.isNotEmpty()

    /**
     * Vérifie si redo est disponible
     */
    fun canRedo(): Boolean = redoStack.isNotEmpty()

    /**
     * Réinitialise l'historique
     */
    fun clear() {
        undoStack.clear()
        redoStack.clear()
    }

    /**
     * Réinitialise avec un état initial
     */
    fun initialize(initialState: T) {
        clear()
        push(initialState)
    }
}





