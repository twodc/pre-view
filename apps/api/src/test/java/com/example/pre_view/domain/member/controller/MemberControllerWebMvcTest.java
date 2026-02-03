package com.example.pre_view.domain.member.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.example.pre_view.common.exception.BusinessException;
import com.example.pre_view.common.exception.ErrorCode;
import com.example.pre_view.domain.auth.jwt.JwtAuthenticationFilter;
import com.example.pre_view.domain.member.dto.MemberResponse;
import com.example.pre_view.domain.member.dto.MemberUpdateRequest;
import com.example.pre_view.domain.member.service.MemberService;
import com.example.pre_view.support.WebMvcTestSupport;

/**
 * MemberController 슬라이스 테스트 (@WebMvcTest)
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
 * @see MemberController
 * @see WebMvcTestSupport
 */
@WebMvcTest(
        controllers = MemberController.class,
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                OAuth2ClientAutoConfiguration.class
        },
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = {JwtAuthenticationFilter.class}
        )
)
@Import(MemberControllerWebMvcTest.LocalTestSecurityConfig.class)
@DisplayName("MemberController 슬라이스 테스트 (@WebMvcTest)")
class MemberControllerWebMvcTest extends WebMvcTestSupport {

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
    private MemberService memberService;

    private static final String TEST_EMAIL = "test@example.com";
    private static final String TEST_NAME = "테스트유저";

    private MemberResponse createMockMemberResponse() {
        return new MemberResponse(
                TEST_MEMBER_ID,
                TEST_EMAIL,
                TEST_NAME,
                "https://example.com/profile.jpg"
        );
    }

    @Nested
    @DisplayName("GET /api/v1/members/me 메서드는")
    class Describe_getCurrentMember {

        @Nested
        @DisplayName("인증된 사용자가 요청하면")
        class Context_with_authenticated_user {

            @Test
            @DisplayName("200 OK와 함께 회원 정보를 반환한다")
            void it_returns_member_info() throws Exception {
                // given
                MemberResponse response = createMockMemberResponse();
                // @CurrentMemberId로 주입되는 TEST_MEMBER_ID와 매칭
                given(memberService.getCurrentMember(eq(TEST_MEMBER_ID))).willReturn(response);

                // when & then
                mockMvc.perform(get("/api/v1/members/me")
                                .with(user("testuser").roles("USER")))
                        .andDo(print())
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.success").value(true))
                        .andExpect(jsonPath("$.data.id").value(TEST_MEMBER_ID))
                        .andExpect(jsonPath("$.data.email").value(TEST_EMAIL))
                        .andExpect(jsonPath("$.data.name").value(TEST_NAME))
                        .andExpect(jsonPath("$.data.profileImage").value("https://example.com/profile.jpg"));
            }
        }

        @Nested
        @DisplayName("존재하지 않는 회원 ID로 요청하면")
        class Context_with_non_existing_member {

            @Test
            @DisplayName("404 Not Found를 반환한다")
            void it_returns_404() throws Exception {
                // given
                willThrow(new BusinessException(ErrorCode.MEMBER_NOT_FOUND))
                        .given(memberService).getCurrentMember(eq(TEST_MEMBER_ID));

                // when & then
                mockMvc.perform(get("/api/v1/members/me")
                                .with(user("testuser").roles("USER")))
                        .andDo(print())
                        .andExpect(status().isNotFound())
                        .andExpect(jsonPath("$.code").value("M001"));
            }
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/members/me 메서드는")
    class Describe_updateProfile {

        @Nested
        @DisplayName("유효한 요청으로 프로필을 수정하면")
        class Context_with_valid_request {

