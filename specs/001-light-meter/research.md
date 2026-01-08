# Research: 필름 카메라 노출계 (Light Meter)

**Feature Branch**: `001-light-meter`
**Date**: 2026-01-07

## 1. CameraX 밝기 측정

### Decision: CameraX ImageAnalysis + YUV Y-plane 평균값

### Rationale:
- CameraX는 Android Jetpack의 공식 카메라 라이브러리로 lifecycle-aware 하며 안정적
- ImageAnalysis use case를 통해 실시간 프레임 분석 가능
- YUV_420_888 포맷에서 Y plane이 밝기(luminance)를 나타냄
- Y plane의 평균값 계산으로 전체 화면의 평균 밝기 측정 가능

### Alternatives considered:
- **Camera2 API 직접 사용**: 복잡하고 boilerplate 코드가 많음. CameraX가 Camera2를 래핑하여 더 간단한 API 제공
- **MLKit/TensorFlow를 통한 분석**: 과도한 복잡성. 단순 밝기 측정에는 불필요

### Implementation Notes:
```
ImageAnalysis.Analyzer 구현:
1. image.planes[0] (Y plane) 접근
2. ByteBuffer에서 픽셀값 추출
3. 평균 luminance 계산 (0-255 범위)
4. EV 값으로 변환
```

---

## 2. 카메라 프리뷰 정지 (Freeze)

### Decision: 마지막 프레임 Bitmap 캡처 및 이미지로 표시

### Rationale:
- CameraX의 Preview use case에는 직접적인 pause/freeze 메서드가 없음
- 가장 실용적인 방법은 셔터 버튼 클릭 시 마지막 분석 프레임을 Bitmap으로 저장하고 ImageView/Image composable로 표시
- 정지 상태에서는 카메라를 실제로 중지할 필요 없이 UI만 프리즈된 이미지로 교체

### Alternatives considered:
- **CameraControl.enableTorch 등 카메라 제어**: freeze 기능 없음
- **PreviewView의 bitmap 속성 사용**: PreviewView.getBitmap()으로 현재 프레임 캡처 가능
- **ImageAnalysis에서 Bitmap 변환**: YUV를 Bitmap으로 변환하여 저장 (추가 처리 필요)

### Implementation Notes:
```
정지 모드 구현:
1. 셔터 버튼 클릭 시 PreviewView.bitmap 또는 ImageAnalysis 프레임 캡처
2. 캡처된 Bitmap을 State로 저장
3. 정지 모드일 때 Image composable로 Bitmap 표시
4. 다시 클릭 시 Bitmap null로 설정하고 카메라 프리뷰로 복귀
```

---

## 3. CameraX + Jetpack Compose 통합

### Decision: AndroidView를 사용한 PreviewView 래핑

### Rationale:
- CameraX의 PreviewView는 현재 View 기반이므로 Compose에서 AndroidView로 래핑 필요
- Google 공식 문서에서 권장하는 패턴
- camera-compose 아티팩트가 있지만 아직 알파 단계

### Alternatives considered:
- **camera-compose 라이브러리**: 아직 불안정. 프로덕션에서는 AndroidView 래핑이 더 안전
- **Surface 직접 제어**: 복잡하고 저수준 작업 필요

### Implementation Notes:
```kotlin
@Composable
fun CameraPreview(
    modifier: Modifier,
    lifecycleOwner: LifecycleOwner,
    onFrameAnalyzed: (Float) -> Unit // luminance callback
) {
    AndroidView(
        factory = { context ->
            PreviewView(context).apply {
                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            }
        },
        modifier = modifier
    )
}
```

---

## 4. 노출 계산 공식

### Decision: EV 기반 계산 + 표준 full-stop 값 사용

### Rationale:
- 사진학의 표준 공식을 사용하여 정확한 노출 계산
- EV(Exposure Value)를 중심으로 ISO, 조리개, 셔터스피드 간 관계 정립
- full-stop 단위 사용으로 필름 카메라 사용자에게 친숙한 값 제공

### Core Formulas:

**EV 계산 (ISO 100 기준)**:
```
EV₁₀₀ = log₂(N² / t)
where N = f-number (조리개), t = exposure time (셔터스피드, 초)
```

