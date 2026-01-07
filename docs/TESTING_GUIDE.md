# 테스트 코드 리팩토링 가이드

## 완료된 작업 (2026-01-07)

### 1. 의존성 업데이트 (`build.gradle`)
- ✅ H2 인메모리 DB 제거
- ✅ MySQL Testcontainers 추가 (`org.testcontainers:mysql:1.20.4`)
- ✅ JUnit Jupiter 통합 (`org.testcontainers:junit-jupiter:1.20.4`)
- ✅ Awaitility 추가 (`org.awaitility:awaitility:4.2.2`)

### 2. 테스트 인프라 구성
- ✅ `IntegrationTestSupport.java` - MySQL + Redis Testcontainers 베이스 클래스
- ✅ `application-integration.yaml` - H2 설정 제거, MySQL Dialect 설정
- ✅ `PreViewApplicationTests.java` - Testcontainers 기반으로 변경

### 3. 예제 테스트 코드
- ✅ `MemberServiceTest.java` - 단위 테스트 (Mockito + DCI 패턴)
- ✅ `MemberServiceIntegrationTest.java` - 통합 테스트 (Testcontainers + DCI 패턴)

---

## 다음 작업을 위한 프롬프트

### 프롬프트 1: Repository 레이어 테스트 추가

```
@DataJpaTest와 Testcontainers를 사용하여 MemberRepository 테스트를 작성해주세요.

요구사항:
1. @DataJpaTest + @Testcontainers 조합 사용
2. MySQL Testcontainers로 실제 DB 쿼리 검증
3. DCI 패턴 (Describe-Context-It) 적용
4. 다음 메서드 테스트:
   - findByEmail()
   - existsByEmail()
5. 커스텀 JPQL 쿼리가 있다면 해당 쿼리도 테스트

참고 파일:
- src/test/java/com/example/pre_view/support/IntegrationTestSupport.java
- src/main/java/com/example/pre_view/domain/member/repository/MemberRepository.java
```

### 프롬프트 2: Controller 레이어 테스트 추가

```
@WebMvcTest를 사용하여 MemberController 테스트를 작성해주세요.

요구사항:
1. @WebMvcTest 슬라이스 테스트 사용 (Spring Context 최소 로딩)
2. MockMvc로 HTTP 요청/응답 검증
3. @MockitoBean으로 Service 레이어 Mock
4. Spring Security 테스트 설정 포함
5. DCI 패턴 적용
6. 다음 시나리오 테스트:
   - 회원 정보 조회 성공/실패
   - 프로필 수정 성공/실패
   - 인증 실패 케이스

참고 파일:
- src/main/java/com/example/pre_view/domain/member/controller/MemberController.java
- src/test/java/com/example/pre_view/domain/auth/controller/TestSecurityConfig.java
```

### 프롬프트 3: Interview 도메인 통합 테스트

```
InterviewService 통합 테스트를 작성해주세요.

요구사항:
1. IntegrationTestSupport 상속
2. 실제 DB 트랜잭션 검증
3. AI 서비스는 @MockitoBean으로 Mock 처리
4. DCI 패턴 적용
5. 다음 시나리오 테스트:
   - 인터뷰 생성
   - 인터뷰 시작
   - 인터뷰 상태 변경 (Optimistic Lock 검증)

참고 파일:
- src/main/java/com/example/pre_view/domain/interview/service/InterviewService.java
- src/main/java/com/example/pre_view/domain/interview/entity/Interview.java
```

### 프롬프트 4: 테스트 커버리지 개선

```
현재 테스트 커버리지를 분석하고 개선점을 제안해주세요.

요구사항:
1. ./gradlew test jacocoTestReport 실행
2. build/reports/jacoco/test/html/index.html 분석
3. 커버리지가 낮은 클래스 식별
4. 우선순위별 테스트 추가 계획 제안
5. Edge case 테스트 시나리오 제안

참고: JaCoCo 설정은 build.gradle에 있음
```

---

## 테스트 실행 명령어

```bash
# 전체 테스트
./gradlew test

# 특정 테스트만 실행
./gradlew test --tests "MemberServiceTest"
./gradlew test --tests "*IntegrationTest"

# 커버리지 리포트 생성
./gradlew test jacocoTestReport

# 캐시 삭제 후 재빌드
./gradlew clean build
```

---

## 테스트 아키텍처 요약

```
┌─────────────────────────────────────────────────────────────┐
│                    테스트 피라미드                            │
├─────────────────────────────────────────────────────────────┤
│                         /\                                  │
│                        /  \    E2E (적게)                    │
│                       /----\                                │
│                      /      \   @SpringBootTest             │
│                     /--------\  + Testcontainers            │
│                    /          \                             │
│                   /------------\ @WebMvcTest, @DataJpaTest  │
│                  /              \                           │
│                 /----------------\ @ExtendWith(Mockito)     │
│                /    단위 테스트    \ (많이, 빠르게)            │
│               /____________________\                        │
└─────────────────────────────────────────────────────────────┘
```

### 테스트 유형별 사용 시점

| 테스트 유형 | 어노테이션 | 용도 | 속도 |
|------------|-----------|------|------|
| 단위 테스트 | `@ExtendWith(MockitoExtension.class)` | 비즈니스 로직 | 빠름 (ms) |
| 슬라이스 테스트 | `@WebMvcTest`, `@DataJpaTest` | 특정 레이어 | 중간 |
| 통합 테스트 | `@SpringBootTest` + Testcontainers | 전체 흐름 | 느림 (s) |

---

## 주요 파일 위치

```
src/test/
├── java/com/example/pre_view/
│   ├── PreViewApplicationTests.java          # 컨텍스트 로드 테스트
│   ├── support/
│   │   └── IntegrationTestSupport.java       # 통합 테스트 베이스 클래스
│   └── domain/
│       ├── member/
│       │   ├── service/
│       │   │   ├── MemberServiceTest.java            # 단위 테스트
│       │   │   └── MemberServiceIntegrationTest.java # 통합 테스트
│       │   └── controller/
│       │       └── MemberControllerTest.java         # 컨트롤러 테스트
│       └── auth/
│           └── controller/
│               └── TestSecurityConfig.java           # 테스트용 Security 설정
└── resources/
    ├── application.yaml                      # 기본 테스트 설정 (사용 안 함)
    └── application-integration.yaml          # 통합 테스트 설정
```

---

## 트러블슈팅

### IDE에서 빨간줄이 뜨는 경우
```
Cmd + Shift + P → "Java: Clean Java Language Server Workspace" 선택
```

### 동시성 테스트에서 데이터가 안 보이는 경우
- `@Transactional` 제거
- `save()` → `saveAndFlush()` 변경
- `finally`에서 테스트 데이터 정리

### Testcontainers 연결 실패
- Docker Desktop이 실행 중인지 확인
- `docker ps`로 컨테이너 상태 확인
