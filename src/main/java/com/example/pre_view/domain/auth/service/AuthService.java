package com.example.pre_view.domain.auth.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.pre_view.common.exception.BusinessException;
import com.example.pre_view.common.exception.ErrorCode;
import com.example.pre_view.domain.auth.dto.LoginRequest;
import com.example.pre_view.domain.auth.dto.SignupRequest;
import com.example.pre_view.domain.auth.dto.TokenResponse;
import com.example.pre_view.domain.auth.jwt.JwtTokenProvider;
import com.example.pre_view.domain.member.entity.Member;
import com.example.pre_view.domain.member.repository.MemberRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 인증 관련 비즈니스 로직
 *
 * - 회원가입 (이메일/비밀번호)
 * - 로그인 (이메일/비밀번호)
 * - 토큰 재발급 (Refresh Token Rotation)
 * - 로그아웃 (Access Token 블랙리스트 등록)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;
    private final AccessTokenBlacklistService blacklistService;
    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * 일반 회원가입
     *
     * @param request 이메일, 이름, 비밀번호
     */
    @Transactional
    public void signup(SignupRequest request) {
        // 1. 이메일 중복 확인
        if (memberRepository.existsByEmail(request.email())) {
            throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
        }

        // 2. 비밀번호 암호화
        String encodedPassword = passwordEncoder.encode(request.password());

        // 3. 회원 생성 및 저장
        Member member = Member.createLocalMember(request.email(), request.name(), encodedPassword);
        memberRepository.save(member);

        log.info("회원가입 완료 - email: {}", request.email());
    }

    /**
     * 일반 로그인
     *
     * @param request 이메일, 비밀번호
     * @return 토큰 쌍
     */
    @Transactional(readOnly = true)
    public TokenResponse login(LoginRequest request) {
        // 1. 이메일로 회원 조회
        Member member = memberRepository.findByEmail(request.email())
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CREDENTIALS));

        // 2. 비밀번호가 설정된 회원인지 확인 (OAuth 전용 회원 체크)
        if (!member.hasPassword()) {
            throw new BusinessException(ErrorCode.INVALID_LOGIN_METHOD);
        }

        // 3. 비밀번호 검증
        if (!passwordEncoder.matches(request.password(), member.getPassword())) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }

        // 4. JWT 토큰 발급
        String memberId = String.valueOf(member.getId());
        String accessToken = jwtTokenProvider.createAccessToken(memberId, member.getRole().getKey());
        String refreshToken = jwtTokenProvider.createRefreshToken(memberId);

        // 5. Refresh Token Redis에 저장
        refreshTokenService.save(memberId, refreshToken);

        log.info("로그인 완료 - memberId: {}", memberId);

        return TokenResponse.of(accessToken, refreshToken);
    }

    /**
     * Access Token 재발급 + Refresh Token Rotation
     *
     * 흐름:
     * 1. Refresh Token 유효성 검증
     * 2. Redis에 저장된 토큰과 일치하는지 확인 (탈취 방지)
     * 3. 새로운 Access Token, Refresh Token 발급
     * 4. Redis에 새 Refresh Token 저장 (Rotation)
     *
     * @param refreshToken 클라이언트가 보낸 Refresh Token
     * @return 새로운 토큰 쌍
     */
    @Transactional(readOnly = true)
    public TokenResponse reissueToken(String refreshToken) {
        // 1. Refresh Token 유효성 검증
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        // 2. 토큰 타입 확인
        String tokenType = jwtTokenProvider.getTokenType(refreshToken);
        if (!"refresh".equals(tokenType)) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN_TYPE);
        }

        // 3. memberId 추출 및 Redis 저장 토큰과 비교
        String memberId = jwtTokenProvider.getMemberId(refreshToken);
        if (!refreshTokenService.validateToken(memberId, refreshToken)) {
            // 저장된 토큰과 다름 → 탈취 가능성 → 모든 토큰 무효화
            log.warn("Refresh Token 불일치 - 탈취 가능성. memberId: {}", memberId);
            refreshTokenService.delete(memberId);
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        // 4. 회원 정보 조회 (role 가져오기)
        Member member = memberRepository.findById(Long.parseLong(memberId))
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        // 5. 새 토큰 발급
        String newAccessToken = jwtTokenProvider.createAccessToken(memberId, member.getRole().getKey());
        String newRefreshToken = jwtTokenProvider.createRefreshToken(memberId);

        // 6. Refresh Token Rotation (Redis에 새 토큰 저장)
        refreshTokenService.save(memberId, newRefreshToken);

        log.info("토큰 재발급 완료 - memberId: {}", memberId);

        return TokenResponse.of(newAccessToken, newRefreshToken);
    }

    /**
     * 로그아웃
     *
     * 흐름:
     * 1. Access Token을 블랙리스트에 등록 (남은 유효시간만큼)
     * 2. Redis에서 Refresh Token 삭제
     *
     * @param accessToken 로그아웃할 Access Token
     */
    public void logout(String accessToken) {
        // 1. Access Token에서 정보 추출
        String memberId = jwtTokenProvider.getMemberId(accessToken);
        long remainingTime = jwtTokenProvider.getRemainingTime(accessToken);

        // 2. Access Token 블랙리스트 등록
        if (remainingTime > 0) {
            blacklistService.add(accessToken, memberId, remainingTime);
        }

        // 3. Refresh Token 삭제
        refreshTokenService.delete(memberId);

        log.info("로그아웃 완료 - memberId: {}", memberId);
    }
}
