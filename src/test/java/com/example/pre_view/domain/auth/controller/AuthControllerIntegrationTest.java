package com.example.pre_view.domain.auth.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import com.example.pre_view.domain.auth.dto.TokenReissueRequest;
import com.example.pre_view.domain.member.entity.Member;
import com.example.pre_view.domain.member.repository.MemberRepository;
import com.example.pre_view.support.IntegrationTestSupport;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * AuthController 통합 테스트
 *
 * 실제 Spring Context를 로드하고 Testcontainers Redis를 사용하여
 * 인증 API의 전체 흐름을 테스트합니다.
 */
@AutoConfigureMockMvc
@Transactional
class AuthControllerIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MemberRepository memberRepository;

    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    @Nested
    @DisplayName("회원가입 통합 테스트")
    class SignupIntegrationTest {

        @Test
        @DisplayName("유효한 정보로 회원가입하면 성공한다")
        void signup_withValidInfo_succeeds() throws Exception {
            // given
            SignupRequest request = new SignupRequest(
                    "newuser@example.com",
                    "테스트유저",
                    "Password123!"
            );

            // when & then
            mockMvc.perform(post("/api/v1/auth/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("회원가입이 완료되었습니다."));

            // 실제 DB에 저장되었는지 확인
            Member savedMember = memberRepository.findByEmail("newuser@example.com")
                    .orElseThrow();
            assert savedMember.getName().equals("테스트유저");
        }

        @Test
        @DisplayName("중복된 이메일로 회원가입하면 409 Conflict를 반환한다")
        void signup_withDuplicateEmail_returns409() throws Exception {
            // given - 먼저 회원가입
            SignupRequest firstRequest = new SignupRequest(
                    "duplicate@example.com",
                    "첫번째유저",
                    "Password123!"
            );
            mockMvc.perform(post("/api/v1/auth/signup")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(jsonMapper.writeValueAsString(firstRequest)));

            // when - 같은 이메일로 다시 회원가입
            SignupRequest duplicateRequest = new SignupRequest(
                    "duplicate@example.com",
                    "두번째유저",
                    "Password456!"
            );

            // then
            mockMvc.perform(post("/api/v1/auth/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonMapper.writeValueAsString(duplicateRequest)))
                    .andDo(print())
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("M002"));
        }

        @Test
        @DisplayName("유효하지 않은 이메일 형식이면 400 Bad Request를 반환한다")
        void signup_withInvalidEmail_returns400() throws Exception {
            // given
            String invalidRequest = """
                {
                    "email": "invalid-email",
                    "name": "테스트",
                    "password": "Password123!"
                }
                """;

            // when & then
            mockMvc.perform(post("/api/v1/auth/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidRequest))
                    .andDo(print())
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("로그인 통합 테스트")
    class LoginIntegrationTest {

        @BeforeEach
        void setUp() throws Exception {
            // 테스트용 회원 생성
            SignupRequest signupRequest = new SignupRequest(
                    "login@example.com",
                    "로그인테스트",
                    "Password123!"
            );
            mockMvc.perform(post("/api/v1/auth/signup")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(jsonMapper.writeValueAsString(signupRequest)));
        }

        @Test
        @DisplayName("올바른 자격증명으로 로그인하면 토큰을 반환한다")
        void login_withValidCredentials_returnsTokens() throws Exception {
            // given
            LoginRequest request = new LoginRequest("login@example.com", "Password123!");

            // when & then
            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.accessToken").exists())
                    .andExpect(jsonPath("$.data.refreshToken").exists())
                    .andExpect(jsonPath("$.data.tokenType").value("Bearer"));
        }

        @Test
        @DisplayName("잘못된 비밀번호로 로그인하면 401 Unauthorized를 반환한다")
        void login_withWrongPassword_returns401() throws Exception {
            // given
            LoginRequest request = new LoginRequest("login@example.com", "WrongPassword!");

            // when & then
            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("M003"));
        }

        @Test
        @DisplayName("존재하지 않는 이메일로 로그인하면 401 Unauthorized를 반환한다")
        void login_withNonExistentEmail_returns401() throws Exception {
            // given
            LoginRequest request = new LoginRequest("notexist@example.com", "Password123!");

            // when & then
            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("M003"));
        }
    }

    @Nested
    @DisplayName("토큰 재발급 통합 테스트")
    class ReissueTokenIntegrationTest {

        private String refreshToken;

        @BeforeEach
        void setUp() throws Exception {
            // 회원가입 및 로그인하여 토큰 획득
            SignupRequest signupRequest = new SignupRequest(
                    "reissue@example.com",
                    "재발급테스트",
                    "Password123!"
            );
            mockMvc.perform(post("/api/v1/auth/signup")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(jsonMapper.writeValueAsString(signupRequest)));

            LoginRequest loginRequest = new LoginRequest("reissue@example.com", "Password123!");
            MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonMapper.writeValueAsString(loginRequest)))
                    .andReturn();

            String responseBody = result.getResponse().getContentAsString();
            JsonNode jsonNode = jsonMapper.readTree(responseBody);
            refreshToken = jsonNode.get("data").get("refreshToken").asText();
        }

        @Test
        @DisplayName("유효한 Refresh Token으로 재발급하면 새 토큰을 반환한다")
        void reissueToken_withValidRefreshToken_returnsNewTokens() throws Exception {
            // given
            TokenReissueRequest request = new TokenReissueRequest(refreshToken);

            // when & then
            mockMvc.perform(post("/api/v1/auth/reissue")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.accessToken").exists())
                    .andExpect(jsonPath("$.data.refreshToken").exists());
        }

        @Test
        @DisplayName("유효하지 않은 Refresh Token으로 재발급하면 401을 반환한다")
        void reissueToken_withInvalidRefreshToken_returns401() throws Exception {
            // given
            TokenReissueRequest request = new TokenReissueRequest("invalid.refresh.token");

            // when & then
            mockMvc.perform(post("/api/v1/auth/reissue")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("로그아웃 통합 테스트")
    class LogoutIntegrationTest {

        private String accessToken;

        @BeforeEach
        void setUp() throws Exception {
            // 회원가입 및 로그인하여 토큰 획득
            SignupRequest signupRequest = new SignupRequest(
                    "logout@example.com",
                    "로그아웃테스트",
                    "Password123!"
            );
            mockMvc.perform(post("/api/v1/auth/signup")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(jsonMapper.writeValueAsString(signupRequest)));

            LoginRequest loginRequest = new LoginRequest("logout@example.com", "Password123!");
            MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonMapper.writeValueAsString(loginRequest)))
                    .andReturn();

            String responseBody = result.getResponse().getContentAsString();
            JsonNode jsonNode = jsonMapper.readTree(responseBody);
            accessToken = jsonNode.get("data").get("accessToken").asText();
        }

        @Test
        @DisplayName("유효한 토큰으로 로그아웃하면 성공한다")
        void logout_withValidToken_succeeds() throws Exception {
            // when & then
            mockMvc.perform(post("/api/v1/auth/logout")
                            .header("Authorization", "Bearer " + accessToken))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("로그아웃 되었습니다."));
        }

        @Test
        @DisplayName("Authorization 헤더가 없으면 401을 반환한다")
        void logout_withoutAuthorizationHeader_returns401() throws Exception {
            // when & then
            mockMvc.perform(post("/api/v1/auth/logout"))
                    .andDo(print())
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("로그아웃은 여러 번 호출해도 성공한다 (멱등성)")
        void logout_calledMultipleTimes_succeeds() throws Exception {
            // given - 먼저 로그아웃
            mockMvc.perform(post("/api/v1/auth/logout")
                            .header("Authorization", "Bearer " + accessToken))
                    .andExpect(status().isOk());

            // when & then - 같은 토큰으로 다시 로그아웃 시도
            // 로그아웃 API는 토큰 형식만 검증하므로 성공 (멱등성)
            mockMvc.perform(post("/api/v1/auth/logout")
                            .header("Authorization", "Bearer " + accessToken))
                    .andDo(print())
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("회원가입 후 로그인 전체 플로우 테스트")
    class FullFlowIntegrationTest {

        @Test
        @DisplayName("회원가입 -> 로그인 -> 토큰 재발급 -> 로그아웃 전체 플로우가 정상 동작한다")
        void fullAuthFlow_succeeds() throws Exception {
            // 1. 회원가입
            SignupRequest signupRequest = new SignupRequest(
                    "fullflow@example.com",
                    "전체플로우",
                    "Password123!"
            );
            mockMvc.perform(post("/api/v1/auth/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonMapper.writeValueAsString(signupRequest)))
                    .andExpect(status().isOk());

            // 2. 로그인
            LoginRequest loginRequest = new LoginRequest("fullflow@example.com", "Password123!");
            MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonMapper.writeValueAsString(loginRequest)))
                    .andExpect(status().isOk())
                    .andReturn();

            JsonNode loginResponse = jsonMapper.readTree(loginResult.getResponse().getContentAsString());
            String accessToken = loginResponse.get("data").get("accessToken").asText();
            String refreshToken = loginResponse.get("data").get("refreshToken").asText();

            // 3. 토큰 재발급
            TokenReissueRequest reissueRequest = new TokenReissueRequest(refreshToken);
            MvcResult reissueResult = mockMvc.perform(post("/api/v1/auth/reissue")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonMapper.writeValueAsString(reissueRequest)))
                    .andExpect(status().isOk())
                    .andReturn();

            JsonNode reissueResponse = jsonMapper.readTree(reissueResult.getResponse().getContentAsString());
            String newAccessToken = reissueResponse.get("data").get("accessToken").asText();

            // 4. 로그아웃 (새 토큰으로)
            mockMvc.perform(post("/api/v1/auth/logout")
                            .header("Authorization", "Bearer " + newAccessToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("로그아웃 되었습니다."));
        }
    }
}
