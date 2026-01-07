package com.example.pre_view.domain.interview.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import com.example.pre_view.domain.interview.entity.Interview;
import com.example.pre_view.domain.interview.enums.ExperienceLevel;
import com.example.pre_view.domain.interview.enums.InterviewStatus;
import com.example.pre_view.domain.interview.enums.InterviewType;
import com.example.pre_view.domain.interview.enums.Position;
import com.example.pre_view.support.RepositoryTestSupport;

/**
 * InterviewRepository 슬라이스 테스트
 *
 * <h2>설계 의도</h2>
 * <ul>
 *   <li>RepositoryTestSupport 상속: MySQL Testcontainers 공유, JPA Auditing 활성화</li>
 *   <li>DCI 패턴: Describe-Context-It 구조로 가독성 향상</li>
 * </ul>
 *
 * <h2>주요 검증 포인트</h2>
 * <ul>
 *   <li>커스텀 쿼리 메서드 동작 검증</li>
 *   <li>Soft Delete 필터링 검증</li>
 *   <li>페이징 및 정렬 검증</li>
 *   <li>회원 ID 기반 조회 검증</li>
 * </ul>
 *
 * @see InterviewRepository
 * @see RepositoryTestSupport
 */
@DisplayName("InterviewRepository 슬라이스 테스트")
class InterviewRepositoryTest extends RepositoryTestSupport {

    @Autowired
    private InterviewRepository interviewRepository;

    private static final Long TEST_MEMBER_ID = 1L;
    private static final Long OTHER_MEMBER_ID = 2L;

    private Interview savedInterview;

    @BeforeEach
    void setUp() {
        savedInterview = Interview.builder()
                .memberId(TEST_MEMBER_ID)
                .title("백엔드 기술 면접")
                .type(InterviewType.TECHNICAL)
                .position(Position.BACKEND)
                .level(ExperienceLevel.JUNIOR)
                .techStacks(List.of("Java", "Spring"))
                .status(InterviewStatus.READY)
                .build();

        persistAndFlush(savedInterview);
    }

    @Nested
    @DisplayName("findByIdAndDeletedFalse 메서드는")
    class Describe_findByIdAndDeletedFalse {

        @Nested
        @DisplayName("삭제되지 않은 면접 ID가 주어지면")
        class Context_with_non_deleted_interview {

            @Test
            @DisplayName("해당 면접을 Optional로 감싸서 반환한다")
            void it_returns_interview_wrapped_in_optional() {
                // when
                var result = interviewRepository.findByIdAndDeletedFalse(savedInterview.getId());

                // then
                assertThat(result)
                        .isPresent()
                        .get()
                        .satisfies(interview -> {
                            assertThat(interview.getTitle()).isEqualTo("백엔드 기술 면접");
                            assertThat(interview.getType()).isEqualTo(InterviewType.TECHNICAL);
                            assertThat(interview.getPosition()).isEqualTo(Position.BACKEND);
                            assertThat(interview.getLevel()).isEqualTo(ExperienceLevel.JUNIOR);
                        });
            }
        }

        @Nested
        @DisplayName("삭제된 면접 ID가 주어지면")
        class Context_with_deleted_interview {

            @Test
            @DisplayName("빈 Optional을 반환한다")
            void it_returns_empty_optional() {
                // given - 면접 삭제
                var interview = interviewRepository.findById(savedInterview.getId()).orElseThrow();
                interview.delete();
                flushAndClear();

                // when
                var result = interviewRepository.findByIdAndDeletedFalse(savedInterview.getId());

                // then
                assertThat(result).isEmpty();
            }
        }

        @Nested
        @DisplayName("존재하지 않는 ID가 주어지면")
        class Context_with_non_existing_id {

            @Test
            @DisplayName("빈 Optional을 반환한다")
            void it_returns_empty_optional() {
                // when
                var result = interviewRepository.findByIdAndDeletedFalse(999L);

                // then
                assertThat(result).isEmpty();
            }
        }
    }

    @Nested
    @DisplayName("findByIdAndMemberIdAndDeletedFalse 메서드는")
    class Describe_findByIdAndMemberIdAndDeletedFalse {

        @Nested
        @DisplayName("소유자 회원 ID와 면접 ID가 일치하면")
        class Context_with_matching_member_id {

            @Test
            @DisplayName("해당 면접을 반환한다")
            void it_returns_interview() {
                // when
                var result = interviewRepository.findByIdAndMemberIdAndDeletedFalse(
                        savedInterview.getId(), TEST_MEMBER_ID);

                // then
                assertThat(result).isPresent();
                assertThat(result.get().getMemberId()).isEqualTo(TEST_MEMBER_ID);
            }
        }

        @Nested
        @DisplayName("다른 회원의 ID로 조회하면")
        class Context_with_different_member_id {

