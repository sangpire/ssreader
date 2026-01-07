package io.github.sangpire.ssreader.camera

import android.graphics.Bitmap
import android.graphics.ImageFormat
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import io.github.sangpire.ssreader.domain.ExposureCalculator
import io.github.sangpire.ssreader.domain.model.MeteringResult
import java.nio.ByteBuffer

/**
 * CameraX ImageAnalysis.Analyzer 구현체
 *
 * Y plane(휘도)의 평균값을 측정하여 노출값을 계산합니다.
 *
 * @property calculator 노출 계산기
 * @property iso 현재 ISO 값 (기본 100)
 * @property onMeteringResult 측광 결과 콜백
 */
class LightMeterAnalyzer(
    private val calculator: ExposureCalculator,
    private var iso: Int = 100,
    private val onMeteringResult: (MeteringResult) -> Unit
) : ImageAnalysis.Analyzer {

    private var lastAnalysisTimestamp = 0L
    private val analysisIntervalMs = 100L // 100ms 간격으로 분석

    /**
     * ISO 값 업데이트
     */
    fun updateIso(newIso: Int) {
        iso = newIso
    }

    override fun analyze(image: ImageProxy) {
        val currentTimestamp = System.currentTimeMillis()

        // 분석 간격 제한
        if (currentTimestamp - lastAnalysisTimestamp < analysisIntervalMs) {
            image.close()
            return
        }

        lastAnalysisTimestamp = currentTimestamp

        try {
            val luminance = calculateAverageLuminance(image)
            val ev = calculator.calculateEV(luminance, iso)

            val result = MeteringResult(
                averageLuminance = luminance,
                calculatedEV = ev,
                timestamp = currentTimestamp
            )

            onMeteringResult(result)
        } finally {
            image.close()
        }
    }

    /**
     * Y plane의 평균 휘도를 계산합니다.
     *
     * @param image 분석할 이미지
     * @return 평균 휘도 (0-255)
     */
    private fun calculateAverageLuminance(image: ImageProxy): Float {
        val yPlane = image.planes[0]
        val yBuffer = yPlane.buffer
        val yRowStride = yPlane.rowStride
        val yPixelStride = yPlane.pixelStride

        val width = image.width
        val height = image.height

        var totalLuminance = 0L
        var sampleCount = 0

        // 성능을 위해 일부 픽셀만 샘플링 (4x4 그리드)
        val stepX = maxOf(1, width / 32)
        val stepY = maxOf(1, height / 32)

        for (y in 0 until height step stepY) {
            for (x in 0 until width step stepX) {
                val index = y * yRowStride + x * yPixelStride
                if (index < yBuffer.capacity()) {
                    val luminance = yBuffer.get(index).toInt() and 0xFF
                    totalLuminance += luminance
                    sampleCount++
                }
            }
        }

        return if (sampleCount > 0) {
            totalLuminance.toFloat() / sampleCount
        } else {
            0f
        }
    }

    /**
     * 현재 프레임을 Bitmap으로 캡처합니다.
     *
     * @param image 캡처할 이미지
     * @return Bitmap 또는 null
     */
    fun captureBitmap(image: ImageProxy): Bitmap? {
        return try {
            val yPlane = image.planes[0]
            val uPlane = image.planes[1]
            val vPlane = image.planes[2]

            val width = image.width
            val height = image.height

            // YUV to RGB 변환
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val yBuffer = yPlane.buffer
            val uBuffer = uPlane.buffer
            val vBuffer = vPlane.buffer

            val yRowStride = yPlane.rowStride
            val uvRowStride = uPlane.rowStride
            val uvPixelStride = uPlane.pixelStride

            val pixels = IntArray(width * height)

            for (y in 0 until height) {
                for (x in 0 until width) {
                    val yIndex = y * yRowStride + x
                    val uvIndex = (y / 2) * uvRowStride + (x / 2) * uvPixelStride

                    val yValue = (yBuffer.get(yIndex).toInt() and 0xFF)
                    val uValue = (uBuffer.get(uvIndex).toInt() and 0xFF) - 128
                    val vValue = (vBuffer.get(uvIndex).toInt() and 0xFF) - 128

                    // YUV to RGB conversion
                    var r = (yValue + 1.370705 * vValue).toInt()
                    var g = (yValue - 0.337633 * uValue - 0.698001 * vValue).toInt()
                    var b = (yValue + 1.732446 * uValue).toInt()

                    r = r.coerceIn(0, 255)
                    g = g.coerceIn(0, 255)
                    b = b.coerceIn(0, 255)

                    pixels[y * width + x] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
                }
            }

            bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
            bitmap
        } catch (e: Exception) {
            null
        }
    }
}
