package com.example.pre_view.domain.answer.entity;



import com.example.pre_view.common.BaseEntity;
import com.example.pre_view.domain.question.entity.Question;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
public class Answer extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id")
    private Question question;

    @Column(length = 5000)
    private String content;

    @Column(length = 5000)
    private String feedback;

    private Integer score;

    @Builder
    public Answer(Question question, String content, String feedback, Integer score) {
        this.question = question;
        this.content = content;
        this.feedback = feedback;
        this.score = score;
    }
}
