package com.example.pre_view.domain.interview.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum InterviewPhase {
    OPENING("인사/자기소개", 1, 3, false, true),  // 템플릿 질문
    TECHNICAL("기술", 2, 5, true, false),  // AI 생성 질문 (이력서/포트폴리오/기술스택 기반)
    PERSONALITY("인성/태도", 3, 4, true, false),  // AI 생성 질문 (이력서/기술스택 기반)
    CLOSING("마무리", 4, 2, false, true);  // 템플릿 질문

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
