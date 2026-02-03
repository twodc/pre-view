import React, { useEffect, useRef } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

/**
 * OAuth 로그인 성공 후 리다이렉트되는 페이지
 * URL 파라미터로 토큰을 받아 처리
 */
const OAuthCallback = () => {
    const navigate = useNavigate();
    const [searchParams] = useSearchParams();
    const { handleOAuthCallback } = useAuth();
    const processed = useRef(false);

    useEffect(() => {
        // 이미 처리했으면 스킵 (StrictMode 등으로 인한 중복 실행 방지)
        if (processed.current) return;

        const accessToken = searchParams.get('accessToken');
        const refreshToken = searchParams.get('refreshToken');
        const error = searchParams.get('error');

        if (error) {
            processed.current = true;
            navigate('/login', {
                state: { error: 'OAuth 로그인에 실패했습니다.' },
                replace: true
            });
            return;
        }

        if (accessToken && refreshToken) {
            processed.current = true;
            handleOAuthCallback(accessToken, refreshToken);
            // state 업데이트 후 navigate
            setTimeout(() => {
                navigate('/', { replace: true });
            }, 100);
        } else {
            processed.current = true;
            navigate('/login', { replace: true });
        }
    }, [searchParams, handleOAuthCallback, navigate]);

    return (
        <div className="min-h-screen flex items-center justify-center bg-gray-50">
            <div className="text-center">
                <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-indigo-600 mx-auto"></div>
                <p className="mt-4 text-gray-600">로그인 처리 중...</p>
            </div>
        </div>
    );
};

export default OAuthCallback;
