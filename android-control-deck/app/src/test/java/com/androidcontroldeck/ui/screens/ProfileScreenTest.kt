package com.androidcontroldeck.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNode
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.androidcontroldeck.data.model.Action
import com.androidcontroldeck.data.model.ActionType
import com.androidcontroldeck.data.model.Control
import com.androidcontroldeck.data.model.ControlType
import com.androidcontroldeck.data.model.Profile
import com.androidcontroldeck.data.storage.AssetCache
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalCoroutinesApi::class)
class ProfileScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val assetCache: AssetCache = mockk(relaxed = true)

    @Before
    fun setUp() {
        coEvery { assetCache.loadIcon(any()) } returns null
    }

    @Test
    fun `shows loading placeholder when profile is null`() {
        composeTestRule.setContent {
            ProfileScreen(
                profile = null,
                onControlEvent = { _, _ -> },
                onNavigateEditor = {},
                onNavigateSettings = {},
                assetCache = assetCache
            )
        }

        composeTestRule.onNodeWithText("Chargement du profil...").assertIsDisplayed()
    }

    @Test
    fun `renders controls and propagates interactions`() {
        val events = mutableListOf<Float>()
        val controls = listOf(
            Control(
                id = "button",
                type = ControlType.BUTTON,
                row = 0,
                col = 0,
                label = "Start",
                colorHex = "#FF5722",
                action = Action(type = ActionType.OBS, payload = "start")
            ),
            Control(
                id = "toggle",
                type = ControlType.TOGGLE,
                row = 0,
                col = 1,
                label = "Mic",
                icon = "mic",
                action = Action(type = ActionType.AUDIO, payload = "mic")
            )
        )
        val profile = Profile(
            id = "profile1",
            name = "Deck",
            rows = 1,
            cols = 2,
            controls = controls,
            version = 1
        )

        composeTestRule.setContent {
            ProfileScreen(
                profile = profile,
                onControlEvent = { _, value -> events.add(value) },
                onNavigateEditor = {},
                onNavigateSettings = {},
                assetCache = assetCache
            )
        }

        composeTestRule.onNodeWithTag("control_button", useUnmergedTree = true).performClick()
        composeTestRule.onNode(
            hasClickAction() and hasAnyAncestor(hasTestTag("control_toggle")),
            useUnmergedTree = true
        ).performClick()

        composeTestRule.runOnIdle {
            assertEquals(listOf(1f, 1f), events)
        }

        composeTestRule.runOnIdle {
            coVerify { assetCache.loadIcon("mic") }
        }
    }
}
