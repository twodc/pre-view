package com.example.pre_view.domain.statistics.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.pre_view.common.dto.ApiResponse;
import com.example.pre_view.domain.auth.annotation.CurrentMemberId;
import com.example.pre_view.domain.statistics.dto.DashboardSummaryResponse;
import com.example.pre_view.domain.statistics.dto.PhasePerformanceResponse;
import com.example.pre_view.domain.statistics.dto.RecentInterviewSummaryResponse;
import com.example.pre_view.domain.statistics.dto.ScoreTrendResponse;
import com.example.pre_view.domain.statistics.service.StatisticsService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 사용자 통계 API 컨트롤러
 *
 * 사용자의 면접 통계 대시보드 데이터를 제공합니다.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/statistics")
@RequiredArgsConstructor
@Tag(name = "Statistics", description = "사용자 통계 API")
public class StatisticsController {

    private final StatisticsService statisticsService;

    @GetMapping("/dashboard")
    @Operation(summary = "대시보드 요약 조회", description = "전체 대시보드 요약 정보를 조회합니다. (면접 횟수, 평균 점수 등)")
    public ResponseEntity<ApiResponse<DashboardSummaryResponse>> getDashboardSummary(
            @CurrentMemberId Long memberId
    ) {
        log.info("대시보드 요약 조회 API 호출 - memberId: {}", memberId);
        DashboardSummaryResponse response = statisticsService.getDashboardSummary(memberId);
        log.info("대시보드 요약 조회 완료 - 총 면접: {}, 완료: {}", response.totalInterviews(), response.completedInterviews());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/phases")
    @Operation(summary = "단계별 성과 조회", description = "단계별(TECHNICAL/PERSONALITY) 성과를 조회합니다.")
    public ResponseEntity<ApiResponse<List<PhasePerformanceResponse>>> getPhasePerformance(
            @CurrentMemberId Long memberId
    ) {
        log.info("단계별 성과 조회 API 호출 - memberId: {}", memberId);
        List<PhasePerformanceResponse> response = statisticsService.getPhasePerformance(memberId);
        log.info("단계별 성과 조회 완료 - 단계 수: {}", response.size());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/trends")
    @Operation(summary = "점수 추이 조회", description = "점수 추이를 월별 또는 주별로 조회합니다.")
    public ResponseEntity<ApiResponse<List<ScoreTrendResponse>>> getScoreTrends(
            @RequestParam(value = "period", defaultValue = "monthly") String period,
            @CurrentMemberId Long memberId
    ) {
        log.info("점수 추이 조회 API 호출 - memberId: {}, period: {}", memberId, period);
        List<ScoreTrendResponse> response = statisticsService.getScoreTrends(memberId, period);
        log.info("점수 추이 조회 완료 - 데이터 포인트: {}", response.size());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/recent")
    @Operation(summary = "최근 면접 요약 조회", description = "최근 면접 요약 정보를 조회합니다.")
    public ResponseEntity<ApiResponse<List<RecentInterviewSummaryResponse>>> getRecentInterviews(
            @RequestParam(value = "limit", defaultValue = "5") int limit,
            @CurrentMemberId Long memberId
    ) {
        log.info("최근 면접 조회 API 호출 - memberId: {}, limit: {}", memberId, limit);
        List<RecentInterviewSummaryResponse> response = statisticsService.getRecentInterviews(memberId, limit);
        log.info("최근 면접 조회 완료 - 조회 수: {}", response.size());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
