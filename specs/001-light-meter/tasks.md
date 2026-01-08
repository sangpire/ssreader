# Tasks: 필름 카메라 노출계 (Light Meter)

**Input**: Design documents from `/specs/001-light-meter/`
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/

**Tests**: 헌법(constitution.md)에서 테스트 표준 준수를 요구하므로 핵심 비즈니스 로직에 대한 테스트를 포함합니다.

**Organization**: 태스크는 사용자 스토리별로 그룹화되어 각 스토리의 독립적인 구현 및 테스트가 가능합니다.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: 병렬 실행 가능 (다른 파일, 의존성 없음)
- **[Story]**: 해당 태스크가 속한 사용자 스토리 (US1, US2, US3, US4)
- 설명에 정확한 파일 경로 포함

## Path Conventions (Android)

- **Main**: `app/src/main/java/io/github/sangpire/ssreader/`
- **Test**: `app/src/test/java/io/github/sangpire/ssreader/`
- **Android Test**: `app/src/androidTest/java/io/github/sangpire/ssreader/`
- **Resources**: `app/src/main/res/`

---

## Phase 1: Setup (프로젝트 설정)

**Purpose**: 프로젝트 의존성 추가 및 기본 구조 생성

- [x] T001 app/build.gradle.kts에 CameraX 의존성 추가 (camera-core, camera-camera2, camera-lifecycle, camera-view)
- [x] T002 app/build.gradle.kts에 lifecycle-viewmodel-compose 의존성 추가
- [x] T003 [P] app/src/main/AndroidManifest.xml에 카메라 권한 및 feature 추가
- [x] T004 [P] plan.md의 프로젝트 구조에 따라 디렉토리 생성 (ui/lightmeter/, domain/, camera/)

---

## Phase 2: Foundational (기반 - 차단 선행 조건)

**Purpose**: 모든 사용자 스토리가 의존하는 핵심 도메인 모델 및 계산 로직

**CRITICAL**: 이 페이즈가 완료되어야 사용자 스토리 작업을 시작할 수 있음

- [x] T005 [P] app/src/main/java/io/github/sangpire/ssreader/domain/model/ExposureType.kt에 ExposureType enum 생성 (ISO, APERTURE, SHUTTER_SPEED)
- [x] T006 [P] app/src/main/java/io/github/sangpire/ssreader/domain/model/ExposureConstants.kt에 표준 ISO/조리개/셔터스피드 값 상수 정의
- [x] T007 app/src/main/java/io/github/sangpire/ssreader/domain/model/ExposureValue.kt에 ExposureValue 데이터 클래스 생성
- [x] T008 app/src/main/java/io/github/sangpire/ssreader/domain/model/ExposureSettings.kt에 ExposureSettings 데이터 클래스 생성
- [x] T009 app/src/main/java/io/github/sangpire/ssreader/domain/model/MeteringResult.kt에 MeteringResult 데이터 클래스 생성
- [x] T010 app/src/main/java/io/github/sangpire/ssreader/domain/model/LightMeterState.kt에 LightMeterState sealed class 생성
- [x] T011 app/src/test/java/io/github/sangpire/ssreader/domain/ExposureCalculatorTest.kt에 노출 계산 테스트 작성 (실패 확인)
- [x] T012 app/src/main/java/io/github/sangpire/ssreader/domain/ExposureCalculator.kt에 노출 계산 로직 구현 (EV 계산, 적정 노출 계산, 다음 스톱 값)
- [x] T013 app/src/main/res/values/strings.xml에 노출계 관련 문자열 리소스 추가

**Checkpoint**: 기반 완료 - 사용자 스토리 구현 시작 가능

---

## Phase 3: User Story 1 - 실시간 적정 노출 확인 (Priority: P1) MVP

**Goal**: 카메라 프리뷰를 전체 화면에 표시하고, 실시간으로 측정한 빛 조건에 따른 적정 노출 값(ISO, 조리개, 셔터스피드)을 화면 오른쪽 아래에 표시

**Independent Test**: 앱 실행 후 카메라를 다양한 밝기의 피사체에 향하게 했을 때 노출 값이 실시간으로 변경되는지 확인

### Tests for User Story 1

- [x] T014 [P] [US1] app/src/test/java/io/github/sangpire/ssreader/ui/lightmeter/LightMeterViewModelTest.kt에 측광 결과 업데이트 테스트 작성

### Implementation for User Story 1

