package io.github.sangpire.ssreader.ui.lightmeter.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.sangpire.ssreader.ui.theme.SSReaderTheme

private val ShutterButtonSize = 72.dp
private val InnerCircleSize = 56.dp
private val BorderWidth = 4.dp

private val DefaultOuterColor = Color.White
private val FrozenOuterColor = Color(0xFFFFD700) // Gold
private val InnerColor = Color.White
private val PressedInnerColor = Color.LightGray

/**
 * 셔터 버튼 Composable
 *
 * 카메라 스타일의 원형 셔터 버튼입니다.
 * 클릭하면 화면이 정지/재개됩니다.
 *
 * @param isFrozen 현재 화면 정지 상태
 * @param onClick 클릭 콜백
 * @param contentDescription 접근성 설명
 * @param modifier Modifier
 */
@Composable
fun ShutterButton(
    isFrozen: Boolean,
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // 애니메이션
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.9f else 1f,
        animationSpec = spring(
            dampingRatio = 0.4f,
            stiffness = 400f
        ),
        label = "scale"
    )

    val outerColor by animateColorAsState(
        targetValue = if (isFrozen) FrozenOuterColor else DefaultOuterColor,
        label = "outerColor"
    )

    val innerColor by animateColorAsState(
        targetValue = if (isPressed) PressedInnerColor else InnerColor,
        label = "innerColor"
    )

    Box(
        modifier = modifier
            .size(ShutterButtonSize)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(CircleShape)
            .border(BorderWidth, outerColor, CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center
    ) {
        // 내부 원
        Box(
            modifier = Modifier
                .size(InnerCircleSize)
                .clip(CircleShape)
                .background(innerColor)
        )

        // 정지 상태일 때 중앙 표시
        if (isFrozen) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(FrozenOuterColor)
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF333333)
@Composable
private fun ShutterButtonPreview() {
    SSReaderTheme {
        ShutterButton(
            isFrozen = false,
            onClick = {},
            contentDescription = "Shutter"
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF333333)
@Composable
private fun ShutterButtonFrozenPreview() {
    SSReaderTheme {
        ShutterButton(
            isFrozen = true,
            onClick = {},
            contentDescription = "Resume"
        )
    }
}
