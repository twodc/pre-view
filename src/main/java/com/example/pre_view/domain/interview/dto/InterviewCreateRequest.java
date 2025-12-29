package com.example.pre_view.domain.interview.dto;

import java.util.List;

import com.example.pre_view.domain.interview.entity.Interview;
import com.example.pre_view.domain.interview.enums.ExperienceLevel;
import com.example.pre_view.domain.interview.enums.InterviewStatus;
import com.example.pre_view.domain.interview.enums.InterviewType;
import com.example.pre_view.domain.interview.enums.Position;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record InterviewCreateRequest(
    @NotBlank(message = "제목은 필수 입력 값입니다.")
    String title,

    @NotNull(message = "면접 유형은 필수 선택 값입니다.")
    InterviewType type,
    
    @NotNull(message = "포지션은 필수 선택 값입니다.")
    Position position,
    
    @NotNull(message = "경력 수준은 필수 선택 값입니다.")
    ExperienceLevel level,
    
    List<String> techStacks
) {

    public Interview toEntity() {
        return Interview.builder()
            .title(title)
            .type(type)
            .position(position)
            .level(level)
            .techStacks(techStacks)
            .status(InterviewStatus.READY)
            .build();
    } 
}
