package com.example.pre_view.domain.question.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.pre_view.common.exception.BusinessException;
import com.example.pre_view.common.exception.ErrorCode;
import com.example.pre_view.domain.answer.repository.AnswerRepository;
import com.example.pre_view.domain.interview.entity.Interview;
import com.example.pre_view.domain.interview.enums.ExperienceLevel;
import com.example.pre_view.domain.interview.enums.InterviewPhase;
import com.example.pre_view.domain.interview.enums.InterviewStatus;
import com.example.pre_view.domain.interview.enums.InterviewType;
import com.example.pre_view.domain.interview.enums.Position;
import com.example.pre_view.domain.interview.repository.InterviewRepository;
import com.example.pre_view.domain.interview.service.AiInterviewService;
import com.example.pre_view.domain.question.dto.QuestionListResponse;
import com.example.pre_view.domain.question.entity.Question;
import com.example.pre_view.domain.question.repository.QuestionRepository;

/**
 * QuestionService 단위 테스트
 */
@ExtendWith(MockitoExtension.class)
class QuestionServiceTest {

    @Mock
    private QuestionRepository questionRepository;

    @Mock
    private InterviewRepository interviewRepository;

    @Mock
    private AnswerRepository answerRepository;

    @Mock
    private AiInterviewService aiInterviewService;

    @InjectMocks
    private QuestionService questionService;

    private Interview testInterview;
    private Long testMemberId;

    @BeforeEach
    void setUp() {
        testMemberId = 1L;
        testInterview = createTestInterview(InterviewType.FULL);
    }

    private Interview createTestInterview(InterviewType type) {
        return Interview.builder()
                .memberId(testMemberId)
                .title("테스트 면접")
                .type(type)
                .position(Position.BACKEND)
                .level(ExperienceLevel.JUNIOR)
                .techStacks(List.of("Java", "Spring"))
                .status(InterviewStatus.READY)
                .build();
    }

    @Nested
    @DisplayName("템플릿 질문 생성 테스트")
    class CreateTemplateQuestionsTest {

        @Test
        @DisplayName("FULL 타입 면접에서 OPENING과 CLOSING 템플릿 질문이 생성된다")
        void createTemplateQuestions_withFullType_createsOpeningAndClosingQuestions() {
            // given
            given(questionRepository.saveAll(anyList()))
                    .willAnswer(invocation -> invocation.getArgument(0));

            // when
            List<Question> questions = questionService.createTemplateQuestions(testInterview);

            // then
            assertThat(questions).isNotEmpty();

            long openingCount = questions.stream()
                    .filter(q -> q.getPhase() == InterviewPhase.OPENING)
                    .count();
            long closingCount = questions.stream()
                    .filter(q -> q.getPhase() == InterviewPhase.CLOSING)
                    .count();

            assertThat(openingCount).isEqualTo(3);
            assertThat(closingCount).isEqualTo(2);

            verify(questionRepository).saveAll(anyList());
        }

        @Test
        @DisplayName("TECHNICAL 타입 면접에서는 템플릿 질문이 생성되지 않는다")
        void createTemplateQuestions_withTechnicalType_createsNoTemplateQuestions() {
            // given
            Interview technicalInterview = createTestInterview(InterviewType.TECHNICAL);
            given(questionRepository.saveAll(anyList()))
                    .willAnswer(invocation -> invocation.getArgument(0));

            // when
            List<Question> questions = questionService.createTemplateQuestions(technicalInterview);

            // then
            assertThat(questions).isEmpty();
        }

        @Test
        @DisplayName("PERSONALITY 타입 면접에서는 템플릿 질문이 생성되지 않는다")
        void createTemplateQuestions_withPersonalityType_createsNoTemplateQuestions() {
            // given
            Interview personalityInterview = createTestInterview(InterviewType.PERSONALITY);
            given(questionRepository.saveAll(anyList()))
                    .willAnswer(invocation -> invocation.getArgument(0));

            // when
            List<Question> questions = questionService.createTemplateQuestions(personalityInterview);

            // then
            assertThat(questions).isEmpty();
        }

