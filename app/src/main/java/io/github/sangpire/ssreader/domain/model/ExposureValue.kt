package io.github.sangpire.ssreader.domain.model

/**
 * 카메라의 개별 노출 설정 값을 나타내는 데이터 클래스
 *
 * @property type 노출 값의 종류 (ISO, APERTURE, SHUTTER_SPEED)
 * @property value 실제 수치 값
 * @property displayValue 표시용 문자열 (예: "f/2.8", "1/125")
 * @property stopIndex 표준 스톱 목록에서의 인덱스
 * @property isLocked 고정 여부
 */
data class ExposureValue(
    val type: ExposureType,
    val value: Double,
    val displayValue: String,
    val stopIndex: Int,
    val isLocked: Boolean = false
) {
    companion object {
        /**
         * ISO 값 생성
         */
        fun iso(index: Int, isLocked: Boolean = false): ExposureValue {
            val value = ExposureConstants.ISO_VALUES.getOrNull(index) ?: ExposureConstants.ISO_VALUES[ExposureConstants.DEFAULT_ISO_INDEX]
            return ExposureValue(
                type = ExposureType.ISO,
                value = value.toDouble(),
                displayValue = value.toString(),
                stopIndex = index.coerceIn(0, ExposureConstants.ISO_VALUES.lastIndex),
                isLocked = isLocked
            )
        }

        /**
         * 조리개 값 생성
         */
        fun aperture(index: Int, isLocked: Boolean = false): ExposureValue {
            val safeIndex = index.coerceIn(0, ExposureConstants.APERTURE_VALUES.lastIndex)
            return ExposureValue(
                type = ExposureType.APERTURE,
                value = ExposureConstants.APERTURE_VALUES[safeIndex],
                displayValue = ExposureConstants.APERTURE_DISPLAY[safeIndex],
                stopIndex = safeIndex,
                isLocked = isLocked
            )
        }

        /**
         * 셔터스피드 값 생성
         */
        fun shutterSpeed(index: Int, isLocked: Boolean = false): ExposureValue {
            val safeIndex = index.coerceIn(0, ExposureConstants.SHUTTER_SPEED_VALUES.lastIndex)
            return ExposureValue(
                type = ExposureType.SHUTTER_SPEED,
                value = ExposureConstants.SHUTTER_SPEED_VALUES[safeIndex],
                displayValue = ExposureConstants.SHUTTER_SPEED_DISPLAY[safeIndex],
                stopIndex = safeIndex,
                isLocked = isLocked
            )
        }

        /**
         * 기본 ISO 값 (ISO 100)
         */
        fun defaultIso(): ExposureValue = iso(ExposureConstants.DEFAULT_ISO_INDEX)

        /**
         * 기본 조리개 값 (f/5.6)
         */
        fun defaultAperture(): ExposureValue = aperture(ExposureConstants.DEFAULT_APERTURE_INDEX)

        /**
         * 기본 셔터스피드 값 (1/125)
         */
        fun defaultShutterSpeed(): ExposureValue = shutterSpeed(ExposureConstants.DEFAULT_SHUTTER_SPEED_INDEX)
    }

    /**
     * 고정 상태 토글
     */
    fun toggleLock(): ExposureValue = copy(isLocked = !isLocked)

    /**
     * 인덱스 변경 (고정 상태로 전환)
     */
    fun withIndex(newIndex: Int): ExposureValue {
        return when (type) {
            ExposureType.ISO -> iso(newIndex, isLocked = true)
            ExposureType.APERTURE -> aperture(newIndex, isLocked = true)
            ExposureType.SHUTTER_SPEED -> shutterSpeed(newIndex, isLocked = true)
        }
    }

    /**
     * 최대 인덱스 반환
     */
    fun maxIndex(): Int = when (type) {
        ExposureType.ISO -> ExposureConstants.ISO_VALUES.lastIndex
        ExposureType.APERTURE -> ExposureConstants.APERTURE_VALUES.lastIndex
        ExposureType.SHUTTER_SPEED -> ExposureConstants.SHUTTER_SPEED_VALUES.lastIndex
    }
}
