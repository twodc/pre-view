package com.example.pre_view.domain.interview.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.example.pre_view.common.exception.BusinessException;
import com.example.pre_view.common.exception.ErrorCode;
import com.example.pre_view.domain.interview.entity.Interview;
import com.example.pre_view.domain.interview.enums.InterviewStatus;
import com.example.pre_view.domain.interview.repository.InterviewRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 면접 상태 변경 관련 트랜잭션을 처리하는 서비스
 *
 * Spring AOP 프록시는 private 메서드에 적용되지 않으므로,
 * 별도 트랜잭션이 필요한 상태 변경 로직을 분리하였습니다.
 *
 * 특히 REQUIRES_NEW 전파 속성은 반드시 외부 빈에서 호출되어야
 * 새로운 트랜잭션이 생성됩니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InterviewStatusService {

    private final InterviewRepository interviewRepository;

    /**
     * 면접 완료 처리 (별도 트랜잭션)
     *
     * REQUIRES_NEW: 호출하는 트랜잭션과 독립적인 새 트랜잭션을 생성합니다.
     * 이를 통해 읽기 전용 트랜잭션(getInterviewResult)에서 호출해도
     * 별도의 쓰기 트랜잭션으로 상태를 변경할 수 있습니다.
     *
     * @param interviewId 면접 ID
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void completeInterviewIfNeeded(Long interviewId) {
        try {
            // 최신 상태를 다시 조회하여 낙관적 락 버전 확인
            Interview interview = interviewRepository.findByIdAndDeletedFalse(interviewId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.INTERVIEW_NOT_FOUND));

            if (interview.getStatus() != InterviewStatus.DONE) {
                interview.complete();
                log.info("면접 완료 처리 - interviewId: {}", interviewId);
            }
        } catch (org.springframework.orm.ObjectOptimisticLockingFailureException e) {
            log.warn("면접 완료 처리 중 동시성 충돌 발생 - interviewId: {}", interviewId);
            throw e;
        }
    }

    /**
     * AI 리포트 캐시 저장 (별도 트랜잭션)
     *
     * 읽기 전용 트랜잭션에서 호출되므로 별도 트랜잭션으로 캐시를 저장합니다.
     *
     * @param interviewId 면접 ID
     * @param reportJson  JSON 형태의 리포트 문자열
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void cacheAiReport(Long interviewId, String reportJson) {
        if (reportJson == null) {
            log.warn("리포트 캐싱 실패 (null) - interviewId: {}", interviewId);
            return;
        }

        try {
            Interview interview = interviewRepository.findByIdAndDeletedFalse(interviewId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.INTERVIEW_NOT_FOUND));

            interview.cacheAiReport(reportJson);
            log.info("AI 리포트 캐싱 완료 - interviewId: {}", interviewId);
        } catch (org.springframework.orm.ObjectOptimisticLockingFailureException e) {
            log.warn("리포트 캐싱 중 동시성 충돌 발생 - interviewId: {}", interviewId);
            // 캐싱 실패는 치명적이지 않으므로 예외를 던지지 않음
        }
    }
}
