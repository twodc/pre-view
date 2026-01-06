package com.example.pre_view.domain.member.dto;

import jakarta.validation.constraints.Size;

/**
 * 회원 정보 수정 요청 DTO
 */
public record MemberUpdateRequest(
        @Size(min = 1, max = 50, message = "이름은 1~50자 이내여야 합니다.")
        String name,

        @Size(max = 500, message = "프로필 이미지 URL은 500자 이내여야 합니다.")
        String profileImage
) {
}
