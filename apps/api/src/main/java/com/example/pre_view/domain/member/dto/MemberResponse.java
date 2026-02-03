package com.example.pre_view.domain.member.dto;

import com.example.pre_view.domain.member.entity.Member;

/**
 * 회원 정보 응답 DTO
 */
public record MemberResponse(
        Long id,
        String email,
        String name,
        String profileImage
) {
    public static MemberResponse from(Member member) {
        return new MemberResponse(
                member.getId(),
                member.getEmail(),
                member.getName(),
                member.getProfileImage()
        );
    }
}
