package com.example.pre_view.domain.auth.oauth2;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import com.example.pre_view.domain.auth.jwt.JwtTokenProvider;
import com.example.pre_view.domain.auth.service.RefreshTokenService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * OAuth2 로그인 성공 핸들러
 *
 * OAuth2 로그인 성공 후:
 * 1. JWT Access Token, Refresh Token 생성
 * 2. Refresh Token을 Redis에 저장
 * 3. 토큰을 URL 파라미터로 프론트엔드에 전달
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;

    @Value("${app.oauth2.redirect-uri:http://localhost:3000/oauth/callback}")
    private String redirectUri;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException {

        CustomOAuth2User oAuth2User = (CustomOAuth2User) authentication.getPrincipal();

        String memberId = String.valueOf(oAuth2User.getMemberId());
        String role = oAuth2User.getRole();

        // 1. JWT 토큰 생성
        String accessToken = jwtTokenProvider.createAccessToken(memberId, role);
        String refreshToken = jwtTokenProvider.createRefreshToken(memberId);

        // 2. Refresh Token을 Redis에 저장
        refreshTokenService.save(memberId, refreshToken);

        log.info("OAuth2 로그인 JWT 발급 - memberId: {}", memberId);

        // 3. 프론트엔드로 토큰과 함께 리다이렉트
        String targetUrl = String.format("%s?accessToken=%s&refreshToken=%s",
                redirectUri, accessToken, refreshToken);
        response.sendRedirect(targetUrl);
    }
}
