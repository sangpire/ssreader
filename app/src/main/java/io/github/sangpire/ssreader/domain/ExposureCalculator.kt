package io.github.sangpire.ssreader.domain

import io.github.sangpire.ssreader.domain.model.ExposureConstants
import io.github.sangpire.ssreader.domain.model.ExposureSettings
import io.github.sangpire.ssreader.domain.model.ExposureType
import io.github.sangpire.ssreader.domain.model.ExposureValue
import kotlin.math.log2
import kotlin.math.roundToInt

/**
 * 노출 계산기
 *
 * EV 계산 및 적정 노출 설정 계산을 담당합니다.
 */
class ExposureCalculator {

    /**
     * 측정된 휘도와 ISO를 기반으로 EV(Exposure Value)를 계산합니다.
     *
     * EV = log2(luminance / REFERENCE_GRAY) + BASE_EV + log2(ISO / 100)
     *
     * @param luminance Y plane 평균 휘도 (0-255)
     * @param iso ISO 감도 값
     * @return 계산된 EV 값
     */
    fun calculateEV(luminance: Float, iso: Int): Float {
        if (luminance <= 0) return 0f

        val luminanceRatio = luminance / ExposureConstants.REFERENCE_GRAY
        val isoFactor = log2(iso / 100f)

        return log2(luminanceRatio) + ExposureConstants.EV_BASE_OFFSET + isoFactor
    }

    /**
     * 측정된 EV와 현재 설정을 기반으로 적정 노출 설정을 계산합니다.
     *
     * 고정된 값은 유지하고, 고정되지 않은 값만 조정합니다.
     *
     * @param settings 현재 노출 설정
     * @param measuredEV 측정된 EV 값
     * @return 계산된 적정 노출 설정
     */
    fun calculateOptimalExposure(settings: ExposureSettings, measuredEV: Float): ExposureSettings {
        var result = settings.withMeasuredEV(measuredEV)

        // 고정된 값들의 EV 기여도 계산
        val isoStops = if (settings.iso.isLocked) {
            log2(settings.iso.value / 100.0).toFloat()
        } else 0f

        val apertureStops = if (settings.aperture.isLocked) {
            // f-stop에서 EV 기여도: 2 * log2(f-number)
            (2 * log2(settings.aperture.value)).toFloat()
        } else 0f

        val shutterStops = if (settings.shutterSpeed.isLocked) {
            // 셔터스피드에서 EV 기여도: -log2(shutter_seconds)
            (-log2(settings.shutterSpeed.value)).toFloat()
        } else 0f

        // EV = log2(N²/t) + log2(ISO/100) 에서 역산
        // measuredEV = apertureStops/2 - shutterStops + isoStops + baseOffset
        // 필요한 총 EV 기여도
        val targetEV = measuredEV

        when {
            // 모든 값이 고정되면 현재 상태 유지
            settings.allLocked -> {
                // 노출 보정만 계산
            }

            // ISO만 고정
            settings.iso.isLocked && !settings.aperture.isLocked && !settings.shutterSpeed.isLocked -> {
                result = balanceApertureAndShutter(result, targetEV, isoStops)
            }

            // 조리개만 고정
            !settings.iso.isLocked && settings.aperture.isLocked && !settings.shutterSpeed.isLocked -> {
                result = balanceIsoAndShutter(result, targetEV, apertureStops)
            }

            // 셔터스피드만 고정
            !settings.iso.isLocked && !settings.aperture.isLocked && settings.shutterSpeed.isLocked -> {
                result = balanceIsoAndAperture(result, targetEV, shutterStops)
            }

            // ISO와 조리개 고정 -> 셔터스피드 계산
            settings.iso.isLocked && settings.aperture.isLocked && !settings.shutterSpeed.isLocked -> {
                val requiredShutterStops = apertureStops - isoStops - (targetEV - ExposureConstants.EV_BASE_OFFSET)
                val shutterIndex = findClosestShutterIndex(requiredShutterStops)
                result = result.copy(shutterSpeed = ExposureValue.shutterSpeed(shutterIndex))
            }

            // ISO와 셔터스피드 고정 -> 조리개 계산
            settings.iso.isLocked && !settings.aperture.isLocked && settings.shutterSpeed.isLocked -> {
                val requiredApertureStops = (targetEV - ExposureConstants.EV_BASE_OFFSET) + isoStops + shutterStops
                val apertureIndex = findClosestApertureIndex(requiredApertureStops)
                result = result.copy(aperture = ExposureValue.aperture(apertureIndex))
            }

            // 조리개와 셔터스피드 고정 -> ISO 계산
            !settings.iso.isLocked && settings.aperture.isLocked && settings.shutterSpeed.isLocked -> {
                val requiredIsoStops = (targetEV - ExposureConstants.EV_BASE_OFFSET) - apertureStops + shutterStops
                val isoIndex = findClosestIsoIndex(requiredIsoStops)
                result = result.copy(iso = ExposureValue.iso(isoIndex))
            }

            // 아무것도 고정되지 않음 -> 기본값 사용하며 균형 맞춤
            else -> {
                result = balanceAllValues(result, targetEV)
            }
        }

        // 노출 보정 계산
        val compensation = calculateExposureCompensation(result, measuredEV)
        result = result.withExposureCompensation(compensation)

        return result
    }

