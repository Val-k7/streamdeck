package com.androidcontroldeck.data.repository

import com.androidcontroldeck.data.model.Action
import com.androidcontroldeck.data.model.ActionType
import com.androidcontroldeck.data.model.Control
import com.androidcontroldeck.data.model.ControlType
import com.androidcontroldeck.data.model.Profile
import com.androidcontroldeck.data.storage.AssetCache
import com.androidcontroldeck.data.storage.ProfileSerializer
import com.androidcontroldeck.data.storage.ProfileStorage
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileRepositoryTest {
    private val dispatcher = StandardTestDispatcher()
    private val scope = TestScope(dispatcher)
    private val serializer = ProfileSerializer()
    private lateinit var storage: ProfileStorage

    @Before
    fun setUp() {
        storage = mockk(relaxed = true)
        coEvery { storage.save(any()) } just runs
    }

    @Test
    fun `creates default profile when none are persisted`() = runTest(dispatcher) {
        coEvery { storage.loadProfiles() } returns emptyList()

        val repository = ProfileRepository(storage, serializer, scope)
        advanceUntilIdle()

        val current = repository.currentProfile.value
        assertNotNull(current)
        assertEquals(1, repository.profiles.value.size)
        coVerify { storage.save(match { it.id == "default_profile" && !it.checksum.isNullOrBlank() }) }
    }

    @Test
    fun `upsert replaces existing profile and updates flows`() = runTest(dispatcher) {
        val initial = Profile(
            id = "demo",
            name = "Demo",
            rows = 2,
            cols = 2,
            controls = emptyList(),
            version = 1,
            checksum = "initial"
        )
        coEvery { storage.loadProfiles() } returns listOf(initial)

        val repository = ProfileRepository(storage, serializer, scope)
        advanceUntilIdle()

        val updated = initial.copy(name = "Updated", checksum = "")
        repository.upsertProfile(updated)
        advanceUntilIdle()

        assertEquals("Updated", repository.currentProfile.value?.name)
        assertEquals(1, repository.profiles.value.size)
        coVerify { storage.save(match { it.name == "Updated" && !it.checksum.isNullOrBlank() }) }
    }

    @Test
    fun `prime reloads data and preloads assets for current profile`() = runTest(dispatcher) {
        val control = Control(
            id = "ctrl",
            type = ControlType.BUTTON,
            row = 0,
            col = 0,
            label = "Tap",
            action = Action(type = ActionType.OBS, payload = "Start"),
            checksum = ""
        )
        val profile = Profile(
            id = "demo",
            name = "Demo",
            rows = 1,
            cols = 1,
            controls = listOf(control),
            version = 1,
            checksum = ""
        )
        val assetCache = mockk<AssetCache>(relaxed = true)
        coEvery { storage.loadProfiles() } returns listOf(profile)

        val repository = ProfileRepository(storage, serializer, scope)
        advanceUntilIdle()

        repository.prime(assetCache)
        advanceUntilIdle()

        coVerify { assetCache.preload(match { it?.id == profile.id }) }
    }
}
