package com.androidcontroldeck

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.androidcontroldeck.ui.ControlDeckApp
import com.androidcontroldeck.core.design.theme.DeckTheme
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = (application as ControlDeckApplication).container
        lifecycleScope.launch { container.profileRepository.prime(container.assetCache) }
        setContent {
            DeckTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    ControlDeckApp(container)
                }
            }
        }
    }
}
