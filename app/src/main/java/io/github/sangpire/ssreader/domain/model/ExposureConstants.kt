package io.github.sangpire.ssreader.domain.model

/**
 * 표준 노출 값 상수 정의 (Full Stop 단위)
 */
object ExposureConstants {

    /**
     * 표준 ISO 값 (Full Stop)
     */
    val ISO_VALUES: List<Int> = listOf(50, 100, 200, 400, 800, 1600, 3200, 6400)

    /**
     * 표준 조리개 값 (Full Stop) - f-number
     */
    val APERTURE_VALUES: List<Double> = listOf(1.0, 1.4, 2.0, 2.8, 4.0, 5.6, 8.0, 11.0, 16.0, 22.0, 32.0)

    /**
     * 표준 셔터스피드 값 (Full Stop) - 초 단위
     */
    val SHUTTER_SPEED_VALUES: List<Double> = listOf(
        1.0 / 8000, 1.0 / 4000, 1.0 / 2000, 1.0 / 1000,
        1.0 / 500, 1.0 / 250, 1.0 / 125, 1.0 / 60,
        1.0 / 30, 1.0 / 15, 1.0 / 8, 1.0 / 4,
        1.0 / 2, 1.0, 2.0, 4.0
    )

    /**
     * 조리개 표시 문자열
     */
    val APERTURE_DISPLAY: List<String> = listOf(
        "f/1", "f/1.4", "f/2", "f/2.8", "f/4", "f/5.6",
        "f/8", "f/11", "f/16", "f/22", "f/32"
    )

    /**
     * 셔터스피드 표시 문자열
     */
    val SHUTTER_SPEED_DISPLAY: List<String> = listOf(
        "1/8000", "1/4000", "1/2000", "1/1000",
        "1/500", "1/250", "1/125", "1/60",
        "1/30", "1/15", "1/8", "1/4",
        "1/2", "1\"", "2\"", "4\""
    )

    /**
     * 기본 ISO 인덱스 (ISO 100)
     */
    const val DEFAULT_ISO_INDEX = 1

    /**
     * 기본 조리개 인덱스 (f/5.6)
     */
    const val DEFAULT_APERTURE_INDEX = 5

    /**
     * 기본 셔터스피드 인덱스 (1/125)
     */
    const val DEFAULT_SHUTTER_SPEED_INDEX = 6

    /**
     * 중간 회색 기준값 (18% gray에 해당하는 Y plane 값)
     */
    const val REFERENCE_GRAY = 118f

    /**
     * EV 계산 시 기준 보정값
     */
    const val EV_BASE_OFFSET = 13f

    /**
     * 측정 가능한 EV 최소값
     */
    const val MIN_EV = -6f

    /**
     * 측정 가능한 EV 최대값
     */
    const val MAX_EV = 17f
}
