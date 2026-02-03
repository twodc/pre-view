package com.example.pre_view.domain.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.example.pre_view.common.exception.BusinessException;
import com.example.pre_view.common.exception.ErrorCode;
import com.example.pre_view.domain.auth.dto.LoginRequest;
import com.example.pre_view.domain.auth.dto.SignupRequest;
import com.example.pre_view.domain.auth.dto.TokenResponse;
import com.example.pre_view.domain.auth.jwt.JwtTokenProvider;
import com.example.pre_view.domain.member.entity.Member;
import com.example.pre_view.domain.member.enums.Role;
import com.example.pre_view.domain.member.repository.MemberRepository;

/**
 * AuthService 단위 테스트
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private AccessTokenBlacklistService blacklistService;

    @Mock
    private LoginAttemptService loginAttemptService;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    private Member testMember;
    private final String TEST_EMAIL = "test@example.com";
    private final String TEST_PASSWORD = "password123!";
    private final String ENCODED_PASSWORD = "encodedPassword";

    @BeforeEach
    void setUp() {
        testMember = Member.builder()
                .email(TEST_EMAIL)
                .name("테스트 유저")
                .password(ENCODED_PASSWORD)
                .role(Role.USER)
                .build();
    }

    @Nested
    @DisplayName("회원가입 테스트")
    class SignupTest {

        @Test
        @DisplayName("유효한 정보로 회원가입하면 성공한다")
        void signup_withValidRequest_succeeds() {
            // given
            SignupRequest request = new SignupRequest(TEST_EMAIL, "테스트 유저", TEST_PASSWORD);
            given(memberRepository.existsByEmail(TEST_EMAIL)).willReturn(false);
            given(passwordEncoder.encode(TEST_PASSWORD)).willReturn(ENCODED_PASSWORD);

            // when
            authService.signup(request);

            // then
            verify(memberRepository).save(any(Member.class));
        }

        @Test
        @DisplayName("이미 존재하는 이메일로 회원가입하면 DUPLICATE_EMAIL 예외가 발생한다")
        void signup_withDuplicateEmail_throwsException() {
            // given
            SignupRequest request = new SignupRequest(TEST_EMAIL, "테스트 유저", TEST_PASSWORD);
            given(memberRepository.existsByEmail(TEST_EMAIL)).willReturn(true);

            // when & then
            assertThatThrownBy(() -> authService.signup(request))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.DUPLICATE_EMAIL);

            verify(memberRepository, never()).save(any(Member.class));
        }
    }

    @Nested
    @DisplayName("로그인 테스트")
    class LoginTest {

        @Test
        @DisplayName("올바른 자격증명으로 로그인하면 토큰을 반환한다")
        void login_withValidCredentials_returnsTokens() {
            // given
            LoginRequest request = new LoginRequest(TEST_EMAIL, TEST_PASSWORD);
            given(loginAttemptService.isLocked(TEST_EMAIL)).willReturn(false);
            given(memberRepository.findByEmail(TEST_EMAIL)).willReturn(Optional.of(testMember));
            given(passwordEncoder.matches(TEST_PASSWORD, ENCODED_PASSWORD)).willReturn(true);
            given(jwtTokenProvider.createAccessToken(anyString(), anyString())).willReturn("accessToken");
            given(jwtTokenProvider.createRefreshToken(anyString())).willReturn("refreshToken");

            // when
            TokenResponse response = authService.login(request);

            // then
            assertThat(response).isNotNull();
            assertThat(response.accessToken()).isEqualTo("accessToken");
            assertThat(response.refreshToken()).isEqualTo("refreshToken");

            verify(loginAttemptService).resetAttempts(TEST_EMAIL);
            verify(refreshTokenService).save(anyString(), anyString());
        }

        @Test
        @DisplayName("존재하지 않는 이메일로 로그인하면 INVALID_CREDENTIALS 예외가 발생한다")
        void login_withInvalidEmail_throwsException() {
            // given
            LoginRequest request = new LoginRequest("wrong@example.com", TEST_PASSWORD);
            given(loginAttemptService.isLocked("wrong@example.com")).willReturn(false);
            given(memberRepository.findByEmail("wrong@example.com")).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_CREDENTIALS);

            verify(loginAttemptService).recordFailedAttempt("wrong@example.com");
        }

        @Test
        @DisplayName("잘못된 비밀번호로 로그인하면 INVALID_CREDENTIALS 예외가 발생한다")
        void login_withWrongPassword_throwsException() {
            // given
            LoginRequest request = new LoginRequest(TEST_EMAIL, "wrongPassword");
            given(loginAttemptService.isLocked(TEST_EMAIL)).willReturn(false);
            given(memberRepository.findByEmail(TEST_EMAIL)).willReturn(Optional.of(testMember));
            given(passwordEncoder.matches("wrongPassword", ENCODED_PASSWORD)).willReturn(false);

            // when & then
            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_CREDENTIALS);

            verify(loginAttemptService).recordFailedAttempt(TEST_EMAIL);
        }

        @Test
        @DisplayName("잠긴 계정으로 로그인하면 ACCOUNT_LOCKED 예외가 발생한다")
        void login_withLockedAccount_throwsException() {
            // given
            LoginRequest request = new LoginRequest(TEST_EMAIL, TEST_PASSWORD);
            given(loginAttemptService.isLocked(TEST_EMAIL)).willReturn(true);

            // when & then
            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ACCOUNT_LOCKED);
        }

        @Test
        @DisplayName("OAuth 전용 회원이 비밀번호 로그인하면 INVALID_LOGIN_METHOD 예외가 발생한다")
        void login_withOAuthOnlyMember_throwsException() {
            // given
            Member oauthMember = Member.createOAuthMember(TEST_EMAIL, "OAuth 유저", null);
            LoginRequest request = new LoginRequest(TEST_EMAIL, TEST_PASSWORD);
            given(loginAttemptService.isLocked(TEST_EMAIL)).willReturn(false);
            given(memberRepository.findByEmail(TEST_EMAIL)).willReturn(Optional.of(oauthMember));

            // when & then
            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_LOGIN_METHOD);
        }
    }

    @Nested
    @DisplayName("토큰 재발급 테스트")
    class ReissueTokenTest {

        @Test
        @DisplayName("유효한 Refresh Token으로 재발급하면 새 토큰을 반환한다")
        void reissueToken_withValidToken_returnsNewTokens() {
            // given
            String refreshToken = "validRefreshToken";
            String memberId = "1";

            given(jwtTokenProvider.validateToken(refreshToken)).willReturn(true);
            given(jwtTokenProvider.getTokenType(refreshToken)).willReturn("refresh");
            given(jwtTokenProvider.getMemberId(refreshToken)).willReturn(memberId);
            given(refreshTokenService.validateToken(memberId, refreshToken)).willReturn(true);
            given(memberRepository.findById(1L)).willReturn(Optional.of(testMember));
            given(jwtTokenProvider.createAccessToken(anyString(), anyString())).willReturn("newAccessToken");
            given(jwtTokenProvider.createRefreshToken(anyString())).willReturn("newRefreshToken");

            // when
            TokenResponse response = authService.reissueToken(refreshToken);

            // then
            assertThat(response.accessToken()).isEqualTo("newAccessToken");
            assertThat(response.refreshToken()).isEqualTo("newRefreshToken");
            verify(refreshTokenService).save(memberId, "newRefreshToken");
        }

        @Test
        @DisplayName("유효하지 않은 Refresh Token으로 재발급하면 예외가 발생한다")
        void reissueToken_withInvalidToken_throwsException() {
            // given
            String invalidToken = "invalidToken";
            given(jwtTokenProvider.validateToken(invalidToken)).willReturn(false);

            // when & then
            assertThatThrownBy(() -> authService.reissueToken(invalidToken))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_REFRESH_TOKEN);
        }

        @Test
        @DisplayName("Access Token으로 재발급하면 INVALID_TOKEN_TYPE 예외가 발생한다")
        void reissueToken_withAccessToken_throwsException() {
            // given
            String accessToken = "accessToken";
            given(jwtTokenProvider.validateToken(accessToken)).willReturn(true);
            given(jwtTokenProvider.getTokenType(accessToken)).willReturn("access");

            // when & then
            assertThatThrownBy(() -> authService.reissueToken(accessToken))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_TOKEN_TYPE);
        }

        @Test
        @DisplayName("Redis에 저장된 토큰과 다르면 탈취 가능성으로 판단하여 예외가 발생한다")
        void reissueToken_withMismatchedToken_throwsException() {
            // given
            String refreshToken = "stolenToken";
            String memberId = "1";

            given(jwtTokenProvider.validateToken(refreshToken)).willReturn(true);
            given(jwtTokenProvider.getTokenType(refreshToken)).willReturn("refresh");
            given(jwtTokenProvider.getMemberId(refreshToken)).willReturn(memberId);
            given(refreshTokenService.validateToken(memberId, refreshToken)).willReturn(false);

            // when & then
            assertThatThrownBy(() -> authService.reissueToken(refreshToken))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_REFRESH_TOKEN);

            verify(refreshTokenService).delete(memberId);
        }
    }

    @Nested
    @DisplayName("로그아웃 테스트")
    class LogoutTest {

        @Test
        @DisplayName("로그아웃하면 Access Token이 블랙리스트에 등록되고 Refresh Token이 삭제된다")
        void logout_withValidToken_blacklistsAndDeletesTokens() {
            // given
            String accessToken = "validAccessToken";
            String memberId = "1";
            long remainingTime = 3600000L;

            given(jwtTokenProvider.getMemberId(accessToken)).willReturn(memberId);
            given(jwtTokenProvider.getRemainingTime(accessToken)).willReturn(remainingTime);

            // when
            authService.logout(accessToken);

            // then
            verify(blacklistService).add(accessToken, memberId, remainingTime);
            verify(refreshTokenService).delete(memberId);
        }

        @Test
        @DisplayName("만료된 Access Token으로 로그아웃하면 블랙리스트에 등록하지 않는다")
        void logout_withExpiredToken_skipsBlacklist() {
            // given
            String expiredToken = "expiredAccessToken";
            String memberId = "1";

            given(jwtTokenProvider.getMemberId(expiredToken)).willReturn(memberId);
            given(jwtTokenProvider.getRemainingTime(expiredToken)).willReturn(0L);

            // when
            authService.logout(expiredToken);

            // then
            verify(blacklistService, never()).add(anyString(), anyString(), any(Long.class));
            verify(refreshTokenService).delete(memberId);
        }
    }
}