            @Test
            @DisplayName("200 OK와 함께 수정된 정보를 반환한다")
            void it_returns_updated_info() throws Exception {
                // given
                MemberUpdateRequest request = new MemberUpdateRequest(
                        "새이름",
                        "https://example.com/new-profile.jpg"
                );
                MemberResponse response = new MemberResponse(
                        TEST_MEMBER_ID,
                        TEST_EMAIL,
                        "새이름",
                        "https://example.com/new-profile.jpg"
                );
                given(memberService.updateProfile(eq(TEST_MEMBER_ID), any(MemberUpdateRequest.class)))
                        .willReturn(response);

                // when & then
                mockMvc.perform(put("/api/v1/members/me")
                                .with(user("testuser").roles("USER"))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(jsonMapper.writeValueAsString(request)))
                        .andDo(print())
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.success").value(true))
                        .andExpect(jsonPath("$.message").value("프로필이 수정되었습니다."))
                        .andExpect(jsonPath("$.data.name").value("새이름"))
                        .andExpect(jsonPath("$.data.profileImage").value("https://example.com/new-profile.jpg"));
            }
        }

        @Nested
        @DisplayName("이름만 수정 요청하면")
        class Context_with_name_only {

            @Test
            @DisplayName("이름만 수정되고 200 OK를 반환한다")
            void it_updates_name_only() throws Exception {
                // given
                MemberUpdateRequest request = new MemberUpdateRequest("새이름", null);
                MemberResponse response = new MemberResponse(
                        TEST_MEMBER_ID,
                        TEST_EMAIL,
                        "새이름",
                        null
                );
                given(memberService.updateProfile(eq(TEST_MEMBER_ID), any(MemberUpdateRequest.class)))
                        .willReturn(response);

                // when & then
                mockMvc.perform(put("/api/v1/members/me")
                                .with(user("testuser").roles("USER"))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(jsonMapper.writeValueAsString(request)))
                        .andDo(print())
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.name").value("새이름"));
            }
        }

        @Nested
        @DisplayName("이름이 50자를 초과하면")
        class Context_with_too_long_name {

            @Test
            @DisplayName("400 Bad Request를 반환한다")
            void it_returns_400() throws Exception {
                // given - 51자 이름
                String longName = "가".repeat(51);
                String invalidRequest = """
                    {
                        "name": "%s"
                    }
                    """.formatted(longName);

                // when & then
                mockMvc.perform(put("/api/v1/members/me")
                                .with(user("testuser").roles("USER"))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(invalidRequest))
                        .andDo(print())
                        .andExpect(status().isBadRequest());
            }
        }

        @Nested
        @DisplayName("프로필 이미지 URL이 500자를 초과하면")
        class Context_with_too_long_profile_image {

            @Test
            @DisplayName("400 Bad Request를 반환한다")
            void it_returns_400() throws Exception {
                // given - 501자 URL
                String longUrl = "https://example.com/" + "a".repeat(481);
                String invalidRequest = """
                    {
                        "name": "테스트",
                        "profileImage": "%s"
                    }
                    """.formatted(longUrl);

                // when & then
                mockMvc.perform(put("/api/v1/members/me")
                                .with(user("testuser").roles("USER"))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(invalidRequest))
                        .andDo(print())
                        .andExpect(status().isBadRequest());
            }
        }

        @Nested
        @DisplayName("존재하지 않는 회원이 수정 요청하면")
        class Context_with_non_existing_member {

            @Test
            @DisplayName("404 Not Found를 반환한다")
            void it_returns_404() throws Exception {
                // given
                MemberUpdateRequest request = new MemberUpdateRequest("새이름", null);
                willThrow(new BusinessException(ErrorCode.MEMBER_NOT_FOUND))
                        .given(memberService).updateProfile(eq(TEST_MEMBER_ID), any(MemberUpdateRequest.class));

                // when & then
                mockMvc.perform(put("/api/v1/members/me")
                                .with(user("testuser").roles("USER"))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(jsonMapper.writeValueAsString(request)))
                        .andDo(print())
                        .andExpect(status().isNotFound())
                        .andExpect(jsonPath("$.code").value("M001"));
            }
        }
    }
}
