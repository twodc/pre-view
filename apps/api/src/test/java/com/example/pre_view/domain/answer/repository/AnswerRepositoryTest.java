package com.example.pre_view.domain.answer.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.example.pre_view.domain.answer.entity.Answer;
import com.example.pre_view.domain.interview.entity.Interview;
import com.example.pre_view.domain.interview.enums.ExperienceLevel;
import com.example.pre_view.domain.interview.enums.InterviewPhase;
import com.example.pre_view.domain.interview.enums.InterviewStatus;
import com.example.pre_view.domain.interview.enums.InterviewType;
import com.example.pre_view.domain.interview.enums.Position;
import com.example.pre_view.domain.question.entity.Question;
import com.example.pre_view.support.RepositoryTestSupport;

/**
 * AnswerRepository 슬라이스 테스트
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
 * </ul>
 *
 * @see AnswerRepository
 * @see RepositoryTestSupport
 */
@DisplayName("AnswerRepository 슬라이스 테스트")
class AnswerRepositoryTest extends RepositoryTestSupport {

    @Autowired
    private AnswerRepository answerRepository;

    private Interview savedInterview;
    private Question technicalQuestion;
    private Question personalityQuestion;
    private Answer technicalAnswer;
    private Answer personalityAnswer;

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
        technicalQuestion = Question.builder()
                .interview(savedInterview)
                .content("Java의 GC에 대해 설명해주세요.")
                .phase(InterviewPhase.TECHNICAL)
                .sequence(1)
                .isFollowUp(false)
                .build();
        entityManager.persist(technicalQuestion);

        // 인성 질문 생성
        personalityQuestion = Question.builder()
                .interview(savedInterview)
                .content("팀워크 경험을 말씀해주세요.")
                .phase(InterviewPhase.PERSONALITY)
                .sequence(2)
                .isFollowUp(false)
                .build();
        entityManager.persist(personalityQuestion);

        // 기술 질문에 대한 답변
        technicalAnswer = Answer.builder()
                .question(technicalQuestion)
                .content("GC는 더 이상 사용되지 않는 객체를 자동으로 회수합니다.")
                .feedback("좋은 답변입니다. Minor GC와 Major GC에 대해서도 설명하면 더 좋겠습니다.")
                .score(80)
                .build();
        entityManager.persist(technicalAnswer);

        // 인성 질문에 대한 답변
        personalityAnswer = Answer.builder()
                .question(personalityQuestion)
                .content("프로젝트에서 의견 충돌이 있었을 때 적극적으로 중재했습니다.")
                .feedback("구체적인 사례를 들어 설명하셨네요.")
                .score(85)
                .build();
        entityManager.persist(personalityAnswer);

