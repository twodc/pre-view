package com.example.pre_view.domain.member.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 사용자 권한을 정의하는 Enum
 * Spring Security에서 ROLE_ 접두사가 필요하므로 key에 포함
 */
@Getter
@RequiredArgsConstructor
public enum Role {

    USER("ROLE_USER", "일반 사용자"),
    ADMIN("ROLE_ADMIN", "관리자");

    private final String key;
    private final String description;
}
