package com.example.pre_view.domain.interview.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import com.example.pre_view.domain.answer.entity.Answer;
import com.example.pre_view.domain.interview.entity.Interview;
import com.example.pre_view.domain.interview.enums.InterviewPhase;
import com.example.pre_view.domain.interview.enums.InterviewType;
import com.example.pre_view.domain.interview.enums.Position;
import com.example.pre_view.domain.question.entity.Question;

public record InterviewResultResponse(
    Long interviewId,
    String title,
    InterviewType type,
    Position position,
    String positionDescription,
    List<String> techStacks,
    LocalDateTime createdAt,
    
    int totalQuestions,
    int answeredQuestions,
    Double averageScore,
    
    Map<InterviewPhase, List<QuestionAnswerDto>> questionAnswersByPhase,
    
    AiReportResponse aiReport
) {
    public record QuestionAnswerDto(
        Long questionId,
        int sequence,
        InterviewPhase phase,
        String phaseDescription,
        String question,
        String answer,
        String feedback,
        Integer score
    ) {
        public static QuestionAnswerDto of(Question question, Answer answer) {
            return new QuestionAnswerDto(
                question.getId(),
                question.getSequence(),
                question.getPhase(),
                question.getPhase().getDescription(),
                question.getContent(),
                answer != null ? answer.getContent() : null,
                answer != null ? answer.getFeedback() : null,
                answer != null ? answer.getScore() : null
            );
        }
    }

    public static InterviewResultResponse of(
            Interview interview, 
            List<Question> questions,
            List<Answer> answers, 
            AiReportResponse aiReport
    ) {
        Map<Long, Answer> answerByQuestionId = answers.stream()
                .collect(Collectors.toMap(a -> a.getQuestion().getId(), a -> a));

        int answeredCount = (int) answers.stream()
                .filter(a -> a.getContent() != null)
                .count();
        
        Double avgScore = answers.stream()
                .map(Answer::getScore)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .average()
                .orElse(0.0);
        
        Map<InterviewPhase, List<QuestionAnswerDto>> groupedByPhase = questions.stream()
                .map(q -> QuestionAnswerDto.of(q, answerByQuestionId.get(q.getId())))
                .collect(Collectors.groupingBy(QuestionAnswerDto::phase));

        return new InterviewResultResponse(
            interview.getId(),
            interview.getTitle(),
            interview.getType(),
            interview.getPosition(),
            interview.getPosition().getDescription(),
            interview.getTechStacks(),
            interview.getCreatedAt(),
            questions.size(),
            answeredCount,
            avgScore,
            groupedByPhase,
            aiReport
        );
    }
}
