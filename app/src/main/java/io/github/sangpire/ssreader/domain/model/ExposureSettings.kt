package io.github.sangpire.ssreader.domain.model

/**
 * 현재 화면에 표시되는 전체 노출 설정 상태
 *
 * @property iso ISO 값
 * @property aperture 조리개 값
 * @property shutterSpeed 셔터스피드 값
 * @property measuredEV 측정된 EV 값
 * @property exposureCompensation 노출 보정 스톱 (현재 설정과 적정 노출의 차이)
 */
data class ExposureSettings(
    val iso: ExposureValue = ExposureValue.defaultIso(),
    val aperture: ExposureValue = ExposureValue.defaultAperture(),
    val shutterSpeed: ExposureValue = ExposureValue.defaultShutterSpeed(),
    val measuredEV: Float = 0f,
    val exposureCompensation: Float = 0f
) {
    /**
     * 모든 값이 고정되어 있는지 확인
     */
    val allLocked: Boolean
        get() = iso.isLocked && aperture.isLocked && shutterSpeed.isLocked

    /**
     * 고정된 값의 개수
     */
    val lockedCount: Int
        get() = listOf(iso.isLocked, aperture.isLocked, shutterSpeed.isLocked).count { it }

    /**
     * 특정 타입의 값 가져오기
     */
    fun getValue(type: ExposureType): ExposureValue = when (type) {
        ExposureType.ISO -> iso
        ExposureType.APERTURE -> aperture
        ExposureType.SHUTTER_SPEED -> shutterSpeed
    }

    /**
     * 특정 타입의 값 업데이트
     */
    fun updateValue(type: ExposureType, value: ExposureValue): ExposureSettings = when (type) {
        ExposureType.ISO -> copy(iso = value)
        ExposureType.APERTURE -> copy(aperture = value)
        ExposureType.SHUTTER_SPEED -> copy(shutterSpeed = value)
    }

    /**
     * 특정 타입의 고정 상태 토글
     */
    fun toggleLock(type: ExposureType): ExposureSettings = when (type) {
        ExposureType.ISO -> copy(iso = iso.toggleLock())
        ExposureType.APERTURE -> copy(aperture = aperture.toggleLock())
        ExposureType.SHUTTER_SPEED -> copy(shutterSpeed = shutterSpeed.toggleLock())
    }

    /**
     * 측정된 EV 값 업데이트
     */
    fun withMeasuredEV(ev: Float): ExposureSettings = copy(measuredEV = ev)

    /**
     * 노출 보정 값 업데이트
     */
    fun withExposureCompensation(compensation: Float): ExposureSettings = copy(exposureCompensation = compensation)

    companion object {
        /**
         * 기본 설정 (모든 값 비고정)
         */
        fun default(): ExposureSettings = ExposureSettings()
    }
}
