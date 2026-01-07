---
description: tasks.md에 정의된 모든 태스크를 처리하고 실행하여 구현 계획을 실행합니다.
---

## 사용자 입력

```text
$ARGUMENTS
```

사용자 입력이 비어있지 않다면 **반드시** 먼저 고려해야 합니다.

## 개요

1. 저장소 루트에서 `.specify/scripts/bash/check-prerequisites.sh --json --require-tasks --include-tasks`를 실행하고 FEATURE_DIR과 AVAILABLE_DOCS 목록을 파싱합니다. 모든 경로는 절대 경로여야 합니다. "I'm Groot" 같은 작은따옴표가 있는 인자는 이스케이프 문법을 사용하세요: 예: 'I'\''m Groot' (또는 가능하면 큰따옴표 사용: "I'm Groot").

2. **체크리스트 상태 확인** (FEATURE_DIR/checklists/가 존재하는 경우):
   - checklists/ 디렉토리의 모든 체크리스트 파일 스캔
   - 각 체크리스트에 대해 카운트:
     - 전체 항목: `- [ ]` 또는 `- [X]` 또는 `- [x]`와 일치하는 모든 라인
     - 완료된 항목: `- [X]` 또는 `- [x]`와 일치하는 라인
     - 미완료 항목: `- [ ]`와 일치하는 라인
   - 상태 테이블 생성:

     ```text
     | 체크리스트 | 전체 | 완료 | 미완료 | 상태 |
     |-----------|------|------|--------|------|
     | ux.md     | 12   | 12   | 0      | ✓ 통과 |
     | test.md   | 8    | 5    | 3      | ✗ 실패 |
     | security.md | 6  | 6    | 0      | ✓ 통과 |
     ```

   - 전체 상태 계산:
     - **통과**: 모든 체크리스트에 미완료 항목이 0개
     - **실패**: 하나 이상의 체크리스트에 미완료 항목이 있음

   - **체크리스트가 미완료인 경우**:
     - 미완료 항목 수가 포함된 테이블 표시
     - **중지**하고 질문: "일부 체크리스트가 미완료입니다. 그래도 구현을 진행하시겠습니까? (yes/no)"
     - 계속하기 전에 사용자 응답 대기
     - 사용자가 "no" 또는 "wait" 또는 "stop"이라고 하면 실행 중단
     - 사용자가 "yes" 또는 "proceed" 또는 "continue"라고 하면 3단계로 진행

   - **모든 체크리스트가 완료된 경우**:
     - 모든 체크리스트가 통과했음을 보여주는 테이블 표시
     - 자동으로 3단계로 진행

3. 구현 컨텍스트 로드 및 분석:
   - **필수**: tasks.md에서 전체 태스크 목록과 실행 계획 읽기
   - **필수**: plan.md에서 기술 스택, 아키텍처, 파일 구조 읽기
   - **있으면**: data-model.md에서 엔티티와 관계 읽기
   - **있으면**: contracts/에서 API 명세와 테스트 요구사항 읽기
   - **있으면**: research.md에서 기술 결정과 제약 조건 읽기
   - **있으면**: quickstart.md에서 통합 시나리오 읽기

