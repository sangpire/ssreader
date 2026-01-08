package io.github.sangpire.ssreader.ui.lightmeter.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.sangpire.ssreader.R
import io.github.sangpire.ssreader.domain.model.ExposureConstants
import io.github.sangpire.ssreader.domain.model.ExposureSettings
import io.github.sangpire.ssreader.domain.model.ExposureType
import io.github.sangpire.ssreader.domain.model.ExposureValue
import io.github.sangpire.ssreader.ui.theme.SSReaderTheme
import kotlin.math.roundToInt

private val LockedColor = Color(0xFFFFD700) // Gold
private val UnlockedColor = Color.White
private val LockedBorderColor = Color(0xFFFFD700).copy(alpha = 0.5f)
private val SwipeThresholdDp = 60.dp

/**
 * 노출 값 표시 Composable
 *
 * ISO, 조리개, 셔터스피드 값을 세로로 표시합니다.
 * 각 값을 탭하면 고정/해제를 토글합니다.
 * 좌우 스와이프로 값을 조절할 수 있습니다.
 *
 * @param exposureSettings 노출 설정
 * @param onToggleLock 고정 토글 콜백
 * @param onChangeValue 값 변경 콜백 (타입, 방향: 1=증가, -1=감소)
 * @param modifier Modifier
 */
@Composable
fun ExposureValueDisplay(
    exposureSettings: ExposureSettings,
    modifier: Modifier = Modifier,
    onToggleLock: (ExposureType) -> Unit = {},
    onChangeValue: (ExposureType, Int) -> Unit = { _, _ -> }
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Black.copy(alpha = 0.6f))
            .padding(16.dp),
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // ISO
        ExposureValueItem(
            label = stringResource(R.string.label_iso),
            value = exposureSettings.iso.displayValue,
            isLocked = exposureSettings.iso.isLocked,
            contentDescription = stringResource(R.string.cd_lock_iso),
            onToggleLock = { onToggleLock(ExposureType.ISO) },
            onSwipe = { direction -> onChangeValue(ExposureType.ISO, direction) }
        )

        // 조리개
        ExposureValueItem(
            label = stringResource(R.string.label_aperture),
            value = exposureSettings.aperture.displayValue,
            isLocked = exposureSettings.aperture.isLocked,
            contentDescription = stringResource(R.string.cd_lock_aperture),
            onToggleLock = { onToggleLock(ExposureType.APERTURE) },
            onSwipe = { direction -> onChangeValue(ExposureType.APERTURE, direction) }
        )

        // 셔터스피드
        ExposureValueItem(
            label = stringResource(R.string.label_shutter_speed),
            value = exposureSettings.shutterSpeed.displayValue,
            isLocked = exposureSettings.shutterSpeed.isLocked,
            contentDescription = stringResource(R.string.cd_lock_shutter),
            onToggleLock = { onToggleLock(ExposureType.SHUTTER_SPEED) },
            onSwipe = { direction -> onChangeValue(ExposureType.SHUTTER_SPEED, direction) }
        )

        Spacer(modifier = Modifier.height(8.dp))

        // EV 값
        val isTooBright = exposureSettings.measuredEV > ExposureConstants.MAX_EV
        val isTooDark = exposureSettings.measuredEV < ExposureConstants.MIN_EV
        val evColor = when {
            isTooBright -> Color(0xFFFF6B6B) // 경고: 빨간색
            isTooDark -> Color(0xFF4ECDC4) // 경고: 청록색
            else -> Color.White.copy(alpha = 0.7f)
        }

        Text(
            text = String.format("EV %.1f", exposureSettings.measuredEV),
            color = evColor,
            fontSize = 12.sp
        )

        // 범위 초과 경고 표시
        if (isTooBright) {
            Text(
                text = stringResource(R.string.warning_too_bright),
                color = Color(0xFFFF6B6B),
                fontSize = 10.sp
            )
        } else if (isTooDark) {
            Text(
                text = stringResource(R.string.warning_too_dark),
                color = Color(0xFF4ECDC4),
                fontSize = 10.sp
            )
        }

        // 노출 보정
        if (exposureSettings.exposureCompensation != 0f) {
            Text(
                text = String.format("%+.1f", exposureSettings.exposureCompensation),
                color = if (exposureSettings.exposureCompensation > 0) Color(0xFFFF6B6B) else Color(0xFF4ECDC4),
                fontSize = 12.sp
            )
        }
    }
}

