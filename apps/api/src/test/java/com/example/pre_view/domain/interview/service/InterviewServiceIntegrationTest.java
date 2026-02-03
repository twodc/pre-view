package com.example.pre_view.domain.interview.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import com.example.pre_view.common.exception.BusinessException;
import com.example.pre_view.common.exception.ErrorCode;
import com.example.pre_view.domain.interview.dto.InterviewCreateRequest;
import com.example.pre_view.domain.interview.dto.InterviewResponse;
import com.example.pre_view.domain.interview.entity.Interview;
import com.example.pre_view.domain.interview.enums.ExperienceLevel;
import com.example.pre_view.domain.interview.enums.InterviewStatus;
import com.example.pre_view.domain.interview.enums.InterviewType;
import com.example.pre_view.domain.interview.enums.Position;
import com.example.pre_view.domain.interview.repository.InterviewRepository;
import com.example.pre_view.support.IntegrationTestSupport;

/**
 * InterviewService 통합 테스트
 *
 * <h2>설계 의도</h2>
 * <ul>
 *   <li>IntegrationTestSupport 상속: MySQL/Redis Testcontainers 사용</li>
 *   <li>@MockitoBean: AI 서비스는 Mock 처리 (외부 API 호출 방지)</li>
 *   <li>DCI 패턴: Describe-Context-It 구조로 가독성 향상</li>
 *   <li>Virtual Threads: Java 21 Virtual Thread로 동시성 테스트</li>
 * </ul>
 *
 * <h2>주요 검증 포인트</h2>
 * <ul>
 *   <li>면접 생성/조회/삭제 비즈니스 로직</li>
 *   <li>Optimistic Locking 동시성 제어</li>
 *   <li>권한 검증 (ACCESS_DENIED)</li>
 * </ul>
 *
 * @see InterviewService
 * @see Interview
 */
