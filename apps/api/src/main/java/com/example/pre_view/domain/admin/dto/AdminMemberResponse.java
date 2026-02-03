package com.example.pre_view.domain.admin.dto;

import java.time.LocalDateTime;

import com.example.pre_view.domain.member.entity.Member;
import com.example.pre_view.domain.member.enums.Role;

/**
 * 관리자용 회원 정보 응답 DTO
 *
 * 관리자가 회원을 조회할 때 사용되는 상세 정보를 제공합니다.
 */
public record AdminMemberResponse(
        Long id,
        String email,
        String name,
        String profileImage,
        Role role,
        String roleDescription,
        boolean isActive,
        boolean hasPassword,
        int oauthAccountCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime deletedAt
) {
    public static AdminMemberResponse from(Member member) {
        return new AdminMemberResponse(
                member.getId(),
                member.getEmail(),
                member.getName(),
                member.getProfileImage(),
                member.getRole(),
                member.getRole().getDescription(),
                member.isActive(),
                member.hasPassword(),
                member.getOauthAccounts() != null ? member.getOauthAccounts().size() : 0,
                member.getCreatedAt(),
                member.getUpdatedAt(),
                member.getDeletedAt()
        );
    }
}
