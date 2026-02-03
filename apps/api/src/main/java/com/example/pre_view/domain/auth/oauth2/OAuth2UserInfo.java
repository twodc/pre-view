package com.example.pre_view.domain.auth.oauth2;

import java.util.Map;

/**
 * OAuth2 제공자별 사용자 정보 추상화
 *
 * 각 제공자(Google, Kakao 등)마다 응답 형식이 다르므로
 * 공통 인터페이스로 추상화하여 처리
 */
public interface OAuth2UserInfo {

    String getProviderId();   // 제공자의 사용자 고유 ID
    String getEmail();
    String getName();
    String getProfileImage();

    /**
     * 제공자별 OAuth2UserInfo 구현체 생성
     */
    static OAuth2UserInfo of(String registrationId, Map<String, Object> attributes) {
        return switch (registrationId) {
            case "google" -> new GoogleOAuth2UserInfo(attributes);
            // case "kakao" -> new KakaoOAuth2UserInfo(attributes);
            default -> throw new IllegalArgumentException("지원하지 않는 OAuth2 제공자입니다: " + registrationId);
        };
    }
}
