package com.androidcontroldeck.ui.screens

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.androidcontroldeck.data.model.Action
import com.androidcontroldeck.data.model.ActionType
import com.androidcontroldeck.data.model.Control
import com.androidcontroldeck.data.model.ControlType
import com.androidcontroldeck.data.model.Profile
import com.androidcontroldeck.data.storage.AssetCache
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalCoroutinesApi::class)
class EditorScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val assetCache: AssetCache = mockk(relaxed = true)

    @Before
    fun setUp() {
        coEvery { assetCache.loadIcon(any()) } returns null
    }

    @Test
    fun `dragging control moves it within bounds`() {
        val control = Control(
            id = "move",
            type = ControlType.BUTTON,
            row = 0,
            col = 0,
            label = "Move",
            action = Action(type = ActionType.OBS, payload = "switch")
        )
        var latestProfile: Profile? = null
        val profile = Profile(
            id = "profile",
            name = "Layout",
            rows = 3,
            cols = 3,
            controls = listOf(control),
            version = 1
        )

        composeTestRule.setContent {
            EditorScreen(
                profile = profile,
                onProfileChanged = { latestProfile = it },
                onNavigateBack = {},
                assetCache = assetCache
            )
        }

        composeTestRule.onNodeWithTag("editor_control_move", useUnmergedTree = true)
            .performTouchInput {
                val start = center
                swipe(start, start + Offset(0f, 400f))
            }

        composeTestRule.runOnIdle {
            assertEquals(1, latestProfile?.controls?.first { it.id == "move" }?.row)
        }
    }

    @Test
    fun `resizing slider updates control span`() {
        val control = Control(
            id = "resize",
            type = ControlType.PAD,
            row = 0,
            col = 0,
            label = "Resize",
            action = Action(type = ActionType.OBS, payload = "resize")
        )
        var latestProfile: Profile? = null
        val profile = Profile(
            id = "profile",
            name = "Layout",
            rows = 2,
            cols = 3,
            controls = listOf(control),
            version = 1
        )

        composeTestRule.setContent {
            EditorScreen(
                profile = profile,
                onProfileChanged = { latestProfile = it },
                onNavigateBack = {},
                assetCache = assetCache
            )
        }

        composeTestRule.onNodeWithTag("editor_control_resize", useUnmergedTree = true)
            .performTouchInput {
                val start = center
                swipe(start, start + Offset(0f, 400f))
            }

        composeTestRule.onNodeWithTag("width_slider")
            .performSemanticsAction(SemanticsActions.SetProgress) { it(2f) }

        composeTestRule.runOnIdle {
            assertEquals(2, latestProfile?.controls?.first { it.id == "resize" }?.colSpan)
        }
    }
}
