# Implementation Plan: 필름 카메라 노출계 (Light Meter)

**Branch**: `001-light-meter` | **Date**: 2026-01-07 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/001-light-meter/spec.md`

## Summary

자동 노출 기능이 없는 오래된/고장난 필름 카메라 사용자를 위한 노출계 앱 구현. 핸드폰 카메라를 통해 실시간으로 빛을 측정하고, ISO/조리개/셔터스피드의 적정 노출 값을 계산하여 표시한다. CameraX를 사용하여 카메라 프리뷰와 밝기 측정을 수행하고, Jetpack Compose로 UI를 구성한다.

## Technical Context

**Language/Version**: Kotlin (JVM 11)
**Primary Dependencies**:
- Jetpack Compose (Material 3)
- CameraX (카메라 프리뷰 및 이미지 분석)
- Kotlin Coroutines + Flow (비동기 처리)

**Storage**: N/A (노출 설정 상태는 ViewModel에서 인메모리로 관리)
**Testing**: JUnit, MockK, Compose Testing
**Target Platform**: Android API 26+ (Android 8.0+)
**Project Type**: Mobile (Android)
**Performance Goals**:
- 노출 값 업데이트 < 1초 (SC-002)
- 스와이프 피드백 < 0.3초 (SC-004)
- 60fps 카메라 프리뷰 유지
**Constraints**:
- 앱 시작 후 5초 이내 노출 값 표시 (SC-001)
- 95% 일반 촬영 조건에서 정확한 노출 (SC-005)
**Scale/Scope**: 단일 화면 앱, 1개 Activity/Screen

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

### I. 코드 품질 우선

| 항목 | 상태 | 비고 |
|------|------|------|
| Kotlin 공식 코딩 컨벤션 | PASS | 프로젝트 표준 따름 |
| 단일 책임 원칙(SRP) | PASS | 카메라, 노출계산, UI 분리 설계 |
| 명확한 클래스/함수명 | PASS | 도메인 용어 사용 (ExposureCalculator, LightMeter 등) |
| 매직 넘버 상수 추출 | PASS | ISO/f-stop/셔터스피드 값 상수로 정의 |
| KDoc 문서화 | PASS | Public API 문서화 예정 |

### II. 테스트 표준 준수

| 항목 | 상태 | 비고 |
|------|------|------|
| 비즈니스 로직 단위 테스트 | PASS | 노출 계산 로직 테스트 필수 |
| 테스트 커버리지 80%+ | PASS | 핵심 로직(ExposureCalculator) 대상 |
| UI 테스트 | PASS | Compose Testing으로 검증 |
| 독립적/반복 실행 가능 | PASS | 카메라 의존성 Mock 처리 |

### III. 사용자 경험 일관성

| 항목 | 상태 | 비고 |
|------|------|------|
| Material Design 3 | PASS | Compose Material 3 사용 |
| 다크/라이트 모드 | PASS | 기존 Theme 활용 |
| 48dp 터치 영역 | PASS | 노출 값 컨트롤 영역 확보 |
| 애니메이션 피드백 | PASS | 값 변경/고정 시 시각적 피드백 |
| 접근성(a11y) | PASS | contentDescription 제공 |
| 오류 안내 | PASS | 권한 거부, 범위 초과 시 안내 |
| 다국어(strings.xml) | PASS | 문자열 리소스 관리 |

### IV. 성능 요구사항

| 항목 | 상태 | 비고 |
|------|------|------|
| 앱 콜드 스타트 2초 이내 | PASS | 단일 화면, 경량 의존성 |
| UI 스레드 블로킹 금지 | PASS | 카메라 분석 백그라운드 처리 |
| 60fps 유지 | PASS | CameraX 최적화 |
| 메모리 누수 방지 | PASS | Lifecycle-aware 컴포넌트 |

### V. 단순성 지향

| 항목 | 상태 | 비고 |
|------|------|------|
| YAGNI 원칙 | PASS | 필요한 기능만 구현 |
| 실제 필요 후 추상화 | PASS | 단일 화면, 직접 구현 |
| 외부 라이브러리 최소화 | PASS | CameraX만 추가 (필수) |

**Gate 결과**: PASS (모든 헌법 원칙 준수)

## Project Structure

### Documentation (this feature)

```text
specs/001-light-meter/
├── plan.md              # This file
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
├── contracts/           # Phase 1 output (N/A - no API)
└── tasks.md             # Phase 2 output
```

### Source Code (repository root)

```text
app/src/main/java/io/github/sangpire/ssreader/
├── MainActivity.kt                    # 기존 파일
├── ui/
│   ├── theme/                         # 기존 테마 파일들
│   └── lightmeter/
│       ├── LightMeterScreen.kt        # 메인 화면 Composable
│       ├── components/
│       │   ├── CameraPreview.kt       # 카메라 프리뷰 컴포넌트
│       │   ├── ExposureValueDisplay.kt # ISO/f/셔터스피드 표시
│       │   └── ShutterButton.kt       # 셔터 버튼
│       └── LightMeterViewModel.kt     # 화면 상태 관리
├── domain/
│   ├── model/
│   │   └── ExposureSettings.kt        # 노출 설정 데이터 클래스
│   └── ExposureCalculator.kt          # 노출 계산 로직
└── camera/
    └── LightMeterAnalyzer.kt          # CameraX 이미지 분석기

app/src/test/java/io/github/sangpire/ssreader/
├── domain/
│   └── ExposureCalculatorTest.kt      # 노출 계산 단위 테스트
└── ui/lightmeter/
    └── LightMeterViewModelTest.kt     # ViewModel 테스트

app/src/androidTest/java/io/github/sangpire/ssreader/
└── ui/lightmeter/
    └── LightMeterScreenTest.kt        # UI 테스트
```

**Structure Decision**: Android 단일 앱 구조. 기존 프로젝트 패키지 구조(`io.github.sangpire.ssreader`)를 유지하며, `lightmeter` 기능을 위한 하위 패키지를 추가한다. Clean Architecture의 단순화된 버전으로 `domain`(비즈니스 로직)과 `camera`(플랫폼 통합)를 분리한다.

## Complexity Tracking

헌법 위반 없음 - 해당 사항 없음.
