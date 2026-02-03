package com.example.pre_view.domain.interview.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ExperienceLevel {
    NEWCOMER("신입", 0),
    JUNIOR("주니어", 1),
    MID("미들", 2),
    SENIOR("시니어", 3);

    private final String description;
    private final int level;
}

