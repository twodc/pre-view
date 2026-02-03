package com.example.pre_view.domain.interview.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.example.pre_view.common.exception.BusinessException;
import com.example.pre_view.common.exception.ErrorCode;
import com.example.pre_view.domain.answer.repository.AnswerRepository;
import com.example.pre_view.domain.file.service.FileUploadService;
import com.example.pre_view.domain.interview.dto.InterviewCreateRequest;
import com.example.pre_view.domain.interview.dto.InterviewResponse;
import com.example.pre_view.domain.interview.entity.Interview;
import com.example.pre_view.domain.interview.enums.ExperienceLevel;
import com.example.pre_view.domain.interview.enums.InterviewStatus;
import com.example.pre_view.domain.interview.enums.InterviewType;
import com.example.pre_view.domain.interview.enums.Position;
import com.example.pre_view.domain.interview.repository.InterviewRepository;
import com.example.pre_view.domain.question.repository.QuestionRepository;
import com.example.pre_view.domain.question.service.QuestionService;

import tools.jackson.databind.json.JsonMapper;

/**
 * InterviewService 단위 테스트
 */
@ExtendWith(MockitoExtension.class)
class InterviewServiceTest {

    @Mock
    private InterviewRepository interviewRepository;

    @Mock
    private AiInterviewService aiInterviewService;

    @Mock
    private QuestionRepository questionRepository;

    @Mock
    private AnswerRepository answerRepository;

    @Mock
    private QuestionService questionService;

    @Mock
    private FileUploadService fileUploadService;

    @Mock
    private InterviewStatusService interviewStatusService;

    @Mock
    private JsonMapper jsonMapper;

    @InjectMocks
    private InterviewService interviewService;

    private Interview testInterview;
    private Long testMemberId;
    private Long testInterviewId;

    @BeforeEach
    void setUp() {
        testMemberId = 1L;
        testInterviewId = 1L;
        testInterview = createTestInterview();
    }

    private Interview createTestInterview() {
        return Interview.builder()
                .memberId(testMemberId)
                .title("백엔드 개발자 면접")
                .type(InterviewType.TECHNICAL)
                .position(Position.BACKEND)
                .level(ExperienceLevel.JUNIOR)
                .techStacks(List.of("Java", "Spring"))
                .status(InterviewStatus.READY)
                .build();
    }

    @Nested
    @DisplayName("면접 생성 테스트")
    class CreateInterviewTest {

        @Test
        @DisplayName("유효한 요청으로 면접을 생성하면 InterviewResponse를 반환한다")
        void createInterview_withValidRequest_returnsInterviewResponse() {
            // given
            InterviewCreateRequest request = new InterviewCreateRequest(
                    "백엔드 개발자 면접",
                    InterviewType.TECHNICAL,
                    Position.BACKEND,
                    ExperienceLevel.JUNIOR,
                    List.of("Java", "Spring")
            );

            given(interviewRepository.save(any(Interview.class)))
                    .willReturn(testInterview);

            // when
            InterviewResponse response = interviewService.createInterview(request, testMemberId);

            // then
            assertThat(response).isNotNull();
            assertThat(response.title()).isEqualTo("백엔드 개발자 면접");
            assertThat(response.type()).isEqualTo(InterviewType.TECHNICAL);
            assertThat(response.position()).isEqualTo(Position.BACKEND);
            assertThat(response.level()).isEqualTo(ExperienceLevel.JUNIOR);
            assertThat(response.status()).isEqualTo(InterviewStatus.READY);

            verify(interviewRepository).save(any(Interview.class));
        }

        @Test
        @DisplayName("기술 스택 없이도 면접을 생성할 수 있다")
        void createInterview_withoutTechStacks_succeeds() {
            // given
            InterviewCreateRequest request = new InterviewCreateRequest(
                    "인성 면접",
                    InterviewType.PERSONALITY,
                    Position.BACKEND,
                    ExperienceLevel.NEWCOMER,
                    null
            );

            Interview interviewWithoutTechStacks = Interview.builder()
                    .memberId(testMemberId)
                    .title("인성 면접")
                    .type(InterviewType.PERSONALITY)
                    .position(Position.BACKEND)
                    .level(ExperienceLevel.NEWCOMER)
                    .status(InterviewStatus.READY)
                    .build();

            given(interviewRepository.save(any(Interview.class)))
                    .willReturn(interviewWithoutTechStacks);

            // when
            InterviewResponse response = interviewService.createInterview(request, testMemberId);

            // then
            assertThat(response).isNotNull();
            assertThat(response.type()).isEqualTo(InterviewType.PERSONALITY);
        }
    }

    @Nested
    @DisplayName("면접 조회 테스트")
    class GetInterviewTest {

        @Test
        @DisplayName("존재하는 면접을 조회하면 InterviewResponse를 반환한다")
        void getInterview_withValidId_returnsInterviewResponse() {
            // given
            given(interviewRepository.findByIdAndMemberIdAndDeletedFalse(testInterviewId, testMemberId))
                    .willReturn(Optional.of(testInterview));

            // when
            InterviewResponse response = interviewService.getInterview(testInterviewId, testMemberId);

            // then
            assertThat(response).isNotNull();
            assertThat(response.title()).isEqualTo("백엔드 개발자 면접");
        }

