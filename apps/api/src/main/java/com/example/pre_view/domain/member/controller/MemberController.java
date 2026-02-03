package com.example.pre_view.domain.member.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.pre_view.common.dto.ApiResponse;
import com.example.pre_view.domain.auth.annotation.CurrentMemberId;
import com.example.pre_view.domain.member.dto.MemberResponse;
import com.example.pre_view.domain.member.dto.MemberUpdateRequest;
import com.example.pre_view.domain.member.service.MemberService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 회원 관련 API 컨트롤러
 *
 * - GET  /api/v1/members/me : 현재 로그인한 회원 정보 조회
 * - PUT  /api/v1/members/me : 회원 프로필 수정
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/members")
@RequiredArgsConstructor
@Tag(name = "Member", description = "회원 API")
public class MemberController {

    private final MemberService memberService;

    @GetMapping("/me")
    @Operation(summary = "내 정보 조회", description = "현재 로그인한 회원의 정보를 조회합니다.")
    public ResponseEntity<ApiResponse<MemberResponse>> getCurrentMember(
            @CurrentMemberId Long memberId
    ) {
        log.info("내 정보 조회 API 호출 - memberId: {}", memberId);
        MemberResponse response = memberService.getCurrentMember(memberId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PutMapping("/me")
    @Operation(summary = "프로필 수정", description = "회원 프로필(이름, 프로필 이미지)을 수정합니다.")
    public ResponseEntity<ApiResponse<MemberResponse>> updateProfile(
            @CurrentMemberId Long memberId,
            @Valid @RequestBody MemberUpdateRequest request
    ) {
        log.info("프로필 수정 API 호출 - memberId: {}", memberId);
        MemberResponse response = memberService.updateProfile(memberId, request);
        return ResponseEntity.ok(ApiResponse.ok("프로필이 수정되었습니다.", response));
    }
}
