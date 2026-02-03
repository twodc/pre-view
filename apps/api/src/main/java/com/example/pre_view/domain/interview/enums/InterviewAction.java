package com.example.pre_view.domain.interview.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * AI 면접 에이전트의 행동 타입
 */
@Getter
@RequiredArgsConstructor
public enum InterviewAction {
    GENERATE_QUESTION("질문 생성"),
    NEXT_PHASE("다음 단계로 이동");

    private final String description;
}

