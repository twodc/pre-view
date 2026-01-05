package com.example.pre_view.domain.answer.dto;

import com.example.pre_view.domain.answer.entity.Answer;
import com.example.pre_view.domain.interview.enums.InterviewPhase;
import com.example.pre_view.domain.question.dto.QuestionResponse;

public record AnswerResponse(
    Long id,
    Long questionId,
    String questionContent,
    InterviewPhase phase,
    String phaseDescription,
    String content,
    String feedback,
    Integer score,
    QuestionResponse followUpQuestion
) {
    public static AnswerResponse from(Answer answer) {
        return new AnswerResponse(
            answer.getId(),
            answer.getQuestion().getId(),
            answer.getQuestion().getContent(),
            answer.getQuestion().getPhase(),
            answer.getQuestion().getPhase().getDescription(),
            answer.getContent(),
            answer.getFeedback(),
            answer.getScore(),
            null
        );
    }

    public static AnswerResponse of(Answer answer, QuestionResponse followUp) {
        return new AnswerResponse(
            answer.getId(),
            answer.getQuestion().getId(),
            answer.getQuestion().getContent(),
            answer.getQuestion().getPhase(),
            answer.getQuestion().getPhase().getDescription(),
            answer.getContent(),
            answer.getFeedback(),
            answer.getScore(),
            followUp
        );
    }
}
