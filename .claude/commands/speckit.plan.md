---
description: 계획 템플릿을 사용하여 구현 계획 워크플로우를 실행하고 설계 산출물을 생성합니다.
handoffs:
  - label: 태스크 생성
    agent: speckit.tasks
    prompt: 계획을 태스크로 분해
    send: true
  - label: 체크리스트 생성
    agent: speckit.checklist
    prompt: 다음 도메인에 대한 체크리스트 생성...
---

## 사용자 입력

```text
$ARGUMENTS
```

사용자 입력이 비어있지 않다면 **반드시** 먼저 고려해야 합니다.

## 개요

1. **설정**: 저장소 루트에서 `.specify/scripts/bash/setup-plan.sh --json`을 실행하고 JSON에서 FEATURE_SPEC, IMPL_PLAN, SPECS_DIR, BRANCH를 파싱합니다. "I'm Groot" 같은 작은따옴표가 있는 인자는 이스케이프 문법을 사용하세요: 예: 'I'\''m Groot' (또는 가능하면 큰따옴표 사용: "I'm Groot").

2. **컨텍스트 로드**: FEATURE_SPEC과 `.specify/memory/constitution.md`를 읽습니다. IMPL_PLAN 템플릿을 로드합니다 (이미 복사됨).

3. **계획 워크플로우 실행**: IMPL_PLAN 템플릿의 구조를 따라:
   - 기술 컨텍스트 채우기 (알 수 없는 것은 "NEEDS CLARIFICATION"으로 표시)
   - 헌법에서 Constitution Check 섹션 채우기
   - 게이트 평가 (정당화되지 않은 위반 시 ERROR)
   - Phase 0: research.md 생성 (모든 NEEDS CLARIFICATION 해결)
   - Phase 1: data-model.md, contracts/, quickstart.md 생성
   - Phase 1: 에이전트 스크립트를 실행하여 에이전트 컨텍스트 업데이트
   - 설계 후 Constitution Check 재평가

4. **중지 및 보고**: Phase 2 계획 후 명령 종료. 브랜치, IMPL_PLAN 경로, 생성된 산출물 보고.

## 페이즈

### Phase 0: 개요 및 리서치

1. **위의 기술 컨텍스트에서 미지수 추출**:
   - 각 NEEDS CLARIFICATION → 리서치 태스크
   - 각 의존성 → 모범 사례 태스크
   - 각 통합 → 패턴 태스크

2. **리서치 에이전트 생성 및 디스패치**:

   ```text
   기술 컨텍스트의 각 미지수에 대해:
     Task: "{feature context}에 대한 {unknown} 리서치"
   각 기술 선택에 대해:
     Task: "{domain}에서 {tech}에 대한 모범 사례 찾기"
   ```

3. **발견 사항을 `research.md`에 통합** 형식 사용:
   - Decision: [선택된 것]
   - Rationale: [선택 이유]
   - Alternatives considered: [평가된 다른 것]

**출력**: 모든 NEEDS CLARIFICATION이 해결된 research.md

### Phase 1: 설계 및 컨트랙트

**선행 조건:** `research.md` 완료

1. **기능 스펙에서 엔티티 추출** → `data-model.md`:
   - 엔티티 이름, 필드, 관계
   - 요구사항에서 검증 규칙
   - 해당되는 경우 상태 전환

2. **기능 요구사항에서 API 컨트랙트 생성**:
   - 각 사용자 액션 → 엔드포인트
   - 표준 REST/GraphQL 패턴 사용
   - `/contracts/`에 OpenAPI/GraphQL 스키마 출력

3. **에이전트 컨텍스트 업데이트**:
   - `.specify/scripts/bash/update-agent-context.sh claude` 실행
   - 이 스크립트들은 사용 중인 AI 에이전트를 감지
   - 적절한 에이전트별 컨텍스트 파일 업데이트
   - 현재 계획의 새 기술만 추가
   - 마커 사이의 수동 추가 사항 보존

**출력**: data-model.md, /contracts/*, quickstart.md, 에이전트별 파일

## 주요 규칙

- 절대 경로 사용
- 게이트 실패 또는 미해결 명확화 시 ERROR
