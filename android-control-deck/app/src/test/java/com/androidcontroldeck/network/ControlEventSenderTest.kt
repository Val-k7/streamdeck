package com.androidcontroldeck.network

import com.androidcontroldeck.network.model.ControlPayload
import io.mockk.CapturedSlot
import io.mockk.capture
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import io.mockk.runs
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ControlEventSenderTest {
    @Test
    fun `serializes payload and forwards to websocket`() {
        val payloadSlot: CapturedSlot<String> = slot()
        val messageIdSlot: CapturedSlot<String?> = slot()
        val webSocketClient = mockk<WebSocketClient>(relaxed = true)
        every { webSocketClient.send(capture(payloadSlot), messageId = capture(messageIdSlot)) } just runs

        val sender = ControlEventSender(webSocketClient, Json { encodeDefaults = true })

        sender.send(controlId = "button_1", type = "tap", value = 0.5f, meta = mapOf("source" to "ui"))

        verify { webSocketClient.send(any(), messageId = any()) }

        val parsed = Json { ignoreUnknownKeys = true }.decodeFromString<ControlPayload>(payloadSlot.captured)
        assertEquals("button_1", parsed.controlId)
        assertEquals("tap", parsed.type)
        assertEquals(0.5f, parsed.value)
        assertEquals("ui", parsed.meta["source"])
        assertEquals(messageIdSlot.captured, parsed.messageId)
        assertTrue(parsed.sentAt > 0)
    }
}
