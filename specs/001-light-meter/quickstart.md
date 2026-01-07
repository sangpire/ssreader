# Quickstart Guide: 필름 카메라 노출계 (Light Meter)

**Feature Branch**: `001-light-meter`
**Date**: 2026-01-07

## 개발 환경 설정

### 1. 의존성 추가

`app/build.gradle.kts`에 다음 의존성 추가:

```kotlin
dependencies {
    // CameraX
    val cameraxVersion = "1.4.0"
    implementation("androidx.camera:camera-core:$cameraxVersion")
    implementation("androidx.camera:camera-camera2:$cameraxVersion")
    implementation("androidx.camera:camera-lifecycle:$cameraxVersion")
    implementation("androidx.camera:camera-view:$cameraxVersion")

    // ViewModel Compose (기존에 없다면)
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.0")

    // 권한 처리 (선택사항)
    implementation("com.google.accompanist:accompanist-permissions:0.34.0")
}
```

### 2. AndroidManifest 권한

`app/src/main/AndroidManifest.xml`에 추가:

```xml
<manifest ...>
    <uses-permission android:name="android.permission.CAMERA" />
    <uses-feature android:name="android.hardware.camera" android:required="true" />

    <application ...>
        ...
    </application>
</manifest>
```

### 3. 프로젝트 구조 생성

```bash
# 디렉토리 생성
mkdir -p app/src/main/java/io/github/sangpire/ssreader/ui/lightmeter/components
mkdir -p app/src/main/java/io/github/sangpire/ssreader/domain/model
mkdir -p app/src/main/java/io/github/sangpire/ssreader/camera
mkdir -p app/src/test/java/io/github/sangpire/ssreader/domain
mkdir -p app/src/test/java/io/github/sangpire/ssreader/ui/lightmeter
mkdir -p app/src/androidTest/java/io/github/sangpire/ssreader/ui/lightmeter
```

---

## 구현 순서 (권장)

### Step 1: 도메인 모델 정의

1. `domain/model/ExposureSettings.kt` - 데이터 클래스 정의
2. `domain/model/ExposureValue.kt` - 노출 값 클래스
3. `domain/model/ExposureConstants.kt` - 표준 ISO/조리개/셔터스피드 상수

### Step 2: 노출 계산 로직

1. `domain/ExposureCalculator.kt` - 노출 계산 인터페이스 및 구현
2. `domain/ExposureCalculatorTest.kt` - 단위 테스트 (TDD 권장)

### Step 3: 카메라 분석기

1. `camera/LightMeterAnalyzer.kt` - ImageAnalysis.Analyzer 구현
2. Y plane에서 평균 밝기 추출 로직

### Step 4: ViewModel

1. `ui/lightmeter/LightMeterViewModel.kt` - 화면 상태 관리
2. `ui/lightmeter/LightMeterViewModelTest.kt` - ViewModel 테스트

### Step 5: UI 컴포넌트

1. `ui/lightmeter/components/CameraPreview.kt` - 카메라 프리뷰
2. `ui/lightmeter/components/ExposureValueDisplay.kt` - 노출 값 표시/조절
3. `ui/lightmeter/components/ShutterButton.kt` - 셔터 버튼
4. `ui/lightmeter/LightMeterScreen.kt` - 메인 화면 조합

### Step 6: 통합 및 네비게이션

1. `MainActivity.kt` 수정 - LightMeterScreen 연결
2. 권한 요청 로직 추가

---

## 핵심 코드 스니펫

### ExposureValue 데이터 클래스

```kotlin
// domain/model/ExposureValue.kt
data class ExposureValue(
    val type: ExposureType,
    val value: Double,
    val displayValue: String,
    val stopIndex: Int,
    val isLocked: Boolean = false
)

enum class ExposureType { ISO, APERTURE, SHUTTER_SPEED }
```

### EV 계산 공식

```kotlin
// domain/ExposureCalculator.kt
fun calculateEV(luminance: Float, iso: Int): Float {
    // 중간 회색 기준값 (18% gray ≈ Y값 118)
    val referenceGray = 118f
    // ISO 100 기준 EV 계산 후 ISO 보정
    val evBase = ln((luminance / referenceGray).coerceAtLeast(0.001f)) / ln(2f)
    val isoCompensation = ln(iso / 100f) / ln(2f)
    return evBase + isoCompensation + 13f // 13 = 기준 보정값 (조정 필요)
}
```

### 스와이프 제스처

```kotlin
// ui/lightmeter/components/ExposureValueDisplay.kt
Modifier.pointerInput(Unit) {
    var accumulatedDrag = 0f
    val threshold = 80.dp.toPx()

    detectHorizontalDragGestures(
        onDragStart = { accumulatedDrag = 0f },
        onDragEnd = {
            if (accumulatedDrag > threshold) onSwipe(1)
            else if (accumulatedDrag < -threshold) onSwipe(-1)
        },
        onHorizontalDrag = { _, dragAmount ->
            accumulatedDrag += dragAmount
        }
    )
}
```

### CameraX 프리뷰 설정

```kotlin
// ui/lightmeter/components/CameraPreview.kt
@Composable
fun CameraPreview(
    modifier: Modifier,
    onMeteringResult: (MeteringResult) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context) }

    LaunchedEffect(Unit) {
        val cameraProvider = context.getCameraProvider()
        val preview = Preview.Builder().build()
        val imageAnalysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .apply {
                setAnalyzer(Dispatchers.Default.asExecutor(), LightMeterAnalyzer(onMeteringResult))
            }

        cameraProvider.unbindAll()
        cameraProvider.bindToLifecycle(
            lifecycleOwner,
            CameraSelector.DEFAULT_BACK_CAMERA,
            preview,
            imageAnalysis
        )
        preview.setSurfaceProvider(previewView.surfaceProvider)
    }

    AndroidView(factory = { previewView }, modifier = modifier)
}
```

---

## 테스트 실행

```bash
# 단위 테스트
./gradlew testDebugUnitTest

# 특정 테스트 클래스 실행
./gradlew testDebugUnitTest --tests "*.ExposureCalculatorTest"

# UI 테스트 (에뮬레이터/기기 필요)
./gradlew connectedDebugAndroidTest
```

---

## 디버깅 팁

### 1. 노출 값 로깅

```kotlin
// ViewModel에서 측광 결과 로깅
fun onMeteringResult(result: MeteringResult) {
    Log.d("LightMeter", "Luminance: ${result.averageLuminance}, EV: ${result.calculatedEV}")
    // ...
}
```

### 2. 카메라 프리뷰 문제

- `PreviewView.implementationMode`를 `COMPATIBLE`로 설정하면 호환성 향상
- 에뮬레이터에서는 카메라 기능이 제한될 수 있음 (실제 기기 권장)

### 3. 권한 거부 처리

```kotlin
// Accompanist Permissions 사용 예시
val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)

when {
    cameraPermissionState.status.isGranted -> {
        LightMeterContent()
    }
    cameraPermissionState.status.shouldShowRationale -> {
        PermissionRationale { cameraPermissionState.launchPermissionRequest() }
    }
    else -> {
        RequestPermissionButton { cameraPermissionState.launchPermissionRequest() }
    }
}
```

---

## 관련 문서

- [spec.md](./spec.md) - 기능 명세
- [plan.md](./plan.md) - 구현 계획
- [research.md](./research.md) - 기술 리서치
- [data-model.md](./data-model.md) - 데이터 모델
- [contracts/internal-interfaces.md](./contracts/internal-interfaces.md) - 인터페이스 계약