- [x] T015 [P] [US1] app/src/main/java/io/github/sangpire/ssreader/camera/LightMeterAnalyzer.kt에 CameraX ImageAnalysis.Analyzer 구현 (Y plane 평균 밝기 측정)
- [x] T016 [US1] app/src/main/java/io/github/sangpire/ssreader/ui/lightmeter/LightMeterViewModel.kt에 ViewModel 기본 구조 생성 (상태 관리, 측광 결과 처리)
- [x] T017 [P] [US1] app/src/main/java/io/github/sangpire/ssreader/ui/lightmeter/components/CameraPreview.kt에 카메라 프리뷰 Composable 구현 (AndroidView + PreviewView)
- [x] T018 [P] [US1] app/src/main/java/io/github/sangpire/ssreader/ui/lightmeter/components/ExposureValueDisplay.kt에 노출 값 표시 Composable 기본 구현 (표시만, 상호작용 없음)
- [x] T019 [US1] app/src/main/java/io/github/sangpire/ssreader/ui/lightmeter/LightMeterScreen.kt에 메인 화면 Composable 구현 (전체 화면 프리뷰 + 오른쪽 아래 노출 값)
- [x] T020 [US1] app/src/main/java/io/github/sangpire/ssreader/MainActivity.kt 수정하여 LightMeterScreen을 메인 화면으로 설정
- [x] T021 [US1] 카메라 권한 요청 로직 구현 (권한 거부 시 안내 화면 표시)

**Checkpoint**: US1 완료 - 실시간 노출 측정 및 표시 기능 동작 확인

---

## Phase 4: User Story 2 - 특정 값 고정 및 나머지 값 자동 계산 (Priority: P1)

**Goal**: 사용자가 ISO, 조리개, 셔터스피드 중 하나 이상의 값을 고정하면 나머지 값이 적정 노출에 맞게 자동 계산됨

**Independent Test**: ISO를 100으로 고정한 후 빛 조건 변경 시 ISO는 유지되고 조리개/셔터스피드만 변경되는지 확인

### Tests for User Story 2

- [x] T022 [P] [US2] app/src/test/java/io/github/sangpire/ssreader/ui/lightmeter/LightMeterViewModelLockTest.kt에 값 고정/해제 및 자동 계산 테스트 추가

### Implementation for User Story 2

- [x] T023 [US2] app/src/main/java/io/github/sangpire/ssreader/ui/lightmeter/LightMeterViewModel.kt에 toggleLock() 메서드 구현
- [x] T024 [US2] app/src/main/java/io/github/sangpire/ssreader/domain/ExposureCalculator.kt에 고정된 값 기반 적정 노출 계산 로직 추가 (Phase 2에서 이미 구현됨)
- [x] T025 [US2] app/src/main/java/io/github/sangpire/ssreader/ui/lightmeter/components/ExposureValueDisplay.kt에 고정 상태 시각적 표시 추가 (잠금 아이콘, 배경색 등)
- [x] T026 [US2] app/src/main/java/io/github/sangpire/ssreader/ui/lightmeter/components/ExposureValueDisplay.kt에 탭으로 고정/해제 토글 기능 추가
- [x] T027 [US2] 세 값 모두 고정 시 노출 보정 표시 구현 (ExposureValueDisplay에 통합)

**Checkpoint**: US2 완료 - 값 고정 및 자동 계산 기능 동작 확인

---

## Phase 5: User Story 3 - 스와이프로 노출 값 조절 (Priority: P2)

**Goal**: 사용자가 각 노출 값을 터치 후 좌우 스와이프로 값을 변경할 수 있음

**Independent Test**: ISO 값을 터치하고 오른쪽 스와이프 시 값 증가, 왼쪽 스와이프 시 값 감소 확인

### Implementation for User Story 3

- [x] T028 [US3] app/src/main/java/io/github/sangpire/ssreader/ui/lightmeter/LightMeterViewModel.kt에 changeExposureValue() 메서드 구현
- [x] T029 [US3] app/src/main/java/io/github/sangpire/ssreader/ui/lightmeter/components/ExposureValueDisplay.kt에 수평 드래그 제스처 추가 (detectHorizontalDragGestures)
- [x] T030 [US3] 스와이프 시 햅틱 피드백 추가 (LocalHapticFeedback)
- [x] T031 [US3] 스와이프 시 값 변경 애니메이션 추가

**Checkpoint**: US3 완료 - 스와이프로 노출 값 조절 기능 동작 확인

---

## Phase 6: User Story 4 - 셔터 버튼으로 화면 정지 (Priority: P2)

**Goal**: 셔터 버튼을 누르면 카메라 화면이 정지되고, 정지 상태에서도 노출 값 조절 가능

**Independent Test**: 셔터 버튼 클릭 시 화면 정지, 정지 상태에서 ISO 변경 시 나머지 값 재계산 확인

### Implementation for User Story 4