    /**
     * 현재 설정과 측정된 EV 사이의 노출 보정값을 계산합니다.
     *
     * @param settings 현재 노출 설정
     * @param measuredEV 측정된 EV 값
     * @return 노출 보정 스톱 (양수: 과다노출, 음수: 부족노출)
     */
    fun calculateExposureCompensation(settings: ExposureSettings, measuredEV: Float): Float {
        // 현재 설정의 EV 계산
        val isoStops = log2(settings.iso.value / 100.0).toFloat()
        val apertureStops = (2 * log2(settings.aperture.value)).toFloat()
        val shutterStops = (-log2(settings.shutterSpeed.value)).toFloat()

        val settingsEV = apertureStops - shutterStops + isoStops + ExposureConstants.EV_BASE_OFFSET

        return settingsEV - measuredEV
    }

    /**
     * 현재 값에서 다음 스톱 값을 반환합니다.
     *
     * @param current 현재 노출 값
     * @param direction 방향 (1: 다음, -1: 이전)
     * @return 다음 스톱 값, 경계에 도달하면 null
     */
    fun getNextStopValue(current: ExposureValue, direction: Int): ExposureValue? {
        val newIndex = current.stopIndex + direction

        return when (current.type) {
            ExposureType.ISO -> {
                if (newIndex < 0 || newIndex > ExposureConstants.ISO_VALUES.lastIndex) null
                else ExposureValue.iso(newIndex, current.isLocked)
            }
            ExposureType.APERTURE -> {
                if (newIndex < 0 || newIndex > ExposureConstants.APERTURE_VALUES.lastIndex) null
                else ExposureValue.aperture(newIndex, current.isLocked)
            }
            ExposureType.SHUTTER_SPEED -> {
                if (newIndex < 0 || newIndex > ExposureConstants.SHUTTER_SPEED_VALUES.lastIndex) null
                else ExposureValue.shutterSpeed(newIndex, current.isLocked)
            }
        }
    }

    // Private helper methods

    private fun balanceApertureAndShutter(
        settings: ExposureSettings,
        targetEV: Float,
        isoStops: Float
    ): ExposureSettings {
        // 기본 조리개 유지, 셔터스피드 조정
        val apertureStops = (2 * log2(settings.aperture.value)).toFloat()
        val requiredShutterStops = apertureStops - isoStops - (targetEV - ExposureConstants.EV_BASE_OFFSET)
        val shutterIndex = findClosestShutterIndex(requiredShutterStops)
        return settings.copy(shutterSpeed = ExposureValue.shutterSpeed(shutterIndex))
    }

    private fun balanceIsoAndShutter(
        settings: ExposureSettings,
        targetEV: Float,
        apertureStops: Float
    ): ExposureSettings {
        // 기본 ISO 유지, 셔터스피드 조정
        val isoStops = log2(settings.iso.value / 100.0).toFloat()
        val requiredShutterStops = apertureStops - isoStops - (targetEV - ExposureConstants.EV_BASE_OFFSET)
        val shutterIndex = findClosestShutterIndex(requiredShutterStops)
        return settings.copy(shutterSpeed = ExposureValue.shutterSpeed(shutterIndex))
    }

    private fun balanceIsoAndAperture(
        settings: ExposureSettings,
        targetEV: Float,
        shutterStops: Float
    ): ExposureSettings {
        // 기본 ISO 유지, 조리개 조정
        val isoStops = log2(settings.iso.value / 100.0).toFloat()
        val requiredApertureStops = (targetEV - ExposureConstants.EV_BASE_OFFSET) + isoStops + shutterStops
        val apertureIndex = findClosestApertureIndex(requiredApertureStops)
        return settings.copy(aperture = ExposureValue.aperture(apertureIndex))
    }

    private fun balanceAllValues(settings: ExposureSettings, targetEV: Float): ExposureSettings {
        // 기본값 사용: ISO 100, f/5.6 기준으로 셔터스피드 조정
        val isoStops = log2(settings.iso.value / 100.0).toFloat()
        val apertureStops = (2 * log2(settings.aperture.value)).toFloat()
        val requiredShutterStops = apertureStops - isoStops - (targetEV - ExposureConstants.EV_BASE_OFFSET)
        val shutterIndex = findClosestShutterIndex(requiredShutterStops)
        return settings.copy(shutterSpeed = ExposureValue.shutterSpeed(shutterIndex))
    }

    private fun findClosestIsoIndex(stops: Float): Int {
        // stops = log2(ISO / 100)
        // ISO = 100 * 2^stops
        val targetIso = 100 * Math.pow(2.0, stops.toDouble())
        return ExposureConstants.ISO_VALUES.indices.minByOrNull { index ->
            kotlin.math.abs(ExposureConstants.ISO_VALUES[index] - targetIso)
        } ?: ExposureConstants.DEFAULT_ISO_INDEX
    }

    private fun findClosestApertureIndex(stops: Float): Int {
        // stops = 2 * log2(f-number)
        // f-number = 2^(stops/2)
        val targetAperture = Math.pow(2.0, stops.toDouble() / 2)
        return ExposureConstants.APERTURE_VALUES.indices.minByOrNull { index ->
            kotlin.math.abs(ExposureConstants.APERTURE_VALUES[index] - targetAperture)
        } ?: ExposureConstants.DEFAULT_APERTURE_INDEX
    }

    private fun findClosestShutterIndex(stops: Float): Int {
        // stops = -log2(shutter_seconds)
        // shutter_seconds = 2^(-stops)
        val targetShutter = Math.pow(2.0, -stops.toDouble())
        return ExposureConstants.SHUTTER_SPEED_VALUES.indices.minByOrNull { index ->
            kotlin.math.abs(
                log2(ExposureConstants.SHUTTER_SPEED_VALUES[index]) - log2(targetShutter)
            )
        } ?: ExposureConstants.DEFAULT_SHUTTER_SPEED_INDEX
    }
}
