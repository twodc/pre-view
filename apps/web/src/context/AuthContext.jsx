import React, { createContext, useContext, useState, useEffect } from 'react';
import { login as loginApi, signup as signupApi, logout as logoutApi } from '../api/authApi';

const AuthContext = createContext(null);

export const AuthProvider = ({ children }) => {
    const [user, setUser] = useState(null);
    const [loading, setLoading] = useState(true);

    // JWT 토큰 만료 여부 확인
    const isTokenExpired = (token) => {
        try {
            const payload = JSON.parse(atob(token.split('.')[1]));
            // exp는 초 단위, Date.now()는 밀리초 단위
            return payload.exp * 1000 < Date.now();
        } catch (e) {
            return true;
        }
    };

    // 초기 로드 시 토큰 확인
    useEffect(() => {
        const accessToken = localStorage.getItem('accessToken');
        if (accessToken) {
            // JWT에서 payload 추출 및 만료 확인
            try {
                // Access Token 만료 확인
                if (isTokenExpired(accessToken)) {
                    // Refresh Token으로 재발급 시도
                    const refreshToken = localStorage.getItem('refreshToken');
                    if (refreshToken && !isTokenExpired(refreshToken)) {
                        // axiosConfig의 인터셉터가 처리하도록 토큰은 유지
                        // 첫 API 호출 시 자동 재발급됨
                        const payload = JSON.parse(atob(accessToken.split('.')[1]));
                        setUser({
                            memberId: payload.memberId,
                            role: payload.role,
                        });
                    } else {
                        // Refresh Token도 만료됨 - 로그아웃 처리
                        localStorage.removeItem('accessToken');
                        localStorage.removeItem('refreshToken');
                    }
                } else {
                    const payload = JSON.parse(atob(accessToken.split('.')[1]));
                    setUser({
                        memberId: payload.memberId,
                        role: payload.role,
                    });
                }
            } catch (e) {
                localStorage.removeItem('accessToken');
                localStorage.removeItem('refreshToken');
            }
        }
        setLoading(false);
    }, []);

    // axios 인터셉터에서 발생한 로그아웃 이벤트 수신
    useEffect(() => {
        const handleForceLogout = () => {
            setUser(null);
        };

        window.addEventListener('auth:logout', handleForceLogout);
        return () => {
            window.removeEventListener('auth:logout', handleForceLogout);
        };
    }, []);

    // 회원가입 후 자동 로그인
    const signup = async (email, name, password) => {
        await signupApi(email, name, password);
        // 회원가입 성공 후 자동 로그인
        return await login(email, password);
    };

    // 로그인
    const login = async (email, password) => {
        const response = await loginApi(email, password);
        const { accessToken, refreshToken } = response.data;

        localStorage.setItem('accessToken', accessToken);
        localStorage.setItem('refreshToken', refreshToken);

        // JWT payload에서 사용자 정보 추출
        const payload = JSON.parse(atob(accessToken.split('.')[1]));
        setUser({
            memberId: payload.memberId,
            role: payload.role,
        });

        return response;
    };

    // 로그아웃
    const logout = async () => {
        try {
            await logoutApi();
        } catch (e) {
            // 서버 에러 무시 (로컬에서는 무조건 로그아웃)
        }
        localStorage.removeItem('accessToken');
        localStorage.removeItem('refreshToken');
        setUser(null);
    };

    // OAuth 로그인 성공 후 토큰 처리
    const handleOAuthCallback = (accessToken, refreshToken) => {
        localStorage.setItem('accessToken', accessToken);
        localStorage.setItem('refreshToken', refreshToken);

        const payload = JSON.parse(atob(accessToken.split('.')[1]));
        setUser({
            memberId: payload.memberId,
            role: payload.role,
        });
    };

    const value = {
        user,
        loading,
        isAuthenticated: !!user,
        signup,
        login,
        logout,
        handleOAuthCallback,
    };

    return (
        <AuthContext.Provider value={value}>
            {children}
        </AuthContext.Provider>
    );
};

export const useAuth = () => {
    const context = useContext(AuthContext);
    if (!context) {
        throw new Error('useAuth must be used within an AuthProvider');
    }
    return context;
};
