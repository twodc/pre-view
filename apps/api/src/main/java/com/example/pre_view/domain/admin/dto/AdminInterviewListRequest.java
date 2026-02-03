package com.example.pre_view.domain.admin.dto;

import com.example.pre_view.domain.interview.enums.InterviewStatus;

/**
 * 관리자용 면접 목록 필터 요청 DTO
 */
public record AdminInterviewListRequest(
        Long memberId,
        InterviewStatus status
) {
    public static AdminInterviewListRequest of(Long memberId, InterviewStatus status) {
        return new AdminInterviewListRequest(memberId, status);
    }
}
