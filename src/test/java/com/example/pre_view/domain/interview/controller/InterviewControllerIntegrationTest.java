package com.example.pre_view.domain.interview.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import com.example.pre_view.domain.auth.dto.LoginRequest;
import com.example.pre_view.domain.auth.dto.SignupRequest;
import com.example.pre_view.domain.interview.dto.InterviewCreateRequest;
import com.example.pre_view.domain.interview.enums.ExperienceLevel;
import com.example.pre_view.domain.interview.enums.InterviewType;
import com.example.pre_view.domain.interview.enums.Position;
import com.example.pre_view.domain.interview.repository.InterviewRepository;
import com.example.pre_view.support.IntegrationTestSupport;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * InterviewController 통합 테스트
 *
 * 면접 생성, 조회, 삭제 등 핵심 기능을 테스트합니다.
 * AI 서비스 의존성이 있는 기능(면접 시작, 답변 제출)은 별도 테스트 필요
 */
@AutoConfigureMockMvc
@Transactional
class InterviewControllerIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private InterviewRepository interviewRepository;

    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    private String accessToken;

    @BeforeEach
    void setUp() throws Exception {
        // 테스트용 회원 생성 및 로그인
        SignupRequest signupRequest = new SignupRequest(
                "interview@example.com",
                "면접테스트",
                "Password123!"
        );
        mockMvc.perform(post("/api/v1/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(signupRequest)));

        LoginRequest loginRequest = new LoginRequest("interview@example.com", "Password123!");
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(loginRequest)))
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        JsonNode jsonNode = jsonMapper.readTree(responseBody);
        accessToken = jsonNode.get("data").get("accessToken").textValue();
    }

    @Nested
    @DisplayName("면접 생성 통합 테스트")
    class CreateInterviewIntegrationTest {

        @Test
        @DisplayName("유효한 요청으로 면접을 생성하면 성공한다")
        void createInterview_withValidRequest_succeeds() throws Exception {
            // given
            InterviewCreateRequest request = new InterviewCreateRequest(
                    "백엔드 기술 면접",
                    InterviewType.TECHNICAL,
                    Position.BACKEND,
                    ExperienceLevel.JUNIOR,
                    List.of("Java", "Spring", "JPA")
            );

            // when & then
            mockMvc.perform(post("/api/v1/interviews")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("면접이 생성되었습니다."))
                    .andExpect(jsonPath("$.data.id").exists())
                    .andExpect(jsonPath("$.data.title").value("백엔드 기술 면접"))
                    .andExpect(jsonPath("$.data.type").value("TECHNICAL"))
                    .andExpect(jsonPath("$.data.position").value("BACKEND"))
                    .andExpect(jsonPath("$.data.status").value("READY"));
        }

        @Test
        @DisplayName("인증 없이 면접을 생성하면 인증 실패 응답을 반환한다")
        void createInterview_withoutAuth_returnsAuthError() throws Exception {
            // given
            InterviewCreateRequest request = new InterviewCreateRequest(
                    "테스트 면접",
                    InterviewType.TECHNICAL,
                    Position.BACKEND,
                    ExperienceLevel.JUNIOR,
                    List.of("Java")
            );

            // when & then
            mockMvc.perform(post("/api/v1/interviews")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(result -> {
                        int status = result.getResponse().getStatus();
                        assert status == 401 || status == 302 :
                                "Expected 401 or 302 but was " + status;
                    });
        }

        @Test
        @DisplayName("필수 필드 누락 시 400 Bad Request를 반환한다")
        void createInterview_withMissingFields_returns400() throws Exception {
            // given - title 누락
            String invalidRequest = """
                {
                    "type": "TECHNICAL",
                    "position": "BACKEND",
                    "level": "JUNIOR"
                }
                """;

            // when & then
            mockMvc.perform(post("/api/v1/interviews")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidRequest))
                    .andDo(print())
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("면접 조회 통합 테스트")
    class GetInterviewIntegrationTest {

        private Long interviewId;

        @BeforeEach
        void createInterview() throws Exception {
            InterviewCreateRequest request = new InterviewCreateRequest(
                    "조회 테스트 면접",
                    InterviewType.FULL,
                    Position.BACKEND,
                    ExperienceLevel.JUNIOR,
                    List.of("Java", "Spring")
            );

            MvcResult result = mockMvc.perform(post("/api/v1/interviews")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonMapper.writeValueAsString(request)))
                    .andReturn();

            JsonNode jsonNode = jsonMapper.readTree(result.getResponse().getContentAsString());
            interviewId = jsonNode.get("data").get("id").asLong();
        }

        @Test
        @DisplayName("면접 ID로 조회하면 면접 정보를 반환한다")
        void getInterview_withValidId_succeeds() throws Exception {
            // when & then
            mockMvc.perform(get("/api/v1/interviews/{id}", interviewId)
                            .header("Authorization", "Bearer " + accessToken))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.id").value(interviewId))
                    .andExpect(jsonPath("$.data.title").value("조회 테스트 면접"));
        }

        @Test
        @DisplayName("존재하지 않는 면접 ID로 조회하면 404를 반환한다")
        void getInterview_withInvalidId_returns404() throws Exception {
            // when & then
            mockMvc.perform(get("/api/v1/interviews/{id}", 99999L)
                            .header("Authorization", "Bearer " + accessToken))
                    .andDo(print())
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("I001"));
        }
    }

    @Nested
    @DisplayName("면접 목록 조회 통합 테스트")
    class GetInterviewsIntegrationTest {

        @BeforeEach
        void createMultipleInterviews() throws Exception {
            // 3개의 면접 생성
            for (int i = 1; i <= 3; i++) {
                InterviewCreateRequest request = new InterviewCreateRequest(
                        "테스트 면접 " + i,
                        InterviewType.TECHNICAL,
                        Position.BACKEND,
                        ExperienceLevel.JUNIOR,
                        List.of("Java")
                );

                mockMvc.perform(post("/api/v1/interviews")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(request)));
            }
        }

        @Test
        @DisplayName("면접 목록을 페이징하여 조회한다")
        void getInterviews_returnsPagedResults() throws Exception {
            // when & then
            mockMvc.perform(get("/api/v1/interviews")
                            .header("Authorization", "Bearer " + accessToken)
                            .param("page", "0")
                            .param("size", "10"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content").isArray())
                    .andExpect(jsonPath("$.data.totalElements").value(3));
        }

        @Test
        @DisplayName("페이지 크기를 지정하여 조회한다")
        void getInterviews_withCustomPageSize_returnsLimitedResults() throws Exception {
            // when & then
            mockMvc.perform(get("/api/v1/interviews")
                            .header("Authorization", "Bearer " + accessToken)
                            .param("page", "0")
                            .param("size", "2"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content.length()").value(2))
                    .andExpect(jsonPath("$.data.totalPages").value(2));
        }
    }

    @Nested
    @DisplayName("면접 삭제 통합 테스트")
    class DeleteInterviewIntegrationTest {

        private Long interviewId;

        @BeforeEach
        void createInterview() throws Exception {
            InterviewCreateRequest request = new InterviewCreateRequest(
                    "삭제 테스트 면접",
                    InterviewType.TECHNICAL,
                    Position.BACKEND,
                    ExperienceLevel.JUNIOR,
                    List.of("Java")
            );

            MvcResult result = mockMvc.perform(post("/api/v1/interviews")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonMapper.writeValueAsString(request)))
                    .andReturn();

            JsonNode jsonNode = jsonMapper.readTree(result.getResponse().getContentAsString());
            interviewId = jsonNode.get("data").get("id").asLong();
        }

        @Test
        @DisplayName("면접을 삭제하면 성공한다")
        void deleteInterview_withValidId_succeeds() throws Exception {
            // when
            mockMvc.perform(delete("/api/v1/interviews/{id}", interviewId)
                            .header("Authorization", "Bearer " + accessToken))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("면접이 삭제되었습니다."));

            // then - 삭제 후 조회하면 404
            mockMvc.perform(get("/api/v1/interviews/{id}", interviewId)
                            .header("Authorization", "Bearer " + accessToken))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("존재하지 않는 면접을 삭제하면 404를 반환한다")
        void deleteInterview_withInvalidId_returns404() throws Exception {
            // when & then
            mockMvc.perform(delete("/api/v1/interviews/{id}", 99999L)
                            .header("Authorization", "Bearer " + accessToken))
                    .andDo(print())
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("I001"));
        }
    }

    @Nested
    @DisplayName("면접 생성 및 조회 전체 플로우 테스트")
    class FullFlowIntegrationTest {

        @Test
        @DisplayName("면접 생성 -> 조회 -> 목록 조회 -> 삭제 전체 플로우")
        void fullInterviewFlow_succeeds() throws Exception {
            // 1. 면접 생성
            InterviewCreateRequest createRequest = new InterviewCreateRequest(
                    "전체 플로우 테스트",
                    InterviewType.FULL,
                    Position.FULLSTACK,
                    ExperienceLevel.MID,
                    List.of("Java", "React", "Docker")
            );

            MvcResult createResult = mockMvc.perform(post("/api/v1/interviews")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonMapper.writeValueAsString(createRequest)))
                    .andExpect(status().isOk())
                    .andReturn();

            JsonNode createResponse = jsonMapper.readTree(createResult.getResponse().getContentAsString());
            Long interviewId = createResponse.get("data").get("id").asLong();

            // 2. 단건 조회
            mockMvc.perform(get("/api/v1/interviews/{id}", interviewId)
                            .header("Authorization", "Bearer " + accessToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.title").value("전체 플로우 테스트"))
                    .andExpect(jsonPath("$.data.type").value("FULL"))
                    .andExpect(jsonPath("$.data.position").value("FULLSTACK"));

            // 3. 목록 조회
            mockMvc.perform(get("/api/v1/interviews")
                            .header("Authorization", "Bearer " + accessToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.totalElements").value(1));

            // 4. 삭제
            mockMvc.perform(delete("/api/v1/interviews/{id}", interviewId)
                            .header("Authorization", "Bearer " + accessToken))
                    .andExpect(status().isOk());

            // 5. 삭제 확인
            mockMvc.perform(get("/api/v1/interviews/{id}", interviewId)
                            .header("Authorization", "Bearer " + accessToken))
                    .andExpect(status().isNotFound());
        }
    }
}
