package com.example.pre_view.domain.question.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.example.pre_view.domain.interview.entity.Interview;
import com.example.pre_view.domain.interview.enums.ExperienceLevel;
import com.example.pre_view.domain.interview.enums.InterviewPhase;
import com.example.pre_view.domain.interview.enums.InterviewStatus;
import com.example.pre_view.domain.interview.enums.InterviewType;
import com.example.pre_view.domain.interview.enums.Position;
import com.example.pre_view.domain.question.entity.Question;
import com.example.pre_view.support.RepositoryTestSupport;

/**
 * QuestionRepository 슬라이스 테스트
 *
 * <h2>설계 의도</h2>
 * <ul>
 *   <li>RepositoryTestSupport 상속: MySQL Testcontainers 공유, JPA Auditing 활성화</li>
 *   <li>DCI 패턴: Describe-Context-It 구조로 가독성 향상</li>
 * </ul>
 *
 * <h2>주요 검증 포인트</h2>
 * <ul>
 *   <li>커스텀 JPQL 쿼리 메서드 동작 검증</li>
 *   <li>Fetch Join 쿼리 검증</li>
 *   <li>정렬 및 필터링 검증</li>
 *   <li>꼬리질문(Follow-up) 관계 검증</li>
 * </ul>
 *
 * @see QuestionRepository
 * @see RepositoryTestSupport
 */
@DisplayName("QuestionRepository 슬라이스 테스트")
class QuestionRepositoryTest extends RepositoryTestSupport {

    @Autowired
    private QuestionRepository questionRepository;

    private Interview savedInterview;
    private Question technicalQuestion1;
    private Question technicalQuestion2;
    private Question personalityQuestion;

    @BeforeEach
    void setUp() {
        // 면접 생성
        savedInterview = Interview.builder()
                .memberId(1L)
                .title("백엔드 기술 면접")
                .type(InterviewType.FULL)
                .position(Position.BACKEND)
                .level(ExperienceLevel.JUNIOR)
                .status(InterviewStatus.IN_PROGRESS)
                .build();
        persistAndFlush(savedInterview);

        // 기술 질문 생성
        technicalQuestion1 = Question.builder()
                .interview(savedInterview)
                .content("Java의 GC에 대해 설명해주세요.")
                .phase(InterviewPhase.TECHNICAL)
                .sequence(1)
                .isFollowUp(false)
                .build();
        entityManager.persist(technicalQuestion1);

        technicalQuestion2 = Question.builder()
                .interview(savedInterview)
                .content("Spring의 IoC/DI에 대해 설명해주세요.")
                .phase(InterviewPhase.TECHNICAL)
                .sequence(2)
                .isFollowUp(false)
                .build();
        entityManager.persist(technicalQuestion2);

        // 인성 질문 생성
        personalityQuestion = Question.builder()
                .interview(savedInterview)
                .content("팀 프로젝트에서 갈등을 해결한 경험이 있나요?")
                .phase(InterviewPhase.PERSONALITY)
                .sequence(3)
                .isFollowUp(false)
                .build();
        entityManager.persist(personalityQuestion);

        flushAndClear();
    }

    @Nested
    @DisplayName("findByInterviewIdOrderBySequence 메서드는")
    class Describe_findByInterviewIdOrderBySequence {

        @Nested
        @DisplayName("면접 ID가 주어지면")
        class Context_with_interview_id {

            @Test
            @DisplayName("해당 면접의 모든 질문을 sequence 오름차순으로 반환한다")
            void it_returns_questions_ordered_by_sequence() {
                // when
                List<Question> questions = questionRepository.findByInterviewIdOrderBySequence(
                        savedInterview.getId());

                // then
                assertThat(questions)
                        .hasSize(3)
                        .extracting(Question::getSequence)
                        .containsExactly(1, 2, 3);
            }

