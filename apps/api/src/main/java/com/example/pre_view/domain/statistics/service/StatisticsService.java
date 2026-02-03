package com.example.pre_view.domain.statistics.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.pre_view.domain.answer.repository.AnswerRepository;
import com.example.pre_view.domain.interview.entity.Interview;
import com.example.pre_view.domain.interview.enums.InterviewPhase;
import com.example.pre_view.domain.interview.enums.InterviewStatus;
import com.example.pre_view.domain.interview.repository.InterviewRepository;
import com.example.pre_view.domain.statistics.dto.DashboardSummaryResponse;
import com.example.pre_view.domain.statistics.dto.PhasePerformanceResponse;
import com.example.pre_view.domain.statistics.dto.RecentInterviewSummaryResponse;
import com.example.pre_view.domain.statistics.dto.ScoreTrendResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 사용자 통계 서비스
 *
 * 사용자의 면접 통계를 계산하고 대시보드 데이터를 제공합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StatisticsService {

    private final InterviewRepository interviewRepository;
    private final AnswerRepository answerRepository;

    /**
     * 대시보드 요약 정보 조회
     */
    public DashboardSummaryResponse getDashboardSummary(Long memberId) {
        log.debug("대시보드 요약 조회 - memberId: {}", memberId);

        int totalInterviews = interviewRepository.countByMemberId(memberId);
        int completedInterviews = interviewRepository.countByMemberIdAndStatus(memberId, InterviewStatus.DONE);
        int inProgressInterviews = interviewRepository.countByMemberIdAndStatus(memberId, InterviewStatus.IN_PROGRESS);

        Double averageScore = answerRepository.findAverageScoreByMemberId(memberId);
        Double technicalScore = answerRepository.findAverageScoreByMemberIdAndPhase(memberId, InterviewPhase.TECHNICAL);
        Double personalityScore = answerRepository.findAverageScoreByMemberIdAndPhase(memberId, InterviewPhase.PERSONALITY);

        return DashboardSummaryResponse.of(
                totalInterviews,
                completedInterviews,
                inProgressInterviews,
                roundScore(averageScore),
                roundScore(technicalScore),
                roundScore(personalityScore)
        );
    }

    /**
     * 단계별 성과 조회
     */
    public List<PhasePerformanceResponse> getPhasePerformance(Long memberId) {
        log.debug("단계별 성과 조회 - memberId: {}", memberId);

        List<PhasePerformanceResponse> performances = new ArrayList<>();

        // TECHNICAL 단계
        Double technicalScore = answerRepository.findAverageScoreByMemberIdAndPhase(memberId, InterviewPhase.TECHNICAL);
        int technicalCount = answerRepository.countByMemberIdAndPhase(memberId, InterviewPhase.TECHNICAL);
        performances.add(PhasePerformanceResponse.of(InterviewPhase.TECHNICAL, roundScore(technicalScore), technicalCount));

        // PERSONALITY 단계
        Double personalityScore = answerRepository.findAverageScoreByMemberIdAndPhase(memberId, InterviewPhase.PERSONALITY);
        int personalityCount = answerRepository.countByMemberIdAndPhase(memberId, InterviewPhase.PERSONALITY);
        performances.add(PhasePerformanceResponse.of(InterviewPhase.PERSONALITY, roundScore(personalityScore), personalityCount));

        return performances;
    }

    /**
     * 점수 추이 조회 (월별/주별)
     *
     * @param memberId 회원 ID
     * @param period "monthly" 또는 "weekly"
     * @return 점수 추이 목록
     */
    public List<ScoreTrendResponse> getScoreTrends(Long memberId, String period) {
        log.debug("점수 추이 조회 - memberId: {}, period: {}", memberId, period);

        LocalDateTime startDate;
        if ("weekly".equalsIgnoreCase(period)) {
            startDate = LocalDateTime.now().minusWeeks(12); // 최근 12주
        } else {
            startDate = LocalDateTime.now().minusMonths(6); // 최근 6개월
        }

        List<Interview> interviews = interviewRepository.findCompletedByMemberIdSince(memberId, startDate);

        if (interviews.isEmpty()) {
            return List.of();
        }

        // 면접별 평균 점수 계산
        Map<Long, Double> interviewScores = interviews.stream()
                .collect(Collectors.toMap(
                        Interview::getId,
                        i -> answerRepository.findAverageScoreByInterviewId(i.getId())
                ));

        List<ScoreTrendResponse> trends = new ArrayList<>();

        if ("weekly".equalsIgnoreCase(period)) {
            // 주별 그룹핑
            Map<LocalDate, List<Interview>> weeklyGroups = interviews.stream()
                    .collect(Collectors.groupingBy(i -> {
                        LocalDate date = i.getCreatedAt().toLocalDate();
                        return date.minusDays(date.getDayOfWeek().getValue() - 1); // 주의 시작일 (월요일)
                    }));

            weeklyGroups.forEach((weekStart, weekInterviews) -> {
                double avgScore = weekInterviews.stream()
                        .map(i -> interviewScores.getOrDefault(i.getId(), 0.0))
                        .filter(score -> score != null && score > 0)
                        .mapToDouble(Double::doubleValue)
                        .average()
                        .orElse(0.0);

                trends.add(ScoreTrendResponse.of(
                        weekStart,
                        weekStart + " ~ " + weekStart.plusDays(6),
                        roundScore(avgScore),
                        weekInterviews.size()
                ));
            });
        } else {
            // 월별 그룹핑
            Map<YearMonth, List<Interview>> monthlyGroups = interviews.stream()
                    .collect(Collectors.groupingBy(i -> YearMonth.from(i.getCreatedAt())));

            monthlyGroups.forEach((yearMonth, monthInterviews) -> {
                double avgScore = monthInterviews.stream()
                        .map(i -> interviewScores.getOrDefault(i.getId(), 0.0))
                        .filter(score -> score != null && score > 0)
                        .mapToDouble(Double::doubleValue)
                        .average()
                        .orElse(0.0);

                trends.add(ScoreTrendResponse.of(
                        yearMonth.atDay(1),
                        yearMonth.toString(),
                        roundScore(avgScore),
                        monthInterviews.size()
                ));
            });
        }

        // 날짜순 정렬
        trends.sort((a, b) -> a.date().compareTo(b.date()));
        return trends;
    }

    /**
     * 최근 면접 요약 조회
     */
    public List<RecentInterviewSummaryResponse> getRecentInterviews(Long memberId, int limit) {
        log.debug("최근 면접 조회 - memberId: {}, limit: {}", memberId, limit);

        List<Interview> interviews = interviewRepository.findRecentByMemberId(memberId, PageRequest.of(0, limit));

        return interviews.stream()
                .map(interview -> {
                    Double avgScore = answerRepository.findAverageScoreByInterviewId(interview.getId());
                    return RecentInterviewSummaryResponse.of(interview, roundScore(avgScore));
                })
                .toList();
    }

    /**
     * 점수 소수점 첫째 자리까지 반올림
     */
    private Double roundScore(Double score) {
        if (score == null) {
            return null;
        }
        return Math.round(score * 10.0) / 10.0;
    }
}
