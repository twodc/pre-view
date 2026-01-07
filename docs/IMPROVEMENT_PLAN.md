# 백엔드 개선 계획서

> 작성일: 2025-01-06
> 분석 대상: pre-view 백엔드 (Spring Boot 4.0 + JWT + OAuth2)

---

## 1. 긴급 개선 (High Priority)

### 1.1 면접 권한 검증 부재 (보안)

**현재 문제:**
- `Interview` 엔티티에 `memberId`가 없음
- 면접 ID만 알면 누구나 해당 면접에 답변 가능
- 다른 사용자의 면접 결과 조회 가능

**영향 받는 파일:**
- `Interview.java` - memberId 필드 추가 필요
- `InterviewService.java` - 면접 생성 시 memberId 설정
- `AnswerFacade.java` - 답변 제출 시 권한 검증
- `InterviewController.java` - 조회/삭제 시 권한 검증

**해결 방안:**
```java
// Interview.java
@Column(name = "member_id")
private Long memberId;

// InterviewService.java - 생성 시
interview.setMemberId(getCurrentMemberId());

// 검증 로직
if (!interview.getMemberId().equals(getCurrentMemberId())) {
    throw new BusinessException(ErrorCode.ACCESS_DENIED);
}
```

---

### 1.2 로그아웃 토큰 파싱 취약점 (보안)

**현재 문제:**
- `AuthController.java:88-89`에서 `bearerToken.substring(7)` 직접 호출
- Authorization 헤더가 없거나 "Bearer " 접두사가 없으면 예외 발생

**영향 받는 파일:**
- `AuthController.java`

**해결 방안:**
```java
@PostMapping("/logout")
public ResponseEntity<ApiResponse<Void>> logout(
        @RequestHeader(value = "Authorization", required = false) String bearerToken
) {
    if (!StringUtils.hasText(bearerToken) || !bearerToken.startsWith("Bearer ")) {
        throw new BusinessException(ErrorCode.INVALID_TOKEN);
    }
    String accessToken = bearerToken.substring(7);
    authService.logout(accessToken);
    return ResponseEntity.ok(ApiResponse.ok("로그아웃 되었습니다."));
}
```

---

### 1.3 파일 업로드 검증 강화 (보안)

**현재 문제:**
- `PdfExtractionService`에 검증 로직이 있으나 `FileUploadService`에서 먼저 검증하지 않음
- Controller 레벨에서 파일 크기/유형 사전 검증 없음

**영향 받는 파일:**
- `InterviewController.java` - 파일 유효성 사전 검증
- `FileUploadService.java` - 검증 로직 호출 순서

**해결 방안:**
```java
// InterviewController.java
@PostMapping("/{id}/resume")
public ResponseEntity<ApiResponse<InterviewResponse>> uploadResume(
    @PathVariable("id") Long id,
    @RequestParam("file") MultipartFile file
) {
    // 빈 파일 검증
    if (file == null || file.isEmpty()) {
        throw new BusinessException(ErrorCode.FILE_UPLOAD_ERROR);
    }
    // ... 기존 로직
}
```

---

### 1.4 Follow-up 질문 제한 미적용 (기능)

**현재 문제:**
- AI 프롬프트에만 "꼬리 질문 최대 2회" 제한이 있음
- 서버에서 강제하지 않아 AI가 제한을 어길 수 있음

**영향 받는 파일:**
- `AnswerFacade.java`

**해결 방안:**
```java
// AnswerFacade.java - AI 호출 전
int followUpDepth = questionService.calculateFollowUpDepth(question);
if (followUpDepth >= 2) {
    // AI 호출 없이 다음 단계로 강제 전환
    AiInterviewAgentResponse forcedResponse = new AiInterviewAgentResponse(
            "꼬리 질문 제한에 도달하여 다음 단계로 넘어갑니다.",
            InterviewAction.NEXT_PHASE,
            null,
            null
    );
    return answerService.saveAnswerWithAgentResult(question, request.content(), aiFeedback, forcedResponse);
}
```

---

### 1.5 테스트 코드 작성 (품질)

**현재 문제:**
- `PreViewApplicationTests.java`에 기본 로드 테스트만 존재
- 비즈니스 로직 테스트 부재

**필요한 테스트:**

| 대상 | 테스트 케이스 |
|------|--------------|
| `AuthService` | 회원가입 (중복 이메일, 유효성), 로그인 (비밀번호 검증), 토큰 재발급 |
| `JwtTokenProvider` | 토큰 생성/파싱, 만료 검증 |
| `InterviewService` | 면접 생성/삭제, 상태 전환, 권한 검증 |
| `AnswerFacade` | AI 호출 흐름, Fallback 처리 |
| `FileUploadService` | PDF 추출, 파일 검증 |

