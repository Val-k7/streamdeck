package com.androidcontroldeck.data.storage

import android.content.Context
import android.graphics.BitmapFactory
import android.os.Trace
import androidx.collection.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.androidcontroldeck.data.model.Profile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AssetCache(private val context: Context) {
    private val cache = LruCache<String, ImageBitmap>(24)

    suspend fun loadIcon(name: String?): ImageBitmap? = withContext(Dispatchers.IO) {
        if (name.isNullOrBlank()) return@withContext null
        cache.get(name)?.let { return@withContext it }
        return@withContext runCatching {
            Trace.beginSection("AssetCache.icon")
            context.assets.open("icons/$name.png").use { input ->
                BitmapFactory.decodeStream(input)?.asImageBitmap()?.also { cache.put(name, it) }
            }
        }.onFailure {
            // silent fallback
        }.getOrNull().also { Trace.endSection() }
    }

    suspend fun preload(profile: Profile?) {
        if (profile == null) return
        profile.controls.forEach { control ->
            loadIcon(control.icon)
        }
    }
}
