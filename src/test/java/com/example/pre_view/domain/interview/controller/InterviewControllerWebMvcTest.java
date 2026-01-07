package com.example.pre_view.domain.interview.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.oauth2.client.autoconfigure.OAuth2ClientAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.example.pre_view.common.exception.BusinessException;
import com.example.pre_view.common.exception.ErrorCode;
import com.example.pre_view.domain.answer.dto.AnswerCreateRequest;
import com.example.pre_view.domain.answer.dto.AnswerResponse;
import com.example.pre_view.domain.answer.service.AnswerFacade;
import com.example.pre_view.domain.auth.jwt.JwtAuthenticationFilter;
import com.example.pre_view.domain.interview.dto.InterviewCreateRequest;
import com.example.pre_view.domain.interview.dto.InterviewResponse;
import com.example.pre_view.domain.interview.dto.InterviewResultResponse;
import com.example.pre_view.domain.interview.enums.ExperienceLevel;
import com.example.pre_view.domain.interview.enums.InterviewPhase;
import com.example.pre_view.domain.interview.enums.InterviewStatus;
import com.example.pre_view.domain.interview.enums.InterviewType;
import com.example.pre_view.domain.interview.enums.Position;
import com.example.pre_view.domain.interview.service.InterviewService;
import com.example.pre_view.domain.question.dto.QuestionListResponse;
import com.example.pre_view.support.WebMvcTestSupport;

/**
 * InterviewController 슬라이스 테스트 (@WebMvcTest)
 *
 * <h2>설계 의도</h2>
 * <ul>
 *   <li>WebMvcTestSupport 상속: @CurrentMemberId ArgumentResolver stubbing 자동 설정</li>
 *   <li>@WebMvcTest: Controller 레이어만 로드하여 빠른 테스트 실행</li>
 *   <li>@MockitoBean: Spring Boot 4.0에서 권장하는 Mock 어노테이션</li>
 *   <li>excludeAutoConfiguration: 불필요한 Security 자동 설정 제외</li>
 *   <li>DCI 패턴: Describe-Context-It 구조로 가독성 향상</li>
 * </ul>
 *
 * @see InterviewController
 * @see WebMvcTestSupport
 */
@WebMvcTest(
        controllers = InterviewController.class,
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                OAuth2ClientAutoConfiguration.class
        },
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = {JwtAuthenticationFilter.class}
        )
)
@Import(InterviewControllerWebMvcTest.LocalTestSecurityConfig.class)
@DisplayName("InterviewController 슬라이스 테스트 (@WebMvcTest)")
class InterviewControllerWebMvcTest extends WebMvcTestSupport {

    /**
     * 테스트 클래스 내부에 정의된 Security 설정
     * - 외부 의존성 없이 완전히 독립적인 테스트 환경 구성
     * - CSRF 비활성화, 모든 요청 허용
     */
    @TestConfiguration
    @EnableWebSecurity
    static class LocalTestSecurityConfig {