            @Test
            @DisplayName("parentQuestion을 Fetch Join으로 함께 로드한다")
            void it_fetches_parent_question_eagerly() {
                // given - 꼬리질문 추가
                var parentQuestion = questionRepository.findById(technicalQuestion1.getId()).orElseThrow();
                Question followUpQuestion = Question.builder()
                        .interview(savedInterview)
                        .content("G1 GC의 동작 원리를 설명해주세요.")
                        .phase(InterviewPhase.TECHNICAL)
                        .sequence(4)
                        .parentQuestion(parentQuestion)
                        .isFollowUp(true)
                        .build();
                entityManager.persist(followUpQuestion);
                flushAndClear();

                // when
                List<Question> questions = questionRepository.findByInterviewIdOrderBySequence(
                        savedInterview.getId());

                // then - N+1 문제 없이 parentQuestion 접근 가능
                Question foundFollowUp = questions.stream()
                        .filter(Question::isFollowUp)
                        .findFirst()
                        .orElseThrow();

                assertThat(foundFollowUp.getParentQuestion()).isNotNull();
                assertThat(foundFollowUp.getParentQuestion().getContent())
                        .isEqualTo("Java의 GC에 대해 설명해주세요.");
            }
        }

        @Nested
        @DisplayName("존재하지 않는 면접 ID가 주어지면")
        class Context_with_non_existing_interview_id {

            @Test
            @DisplayName("빈 리스트를 반환한다")
            void it_returns_empty_list() {
                // when
                List<Question> questions = questionRepository.findByInterviewIdOrderBySequence(999L);

                // then
                assertThat(questions).isEmpty();
            }
        }
    }

    @Nested
    @DisplayName("findByInterviewIdAndPhaseOrderBySequence 메서드는")
    class Describe_findByInterviewIdAndPhaseOrderBySequence {

        @Nested
        @DisplayName("면접 ID와 단계가 주어지면")
        class Context_with_interview_id_and_phase {

            @Test
            @DisplayName("해당 단계의 질문만 반환한다")
            void it_returns_questions_for_specific_phase() {
                // when
                List<Question> technicalQuestions = questionRepository.findByInterviewIdAndPhaseOrderBySequence(
                        savedInterview.getId(), InterviewPhase.TECHNICAL);

                // then
                assertThat(technicalQuestions)
                        .hasSize(2)
                        .allSatisfy(q -> assertThat(q.getPhase()).isEqualTo(InterviewPhase.TECHNICAL));
            }

            @Test
            @DisplayName("sequence 오름차순으로 정렬된다")
            void it_returns_questions_ordered_by_sequence() {
                // when
                List<Question> technicalQuestions = questionRepository.findByInterviewIdAndPhaseOrderBySequence(
                        savedInterview.getId(), InterviewPhase.TECHNICAL);

                // then
                assertThat(technicalQuestions)
                        .extracting(Question::getSequence)
                        .containsExactly(1, 2);
            }
        }

        @Nested
        @DisplayName("해당 단계의 질문이 없으면")
        class Context_with_no_questions_for_phase {

            @Test
            @DisplayName("빈 리스트를 반환한다")
            void it_returns_empty_list() {
                // when - OPENING 단계 질문은 없음
                List<Question> openingQuestions = questionRepository.findByInterviewIdAndPhaseOrderBySequence(
                        savedInterview.getId(), InterviewPhase.OPENING);

                // then
                assertThat(openingQuestions).isEmpty();
            }
        }
    }

    @Nested
    @DisplayName("findByParentQuestionId 메서드는")
    class Describe_findByParentQuestionId {

        @Nested
        @DisplayName("부모 질문 ID가 주어지면")
        class Context_with_parent_question_id {

