package com.example.pre_view.domain.member.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
import com.example.pre_view.domain.member.dto.MemberUpdateRequest;
import com.example.pre_view.support.IntegrationTestSupport;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * MemberController 통합 테스트
 *
 * 인증된 사용자의 회원 정보 조회 및 수정 API를 테스트합니다.
 */
@AutoConfigureMockMvc
@Transactional
class MemberControllerIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private MockMvc mockMvc;

    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    private String accessToken;

    @BeforeEach
    void setUp() throws Exception {
        // 테스트용 회원 생성 및 로그인
        SignupRequest signupRequest = new SignupRequest(
                "member@example.com",
                "테스트회원",
                "Password123!"
        );
        mockMvc.perform(post("/api/v1/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(signupRequest)));

        LoginRequest loginRequest = new LoginRequest("member@example.com", "Password123!");
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(loginRequest)))
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        JsonNode jsonNode = jsonMapper.readTree(responseBody);
        accessToken = jsonNode.get("data").get("accessToken").textValue();
    }

    @Nested
    @DisplayName("내 정보 조회 통합 테스트")
    class GetCurrentMemberIntegrationTest {

        @Test
        @DisplayName("인증된 사용자가 내 정보를 조회하면 성공한다")
        void getCurrentMember_withValidToken_succeeds() throws Exception {
            // when & then
            mockMvc.perform(get("/api/v1/members/me")
                            .header("Authorization", "Bearer " + accessToken))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.email").value("member@example.com"))
                    .andExpect(jsonPath("$.data.name").value("테스트회원"));
        }

        @Test
        @DisplayName("인증 없이 내 정보를 조회하면 인증 실패 응답을 반환한다")
        void getCurrentMember_withoutToken_returnsAuthError() throws Exception {
            // when & then
            // Spring Security OAuth2 설정에 따라 302(리다이렉트) 또는 401 반환
            mockMvc.perform(get("/api/v1/members/me"))
                    .andDo(print())
                    .andExpect(result -> {
                        int status = result.getResponse().getStatus();
                        assert status == 401 || status == 302 :
                                "Expected 401 or 302 but was " + status;
                    });
        }

        @Test
        @DisplayName("유효하지 않은 토큰으로 조회하면 인증 실패 응답을 반환한다")
        void getCurrentMember_withInvalidToken_returnsAuthError() throws Exception {
            // when & then
            mockMvc.perform(get("/api/v1/members/me")
                            .header("Authorization", "Bearer invalid.token.here"))
                    .andDo(print())
                    .andExpect(result -> {
                        int status = result.getResponse().getStatus();
                        assert status == 401 || status == 302 :
                                "Expected 401 or 302 but was " + status;
                    });
        }
    }

    @Nested
    @DisplayName("프로필 수정 통합 테스트")
    class UpdateProfileIntegrationTest {

        @Test
        @DisplayName("인증된 사용자가 이름을 수정하면 성공한다")
        void updateProfile_withValidName_succeeds() throws Exception {
            // given
            MemberUpdateRequest request = new MemberUpdateRequest("새이름", null);

            // when & then
            mockMvc.perform(put("/api/v1/members/me")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("프로필이 수정되었습니다."))
                    .andExpect(jsonPath("$.data.name").value("새이름"));

            // 다시 조회해서 변경되었는지 확인
            mockMvc.perform(get("/api/v1/members/me")
                            .header("Authorization", "Bearer " + accessToken))
                    .andExpect(jsonPath("$.data.name").value("새이름"));
        }

        @Test
        @DisplayName("프로필 이미지를 수정하면 성공한다")
        void updateProfile_withProfileImage_succeeds() throws Exception {
            // given
            MemberUpdateRequest request = new MemberUpdateRequest(null, "https://example.com/new-profile.jpg");

            // when & then
            mockMvc.perform(put("/api/v1/members/me")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.profileImage").value("https://example.com/new-profile.jpg"));
        }

        @Test
        @DisplayName("인증 없이 프로필을 수정하면 인증 실패 응답을 반환한다")
        void updateProfile_withoutToken_returnsAuthError() throws Exception {
            // given
            MemberUpdateRequest request = new MemberUpdateRequest("새이름", null);

            // when & then
            mockMvc.perform(put("/api/v1/members/me")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(result -> {
                        int status = result.getResponse().getStatus();
                        assert status == 401 || status == 302 || status == 403 :
                                "Expected 401, 302, or 403 but was " + status;
                    });
        }

        @Test
        @DisplayName("이름이 50자를 초과하면 400 Bad Request를 반환한다")
        void updateProfile_withTooLongName_returns400() throws Exception {
            // given - 51자 이름
            String longName = "가".repeat(51);
            MemberUpdateRequest request = new MemberUpdateRequest(longName, null);

            // when & then
            mockMvc.perform(put("/api/v1/members/me")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("회원 정보 조회 및 수정 전체 플로우 테스트")
    class FullFlowIntegrationTest {

        @Test
        @DisplayName("회원가입 -> 로그인 -> 정보 조회 -> 프로필 수정 -> 재조회 전체 플로우")
        void fullMemberFlow_succeeds() throws Exception {
            // 1. 새 회원 가입
            SignupRequest signupRequest = new SignupRequest(
                    "newmember@example.com",
                    "신규회원",
                    "Password123!"
            );
            mockMvc.perform(post("/api/v1/auth/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonMapper.writeValueAsString(signupRequest)))
                    .andExpect(status().isOk());

            // 2. 로그인
            LoginRequest loginRequest = new LoginRequest("newmember@example.com", "Password123!");
            MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonMapper.writeValueAsString(loginRequest)))
                    .andExpect(status().isOk())
                    .andReturn();

            JsonNode loginResponse = jsonMapper.readTree(loginResult.getResponse().getContentAsString());
            String token = loginResponse.get("data").get("accessToken").textValue();

            // 3. 내 정보 조회
            mockMvc.perform(get("/api/v1/members/me")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.name").value("신규회원"));

            // 4. 프로필 수정
            MemberUpdateRequest updateRequest = new MemberUpdateRequest(
                    "수정된이름",
                    "https://example.com/profile.png"
            );
            mockMvc.perform(put("/api/v1/members/me")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonMapper.writeValueAsString(updateRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.name").value("수정된이름"));

            // 5. 다시 조회하여 변경 확인
            mockMvc.perform(get("/api/v1/members/me")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.name").value("수정된이름"))
                    .andExpect(jsonPath("$.data.profileImage").value("https://example.com/profile.png"));
        }
    }
}
