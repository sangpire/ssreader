package io.github.sangpire.ssreader.ui.lightmeter

import io.github.sangpire.ssreader.domain.ExposureCalculator
import io.github.sangpire.ssreader.domain.model.ExposureType
import io.github.sangpire.ssreader.domain.model.LightMeterState
import io.github.sangpire.ssreader.domain.model.MeteringResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LightMeterViewModelLockTest {

    private lateinit var viewModel: LightMeterViewModel
    private lateinit var calculator: ExposureCalculator
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        calculator = ExposureCalculator()
        viewModel = LightMeterViewModel(calculator)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `toggleLock toggles ISO lock state`() = runTest {
        viewModel.onCameraReady()
        advanceUntilIdle()

        // Initial - not locked
        var state = viewModel.state.value as LightMeterState.Ready
        assertFalse("ISO should not be locked initially", state.exposureSettings.iso.isLocked)

        // Toggle lock
        viewModel.toggleLock(ExposureType.ISO)
        advanceUntilIdle()

        state = viewModel.state.value as LightMeterState.Ready
        assertTrue("ISO should be locked after toggle", state.exposureSettings.iso.isLocked)

        // Toggle unlock
        viewModel.toggleLock(ExposureType.ISO)
        advanceUntilIdle()

        state = viewModel.state.value as LightMeterState.Ready
        assertFalse("ISO should be unlocked after second toggle", state.exposureSettings.iso.isLocked)
    }

    @Test
    fun `toggleLock toggles aperture lock state`() = runTest {
        viewModel.onCameraReady()
        advanceUntilIdle()

        viewModel.toggleLock(ExposureType.APERTURE)
        advanceUntilIdle()

        val state = viewModel.state.value as LightMeterState.Ready
        assertTrue("Aperture should be locked", state.exposureSettings.aperture.isLocked)
    }

    @Test
    fun `toggleLock toggles shutter speed lock state`() = runTest {
        viewModel.onCameraReady()
        advanceUntilIdle()

        viewModel.toggleLock(ExposureType.SHUTTER_SPEED)
        advanceUntilIdle()

        val state = viewModel.state.value as LightMeterState.Ready
        assertTrue("Shutter speed should be locked", state.exposureSettings.shutterSpeed.isLocked)
    }

    @Test
    fun `locked ISO value is preserved when metering result changes`() = runTest {
        viewModel.onCameraReady()
        advanceUntilIdle()

        // Lock ISO
        viewModel.toggleLock(ExposureType.ISO)
        advanceUntilIdle()

        var state = viewModel.state.value as LightMeterState.Ready
        val lockedIsoValue = state.exposureSettings.iso.value

        // Apply metering result
        viewModel.onMeteringResult(MeteringResult(averageLuminance = 200f, calculatedEV = 15f))
        advanceUntilIdle()

        state = viewModel.state.value as LightMeterState.Ready
        assertEquals("ISO value should be preserved", lockedIsoValue, state.exposureSettings.iso.value, 0.01)
        assertTrue("ISO should remain locked", state.exposureSettings.iso.isLocked)
    }

    @Test
    fun `locked aperture value is preserved when metering result changes`() = runTest {
        viewModel.onCameraReady()
        advanceUntilIdle()

        // Lock aperture
        viewModel.toggleLock(ExposureType.APERTURE)
        advanceUntilIdle()

        var state = viewModel.state.value as LightMeterState.Ready
        val lockedApertureValue = state.exposureSettings.aperture.value

        // Apply metering result
        viewModel.onMeteringResult(MeteringResult(averageLuminance = 200f, calculatedEV = 15f))
        advanceUntilIdle()

        state = viewModel.state.value as LightMeterState.Ready
        assertEquals("Aperture value should be preserved", lockedApertureValue, state.exposureSettings.aperture.value, 0.01)
        assertTrue("Aperture should remain locked", state.exposureSettings.aperture.isLocked)
    }

    @Test
    fun `unlocked values change when metering result changes with locked ISO`() = runTest {
        viewModel.onCameraReady()
        advanceUntilIdle()

        // First metering result
        viewModel.onMeteringResult(MeteringResult(averageLuminance = 100f, calculatedEV = 12f))
        advanceUntilIdle()

        var state = viewModel.state.value as LightMeterState.Ready
        val initialShutter = state.exposureSettings.shutterSpeed.value

        // Lock ISO
        viewModel.toggleLock(ExposureType.ISO)
        advanceUntilIdle()

        // Different metering result
        viewModel.onMeteringResult(MeteringResult(averageLuminance = 200f, calculatedEV = 15f))
        advanceUntilIdle()

        state = viewModel.state.value as LightMeterState.Ready
        assertNotEquals("Shutter speed should change with different EV", initialShutter, state.exposureSettings.shutterSpeed.value)
    }

    @Test
    fun `all values can be locked`() = runTest {
        viewModel.onCameraReady()
        advanceUntilIdle()

        viewModel.toggleLock(ExposureType.ISO)
        viewModel.toggleLock(ExposureType.APERTURE)
        viewModel.toggleLock(ExposureType.SHUTTER_SPEED)
        advanceUntilIdle()

        val state = viewModel.state.value as LightMeterState.Ready
        assertTrue("All values should be locked", state.exposureSettings.allLocked)
        assertEquals("Locked count should be 3", 3, state.exposureSettings.lockedCount)
    }
}
