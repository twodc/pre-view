package com.example.pre_view.domain.interview.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum InterviewPhase {
    GREETING("인사", 1, 1, false, true),  // 템플릿 질문
    SELF_INTRO("자기소개", 2, 2, true, true),  // 템플릿 질문
    PERSONALITY("인성/태도", 3, 4, true, false),  // AI 생성 질문 (이력서 기반)
    TECHNICAL("기술", 4, 5, true, false),  // AI 생성 질문 (이력서/포트폴리오/기술스택 기반)
    CLOSING("마무리", 5, 2, false, true);  // 템플릿 질문

    private final String description;
    private final int order;
    private final int defaultQuestionCount;
    private final boolean allowFollowUp;
    private final boolean isTemplateQuestion;  // 템플릿 질문 여부 (true: 고정 질문, false: AI 실시간 생성)

    /**
     * 이 단계가 고정 템플릿 질문인지 확인
     * @return true: 고정 템플릿 질문, false: AI로 실시간 생성
     */
    public boolean isTemplate() {
        return isTemplateQuestion;
    }
}
