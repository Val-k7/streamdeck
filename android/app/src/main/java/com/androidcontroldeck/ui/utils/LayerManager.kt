package com.androidcontroldeck.ui.utils

import com.androidcontroldeck.data.model.Control

/**
 * Gestionnaire de calques (z-index) pour superposer des contrôles
 */
object LayerManager {

    /**
     * Représente un calque avec z-index
     */
    data class Layer(
        val id: String,
        val name: String,
        val zIndex: Int,
        val controlIds: Set<String>,
        val locked: Boolean = false,
        val visible: Boolean = true
    )

    /**
     * Calcule le z-index optimal pour un nouveau calque
     */
    fun calculateNextZIndex(existingLayers: List<Layer>): Int {
        return if (existingLayers.isEmpty()) {
            0
        } else {
            existingLayers.maxOfOrNull { it.zIndex }?.plus(1) ?: 0
        }
    }

    /**
     * Crée un nouveau calque
     */
    fun createLayer(
        controlIds: List<String>,
        name: String = "Calque ${System.currentTimeMillis()}",
        existingLayers: List<Layer> = emptyList()
    ): Layer {
        return Layer(
            id = "layer_${System.currentTimeMillis()}",
            name = name,
            zIndex = calculateNextZIndex(existingLayers),
            controlIds = controlIds.toSet()
        )
    }

    /**
     * Ajoute un contrôle à un calque
     */
    fun addControlToLayer(layer: Layer, controlId: String): Layer {
        return layer.copy(controlIds = layer.controlIds + controlId)
    }

    /**
     * Retire un contrôle d'un calque
     */
    fun removeControlFromLayer(layer: Layer, controlId: String): Layer {
        return layer.copy(controlIds = layer.controlIds - controlId)
    }

    /**
     * Déplace un calque vers l'avant (z-index plus élevé)
     */
    fun bringToFront(layer: Layer, allLayers: List<Layer>): List<Layer> {
        val maxZIndex = allLayers.maxOfOrNull { it.zIndex } ?: 0
        val updatedLayer = layer.copy(zIndex = maxZIndex + 1)
        return allLayers.map { if (it.id == layer.id) updatedLayer else it }
    }

    /**
     * Déplace un calque vers l'arrière (z-index plus bas)
     */
    fun sendToBack(layer: Layer, allLayers: List<Layer>): List<Layer> {
        val minZIndex = allLayers.minOfOrNull { it.zIndex } ?: 0
        val updatedLayer = layer.copy(zIndex = minZIndex - 1)
        return allLayers.map { if (it.id == layer.id) updatedLayer else it }
    }

    /**
     * Déplace un calque d'un niveau vers l'avant
     */
    fun bringForward(layer: Layer, allLayers: List<Layer>): List<Layer> {
        val layersAbove = allLayers.filter { it.zIndex > layer.zIndex }
        if (layersAbove.isEmpty()) return allLayers // Déjà au premier plan

        val nextZIndex = layersAbove.minOf { it.zIndex }
        val updatedLayer = layer.copy(zIndex = nextZIndex)
        val swappedLayer = layersAbove.first { it.zIndex == nextZIndex }
        val swappedUpdated = swappedLayer.copy(zIndex = layer.zIndex)

        return allLayers.map {
            when (it.id) {
                layer.id -> updatedLayer
                swappedLayer.id -> swappedUpdated
                else -> it
            }
        }
    }

    /**
     * Déplace un calque d'un niveau vers l'arrière
     */
    fun sendBackward(layer: Layer, allLayers: List<Layer>): List<Layer> {
        val layersBelow = allLayers.filter { it.zIndex < layer.zIndex }
        if (layersBelow.isEmpty()) return allLayers // Déjà au dernier plan

        val nextZIndex = layersBelow.maxOf { it.zIndex }
        val updatedLayer = layer.copy(zIndex = nextZIndex)
        val swappedLayer = layersBelow.first { it.zIndex == nextZIndex }
        val swappedUpdated = swappedLayer.copy(zIndex = layer.zIndex)

        return allLayers.map {
            when (it.id) {
                layer.id -> updatedLayer
                swappedLayer.id -> swappedUpdated
                else -> it
            }
        }
    }

    /**
     * Verrouille/déverrouille un calque
     */
    fun toggleLock(layer: Layer): Layer {
        return layer.copy(locked = !layer.locked)
    }

    /**
     * Affiche/cache un calque
     */
    fun toggleVisibility(layer: Layer): Layer {
        return layer.copy(visible = !layer.visible)
    }

    /**
     * Trouve le calque contenant un contrôle
     */
    fun findLayerForControl(controlId: String, layers: List<Layer>): Layer? {
        return layers.firstOrNull { controlId in it.controlIds }
    }

    /**
     * Trouve tous les calques contenant un contrôle
     */
    fun findLayersForControl(controlId: String, layers: List<Layer>): List<Layer> {
        return layers.filter { controlId in it.controlIds }
    }

    /**
     * Trie les contrôles par z-index (du plus bas au plus haut)
     */
    fun sortControlsByZIndex(controls: List<Control>, layers: List<Layer>): List<Control> {
        val controlToZIndex = mutableMapOf<String, Int>()

        controls.forEach { control ->
            val layer = findLayerForControl(control.id, layers)
            controlToZIndex[control.id] = layer?.zIndex ?: Int.MIN_VALUE
        }

        return controls.sortedBy { controlToZIndex[it.id] ?: Int.MIN_VALUE }
    }

    /**
     * Filtre les contrôles visibles (selon la visibilité des calques)
     */
    fun filterVisibleControls(controls: List<Control>, layers: List<Layer>): List<Control> {
        val hiddenLayerIds = layers.filter { !it.visible }.flatMap { it.controlIds }.toSet()
        return controls.filter { it.id !in hiddenLayerIds }
    }

    /**
     * Vérifie si un contrôle est verrouillé (via son calque)
     */
    fun isControlLocked(controlId: String, layers: List<Layer>): Boolean {
        return findLayerForControl(controlId, layers)?.locked == true
    }

    /**
     * Normalise les z-index pour éviter les valeurs trop grandes
     */
    fun normalizeZIndices(layers: List<Layer>): List<Layer> {
        if (layers.isEmpty()) return layers

        val sorted = layers.sortedBy { it.zIndex }
        return sorted.mapIndexed { index, layer ->
            layer.copy(zIndex = index)
        }
    }

    /**
     * Fusionne deux calques
     */
    fun mergeLayers(layer1: Layer, layer2: Layer, newName: String? = null): Layer {
        return Layer(
            id = layer1.id,
            name = newName ?: "${layer1.name} + ${layer2.name}",
            zIndex = maxOf(layer1.zIndex, layer2.zIndex), // Prendre le z-index le plus élevé
            controlIds = layer1.controlIds + layer2.controlIds,
            locked = layer1.locked || layer2.locked,
            visible = layer1.visible && layer2.visible
        )
    }

    /**
     * Duplique un calque
     */
    fun duplicateLayer(layer: Layer, existingLayers: List<Layer>): Layer {
        return Layer(
            id = "layer_${System.currentTimeMillis()}",
            name = "${layer.name} (Copy)",
            zIndex = calculateNextZIndex(existingLayers),
            controlIds = layer.controlIds,
            locked = false, // Nouveau calque non verrouillé
            visible = layer.visible
        )
    }
}





