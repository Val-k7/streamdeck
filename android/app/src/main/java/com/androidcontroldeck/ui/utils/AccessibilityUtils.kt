package com.androidcontroldeck.ui.utils

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*

/**
 * Utilitaires pour améliorer l'accessibilité de l'application
 */

/**
 * Ajoute des propriétés d'accessibilité à un élément cliquable
 */
@Composable
fun Modifier.accessibleClickable(
    onClick: () -> Unit,
    label: String? = null,
    role: Role = Role.Button,
    enabled: Boolean = true
): Modifier {
    return this
        .clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = rememberRipple(),
            enabled = enabled,
            onClick = onClick
        )
        .semantics {
            this.onClick { onClick(); true }
            label?.let { contentDescription = it }
            this.role = role
            if (!enabled) {
                disabled()
            }
        }
        .testTag(label ?: "clickable")
}

/**
 * Ajoute une description de contenu pour TalkBack
 */
fun Modifier.accessibleContentDescription(description: String): Modifier {
    return this.semantics {
        contentDescription = description
    }
}

/**
 * Marque un élément comme important pour l'accessibilité
 */
fun Modifier.accessibleImportant(important: Boolean = true): Modifier {
    return this.semantics {
        if (important) {
            testTag = "important"
        }
    }
}

/**
 * Ajoute un état personnalisé pour l'accessibilité
 */
fun Modifier.accessibleState(state: String, value: String): Modifier {
    return this.semantics {
        testTag = "$state:$value"
    }
}

/**
 * Marque un élément comme en-tête pour la navigation
 */
fun Modifier.accessibleHeading(level: Int = 1): Modifier {
    return this.semantics {
        heading()
    }
}

/**
 * Ajoute un texte alternatif pour les images
 */
fun Modifier.accessibleImageAlt(altText: String): Modifier {
    return this.semantics {
        contentDescription = altText
    }
}

/**
 * Marque un élément comme un bouton avec un label
 */
@Composable
fun Modifier.accessibleButton(
    label: String,
    onClick: () -> Unit,
    enabled: Boolean = true
): Modifier {
    return this
        .accessibleClickable(onClick, label, Role.Button, enabled)
        .semantics {
            stateDescription = if (enabled) "Activé" else "Désactivé"
        }
}

/**
 * Marque un élément comme un toggle avec un état
 */
@Composable
fun Modifier.accessibleToggle(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
): Modifier {
    return this
        .accessibleClickable(
            onClick = { onCheckedChange(!checked) },
            label = label,
            role = Role.Checkbox
        )
        .semantics {
            stateDescription = if (checked) "Coché" else "Décoché"
            // toggleableState est géré automatiquement par le composant Switch
        }
}

/**
 * Ajoute une description détaillée pour les éléments complexes
 */
fun Modifier.accessibleDetailedDescription(description: String): Modifier {
    return this.semantics {
        // Utiliser contentDescription pour la description principale
        // et testTag pour stocker la description détaillée
        contentDescription = description
    }
}

/**
 * Marque un élément comme un champ de texte éditable
 */
fun Modifier.accessibleTextField(
    label: String,
    value: String,
    placeholder: String? = null
): Modifier {
    return this.semantics {
        this.text = androidx.compose.ui.text.AnnotatedString(value)
        contentDescription = label
        placeholder?.let {
            // Le placeholder peut être ajouté via le composant TextField lui-même
        }
    }
}

/**
 * Ajoute une indication de progression
 */
fun Modifier.accessibleProgress(
    label: String,
    progress: Float,
    max: Float = 100f
): Modifier {
    return this.semantics {
        contentDescription = "$label: ${(progress / max * 100).toInt()}%"
        progressBarRangeInfo = ProgressBarRangeInfo(
            current = progress,
            range = 0f..max
        )
    }
}

/**
 * Marque un élément comme un élément de liste
 */
fun Modifier.accessibleListItem(
    label: String,
    position: Int,
    total: Int
): Modifier {
    return this.semantics {
        contentDescription = "$label, élément $position sur $total"
        testTag = "list_item_$position"
    }
}

/**
 * Ajoute une indication de chargement
 */
fun Modifier.accessibleLoading(loading: Boolean): Modifier {
    return this.semantics {
        if (loading) {
            stateDescription = "Chargement en cours"
        }
    }
}

/**
 * Marque un élément comme un élément de navigation
 */
@Composable
fun Modifier.accessibleNavigation(
    label: String,
    onClick: () -> Unit
): Modifier {
    return this
        .accessibleClickable(onClick, label, Role.Tab)
        .semantics {
            selected = false
        }
}

/**
 * Ajoute une indication d'erreur
 */
fun Modifier.accessibleError(errorMessage: String): Modifier {
    return this.semantics {
        contentDescription = "Erreur: $errorMessage"
        stateDescription = "Erreur"
    }
}

/**
 * Marque un élément comme un élément de formulaire requis
 */
fun Modifier.accessibleRequired(required: Boolean = true): Modifier {
    return this.semantics {
        if (required) {
            stateDescription = "Requis"
        }
    }
}

/**
 * Ajoute une indication de focus
 */
fun Modifier.accessibleFocused(focused: Boolean): Modifier {
    return this.semantics {
        if (focused) {
            stateDescription = "Focalisé"
        }
    }
}

/**
 * Crée un conteneur accessible pour les groupes d'éléments
 */
@Composable
fun AccessibleContainer(
    label: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .semantics {
                contentDescription = label
                testTag = "accessible_container"
            }
    ) {
        content()
    }
}