/**
 * 개별 노출 값 표시 항목
 *
 * 탭하면 고정/해제를 토글하고, 좌우 스와이프로 값을 조절합니다.
 *
 * @param label 레이블 (예: "ISO")
 * @param value 표시 값 (예: "100")
 * @param isLocked 고정 상태
 * @param contentDescription 접근성 설명
 * @param onToggleLock 고정 토글 콜백
 * @param onSwipe 스와이프 콜백 (방향: 1=증가, -1=감소)
 */
@Composable
private fun ExposureValueItem(
    label: String,
    value: String,
    isLocked: Boolean,
    contentDescription: String,
    onToggleLock: () -> Unit,
    onSwipe: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(8.dp)
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val swipeThresholdPx = with(density) { SwipeThresholdDp.toPx() }

    // 드래그 오프셋 상태
    var dragOffset by remember { mutableFloatStateOf(0f) }

    // 애니메이션된 오프셋 (드래그 종료 후 원위치로 돌아감)
    val animatedOffset by animateFloatAsState(
        targetValue = dragOffset,
        animationSpec = spring(
            dampingRatio = 0.6f,
            stiffness = 400f
        ),
        label = "dragOffset"
    )

    // 스와이프 방향에 따른 배경색 애니메이션
    val swipeBackgroundColor by animateColorAsState(
        targetValue = when {
            dragOffset > swipeThresholdPx * 0.5f -> Color(0xFF4CAF50).copy(alpha = 0.3f)
            dragOffset < -swipeThresholdPx * 0.5f -> Color(0xFFFF5722).copy(alpha = 0.3f)
            isLocked -> Color.Black.copy(alpha = 0.3f)
            else -> Color.Transparent
        },
        label = "swipeBackground"
    )

    Row(
        modifier = modifier
            .clip(shape)
            .then(
                if (isLocked) {
                    Modifier.border(1.dp, LockedBorderColor, shape)
                } else {
                    Modifier
                }
            )
            .background(swipeBackgroundColor)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onToggleLock() }
                )
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = {
                        dragOffset = 0f
                    },
                    onDragEnd = {
                        // 임계값을 넘으면 값 변경
                        if (dragOffset > swipeThresholdPx) {
                            onSwipe(1) // 오른쪽: 증가
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        } else if (dragOffset < -swipeThresholdPx) {
                            onSwipe(-1) // 왼쪽: 감소
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                        // 원위치로 복귀
                        dragOffset = 0f
                    },
                    onDragCancel = {
                        dragOffset = 0f
                    },
                    onHorizontalDrag = { _, dragAmount ->
                        // 최대 드래그 범위 제한
                        dragOffset = (dragOffset + dragAmount).coerceIn(-swipeThresholdPx * 1.5f, swipeThresholdPx * 1.5f)
                    }
                )
            }
            .offset { IntOffset(animatedOffset.roundToInt(), 0) }
            .graphicsLayer {
                // 스와이프 시 약간의 기울기 효과
                rotationZ = (dragOffset / swipeThresholdPx) * 2f
            }
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .semantics { this.contentDescription = contentDescription },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End
    ) {
        Text(
            text = label,
            color = (if (isLocked) LockedColor else UnlockedColor).copy(alpha = 0.7f),
            fontSize = 12.sp
        )

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = value,
            color = if (isLocked) LockedColor else UnlockedColor,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )

        if (isLocked) {
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "🔒",
                fontSize = 14.sp
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF333333)
@Composable
private fun ExposureValueDisplayPreview() {
    SSReaderTheme {
        ExposureValueDisplay(
            exposureSettings = ExposureSettings(
                iso = ExposureValue.iso(1, isLocked = true),
                aperture = ExposureValue.aperture(5),
                shutterSpeed = ExposureValue.shutterSpeed(6),
                measuredEV = 13.5f,
                exposureCompensation = 0.5f
            )
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF333333)
@Composable
private fun ExposureValueDisplayAllLockedPreview() {
    SSReaderTheme {
        ExposureValueDisplay(
            exposureSettings = ExposureSettings(
                iso = ExposureValue.iso(1, isLocked = true),
                aperture = ExposureValue.aperture(5, isLocked = true),
                shutterSpeed = ExposureValue.shutterSpeed(6, isLocked = true),
                measuredEV = 13.5f,
                exposureCompensation = -1.0f
            )
        )
    }
}
