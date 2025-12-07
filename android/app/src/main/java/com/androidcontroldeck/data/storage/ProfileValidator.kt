package com.androidcontroldeck.data.storage

import com.androidcontroldeck.data.model.Profile
import com.androidcontroldeck.data.model.Control

/**
 * Résultat de la validation d'un profil
 */
data class ProfileValidationResult(
    val isValid: Boolean,
    val errors: List<ValidationError> = emptyList(),
    val warnings: List<ValidationWarning> = emptyList()
)

/**
 * Erreur de validation
 */
data class ValidationError(
    val field: String,
    val message: String,
    val severity: ErrorSeverity = ErrorSeverity.ERROR
)

/**
 * Avertissement de validation
 */
data class ValidationWarning(
    val field: String,
    val message: String
)

/**
 * Sévérité de l'erreur
 */
enum class ErrorSeverity {
    ERROR,      // Bloque l'import
    WARNING     // Avertit mais permet l'import
}

/**
 * Validateur de profils avec validation complète
 */
class ProfileValidator {

    /**
     * Valide un profil complet
     */
    fun validate(profile: Profile): ProfileValidationResult {
        val errors = mutableListOf<ValidationError>()
        val warnings = mutableListOf<ValidationWarning>()

        // Validation de base
        validateBasicFields(profile, errors, warnings)

        // Validation de la grille
        validateGrid(profile, errors, warnings)

        // Validation des contrôles
        validateControls(profile, errors, warnings)

        // Validation de compatibilité
        validateCompatibility(profile, warnings)

        return ProfileValidationResult(
            isValid = errors.isEmpty(),
            errors = errors,
            warnings = warnings
        )
    }

    /**
     * Valide les champs de base du profil
     */
    private fun validateBasicFields(
        profile: Profile,
        errors: MutableList<ValidationError>,
        warnings: MutableList<ValidationWarning>
    ) {
        // ID
        if (profile.id.isBlank()) {
            errors.add(ValidationError("id", "L'ID du profil ne peut pas être vide"))
        } else if (!profile.id.matches(Regex("^[a-zA-Z0-9_-]+$"))) {
            errors.add(ValidationError("id", "L'ID du profil contient des caractères invalides (utilisez uniquement lettres, chiffres, _ et -)"))
        }

        // Nom
        if (profile.name.isBlank()) {
            errors.add(ValidationError("name", "Le nom du profil ne peut pas être vide"))
        } else if (profile.name.length > 50) {
            warnings.add(ValidationWarning("name", "Le nom du profil est très long (${profile.name.length} caractères)"))
        }

        // Dimensions
        if (profile.rows < 1 || profile.rows > 20) {
            errors.add(ValidationError("rows", "Le nombre de lignes doit être entre 1 et 20 (actuel: ${profile.rows})"))
        }

        if (profile.cols < 1 || profile.cols > 20) {
            errors.add(ValidationError("cols", "Le nombre de colonnes doit être entre 1 et 20 (actuel: ${profile.cols})"))
        }

        // Version
        if (profile.version < 1) {
            warnings.add(ValidationWarning("version", "La version du profil est inférieure à 1 (${profile.version})"))
        }
    }

    /**
     * Valide la grille et les positions des contrôles
     */
    private fun validateGrid(
        profile: Profile,
        errors: MutableList<ValidationError>,
        warnings: MutableList<ValidationWarning>
    ) {
        val grid = Array(profile.rows) { BooleanArray(profile.cols) { false } }
        val controlPositions = mutableMapOf<String, Pair<Int, Int>>()

        for (control in profile.controls) {
            // Vérifier que le contrôle est dans les limites
            if (control.row < 0 || control.row >= profile.rows) {
                errors.add(ValidationError(
                    "controls[${control.id}].row",
                    "Le contrôle '${control.id}' est en dehors des limites (ligne ${control.row}, max: ${profile.rows - 1})"
                ))
                continue
            }

            if (control.col < 0 || control.col >= profile.cols) {
                errors.add(ValidationError(
                    "controls[${control.id}].col",
                    "Le contrôle '${control.id}' est en dehors des limites (colonne ${control.col}, max: ${profile.cols - 1})"
                ))
                continue
            }

            // Vérifier que le contrôle ne dépasse pas les limites
            if (control.row + control.rowSpan > profile.rows) {
                errors.add(ValidationError(
                    "controls[${control.id}].rowSpan",
                    "Le contrôle '${control.id}' dépasse les limites (ligne ${control.row} + span ${control.rowSpan} > ${profile.rows})"
                ))
            }

            if (control.col + control.colSpan > profile.cols) {
                errors.add(ValidationError(
                    "controls[${control.id}].colSpan",
                    "Le contrôle '${control.id}' dépasse les limites (colonne ${control.col} + span ${control.colSpan} > ${profile.cols})"
                ))
            }

            // Vérifier les chevauchements
            for (row in control.row until (control.row + control.rowSpan).coerceAtMost(profile.rows)) {
                for (col in control.col until (control.col + control.colSpan).coerceAtMost(profile.cols)) {
                    if (grid[row][col]) {
                        val existingControl = controlPositions.entries.firstOrNull { (_, pos) ->
                            pos.first == row && pos.second == col
                        }
                        errors.add(ValidationError(
                            "controls[${control.id}]",
                            "Le contrôle '${control.id}' chevauche le contrôle '${existingControl?.key ?: "inconnu"}' à la position ($row, $col)"
                        ))
                    } else {
                        grid[row][col] = true
                        controlPositions[control.id] = Pair(row, col)
                    }
                }
            }
        }

        // Avertir si la grille est très peu remplie
        val totalCells = profile.rows * profile.cols
        val usedCells = grid.sumOf { row -> row.count { it } }
        val fillPercentage = (usedCells.toFloat() / totalCells) * 100

        if (fillPercentage < 10 && profile.controls.isNotEmpty()) {
            warnings.add(ValidationWarning(
                "grid",
                "La grille est très peu remplie (${String.format("%.1f", fillPercentage)}% utilisé)"
            ))
        }
    }

