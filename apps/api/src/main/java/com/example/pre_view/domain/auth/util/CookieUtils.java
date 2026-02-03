package com.example.pre_view.domain.auth.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * 쿠키 관련 유틸리티
 *
 * 보안 설정:
 * - HttpOnly: JavaScript에서 접근 불가 (XSS 방지)
 * - Secure: HTTPS에서만 전송 (중간자 공격 방지)
 * - SameSite: CSRF 방지
 */
@Slf4j
@Component
public class CookieUtils {

    public static final String ACCESS_TOKEN_COOKIE = "access_token";
    public static final String REFRESH_TOKEN_COOKIE = "refresh_token";

    @Value("${jwt.access-expiration}")
    private long accessExpiration;

    @Value("${jwt.refresh-expiration}")
    private long refreshExpiration;

    @Value("${app.cookie.secure:false}")
    private boolean secure;  // 프로덕션에서는 true (HTTPS)

    @Value("${app.cookie.same-site:Lax}")
    private String sameSite;

    @Value("${app.cookie.domain:}")
    private String domain;

    /**
     * Access Token 쿠키 생성
     */
    public ResponseCookie createAccessTokenCookie(String token) {
        return createCookie(ACCESS_TOKEN_COOKIE, token, accessExpiration / 1000);
    }

    /**
     * Refresh Token 쿠키 생성
     */
    public ResponseCookie createRefreshTokenCookie(String token) {
        return createCookie(REFRESH_TOKEN_COOKIE, token, refreshExpiration / 1000);
    }

    /**
     * 쿠키 삭제 (로그아웃 시)
     */
    public ResponseCookie deleteAccessTokenCookie() {
        return deleteCookie(ACCESS_TOKEN_COOKIE);
    }

    public ResponseCookie deleteRefreshTokenCookie() {
        return deleteCookie(REFRESH_TOKEN_COOKIE);
    }

    /**
     * 요청에서 쿠키 값 추출
     */
    public String extractTokenFromCookie(HttpServletRequest request, String cookieName) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookieName.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    /**
     * 응답에 쿠키 추가
     */
    public void addCookiesToResponse(HttpServletResponse response,
                                      String accessToken,
                                      String refreshToken) {
        response.addHeader("Set-Cookie", createAccessTokenCookie(accessToken).toString());
        response.addHeader("Set-Cookie", createRefreshTokenCookie(refreshToken).toString());
    }

    /**
     * 응답에서 쿠키 삭제
     */
    public void deleteCookiesFromResponse(HttpServletResponse response) {
        response.addHeader("Set-Cookie", deleteAccessTokenCookie().toString());
        response.addHeader("Set-Cookie", deleteRefreshTokenCookie().toString());
    }

    private ResponseCookie createCookie(String name, String value, long maxAgeSeconds) {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(name, value)
                .httpOnly(true)           // JavaScript 접근 차단
                .secure(secure)           // HTTPS에서만 전송 (프로덕션)
                .sameSite(sameSite)       // CSRF 방지
                .path("/")                // 모든 경로에서 유효
                .maxAge(maxAgeSeconds);

        // 도메인 설정 (서브도메인 공유 필요 시)
        if (domain != null && !domain.isEmpty()) {
            builder.domain(domain);
        }

        return builder.build();
    }

    private ResponseCookie deleteCookie(String name) {
        return ResponseCookie.from(name, "")
                .httpOnly(true)
                .secure(secure)
                .sameSite(sameSite)
                .path("/")
                .maxAge(0)  // 즉시 만료
                .build();
    }
}