        @Bean
        SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
            http
                    .csrf(csrf -> csrf.disable())
                    .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
            return http.build();
        }
    }

    @MockitoBean
    private InterviewService interviewService;

    @MockitoBean
    private AnswerFacade answerFacade;

    private static final Long TEST_INTERVIEW_ID = 100L;

    private InterviewResponse createMockInterviewResponse(
            InterviewStatus status, InterviewPhase phase, Integer totalQuestions) {
        return new InterviewResponse(
                TEST_INTERVIEW_ID,
                "테스트 면접",
                InterviewType.TECHNICAL,
                "기술 면접",
                Position.BACKEND,
                "백엔드",
                ExperienceLevel.JUNIOR,
                "주니어",
                List.of("Java", "Spring"),
                status,
                phase,
                phase != null ? phase.getDescription() : null,
                totalQuestions,
                false,
                false
        );
    }

    @Nested
    @DisplayName("POST /api/v1/interviews 메서드는")
    class Describe_createInterview {

        @Nested
        @DisplayName("유효한 요청으로 면접을 생성하면")
        class Context_with_valid_request {

            @Test
            @DisplayName("200 OK와 함께 생성된 면접 정보를 반환한다")
            void it_returns_created_interview() throws Exception {
                // given
                InterviewCreateRequest request = new InterviewCreateRequest(
                        "백엔드 기술 면접",
                        InterviewType.TECHNICAL,
                        Position.BACKEND,
                        ExperienceLevel.JUNIOR,
                        List.of("Java", "Spring")
                );
                InterviewResponse response = createMockInterviewResponse(InterviewStatus.READY, null, 0);
                given(interviewService.createInterview(any(InterviewCreateRequest.class), eq(TEST_MEMBER_ID)))
                        .willReturn(response);

                // when & then
                mockMvc.perform(post("/api/v1/interviews")
                                .with(user("testuser").roles("USER"))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(jsonMapper.writeValueAsString(request)))
                        .andDo(print())
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.success").value(true))
                        .andExpect(jsonPath("$.message").value("면접이 생성되었습니다."))
                        .andExpect(jsonPath("$.data.id").value(TEST_INTERVIEW_ID))
                        .andExpect(jsonPath("$.data.status").value("READY"));
            }
        }

        @Nested
        @DisplayName("필수 필드가 누락된 경우")
        class Context_with_missing_required_fields {

            @Test
            @DisplayName("400 Bad Request를 반환한다")
            void it_returns_400_bad_request() throws Exception {
                // given - title이 null인 요청
                String invalidRequest = """
                    {
                        "type": "TECHNICAL",
                        "position": "BACKEND",
                        "level": "JUNIOR"
                    }
                    """;

                // when & then
                mockMvc.perform(post("/api/v1/interviews")
                                .with(user("testuser").roles("USER"))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(invalidRequest))
                        .andDo(print())
                        .andExpect(status().isBadRequest());
            }
        }
    }

    @Nested
    @DisplayName("POST /api/v1/interviews/{id}/start 메서드는")
    class Describe_startInterview {

        @Nested
        @DisplayName("유효한 면접 ID로 시작하면")
        class Context_with_valid_interview_id {

            @Test
            @DisplayName("질문이 생성되고 200 OK를 반환한다")
            void it_returns_started_interview() throws Exception {
                // given
                InterviewResponse response = createMockInterviewResponse(
                        InterviewStatus.IN_PROGRESS, InterviewPhase.TECHNICAL, 5);
                given(interviewService.startInterview(eq(TEST_INTERVIEW_ID), eq(TEST_MEMBER_ID)))
                        .willReturn(response);

                // when & then
                mockMvc.perform(post("/api/v1/interviews/{id}/start", TEST_INTERVIEW_ID)
                                .with(user("testuser").roles("USER"))
                                .with(csrf()))
                        .andDo(print())
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.success").value(true))
                        .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"))
                        .andExpect(jsonPath("$.data.totalQuestions").value(5));
            }
        }

        @Nested
        @DisplayName("존재하지 않는 면접 ID인 경우")
        class Context_with_non_existing_interview {

            @Test
            @DisplayName("404 Not Found를 반환한다")
            void it_returns_404_not_found() throws Exception {
                // given
                willThrow(new BusinessException(ErrorCode.INTERVIEW_NOT_FOUND))
                        .given(interviewService).startInterview(eq(999L), eq(TEST_MEMBER_ID));

                // when & then
                mockMvc.perform(post("/api/v1/interviews/{id}/start", 999L)
                                .with(user("testuser").roles("USER"))
                                .with(csrf()))
                        .andDo(print())
                        .andExpect(status().isNotFound())
                        .andExpect(jsonPath("$.code").value("I001"));
            }
        }

        @Nested
        @DisplayName("다른 사용자의 면접을 시작하려는 경우")
        class Context_with_unauthorized_access {

            @Test
            @DisplayName("403 Forbidden을 반환한다")
            void it_returns_403_forbidden() throws Exception {
                // given
                willThrow(new BusinessException(ErrorCode.ACCESS_DENIED))
                        .given(interviewService).startInterview(eq(TEST_INTERVIEW_ID), eq(TEST_MEMBER_ID));

                // when & then
                mockMvc.perform(post("/api/v1/interviews/{id}/start", TEST_INTERVIEW_ID)
                                .with(user("testuser").roles("USER"))
                                .with(csrf()))
                        .andDo(print())
                        .andExpect(status().isForbidden())
                        .andExpect(jsonPath("$.code").value("AUTH006"));
            }
        }
    }

    @Nested
    @DisplayName("GET /api/v1/interviews/{id} 메서드는")
    class Describe_getInterview {

        @Nested
        @DisplayName("유효한 면접 ID로 조회하면")
        class Context_with_valid_interview_id {

            @Test
            @DisplayName("면접 정보를 반환한다")
            void it_returns_interview_info() throws Exception {
                // given
                InterviewResponse response = createMockInterviewResponse(
                        InterviewStatus.IN_PROGRESS, InterviewPhase.TECHNICAL, 5);
                given(interviewService.getInterview(eq(TEST_INTERVIEW_ID), eq(TEST_MEMBER_ID)))
                        .willReturn(response);

                // when & then
                mockMvc.perform(get("/api/v1/interviews/{id}", TEST_INTERVIEW_ID)
                                .with(user("testuser").roles("USER")))
                        .andDo(print())
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.success").value(true))
                        .andExpect(jsonPath("$.data.id").value(TEST_INTERVIEW_ID))
                        .andExpect(jsonPath("$.data.type").value("TECHNICAL"))
                        .andExpect(jsonPath("$.data.position").value("BACKEND"));
            }
        }

        @Nested
        @DisplayName("존재하지 않는 면접 ID인 경우")
        class Context_with_non_existing_interview {

            @Test
            @DisplayName("404 Not Found를 반환한다")
            void it_returns_404_not_found() throws Exception {
                // given
                willThrow(new BusinessException(ErrorCode.INTERVIEW_NOT_FOUND))
                        .given(interviewService).getInterview(eq(999L), eq(TEST_MEMBER_ID));

                // when & then
                mockMvc.perform(get("/api/v1/interviews/{id}", 999L)
                                .with(user("testuser").roles("USER")))
                        .andDo(print())
                        .andExpect(status().isNotFound())
                        .andExpect(jsonPath("$.code").value("I001"));
            }
        }
    }

    @Nested
    @DisplayName("GET /api/v1/interviews 메서드는")
    class Describe_getInterviews {

        @Nested
        @DisplayName("면접 목록을 조회하면")
        class Context_with_valid_request {

            @Test
            @DisplayName("페이징된 면접 목록을 반환한다")
            void it_returns_paged_interviews() throws Exception {
                // given
                InterviewResponse response = createMockInterviewResponse(
                        InterviewStatus.READY, null, 0);
                Page<InterviewResponse> page = new PageImpl<>(
                        List.of(response),
                        org.springframework.data.domain.PageRequest.of(0, 10),
                        1
                );
                given(interviewService.getInterviews(eq(TEST_MEMBER_ID), any(Pageable.class)))
                        .willReturn(page);

                // when & then
                mockMvc.perform(get("/api/v1/interviews")
                                .with(user("testuser").roles("USER"))
                                .param("page", "0")
                                .param("size", "10"))
                        .andDo(print())
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.success").value(true));
            }
        }
    }

    @Nested
    @DisplayName("GET /api/v1/interviews/{id}/questions 메서드는")
    class Describe_getQuestions {

        @Nested
        @DisplayName("면접 질문 목록을 조회하면")
        class Context_with_valid_interview {

            @Test
            @DisplayName("질문 목록을 반환한다")
            void it_returns_question_list() throws Exception {
                // given
                QuestionListResponse response = new QuestionListResponse(
                        TEST_INTERVIEW_ID,
                        3,   // totalCount
                        3,   // mainQuestionCount
                        0,   // followUpCount
                        Map.of()  // questionsByPhase
                );
                given(interviewService.getQuestions(eq(TEST_INTERVIEW_ID), eq(TEST_MEMBER_ID)))
                        .willReturn(response);

                // when & then
                mockMvc.perform(get("/api/v1/interviews/{id}/questions", TEST_INTERVIEW_ID)
                                .with(user("testuser").roles("USER")))
                        .andDo(print())
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.success").value(true))
                        .andExpect(jsonPath("$.data.interviewId").value(TEST_INTERVIEW_ID))
                        .andExpect(jsonPath("$.data.mainQuestionCount").value(3));
            }
        }
    }

    @Nested
    @DisplayName("POST /api/v1/interviews/{id}/questions/{questionId}/answers 메서드는")
    class Describe_createAnswer {

        @Nested
        @DisplayName("유효한 답변을 제출하면")
        class Context_with_valid_answer {

            @Test
            @DisplayName("AI 피드백과 함께 200 OK를 반환한다")
            void it_returns_answer_with_feedback() throws Exception {
                // given
                Long questionId = 10L;
                AnswerCreateRequest request = new AnswerCreateRequest("이것은 테스트 답변입니다.");
                AnswerResponse response = new AnswerResponse(
                        1L,                             // id
                        questionId,                      // questionId
                        "테스트 질문입니다",              // questionContent
                        InterviewPhase.TECHNICAL,       // phase
                        "기술",                          // phaseDescription
                        "이것은 테스트 답변입니다.",      // content
                        "좋은 답변입니다.",               // feedback
                        85,                              // score
                        null                             // followUpQuestion
                );
                given(answerFacade.createAnswer(
                        eq(TEST_INTERVIEW_ID), eq(questionId), eq(TEST_MEMBER_ID), any(AnswerCreateRequest.class)))
                        .willReturn(response);

                // when & then
                mockMvc.perform(post("/api/v1/interviews/{id}/questions/{questionId}/answers",
                                TEST_INTERVIEW_ID, questionId)
                                .with(user("testuser").roles("USER"))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(jsonMapper.writeValueAsString(request)))
                        .andDo(print())
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.success").value(true))
                        .andExpect(jsonPath("$.data.score").value(85))
                        .andExpect(jsonPath("$.data.feedback").value("좋은 답변입니다."));
            }
        }

        @Nested
        @DisplayName("빈 답변을 제출하면")
        class Context_with_empty_answer {

            @Test
            @DisplayName("400 Bad Request를 반환한다")
            void it_returns_400_bad_request() throws Exception {
                // given
                Long questionId = 10L;
                String invalidRequest = """
                    {
                        "content": ""
                    }
                    """;

                // when & then
                mockMvc.perform(post("/api/v1/interviews/{id}/questions/{questionId}/answers",
                                TEST_INTERVIEW_ID, questionId)
                                .with(user("testuser").roles("USER"))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(invalidRequest))
                        .andDo(print())
                        .andExpect(status().isBadRequest());
            }
        }
    }

    @Nested
    @DisplayName("GET /api/v1/interviews/{id}/result 메서드는")
    class Describe_getInterviewResult {

        @Nested
        @DisplayName("완료된 면접 결과를 조회하면")
        class Context_with_completed_interview {

            @Test
            @DisplayName("면접 결과를 반환한다")
            void it_returns_interview_result() throws Exception {
                // given
                InterviewResultResponse response = new InterviewResultResponse(
                        TEST_INTERVIEW_ID,              // interviewId
                        "테스트 면접",                   // title
                        InterviewType.TECHNICAL,        // type
                        Position.BACKEND,               // position
                        "백엔드",                        // positionDescription
                        List.of("Java", "Spring"),      // techStacks
                        LocalDateTime.now(),            // createdAt
                        5,                              // totalQuestions
                        5,                              // answeredQuestions
                        85.0,                           // averageScore
                        Map.of(),                       // questionAnswersByPhase
                        null                            // aiReport
                );
                given(interviewService.getInterviewResult(eq(TEST_INTERVIEW_ID), eq(TEST_MEMBER_ID)))
                        .willReturn(response);

                // when & then
                mockMvc.perform(get("/api/v1/interviews/{id}/result", TEST_INTERVIEW_ID)
                                .with(user("testuser").roles("USER")))
                        .andDo(print())
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.success").value(true))
                        .andExpect(jsonPath("$.data.interviewId").value(TEST_INTERVIEW_ID))
                        .andExpect(jsonPath("$.data.averageScore").value(85.0));
            }
        }

        @Nested
        @DisplayName("완료되지 않은 면접 결과를 조회하면")
        class Context_with_incomplete_interview {

            @Test
            @DisplayName("400 Bad Request를 반환한다")
            void it_returns_400_bad_request() throws Exception {
                // given
                willThrow(new BusinessException(ErrorCode.INVALID_INTERVIEW_STATUS))
                        .given(interviewService).getInterviewResult(eq(TEST_INTERVIEW_ID), eq(TEST_MEMBER_ID));

                // when & then
                mockMvc.perform(get("/api/v1/interviews/{id}/result", TEST_INTERVIEW_ID)
                                .with(user("testuser").roles("USER")))
                        .andDo(print())
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.code").value("I002"));
            }
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/interviews/{id} 메서드는")
    class Describe_deleteInterview {

        @Nested
        @DisplayName("유효한 면접 ID로 삭제하면")
        class Context_with_valid_interview_id {

            @Test
            @DisplayName("200 OK를 반환한다")
            void it_returns_200_ok() throws Exception {
                // given
                willDoNothing().given(interviewService).deleteInterview(eq(TEST_INTERVIEW_ID), eq(TEST_MEMBER_ID));

                // when & then
                mockMvc.perform(delete("/api/v1/interviews/{id}", TEST_INTERVIEW_ID)
                                .with(user("testuser").roles("USER"))
                                .with(csrf()))
                        .andDo(print())
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.success").value(true))
                        .andExpect(jsonPath("$.message").value("면접이 삭제되었습니다."));
            }
        }

        @Nested
        @DisplayName("존재하지 않는 면접 ID인 경우")
        class Context_with_non_existing_interview {

            @Test
            @DisplayName("404 Not Found를 반환한다")
            void it_returns_404_not_found() throws Exception {
                // given
                willThrow(new BusinessException(ErrorCode.INTERVIEW_NOT_FOUND))
                        .given(interviewService).deleteInterview(eq(999L), eq(TEST_MEMBER_ID));

                // when & then
                mockMvc.perform(delete("/api/v1/interviews/{id}", 999L)
                                .with(user("testuser").roles("USER"))
                                .with(csrf()))
                        .andDo(print())
                        .andExpect(status().isNotFound())
                        .andExpect(jsonPath("$.code").value("I001"));
            }
        }
    }
}
