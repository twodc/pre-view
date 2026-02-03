package com.example.pre_view.domain.auth.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.oauth2.client.autoconfigure.OAuth2ClientAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.pre_view.common.exception.BusinessException;
import com.example.pre_view.common.exception.ErrorCode;
import com.example.pre_view.domain.auth.dto.LoginRequest;
import com.example.pre_view.domain.auth.dto.SignupRequest;
import com.example.pre_view.domain.auth.dto.TokenReissueRequest;
import com.example.pre_view.domain.auth.dto.TokenResponse;
import com.example.pre_view.domain.auth.jwt.JwtAuthenticationFilter;
import com.example.pre_view.domain.auth.jwt.JwtTokenProvider;
import com.example.pre_view.domain.auth.service.AccessTokenBlacklistService;
import com.example.pre_view.domain.auth.service.AuthService;

import tools.jackson.databind.json.JsonMapper;

/**
 * AuthController WebMvc 슬라이스 테스트
 *
 * <p>Spring Boot 4.0의 @WebMvcTest를 사용하여 컨트롤러 레이어만 테스트합니다.
 * 순수 Mockito 단위테스트 대신 실제 Spring MVC 인프라를 활용합니다.</p>
 */
@WebMvcTest(
    controllers = AuthController.class,
    excludeAutoConfiguration = {
        SecurityAutoConfiguration.class,
        OAuth2ClientAutoConfiguration.class
    },
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        classes = {JwtAuthenticationFilter.class}
    )
)
@Import(AuthControllerWebMvcTest.LocalTestSecurityConfig.class)
@DisplayName("AuthController WebMvc 슬라이스 테스트")
class AuthControllerWebMvcTest {