        @Test
        @DisplayName("템플릿 질문은 순차적인 sequence 번호가 부여된다")
        void createTemplateQuestions_assignsSequentialNumbers() {
            // given
            given(questionRepository.saveAll(anyList()))
                    .willAnswer(invocation -> invocation.getArgument(0));

            // when
            List<Question> questions = questionService.createTemplateQuestions(testInterview);

            // then
            for (int i = 0; i < questions.size(); i++) {
                assertThat(questions.get(i).getSequence()).isEqualTo(i + 1);
            }
        }
    }

    @Nested
    @DisplayName("질문 목록 조회 테스트")
    class GetQuestionsTest {

        @Test
        @DisplayName("면접의 질문 목록을 조회한다")
        void getQuestions_withValidInterviewId_returnsQuestionList() {
            // given
            Long interviewId = 1L;
            Question question1 = Question.builder()
                    .content("질문 1")
                    .interview(testInterview)
                    .phase(InterviewPhase.OPENING)
                    .sequence(1)
                    .isFollowUp(false)
                    .build();
            Question question2 = Question.builder()
                    .content("질문 2")
                    .interview(testInterview)
                    .phase(InterviewPhase.OPENING)
                    .sequence(2)
                    .isFollowUp(false)
                    .build();

            given(interviewRepository.findByIdAndDeletedFalse(interviewId))
                    .willReturn(Optional.of(testInterview));
            // interview.getId()가 null이므로 null로 stubbing
            given(questionRepository.findByInterviewIdOrderBySequence(null))
                    .willReturn(List.of(question1, question2));

            // when
            QuestionListResponse response = questionService.getQuestions(interviewId);

            // then
            assertThat(response).isNotNull();
            assertThat(response.totalCount()).isEqualTo(2);
        }

        @Test
        @DisplayName("존재하지 않는 면접의 질문을 조회하면 INTERVIEW_NOT_FOUND 예외가 발생한다")
        void getQuestions_withInvalidInterviewId_throwsException() {
            // given
            given(interviewRepository.findByIdAndDeletedFalse(anyLong()))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> questionService.getQuestions(999L))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INTERVIEW_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("꼬리 질문 깊이 계산 테스트")
    class CalculateFollowUpDepthTest {

        @Test
        @DisplayName("주 질문의 깊이는 0이다")
        void calculateFollowUpDepth_withMainQuestion_returnsZero() {
            // given
            Question mainQuestion = Question.builder()
                    .content("주 질문")
                    .interview(testInterview)
                    .phase(InterviewPhase.TECHNICAL)
                    .sequence(1)
                    .isFollowUp(false)
                    .build();

            // when
            int depth = questionService.calculateFollowUpDepth(mainQuestion);

            // then
            assertThat(depth).isZero();
        }

        @Test
        @DisplayName("꼬리 질문의 깊이는 부모 질문 체인 수와 같다")
        void calculateFollowUpDepth_withFollowUpQuestion_returnsCorrectDepth() {
            // given
            Question mainQuestion = Question.builder()
                    .content("주 질문")
                    .interview(testInterview)
                    .phase(InterviewPhase.TECHNICAL)
                    .sequence(1)
                    .isFollowUp(false)
                    .build();

            Question followUp1 = Question.builder()
                    .content("꼬리 질문 1")
                    .interview(testInterview)
                    .phase(InterviewPhase.TECHNICAL)
                    .sequence(2)
                    .isFollowUp(true)
                    .parentQuestion(mainQuestion)
                    .build();

            Question followUp2 = Question.builder()
                    .content("꼬리 질문 2")
                    .interview(testInterview)
                    .phase(InterviewPhase.TECHNICAL)
                    .sequence(3)
                    .isFollowUp(true)
                    .parentQuestion(followUp1)
                    .build();

            // when
            int depth1 = questionService.calculateFollowUpDepth(followUp1);
            int depth2 = questionService.calculateFollowUpDepth(followUp2);

            // then
            assertThat(depth1).isEqualTo(1);
            assertThat(depth2).isEqualTo(2);
        }
    }
}
