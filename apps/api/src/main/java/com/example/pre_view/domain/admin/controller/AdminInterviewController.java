package com.example.pre_view.domain.admin.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.pre_view.common.dto.ApiResponse;
import com.example.pre_view.domain.admin.dto.AdminInterviewResponse;
import com.example.pre_view.domain.admin.dto.SystemStatisticsResponse;
import com.example.pre_view.domain.admin.service.AdminInterviewService;
import com.example.pre_view.domain.auth.annotation.CurrentMemberId;
import com.example.pre_view.domain.interview.enums.InterviewStatus;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 관리자용 면접/시스템 관리 API 컨트롤러
 *
 * 관리자가 면접을 조회, 삭제하고 시스템 통계를 확인할 수 있는 API를 제공합니다.
 */
@Slf4j
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - Interview", description = "관리자 면접/시스템 관리 API")
public class AdminInterviewController {

    private final AdminInterviewService adminInterviewService;

    @GetMapping("/interviews")
    @Operation(summary = "면접 목록 조회", description = "전체 면접 목록을 필터링하여 조회합니다.")
    public ResponseEntity<ApiResponse<Page<AdminInterviewResponse>>> getInterviews(
            @RequestParam(value = "memberId", required = false) Long memberId,
            @RequestParam(value = "status", required = false) InterviewStatus status,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @RequestParam(value = "sortBy", defaultValue = "createdAt") String sortBy,
            @CurrentMemberId Long adminMemberId
    ) {
        log.info("관리자 면접 목록 조회 API 호출 - adminMemberId: {}, memberId: {}, status: {}, page: {}",
                adminMemberId, memberId, status, page);
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy).descending());
        Page<AdminInterviewResponse> response = adminInterviewService.getInterviews(memberId, status, pageable);
        log.info("관리자 면접 목록 조회 완료 - 총 {}개 (전체: {}개)",
                response.getNumberOfElements(), response.getTotalElements());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/interviews/{id}")
    @Operation(summary = "면접 상세 조회", description = "특정 면접의 상세 정보를 조회합니다.")
    public ResponseEntity<ApiResponse<AdminInterviewResponse>> getInterview(
            @PathVariable("id") Long id,
            @CurrentMemberId Long adminMemberId
    ) {
        log.info("관리자 면접 상세 조회 API 호출 - interviewId: {}, adminMemberId: {}", id, adminMemberId);
        AdminInterviewResponse response = adminInterviewService.getInterview(id);
        log.info("관리자 면접 상세 조회 완료 - interviewId: {}, memberId: {}", id, response.memberId());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @DeleteMapping("/interviews/{id}")
    @Operation(summary = "면접 삭제", description = "특정 면접을 삭제합니다.")
    public ResponseEntity<ApiResponse<Void>> deleteInterview(
            @PathVariable("id") Long id,
            @CurrentMemberId Long adminMemberId
    ) {
        log.info("관리자 면접 삭제 API 호출 - interviewId: {}, adminMemberId: {}", id, adminMemberId);
        adminInterviewService.deleteInterview(id);
        log.info("관리자 면접 삭제 완료 - interviewId: {}", id);
        return ResponseEntity.ok(ApiResponse.ok("면접이 삭제되었습니다."));
    }

    @GetMapping("/statistics")
    @Operation(summary = "시스템 전체 통계 조회", description = "시스템 전체 통계를 조회합니다.")
    public ResponseEntity<ApiResponse<SystemStatisticsResponse>> getSystemStatistics(
            @CurrentMemberId Long adminMemberId
    ) {
        log.info("관리자 시스템 통계 조회 API 호출 - adminMemberId: {}", adminMemberId);
        SystemStatisticsResponse response = adminInterviewService.getSystemStatistics();
        log.info("관리자 시스템 통계 조회 완료 - 총 회원: {}, 총 면접: {}",
                response.totalMembers(), response.totalInterviews());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
