package com.example.pre_view.domain.interview.enums;

import java.util.List;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum InterviewType {
    FULL("전체 면접", List.of(
        InterviewPhase.GREETING,
        InterviewPhase.SELF_INTRO,
        InterviewPhase.PERSONALITY,
        InterviewPhase.TECHNICAL,
        InterviewPhase.CLOSING
    )),
    TECHNICAL("기술 면접", List.of(
        InterviewPhase.TECHNICAL
    )),
    PERSONALITY("인성 면접", List.of(
        InterviewPhase.PERSONALITY
    ));

    private final String description;
    private final List<InterviewPhase> phases;

    // 해당 유형에 특정 단계가 포함되는지 확인
    public boolean hasPhase(InterviewPhase phase) {
        return phases.contains(phase);
    }
}

