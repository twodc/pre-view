package com.example.pre_view.domain.file.service;

import java.io.IOException;
import java.io.InputStream;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.example.pre_view.common.exception.BusinessException;
import com.example.pre_view.common.exception.ErrorCode;

import lombok.extern.slf4j.Slf4j;

/**
 * PDF 파일에서 텍스트를 추출하는 서비스
 */
@Slf4j
@Service
public class PdfExtractionService {

    private static final String PDF_CONTENT_TYPE = "application/pdf";
    private static final int MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB

    /**
     * PDF 파일에서 텍스트를 추출합니다.
     * 
     * @param file 업로드된 PDF 파일
     * @return 추출된 텍스트
     * @throws BusinessException 파일 형식이 올바르지 않거나 읽기 실패 시
     */
    public String extractText(MultipartFile file) {
        log.debug("PDF 텍스트 추출 시작 - 파일명: {}, 크기: {} bytes", file.getOriginalFilename(), file.getSize());
        validateFile(file);

        try (InputStream inputStream = file.getInputStream()) {
            byte[] pdfBytes = inputStream.readAllBytes();
            PDDocument document = org.apache.pdfbox.Loader.loadPDF(pdfBytes);
            
            try {
                PDFTextStripper stripper = new PDFTextStripper();
                String text = stripper.getText(document);

                log.info("PDF 텍스트 추출 완료 - 파일명: {}, 추출된 텍스트 길이: {} 문자", 
                        file.getOriginalFilename(), text.length());

                return text.trim();
            } finally {
                document.close();
            }
        } catch (IOException e) {
            log.error("PDF 파일 읽기 실패 - 파일명: {}", file.getOriginalFilename(), e);
            throw new BusinessException(ErrorCode.FILE_READ_ERROR);
        }
    }

    /**
     * 파일 유효성 검증
     */
    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            log.warn("빈 파일 업로드 시도");
            throw new BusinessException(ErrorCode.FILE_UPLOAD_ERROR);
        }

        // 파일 크기 검증
        if (file.getSize() > MAX_FILE_SIZE) {
            log.warn("파일 크기 초과 - 파일명: {}, 크기: {} bytes", file.getOriginalFilename(), file.getSize());
            throw new BusinessException(ErrorCode.FILE_UPLOAD_ERROR);
        }

        // 파일 형식 검증
        String contentType = file.getContentType();
        if (contentType == null || !contentType.equals(PDF_CONTENT_TYPE)) {
            log.warn("지원하지 않는 파일 형식 - 파일명: {}, Content-Type: {}", 
                    file.getOriginalFilename(), contentType);
            throw new BusinessException(ErrorCode.INVALID_FILE_TYPE);
        }

        // 파일 확장자 검증 (추가 안전장치)
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.toLowerCase().endsWith(".pdf")) {
            log.warn("PDF 확장자가 아닌 파일 - 파일명: {}", originalFilename);
            throw new BusinessException(ErrorCode.INVALID_FILE_TYPE);
        }
    }
}

