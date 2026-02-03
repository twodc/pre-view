package com.example.pre_view.domain.admin.dto;

import com.example.pre_view.domain.member.enums.Role;

import jakarta.validation.constraints.NotNull;

/**
 * 권한 변경 요청 DTO
 */
public record RoleUpdateRequest(
        @NotNull(message = "변경할 권한을 입력해주세요.")
        Role role
) {
}
