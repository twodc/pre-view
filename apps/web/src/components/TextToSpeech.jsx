import React, { useState, useRef } from 'react';
import { synthesizeSpeech } from '../api/voiceApi';

/**
 * 텍스트 음성 변환 컴포넌트 (TTS)
 * 텍스트를 음성으로 읽어줌
 */
const TextToSpeech = ({ text, disabled = false }) => {
    const [isPlaying, setIsPlaying] = useState(false);
    const [isLoading, setIsLoading] = useState(false);
    const [error, setError] = useState('');
    const audioRef = useRef(null);

    const playAudio = async () => {
        if (!text || isPlaying || isLoading) return;

        setError('');
        setIsLoading(true);

        try {
            const response = await synthesizeSpeech(text, 'Korean');

            if (response.audioData) {
                // Base64 오디오 데이터를 재생
                const audio = new Audio(`data:audio/wav;base64,${response.audioData}`);
                audioRef.current = audio;

                audio.onplay = () => {
                    setIsPlaying(true);
                    setIsLoading(false);
                };

                audio.onended = () => {
                    setIsPlaying(false);
                    audioRef.current = null;
                };

                audio.onerror = () => {
                    setError('오디오 재생 실패');
                    setIsPlaying(false);
                    setIsLoading(false);
                    audioRef.current = null;
                };

                await audio.play();
            } else {
                // Fallback: Web Speech API 사용
                const utterance = new SpeechSynthesisUtterance(text);
                utterance.lang = 'ko-KR';
                utterance.rate = 0.9;

                utterance.onstart = () => {
                    setIsPlaying(true);
                    setIsLoading(false);
                };

                utterance.onend = () => {
                    setIsPlaying(false);
                };

                utterance.onerror = () => {
                    setError('음성 합성 실패');
                    setIsPlaying(false);
                    setIsLoading(false);
                };

                window.speechSynthesis.speak(utterance);
            }
        } catch (err) {
            console.error('TTS 오류:', err);
            // Fallback: Web Speech API
            try {
                const utterance = new SpeechSynthesisUtterance(text);
                utterance.lang = 'ko-KR';
                utterance.rate = 0.9;

                utterance.onstart = () => {
                    setIsPlaying(true);
                    setIsLoading(false);
                };

                utterance.onend = () => {
                    setIsPlaying(false);
                };

                window.speechSynthesis.speak(utterance);
            } catch (fallbackErr) {
                setError('음성 재생에 실패했습니다.');
                setIsLoading(false);
            }
        }
    };

    const stopAudio = () => {
        // HTMLAudioElement 중지
        if (audioRef.current) {
            audioRef.current.pause();
            audioRef.current.currentTime = 0;
            audioRef.current = null;
        }
        // Web Speech API 중지
        window.speechSynthesis.cancel();
        setIsPlaying(false);
    };

    return (
        <button
            type="button"
            onClick={isPlaying ? stopAudio : playAudio}
            disabled={disabled || isLoading || !text}
            className={`p-2 rounded-lg transition-all duration-200 ${
                isPlaying
                    ? 'bg-blue-500 text-white'
                    : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
            } ${(disabled || isLoading || !text) ? 'opacity-50 cursor-not-allowed' : ''}`}
            title={isPlaying ? '정지' : '질문 듣기'}
        >
            {isLoading ? (
                <svg className="w-5 h-5 animate-spin" fill="none" viewBox="0 0 24 24">
                    <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                    <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                </svg>
            ) : isPlaying ? (
                <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 10h.01M15 10h.01M9 14h6" />
                </svg>
            ) : (
                <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15.536 8.464a5 5 0 010 7.072m2.828-9.9a9 9 0 010 12.728M5.586 15H4a1 1 0 01-1-1v-4a1 1 0 011-1h1.586l4.707-4.707C10.923 3.663 12 4.109 12 5v14c0 .891-1.077 1.337-1.707.707L5.586 15z" />
                </svg>
            )}
        </button>
    );
};

export default TextToSpeech;
