package com.androidcontroldeck.core.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.androidcontroldeck.core.design.theme.LocalSpacing

@Composable
fun DeckCard(
    modifier: Modifier = Modifier,
    tonalElevation: Float = 3f,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = tonalElevation.dp)
    ) {
        Column(modifier = Modifier.padding(LocalSpacing.current.lg.dp)) {
            content()
        }
    }
}
