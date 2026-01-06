import api from './axiosConfig';

/**
 * 회원가입
 */
export const signup = async (email, name, password) => {
    const response = await api.post('/auth/signup', { email, name, password });
    return response;
};

/**
 * 로그인
 */
export const login = async (email, password) => {
    const response = await api.post('/auth/login', { email, password });
    return response;
};

/**
 * 로그아웃
 */
export const logout = async () => {
    const response = await api.post('/auth/logout');
    return response;
};

/**
 * 토큰 재발급
 */
export const reissueToken = async (refreshToken) => {
    const response = await api.post('/auth/reissue', { refreshToken });
    return response;
};

/**
 * Google OAuth 로그인 URL (OAuth2는 /api/v1 외부 경로)
 * 배포 환경에서는 Nginx 프록시를 통해 /oauth2로 접근
 */
export const getGoogleLoginUrl = () => {
    // 배포 환경: 상대 경로 사용 (Nginx가 백엔드로 프록시)
    // 개발 환경: localhost:8080 직접 접근
    const baseUrl = import.meta.env.VITE_API_BASE_URL?.startsWith('http')
        ? import.meta.env.VITE_API_BASE_URL.replace('/api/v1', '')
        : '';
    return `${baseUrl}/oauth2/authorization/google`;
};
