# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

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

SSReader is an Android app built with Jetpack Compose and CameraX. Current feature: **Light Meter** - a film camera exposure meter.

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

- **MVVM**: ViewModel manages `StateFlow<LightMeterState>` consumed by Compose
- **CameraX ImageAnalysis**: Analyzer calculates Y-plane average luminance → EV → optimal exposure
- **Exposure Lock System**: Users can lock ISO/aperture/shutter, unlocked values auto-adjust

## Feature Specifications (Speckit)

This project uses a specification-driven workflow. Feature specs live in `specs/<feature-id>/`:

- `spec.md` - Feature requirements
- `plan.md` - Technical design
- `tasks.md` - Implementation checklist
- `data-model.md` - Entity definitions
- `research.md` - Technical decisions

Project constitution: `.specify/memory/constitution.md`

## Code Standards (from Constitution)

- Kotlin official conventions
- KDoc for public APIs
- 80%+ test coverage on business logic
- Material Design 3
- All UI strings in `strings.xml`
- contentDescription for accessibility
- Min SDK 26, Target SDK 36, JVM 11

## Dependencies

Key libraries: CameraX 1.4.0, Compose BOM 2024.09.00, Kotlin 2.0.21
