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

        Interview interview = interviewRepository.findByIdAndNotDeleted(interviewId)
                .orElseThrow(() -> {
                    log.warn("면접을 찾을 수 없음 - interviewId: {}", interviewId);
                    return new BusinessException(ErrorCode.INTERVIEW_NOT_FOUND);
                });

        // PDF에서 텍스트 추출
        String resumeText = pdfExtractionService.extractText(file);

        // 면접 엔티티에 텍스트 저장
        interview.updateResumeText(resumeText);

        log.info("이력서 업로드 완료 - interviewId: {}, 추출된 텍스트 길이: {} 문자",
                interviewId, resumeText.length());

        return InterviewResponse.from(interview);
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

        Interview interview = interviewRepository.findByIdAndNotDeleted(interviewId)
                .orElseThrow(() -> {
                    log.warn("면접을 찾을 수 없음 - interviewId: {}", interviewId);
                    return new BusinessException(ErrorCode.INTERVIEW_NOT_FOUND);
                });

        // PDF에서 텍스트 추출
        String portfolioText = pdfExtractionService.extractText(file);

        // 면접 엔티티에 텍스트 저장
        interview.updatePortfolioText(portfolioText);

        log.info("포트폴리오 업로드 완료 - interviewId: {}, 추출된 텍스트 길이: {} 문자",
                interviewId, portfolioText.length());

        return InterviewResponse.from(interview);
    }
}