**ISO 보정**:
```
EV_s = EV₁₀₀ + log₂(S / 100)
where S = ISO value
```

**밝기(Luminance)에서 EV 계산**:
```
EV = log₂(L × S / K)
where L = luminance (cd/m²), S = ISO, K = calibration constant (typically 12.5)
```

**센서 평균 밝기에서 EV 근사**:
```
// Y plane 평균값 (0-255)을 EV로 변환
// 중간 회색(18% gray) = Y값 약 118 = EV 0 (ISO 100 기준)
EV ≈ log₂(Y_avg / 118) + baseEV
```

### Standard Values (Full Stops):

**ISO**:
```
50, 100, 200, 400, 800, 1600, 3200, 6400
```

**Aperture (f-stop)**:
```
f/1.0, f/1.4, f/2, f/2.8, f/4, f/5.6, f/8, f/11, f/16, f/22, f/32
```

**Shutter Speed**:
```
1/8000, 1/4000, 1/2000, 1/1000, 1/500, 1/250, 1/125, 1/60, 1/30, 1/15, 1/8, 1/4, 1/2, 1", 2", 4"
```

### Sunny 16 Rule (참고):
- 맑은 날 야외: f/16, 셔터스피드 = 1/ISO
- 예: ISO 100, f/16, 1/125초

---

## 5. 스와이프 제스처 구현

### Decision: Modifier.pointerInput + detectHorizontalDragGestures

### Rationale:
- Compose의 기본 제스처 API 사용으로 플랫폼 일관성 유지
- detectHorizontalDragGestures가 수평 드래그에 최적화됨
- 드래그 거리(delta)에 따라 값 변경량 조절 가능

### Alternatives considered:
- **Modifier.draggable**: 단순하지만 세밀한 제어 어려움
- **Modifier.swipeable**: deprecated, AnchoredDraggable로 대체됨
- **third-party 라이브러리**: 불필요한 의존성 추가

### Implementation Notes:
```kotlin
Modifier.pointerInput(Unit) {
    detectHorizontalDragGestures { _, dragAmount ->
        // dragAmount > 0: 오른쪽 스와이프 (값 증가)
        // dragAmount < 0: 왼쪽 스와이프 (값 감소)
        val threshold = 50.dp.toPx() // 값 변경을 위한 최소 드래그 거리
    }
}
```

---

## 6. 햅틱 피드백

### Decision: LocalHapticFeedback + HapticFeedbackType.LongPress

### Rationale:
- Compose의 기본 햅틱 API 사용
- 값 변경 시 HapticFeedbackType.LongPress로 촉각 피드백 제공
- 고정/해제 시 추가 피드백으로 사용자 인지 향상

### Implementation Notes:
```kotlin
val haptic = LocalHapticFeedback.current
// 값 변경 시
haptic.performHapticFeedback(HapticFeedbackType.LongPress)
```

---

## 7. 의존성 목록

### 필수 추가 의존성:

```kotlin
// CameraX
implementation("androidx.camera:camera-core:1.4.0")
implementation("androidx.camera:camera-camera2:1.4.0")
implementation("androidx.camera:camera-lifecycle:1.4.0")
implementation("androidx.camera:camera-view:1.4.0")

// Compose ViewModel (이미 있을 수 있음)
implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.0")

// Accompanist Permissions (선택적 - 권한 처리 간소화)
implementation("com.google.accompanist:accompanist-permissions:0.34.0")
```

### AndroidManifest 권한:
```xml
<uses-permission android:name="android.permission.CAMERA" />
<uses-feature android:name="android.hardware.camera" android:required="true" />
```

---

## 8. 리서치 결론

모든 기술적 미지수가 해결됨:
- CameraX ImageAnalysis로 밝기 측정 ✓
- Bitmap 캡처로 프리뷰 정지 ✓
- AndroidView로 Compose 통합 ✓
- EV 기반 노출 계산 공식 확립 ✓
- detectHorizontalDragGestures로 스와이프 구현 ✓
- LocalHapticFeedback으로 촉각 피드백 ✓

Phase 1 설계로 진행 준비 완료.
