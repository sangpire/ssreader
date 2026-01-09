package io.github.sangpire.ssreader.ui.lightmeter

import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.sangpire.ssreader.util.ScreenshotUtils
import kotlinx.coroutines.delay
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.sangpire.ssreader.R
import io.github.sangpire.ssreader.camera.LightMeterAnalyzer
import io.github.sangpire.ssreader.domain.ExposureCalculator
import io.github.sangpire.ssreader.domain.model.ErrorType
import io.github.sangpire.ssreader.domain.model.ExposureType
import io.github.sangpire.ssreader.domain.model.LightMeterState
import io.github.sangpire.ssreader.ui.lightmeter.components.CameraPreview
import io.github.sangpire.ssreader.ui.lightmeter.components.ExposureValueDisplay
import io.github.sangpire.ssreader.ui.lightmeter.components.ShutterButton

/**
 * 노출계 메인 화면
 *
 * 전체 화면 카메라 프리뷰와 오른쪽 아래 노출 값 표시
 */
@Composable
fun LightMeterScreen(
    modifier: Modifier = Modifier,
    viewModel: LightMeterViewModel = viewModel { LightMeterViewModel() }
) {
    val state by viewModel.state.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current

    // 백그라운드에서 복귀 시 실시간 모드로 복귀
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.onResume()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val calculator = remember { ExposureCalculator() }
    val analyzer = remember {
        LightMeterAnalyzer(
            calculator = calculator,
            onMeteringResult = { result ->
                viewModel.onMeteringResult(result)
            }
        )
    }

    // PreviewView에서 Bitmap 캡처를 위한 콜백 저장
    var captureCallback by remember { mutableStateOf<(() -> Bitmap?)?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // 카메라 프리뷰: Loading 또는 Ready 상태에서 렌더링 (초기화 트리거)
        if (state !is LightMeterState.Error) {
            val readyState = state as? LightMeterState.Ready
            CameraPreview(
                modifier = Modifier.fillMaxSize(),
                isFrozen = readyState?.isFrozen ?: false,
                frozenBitmap = readyState?.frozenBitmap,
                analyzer = analyzer,
                onCameraReady = { viewModel.onCameraReady() },
                onCameraError = { message ->
                    viewModel.onError(ErrorType.CAMERA_UNAVAILABLE, message)
                },
                onCaptureCallbackReady = { callback -> captureCallback = callback }
            )
        }

        when (val currentState = state) {
            is LightMeterState.Loading -> {
                // 스피너 오버레이
                LoadingContent(message = currentState.message)
            }

            is LightMeterState.Ready -> {
                // 노출 값 표시 및 컨트롤 UI
                ReadyContentOverlay(
                    state = currentState,
                    captureCallback = captureCallback,
                    onToggleLock = { type -> viewModel.toggleLock(type) },
                    onChangeValue = { type, direction -> viewModel.changeExposureValue(type, direction) },
                    onShutterClick = { bitmap -> viewModel.onShutterClick(bitmap) },
                    onHideShutterButton = { viewModel.hideShutterButton() },
                    onShowShutterButton = { viewModel.showShutterButton() }
                )
            }

            is LightMeterState.Error -> {
                ErrorContent(
                    state = currentState,
                    onRetry = { viewModel.onRetry() }
                )
            }
        }
    }
}

@Composable
private fun LoadingContent(
    message: String?,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            color = Color.White
        )
        message?.let {
            Text(
                text = it,
                color = Color.White,
                modifier = Modifier.padding(top = 64.dp)
            )
        }
    }
}

@Composable
private fun ReadyContentOverlay(
    state: LightMeterState.Ready,
    captureCallback: (() -> Bitmap?)?,
    onToggleLock: (ExposureType) -> Unit,
    onChangeValue: (ExposureType, Int) -> Unit,
    onShutterClick: (Bitmap?) -> Unit,
    onHideShutterButton: () -> Unit,
    onShowShutterButton: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val rootView = LocalView.current.rootView
    var shouldCaptureScreenshot by remember { mutableStateOf(false) }

    // 버튼이 숨겨졌을 때 스크린샷 캡처
    LaunchedEffect(state.isShutterButtonVisible) {
        if (!state.isShutterButtonVisible && shouldCaptureScreenshot) {
            // UI 업데이트 대기 (버튼이 완전히 사라지도록)
            delay(100)

            // 화면 캡처
            val bitmap = ScreenshotUtils.captureView(rootView)

            // 저장
            val success = ScreenshotUtils.saveBitmapToGallery(context, bitmap)

            // 사용자에게 알림
            val message = if (success) {
                "스크린샷이 갤러리에 저장되었습니다"
            } else {
                "스크린샷 저장에 실패했습니다"
            }
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()

            // 버튼 다시 표시
            shouldCaptureScreenshot = false
            onShowShutterButton()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        // 노출 값 표시 (오른쪽 아래)
        ExposureValueDisplay(
            exposureSettings = state.exposureSettings,
            onToggleLock = onToggleLock,
            onChangeValue = onChangeValue,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        )

        // 셔터 버튼 (하단 중앙) - 가시성 제어
        if (state.isShutterButtonVisible) {
            ShutterButton(
                isFrozen = state.isFrozen,
                onClick = {
                    if (state.isFrozen) {
                        // 해제: null 전달
                        onShutterClick(null)
                    } else {
                        // 스크린샷 캡처 시작
                        shouldCaptureScreenshot = true
                        onHideShutterButton()
                    }
                },
                contentDescription = if (state.isFrozen) {
                    stringResource(R.string.cd_resume_camera)
                } else {
                    stringResource(R.string.cd_freeze_camera)
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 32.dp)
            )
        }

        // 고정 상태 표시
        if (state.isFrozen) {
            Text(
                text = stringResource(R.string.status_frozen),
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 48.dp)
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
    }
}

@Composable
private fun ErrorContent(
    state: LightMeterState.Error,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = state.message,
            color = Color.White,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}