            @Test
            @DisplayName("빈 Optional을 반환한다")
            void it_returns_empty_optional() {
                // when
                var result = interviewRepository.findByIdAndMemberIdAndDeletedFalse(
                        savedInterview.getId(), OTHER_MEMBER_ID);

                // then
                assertThat(result).isEmpty();
            }
        }
    }

    @Nested
    @DisplayName("findByMemberIdAndDeletedFalseOrderByCreatedAtDesc 메서드는")
    class Describe_findByMemberIdAndDeletedFalse {

        @BeforeEach
        void setUpMultipleInterviews() {
            // 여러 면접 추가
            for (int i = 1; i <= 3; i++) {
                Interview interview = Interview.builder()
                        .memberId(TEST_MEMBER_ID)
                        .title("면접 " + i)
                        .type(InterviewType.TECHNICAL)
                        .position(Position.BACKEND)
                        .level(ExperienceLevel.JUNIOR)
                        .status(InterviewStatus.READY)
                        .build();
                entityManager.persist(interview);
            }

            // 다른 회원의 면접 추가
            Interview otherMemberInterview = Interview.builder()
                    .memberId(OTHER_MEMBER_ID)
                    .title("다른 회원 면접")
                    .type(InterviewType.FULL)
                    .position(Position.FRONTEND)
                    .level(ExperienceLevel.MID)
                    .status(InterviewStatus.READY)
                    .build();
            entityManager.persist(otherMemberInterview);

            flushAndClear();
        }

        @Nested
        @DisplayName("회원 ID로 조회하면")
        class Context_with_member_id {

            @Test
            @DisplayName("해당 회원의 면접만 페이징하여 반환한다")
            void it_returns_only_member_interviews() {
                // when
                Page<Interview> result = interviewRepository.findByMemberIdAndDeletedFalseOrderByCreatedAtDesc(
                        TEST_MEMBER_ID, PageRequest.of(0, 10));

                // then
                assertThat(result.getContent())
                        .hasSize(4)  // setUp의 1개 + 추가 3개
                        .allSatisfy(interview ->
                                assertThat(interview.getMemberId()).isEqualTo(TEST_MEMBER_ID)
                        );
            }

            @Test
            @DisplayName("생성일시 내림차순으로 정렬된다")
            void it_returns_interviews_ordered_by_createdAt_desc() {
                // when
                Page<Interview> result = interviewRepository.findByMemberIdAndDeletedFalseOrderByCreatedAtDesc(
                        TEST_MEMBER_ID, PageRequest.of(0, 10));

                // then
                var interviews = result.getContent();
                for (int i = 0; i < interviews.size() - 1; i++) {
                    assertThat(interviews.get(i).getCreatedAt())
                            .isAfterOrEqualTo(interviews.get(i + 1).getCreatedAt());
                }
            }
        }

        @Nested
        @DisplayName("페이지 크기를 지정하면")
        class Context_with_page_size {

            @Test
            @DisplayName("지정된 크기만큼만 반환한다")
            void it_returns_specified_page_size() {
                // when
                Page<Interview> result = interviewRepository.findByMemberIdAndDeletedFalseOrderByCreatedAtDesc(
                        TEST_MEMBER_ID, PageRequest.of(0, 2));

                // then
                assertThat(result.getContent()).hasSize(2);
                assertThat(result.getTotalElements()).isEqualTo(4);
                assertThat(result.getTotalPages()).isEqualTo(2);
            }
        }
    }

    @Nested
    @DisplayName("findAllByDeletedFalseOrderByCreatedAtDesc 메서드는")
    class Describe_findAllByDeletedFalse {

        @Test
        @DisplayName("삭제되지 않은 모든 면접을 페이징하여 반환한다")
        void it_returns_all_non_deleted_interviews() {
            // when
            Page<Interview> result = interviewRepository.findAllByDeletedFalseOrderByCreatedAtDesc(
                    PageRequest.of(0, 10));

            // then
            assertThat(result.getContent())
                    .isNotEmpty()
                    .allSatisfy(interview -> assertThat(interview.isDeleted()).isFalse());
        }

        @Test
        @DisplayName("삭제된 면접은 조회되지 않는다")
        void it_excludes_deleted_interviews() {
            // given - 면접 삭제
            var interview = interviewRepository.findById(savedInterview.getId()).orElseThrow();
            interview.delete();
            flushAndClear();

            // when
            Page<Interview> result = interviewRepository.findAllByDeletedFalseOrderByCreatedAtDesc(
                    PageRequest.of(0, 10));

            // then
            assertThat(result.getContent())
                    .noneMatch(i -> i.getId().equals(savedInterview.getId()));
        }
    }

    @Nested
    @DisplayName("Interview 엔티티의 @Version은")
    class Describe_optimistic_lock {

