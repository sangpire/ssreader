package io.github.sangpire.ssreader.ui.lightmeter

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import io.github.sangpire.ssreader.domain.ExposureCalculator
import io.github.sangpire.ssreader.domain.model.ErrorType
import io.github.sangpire.ssreader.domain.model.ExposureSettings
import io.github.sangpire.ssreader.domain.model.ExposureType
import io.github.sangpire.ssreader.domain.model.LightMeterState
import io.github.sangpire.ssreader.domain.model.MeteringResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * 노출계 화면 ViewModel
 *
 * 카메라 측광 결과를 처리하고 UI 상태를 관리합니다.
 */
class LightMeterViewModel(
    private val calculator: ExposureCalculator = ExposureCalculator()
) : ViewModel() {

    private val _state = MutableStateFlow<LightMeterState>(LightMeterState.Loading())
    val state: StateFlow<LightMeterState> = _state.asStateFlow()

    /**
     * 카메라 준비 완료 시 호출
     */
    fun onCameraReady() {
        _state.value = LightMeterState.Ready(
            exposureSettings = ExposureSettings.default()
        )
    }

    /**
     * 측광 결과 수신 시 호출
     *
     * @param result 측광 결과
     */
    fun onMeteringResult(result: MeteringResult) {
        val currentState = _state.value

        // 고정 상태에서는 무시
        if (currentState is LightMeterState.Ready && currentState.isFrozen) {
            return
        }

        if (currentState is LightMeterState.Ready) {
            val updatedSettings = calculator.calculateOptimalExposure(
                settings = currentState.exposureSettings,
                measuredEV = result.calculatedEV
            )

            _state.update { state ->
                if (state is LightMeterState.Ready) {
                    state.copy(exposureSettings = updatedSettings)
                } else {
                    state
                }
            }
        }
    }

    /**
     * 셔터 버튼 클릭 시 호출 (화면 고정/해제 토글)
     *
     * @param capturedBitmap 캡처된 비트맵 (고정 시)
     */
    fun onShutterClick(capturedBitmap: Bitmap?) {
        _state.update { state ->
            if (state is LightMeterState.Ready) {
                if (state.isFrozen) {
                    // 해제
                    state.copy(isFrozen = false, frozenBitmap = null)
                } else {
                    // 고정
                    state.copy(isFrozen = true, frozenBitmap = capturedBitmap)
                }
            } else {
                state
            }
        }
    }

    /**
     * 오류 발생 시 호출
     *
     * @param type 오류 유형
     * @param message 오류 메시지
     */
    fun onError(type: ErrorType, message: String) {
        _state.value = LightMeterState.Error(type = type, message = message)
    }

    /**
     * 오류 상태에서 재시도
     */
    fun onRetry() {
        _state.value = LightMeterState.Loading()
    }

    /**
     * 앱이 백그라운드에서 복귀할 때 호출
     *
     * 화면이 고정 상태였다면 실시간 모드로 복귀합니다.
     */
    fun onResume() {
        _state.update { state ->
            if (state is LightMeterState.Ready && state.isFrozen) {
                state.copy(isFrozen = false, frozenBitmap = null)
            } else {
                state
            }
        }
    }

    /**
     * 노출 값 고정/해제 토글
     *
     * @param type 토글할 노출 값 타입
     */
    fun toggleLock(type: ExposureType) {
        _state.update { state ->
            if (state is LightMeterState.Ready) {
                state.copy(exposureSettings = state.exposureSettings.toggleLock(type))
            } else {
                state
            }
        }
    }

    /**
     * 노출 값 변경
     *
     * 값을 변경하면 측정된 EV를 기준으로 다른 값들도 재계산됩니다.
     *
     * @param type 변경할 노출 값 타입
     * @param direction 방향 (1: 증가, -1: 감소)
     */
    fun changeExposureValue(type: ExposureType, direction: Int) {
        _state.update { state ->
            if (state is LightMeterState.Ready) {
                val currentValue = state.exposureSettings.getValue(type)
                val newValue = calculator.getNextStopValue(currentValue, direction)

                if (newValue != null) {
                    // 변경할 값 적용
                    val updatedSettings = state.exposureSettings.updateValue(type, newValue)

                    // 변경한 값을 임시로 locked 처리하여 재계산 시 유지
                    val settingsForRecalc = when (type) {
                        ExposureType.ISO -> updatedSettings.copy(
                            iso = updatedSettings.iso.copy(isLocked = true)
                        )
                        ExposureType.APERTURE -> updatedSettings.copy(
                            aperture = updatedSettings.aperture.copy(isLocked = true)
                        )
                        ExposureType.SHUTTER_SPEED -> updatedSettings.copy(
                            shutterSpeed = updatedSettings.shutterSpeed.copy(isLocked = true)
                        )
                    }

                    // 측정된 EV 기준으로 다른 값들 재계산
                    val recalculated = calculator.calculateOptimalExposure(
                        settingsForRecalc,
                        updatedSettings.measuredEV
                    )

                    // 원래 lock 상태로 복원
                    val finalSettings = when (type) {
                        ExposureType.ISO -> recalculated.copy(
                            iso = recalculated.iso.copy(isLocked = newValue.isLocked)
                        )
                        ExposureType.APERTURE -> recalculated.copy(
                            aperture = recalculated.aperture.copy(isLocked = newValue.isLocked)
                        )
                        ExposureType.SHUTTER_SPEED -> recalculated.copy(
                            shutterSpeed = recalculated.shutterSpeed.copy(isLocked = newValue.isLocked)
                        )
                    }

                    state.copy(exposureSettings = finalSettings)
                } else {
                    state
                }
            } else {
                state
            }
        }
    }
}