    /**
     * Valide les contrôles individuels
     */
    private fun validateControls(
        profile: Profile,
        errors: MutableList<ValidationError>,
        warnings: MutableList<ValidationWarning>
    ) {
        val controlIds = mutableSetOf<String>()

        for (control in profile.controls) {
            // Vérifier l'unicité des IDs
            if (control.id in controlIds) {
                errors.add(ValidationError(
                    "controls[${control.id}].id",
                    "L'ID du contrôle '${control.id}' est dupliqué"
                ))
            } else {
                controlIds.add(control.id)
            }

            // Vérifier le label
            if (control.label.isBlank()) {
                warnings.add(ValidationWarning(
                    "controls[${control.id}].label",
                    "Le contrôle '${control.id}' n'a pas de label"
                ))
            }

            // Vérifier les dimensions
            if (control.rowSpan < 1 || control.rowSpan > 4) {
                errors.add(ValidationError(
                    "controls[${control.id}].rowSpan",
                    "Le rowSpan doit être entre 1 et 4 (actuel: ${control.rowSpan})"
                ))
            }

            if (control.colSpan < 1 || control.colSpan > 4) {
                errors.add(ValidationError(
                    "controls[${control.id}].colSpan",
                    "Le colSpan doit être entre 1 et 4 (actuel: ${control.colSpan})"
                ))
            }

            // Vérifier les valeurs min/max pour les faders et knobs
            if (control.type.name in listOf("FADER", "KNOB")) {
                if (control.minValue != null && control.maxValue != null) {
                    if (control.minValue!! >= control.maxValue!!) {
                        errors.add(ValidationError(
                            "controls[${control.id}].minValue/maxValue",
                            "Le minValue (${control.minValue}) doit être inférieur au maxValue (${control.maxValue})"
                        ))
                    }
                }
            }

            // Vérifier la couleur hex
            if (control.colorHex != null) {
                if (!control.colorHex!!.matches(Regex("^#[0-9A-Fa-f]{6}$"))) {
                    errors.add(ValidationError(
                        "controls[${control.id}].colorHex",
                        "La couleur hex '${control.colorHex}' n'est pas valide (format attendu: #RRGGBB)"
                    ))
                }
            }

            // Valider l'action si présente
            if (control.action != null) {
                validateAction(control.id, control.action, errors, warnings)
            } else {
                warnings.add(ValidationWarning(
                    "controls[${control.id}].action",
                    "Le contrôle '${control.id}' n'a pas d'action définie"
                ))
            }
        }
    }

