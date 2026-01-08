package io.github.sangpire.ssreader.domain

import io.github.sangpire.ssreader.domain.model.ExposureConstants
import io.github.sangpire.ssreader.domain.model.ExposureSettings
import io.github.sangpire.ssreader.domain.model.ExposureType
import io.github.sangpire.ssreader.domain.model.ExposureValue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ExposureCalculatorTest {

    private lateinit var calculator: ExposureCalculator

    @Before
    fun setup() {
        calculator = ExposureCalculator()
    }

    @Test
    fun `calculateEV returns reasonable value for middle gray luminance`() {
        // 중간 회색 (Y = 118)에서 ISO 100 기준 EV 약 13 (맑은 날 야외 기준)
        val luminance = ExposureConstants.REFERENCE_GRAY
        val ev = calculator.calculateEV(luminance, 100)

        assertTrue("EV should be around 13 for middle gray at ISO 100", ev in 10f..16f)
    }

    @Test
    fun `calculateEV increases with higher luminance`() {
        val ev1 = calculator.calculateEV(50f, 100)
        val ev2 = calculator.calculateEV(100f, 100)
        val ev3 = calculator.calculateEV(200f, 100)

        assertTrue("Higher luminance should result in higher EV", ev1 < ev2 && ev2 < ev3)
    }

    @Test
    fun `calculateEV adjusts for ISO`() {
        val evIso100 = calculator.calculateEV(118f, 100)
        val evIso200 = calculator.calculateEV(118f, 200)
        val evIso400 = calculator.calculateEV(118f, 400)

        // ISO가 두 배가 되면 EV가 1 증가
        assertTrue("Doubling ISO should increase EV by ~1", (evIso200 - evIso100) in 0.8f..1.2f)
        assertTrue("Quadrupling ISO should increase EV by ~2", (evIso400 - evIso100) in 1.8f..2.2f)
    }

    @Test
    fun `calculateOptimalExposure returns valid settings when all unlocked`() {
        val settings = ExposureSettings.default()
        val measuredEV = 13f

        val result = calculator.calculateOptimalExposure(settings, measuredEV)

        assertNotNull("Result should not be null", result)
        assertEquals("Measured EV should be set", measuredEV, result.measuredEV)
    }

    @Test
    fun `calculateOptimalExposure keeps locked ISO value`() {
        val lockedIso = ExposureValue.iso(1, isLocked = true) // ISO 100 locked
        val settings = ExposureSettings.default().copy(iso = lockedIso)
        val measuredEV = 13f

        val result = calculator.calculateOptimalExposure(settings, measuredEV)

        assertEquals("ISO should remain locked", lockedIso.value, result.iso.value, 0.01)
        assertTrue("ISO should still be locked", result.iso.isLocked)
    }

    @Test
    fun `calculateOptimalExposure keeps locked aperture value`() {
        val lockedAperture = ExposureValue.aperture(5, isLocked = true) // f/5.6 locked
        val settings = ExposureSettings.default().copy(aperture = lockedAperture)
        val measuredEV = 13f

        val result = calculator.calculateOptimalExposure(settings, measuredEV)

        assertEquals("Aperture should remain locked", lockedAperture.value, result.aperture.value, 0.01)
        assertTrue("Aperture should still be locked", result.aperture.isLocked)
    }

    @Test
    fun `calculateOptimalExposure calculates shutter when ISO and aperture locked`() {
        val lockedIso = ExposureValue.iso(1, isLocked = true) // ISO 100
        val lockedAperture = ExposureValue.aperture(5, isLocked = true) // f/5.6
        val settings = ExposureSettings.default().copy(iso = lockedIso, aperture = lockedAperture)
        val measuredEV = 13f

        val result = calculator.calculateOptimalExposure(settings, measuredEV)

        assertTrue("ISO should be locked", result.iso.isLocked)
        assertTrue("Aperture should be locked", result.aperture.isLocked)
        // 셔터스피드는 자동 계산되므로 고정되지 않음
    }

    @Test
    fun `calculateExposureCompensation returns 0 for correct exposure`() {
        val settings = ExposureSettings.default()
        val measuredEV = 13f

        // 적정 노출에서 보정값은 0에 가까워야 함
        val optimal = calculator.calculateOptimalExposure(settings, measuredEV)
        val compensation = calculator.calculateExposureCompensation(optimal, measuredEV)

        assertTrue("Compensation should be near 0 for optimal exposure", compensation in -0.5f..0.5f)
    }

    @Test
    fun `getNextStopValue returns next ISO value`() {
        val currentIso = ExposureValue.iso(1) // ISO 100

        val nextIso = calculator.getNextStopValue(currentIso, 1)
        val prevIso = calculator.getNextStopValue(currentIso, -1)

        assertNotNull("Next ISO should not be null", nextIso)
        assertEquals("Next ISO should be 200", 200.0, nextIso!!.value, 0.01)

        assertNotNull("Previous ISO should not be null", prevIso)
        assertEquals("Previous ISO should be 50", 50.0, prevIso!!.value, 0.01)
    }

    @Test
    fun `getNextStopValue returns null at boundaries`() {
        val minIso = ExposureValue.iso(0) // ISO 50
        val maxIso = ExposureValue.iso(ExposureConstants.ISO_VALUES.lastIndex) // ISO 6400

        val belowMin = calculator.getNextStopValue(minIso, -1)
        val aboveMax = calculator.getNextStopValue(maxIso, 1)

        assertEquals("Should return null below minimum", null, belowMin)
        assertEquals("Should return null above maximum", null, aboveMax)
    }

    @Test
    fun `getNextStopValue returns next aperture value`() {
        val currentAperture = ExposureValue.aperture(5) // f/5.6

        val nextAperture = calculator.getNextStopValue(currentAperture, 1)
        val prevAperture = calculator.getNextStopValue(currentAperture, -1)

        assertNotNull("Next aperture should not be null", nextAperture)
        assertEquals("Next aperture should be f/8", 8.0, nextAperture!!.value, 0.01)

        assertNotNull("Previous aperture should not be null", prevAperture)
        assertEquals("Previous aperture should be f/4", 4.0, prevAperture!!.value, 0.01)
    }

    @Test
    fun `getNextStopValue returns next shutter speed value`() {
        val currentShutter = ExposureValue.shutterSpeed(6) // 1/125

        val nextShutter = calculator.getNextStopValue(currentShutter, 1)
        val prevShutter = calculator.getNextStopValue(currentShutter, -1)

        assertNotNull("Next shutter should not be null", nextShutter)
        // 다음 셔터스피드는 1/60 (더 느림)
        assertTrue("Next shutter should be slower", nextShutter!!.value > currentShutter.value)

        assertNotNull("Previous shutter should not be null", prevShutter)
        // 이전 셔터스피드는 1/250 (더 빠름)
        assertTrue("Previous shutter should be faster", prevShutter!!.value < currentShutter.value)
    }
}
