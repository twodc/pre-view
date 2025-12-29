package com.example.pre_view.domain.answer.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.pre_view.common.exception.BusinessException;
import com.example.pre_view.common.exception.ErrorCode;
import com.example.pre_view.domain.answer.dto.AiFeedbackResponse;
import com.example.pre_view.domain.answer.dto.AnswerCreateRequest;
import com.example.pre_view.domain.answer.dto.AnswerResponse;
import com.example.pre_view.domain.answer.entity.Answer;
import com.example.pre_view.domain.answer.repository.AnswerRepository;
import com.example.pre_view.domain.interview.service.AiInterviewService;
import com.example.pre_view.domain.question.dto.QuestionResponse;
import com.example.pre_view.domain.question.entity.Question;
import com.example.pre_view.domain.question.repository.QuestionRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnswerService {

    private final AnswerRepository answerRepository;
    private final QuestionRepository questionRepository;
    private final AiInterviewService aiInterviewService;

    @Transactional
    public AnswerResponse createAnswer(Long questionId, AnswerCreateRequest request) {
        log.info("답변 생성 시작 - questionId: {}", questionId);
        
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> {
                    log.warn("질문을 찾을 수 없음 - questionId: {}", questionId);
                    return new BusinessException(ErrorCode.QUESTION_NOT_FOUND);
                });

        log.debug("AI 피드백 생성 시작 - questionId: {}, phase: {}", questionId, question.getPhase());
        AiFeedbackResponse aiFeedback = aiInterviewService.generateFeedback(
                question.getPhase(),
                question.getContent(),
                request.content());
        log.debug("AI 피드백 생성 완료 - questionId: {}, score: {}", questionId, aiFeedback.score());

        Answer answer = Answer.builder()
                .question(question)
                .content(request.content())
                .feedback(aiFeedback.feedback())
                .score(aiFeedback.score())
                .build();

        Answer savedAnswer = answerRepository.save(answer);
        log.info("답변 저장 완료 - questionId: {}, answerId: {}, score: {}", 
                questionId, savedAnswer.getId(), savedAnswer.getScore());

        if (aiFeedback.needsFollowUp() && aiFeedback.followUpQuestion() != null) {
            log.debug("후속 질문 생성 시작 - questionId: {}", questionId);
            Question followUp = Question.builder()
                    .content(aiFeedback.followUpQuestion())
                    .interview(question.getInterview())
                    .phase(question.getPhase())
                    .sequence(getNextSequence(question.getInterview().getId()))
                    .parentQuestion(question)
                    .isFollowUp(true)
                    .build();

            Question savedFollowUp = questionRepository.save(followUp);
            log.info("후속 질문 생성 완료 - parentQuestionId: {}, followUpQuestionId: {}", 
                    questionId, savedFollowUp.getId());
            return AnswerResponse.of(savedAnswer, QuestionResponse.from(savedFollowUp));
        }

        return AnswerResponse.from(savedAnswer);
    }

    private int getNextSequence(Long interviewId) {
        return questionRepository.findByInterviewIdOrderBySequence(interviewId).size() + 1;
    }
}
