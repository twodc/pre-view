package com.example.pre_view.domain.interview.entity;

import java.util.ArrayList;
import java.util.List;

import com.example.pre_view.common.BaseEntity;
import com.example.pre_view.domain.interview.enums.ExperienceLevel;
import com.example.pre_view.domain.interview.enums.InterviewPhase;
import com.example.pre_view.domain.interview.enums.InterviewStatus;
import com.example.pre_view.domain.interview.enums.InterviewType;
import com.example.pre_view.domain.interview.enums.Position;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Interview extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InterviewType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Position position;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExperienceLevel level;

    @ElementCollection
    @CollectionTable(name = "interview_tech_stack", joinColumns = @JoinColumn(name = "interview_id"))
    @Column(name = "tech_stack")
    private List<String> techStacks = new ArrayList<>();

    @Column(columnDefinition = "TEXT")
    private String resumeText;

    @Column(columnDefinition = "TEXT")
    private String portfolioText;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InterviewStatus status;

    @Enumerated(EnumType.STRING)
    private InterviewPhase currentPhase;

    private Integer totalQuestions;

    @Builder
    public Interview(String title, InterviewType type, Position position, ExperienceLevel level,
                     List<String> techStacks, String resumeText, String portfolioText,
                     InterviewStatus status, Integer totalQuestions) {
        this.title = title;
        this.type = type != null ? type : InterviewType.TECHNICAL;
        this.position = position;
        this.level = level;
        this.techStacks = techStacks != null ? techStacks : new ArrayList<>();
        this.resumeText = resumeText;
        this.portfolioText = portfolioText;
        this.status = status != null ? status : InterviewStatus.READY;
        this.currentPhase = null;
        this.totalQuestions = totalQuestions;
    }

    /**
     * 이력서 텍스트 업데이트
     */
    public void updateResumeText(String resumeText) {
        this.resumeText = resumeText;
    }

    /**
     * 포트폴리오 텍스트 업데이트
     */
    public void updatePortfolioText(String portfolioText) {
        this.portfolioText = portfolioText;
    }

    public void start() {
        this.status = InterviewStatus.IN_PROGRESS;
        this.currentPhase = this.type.getPhases().get(0);
    }

    public void nextPhase() {
        List<InterviewPhase> phases = this.type.getPhases();
        int currentIndex = phases.indexOf(this.currentPhase);
        if (currentIndex < phases.size() - 1) {
            this.currentPhase = phases.get(currentIndex + 1);
        }
    }

    public void complete() {
        this.status = InterviewStatus.DONE;
    }

    public void cancel() {
        this.status = InterviewStatus.CANCELLED;
    }

    public boolean isLastPhase() {
        List<InterviewPhase> phases = this.type.getPhases();
        return this.currentPhase == phases.get(phases.size() - 1);
    }
}
