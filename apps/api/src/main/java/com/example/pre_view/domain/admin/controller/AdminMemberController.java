package com.example.pre_view.domain.admin.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.pre_view.common.dto.ApiResponse;
import com.example.pre_view.domain.admin.dto.AdminMemberResponse;
import com.example.pre_view.domain.admin.dto.RoleUpdateRequest;
import com.example.pre_view.domain.admin.service.AdminMemberService;
import com.example.pre_view.domain.auth.annotation.CurrentMemberId;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 관리자용 회원 관리 API 컨트롤러
 *
 * 관리자가 회원을 조회, 권한 변경, 비활성화할 수 있는 API를 제공합니다.
 */
@Slf4j
@RestController
@RequestMapping("/admin/members")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - Member", description = "관리자 회원 관리 API")
public class AdminMemberController {

    private final AdminMemberService adminMemberService;

    @GetMapping
    @Operation(summary = "회원 목록 조회", description = "전체 회원 목록을 페이징하여 조회합니다.")
    public ResponseEntity<ApiResponse<Page<AdminMemberResponse>>> getMembers(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @RequestParam(value = "sortBy", defaultValue = "createdAt") String sortBy,
            @CurrentMemberId Long adminMemberId
    ) {
        log.info("관리자 회원 목록 조회 API 호출 - adminMemberId: {}, page: {}, size: {}",
                adminMemberId, page, size);
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy).descending());
        Page<AdminMemberResponse> response = adminMemberService.getMembers(pageable);
        log.info("관리자 회원 목록 조회 완료 - 총 {}개 (전체: {}개)",
                response.getNumberOfElements(), response.getTotalElements());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "회원 상세 조회", description = "특정 회원의 상세 정보를 조회합니다.")
    public ResponseEntity<ApiResponse<AdminMemberResponse>> getMember(
            @PathVariable("id") Long id,
            @CurrentMemberId Long adminMemberId
    ) {
        log.info("관리자 회원 상세 조회 API 호출 - targetMemberId: {}, adminMemberId: {}",
                id, adminMemberId);
        AdminMemberResponse response = adminMemberService.getMember(id);
        log.info("관리자 회원 상세 조회 완료 - memberId: {}, email: {}", id, response.email());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PatchMapping("/{id}/role")
    @Operation(summary = "회원 권한 변경", description = "특정 회원의 권한을 변경합니다.")
    public ResponseEntity<ApiResponse<AdminMemberResponse>> updateRole(
            @PathVariable("id") Long id,
            @Valid @RequestBody RoleUpdateRequest request,
            @CurrentMemberId Long adminMemberId
    ) {
        log.info("관리자 회원 권한 변경 API 호출 - targetMemberId: {}, adminMemberId: {}, newRole: {}",
                id, adminMemberId, request.role());
        AdminMemberResponse response = adminMemberService.updateRole(id, adminMemberId, request);
        log.info("관리자 회원 권한 변경 완료 - memberId: {}, newRole: {}", id, response.role());
        return ResponseEntity.ok(ApiResponse.ok("권한이 변경되었습니다.", response));
    }

    @PatchMapping("/{id}/deactivate")
    @Operation(summary = "회원 계정 비활성화", description = "특정 회원의 계정을 비활성화합니다.")
    public ResponseEntity<ApiResponse<AdminMemberResponse>> deactivateMember(
            @PathVariable("id") Long id,
            @CurrentMemberId Long adminMemberId
    ) {
        log.info("관리자 회원 비활성화 API 호출 - targetMemberId: {}, adminMemberId: {}", id, adminMemberId);
        AdminMemberResponse response = adminMemberService.deactivateMember(id, adminMemberId);
        log.info("관리자 회원 비활성화 완료 - memberId: {}", id);
        return ResponseEntity.ok(ApiResponse.ok("계정이 비활성화되었습니다.", response));
    }
}
