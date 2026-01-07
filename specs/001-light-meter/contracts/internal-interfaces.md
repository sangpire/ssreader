# Internal Interfaces: 필름 카메라 노출계 (Light Meter)

**Feature Branch**: `001-light-meter`
**Date**: 2026-01-07

> 이 기능은 외부 API가 없는 단일 앱이므로, REST/GraphQL 스키마 대신 내부 컴포넌트 간 인터페이스 계약을 정의합니다.

## 1. ExposureCalculator Interface

노출 계산 비즈니스 로직을 담당하는 핵심 인터페이스

```kotlin
interface ExposureCalculator {
    /**
     * 측정된 밝기값에서 EV(Exposure Value)를 계산
     * @param luminance Y plane 평균값 (0-255)
     * @param iso 현재 ISO 값
     * @return 계산된 EV 값
     */
    fun calculateEV(luminance: Float, iso: Int): Float

    /**
     * 현재 노출 설정과 측정된 EV를 기반으로 적정 노출 설정 계산
     * @param currentSettings 현재 노출 설정 (고정된 값 포함)
     * @param measuredEV 측정된 EV 값
     * @return 업데이트된 노출 설정
     */
    fun calculateOptimalExposure(
        currentSettings: ExposureSettings,
        measuredEV: Float
    ): ExposureSettings

    /**
     * 현재 노출 설정의 노출 보정값 계산
     * @param settings 현재 노출 설정
     * @param measuredEV 측정된 EV 값
     * @return 노출 보정 스톱 수 (양수: 과다 노출, 음수: 부족 노출)
     */
    fun calculateExposureCompensation(
        settings: ExposureSettings,
        measuredEV: Float
    ): Float

    /**
     * 다음/이전 표준 스톱 값 반환
     * @param current 현재 값
     * @param type 노출 타입 (ISO, APERTURE, SHUTTER_SPEED)
     * @param direction 방향 (1: 증가, -1: 감소)
     * @return 새로운 ExposureValue, 범위 초과 시 null
     */
    fun getNextStopValue(
        current: ExposureValue,
        direction: Int
    ): ExposureValue?
}
```

---

## 2. LightMeterAnalyzer Interface

CameraX ImageAnalysis.Analyzer 구현을 위한 인터페이스

```kotlin
interface LightMeterAnalyzer : ImageAnalysis.Analyzer {
    /**
     * 측광 결과 콜백 등록
     * @param callback 측광 결과를 받을 콜백 함수
     */
    fun setMeteringCallback(callback: (MeteringResult) -> Unit)

    /**
     * 분석기 활성화/비활성화
     * @param enabled true면 분석 수행, false면 분석 건너뜀
     */
    fun setEnabled(enabled: Boolean)

    /**
     * 현재 프레임의 Bitmap 캡처 요청
     * @return 캡처된 Bitmap, 실패 시 null
     */
    suspend fun captureCurrentFrame(): Bitmap?
}
```

---

## 3. LightMeterViewModel Contract

UI와 비즈니스 로직 간의 계약

### State (읽기 전용)

```kotlin
interface LightMeterViewModelContract {
    // UI State
    val uiState: StateFlow<LightMeterState>

    // 개별 노출 값 (편의를 위한 derived state)
    val iso: StateFlow<ExposureValue>
    val aperture: StateFlow<ExposureValue>
    val shutterSpeed: StateFlow<ExposureValue>

    // 측광 값
    val measuredEV: StateFlow<Float>

    // 노출 보정 (현재 설정과 적정 노출의 차이)
    val exposureCompensation: StateFlow<Float>
}
```

### Actions (이벤트)

```kotlin
interface LightMeterViewModelActions {
    /**
     * 노출 값 변경 (스와이프로 호출)
     * @param type 변경할 노출 타입
     * @param direction 방향 (1: 증가, -1: 감소)
     */
    fun changeExposureValue(type: ExposureType, direction: Int)

    /**
     * 노출 값 고정/해제 토글
     * @param type 토글할 노출 타입
     */
    fun toggleLock(type: ExposureType)

    /**
     * 셔터 버튼 클릭 (정지/재개 토글)
     */
    fun onShutterClick()

    /**
     * 측광 결과 업데이트 (카메라 분석기에서 호출)
     * @param result 측광 결과
     */
    fun onMeteringResult(result: MeteringResult)

    /**
     * 카메라 권한 결과 처리
     * @param granted 권한 승인 여부
     */
    fun onCameraPermissionResult(granted: Boolean)
}
```

---

## 4. UI Component Contracts

### CameraPreview Component

```kotlin
@Composable
fun CameraPreview(
    modifier: Modifier = Modifier,
    isFrozen: Boolean,
    frozenBitmap: Bitmap?,
    onMeteringResult: (MeteringResult) -> Unit,
    onCaptureRequest: suspend () -> Bitmap?
)
```

### ExposureValueDisplay Component

```kotlin
@Composable
fun ExposureValueDisplay(
    modifier: Modifier = Modifier,
    label: String,           // "ISO", "f", "SS" 등
    value: ExposureValue,
    onSwipe: (Int) -> Unit,  // 방향: 1 또는 -1
    onTap: () -> Unit        // 고정/해제 토글
)
```

### ShutterButton Component

```kotlin
@Composable
fun ShutterButton(
    modifier: Modifier = Modifier,
    isFrozen: Boolean,
    onClick: () -> Unit
)
```

### ExposureWarning Component

```kotlin
@Composable
fun ExposureWarning(
    modifier: Modifier = Modifier,
    compensation: Float,     // 노출 보정 스톱 수
    isVisible: Boolean       // 모든 값 고정 시에만 표시
)
```

---

## 5. Event Flow Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                         User Actions                             │
├─────────────────────────────────────────────────────────────────┤
│  Swipe ISO          Tap Aperture       Click Shutter            │
│       │                   │                  │                  │
│       ▼                   ▼                  ▼                  │
│ changeExposureValue  toggleLock        onShutterClick           │
│       │                   │                  │                  │
└───────┴───────────────────┴──────────────────┴──────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────────┐
│                    LightMeterViewModel                          │
├─────────────────────────────────────────────────────────────────┤
│  1. Update ExposureSettings                                     │
│  2. Recalculate optimal exposure via ExposureCalculator         │
│  3. Emit new LightMeterState                                    │
└─────────────────────────────────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────────┐
│                      UI Components                               │
├─────────────────────────────────────────────────────────────────┤
│  LightMeterScreen observes uiState and recomposes               │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│                     Camera Pipeline                              │
├─────────────────────────────────────────────────────────────────┤
│  CameraX ImageAnalysis                                          │
│       │                                                         │
│       ▼                                                         │
│  LightMeterAnalyzer.analyze()                                   │
│       │                                                         │
│       ▼                                                         │
│  Calculate average luminance from Y plane                       │
│       │                                                         │
│       ▼                                                         │
│  MeteringResult → ViewModel.onMeteringResult()                  │
│       │                                                         │
│       ▼                                                         │
│  Update measuredEV → Recalculate → Emit State                   │
└─────────────────────────────────────────────────────────────────┘
```

---

## 6. Error Handling Contract

```kotlin
sealed class LightMeterError {
    data class CameraPermissionDenied(
        val shouldShowRationale: Boolean
    ) : LightMeterError()

    data class CameraUnavailable(
        val reason: String
    ) : LightMeterError()

    data class AnalysisFailed(
        val exception: Exception
    ) : LightMeterError()
}

interface ErrorHandler {
    fun handleError(error: LightMeterError): LightMeterState.Error
    fun getRecoveryAction(error: LightMeterError): (() -> Unit)?
}
```
