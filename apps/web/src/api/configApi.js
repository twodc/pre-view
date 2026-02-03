import api from './axiosConfig';

/**
 * 기능 설정 조회
 * @returns {Promise<{voiceEnabled: boolean, gradioConfigured: boolean}>}
 */
export const getFeatures = async () => {
    const response = await api.get('/config/features');
    return response.data;
};
