---
name: commit-and-document
description: 커밋하고 푸시한 후, 작업 내용을 Notion 기록 보드에 자동으로 문서화합니다. 커밋 메시지와 변경 사항을 기반으로 구조화된 문서를 생성합니다.
---

# Commit and Document

커밋, 푸시 후 Notion에 작업 내용을 자동으로 문서화하는 스킬입니다.

## 워크플로우

### Step 1: 커밋 준비

```
1. git status로 변경 사항 확인
2. git diff로 상세 변경 내용 파악
3. git log로 최근 커밋 스타일 확인
```

### Step 2: 커밋 & 푸시

```
1. 변경된 파일들을 staging (git add)
2. 의미 있는 커밋 메시지 작성
3. 원격 저장소에 푸시 (git push)
```

### Step 3: Notion 문서화 (Knowledge Capture)

커밋 완료 후, 다음 정보를 추출하여 Notion에 저장:

```
추출할 정보:
- 커밋 메시지 (무엇을 했는지)
- 변경된 파일 목록
- 주요 변경 사항 요약
- 카테고리 분류 (기능 구현, 버그 수정, 리팩토링 등)
```

### Step 4: Notion 페이지 생성

```
1. Notion:notion-search로 "기록 보드" 데이터베이스 찾기
2. Notion:notion-create-pages로 새 항목 추가
   - Name: 커밋 메시지 또는 작업 제목
   - Category: 적절한 카테고리 선택
   - 본문: 상세 변경 내용
```

## 카테고리 매핑

| 커밋 타입 | Notion 카테고리 |
|-----------|-----------------|
| feat, feature | 기능 구현 |
| fix, bugfix | 버그 수정 |
| refactor | 성능 최적화 |
| docs | 기획/설계 |
| security | 안정성/아키텍처 |
| perf, optimize | 성능 최적화 |

## 문서 템플릿

```markdown
## 개요
[커밋 메시지 요약]

## 변경 사항
- [변경된 파일 1]: [변경 내용]
- [변경된 파일 2]: [변경 내용]

## 상세 내용
[대화에서 논의된 내용, 결정 사항, 구현 방식 등]

## 관련 커밋
- [커밋 해시]: [커밋 메시지]
```

## 사용 예시

사용자가 다음과 같이 요청하면 이 스킬이 트리거됩니다:

- "커밋하고 Notion에 문서화해줘"
- "푸시하고 기록 보드에 정리해줘"
- "커밋 후 작업 내용 Notion에 저장해줘"
- `/commit-and-document`