- [x] T032 [P] [US4] app/src/main/java/io/github/sangpire/ssreader/ui/lightmeter/components/ShutterButton.kt에 셔터 버튼 Composable 생성
- [x] T033 [US4] app/src/main/java/io/github/sangpire/ssreader/ui/lightmeter/LightMeterViewModel.kt에 onShutterClick() 및 화면 정지 상태 관리 구현
- [x] T034 [US4] app/src/main/java/io/github/sangpire/ssreader/camera/LightMeterAnalyzer.kt에 현재 프레임 Bitmap 캡처 기능 추가
- [x] T035 [US4] app/src/main/java/io/github/sangpire/ssreader/ui/lightmeter/components/CameraPreview.kt 수정하여 정지 시 캡처된 Bitmap 표시
- [x] T036 [US4] app/src/main/java/io/github/sangpire/ssreader/ui/lightmeter/LightMeterScreen.kt에 셔터 버튼 배치 (하단 중앙)

**Checkpoint**: US4 완료 - 셔터 버튼으로 화면 정지 및 재개 기능 동작 확인

---

## Phase 7: Polish & Cross-Cutting Concerns (마무리)

**Purpose**: 전체 기능에 걸친 개선 및 품질 보증

- [x] T037 [P] 다크 모드 지원 확인 및 테마 조정
- [x] T038 [P] 접근성(a11y) contentDescription 추가
- [x] T039 극단적 밝기 조건에서 범위 한계 경고 표시 구현 (FR-015)
- [x] T040 앱 백그라운드 복귀 시 실시간 모드로 복귀 로직 구현
- [x] T041 [P] KDoc 문서화 추가 (public API)
- [x] T042 quickstart.md 검증 실행 및 확인

---

## Dependencies & Execution Order

### Phase Dependencies

- **Phase 1 (Setup)**: 의존성 없음 - 즉시 시작 가능
- **Phase 2 (Foundational)**: Phase 1 완료 필요 - 모든 사용자 스토리 차단
- **Phase 3 (US1)**: Phase 2 완료 필요 - MVP
- **Phase 4 (US2)**: Phase 3 완료 필요 (US1 UI에 고정 기능 추가)
- **Phase 5 (US3)**: Phase 4 완료 필요 (고정 로직 위에 스와이프 추가)
- **Phase 6 (US4)**: Phase 3 완료 필요 (US1 완료 후 독립 구현 가능)
- **Phase 7 (Polish)**: 모든 사용자 스토리 완료 필요

### User Story Dependencies

```
Phase 2 (Foundation)
       │
       ▼
Phase 3 (US1: 실시간 노출 확인) ◄─── MVP
       │
       ├──────────────────┐
       ▼                  ▼
Phase 4 (US2: 값 고정)   Phase 6 (US4: 화면 정지)
       │
       ▼
Phase 5 (US3: 스와이프 조절)
       │
       ▼
Phase 7 (Polish)
```

### Parallel Opportunities

**Phase 1 내:**
- T003, T004 병렬 실행 가능

**Phase 2 내:**
- T005, T006 병렬 실행 가능

**Phase 3 내:**
- T015, T017, T018 병렬 실행 가능 (모델 완료 후)

**Phase 4와 Phase 6:**
- US4는 US1 완료 후 US2와 병렬로 진행 가능 (별도 팀원이 있는 경우)

---

## Parallel Example: User Story 1

```bash
# Phase 2 완료 후 US1 모델/컴포넌트 병렬 실행:
Task: "T015 [P] [US1] LightMeterAnalyzer.kt에 ImageAnalysis.Analyzer 구현"
Task: "T017 [P] [US1] CameraPreview.kt에 카메라 프리뷰 Composable 구현"
Task: "T018 [P] [US1] ExposureValueDisplay.kt에 노출 값 표시 Composable 구현"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Phase 1: Setup 완료
2. Phase 2: Foundational 완료 (CRITICAL)
3. Phase 3: User Story 1 완료
4. **STOP and VALIDATE**: 실시간 노출 측정 및 표시 테스트
5. 필요시 배포/데모

### Incremental Delivery

1. Setup + Foundational → 기반 완료
2. User Story 1 → 독립 테스트 → 배포/데모 (MVP!)
3. User Story 2 → 독립 테스트 → 배포/데모 (값 고정 기능 추가)
4. User Story 3 → 독립 테스트 → 배포/데모 (스와이프 조절 추가)
5. User Story 4 → 독립 테스트 → 배포/데모 (화면 정지 기능 추가)
6. Polish → 최종 품질 보증

---

## Notes

- [P] 태스크 = 다른 파일, 의존성 없음
- [Story] 라벨은 태스크를 특정 사용자 스토리에 매핑
- 각 사용자 스토리는 독립적으로 완료 및 테스트 가능해야 함
- 테스트는 구현 전에 작성하고 실패 확인
- 각 태스크 또는 논리적 그룹 완료 후 커밋
- 체크포인트에서 스토리 독립 검증 가능