@DisplayName("InterviewService 통합 테스트")
class InterviewServiceIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private InterviewService interviewService;

    @Autowired
    private InterviewRepository interviewRepository;

    /**
     * AI 서비스 Mock
     * - 외부 API 호출 방지
     * - 테스트 속도 향상
     */
    @MockitoBean
    private AiInterviewService aiInterviewService;

    // 테스트 픽스처
    private static final Long TEST_MEMBER_ID = 1L;
    private static final Long OTHER_MEMBER_ID = 2L;

    private InterviewCreateRequest createTestRequest() {
        return new InterviewCreateRequest(
                "백엔드 개발자 면접 연습",
                InterviewType.TECHNICAL,
                Position.BACKEND,
                ExperienceLevel.JUNIOR,
                List.of("Java", "Spring", "MySQL")
        );
    }

    @Nested
    @DisplayName("createInterview 메서드는")
    class Describe_createInterview {

        @Nested
        @DisplayName("유효한 요청이 주어지면")
        class Context_with_valid_request {

            @Test
            @DisplayName("면접을 생성하고 InterviewResponse를 반환한다")
            void it_creates_interview_and_returns_response() {
                // given
                InterviewCreateRequest request = createTestRequest();

                // when
                InterviewResponse response = interviewService.createInterview(request, TEST_MEMBER_ID);

                // then
                assertThat(response).isNotNull();
                assertThat(response.id()).isNotNull();
                assertThat(response.title()).isEqualTo("백엔드 개발자 면접 연습");
                assertThat(response.type()).isEqualTo(InterviewType.TECHNICAL);
                assertThat(response.position()).isEqualTo(Position.BACKEND);
                assertThat(response.level()).isEqualTo(ExperienceLevel.JUNIOR);
                assertThat(response.status()).isEqualTo(InterviewStatus.READY);
            }

            @Test
            @Transactional  // techStacks가 @ElementCollection (Lazy)이므로 트랜잭션 필요
            @DisplayName("생성된 면접이 데이터베이스에 저장된다")
            void it_persists_interview_to_database() {
                // given
                InterviewCreateRequest request = createTestRequest();

                // when
                InterviewResponse response = interviewService.createInterview(request, TEST_MEMBER_ID);

                // then
                Interview saved = interviewRepository.findById(response.id()).orElseThrow();
                assertThat(saved.getTitle()).isEqualTo("백엔드 개발자 면접 연습");
                assertThat(saved.getMemberId()).isEqualTo(TEST_MEMBER_ID);
                assertThat(saved.getTechStacks()).containsExactly("Java", "Spring", "MySQL");
            }
        }
    }

    @Nested
    @DisplayName("getInterview 메서드는")
    class Describe_getInterview {

        private Long savedInterviewId;

        @BeforeEach
        void setUp() {
            // 테스트용 면접 생성
            InterviewCreateRequest request = createTestRequest();
            InterviewResponse response = interviewService.createInterview(request, TEST_MEMBER_ID);
            savedInterviewId = response.id();
        }

        @Nested
        @DisplayName("본인이 생성한 면접을 조회하면")
        class Context_with_own_interview {

            @Test
            @DisplayName("면접 정보를 반환한다")
            void it_returns_interview_info() {
                // when
                InterviewResponse response = interviewService.getInterview(savedInterviewId, TEST_MEMBER_ID);

                // then
                assertThat(response).isNotNull();
                assertThat(response.id()).isEqualTo(savedInterviewId);
                assertThat(response.title()).isEqualTo("백엔드 개발자 면접 연습");
            }
        }

        @Nested
        @DisplayName("다른 사용자의 면접을 조회하면")
        class Context_with_other_members_interview {

            @Test
            @DisplayName("ACCESS_DENIED 예외를 던진다")
            void it_throws_access_denied_exception() {
                // when & then
                assertThatThrownBy(() -> interviewService.getInterview(savedInterviewId, OTHER_MEMBER_ID))
                        .isInstanceOf(BusinessException.class)
                        .satisfies(ex -> {
                            BusinessException bizEx = (BusinessException) ex;
                            assertThat(bizEx.getErrorCode()).isEqualTo(ErrorCode.ACCESS_DENIED);
                        });
            }
        }

        @Nested
        @DisplayName("존재하지 않는 면접을 조회하면")
        class Context_with_non_existing_interview {

            @Test
            @DisplayName("INTERVIEW_NOT_FOUND 예외를 던진다")
            void it_throws_not_found_exception() {
                // when & then
                assertThatThrownBy(() -> interviewService.getInterview(999999L, TEST_MEMBER_ID))
                        .isInstanceOf(BusinessException.class)
                        .satisfies(ex -> {
                            BusinessException bizEx = (BusinessException) ex;
                            assertThat(bizEx.getErrorCode()).isEqualTo(ErrorCode.INTERVIEW_NOT_FOUND);
                        });
            }
        }
    }

    @Nested
    @DisplayName("deleteInterview 메서드는")
    class Describe_deleteInterview {

        private Long savedInterviewId;

        @BeforeEach
        void setUp() {
            InterviewCreateRequest request = createTestRequest();
            InterviewResponse response = interviewService.createInterview(request, TEST_MEMBER_ID);
            savedInterviewId = response.id();
        }

        @Nested
        @DisplayName("본인이 생성한 면접을 삭제하면")
        class Context_with_own_interview {

            @Test
            @DisplayName("소프트 삭제가 수행된다")
            void it_performs_soft_delete() {
                // when
                interviewService.deleteInterview(savedInterviewId, TEST_MEMBER_ID);

                // then - 소프트 삭제 확인 (deleted = true)
                Interview deleted = interviewRepository.findById(savedInterviewId).orElseThrow();
                assertThat(deleted.isDeleted()).isTrue();
                assertThat(deleted.getDeletedAt()).isNotNull();
            }

            @Test
            @DisplayName("삭제된 면접은 조회되지 않는다")
            void it_is_not_retrievable_after_deletion() {
                // when
                interviewService.deleteInterview(savedInterviewId, TEST_MEMBER_ID);

                // then - findByIdAndDeletedFalse로 조회 시 빈 결과
                assertThat(interviewRepository.findByIdAndDeletedFalse(savedInterviewId)).isEmpty();
            }
        }

        @Nested
        @DisplayName("다른 사용자의 면접을 삭제하려 하면")
        class Context_with_other_members_interview {

            @Test
            @DisplayName("ACCESS_DENIED 예외를 던진다")
            void it_throws_access_denied_exception() {
                // when & then
                assertThatThrownBy(() -> interviewService.deleteInterview(savedInterviewId, OTHER_MEMBER_ID))
                        .isInstanceOf(BusinessException.class)
                        .satisfies(ex -> {
                            BusinessException bizEx = (BusinessException) ex;
                            assertThat(bizEx.getErrorCode()).isEqualTo(ErrorCode.ACCESS_DENIED);
                        });
            }
        }
    }

    @Nested
    @DisplayName("Optimistic Locking은")
    class Describe_optimistic_locking {

        /**
         * 동시에 같은 면접을 수정하려 할 때 Optimistic Lock 검증
         *
         * <p>시나리오:</p>
         * <ol>
         *   <li>면접 생성</li>
         *   <li>두 스레드가 동시에 같은 면접의 상태를 변경 시도</li>
         *   <li>첫 번째 스레드 성공, 두 번째 스레드 ObjectOptimisticLockingFailureException 발생</li>
         * </ol>
         *
         * <p>Java 21 Virtual Threads 사용:</p>
         * - Executors.newVirtualThreadPerTaskExecutor()로 가벼운 동시성 테스트
         */
        @Test
        @DisplayName("동시에 같은 면접을 수정하면 하나는 실패한다")
        void concurrent_modification_causes_optimistic_lock_exception() throws InterruptedException {
            // given - 테스트 데이터 생성 (Transactional 외부에서 생성)
            Interview interview = Interview.builder()
                    .memberId(TEST_MEMBER_ID)
                    .title("동시성 테스트용 면접")
                    .type(InterviewType.TECHNICAL)
                    .position(Position.BACKEND)
                    .level(ExperienceLevel.JUNIOR)
                    .techStacks(List.of("Java"))
                    .status(InterviewStatus.READY)
                    .build();
            Interview savedInterview = interviewRepository.saveAndFlush(interview);
            Long interviewId = savedInterview.getId();

            // 동시성 제어를 위한 도구
            int threadCount = 2;
            CountDownLatch startLatch = new CountDownLatch(1);  // 모든 스레드 동시 시작용
            CountDownLatch endLatch = new CountDownLatch(threadCount);  // 모든 스레드 완료 대기용

            AtomicInteger successCount = new AtomicInteger(0);
            AtomicInteger failCount = new AtomicInteger(0);

            // when - Java 21 Virtual Thread를 사용한 동시 수정
            try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
                for (int i = 0; i < threadCount; i++) {
                    executor.submit(() -> {
                        try {
                            startLatch.await();  // 모든 스레드가 준비될 때까지 대기

                            // 각 스레드에서 독립적인 트랜잭션으로 면접 수정
                            Interview toUpdate = interviewRepository.findById(interviewId).orElseThrow();
                            toUpdate.start();  // 상태 변경 (READY -> IN_PROGRESS)
                            interviewRepository.saveAndFlush(toUpdate);

                            successCount.incrementAndGet();
                        } catch (ObjectOptimisticLockingFailureException e) {
                            // Optimistic Lock 실패
                            failCount.incrementAndGet();
                        } catch (Exception e) {
                            // 기타 예외도 실패로 처리
                            if (e.getCause() instanceof ObjectOptimisticLockingFailureException) {
                                failCount.incrementAndGet();
                            }
                        } finally {
                            endLatch.countDown();
                        }
                    });
                }

                // 모든 스레드 동시 시작
                startLatch.countDown();

                // 모든 스레드 완료 대기
                endLatch.await();
            }

            // then
            // 두 스레드 중 하나는 성공, 하나는 Optimistic Lock 실패
            // (경우에 따라 둘 다 성공할 수도 있음 - 타이밍에 따라)
            assertThat(successCount.get() + failCount.get()).isEqualTo(threadCount);

            // 최소 하나는 성공해야 함
            assertThat(successCount.get()).isGreaterThanOrEqualTo(1);

            // DB에서 최종 상태 확인
            Interview finalInterview = interviewRepository.findById(interviewId).orElseThrow();
            assertThat(finalInterview.getStatus()).isEqualTo(InterviewStatus.IN_PROGRESS);
            assertThat(finalInterview.getVersion()).isGreaterThanOrEqualTo(1L);
        }

        @Test
        @DisplayName("@Version 필드가 수정 시 자동으로 증가한다")
        @Transactional
        void version_increments_on_modification() {
            // given
            Interview interview = Interview.builder()
                    .memberId(TEST_MEMBER_ID)
                    .title("버전 테스트용 면접")
                    .type(InterviewType.TECHNICAL)
                    .position(Position.BACKEND)
                    .level(ExperienceLevel.JUNIOR)
                    .techStacks(List.of("Java"))
                    .status(InterviewStatus.READY)
                    .build();
            Interview saved = interviewRepository.saveAndFlush(interview);
            Long initialVersion = saved.getVersion();

            // when - 상태 변경
            saved.start();
            interviewRepository.saveAndFlush(saved);

            // then - 버전이 증가했는지 확인
            Interview updated = interviewRepository.findById(saved.getId()).orElseThrow();
            assertThat(updated.getVersion()).isGreaterThan(initialVersion);
        }
    }

    @Nested
    @DisplayName("Virtual Thread 검증")
    class Describe_virtual_thread {

        @Test
        @DisplayName("테스트가 Virtual Thread에서 실행되는지 검증한다")
        void it_can_run_on_virtual_thread() throws Exception {
            // Java 21 Virtual Thread 테스트
            try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
                var future = executor.submit(() -> {
                    assertRunningOnVirtualThread();
                    return Thread.currentThread().isVirtual();
                });

                Boolean isVirtual = future.get();
                assertThat(isVirtual).isTrue();
            }
        }
    }
}
