package com.example.pre_view.domain.question.service;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.stereotype.Service;

import com.example.pre_view.common.exception.BusinessException;
import com.example.pre_view.common.exception.ErrorCode;
import com.example.pre_view.domain.interview.enums.InterviewPhase;

/**
 * 고정 템플릿 질문을 제공하는 서비스
 * 인사/자기소개, 마무리 단계의 질문은 미리 정의된 템플릿을 사용합니다.
 */
@Service
public class QuestionTemplateService {

    /**
     * 단계별 고정 템플릿 질문을 반환
     * 
     * @param phase 면접 단계 (OPENING 또는 CLOSING)
     * @return 해당 단계의 템플릿 질문 목록
     */
    public List<String> getTemplateQuestions(InterviewPhase phase) {
        return switch (phase) {
            case OPENING -> {
                // 그룹 1: 인사 + 자기소개
                List<String> selfIntroQuestions = List.of(
                        "안녕하세요. 오늘 면접을 위해 시간 내어주셔서 감사합니다. 먼저 간단하게 자기소개 부탁드립니다.",
                        "안녕하세요. 먼저 본인을 소개해주시겠어요?",
                        "안녕하세요. 간단하게 자기소개와 함께 본인의 강점을 말씀해주시겠어요?");

                // 그룹 2: 지원 동기
                List<String> motivationQuestions = List.of(
                        "지원하신 포지션에 대한 관심과 지원 동기를 말씀해주시겠어요?",
                        "이 포지션에 지원하게 된 이유와 관심을 가지게 된 계기를 말씀해주시겠어요?",
                        "저희 회사와 이 직무에 지원하신 이유를 말씀해주시겠어요?");

                // 그룹 3: 기술/프로젝트 소개
                List<String> techProjectQuestions = List.of(
                        "가장 자신 있는 기술이 무엇인지, 또는 가장 기억에 남는 프로젝트 하나를 소개해주시겠어요?",
                        "본인이 가장 자신 있다고 생각하는 기술 스택과 그 이유를 말씀해주시겠어요?",
                        "지금까지 진행한 프로젝트 중 가장 인상 깊었던 프로젝트를 하나 소개해주시겠어요?");

                // 각 그룹에서 랜덤으로 1개씩 선택
                List<String> selectedQuestions = List.of(
                        selectRandom(selfIntroQuestions),
                        selectRandom(motivationQuestions),
                        selectRandom(techProjectQuestions));

                yield selectedQuestions;
            }

            case CLOSING -> {
                // 그룹 1: 입사 후 목표
                List<String> goalQuestions = List.of(
                        "입사 후 어떤 목표를 가지고 계신지 말씀해주시겠어요?",
                        "입사 후 1년, 3년 후의 목표나 비전을 말씀해주시겠어요?",
                        "저희 회사에서 이루고 싶은 목표와 기여하고 싶은 부분을 말씀해주시겠어요?");

                // 그룹 2: 마지막 하고 싶은 말
                List<String> lastAppealQuestions = List.of(
                        "네, 잘 들었습니다. 마지막으로 못다 한 이야기가 있거나 꼭 하고 싶은 말씀이 있다면 편하게 해주세요.",
                        "면접을 마치기 전에, 본인을 어필할 수 있는 마지막 기회를 드리고 싶습니다. 하시고 싶은 말씀이 있으신가요?",
                        "긴 시간 고생하셨습니다. 끝으로 하고 싶은 말씀이 있다면 듣고 마무리하겠습니다.");

                // 각 그룹에서 랜덤으로 1개씩 선택
                List<String> selectedQuestions = List.of(
                        selectRandom(goalQuestions),
                        selectRandom(lastAppealQuestions));

                yield selectedQuestions;
            }

            default -> throw new BusinessException(ErrorCode.INVALID_QUESTION_PHASE);
        };
    }

    /**
     * 질문 리스트에서 랜덤으로 1개를 선택하는 헬퍼 메서드
     * 
     * @param questions 질문 리스트
     * @return 랜덤으로 선택된 질문
     */
    private String selectRandom(List<String> questions) {
        return questions.get(ThreadLocalRandom.current().nextInt(questions.size()));
    }
}