    /**
     * Valide une action et son payload
     */
    private fun validateAction(
        controlId: String,
        action: com.androidcontroldeck.data.model.Action,
        errors: MutableList<ValidationError>,
        warnings: MutableList<ValidationWarning>
    ) {
        // Liste des ActionType supportés directement
        val supportedActionTypes = setOf(
            com.androidcontroldeck.data.model.ActionType.KEYBOARD,
            com.androidcontroldeck.data.model.ActionType.OBS,
            com.androidcontroldeck.data.model.ActionType.SCRIPT,
            com.androidcontroldeck.data.model.ActionType.AUDIO,
            com.androidcontroldeck.data.model.ActionType.CUSTOM
        )

        // Vérifier que le type d'action est supporté
        if (action.type !in supportedActionTypes) {
            errors.add(ValidationError(
                "controls[$controlId].action.type",
                "Type d'action non supporté: ${action.type}. Types supportés: ${supportedActionTypes.joinToString()}"
            ))
        }

        // Valider le payload selon le type d'action
        when (action.type) {
            com.androidcontroldeck.data.model.ActionType.KEYBOARD -> {
                // Pour KEYBOARD, le payload doit être une string (ex: "CTRL+S")
                if (action.payload.isNullOrBlank()) {
                    warnings.add(ValidationWarning(
                        "controls[$controlId].action.payload",
                        "L'action KEYBOARD du contrôle '$controlId' n'a pas de payload (ex: \"CTRL+S\")"
                    ))
                }
            }

            com.androidcontroldeck.data.model.ActionType.OBS -> {
                // Pour OBS, le payload doit être un JSON valide
                if (action.payload.isNullOrBlank()) {
                    warnings.add(ValidationWarning(
                        "controls[$controlId].action.payload",
                        "L'action OBS du contrôle '$controlId' n'a pas de payload"
                    ))
                } else {
                    validateJsonPayload(controlId, action.payload, "OBS", errors, warnings)
                }
            }

            com.androidcontroldeck.data.model.ActionType.AUDIO -> {
                // Pour AUDIO, le payload doit être un JSON valide ou un string (nom de périphérique)
                if (action.payload.isNullOrBlank()) {
                    warnings.add(ValidationWarning(
                        "controls[$controlId].action.payload",
                        "L'action AUDIO du contrôle '$controlId' n'a pas de payload"
                    ))
                } else {
                    // Essayer de parser comme JSON, sinon considérer comme string (nom de périphérique)
                    try {
                        kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
                            .decodeFromString<Map<String, Any>>(action.payload)
                    } catch (e: Exception) {
                        // Si ce n'est pas du JSON, c'est probablement un nom de périphérique (valide)
                        // Pas d'erreur, juste un warning si nécessaire
                    }
                }
            }

            com.androidcontroldeck.data.model.ActionType.SCRIPT -> {
                // Pour SCRIPT, le payload doit être une commande valide
                if (action.payload.isNullOrBlank()) {
                    warnings.add(ValidationWarning(
                        "controls[$controlId].action.payload",
                        "L'action SCRIPT du contrôle '$controlId' n'a pas de payload (commande à exécuter)"
                    ))
                }
            }

            com.androidcontroldeck.data.model.ActionType.CUSTOM -> {
                // Pour CUSTOM, le payload doit être un JSON valide
                if (action.payload.isNullOrBlank()) {
                    warnings.add(ValidationWarning(
                        "controls[$controlId].action.payload",
                        "L'action CUSTOM du contrôle '$controlId' n'a pas de payload"
                    ))
                } else {
                    validateJsonPayload(controlId, action.payload, "CUSTOM", errors, warnings)

                    // Vérifier que le payload CUSTOM contient soit 'plugin', soit 'action'
                    try {
                        val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
                        val customPayload = json.decodeFromString<Map<String, Any>>(action.payload)

                        val hasPlugin = customPayload.containsKey("plugin")
                        val hasAction = customPayload.containsKey("action")

                        if (!hasPlugin && !hasAction) {
                            warnings.add(ValidationWarning(
                                "controls[$controlId].action.payload",
                                "Le payload CUSTOM du contrôle '$controlId' devrait contenir 'plugin' ou 'action' pour être fonctionnel"
                            ))
                        }

                        // Vérifier les actions supportées via CUSTOM
                        if (hasAction) {
                            val actionName = customPayload["action"] as? String
                            val supportedCustomActions = setOf(
                                "media", "system", "window", "clipboard",
                                "screenshot", "process", "file"
                            )

                            if (actionName != null && actionName !in supportedCustomActions) {
                                warnings.add(ValidationWarning(
                                    "controls[$controlId].action.payload",
                                    "L'action CUSTOM '$actionName' du contrôle '$controlId' pourrait ne pas être supportée. Actions supportées: ${supportedCustomActions.joinToString()}"
                                ))
                            }
                        }
                    } catch (e: Exception) {
                        // Erreur déjà gérée par validateJsonPayload
                    }
                }
            }
        }
    }

    /**
     * Valide qu'un payload est un JSON valide
     */
    private fun validateJsonPayload(
        controlId: String,
        payload: String,
        actionType: String,
        errors: MutableList<ValidationError>,
        warnings: MutableList<ValidationWarning>
    ) {
        try {
            val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
            json.decodeFromString<Map<String, Any>>(payload)
        } catch (e: Exception) {
            errors.add(ValidationError(
                "controls[$controlId].action.payload",
                "Le payload de l'action $actionType du contrôle '$controlId' n'est pas un JSON valide: ${e.message}"
            ))
        }
    }

    /**
     * Valide la compatibilité de version
     */
    private fun validateCompatibility(
        profile: Profile,
        warnings: MutableList<ValidationWarning>
    ) {
        // Avertir si la version est très ancienne
        if (profile.version < 1) {
            warnings.add(ValidationWarning(
                "version",
                "Ce profil utilise une version très ancienne (${profile.version}). Certaines fonctionnalités peuvent ne pas être disponibles."
            ))
        }

        // Avertir si le checksum est manquant
        if (profile.checksum == null) {
            warnings.add(ValidationWarning(
                "checksum",
                "Le profil n'a pas de checksum. L'intégrité ne peut pas être vérifiée."
            ))
        }
    }

    /**
     * Valide un JSON avant import (syntaxe JSON)
     */
    fun validateJsonSyntax(jsonPayload: String): Pair<Boolean, String?> {
        return try {
            // Vérifier que c'est du JSON valide en essayant de le parser
            val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
            json.decodeFromString<Profile>(jsonPayload)
            Pair(true, null)
        } catch (e: Exception) {
            Pair(false, "JSON invalide: ${e.message}")
        }
    }
}

