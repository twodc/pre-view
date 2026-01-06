package com.example.pre_view.domain.auth.oauth2;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import com.example.pre_view.domain.member.entity.Member;

import lombok.Getter;

/**
 * Spring Security OAuth2User 구현체
 *
 * OAuth2 로그인 성공 후 SecurityContext에 저장되는 사용자 정보
 * Member 엔티티 정보를 포함하여 JWT 발급 시 사용
 */
@Getter
public class CustomOAuth2User implements OAuth2User {

    private final Member member;
    private final Map<String, Object> attributes;

    public CustomOAuth2User(Member member, Map<String, Object> attributes) {
        this.member = member;
        this.attributes = attributes;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singleton(
                new SimpleGrantedAuthority(member.getRole().getKey())
        );
    }

    @Override
    public String getName() {
        return member.getName();
    }

    // JWT 발급에 필요한 정보
    public Long getMemberId() {
        return member.getId();
    }

    public String getRole() {
        return member.getRole().getKey();
    }
}
