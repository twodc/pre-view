package com.example.pre_view.domain.auth.jwt;

import java.io.IOException;
import java.util.List;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import com.example.pre_view.domain.auth.service.AccessTokenBlacklistService;
import com.example.pre_view.domain.auth.util.CookieUtils;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * JWT 인증 필터
 *
 * 모든 요청에서 JWT를 검증하고, 유효하면 SecurityContext에 인증 정보를 설정
 * 토큰 추출 우선순위: 1. Authorization 헤더 → 2. HttpOnly 쿠키
 *
 * OncePerRequestFilter: 요청당 한 번만 실행 보장
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenProvider jwtTokenProvider;
    private final AccessTokenBlacklistService blacklistService;
    private final CookieUtils cookieUtils;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        // 1. 헤더에서 토큰 추출
        String token = resolveToken(request);

        // 2. 토큰 검증 및 인증 처리
        if (StringUtils.hasText(token) && jwtTokenProvider.validateToken(token)) {

            // 3. 블랙리스트 확인 (로그아웃된 토큰인지)
            if (blacklistService.isBlacklisted(token)) {
                log.warn("블랙리스트에 등록된 토큰입니다.");
                // 인증 정보 설정하지 않고 다음 필터로 진행
                // → SecurityConfig에서 인증 필요한 경로면 401 반환
            } else {
                // 4. 토큰 타입 확인 (Access Token만 허용)
                String tokenType = jwtTokenProvider.getTokenType(token);
                if (!"access".equals(tokenType)) {
                    log.warn("Access Token이 아닙니다. type: {}", tokenType);
                } else {
                    // 5. SecurityContext에 인증 정보 설정
                    setAuthentication(token);
                }
            }
        }

        filterChain.doFilter(request, response);
    }

    /**
     * 토큰 추출 (우선순위: 헤더 → 쿠키)
     */
    private String resolveToken(HttpServletRequest request) {
        // 1. Authorization 헤더에서 추출
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX.length());
        }

        // 2. HttpOnly 쿠키에서 추출
        String cookieToken = cookieUtils.extractTokenFromCookie(request, CookieUtils.ACCESS_TOKEN_COOKIE);
        if (StringUtils.hasText(cookieToken)) {
            return cookieToken;
        }

        return null;
    }

    /**
     * SecurityContext에 인증 정보 설정
     *
     * UsernamePasswordAuthenticationToken:
     * - principal: 사용자 식별자 (memberId)
     * - credentials: 비밀번호 (JWT에서는 불필요하므로 null)
     * - authorities: 권한 목록
     */
    private void setAuthentication(String token) {
        String memberId = jwtTokenProvider.getMemberId(token);
        String role = jwtTokenProvider.getRole(token);

        // 권한 설정 (예: ROLE_USER → SimpleGrantedAuthority)
        List<SimpleGrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority(role)
        );

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(memberId, null, authorities);

        SecurityContextHolder.getContext().setAuthentication(authentication);
        log.debug("인증 정보 설정 완료 - memberId: {}, role: {}", memberId, role);
    }
}
