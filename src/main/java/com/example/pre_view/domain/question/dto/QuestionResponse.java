package com.example.pre_view.domain.question.dto;

import com.example.pre_view.domain.interview.enums.InterviewPhase;
import com.example.pre_view.domain.question.entity.Question;

public record QuestionResponse(
    Long id,
    String content,
    InterviewPhase phase,
    String phaseDescription,
    Integer sequence,
    boolean isFollowUp,
    Long parentQuestionId,
    boolean isAnswered
) {
    public static QuestionResponse from(Question question) {
        return new QuestionResponse(
            question.getId(),
            question.getContent(),
            question.getPhase(),
            question.getPhase().getDescription(),
            question.getSequence(),
            question.isFollowUp(),
            question.getParentQuestion() != null ? question.getParentQuestion().getId() : null,
            question.isAnswered()
        );
    }
}
