# Data Model: 필름 카메라 노출계 (Light Meter)

**Feature Branch**: `001-light-meter`
**Date**: 2026-01-07

## 핵심 엔티티

### 1. ExposureValue (노출 값)

카메라의 개별 노출 설정 값을 나타내는 열거형/클래스

```
ExposureValue
├── type: ExposureType (ISO | APERTURE | SHUTTER_SPEED)
├── value: Double (실제 수치 값)
├── displayValue: String (표시용 문자열, 예: "f/2.8", "1/125")
├── stopIndex: Int (표준 스톱 목록에서의 인덱스)
└── isLocked: Boolean (고정 여부)
```

**Validation Rules:**
- ISO: 50 ~ 6400 범위 (표준 full-stop 값만 허용)
- Aperture: f/1.0 ~ f/32 범위 (표준 full-stop 값만 허용)
- Shutter Speed: 1/8000 ~ 4초 범위 (표준 full-stop 값만 허용)

---

### 2. ExposureSettings (노출 설정)

현재 화면에 표시되는 전체 노출 설정 상태

```
ExposureSettings
├── iso: ExposureValue
├── aperture: ExposureValue
├── shutterSpeed: ExposureValue
├── measuredEV: Float (측정된 EV 값)
└── exposureCompensation: Float (노출 보정 스톱, 현재 설정과 적정 노출의 차이)
```

**State Transitions:**
- `UNLOCKED` → `LOCKED`: 사용자가 값을 스와이프하여 변경
- `LOCKED` → `UNLOCKED`: 사용자가 고정된 값을 탭

**Validation Rules:**
- 최소 1개 값은 항상 UNLOCKED 상태여야 함 (세 값 모두 LOCKED 시 경고만 표시)
- measuredEV 범위: -6 ~ +17 EV (일반적인 촬영 조건)

---

### 3. LightMeterState (노출계 상태)

화면 전체의 UI 상태를 나타내는 sealed class

```
LightMeterState
├── Loading (카메라 초기화 중)
│   └── message: String?
├── Ready (실시간 측광 중)
│   ├── exposureSettings: ExposureSettings
│   ├── isFrozen: Boolean
│   └── frozenBitmap: Bitmap? (정지 시 캡처된 이미지)
├── Frozen (화면 정지 상태)
│   ├── exposureSettings: ExposureSettings
│   └── frozenBitmap: Bitmap
└── Error (오류 상태)
    ├── type: ErrorType
    └── message: String
```

**ErrorType:**
- `CAMERA_PERMISSION_DENIED`: 카메라 권한 거부됨
- `CAMERA_UNAVAILABLE`: 카메라 사용 불가
- `ANALYSIS_FAILED`: 이미지 분석 실패

---

### 4. ExposureType (노출 타입)

노출 값의 종류를 구분하는 열거형

```
ExposureType
├── ISO
├── APERTURE
└── SHUTTER_SPEED
```

---

### 5. MeteringResult (측광 결과)

카메라 분석기에서 반환하는 측광 결과

```
MeteringResult
├── averageLuminance: Float (0-255 범위의 Y plane 평균값)
├── calculatedEV: Float (계산된 EV 값)
└── timestamp: Long (측정 시간)
```

---

## 표준 값 목록 (상수)

### ISO Values (Full Stop)
```
[50, 100, 200, 400, 800, 1600, 3200, 6400]
```

### Aperture Values (Full Stop)
```
[1.0, 1.4, 2.0, 2.8, 4.0, 5.6, 8.0, 11.0, 16.0, 22.0, 32.0]
Display: ["f/1", "f/1.4", "f/2", "f/2.8", "f/4", "f/5.6", "f/8", "f/11", "f/16", "f/22", "f/32"]
```

### Shutter Speed Values (Full Stop)
```
[1/8000, 1/4000, 1/2000, 1/1000, 1/500, 1/250, 1/125, 1/60, 1/30, 1/15, 1/8, 1/4, 1/2, 1, 2, 4]
(초 단위 double 값)
Display: ["1/8000", "1/4000", "1/2000", "1/1000", "1/500", "1/250", "1/125", "1/60", "1/30", "1/15", "1/8", "1/4", "1/2", "1\"", "2\"", "4\""]
```

---

## 관계도

```
┌─────────────────────────────────────────────────────────┐
│                    LightMeterState                       │
│  ┌─────────┐  ┌─────────┐  ┌─────────┐  ┌─────────┐    │
│  │ Loading │  │  Ready  │  │ Frozen  │  │  Error  │    │
│  └─────────┘  └────┬────┘  └────┬────┘  └─────────┘    │
│                    │            │                       │
│                    ▼            ▼                       │
│              ┌─────────────────────┐                   │
│              │  ExposureSettings   │                   │
│              │  ┌───────────────┐  │                   │
│              │  │ measuredEV    │  │                   │
│              │  └───────────────┘  │                   │
│              └──────────┬──────────┘                   │
│                         │                              │
│         ┌───────────────┼───────────────┐              │
│         ▼               ▼               ▼              │
│   ┌───────────┐  ┌───────────┐  ┌───────────┐         │
│   │    ISO    │  │  Aperture │  │  Shutter  │         │
│   │ExposureVal│  │ExposureVal│  │ExposureVal│         │
│   │ isLocked  │  │ isLocked  │  │ isLocked  │         │
│   └───────────┘  └───────────┘  └───────────┘         │
└─────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────┐
│                   MeteringResult                        │
│  (CameraX ImageAnalysis → LightMeterViewModel)         │
│  ┌─────────────────┐ ┌─────────────────┐               │
│  │averageLuminance │ │  calculatedEV   │               │
│  └─────────────────┘ └─────────────────┘               │
└─────────────────────────────────────────────────────────┘
```

---

## 계산 로직 흐름

```
1. 카메라 프레임 수신
   └── ImageAnalysis.Analyzer.analyze(image)

2. 밝기 측정
   └── Y plane 평균값 계산 (0-255)

3. EV 계산
   └── EV = log₂(luminance / 118) + baseEV
       (118 = 중간 회색 기준값)

4. 적정 노출 계산
   ├── 모든 값 UNLOCKED: 기본값으로 균형 있는 조합 계산
   ├── 1개 값 LOCKED: 나머지 2개 중 우선순위에 따라 계산
   ├── 2개 값 LOCKED: 나머지 1개 값 계산
   └── 3개 값 LOCKED: 현재 조합과 적정 노출의 차이 표시 (경고)

5. UI 업데이트
   └── ExposureSettings → LightMeterState.Ready
```

---

## 기본값 (Initial State)

```
ExposureSettings(
    iso = ExposureValue(type=ISO, value=100.0, isLocked=false),
    aperture = ExposureValue(type=APERTURE, value=5.6, isLocked=false),
    shutterSpeed = ExposureValue(type=SHUTTER_SPEED, value=1/125, isLocked=false),
    measuredEV = 0.0,
    exposureCompensation = 0.0
)
```
