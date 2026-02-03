package com.example.pre_view.domain.interview.dto;

import com.example.pre_view.domain.interview.enums.InterviewAction;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * AI 면접 에이전트의 응답 DTO
 * 
 * AI가 사용자의 답변을 평가하고, 다음 질문을 생성할지 또는 다음 단계로 넘어갈지 판단한 결과를 담습니다.
 */
public record AiInterviewAgentResponse(
    /**
     * AI의 사고 과정 (Reasoning)
     * 현재 상황에 대한 AI의 생각과 판단 근거를 한국어로 설명
     */
    String thought,
    
    /**
     * AI가 결정한 행동
     * GENERATE_QUESTION: 새로운 질문 생성
     * NEXT_PHASE: 다음 단계로 이동
     */
    @JsonProperty("action")
    InterviewAction action,
    
    /**
     * 사용자에게 보여줄 질문 내용
     * action이 GENERATE_QUESTION일 때는 질문 텍스트가 들어감
     * action이 NEXT_PHASE일 때는 null 가능
     */
    String message,
    
    /**
     * 방금 받은 답변에 대한 평가 (선택적)
     * 점수, 키워드, 간단한 코멘트 등
     */
    String evaluation
) {
}

