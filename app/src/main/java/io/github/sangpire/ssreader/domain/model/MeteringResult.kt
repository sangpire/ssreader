package io.github.sangpire.ssreader.domain.model

/**
 * 카메라 분석기에서 반환하는 측광 결과
 *
 * @property averageLuminance Y plane 평균값 (0-255)
 * @property calculatedEV 계산된 EV 값
 * @property timestamp 측정 시간 (밀리초)
 */
data class MeteringResult(
    val averageLuminance: Float,
    val calculatedEV: Float,
    val timestamp: Long = System.currentTimeMillis()
)