4. **프로젝트 설정 검증**:
   - **필수**: 실제 프로젝트 설정에 따라 ignore 파일 생성/검증:

   **감지 및 생성 로직**:
   - 저장소가 git 저장소인지 확인하려면 다음 명령이 성공하는지 확인 (그렇다면 .gitignore 생성/검증):

     ```sh
     git rev-parse --git-dir 2>/dev/null
     ```

   - Dockerfile*이 존재하거나 plan.md에 Docker가 있으면 → .dockerignore 생성/검증
   - .eslintrc*가 존재하면 → .eslintignore 생성/검증
   - eslint.config.*가 존재하면 → 설정의 `ignores` 항목이 필수 패턴을 커버하는지 확인
   - .prettierrc*가 존재하면 → .prettierignore 생성/검증
   - .npmrc 또는 package.json이 존재하면 → .npmignore 생성/검증 (퍼블리싱하는 경우)
   - terraform 파일 (*.tf)이 존재하면 → .terraformignore 생성/검증
   - .helmignore가 필요하면 (helm 차트가 있음) → .helmignore 생성/검증

   **ignore 파일이 이미 존재하면**: 필수 패턴이 포함되어 있는지 확인, 누락된 중요 패턴만 추가
   **ignore 파일이 없으면**: 감지된 기술에 대한 전체 패턴 세트로 생성

   **기술별 일반 패턴** (plan.md 기술 스택에서):
   - **Node.js/JavaScript/TypeScript**: `node_modules/`, `dist/`, `build/`, `*.log`, `.env*`
   - **Python**: `__pycache__/`, `*.pyc`, `.venv/`, `venv/`, `dist/`, `*.egg-info/`
   - **Java**: `target/`, `*.class`, `*.jar`, `.gradle/`, `build/`
   - **C#/.NET**: `bin/`, `obj/`, `*.user`, `*.suo`, `packages/`
   - **Go**: `*.exe`, `*.test`, `vendor/`, `*.out`
   - **Ruby**: `.bundle/`, `log/`, `tmp/`, `*.gem`, `vendor/bundle/`
   - **PHP**: `vendor/`, `*.log`, `*.cache`, `*.env`
   - **Rust**: `target/`, `debug/`, `release/`, `*.rs.bk`, `*.rlib`, `*.prof*`, `.idea/`, `*.log`, `.env*`
   - **Kotlin**: `build/`, `out/`, `.gradle/`, `.idea/`, `*.class`, `*.jar`, `*.iml`, `*.log`, `.env*`
   - **C++**: `build/`, `bin/`, `obj/`, `out/`, `*.o`, `*.so`, `*.a`, `*.exe`, `*.dll`, `.idea/`, `*.log`, `.env*`
   - **C**: `build/`, `bin/`, `obj/`, `out/`, `*.o`, `*.a`, `*.so`, `*.exe`, `Makefile`, `config.log`, `.idea/`, `*.log`, `.env*`
   - **Swift**: `.build/`, `DerivedData/`, `*.swiftpm/`, `Packages/`
   - **R**: `.Rproj.user/`, `.Rhistory`, `.RData`, `.Ruserdata`, `*.Rproj`, `packrat/`, `renv/`
   - **공통**: `.DS_Store`, `Thumbs.db`, `*.tmp`, `*.swp`, `.vscode/`, `.idea/`

   **도구별 패턴**:
   - **Docker**: `node_modules/`, `.git/`, `Dockerfile*`, `.dockerignore`, `*.log*`, `.env*`, `coverage/`
   - **ESLint**: `node_modules/`, `dist/`, `build/`, `coverage/`, `*.min.js`
   - **Prettier**: `node_modules/`, `dist/`, `build/`, `coverage/`, `package-lock.json`, `yarn.lock`, `pnpm-lock.yaml`
   - **Terraform**: `.terraform/`, `*.tfstate*`, `*.tfvars`, `.terraform.lock.hcl`
   - **Kubernetes/k8s**: `*.secret.yaml`, `secrets/`, `.kube/`, `kubeconfig*`, `*.key`, `*.crt`

5. tasks.md 구조 파싱 및 추출:
   - **태스크 페이즈**: 설정, 테스트, 코어, 통합, 마무리
   - **태스크 의존성**: 순차 vs 병렬 실행 규칙
   - **태스크 세부사항**: ID, 설명, 파일 경로, 병렬 마커 [P]
   - **실행 흐름**: 순서 및 의존성 요구사항

6. 태스크 계획에 따라 구현 실행:
   - **페이즈별 실행**: 다음 페이즈로 이동하기 전에 각 페이즈 완료
   - **의존성 존중**: 순차 태스크는 순서대로, 병렬 태스크 [P]는 함께 실행 가능
   - **TDD 접근 방식 따르기**: 해당 구현 태스크 전에 테스트 태스크 실행
   - **파일 기반 조정**: 동일 파일에 영향을 미치는 태스크는 순차적으로 실행
   - **검증 체크포인트**: 진행하기 전에 각 페이즈 완료 확인

7. 구현 실행 규칙:
   - **설정 우선**: 프로젝트 구조, 의존성, 설정 초기화
   - **코드 전에 테스트**: 컨트랙트, 엔티티, 통합 시나리오에 대한 테스트 작성이 필요한 경우
   - **코어 개발**: 모델, 서비스, CLI 명령, 엔드포인트 구현
   - **통합 작업**: 데이터베이스 연결, 미들웨어, 로깅, 외부 서비스
   - **마무리 및 검증**: 단위 테스트, 성능 최적화, 문서화

8. 진행 상황 추적 및 에러 처리:
   - 완료된 각 태스크 후 진행 상황 보고
   - 비병렬 태스크가 실패하면 실행 중단
   - 병렬 태스크 [P]의 경우, 성공한 태스크는 계속, 실패한 것은 보고
   - 디버깅을 위한 컨텍스트와 함께 명확한 에러 메시지 제공
   - 구현을 진행할 수 없는 경우 다음 단계 제안
   - **중요** 완료된 태스크의 경우, tasks 파일에서 태스크를 [X]로 표시해야 합니다.

9. 완료 검증:
   - 모든 필수 태스크가 완료되었는지 확인
   - 구현된 기능이 원래 명세와 일치하는지 확인
   - 테스트가 통과하고 커버리지가 요구사항을 충족하는지 검증
   - 구현이 기술 계획을 따르는지 확인
   - 완료된 작업 요약과 함께 최종 상태 보고

참고: 이 명령은 tasks.md에 완전한 태스크 분해가 존재한다고 가정합니다. 태스크가 불완전하거나 누락된 경우, 먼저 `/speckit.tasks`를 실행하여 태스크 목록을 재생성하는 것을 제안하세요.
