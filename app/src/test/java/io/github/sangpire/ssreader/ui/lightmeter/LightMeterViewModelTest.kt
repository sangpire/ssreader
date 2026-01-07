package io.github.sangpire.ssreader.ui.lightmeter

import io.github.sangpire.ssreader.domain.ExposureCalculator
import io.github.sangpire.ssreader.domain.model.ExposureSettings
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LightMeterViewModelTest {

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
    fun `initial state is Loading`() = runTest {
        val state = viewModel.state.value
        assertTrue("Initial state should be Loading", state is LightMeterState.Loading)
    }

    @Test
    fun `onCameraReady changes state to Ready`() = runTest {
        viewModel.onCameraReady()
        advanceUntilIdle()

        val state = viewModel.state.value
        assertTrue("State should be Ready after camera is ready", state is LightMeterState.Ready)
    }

    @Test
    fun `onMeteringResult updates exposure settings`() = runTest {
        viewModel.onCameraReady()
        advanceUntilIdle()

        val result = MeteringResult(
            averageLuminance = 118f,
            calculatedEV = 13f
        )

        viewModel.onMeteringResult(result)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertTrue("State should be Ready", state is LightMeterState.Ready)

        val readyState = state as LightMeterState.Ready
        assertEquals("Measured EV should be updated", 13f, readyState.exposureSettings.measuredEV, 0.1f)
    }

    @Test
    fun `onMeteringResult is ignored when frozen`() = runTest {
        viewModel.onCameraReady()
        advanceUntilIdle()

        // First result
        val result1 = MeteringResult(averageLuminance = 118f, calculatedEV = 13f)
        viewModel.onMeteringResult(result1)
        advanceUntilIdle()

        // Freeze
        viewModel.onShutterClick(null)
        advanceUntilIdle()

        // Second result (should be ignored)
        val result2 = MeteringResult(averageLuminance = 200f, calculatedEV = 15f)
        viewModel.onMeteringResult(result2)
        advanceUntilIdle()

        val state = viewModel.state.value as LightMeterState.Ready
        assertTrue("Should be frozen", state.isFrozen)
        assertEquals("EV should not change when frozen", 13f, state.exposureSettings.measuredEV, 0.1f)
    }

    @Test
    fun `onShutterClick toggles frozen state`() = runTest {
        viewModel.onCameraReady()
        advanceUntilIdle()

        // Initial state - not frozen
        var state = viewModel.state.value as LightMeterState.Ready
        assertFalse("Initial state should not be frozen", state.isFrozen)

        // First click - freeze
        viewModel.onShutterClick(null)
        advanceUntilIdle()
        state = viewModel.state.value as LightMeterState.Ready
        assertTrue("Should be frozen after first click", state.isFrozen)

        // Second click - unfreeze
        viewModel.onShutterClick(null)
        advanceUntilIdle()
        state = viewModel.state.value as LightMeterState.Ready
        assertFalse("Should be unfrozen after second click", state.isFrozen)
    }

    @Test
    fun `onError changes state to Error`() = runTest {
        viewModel.onError(io.github.sangpire.ssreader.domain.model.ErrorType.CAMERA_UNAVAILABLE, "Camera error")
        advanceUntilIdle()

        val state = viewModel.state.value
        assertTrue("State should be Error", state is LightMeterState.Error)

        val errorState = state as LightMeterState.Error
        assertEquals("Error message should match", "Camera error", errorState.message)
    }
}
