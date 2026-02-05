import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { getGoogleLoginUrl } from '../api/authApi';
import { Mail, Lock, User, Check, AlertCircle } from 'lucide-react';
import { Button } from '../components/ui/button';

const Signup = () => {
    const navigate = useNavigate();
    const { signup } = useAuth();

    const [formData, setFormData] = useState({
        email: '',
        name: '',
        password: '',
        confirmPassword: '',
    });
    const [error, setError] = useState('');
    const [loading, setLoading] = useState(false);

    const handleChange = (e) => {
        const { name, value } = e.target;
        setFormData(prev => ({ ...prev, [name]: value }));
        setError('');
    };

    const validatePassword = (password) => {
        const hasLetter = /[a-zA-Z]/.test(password);
        const hasNumber = /\d/.test(password);
        const hasSpecial = /[!@#$%^&*()_+\-=\[\]{};':"\\|,.<>\/?]/.test(password);
        const hasLength = password.length >= 8;
        return { hasLetter, hasNumber, hasSpecial, hasLength };
    };

    const passwordValidation = validatePassword(formData.password);
    const isPasswordValid = passwordValidation.hasLength &&
        passwordValidation.hasLetter &&
        passwordValidation.hasNumber &&
        passwordValidation.hasSpecial;

    const validateForm = () => {
        if (formData.name.length < 2) {
            setError('이름은 2자 이상이어야 합니다.');
            return false;
        }
        if (!passwordValidation.hasLength) {
            setError('비밀번호는 8자 이상이어야 합니다.');
            return false;
        }
        if (!isPasswordValid) {
            setError('비밀번호는 영문, 숫자, 특수문자를 각각 1개 이상 포함해야 합니다.');
            return false;
        }
        if (formData.password !== formData.confirmPassword) {
            setError('비밀번호가 일치하지 않습니다.');
            return false;
        }
        return true;
    };

    const handleSubmit = async (e) => {
        e.preventDefault();

        if (!validateForm()) return;

        setLoading(true);
        setError('');

        try {
            await signup(formData.email, formData.name, formData.password);
            navigate('/create');
        } catch (err) {
            const errorMessage = err.response?.data?.message || '회원가입에 실패했습니다.';
            setError(errorMessage);
        } finally {
            setLoading(false);
        }
    };

    const handleGoogleLogin = () => {
        window.location.href = getGoogleLoginUrl();
    };

    const ValidationItem = ({ valid, text }) => (
        <span className={`flex items-center gap-1 text-xs ${valid ? 'text-green-600' : 'text-gray-400'}`}>
            {valid ? <Check className="w-3 h-3" /> : <span className="w-3 h-3 rounded-full border border-gray-300" />}
            {text}
        </span>
    );

    return (
        <div className="min-h-screen bg-white flex">
            {/* Left Side - Decorative */}
            <div className="hidden lg:flex lg:flex-1 bg-gradient-to-br from-blue-500 via-blue-600 to-cyan-500 relative overflow-hidden">
                <div className="absolute inset-0">
                    <div className="absolute -top-24 -right-24 w-96 h-96 bg-white/10 rounded-full blur-3xl" />
                    <div className="absolute -bottom-24 -left-24 w-64 h-64 bg-cyan-400/20 rounded-full blur-3xl" />
                </div>
                <div className="relative flex flex-col justify-center px-12 text-white">
                    <h2 className="text-4xl font-bold mb-4">
                        면접 준비,<br />
                        이제 혼자 하지 마세요
                    </h2>
                    <p className="text-blue-100 text-lg">
                        AI 면접관과 함께 연습하고,<br />
                        맞춤형 피드백으로 빠르게 성장하세요.
                    </p>
                </div>
            </div>

            {/* Right Side - Form */}
            <div className="flex-1 flex flex-col justify-center px-6 py-12 lg:px-8">
                <div className="sm:mx-auto sm:w-full sm:max-w-md">
                    {/* Logo */}
                    <div className="flex items-center justify-center mb-8">
                        <img src="/logo.png" alt="PreView" className="h-16 mix-blend-multiply" />
                    </div>

                    <h2 className="text-center text-3xl font-bold text-gray-900 mb-2">
                        회원가입
                    </h2>
                    <p className="text-center text-gray-500 mb-8">
                        이미 계정이 있으신가요?{' '}
                        <Link to="/login" className="font-medium text-blue-500 hover:text-blue-600">
                            로그인
                        </Link>
                    </p>
                </div>

                <div className="sm:mx-auto sm:w-full sm:max-w-md">
                    {/* Error Message */}
                    {error && (
                        <div className="mb-6 p-4 rounded-xl flex items-start gap-3 bg-red-50 border border-red-200">
                            <AlertCircle className="w-5 h-5 flex-shrink-0 mt-0.5 text-red-500" />
                            <p className="text-sm font-medium text-red-600">{error}</p>
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
                            <label htmlFor="name" className="block text-sm font-medium text-gray-700 mb-2">
                                이름
                            </label>
                            <div className="relative">
                                <User className="absolute left-4 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400" />
                                <input
                                    id="name"
                                    name="name"
                                    type="text"
                                    required
                                    value={formData.name}
                                    onChange={handleChange}
                                    className="block w-full pl-12 pr-4 py-3 border border-gray-200 rounded-xl bg-gray-50 focus:bg-white focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-all"
                                    placeholder="홍길동"
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
                                    placeholder="영문, 숫자, 특수문자 포함 8자 이상"
                                />
                            </div>
                            {formData.password && (
                                <div className="mt-2 flex flex-wrap gap-3">
                                    <ValidationItem valid={passwordValidation.hasLength} text="8자 이상" />
                                    <ValidationItem valid={passwordValidation.hasLetter} text="영문" />
                                    <ValidationItem valid={passwordValidation.hasNumber} text="숫자" />
                                    <ValidationItem valid={passwordValidation.hasSpecial} text="특수문자" />
                                </div>
                            )}
                        </div>

                        <div>
                            <label htmlFor="confirmPassword" className="block text-sm font-medium text-gray-700 mb-2">
                                비밀번호 확인
                            </label>
                            <div className="relative">
                                <Lock className="absolute left-4 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400" />
                                <input
                                    id="confirmPassword"
                                    name="confirmPassword"
                                    type="password"
                                    required
                                    value={formData.confirmPassword}
                                    onChange={handleChange}
                                    className="block w-full pl-12 pr-4 py-3 border border-gray-200 rounded-xl bg-gray-50 focus:bg-white focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-all"
                                    placeholder="비밀번호 재입력"
                                />
                            </div>
                        </div>

                        <Button
                            type="submit"
                            disabled={loading}
                            className="w-full bg-gradient-to-r from-blue-500 to-cyan-400 hover:from-blue-600 hover:to-cyan-500 text-white font-semibold rounded-xl h-12 shadow-lg shadow-blue-500/25 transition-all disabled:opacity-50"
                        >
                            {loading ? '가입 중...' : '회원가입'}
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
        </div>
    );
};

export default Signup;