---

## 2. 중요 개선 (Medium Priority)

### 2.1 회원 정보 API 추가 (기능)

**현재 문제:**
- 사용자가 자신의 정보를 조회/수정할 수 없음

**필요한 API:**
```
GET  /api/v1/members/me     - 현재 사용자 정보 조회
PUT  /api/v1/members/me     - 프로필 수정 (이름, 프로필 이미지)
```

**필요한 파일:**
- `MemberController.java` (신규)
- `MemberService.java` (신규)
- `MemberResponse.java` (신규)

---

### 2.2 쿼리 최적화 (성능)

**현재 문제:**
- `QuestionService.getPreviousAnswers()`가 전체 답변 조회 후 필터링
- Interview 목록 조회 시 인덱스 없음

**해결 방안:**

```java
// AnswerRepository.java 추가
@Query("SELECT a FROM Answer a JOIN FETCH a.question q " +
       "WHERE q.interview.id = :interviewId AND q.phase = :phase")
List<Answer> findByInterviewIdAndPhase(
    @Param("interviewId") Long interviewId,
    @Param("phase") InterviewPhase phase
);
```

```sql
-- 인덱스 추가 (DB)
CREATE INDEX idx_interview_deleted_created ON interview(deleted, created_at DESC);
CREATE INDEX idx_question_interview_phase ON question(interview_id, phase);
```

---

### 2.3 로그인 재시도 제한 (보안)

**현재 문제:**
- 비밀번호 오류 시 재시도 횟수 제한 없음
- Brute Force 공격에 취약

**해결 방안:**
```java
// AuthService.java
private static final int MAX_LOGIN_ATTEMPTS = 5;
private static final long LOCK_DURATION_MINUTES = 5;

public TokenResponse login(LoginRequest request) {
    String key = "login_attempt:" + request.email();

    // 잠금 확인
    if (isAccountLocked(key)) {
        throw new BusinessException(ErrorCode.ACCOUNT_LOCKED);
    }

    try {
        // 기존 로그인 로직
        // 성공 시 시도 횟수 초기화
        resetLoginAttempts(key);
    } catch (BusinessException e) {
        incrementLoginAttempts(key);
        throw e;
    }
}
```

**필요한 ErrorCode:**
```java
ACCOUNT_LOCKED(HttpStatus.TOO_MANY_REQUESTS, "AUTH007", "로그인 시도 횟수를 초과했습니다. 5분 후 다시 시도해주세요.")
```

---

### 2.4 민감 정보 암호화 (보안)

**현재 문제:**
- 이력서/포트폴리오 텍스트가 DB에 평문 저장
- AI 리포트 캐시도 평문 저장

**해결 방안:**
- AES-256 암호화 적용
- 암호화 키는 환경변수로 관리

---

## 3. 참고 개선 (Low Priority)

### 3.1 추가 기능

| 기능 | 설명 | API |
|------|------|-----|
| 면접 통계 | 총 횟수, 평균 점수 등 | `GET /api/v1/interviews/stats` |
| 면접 일시 중지/재개 | 진행 중 일시 정지 | `PUT /api/v1/interviews/{id}/pause` |
| 답변 임시 저장 | 제출 전 임시 저장 | `POST /api/v1/interviews/{id}/questions/{qId}/draft` |
| 결과 공유 | 공유 링크 생성 | `GET /api/v1/interviews/{id}/share-token` |

---

## 4. 현재 강점

- JWT + OAuth2 인증 체계 완성
- Refresh Token Rotation으로 토큰 탈취 방지
- Optimistic Locking으로 동시성 제어
- AI 리포트 캐싱으로 성능 최적화
- Facade 패턴으로 비즈니스 흐름 분리
- Resilience4j Retry로 AI API 안정성 확보

---

## 5. 작업 순서 권장

```
1단계: 보안 (1.1 → 1.2 → 1.3)
   └─ 면접 권한 검증 → 로그아웃 안전성 → 파일 검증

2단계: 기능 정합성 (1.4)
   └─ Follow-up 질문 제한

3단계: 기능 추가 (2.1)
   └─ 회원 정보 API

4단계: 성능 (2.2)
   └─ 쿼리 최적화

5단계: 테스트 (1.5)
   └─ 핵심 비즈니스 로직 테스트

6단계: 추가 보안 (2.3 → 2.4)
   └─ 로그인 제한 → 암호화
```

---

## 변경 이력

| 날짜 | 내용 |
|------|------|
| 2025-01-06 | 초기 분석 문서 작성 |
