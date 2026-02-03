import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { getGoogleLoginUrl } from '../api/authApi';
import { Sparkles, Mail, Lock, AlertCircle } from 'lucide-react';
import { Button } from '../components/ui/button';

const Login = () => {
    const navigate = useNavigate();
    const { login } = useAuth();

    const [formData, setFormData] = useState({
        email: '',
        password: '',
    });
    const [error, setError] = useState('');
    const [errorType, setErrorType] = useState('');
    const [loading, setLoading] = useState(false);

    const handleChange = (e) => {
        const { name, value } = e.target;
        setFormData(prev => ({ ...prev, [name]: value }));
        setError('');
        setErrorType('');
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        setLoading(true);
        setError('');
        setErrorType('');

        try {
            await login(formData.email, formData.password);
            navigate('/');
        } catch (err) {
            const errorCode = err.response?.data?.code || '';
            const errorMessage = err.response?.data?.message || '로그인에 실패했습니다.';

            if (errorCode === 'AUTH007') {
                setErrorType('ACCOUNT_LOCKED');
            }

            setError(errorMessage);
        } finally {
            setLoading(false);
        }
    };

    const handleGoogleLogin = () => {
        window.location.href = getGoogleLoginUrl();
    };

    return (
        <div className="min-h-screen bg-white flex">
            {/* Left Side - Form */}
            <div className="flex-1 flex flex-col justify-center px-6 py-12 lg:px-8">
                <div className="sm:mx-auto sm:w-full sm:max-w-md">
                    {/* Logo */}
                    <div className="flex items-center justify-center gap-2 mb-8">
                        <div className="w-10 h-10 rounded-xl bg-gradient-to-br from-blue-500 to-cyan-400 flex items-center justify-center shadow-lg shadow-blue-500/25">
                            <Sparkles className="w-6 h-6 text-white" />
                        </div>
                        <span className="text-2xl font-bold text-gray-900">PreView</span>
                    </div>

                    <h2 className="text-center text-3xl font-bold text-gray-900 mb-2">
                        다시 오신 것을 환영해요
                    </h2>
                    <p className="text-center text-gray-500 mb-8">
                        계정이 없으신가요?{' '}
                        <Link to="/signup" className="font-medium text-blue-500 hover:text-blue-600">
                            회원가입
                        </Link>
                    </p>
                </div>

                <div className="sm:mx-auto sm:w-full sm:max-w-md">
                    {/* Error Message */}
                    {error && (
                        <div className={`mb-6 p-4 rounded-xl flex items-start gap-3 ${
                            errorType === 'ACCOUNT_LOCKED'
                                ? 'bg-amber-50 border border-amber-200'
                                : 'bg-red-50 border border-red-200'
                        }`}>
                            <AlertCircle className={`w-5 h-5 flex-shrink-0 mt-0.5 ${
                                errorType === 'ACCOUNT_LOCKED' ? 'text-amber-500' : 'text-red-500'
                            }`} />
                            <div>
                                <p className={`text-sm font-medium ${
                                    errorType === 'ACCOUNT_LOCKED' ? 'text-amber-700' : 'text-red-600'
                                }`}>
                                    {error}
                                </p>
                                {errorType === 'ACCOUNT_LOCKED' && (
                                    <p className="text-xs text-amber-600 mt-1">
                                        30분 후에 다시 시도해 주세요.
                                    </p>
                                )}
                            </div>
                        </div>
                    )}

                    <form onSubmit={handleSubmit} className="space-y-5">
                        <div>
                            <label htmlFor="email" className="block text-sm font-medium text-gray-700 mb-2">
                                이메일
                            </label>
                            <div className="relative">
                                <Mail className="absolute left-4 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400" />
                                <input
                                    id="email"
                                    name="email"
                                    type="email"
                                    required
                                    value={formData.email}
                                    onChange={handleChange}
                                    className="block w-full pl-12 pr-4 py-3 border border-gray-200 rounded-xl bg-gray-50 focus:bg-white focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-all"
                                    placeholder="example@email.com"
                                />
                            </div>
                        </div>

                        <div>
                            <label htmlFor="password" className="block text-sm font-medium text-gray-700 mb-2">
                                비밀번호
                            </label>
                            <div className="relative">
                                <Lock className="absolute left-4 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400" />
                                <input
                                    id="password"
                                    name="password"
                                    type="password"
                                    required
                                    value={formData.password}
                                    onChange={handleChange}
                                    className="block w-full pl-12 pr-4 py-3 border border-gray-200 rounded-xl bg-gray-50 focus:bg-white focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-all"
                                    placeholder="••••••••"
                                />
                            </div>
                        </div>

                        <Button
                            type="submit"
                            disabled={loading}
                            className="w-full bg-gradient-to-r from-blue-500 to-cyan-400 hover:from-blue-600 hover:to-cyan-500 text-white font-semibold rounded-xl h-12 shadow-lg shadow-blue-500/25 transition-all disabled:opacity-50"
                        >
                            {loading ? '로그인 중...' : '로그인'}
                        </Button>
                    </form>

                    {/* Divider */}
                    <div className="relative my-8">
                        <div className="absolute inset-0 flex items-center">
                            <div className="w-full border-t border-gray-200" />
                        </div>
                        <div className="relative flex justify-center text-sm">
                            <span className="px-4 bg-white text-gray-500">또는</span>
                        </div>
                    </div>

                    {/* Social Login */}
                    <button
                        onClick={handleGoogleLogin}
                        className="w-full flex items-center justify-center gap-3 py-3 px-4 border border-gray-200 rounded-xl bg-white text-sm font-medium text-gray-700 hover:bg-gray-50 hover:border-gray-300 transition-all"
                    >
                        <svg className="w-5 h-5" viewBox="0 0 24 24">
                            <path fill="#4285F4" d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z" />
                            <path fill="#34A853" d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z" />
                            <path fill="#FBBC05" d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z" />
                            <path fill="#EA4335" d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z" />
                        </svg>
                        Google로 계속하기
                    </button>
                </div>
            </div>

            {/* Right Side - Decorative */}
            <div className="hidden lg:flex lg:flex-1 bg-gradient-to-br from-blue-500 via-blue-600 to-cyan-500 relative overflow-hidden">
                <div className="absolute inset-0">
                    <div className="absolute -top-24 -right-24 w-96 h-96 bg-white/10 rounded-full blur-3xl" />
                    <div className="absolute -bottom-24 -left-24 w-64 h-64 bg-cyan-400/20 rounded-full blur-3xl" />
                </div>
                <div className="relative flex flex-col justify-center px-12 text-white">
                    <h2 className="text-4xl font-bold mb-4">
                        AI 면접 코칭으로<br />
                        자신감을 키우세요
                    </h2>
                    <p className="text-blue-100 text-lg">
                        실제 면접처럼 연습하고,<br />
                        즉각적인 피드백으로 빠르게 성장하세요.
                    </p>
                </div>
            </div>
        </div>
    );
};

export default Login;
