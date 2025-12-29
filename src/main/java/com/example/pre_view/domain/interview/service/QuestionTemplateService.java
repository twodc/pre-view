package com.example.pre_view.domain.interview.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.example.pre_view.common.exception.BusinessException;
import com.example.pre_view.common.exception.ErrorCode;
import com.example.pre_view.domain.interview.enums.InterviewPhase;

/**
 * 고정 템플릿 질문을 제공하는 서비스
 * 인사, 자기소개, 마무리 단계의 질문은 미리 정의된 템플릿을 사용합니다.
 */
@Service
public class QuestionTemplateService {

    /**
     * 단계별 고정 템플릿 질문을 반환
     * @param phase 면접 단계
     * @return 해당 단계의 템플릿 질문 목록
     */
    public List<String> getTemplateQuestions(InterviewPhase phase) {
        return switch (phase) {
            case GREETING -> List.of(
                "안녕하세요. 오늘 면접을 위해 시간 내어주셔서 감사합니다. 오늘 컨디션은 어떠신가요?"
            );
            
            case SELF_INTRO -> List.of(
                "먼저 간단하게 자기소개를 부탁드립니다.",
                "지원하신 포지션에 대한 관심과 지원 동기를 말씀해주시겠어요?"
            );
            
            case CLOSING -> List.of(
                "마지막으로 저희에게 궁금한 점이나 질문이 있으신가요?",
                "입사 후 어떤 목표를 가지고 계신지 말씀해주시겠어요?"
            );
            
            default -> throw new BusinessException(ErrorCode.INVALID_QUESTION_PHASE);
        };
    }
}
