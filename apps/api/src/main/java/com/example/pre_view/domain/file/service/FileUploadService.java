package com.example.pre_view.domain.file.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.example.pre_view.common.exception.BusinessException;
import com.example.pre_view.common.exception.ErrorCode;
import com.example.pre_view.domain.interview.dto.InterviewResponse;
import com.example.pre_view.domain.interview.entity.Interview;
import com.example.pre_view.domain.interview.repository.InterviewRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 파일 업로드 관련 비즈니스 로직을 처리하는 서비스
 * - 이력서 PDF 업로드 및 텍스트 추출
 * - 포트폴리오 PDF 업로드 및 텍스트 추출
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileUploadService {

    private final InterviewRepository interviewRepository;
    private final PdfExtractionService pdfExtractionService;

    /**
     * 이력서 PDF 파일 업로드 및 텍스트 추출
     * 
     * @param interviewId 면접 ID
     * @param file 업로드된 PDF 파일
     * @return 업데이트된 면접 응답 DTO
     */
    @Transactional
    public InterviewResponse uploadResume(Long interviewId, MultipartFile file) {
        log.info("이력서 업로드 시작 - interviewId: {}, 파일명: {}", interviewId, file.getOriginalFilename());
        return uploadFile(interviewId, file, "이력서", Interview::updateResumeText);
    }

    /**
     * 포트폴리오 PDF 파일 업로드 및 텍스트 추출
     * 
     * @param interviewId 면접 ID
     * @param file 업로드된 PDF 파일
     * @return 업데이트된 면접 응답 DTO
     */
    @Transactional
    public InterviewResponse uploadPortfolio(Long interviewId, MultipartFile file) {
        log.info("포트폴리오 업로드 시작 - interviewId: {}, 파일명: {}", interviewId, file.getOriginalFilename());
        return uploadFile(interviewId, file, "포트폴리오", Interview::updatePortfolioText);
    }

    /**
     * 파일 업로드 공통 로직
     *
     * Fail-Fast 원칙: 파일 검증을 먼저 수행하여 잘못된 파일은 DB 조회 전에 빠르게 실패시킵니다.
     *
     * @param interviewId 면접 ID
     * @param file 업로드된 PDF 파일
     * @param fileType 파일 유형 (로깅용)
     * @param updateFunction 인터뷰 엔티티 업데이트 함수 (Interview, String) -> void
     * @return 업데이트된 면접 응답 DTO
     */
    private InterviewResponse uploadFile(
            Long interviewId,
            MultipartFile file,
            String fileType,
            java.util.function.BiConsumer<Interview, String> updateFunction) {

        // 1. 파일 검증 및 텍스트 추출 먼저 수행 (Fail-Fast)
        String extractedText = pdfExtractionService.extractText(file);

        // 2. 검증 통과 후 DB 조회
        Interview interview = interviewRepository.findByIdAndDeletedFalse(interviewId)
                .orElseThrow(() -> {
                    log.warn("면접을 찾을 수 없음 - interviewId: {}", interviewId);
                    return new BusinessException(ErrorCode.INTERVIEW_NOT_FOUND);
                });

        updateFunction.accept(interview, extractedText);

        log.info("{} 업로드 완료 - interviewId: {}, 추출된 텍스트 길이: {} 문자",
                fileType, interviewId, extractedText.length());

        return InterviewResponse.from(interview);
    }
}
