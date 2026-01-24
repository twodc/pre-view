package com.example.pre_view.domain.auth.jwt;

import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;

/**
 * JWT 토큰 생성 및 검증 담당
 *
 * Access Token: 짧은 유효기간 (15분), 인증에 사용
 * Refresh Token: 긴 유효기간 (7일), Access Token 재발급용
 */
@Slf4j
@Component
public class JwtTokenProvider {

    private final SecretKey secretKey;
    private final long accessExpiration;
    private final long refreshExpiration;

    public JwtTokenProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-expiration}") long accessExpiration,
            @Value("${jwt.refresh-expiration}") long refreshExpiration
    ) {
        // JWT Secret 키 길이 검증 (HMAC-SHA256: 최소 256비트 = 32바이트)
        if (secret == null || secret.getBytes().length < 32) {
            throw new IllegalArgumentException(
                "JWT Secret 키는 최소 32바이트(256비트) 이상이어야 합니다. 현재: " +
                (secret == null ? "null" : secret.getBytes().length + "바이트")
            );
        }

        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes());
        this.accessExpiration = accessExpiration;
        this.refreshExpiration = refreshExpiration;
    }

    /**
     * Access Token 생성
     *
     * @param memberId 사용자 식별자
     * @param role 사용자 권한 (ROLE_USER, ROLE_ADMIN)
     * @return JWT Access Token
     */
    public String createAccessToken(String memberId, String role) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + accessExpiration);

        return Jwts.builder()
                .subject(memberId)                    // sub: 토큰 주체 (사용자 ID)
                .claim("role", role)                  // 커스텀 클레임: 권한
                .claim("type", "access")              // 토큰 타입 구분
                .issuedAt(now)                        // iat: 발급 시간
                .expiration(expiry)                   // exp: 만료 시간
                .signWith(secretKey)                  // 서명
                .compact();
    }

    /**
     * Refresh Token 생성
     * Access Token보다 정보가 적음 (role 없음)
     */
    public String createRefreshToken(String memberId) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + refreshExpiration);

        return Jwts.builder()
                .subject(memberId)
                .claim("type", "refresh")
                .issuedAt(now)
                .expiration(expiry)
                .signWith(secretKey)
                .compact();
    }

    /**
     * 토큰에서 memberId 추출
     */
    public String getMemberId(String token) {
        return parseClaims(token).getSubject();
    }

    /**
     * 토큰에서 role 추출
     */
    public String getRole(String token) {
        return parseClaims(token).get("role", String.class);
    }

    /**
     * 토큰에서 타입 추출 (access/refresh)
     */
    public String getTokenType(String token) {
        return parseClaims(token).get("type", String.class);
    }

    /**
     * 토큰의 남은 유효시간 반환 (밀리초)
     * Blacklist 등록 시 TTL 설정에 사용
     */
    public long getRemainingTime(String token) {
        Date expiration = parseClaims(token).getExpiration();
        return expiration.getTime() - System.currentTimeMillis();
    }

    /**
     * 토큰 유효성 검증
     *
     * @return true: 유효한 토큰, false: 유효하지 않은 토큰
     */
    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.warn("만료된 JWT 토큰입니다.");
        } catch (JwtException e) {
            log.warn("유효하지 않은 JWT 토큰입니다: {}", e.getMessage());
        }
        return false;
    }

    /**
     * 만료된 토큰인지 확인 (Refresh Token으로 재발급 시 사용)
     */
    public boolean isExpired(String token) {
        try {
            parseClaims(token);
            return false;
        } catch (ExpiredJwtException e) {
            return true;
        } catch (JwtException e) {
            return false; // 다른 문제가 있는 토큰
        }
    }

    /**
     * 만료된 토큰에서도 Claims 추출 (재발급 로직에서 사용)
     */
    public Claims parseClaimsAllowExpired(String token) {
        try {
            return parseClaims(token);
        } catch (ExpiredJwtException e) {
            // 만료된 토큰이어도 Claims는 반환됨
            return e.getClaims();
        }
    }

    /**
     * JWT 파싱 및 Claims 추출
     */
    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
