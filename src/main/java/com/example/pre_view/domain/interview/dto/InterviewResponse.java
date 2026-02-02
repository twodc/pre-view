package com.example.pre_view.domain.interview.dto;

import java.util.ArrayList;
import java.util.List;

import com.example.pre_view.domain.interview.entity.Interview;
import com.example.pre_view.domain.interview.enums.ExperienceLevel;
import com.example.pre_view.domain.interview.enums.InterviewPhase;
import com.example.pre_view.domain.interview.enums.InterviewStatus;
import com.example.pre_view.domain.interview.enums.InterviewType;
import com.example.pre_view.domain.interview.enums.Position;

public record InterviewResponse(
    Long id,
    String title,
    InterviewType type,
    String typeDescription,
    Position position,
    String positionDescription,
    ExperienceLevel level,
    String levelDescription,
    List<String> techStacks,
    InterviewStatus status,
    InterviewPhase currentPhase,
    String currentPhaseDescription,
    Integer totalQuestions,
    Boolean hasResume,
    Boolean hasPortfolio
) {
    public static InterviewResponse from(Interview interview) {
        Boolean hasResume = interview.getResumeText() != null 
                && !interview.getResumeText().isBlank();
        
        Boolean hasPortfolio = interview.getPortfolioText() != null 
                && !interview.getPortfolioText().isBlank();
        
        return new InterviewResponse(
            interview.getId(), 
            interview.getTitle(),
            interview.getType(),
            interview.getType().getDescription(),
            interview.getPosition(),
            interview.getPosition().getDescription(),
            interview.getLevel(),
            interview.getLevel().getDescription(),
            interview.getTechStacks() != null ? new ArrayList<>(interview.getTechStacks()) : List.of(),
            interview.getStatus(),
            interview.getCurrentPhase(),
            interview.getCurrentPhase() != null ? interview.getCurrentPhase().getDescription() : null,
            interview.getTotalQuestions(),
            hasResume,
            hasPortfolio
        );
    }
}
