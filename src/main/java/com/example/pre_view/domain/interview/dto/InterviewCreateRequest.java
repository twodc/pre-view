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

    /**
     * 면접 엔티티로 변환합니다.
     *
     * @param memberId 면접을 생성하는 사용자의 ID
     * @return Interview 엔티티
     */
    public Interview toEntity(Long memberId) {
        return Interview.builder()
            .memberId(memberId)
            .title(title)
            .type(type)
            .position(position)
            .level(level)
            .techStacks(techStacks)
            .status(InterviewStatus.READY)
            .build();
    }
}
