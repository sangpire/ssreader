package io.github.sangpire.ssreader.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import io.github.sangpire.ssreader.domain.model.ExposureSettings

/**
 * 이미지에 텍스트 오버레이를 추가하는 유틸리티
 */
object ImageOverlayUtils {

    /**
     * 이미지에 노출 정보를 오버레이
     *
     * @param bitmap 원본 Bitmap
     * @param exposureSettings 노출 설정 정보
     * @return 오버레이가 추가된 새로운 Bitmap
     */
    fun overlayExposureInfo(
        bitmap: Bitmap,
        exposureSettings: ExposureSettings
    ): Bitmap {
        val result = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(result)

        // 텍스트 페인트 설정
        val textPaint = Paint().apply {
            color = Color.WHITE
            textSize = calculateTextSize(bitmap.width)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
            style = Paint.Style.FILL
            setShadowLayer(8f, 0f, 0f, Color.BLACK)
        }

        // 배경 페인트 설정 (반투명 검은색)
        val backgroundPaint = Paint().apply {
            color = Color.argb(180, 0, 0, 0)
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        // 노출 정보 텍스트 생성
        val isoText = "ISO ${exposureSettings.iso.value.toInt()}"
        val apertureText = "f/${exposureSettings.aperture.value}"
        val shutterText = formatShutterSpeed(exposureSettings.shutterSpeed.value)

        // 텍스트 크기 측정
        val textBounds = Rect()
        val padding = (textPaint.textSize * 0.5f).toInt()
        val lineSpacing = (textPaint.textSize * 0.3f).toInt()

        // 가장 긴 텍스트 찾기
        val texts = listOf(isoText, apertureText, shutterText)
        val maxWidth = texts.maxOf { text ->
            textPaint.getTextBounds(text, 0, text.length, textBounds)
            textBounds.width()
        }

        // 배경 박스 크기 계산
        val boxWidth = maxWidth + padding * 2
        val boxHeight = (textPaint.textSize.toInt() * 3) + (lineSpacing * 2) + (padding * 2)

        // 오른쪽 아래에 배치
        val boxLeft = bitmap.width - boxWidth - padding * 2
        val boxTop = bitmap.height - boxHeight - padding * 2
        val boxRight = bitmap.width - padding * 2
        val boxBottom = bitmap.height - padding * 2

        // 둥근 모서리 배경 그리기
        canvas.drawRoundRect(
            boxLeft.toFloat(),
            boxTop.toFloat(),
            boxRight.toFloat(),
            boxBottom.toFloat(),
            padding.toFloat(),
            padding.toFloat(),
            backgroundPaint
        )

        // 텍스트 그리기
        var textY = boxTop + padding + textPaint.textSize

        // ISO
        canvas.drawText(
            isoText,
            boxLeft.toFloat() + padding,
            textY,
            textPaint
        )

        // 조리개
        textY += textPaint.textSize + lineSpacing
        canvas.drawText(
            apertureText,
            boxLeft.toFloat() + padding,
            textY,
            textPaint
        )

        // 셔터 스피드
        textY += textPaint.textSize + lineSpacing
        canvas.drawText(
            shutterText,
            boxLeft.toFloat() + padding,
            textY,
            textPaint
        )

        return result
    }

    /**
     * 이미지 크기에 따라 적절한 텍스트 크기 계산
     *
     * @param imageWidth 이미지 너비
     * @return 텍스트 크기 (픽셀)
     */
    private fun calculateTextSize(imageWidth: Int): Float {
        // 이미지 너비의 약 4%를 텍스트 크기로 사용
        return (imageWidth * 0.04f).coerceIn(40f, 120f)
    }

    /**
     * 셔터 스피드를 읽기 쉬운 형식으로 포맷
     *
     * @param shutterSpeed 셔터 스피드 (초)
     * @return 포맷된 문자열
     */
    private fun formatShutterSpeed(shutterSpeed: Double): String {
        return when {
            shutterSpeed >= 1.0 -> {
                // 1초 이상: "2s", "4s" 등
                "${shutterSpeed.toInt()}s"
            }
            shutterSpeed >= 0.5 -> {
                // 0.5초 이상: "1/2" 등
                "1/${(1.0 / shutterSpeed).toInt()}"
            }
            else -> {
                // 1초 미만: "1/60", "1/125" 등
                val denominator = (1.0 / shutterSpeed).toInt()
                "1/$denominator"
            }
        }
    }
}
