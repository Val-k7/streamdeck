package com.androidcontroldeck.logging

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.runBlocking
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

private val dateFormatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.US)

class UnifiedLogger(
    context: Context,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
) {
    private val logDir = File(context.filesDir, "logs").apply { mkdirs() }
    private val logFile = File(logDir, "controldeck.log")
    private val mutex = Mutex()

    private val timers = mutableMapOf<String, Long>()

    data class LogEntry(
        val level: String,
        val message: String,
        val metadata: Map<String, Any?> = emptyMap(),
        val timestamp: Long = System.currentTimeMillis(),
        val throwable: Throwable? = null,
    )

    fun logInfo(message: String, metadata: Map<String, Any?> = emptyMap()) {
        write(LogEntry("INFO", message, metadata))
    }

    fun logDebug(message: String, metadata: Map<String, Any?> = emptyMap()) {
        write(LogEntry("DEBUG", message, metadata))
    }

    fun logError(message: String, throwable: Throwable? = null, metadata: Map<String, Any?> = emptyMap()) {
        write(LogEntry("ERROR", message, metadata, throwable = throwable))
    }

    fun logNetworkError(message: String, throwable: Throwable? = null, metadata: Map<String, Any?> = emptyMap()) {
        logError("Network: $message", throwable, metadata)
    }

    fun startTimedEvent(name: String, metadata: Map<String, Any?> = emptyMap()): String {
        val id = UUID.randomUUID().toString()
        timers[id] = System.currentTimeMillis()
        logDebug("Start $name", metadata + mapOf("eventId" to id))
        return id
    }

    fun endTimedEvent(id: String, name: String, metadata: Map<String, Any?> = emptyMap()) {
        val started = timers.remove(id)
        val duration = started?.let { System.currentTimeMillis() - it }
        logInfo(
            "End $name",
            metadata + mapOf(
                "eventId" to id,
                "durationMs" to duration,
            )
        )
    }

    suspend fun getLogFile(): File = mutex.withLock { logFile }

    fun exportCompressedLogs(): File {
        val archive = File(logDir, "controldeck-logs.zip")
        runBlocking { createArchive(archive) }
        return archive
    }

    private suspend fun createArchive(target: File) {
        mutex.withLock {
            FileOutputStream(target).use { fos ->
                ZipOutputStream(fos).use { zos ->
                    zos.putNextEntry(ZipEntry(logFile.name))
                    if (logFile.exists()) {
                        logFile.inputStream().use { input ->
                            input.copyTo(zos)
                        }
                    }
                    zos.closeEntry()
                }
            }
        }
    }

    private fun write(entry: LogEntry) {
        scope.launch {
            mutex.withLock {
                val formatted = buildString {
                    append(dateFormatter.format(Date(entry.timestamp)))
                    append(" [").append(entry.level).append("] ")
                    append(entry.message)
                    if (entry.metadata.isNotEmpty()) append(" | ").append(entry.metadata)
                    entry.throwable?.let { append(" | error=").append(it.message) }
                    append('\n')
                }
                logFile.appendText(formatted)
            }
        }
    }
}
