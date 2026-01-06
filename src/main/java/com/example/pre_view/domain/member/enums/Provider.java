package com.example.pre_view.domain.member.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 인증 제공자를 정의하는 Enum
 * LOCAL: 이메일/비밀번호 회원가입
 * GOOGLE, KAKAO: OAuth2 소셜 로그인
 */
@Getter
@RequiredArgsConstructor
public enum Provider {

    LOCAL("local"),     // 일반 회원가입
    GOOGLE("google"),
    KAKAO("kakao");

    private final String registrationId;
}
