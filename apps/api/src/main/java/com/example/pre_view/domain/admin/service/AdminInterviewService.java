package com.example.pre_view.domain.admin.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.pre_view.common.exception.BusinessException;
import com.example.pre_view.common.exception.ErrorCode;
import com.example.pre_view.domain.admin.dto.AdminInterviewResponse;
import com.example.pre_view.domain.admin.dto.DailyCountResponse;
import com.example.pre_view.domain.admin.dto.SystemStatisticsResponse;
import com.example.pre_view.domain.answer.repository.AnswerRepository;
import com.example.pre_view.domain.interview.entity.Interview;
import com.example.pre_view.domain.interview.enums.InterviewStatus;
import com.example.pre_view.domain.interview.repository.InterviewRepository;
import com.example.pre_view.domain.member.enums.Role;
import com.example.pre_view.domain.member.repository.MemberRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 관리자용 면접 관리 서비스
 *
 * 관리자가 면접을 조회, 삭제하고 시스템 통계를 확인할 수 있는 기능을 제공합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminInterviewService {

    private final InterviewRepository interviewRepository;
    private final MemberRepository memberRepository;
    private final AnswerRepository answerRepository;

    /**
     * 면접 목록 조회 (필터링, 페이징)
     */
    public Page<AdminInterviewResponse> getInterviews(Long memberId, InterviewStatus status, Pageable pageable) {
        log.debug("관리자 면접 목록 조회 - memberId: {}, status: {}, page: {}",
                memberId, status, pageable.getPageNumber());
        return interviewRepository.findAllWithFilters(memberId, status, pageable)
                .map(AdminInterviewResponse::from);
    }

    /**
     * 면접 상세 조회
     */
    public AdminInterviewResponse getInterview(Long interviewId) {
        log.debug("관리자 면접 상세 조회 - interviewId: {}", interviewId);
        Interview interview = findInterviewById(interviewId);
        return AdminInterviewResponse.from(interview);
    }

    /**
     * 면접 삭제 (소프트 삭제)
     */
    @Transactional
    public void deleteInterview(Long interviewId) {
        log.info("관리자 면접 삭제 - interviewId: {}", interviewId);
        Interview interview = findInterviewById(interviewId);
        interview.delete();
        log.info("관리자 면접 삭제 완료 - interviewId: {}", interviewId);
    }

    /**
     * 시스템 전체 통계 조회
     */
    public SystemStatisticsResponse getSystemStatistics() {
        log.debug("시스템 전체 통계 조회");

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfToday = LocalDate.now().atStartOfDay();
        LocalDateTime startOfWeek = LocalDate.now().minusWeeks(1).atStartOfDay();
        LocalDateTime startOfMonth = LocalDate.now().minusMonths(1).atStartOfDay();

        // 회원 통계
        long totalMembers = memberRepository.countAllActive();
        long adminMembers = memberRepository.countByRole(Role.ADMIN);
        long userMembers = memberRepository.countByRole(Role.USER);
        long newMembersToday = memberRepository.countSince(startOfToday);
        long newMembersThisWeek = memberRepository.countSince(startOfWeek);
        long newMembersThisMonth = memberRepository.countSince(startOfMonth);

        // 면접 통계
        long totalInterviews = interviewRepository.countAllActive();
        long readyInterviews = interviewRepository.countByStatus(InterviewStatus.READY);
        long inProgressInterviews = interviewRepository.countByStatus(InterviewStatus.IN_PROGRESS);
        long completedInterviews = interviewRepository.countByStatus(InterviewStatus.DONE);
        long newInterviewsToday = interviewRepository.countSince(startOfToday);
        long newInterviewsThisWeek = interviewRepository.countSince(startOfWeek);

        // 답변 통계
        long totalAnswers = answerRepository.countAllActive();
        Double overallAverageScore = answerRepository.findOverallAverageScore();

        // 일별 추이 (최근 7일)
        List<DailyCountResponse> dailyInterviewCounts = calculateDailyCounts(7, true);
        List<DailyCountResponse> dailyMemberCounts = calculateDailyCounts(7, false);

        return SystemStatisticsResponse.of(
                totalMembers,
                adminMembers,
                userMembers,
                newMembersToday,
                newMembersThisWeek,
                newMembersThisMonth,
                totalInterviews,
                readyInterviews,
                inProgressInterviews,
                completedInterviews,
                newInterviewsToday,
                newInterviewsThisWeek,
                totalAnswers,
                roundScore(overallAverageScore),
                dailyInterviewCounts,
                dailyMemberCounts
        );
    }

    private Interview findInterviewById(Long interviewId) {
        return interviewRepository.findByIdAndDeletedFalse(interviewId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INTERVIEW_NOT_FOUND));
    }

    /**
     * 최근 N일간 일별 카운트 계산
     */
    private List<DailyCountResponse> calculateDailyCounts(int days, boolean isInterview) {
        List<DailyCountResponse> counts = new ArrayList<>();
        LocalDate today = LocalDate.now();

        for (int i = days - 1; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            LocalDateTime startOfDay = date.atStartOfDay();
            LocalDateTime endOfDay = date.atTime(LocalTime.MAX);

            long count;
            if (isInterview) {
                count = interviewRepository.countSince(startOfDay)
                        - (i > 0 ? interviewRepository.countSince(date.plusDays(1).atStartOfDay()) : 0);
            } else {
                count = memberRepository.countSince(startOfDay)
                        - (i > 0 ? memberRepository.countSince(date.plusDays(1).atStartOfDay()) : 0);
            }

            counts.add(DailyCountResponse.of(date, Math.max(0, count)));
        }

        return counts;
    }

    private Double roundScore(Double score) {
        if (score == null) {
            return null;
        }
        return Math.round(score * 10.0) / 10.0;
    }
}
