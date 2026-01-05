package com.example.pre_view.domain.question.service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.pre_view.common.exception.BusinessException;
import com.example.pre_view.common.exception.ErrorCode;
import com.example.pre_view.domain.answer.repository.AnswerRepository;
import com.example.pre_view.domain.interview.dto.AiInterviewAgentResponse;
import com.example.pre_view.domain.interview.entity.Interview;
import com.example.pre_view.domain.interview.enums.InterviewAction;
import com.example.pre_view.domain.interview.enums.InterviewPhase;
import com.example.pre_view.domain.interview.repository.InterviewRepository;
import com.example.pre_view.domain.interview.service.AiInterviewService;
import com.example.pre_view.domain.question.dto.QuestionListResponse;
import com.example.pre_view.domain.question.entity.Question;
import com.example.pre_view.domain.question.repository.QuestionRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 질문 관련 비즈니스 로직을 처리하는 서비스
 * - 템플릿 질문 생성
 * - AI 에이전트를 통한 질문 생성
 * - 질문 목록 조회
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QuestionService {

    private final QuestionRepository questionRepository;
    private final InterviewRepository interviewRepository;
    private final AnswerRepository answerRepository;
    private final AiInterviewService aiInterviewService;

    /**
     * 면접 시작 시 템플릿 질문들을 생성합니다.
     * 인사/자기소개, 마무리 단계의 고정 질문들을 미리 생성합니다.
     * 
     * @param interview 면접 엔티티
     * @return 생성된 템플릿 질문 목록
     */
    @Transactional
    public List<Question> createTemplateQuestions(Interview interview) {
        log.debug("템플릿 질문 생성 시작 - interviewId: {}", interview.getId());

        List<Question> templateQuestions = new ArrayList<>();
        AtomicInteger sequence = new AtomicInteger(1);

        // 면접 타입에 따른 단계별로 템플릿 질문 생성
        for (InterviewPhase phase : interview.getType().getPhases()) {
            if (phase.isTemplate()) {
                log.debug("템플릿 질문 생성 - interviewId: {}, phase: {}", interview.getId(), phase);
                List<String> templateQuestionTexts = getTemplateQuestions(phase);

                List<Question> phaseQuestions = templateQuestionTexts.stream()
                        .map(text -> Question.builder()
                                .content(text)
                                .interview(interview)
                                .phase(phase)
                                .sequence(sequence.getAndIncrement())
                                .isFollowUp(false)
                                .build())
                        .toList();

                templateQuestions.addAll(phaseQuestions);
                log.debug("템플릿 질문 생성 완료 - interviewId: {}, phase: {}, 질문 수: {}",
                        interview.getId(), phase, phaseQuestions.size());
            }
        }

        List<Question> savedQuestions = questionRepository.saveAll(templateQuestions);
        log.info("템플릿 질문 생성 완료 - interviewId: {}, 총 질문 수: {}",
                interview.getId(), savedQuestions.size());

        return savedQuestions;
    }

    /**
     * 면접의 질문 목록을 조회합니다.
     * 
     * @param interviewId 면접 ID
     * @return 질문 목록 응답 DTO
     */
    @Transactional
    public QuestionListResponse getQuestions(Long interviewId) {
        log.debug("질문 목록 조회 시작 - interviewId: {}", interviewId);

        Interview interview = interviewRepository.findByIdAndDeletedFalse(interviewId)
                .orElseThrow(() -> {
                    log.warn("면접을 찾을 수 없음 - interviewId: {}", interviewId);
                    return new BusinessException(ErrorCode.INTERVIEW_NOT_FOUND);
                });

        List<Question> existingQuestions = questionRepository.findByInterviewIdOrderBySequence(interview.getId());

        log.debug("질문 목록 조회 완료 - interviewId: {}, 총 질문 수: {}", interviewId, existingQuestions.size());
        return QuestionListResponse.of(interviewId, existingQuestions);
    }

    /**
     * AI 에이전트를 호출하여 다음 단계의 첫 질문을 생성합니다.
     * Opening 단계에서 Technical 단계로 전환될 때, Opening의 마지막 답변을 브릿지 답변으로 사용합니다.
     * 
     * @param interview     면접 엔티티
     * @param previousPhase 이전 단계 (Opening 등)
     * @param nextPhase     다음 단계 (Technical 또는 Personality)
     */
    @Transactional
    public void generateFirstQuestionByAgent(Interview interview, InterviewPhase previousPhase, InterviewPhase nextPhase) {
        try {
            log.info("AI 에이전트를 통한 첫 질문 생성 시작 - interviewId: {}, phase: {}", interview.getId(), nextPhase);

            // 브릿지 답변 가져오기 (Opening 단계의 마지막 질문에 대한 답변)
            String bridgeAnswer = null;
            if (previousPhase == InterviewPhase.OPENING) {
                bridgeAnswer = answerRepository.findByInterviewIdWithQuestion(interview.getId()).stream()
                        .filter(a -> a.getQuestion().getPhase() == InterviewPhase.OPENING)
                        .filter(a -> !a.getQuestion().isFollowUp())
                        .max((a1, a2) -> {
                            Integer seq1 = a1.getQuestion().getSequence();
                            Integer seq2 = a2.getQuestion().getSequence();
                            if (seq1 == null) return -1;
                            if (seq2 == null) return 1;
                            return seq1.compareTo(seq2);
                        })
                        .map(com.example.pre_view.domain.answer.entity.Answer::getContent)
                        .orElse(null);
            }

            // 면접 컨텍스트 생성 (Interview 엔티티의 도메인 메서드 활용)
            String interviewContext = interview.buildContext();

            // 에이전트 호출
            AiInterviewAgentResponse agentResponse = aiInterviewService.processInterviewStep(
                    nextPhase,
                    bridgeAnswer, // Opening의 마지막 답변
                    interviewContext,
                    interview.getResumeText(),
                    interview.getPortfolioText(),
                    List.of(), // 첫 질문이므로 이전 질문 없음
                    List.of(), // 첫 질문이므로 이전 답변 없음
                    0 // 꼬리 질문 횟수 0
            );

            // 에이전트 응답 처리 (null 체크 포함)
            String questionContent = null;
            if (agentResponse != null
                    && agentResponse.action() == InterviewAction.GENERATE_QUESTION
                    && agentResponse.message() != null) {
                questionContent = agentResponse.message();
                log.info("AI 에이전트가 질문 생성 - interviewId: {}, phase: {}", interview.getId(), nextPhase);
            } else {
                // Fallback: Agent가 null 반환하거나 질문을 생성하지 않은 경우
                log.warn("AI 에이전트가 질문 생성하지 않음, Fallback 질문 생성 - interviewId: {}, phase: {}, agentResponse: {}",
                        interview.getId(), nextPhase, agentResponse != null ? agentResponse.action() : "null");
                questionContent = generateFallbackQuestion(interview, previousPhase, nextPhase);
            }

            // 질문 저장
            if (questionContent != null) {
                int nextSequence = getNextSequence(interview.getId());
                Question aiQuestion = Question.builder()
                        .content(questionContent)
                        .interview(interview)
                        .phase(nextPhase)
                        .sequence(nextSequence)
                        .isFollowUp(false)
                        .build();

                questionRepository.save(aiQuestion);
                log.info("첫 질문 생성 완료 - interviewId: {}, phase: {}, questionId: {}",
                        interview.getId(), nextPhase, aiQuestion.getId());
            }
        } catch (Exception e) {
            log.error("AI 에이전트를 통한 첫 질문 생성 실패, Fallback 질문 생성 - interviewId: {}, phase: {}",
                    interview.getId(), nextPhase, e);

            // 예외 발생 시에도 Fallback 질문 생성
            String fallbackQuestion = generateFallbackQuestion(interview, previousPhase, nextPhase);
            int nextSequence = getNextSequence(interview.getId());

            Question fallbackQuestionEntity = Question.builder()
                    .content(fallbackQuestion)
                    .interview(interview)
                    .phase(nextPhase)
                    .sequence(nextSequence)
                    .isFollowUp(false)
                    .build();

            questionRepository.save(fallbackQuestionEntity);
            log.info("Fallback 질문 저장 완료 - interviewId: {}, phase: {}", interview.getId(), nextPhase);
        }
    }

    /**
     * 단계별 고정 템플릿 질문을 반환
     * 
     * @param phase 면접 단계 (OPENING 또는 CLOSING)
     * @return 해당 단계의 템플릿 질문 목록
     */
    private List<String> getTemplateQuestions(InterviewPhase phase) {
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

    private int getNextSequence(Long interviewId) {
        return questionRepository.countByInterviewId(interviewId) + 1;
    }

    /**
     * 현재 질문의 꼬리 질문 체인 깊이를 계산합니다.
     * 예: 주 질문 → 꼬리1 → 꼬리2 에서 꼬리2의 깊이는 2
     *
     * @param question 현재 질문
     * @return 꼬리 질문 깊이 (주 질문은 0)
     */
    public int calculateFollowUpDepth(Question question) {
        int depth = 0;
        Question current = question;
        while (current.getParentQuestion() != null) {
            depth++;
            current = current.getParentQuestion();
        }
        return depth;
    }

    /**
     * 면접의 특정 단계에서 이전 질문 목록을 수집합니다.
     *
     * @param interview 면접 엔티티
     * @param phase 면접 단계
     * @return 해당 단계의 질문 내용 목록
     */
    public List<String> getPreviousQuestions(Interview interview, InterviewPhase phase) {
        return questionRepository.findByInterviewIdAndPhaseOrderBySequence(interview.getId(), phase)
                .stream()
                .filter(Question::isAnswered)
                .map(Question::getContent)
                .toList();
    }

    /**
     * 면접의 특정 단계에서 이전 답변 목록을 수집합니다.
     *
     * @param interview 면접 엔티티
     * @param phase 면접 단계
     * @return 해당 단계의 답변 내용 목록
     */
    public List<String> getPreviousAnswers(Interview interview, InterviewPhase phase) {
        return answerRepository.findByInterviewIdWithQuestion(interview.getId())
                .stream()
                .filter(a -> a.getQuestion().getPhase() == phase)
                .map(com.example.pre_view.domain.answer.entity.Answer::getContent)
                .toList();
    }

    /**
     * Agent가 질문을 생성하지 않았을 때 Fallback 질문을 생성합니다.
     *
     * @param interview     면접 엔티티
     * @param previousPhase 이전 단계 (nullable, 면접 시작 시 null)
     * @param nextPhase     다음 단계
     * @return Fallback 질문 내용
     */
    private String generateFallbackQuestion(Interview interview, InterviewPhase previousPhase, InterviewPhase nextPhase) {
        log.debug("Fallback 질문 생성 시작 - interviewId: {}, previousPhase: {}, nextPhase: {}",
                interview.getId(), previousPhase, nextPhase);

        return getDefaultFallbackQuestion(nextPhase);
    }

    /**
     * 단계별 기본 Fallback 질문 반환
     */
    private String getDefaultFallbackQuestion(InterviewPhase phase) {
        return switch (phase) {
            case TECHNICAL -> selectRandom(getTechnicalFallbackQuestions());
            case PERSONALITY -> selectRandom(getPersonalityFallbackQuestions());
            default -> "지금까지의 경험 중 가장 도전적이었던 상황과 그것을 어떻게 극복했는지 말씀해주세요.";
        };
    }

    /**
     * 기술 면접 Fallback 질문 목록 (기본 CS 질문)
     */
    private List<String> getTechnicalFallbackQuestions() {
        return List.of(
                "객체지향 프로그래밍의 4가지 특징(캡슐화, 상속, 다형성, 추상화)에 대해 설명해주시고, 실제로 적용해본 경험이 있다면 말씀해주세요.",
                "RESTful API의 개념과 설계 원칙에 대해 설명해주시고, 실제 프로젝트에서 어떻게 적용하셨는지 말씀해주세요.",
                "데이터베이스 인덱스의 동작 원리와 장단점에 대해 설명해주시고, 인덱스를 설계할 때 고려했던 점이 있다면 말씀해주세요."
        );
    }

    /**
     * 인성 면접 Fallback 질문 목록
     */
    private List<String> getPersonalityFallbackQuestions() {
        return List.of(
                "팀 프로젝트에서 의견 충돌이 발생했을 때 어떻게 해결하셨는지 구체적인 경험을 말씀해주세요.",
                "프로젝트 진행 중 예상치 못한 문제가 발생했을 때 어떻게 대처하셨는지 경험을 말씀해주세요.",
                "본인의 가장 큰 강점과 약점은 무엇이라고 생각하시나요? 약점을 극복하기 위해 어떤 노력을 하고 계신가요?"
        );
    }
}
