package com.example.pre_view.domain.auth.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.pre_view.common.dto.ApiResponse;
import com.example.pre_view.common.exception.BusinessException;
import com.example.pre_view.common.exception.ErrorCode;
import com.example.pre_view.domain.auth.dto.LoginRequest;
import com.example.pre_view.domain.auth.dto.SignupRequest;
import com.example.pre_view.domain.auth.dto.TokenReissueRequest;
import com.example.pre_view.domain.auth.dto.TokenResponse;
import com.example.pre_view.domain.auth.service.AuthService;
import com.example.pre_view.domain.auth.jwt.JwtTokenProvider;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 인증 관련 API 컨트롤러
 *
 * - POST /api/v1/auth/signup  : 회원가입
 * - POST /api/v1/auth/login   : 로그인
 * - POST /api/v1/auth/reissue : Access Token 재발급
 * - POST /api/v1/auth/logout  : 로그아웃
 */
@Slf4j
@Tag(name = "Auth", description = "인증 API")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * 회원가입
     */
    @Operation(summary = "회원가입", description = "이메일/비밀번호로 회원가입합니다.")
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<Void>> signup(
            @Valid @RequestBody SignupRequest request
    ) {
        log.info("회원가입 API 호출 - email: {}", request.email());
        authService.signup(request);
        return ResponseEntity.ok(ApiResponse.ok("회원가입이 완료되었습니다."));
    }

    /**
     * 로그인
     */
    @Operation(summary = "로그인", description = "이메일/비밀번호로 로그인합니다.")
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<TokenResponse>> login(
            @Valid @RequestBody LoginRequest request
    ) {
        log.info("로그인 API 호출 - email: {}", request.email());
        TokenResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    /**
     * Access Token 재발급
     *
     * Refresh Token으로 새로운 Access Token과 Refresh Token을 발급받습니다.
     * Refresh Token Rotation이 적용되어 매 재발급 시 Refresh Token도 갱신됩니다.
     */
    @Operation(summary = "토큰 재발급", description = "Refresh Token으로 새로운 토큰 쌍을 발급받습니다.")
    @PostMapping("/reissue")
    public ResponseEntity<ApiResponse<TokenResponse>> reissueToken(
            @Valid @RequestBody TokenReissueRequest request
    ) {
        log.info("토큰 재발급 API 호출");
        TokenResponse response = authService.reissueToken(request.refreshToken());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    private static final String BEARER_PREFIX = "Bearer ";

    /**
     * 로그아웃
     *
     * Access Token을 블랙리스트에 등록하고 Refresh Token을 삭제합니다.
     * Authorization 헤더에 Access Token을 포함해야 합니다.
     */
    @Operation(summary = "로그아웃", description = "현재 토큰을 무효화합니다.")
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @RequestHeader(value = "Authorization", required = false) String bearerToken
    ) {
        log.info("로그아웃 API 호출");
        // Authorization 헤더 유효성 검증
        if (!StringUtils.hasText(bearerToken) || !bearerToken.startsWith(BEARER_PREFIX)) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }

        String accessToken = bearerToken.substring(BEARER_PREFIX.length());

        // 토큰 유효성 검증
        if (!jwtTokenProvider.validateToken(accessToken)) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }

        authService.logout(accessToken);
        return ResponseEntity.ok(ApiResponse.ok("로그아웃 되었습니다."));
    }
}