        @Test
        @DisplayName("존재하지 않는 면접을 조회하면 INTERVIEW_NOT_FOUND 예외가 발생한다")
        void getInterview_withInvalidId_throwsException() {
            // given
            given(interviewRepository.findByIdAndMemberIdAndDeletedFalse(anyLong(), anyLong()))
                    .willReturn(Optional.empty());
            given(interviewRepository.findByIdAndDeletedFalse(anyLong()))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> interviewService.getInterview(999L, testMemberId))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INTERVIEW_NOT_FOUND);
        }

        @Test
        @DisplayName("다른 사용자의 면접을 조회하면 ACCESS_DENIED 예외가 발생한다")
        void getInterview_withUnauthorizedUser_throwsException() {
            // given
            Long otherMemberId = 999L;
            given(interviewRepository.findByIdAndMemberIdAndDeletedFalse(testInterviewId, otherMemberId))
                    .willReturn(Optional.empty());
            given(interviewRepository.findByIdAndDeletedFalse(testInterviewId))
                    .willReturn(Optional.of(testInterview));

            // when & then
            assertThatThrownBy(() -> interviewService.getInterview(testInterviewId, otherMemberId))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ACCESS_DENIED);
        }
    }

    @Nested
    @DisplayName("면접 목록 조회 테스트")
    class GetInterviewsTest {

        @Test
        @DisplayName("페이지네이션된 면접 목록을 조회한다")
        void getInterviews_withPagination_returnsPagedResponse() {
            // given
            Pageable pageable = PageRequest.of(0, 10);
            List<Interview> interviews = List.of(testInterview);
            Page<Interview> interviewPage = new PageImpl<>(interviews, pageable, 1);

            given(interviewRepository.findByMemberIdAndDeletedFalseOrderByCreatedAtDesc(testMemberId, pageable))
                    .willReturn(interviewPage);

            // when
            Page<InterviewResponse> responsePage = interviewService.getInterviews(testMemberId, pageable);

            // then
            assertThat(responsePage).isNotNull();
            assertThat(responsePage.getContent()).hasSize(1);
            assertThat(responsePage.getTotalElements()).isEqualTo(1);
        }

        @Test
        @DisplayName("면접이 없는 사용자는 빈 페이지를 반환한다")
        void getInterviews_withNoInterviews_returnsEmptyPage() {
            // given
            Pageable pageable = PageRequest.of(0, 10);
            Page<Interview> emptyPage = new PageImpl<>(List.of(), pageable, 0);

            given(interviewRepository.findByMemberIdAndDeletedFalseOrderByCreatedAtDesc(testMemberId, pageable))
                    .willReturn(emptyPage);

            // when
            Page<InterviewResponse> responsePage = interviewService.getInterviews(testMemberId, pageable);

            // then
            assertThat(responsePage).isNotNull();
            assertThat(responsePage.getContent()).isEmpty();
            assertThat(responsePage.getTotalElements()).isZero();
        }
    }

    @Nested
    @DisplayName("면접 삭제 테스트")
    class DeleteInterviewTest {

        @Test
        @DisplayName("면접을 삭제하면 soft delete 처리된다")
        void deleteInterview_withValidId_softDeletes() {
            // given
            given(interviewRepository.findByIdAndMemberIdAndDeletedFalse(testInterviewId, testMemberId))
                    .willReturn(Optional.of(testInterview));

            // when
            interviewService.deleteInterview(testInterviewId, testMemberId);

            // then
            assertThat(testInterview.isDeleted()).isTrue();
        }

        @Test
        @DisplayName("존재하지 않는 면접을 삭제하면 예외가 발생한다")
        void deleteInterview_withInvalidId_throwsException() {
            // given
            given(interviewRepository.findByIdAndMemberIdAndDeletedFalse(anyLong(), anyLong()))
                    .willReturn(Optional.empty());
            given(interviewRepository.findByIdAndDeletedFalse(anyLong()))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> interviewService.deleteInterview(999L, testMemberId))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INTERVIEW_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("면접 시작 테스트")
    class StartInterviewTest {

        @Test
        @DisplayName("READY 상태의 면접을 시작하면 IN_PROGRESS로 변경된다")
        void startInterview_withReadyStatus_changesStatusToInProgress() {
            // given
            given(interviewRepository.findByIdAndMemberIdAndDeletedFalse(testInterviewId, testMemberId))
                    .willReturn(Optional.of(testInterview));
            given(questionService.createTemplateQuestions(any(Interview.class)))
                    .willReturn(List.of());

            // when
            InterviewResponse response = interviewService.startInterview(testInterviewId, testMemberId);

            // then
            assertThat(response.status()).isEqualTo(InterviewStatus.IN_PROGRESS);
        }

        @Test
        @DisplayName("이미 시작된 면접을 다시 시작하면 예외가 발생한다")
        void startInterview_withInProgressStatus_throwsException() {
            // given
            testInterview.start();
            given(interviewRepository.findByIdAndMemberIdAndDeletedFalse(testInterviewId, testMemberId))
                    .willReturn(Optional.of(testInterview));

            // when & then
            assertThatThrownBy(() -> interviewService.startInterview(testInterviewId, testMemberId))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INTERVIEW_STATUS);
        }
    }
}
