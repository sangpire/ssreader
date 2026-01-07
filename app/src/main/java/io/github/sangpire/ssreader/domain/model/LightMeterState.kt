package io.github.sangpire.ssreader.domain.model

import android.graphics.Bitmap

/**
 * 화면 전체의 UI 상태를 나타내는 sealed class
 */
sealed class LightMeterState {

    /**
     * 카메라 초기화 중
     */
    data class Loading(val message: String? = null) : LightMeterState()

    /**
     * 실시간 측광 중 (기본 상태)
     */
    data class Ready(
        val exposureSettings: ExposureSettings,
        val isFrozen: Boolean = false,
        val frozenBitmap: Bitmap? = null
    ) : LightMeterState()

    /**
     * 오류 상태
     */
    data class Error(
        val type: ErrorType,
        val message: String
    ) : LightMeterState()
}

/**
 * 오류 유형
 */
enum class ErrorType {
    /** 카메라 권한 거부됨 */
    CAMERA_PERMISSION_DENIED,
    /** 카메라 사용 불가 */
    CAMERA_UNAVAILABLE,
    /** 이미지 분석 실패 */
    ANALYSIS_FAILED
}
