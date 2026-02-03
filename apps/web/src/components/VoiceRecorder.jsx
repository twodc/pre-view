import React, { useState, useRef, useEffect } from 'react';
import { transcribeAudio } from '../api/voiceApi';

/**
 * 음성 녹음 컴포넌트 (STT)
 * 마이크로 녹음 → 텍스트 변환
 */
const VoiceRecorder = ({ onTranscript, disabled = false }) => {
    const [isRecording, setIsRecording] = useState(false);
    const [isProcessing, setIsProcessing] = useState(false);
    const [error, setError] = useState('');
    const [remainingTime, setRemainingTime] = useState(60);
    const mediaRecorderRef = useRef(null);
    const chunksRef = useRef([]);
    const timerRef = useRef(null);

    const startRecording = async () => {
        setError('');
        setRemainingTime(60);
        try {
            const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
            const mediaRecorder = new MediaRecorder(stream, {
                mimeType: 'audio/webm;codecs=opus'
            });

            mediaRecorderRef.current = mediaRecorder;
            chunksRef.current = [];

            mediaRecorder.ondataavailable = (e) => {
                if (e.data.size > 0) {
                    chunksRef.current.push(e.data);
                }
            };

            mediaRecorder.onstop = async () => {
                const audioBlob = new Blob(chunksRef.current, { type: 'audio/webm' });
                await processAudio(audioBlob);

                // 스트림 정리
                stream.getTracks().forEach(track => track.stop());
            };

            mediaRecorder.start();
            setIsRecording(true);

            // 60초 카운트다운 타이머 시작
            timerRef.current = setInterval(() => {
                setRemainingTime((prev) => {
                    if (prev <= 1) {
                        stopRecording();
                        return 60;
                    }
                    return prev - 1;
                });
            }, 1000);
        } catch (err) {
            console.error('마이크 접근 오류:', err);
            setError('마이크 접근이 거부되었습니다. 브라우저 설정을 확인해주세요.');
        }
    };

    const stopRecording = () => {
        if (mediaRecorderRef.current && isRecording) {
            mediaRecorderRef.current.stop();
            setIsRecording(false);
        }
        // 타이머 정리
        if (timerRef.current) {
            clearInterval(timerRef.current);
            timerRef.current = null;
        }
    };

    const processAudio = async (audioBlob) => {
        setIsProcessing(true);
        try {
            // 파일 크기 검증 (10MB 제한)
            if (audioBlob.size > 10 * 1024 * 1024) {
                setError('녹음이 너무 깁니다. 60초 이내로 녹음해주세요.');
                return;
            }

            // Blob을 File로 변환 (실제 MIME 타입에 맞는 확장자 사용)
            const mimeType = audioBlob.type || 'audio/webm';
            const extension = mimeType.includes('webm') ? 'webm' : mimeType.includes('ogg') ? 'ogg' : 'webm';
            const audioFile = new File([audioBlob], `recording.${extension}`, { type: mimeType });

            const response = await transcribeAudio(audioFile, 'korean');
            if (response.text) {
                onTranscript(response.text);
            }
        } catch (err) {
            console.error('음성 인식 오류:', err);

            // 개선된 에러 메시지 처리
            if (err.name === 'TimeoutError' || err.name === 'AbortError' || err.code === 'ECONNABORTED') {
                setError('서버 응답 시간 초과. 다시 시도해주세요.');
            } else if (err.response?.status >= 500) {
                setError('서버 오류가 발생했습니다. 잠시 후 다시 시도해주세요.');
            } else if (err.response?.status === 413) {
                setError('파일 크기가 너무 큽니다.');
            } else {
                setError('음성 인식에 실패했습니다. 다시 시도해주세요.');
            }
        } finally {
            setIsProcessing(false);
        }
    };

    const handleClick = () => {
        if (isRecording) {
            stopRecording();
        } else {
            startRecording();
        }
    };

    // 컴포넌트 언마운트 시 정리
    useEffect(() => {
        return () => {
            if (timerRef.current) {
                clearInterval(timerRef.current);
            }
            // 녹음 중이면 중지
            if (mediaRecorderRef.current && mediaRecorderRef.current.state !== 'inactive') {
                mediaRecorderRef.current.stop();
            }
        };
    }, []);

    return (
        <div className="flex items-center gap-2">
            <button
                type="button"
                onClick={handleClick}
                disabled={disabled || isProcessing}
                className={`p-3 rounded-xl transition-all duration-200 flex items-center gap-2 ${
                    isRecording
                        ? 'bg-red-500 text-white animate-pulse hover:bg-red-600'
                        : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
                } ${(disabled || isProcessing) ? 'opacity-50 cursor-not-allowed' : ''}`}
                title={isRecording ? '녹음 중지' : '음성으로 답변하기'}
            >
                {isProcessing ? (
                    <svg className="w-5 h-5 animate-spin" fill="none" viewBox="0 0 24 24">
                        <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                        <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                    </svg>
                ) : (
                    <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 11a7 7 0 01-7 7m0 0a7 7 0 01-7-7m7 7v4m0 0H8m4 0h4m-4-8a3 3 0 01-3-3V5a3 3 0 116 0v6a3 3 0 01-3 3z" />
                    </svg>
                )}
                <span className="text-sm font-medium">
                    {isProcessing ? '변환 중...' : isRecording ? `녹음 중 ${remainingTime}초` : '음성 입력'}
                </span>
            </button>

            {error && (
                <span className="text-sm text-red-500">{error}</span>
            )}
        </div>
    );
};

export default VoiceRecorder;