            @Test
            @DisplayName("해당 부모의 꼬리질문들을 반환한다")
            void it_returns_follow_up_questions() {
                // given - 꼬리질문 추가
                var parentQuestion = questionRepository.findById(technicalQuestion1.getId()).orElseThrow();

                Question followUp1 = Question.builder()
                        .interview(savedInterview)
                        .content("G1 GC에 대해 더 설명해주세요.")
                        .phase(InterviewPhase.TECHNICAL)
                        .sequence(4)
                        .parentQuestion(parentQuestion)
                        .isFollowUp(true)
                        .build();
                Question followUp2 = Question.builder()
                        .interview(savedInterview)
                        .content("ZGC와 비교해서 설명해주세요.")
                        .phase(InterviewPhase.TECHNICAL)
                        .sequence(5)
                        .parentQuestion(parentQuestion)
                        .isFollowUp(true)
                        .build();
                entityManager.persist(followUp1);
                entityManager.persist(followUp2);
                flushAndClear();

                // when
                List<Question> followUps = questionRepository.findByParentQuestionId(
                        technicalQuestion1.getId());

                // then
                assertThat(followUps)
                        .hasSize(2)
                        .allSatisfy(q -> {
                            assertThat(q.isFollowUp()).isTrue();
                            assertThat(q.getParentQuestion().getId()).isEqualTo(technicalQuestion1.getId());
                        });
            }
        }

        @Nested
        @DisplayName("꼬리질문이 없는 부모 질문 ID가 주어지면")
        class Context_with_no_follow_ups {

            @Test
            @DisplayName("빈 리스트를 반환한다")
            void it_returns_empty_list() {
                // when
                List<Question> followUps = questionRepository.findByParentQuestionId(
                        technicalQuestion2.getId());

                // then
                assertThat(followUps).isEmpty();
            }
        }
    }

    @Nested
    @DisplayName("findByIdWithInterview 메서드는")
    class Describe_findByIdWithInterview {

        @Nested
        @DisplayName("질문 ID가 주어지면")
        class Context_with_question_id {

            @Test
            @DisplayName("Interview를 Fetch Join으로 함께 로드한다")
            void it_fetches_interview_eagerly() {
                // when
                var result = questionRepository.findByIdWithInterview(technicalQuestion1.getId());

                // then
                assertThat(result).isPresent();
                assertThat(result.get().getInterview()).isNotNull();
                assertThat(result.get().getInterview().getTitle()).isEqualTo("백엔드 기술 면접");
            }
        }

        @Nested
        @DisplayName("존재하지 않는 ID가 주어지면")
        class Context_with_non_existing_id {

            @Test
            @DisplayName("빈 Optional을 반환한다")
            void it_returns_empty_optional() {
                // when
                var result = questionRepository.findByIdWithInterview(999L);

                // then
                assertThat(result).isEmpty();
            }
        }
    }

    @Nested
    @DisplayName("countByInterviewId 메서드는")
    class Describe_countByInterviewId {

        @Test
        @DisplayName("면접의 총 질문 수를 반환한다")
        void it_returns_total_question_count() {
            // when
            int count = questionRepository.countByInterviewId(savedInterview.getId());

            // then
            assertThat(count).isEqualTo(3);
        }

        @Test
        @DisplayName("존재하지 않는 면접 ID면 0을 반환한다")
        void it_returns_zero_for_non_existing_interview() {
            // when
            int count = questionRepository.countByInterviewId(999L);

            // then
            assertThat(count).isZero();
        }
    }

    @Nested
    @DisplayName("Question 엔티티의 markAsAnswered 메서드는")
    class Describe_markAsAnswered {

        @Test
        @DisplayName("isAnswered를 true로 변경한다")
        void it_sets_isAnswered_to_true() {
            // given
            var question = questionRepository.findById(technicalQuestion1.getId()).orElseThrow();
            assertThat(question.isAnswered()).isFalse();

            // when
            question.markAsAnswered();
            flushAndClear();

            // then
            var updated = questionRepository.findById(technicalQuestion1.getId()).orElseThrow();
            assertThat(updated.isAnswered()).isTrue();
        }
    }
}
