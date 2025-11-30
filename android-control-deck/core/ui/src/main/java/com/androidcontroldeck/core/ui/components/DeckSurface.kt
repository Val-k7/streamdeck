package com.androidcontroldeck.core.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.androidcontroldeck.core.design.theme.LocalSpacing

@Composable
fun DeckSurface(
    modifier: Modifier = Modifier,
    paddingValues: PaddingValues = PaddingValues(LocalSpacing.current.gutter.dp),
    content: @Composable () -> Unit
) {
    Surface(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            content()
        }
    }
}
