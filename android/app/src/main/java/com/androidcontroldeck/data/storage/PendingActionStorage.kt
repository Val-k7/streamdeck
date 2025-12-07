package com.androidcontroldeck.data.storage

import android.content.Context
import com.androidcontroldeck.data.model.PendingAction
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

class PendingActionStorage(
    context: Context,
    private val json: Json = Json { encodeDefaults = true; ignoreUnknownKeys = true }
) {
    private val prefs = context.getSharedPreferences("pending_actions", Context.MODE_PRIVATE)
    private val key = "pending_actions"

    fun load(): List<PendingAction> {
        val raw = prefs.getString(key, null) ?: return emptyList()
        return runCatching { json.decodeFromString(ListSerializer(PendingAction.serializer()), raw) }
            .getOrDefault(emptyList())
    }

    fun save(actions: List<PendingAction>) {
        val payload = json.encodeToString(ListSerializer(PendingAction.serializer()), actions)
        prefs.edit().putString(key, payload).apply()
    }

    fun clear() {
        prefs.edit().remove(key).apply()
    }
}
