package com.example.pre_view.domain.question.dto;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.example.pre_view.domain.interview.enums.InterviewPhase;
import com.example.pre_view.domain.question.entity.Question;

public record QuestionListResponse(
    Long interviewId,
    int totalCount,
    int mainQuestionCount,
    int followUpCount,
    Map<InterviewPhase, List<QuestionResponse>> questionsByPhase
) {
    public static QuestionListResponse of(Long interviewId, List<Question> questions) {
        Map<InterviewPhase, List<QuestionResponse>> grouped = questions.stream()
            .map(QuestionResponse::from)
            .collect(Collectors.groupingBy(QuestionResponse::phase));

        int followUps = (int) questions.stream().filter(Question::isFollowUp).count();

        return new QuestionListResponse(
            interviewId,
            questions.size(),
            questions.size() - followUps,
            followUps,
            grouped
        );
    }
}
