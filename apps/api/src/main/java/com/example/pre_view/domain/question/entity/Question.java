package com.example.pre_view.domain.question.entity;

import com.example.pre_view.common.BaseEntity;
import com.example.pre_view.domain.interview.entity.Interview;
import com.example.pre_view.domain.interview.enums.InterviewPhase;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Question extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String content;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "interview_id")
    private Interview interview;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InterviewPhase phase;

    private Integer sequence;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_question_id")
    private Question parentQuestion;

    private boolean isFollowUp;

    @Column(nullable = false)
    private boolean isAnswered = false;

    @Builder
    public Question(String content, Interview interview, InterviewPhase phase, 
                    Integer sequence, Question parentQuestion, boolean isFollowUp, Boolean isAnswered) {
        this.content = content;
        this.interview = interview;
        this.phase = phase;
        this.sequence = sequence;
        this.parentQuestion = parentQuestion;
        this.isFollowUp = isFollowUp;
        this.isAnswered = isAnswered != null ? isAnswered : false;
    }

    public void markAsAnswered() {
        this.isAnswered = true;
    }
}
