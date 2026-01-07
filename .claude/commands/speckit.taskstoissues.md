---
description: 사용 가능한 설계 산출물을 기반으로 기존 태스크를 실행 가능하고 의존성 순서가 지정된 GitHub 이슈로 변환합니다.
tools: ['github/github-mcp-server/issue_write']
---

## 사용자 입력

```text
$ARGUMENTS
```

사용자 입력이 비어있지 않다면 **반드시** 먼저 고려해야 합니다.

## 개요

1. 저장소 루트에서 `.specify/scripts/bash/check-prerequisites.sh --json --require-tasks --include-tasks`를 실행하고 FEATURE_DIR과 AVAILABLE_DOCS 목록을 파싱합니다. 모든 경로는 절대 경로여야 합니다. "I'm Groot" 같은 작은따옴표가 있는 인자는 이스케이프 문법을 사용하세요: 예: 'I'\''m Groot' (또는 가능하면 큰따옴표 사용: "I'm Groot").
1. 실행된 스크립트에서 **tasks** 경로를 추출합니다.
1. 다음을 실행하여 Git 원격을 가져옵니다:

```bash
git config --get remote.origin.url
```

> [!CAUTION]
> 원격이 GITHUB URL인 경우에만 다음 단계로 진행하세요

1. 목록의 각 태스크에 대해 GitHub MCP 서버를 사용하여 Git 원격을 대표하는 저장소에 새 이슈를 생성합니다.

> [!CAUTION]
> 어떤 상황에서도 원격 URL과 일치하지 않는 저장소에 이슈를 생성하지 마세요
