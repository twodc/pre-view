package com.example.pre_view.domain.auth.oauth2;

import java.util.Map;

/**
 * Google OAuth2 사용자 정보
 *
 * Google 응답 예시:
 * {
 *   "sub": "123456789",          // 고유 ID
 *   "email": "user@gmail.com",
 *   "name": "홍길동",
 *   "picture": "https://..."     // 프로필 이미지
 * }
 */
public class GoogleOAuth2UserInfo implements OAuth2UserInfo {

    private final Map<String, Object> attributes;

    public GoogleOAuth2UserInfo(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    @Override
    public String getProviderId() {
        return (String) attributes.get("sub");
    }

    @Override
    public String getEmail() {
        return (String) attributes.get("email");
    }

    @Override
    public String getName() {
        return (String) attributes.get("name");
    }

    @Override
    public String getProfileImage() {
        return (String) attributes.get("picture");
    }
}