    /**
     * 테스트용 Security 설정 - 인증 우회
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

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private AccessTokenBlacklistService accessTokenBlacklistService;

    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    @Nested
    @DisplayName("POST /api/v1/auth/signup 메서드는")
    class Describe_signup {

        @Nested
        @DisplayName("유효한 요청인 경우")
        class Context_with_valid_request {

            @Test
            @DisplayName("200 OK와 성공 메시지를 반환한다")
            void signup_withValidRequest_returns200() throws Exception {
                // given
                SignupRequest request = new SignupRequest(
                        "test@example.com",
                        "테스트유저",
                        "password123!"
                );
                willDoNothing().given(authService).signup(any(SignupRequest.class));

                // when & then
                mockMvc.perform(post("/api/v1/auth/signup")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(jsonMapper.writeValueAsString(request)))
                        .andDo(print())
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.success").value(true))
                        .andExpect(jsonPath("$.message").value("회원가입이 완료되었습니다."));
            }
        }

        @Nested
        @DisplayName("중복된 이메일인 경우")
        class Context_with_duplicate_email {

            @Test
            @DisplayName("409 Conflict를 반환한다")
            void signup_withDuplicateEmail_returns409() throws Exception {
                // given
                SignupRequest request = new SignupRequest(
                        "duplicate@example.com",
                        "테스트유저",
                        "password123!"
                );
                willThrow(new BusinessException(ErrorCode.DUPLICATE_EMAIL))
                        .given(authService).signup(any(SignupRequest.class));

                // when & then
                mockMvc.perform(post("/api/v1/auth/signup")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(jsonMapper.writeValueAsString(request)))
                        .andDo(print())
                        .andExpect(status().isConflict())
                        .andExpect(jsonPath("$.code").value("M002"));
            }
        }

        @Nested
        @DisplayName("유효하지 않은 요청인 경우")
        class Context_with_invalid_request {

            @Test
            @DisplayName("이메일 형식이 잘못되면 400 Bad Request를 반환한다")
            void signup_withInvalidEmail_returns400() throws Exception {
                // given - 잘못된 이메일 형식
                String invalidRequest = """
                    {
                        "email": "invalid-email",
                        "name": "테스트유저",
                        "password": "password123!"
                    }
                    """;

                // when & then
                mockMvc.perform(post("/api/v1/auth/signup")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(invalidRequest))
                        .andDo(print())
                        .andExpect(status().isBadRequest());
            }

            @Test
            @DisplayName("이름이 비어있으면 400 Bad Request를 반환한다")
            void signup_withEmptyName_returns400() throws Exception {
                // given
                String invalidRequest = """
                    {
                        "email": "test@example.com",
                        "name": "",
                        "password": "password123!"
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
    }

    @Nested
    @DisplayName("POST /api/v1/auth/login 메서드는")
    class Describe_login {

        @Nested
        @DisplayName("올바른 자격증명인 경우")
        class Context_with_valid_credentials {

            @Test
            @DisplayName("토큰 쌍을 반환한다")
            void login_withValidCredentials_returnsTokens() throws Exception {
                // given
                LoginRequest request = new LoginRequest("test@example.com", "password123!");
                TokenResponse tokenResponse = TokenResponse.of("accessToken", "refreshToken");
                given(authService.login(any(LoginRequest.class))).willReturn(tokenResponse);

                // when & then
                mockMvc.perform(post("/api/v1/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(jsonMapper.writeValueAsString(request)))
                        .andDo(print())
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.success").value(true))
                        .andExpect(jsonPath("$.data.accessToken").value("accessToken"))
                        .andExpect(jsonPath("$.data.refreshToken").value("refreshToken"));
            }
        }

        @Nested
        @DisplayName("잘못된 자격증명인 경우")
        class Context_with_invalid_credentials {

            @Test
            @DisplayName("401 Unauthorized를 반환한다")
            void login_withInvalidCredentials_returns401() throws Exception {
                // given
                LoginRequest request = new LoginRequest("test@example.com", "wrongPassword");
                willThrow(new BusinessException(ErrorCode.INVALID_CREDENTIALS))
                        .given(authService).login(any(LoginRequest.class));

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
        @DisplayName("잠긴 계정인 경우")
        class Context_with_locked_account {

            @Test
            @DisplayName("429 Too Many Requests를 반환한다")
            void login_withLockedAccount_returns429() throws Exception {
                // given
                LoginRequest request = new LoginRequest("locked@example.com", "password123!");
                willThrow(new BusinessException(ErrorCode.ACCOUNT_LOCKED))
                        .given(authService).login(any(LoginRequest.class));

                // when & then
                mockMvc.perform(post("/api/v1/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(jsonMapper.writeValueAsString(request)))
                        .andDo(print())
                        .andExpect(status().isTooManyRequests())
                        .andExpect(jsonPath("$.code").value("AUTH007"));
            }
        }
    }

    @Nested
    @DisplayName("POST /api/v1/auth/reissue 메서드는")
    class Describe_reissueToken {

        @Nested
        @DisplayName("유효한 Refresh Token인 경우")
        class Context_with_valid_refresh_token {

            @Test
            @DisplayName("새로운 토큰 쌍을 반환한다")
            void reissueToken_withValidToken_returnsNewTokens() throws Exception {
                // given
                TokenReissueRequest request = new TokenReissueRequest("validRefreshToken");
                TokenResponse tokenResponse = TokenResponse.of("newAccessToken", "newRefreshToken");
                given(authService.reissueToken(anyString())).willReturn(tokenResponse);

                // when & then
                mockMvc.perform(post("/api/v1/auth/reissue")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(jsonMapper.writeValueAsString(request)))
                        .andDo(print())
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.success").value(true))
                        .andExpect(jsonPath("$.data.accessToken").value("newAccessToken"))
                        .andExpect(jsonPath("$.data.refreshToken").value("newRefreshToken"));
            }
        }

        @Nested
        @DisplayName("유효하지 않은 Refresh Token인 경우")
        class Context_with_invalid_refresh_token {

            @Test
            @DisplayName("401 Unauthorized를 반환한다")
            void reissueToken_withInvalidToken_returns401() throws Exception {
                // given
                TokenReissueRequest request = new TokenReissueRequest("invalidToken");
                willThrow(new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN))
                        .given(authService).reissueToken(anyString());

                // when & then
                mockMvc.perform(post("/api/v1/auth/reissue")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(jsonMapper.writeValueAsString(request)))
                        .andDo(print())
                        .andExpect(status().isUnauthorized())
                        .andExpect(jsonPath("$.code").value("AUTH004"));
            }
        }
    }

    @Nested
    @DisplayName("POST /api/v1/auth/logout 메서드는")
    class Describe_logout {

        @Nested
        @DisplayName("유효한 토큰으로 요청하는 경우")
        class Context_with_valid_token {

            @Test
            @DisplayName("200 OK와 성공 메시지를 반환한다")
            void logout_withValidToken_returns200() throws Exception {
                // given
                given(jwtTokenProvider.validateToken(anyString())).willReturn(true);
                willDoNothing().given(authService).logout(anyString());

                // when & then
                mockMvc.perform(post("/api/v1/auth/logout")
                                .header("Authorization", "Bearer validAccessToken"))
                        .andDo(print())
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.success").value(true))
                        .andExpect(jsonPath("$.message").value("로그아웃 되었습니다."));
            }
        }

        @Nested
        @DisplayName("Authorization 헤더가 없는 경우")
        class Context_without_authorization_header {

            @Test
            @DisplayName("401 Unauthorized를 반환한다")
            void logout_withoutAuthorizationHeader_returns401() throws Exception {
                // when & then
                mockMvc.perform(post("/api/v1/auth/logout"))
                        .andDo(print())
                        .andExpect(status().isUnauthorized());
            }
        }

        @Nested
        @DisplayName("Bearer 형식이 아닌 경우")
        class Context_with_invalid_bearer_format {

            @Test
            @DisplayName("401 Unauthorized를 반환한다")
            void logout_withInvalidBearerFormat_returns401() throws Exception {
                // when & then
                mockMvc.perform(post("/api/v1/auth/logout")
                                .header("Authorization", "InvalidFormat token"))
                        .andDo(print())
                        .andExpect(status().isUnauthorized());
            }
        }
    }
}
