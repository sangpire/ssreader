# AGENTS.md

이 문서는 Codex가 이 저장소에서 작업할 때 참고할 지침을 제공합니다.

## Build & Test Commands

```bash
# Build debug APK
./gradlew assembleDebug

# Run all unit tests
./gradlew testDebugUnitTest

# Run specific test class
./gradlew testDebugUnitTest --tests "*.ExposureCalculatorTest"

# Run instrumented tests (requires emulator/device)
./gradlew connectedDebugAndroidTest

# Clean build
./gradlew clean assembleDebug
```

## Architecture Overview

SSReader는 Jetpack Compose와 CameraX로 만든 Android 앱입니다. 현재 기능은 **Light Meter**(필름 카메라 노출계)입니다.

### Layer Structure

```
app/src/main/java/io/github/sangpire/ssreader/
├── domain/              # Business logic (pure Kotlin, no Android deps)
│   ├── model/           # Data classes (ExposureSettings, ExposureValue, etc.)
│   └── ExposureCalculator.kt  # EV calculation logic
├── camera/              # CameraX integration
│   └── LightMeterAnalyzer.kt  # ImageAnalysis.Analyzer for luminance
├── ui/
│   ├── lightmeter/      # Light meter feature
│   │   ├── LightMeterScreen.kt      # Main screen Composable
│   │   ├── LightMeterViewModel.kt   # UI state management
│   │   └── components/              # Reusable UI components
│   └── theme/           # Material 3 theming
└── MainActivity.kt      # Entry point
```

### Key Patterns

- **MVVM**: ViewModel이 `StateFlow<LightMeterState>`를 관리하고 Compose에서 소비합니다.
- **CameraX ImageAnalysis**: Analyzer가 Y-플레인 평균 휘도를 계산 → EV → 최적 노출 값을 산출합니다.
- **Exposure Lock System**: 사용자가 ISO/조리개/셔터를 잠그면, 잠기지 않은 값이 자동으로 조정됩니다.

## Code Standards

- Kotlin 공식 컨벤션 준수
- 공개 API에는 KDoc 작성
- 비즈니스 로직 80%+ 테스트 커버리지
- Material Design 3
- 모든 UI 문자열은 `strings.xml`에 정의
- 접근성 준수를 위해 contentDescription 제공
- Min SDK 26, Target SDK 36, JVM 11

## Specs Workflow

기능 스펙은 `specs/<feature-id>/` 아래에 있으며 `spec.md`, `plan.md`, `tasks.md` 등 관련 설계를 포함합니다.

## 문서 언어 지침

가능하면 문서는 한글로 작성합니다.
