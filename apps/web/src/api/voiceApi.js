import api from './axiosConfig';

/**
 * 음성 인식 (STT) - 오디오 파일을 텍스트로 변환
 * @param {File} audioFile - 오디오 파일
 * @param {string} language - 언어 (korean, english, japanese)
 * @returns {Promise} - { text, language, confidence }
 */
export const transcribeAudio = async (audioFile, language = 'korean') => {
    const formData = new FormData();
    formData.append('file', audioFile);
    formData.append('language', language);

    const controller = new AbortController();
    const timeoutId = setTimeout(() => controller.abort(), 30000);

    try {
        const response = await api.post('/voice/stt', formData, {
            headers: {
                'Content-Type': 'multipart/form-data',
            },
            signal: controller.signal,
        });
        return response;
    } catch (error) {
        if (error.name === 'AbortError' || error.code === 'ECONNABORTED') {
            const timeoutError = new Error('timeout');
            timeoutError.name = 'AbortError';
            throw timeoutError;
        }
        throw error;
    } finally {
        clearTimeout(timeoutId);
    }
};

/**
 * 음성 합성 (TTS) - 텍스트를 음성으로 변환
 * @param {string} text - 변환할 텍스트
 * @param {string} language - 언어 (Korean, English, Japanese)
 * @returns {Promise} - { audioData, format, sampleRate }
 */
export const synthesizeSpeech = async (text, language = 'Korean') => {
    const response = await api.post(`/voice/tts?text=${encodeURIComponent(text)}&language=${language}`);
    return response;
};

/**
 * 음성 서비스 헬스체크
 * @returns {Promise} - 'OK' or error
 */
export const checkVoiceHealth = async () => {
    const response = await api.get('/voice/health');
    return response;
};