        @Test
        @DisplayName("저장 시 초기 버전이 0이다")
        void it_starts_with_version_zero() {
            // when
            var interview = interviewRepository.findById(savedInterview.getId()).orElseThrow();

            // then
            assertThat(interview.getVersion()).isEqualTo(0L);
        }

        @Test
        @DisplayName("엔티티 수정 시 버전이 증가한다")
        void it_increments_version_on_update() {
            // given
            var interview = interviewRepository.findById(savedInterview.getId()).orElseThrow();
            Long originalVersion = interview.getVersion();

            // when
            interview.start();  // 상태 변경
            flushAndClear();

            // then
            var updated = interviewRepository.findById(savedInterview.getId()).orElseThrow();
            assertThat(updated.getVersion()).isEqualTo(originalVersion + 1);
        }
    }

    @Nested
    @DisplayName("Interview 상태 변경 메서드는")
    class Describe_status_transitions {

        @Test
        @DisplayName("start() 호출 시 IN_PROGRESS 상태로 변경된다")
        void start_changes_status_to_in_progress() {
            // given
            var interview = interviewRepository.findById(savedInterview.getId()).orElseThrow();
            assertThat(interview.getStatus()).isEqualTo(InterviewStatus.READY);

            // when
            interview.start();
            flushAndClear();

            // then
            var updated = interviewRepository.findById(savedInterview.getId()).orElseThrow();
            assertThat(updated.getStatus()).isEqualTo(InterviewStatus.IN_PROGRESS);
            assertThat(updated.getCurrentPhase()).isNotNull();
        }

        @Test
        @DisplayName("complete() 호출 시 DONE 상태로 변경된다")
        void complete_changes_status_to_done() {
            // given
            var interview = interviewRepository.findById(savedInterview.getId()).orElseThrow();
            interview.start();  // 먼저 시작해야 완료 가능

            // when
            interview.complete();
            flushAndClear();

            // then
            var updated = interviewRepository.findById(savedInterview.getId()).orElseThrow();
            assertThat(updated.getStatus()).isEqualTo(InterviewStatus.DONE);
        }
    }

    @Nested
    @DisplayName("Edge Case 테스트")
    class Describe_edge_cases {

        @Test
        @DisplayName("techStacks가 빈 리스트인 면접도 정상 저장된다")
        void it_saves_interview_with_empty_tech_stacks() {
            // given
            Interview interview = Interview.builder()
                    .memberId(TEST_MEMBER_ID)
                    .title("인성 면접")
                    .type(InterviewType.PERSONALITY)
                    .position(Position.BACKEND)
                    .level(ExperienceLevel.JUNIOR)
                    .techStacks(List.of())  // 빈 리스트
                    .status(InterviewStatus.READY)
                    .build();

            // when
            persistAndFlush(interview);

            // then
            var saved = interviewRepository.findById(interview.getId()).orElseThrow();
            assertThat(saved.getTechStacks()).isEmpty();
        }

        @Test
        @DisplayName("같은 회원이 여러 면접을 가질 수 있다")
        void it_allows_multiple_interviews_for_same_member() {
            // given - 추가 면접 생성
            for (int i = 0; i < 5; i++) {
                Interview interview = Interview.builder()
                        .memberId(TEST_MEMBER_ID)
                        .title("면접 " + i)
                        .type(InterviewType.TECHNICAL)
                        .position(Position.BACKEND)
                        .level(ExperienceLevel.JUNIOR)
                        .status(InterviewStatus.READY)
                        .build();
                entityManager.persist(interview);
            }
            flushAndClear();

            // when
            var result = interviewRepository.findByMemberIdAndDeletedFalseOrderByCreatedAtDesc(
                    TEST_MEMBER_ID, PageRequest.of(0, 10));

            // then - setUp의 1개 + 추가 5개
            assertThat(result.getTotalElements()).isEqualTo(6);
        }

        @Test
        @DisplayName("회원이 면접을 가지지 않으면 빈 페이지를 반환한다")
        void it_returns_empty_page_for_member_without_interviews() {
            // when
            var result = interviewRepository.findByMemberIdAndDeletedFalseOrderByCreatedAtDesc(
                    999L, PageRequest.of(0, 10));

            // then
            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isZero();
        }

        @Test
        @DisplayName("삭제 후 복구 시나리오 - deleted 플래그만 변경")
        void it_can_track_deleted_and_restored() {
            // given - 면접 삭제
            var interview = interviewRepository.findById(savedInterview.getId()).orElseThrow();
            interview.delete();
            flushAndClear();

            // then - 삭제 상태 확인
            assertThat(interviewRepository.findByIdAndDeletedFalse(savedInterview.getId())).isEmpty();

            // 실제 데이터는 여전히 존재 (soft delete)
            var deletedInterview = interviewRepository.findById(savedInterview.getId()).orElseThrow();
            assertThat(deletedInterview.isDeleted()).isTrue();
            assertThat(deletedInterview.getDeletedAt()).isNotNull();
        }
    }
}