        flushAndClear();
    }

    @Nested
    @DisplayName("findByInterviewIdWithQuestion 메서드는")
    class Describe_findByInterviewIdWithQuestion {

        @Nested
        @DisplayName("면접 ID가 주어지면")
        class Context_with_interview_id {

            @Test
            @DisplayName("해당 면접의 모든 답변을 Question과 함께 반환한다")
            void it_returns_answers_with_questions() {
                // when
                List<Answer> answers = answerRepository.findByInterviewIdWithQuestion(
                        savedInterview.getId());

                // then
                assertThat(answers)
                        .hasSize(2)
                        .allSatisfy(answer -> {
                            assertThat(answer.getQuestion()).isNotNull();
                            assertThat(answer.getQuestion().getInterview().getId())
                                    .isEqualTo(savedInterview.getId());
                        });
            }

            @Test
            @DisplayName("question의 sequence 오름차순으로 정렬된다")
            void it_returns_answers_ordered_by_question_sequence() {
                // when
                List<Answer> answers = answerRepository.findByInterviewIdWithQuestion(
                        savedInterview.getId());

                // then
                assertThat(answers)
                        .extracting(a -> a.getQuestion().getSequence())
                        .containsExactly(1, 2);
            }

            @Test
            @DisplayName("Question을 Fetch Join으로 함께 로드한다 (N+1 방지)")
            void it_fetches_question_eagerly() {
                // when
                List<Answer> answers = answerRepository.findByInterviewIdWithQuestion(
                        savedInterview.getId());

                // then - 추가 쿼리 없이 Question 접근 가능
                assertThat(answers.get(0).getQuestion().getContent())
                        .isEqualTo("Java의 GC에 대해 설명해주세요.");
                assertThat(answers.get(1).getQuestion().getContent())
                        .isEqualTo("팀워크 경험을 말씀해주세요.");
            }
        }

        @Nested
        @DisplayName("존재하지 않는 면접 ID가 주어지면")
        class Context_with_non_existing_interview_id {

            @Test
            @DisplayName("빈 리스트를 반환한다")
            void it_returns_empty_list() {
                // when
                List<Answer> answers = answerRepository.findByInterviewIdWithQuestion(999L);

                // then
                assertThat(answers).isEmpty();
            }
        }
    }

    @Nested
    @DisplayName("findByInterviewIdAndPhase 메서드는")
    class Describe_findByInterviewIdAndPhase {

        @Nested
        @DisplayName("면접 ID와 단계가 주어지면")
        class Context_with_interview_id_and_phase {

            @Test
            @DisplayName("해당 단계의 답변만 반환한다")
            void it_returns_answers_for_specific_phase() {
                // when
                List<Answer> technicalAnswers = answerRepository.findByInterviewIdAndPhase(
                        savedInterview.getId(), InterviewPhase.TECHNICAL);

                // then
                assertThat(technicalAnswers)
                        .hasSize(1)
                        .allSatisfy(answer ->
                                assertThat(answer.getQuestion().getPhase())
                                        .isEqualTo(InterviewPhase.TECHNICAL)
                        );
            }

            @Test
            @DisplayName("Question을 Fetch Join으로 함께 로드한다")
            void it_fetches_question_eagerly() {
                // when
                List<Answer> answers = answerRepository.findByInterviewIdAndPhase(
                        savedInterview.getId(), InterviewPhase.PERSONALITY);

                // then
                assertThat(answers).hasSize(1);
                assertThat(answers.get(0).getQuestion().getContent())
                        .isEqualTo("팀워크 경험을 말씀해주세요.");
            }
        }

        @Nested
        @DisplayName("해당 단계의 답변이 없으면")
        class Context_with_no_answers_for_phase {

            @Test
            @DisplayName("빈 리스트를 반환한다")
            void it_returns_empty_list() {
                // when - OPENING 단계 답변은 없음
                List<Answer> openingAnswers = answerRepository.findByInterviewIdAndPhase(
                        savedInterview.getId(), InterviewPhase.OPENING);

                // then
                assertThat(openingAnswers).isEmpty();
            }
        }
    }

    @Nested
    @DisplayName("Answer 엔티티는")
    class Describe_answer_entity {

        @Test
        @DisplayName("score와 feedback을 함께 저장한다")
        void it_saves_score_and_feedback() {
            // when
            var answer = answerRepository.findById(technicalAnswer.getId()).orElseThrow();

            // then
            assertThat(answer.getScore()).isEqualTo(80);
            assertThat(answer.getFeedback()).contains("Minor GC");
        }

        @Test
        @DisplayName("JPA Auditing으로 createdAt이 자동 설정된다")
        void it_sets_createdAt_automatically() {
            // when
            var answer = answerRepository.findById(technicalAnswer.getId()).orElseThrow();

            // then
            assertThat(answer.getCreatedAt()).isNotNull();
        }
    }

    @Nested
    @DisplayName("여러 답변이 있는 경우")
    class Describe_multiple_answers {

        @BeforeEach
        void setUpMultipleAnswers() {
            // 추가 질문과 답변 생성
            // 정렬은 question.sequence 기준이므로 createdAt 순서 보장 불필요
            Question additionalQuestion = Question.builder()
                    .interview(savedInterview)
                    .content("Spring Boot의 장점은?")
                    .phase(InterviewPhase.TECHNICAL)
                    .sequence(3)
                    .isFollowUp(false)
                    .build();
            entityManager.persist(additionalQuestion);

            Answer additionalAnswer = Answer.builder()
                    .question(additionalQuestion)
                    .content("자동 설정과 내장 서버를 제공합니다.")
                    .feedback("정확한 답변입니다.")
                    .score(90)
                    .build();
            entityManager.persist(additionalAnswer);

            flushAndClear();
        }

        @Test
        @DisplayName("sequence 순서대로 정렬된다")
        void it_returns_answers_in_sequence_order() {
            // when
            List<Answer> answers = answerRepository.findByInterviewIdWithQuestion(
                    savedInterview.getId());

            // then
            assertThat(answers)
                    .hasSize(3)
                    .extracting(a -> a.getQuestion().getSequence())
                    .containsExactly(1, 2, 3);
        }

        @Test
        @DisplayName("TECHNICAL 단계 답변만 필터링된다")
        void it_filters_by_phase() {
            // when
            List<Answer> technicalAnswers = answerRepository.findByInterviewIdAndPhase(
                    savedInterview.getId(), InterviewPhase.TECHNICAL);

            // then
            assertThat(technicalAnswers).hasSize(2);
        }
    }
}
