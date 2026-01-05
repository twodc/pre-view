package com.example.pre_view.domain.interview.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Position {
    BACKEND("백엔드"),
    FRONTEND("프론트엔드"),
    FULLSTACK("풀스택"),
    DEVOPS("데브옵스"),
    DATA_ENGINEER("데이터 엔지니어"),
    AI_ML("AI/ML"),
    IOS("iOS"),
    ANDROID("Android"),
    GAME("게임");

    private final String description;
}
