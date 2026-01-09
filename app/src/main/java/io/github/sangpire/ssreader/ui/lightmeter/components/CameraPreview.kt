package io.github.sangpire.ssreader.ui.lightmeter.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import io.github.sangpire.ssreader.R
import io.github.sangpire.ssreader.camera.LightMeterAnalyzer
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/**
 * 카메라 프리뷰 Composable
 *
 * @param modifier Modifier
 * @param isFrozen 화면 고정 여부
 * @param frozenBitmap 고정 시 표시할 비트맵
 * @param analyzer 이미지 분석기
 * @param onCameraReady 카메라 준비 완료 콜백
 * @param onCameraError 카메라 오류 콜백
 * @param onCaptureCallbackReady PreviewView Bitmap 캡처 콜백이 준비되면 호출
 * @param onImageCaptureReady 고해상도 이미지 캡처 콜백이 준비되면 호출
 */
@Composable
fun CameraPreview(
    modifier: Modifier = Modifier,
    isFrozen: Boolean = false,
    frozenBitmap: Bitmap? = null,
    analyzer: LightMeterAnalyzer,
    onCameraReady: () -> Unit = {},
    onCameraError: (String) -> Unit = {},
    onCaptureCallbackReady: ((() -> Bitmap?) -> Unit)? = null,
    onImageCaptureReady: ((suspend () -> Bitmap?) -> Unit)? = null
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraDescription = stringResource(R.string.cd_camera_preview)

    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }

    if (isFrozen && frozenBitmap != null) {
        // 고정된 화면 표시
        Image(
            bitmap = frozenBitmap.asImageBitmap(),
            contentDescription = cameraDescription,
            modifier = modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
    } else {
        // 실시간 카메라 프리뷰
        val previewViewRef = remember { mutableStateOf<PreviewView?>(null) }

        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                }.also { previewViewRef.value = it }
            },
            modifier = modifier
                .fillMaxSize()
                .semantics { contentDescription = cameraDescription },
            update = { previewView ->
                previewViewRef.value = previewView
                // Bitmap 캡처 콜백 제공
                onCaptureCallbackReady?.invoke { previewView.bitmap }
                setupCamera(
                    context = context,
                    previewView = previewView,
                    analyzer = analyzer,
                    cameraExecutor = cameraExecutor,
                    lifecycleOwner = lifecycleOwner,
                    onCameraReady = onCameraReady,
                    onCameraError = onCameraError,
                    onImageCaptureReady = onImageCaptureReady
                )
            }
        )
    }
}

private fun setupCamera(
    context: Context,
    previewView: PreviewView,
    analyzer: LightMeterAnalyzer,
    cameraExecutor: ExecutorService,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    onCameraReady: () -> Unit,
    onCameraError: (String) -> Unit,
    onImageCaptureReady: ((suspend () -> Bitmap?) -> Unit)?
) {
    val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

    cameraProviderFuture.addListener({
        try {
            val cameraProvider = cameraProviderFuture.get()

            // Preview use case
            val preview = Preview.Builder()
                .build()
                .also {
                    it.surfaceProvider = previewView.surfaceProvider
                }

            // Image analysis use case
            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor, analyzer)
                }

            // Image capture use case (고해상도 촬영)
            val imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                .build()

            // 고해상도 이미지 캡처 콜백 제공
            onImageCaptureReady?.invoke {
                captureHighResolutionImage(imageCapture, cameraExecutor)
            }

            // 후면 카메라 선택
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            // 기존 바인딩 해제 후 새로 바인딩
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                imageAnalysis,
                imageCapture
            )

            onCameraReady()
        } catch (e: Exception) {
            onCameraError(e.message ?: "Camera initialization failed")
        }
    }, ContextCompat.getMainExecutor(context))
}

/**
 * 고해상도 이미지를 메모리로 캡처
 *
 * @param imageCapture ImageCapture use case
 * @param executor Executor
 * @return 캡처된 Bitmap, 실패 시 null
 */
private suspend fun captureHighResolutionImage(
    imageCapture: ImageCapture,
    executor: ExecutorService
): Bitmap? = suspendCoroutine { continuation ->
    imageCapture.takePicture(
        executor,
        object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(imageProxy: androidx.camera.core.ImageProxy) {
                try {
                    // ImageProxy를 Bitmap으로 변환
                    val buffer = imageProxy.planes[0].buffer
                    val bytes = ByteArray(buffer.remaining())
                    buffer.get(bytes)

                    var bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)

                    // 회전 처리 (필요시)
                    if (imageProxy.imageInfo.rotationDegrees != 0) {
                        val matrix = Matrix()
                        matrix.postRotate(imageProxy.imageInfo.rotationDegrees.toFloat())
                        bitmap = Bitmap.createBitmap(
                            bitmap, 0, 0,
                            bitmap.width, bitmap.height,
                            matrix, true
                        )
                    }

                    imageProxy.close()
                    continuation.resume(bitmap)
                } catch (e: Exception) {
                    imageProxy.close()
                    continuation.resume(null)
                }
            }

            override fun onError(exception: ImageCaptureException) {
                exception.printStackTrace()
                continuation.resume(null)
            }
        }
    )
}
