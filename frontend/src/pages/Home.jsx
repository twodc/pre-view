import React from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

// Feature별 아이콘 정의
const FEATURE_ICONS = {
    question: (
        <svg className="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M8.228 9c.549-1.165 2.03-2 3.772-2 2.21 0 4 1.343 4 3 0 1.4-1.278 2.575-3.006 2.907-.542.104-.994.54-.994 1.093m0 3h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
        </svg>
    ),
    feedback: (
        <svg className="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
        </svg>
    ),
    followUp: (
        <svg className="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M17 8h2a2 2 0 012 2v6a2 2 0 01-2 2h-2v4l-4-4H9a1.994 1.994 0 01-1.414-.586m0 0L11 14h4a2 2 0 002-2V6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2v4l.586-.586z" />
        </svg>
    ),
    report: (
        <svg className="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M9 17v-2m3 2v-4m3 4v-6m2 10H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
        </svg>
    ),
};

const FEATURES = [
    {
        name: '맞춤형 질문 생성',
        description: '지원하는 포지션과 경력에 맞춰 AI가 최적의 질문을 생성합니다.',
        icon: 'question',
    },
    {
        name: '실시간 피드백',
        description: '답변 직후 AI가 개선점과 모범 답안을 제시하여 즉각적인 학습이 가능합니다.',
        icon: 'feedback',
    },
    {
        name: '심층 꼬리 질문',
        description: '꼬리를 무는 심층 질문으로 실제 면접 같은 긴장감과 대응력을 기르세요.',
        icon: 'followUp',
    },
    {
        name: '종합 리포트',
        description: '면접 종료 후 강점과 약점을 분석한 상세 리포트를 제공합니다.',
        icon: 'report',
    },
];

const Home = () => {
    const { isAuthenticated, logout } = useAuth();

    return (
        <div className="bg-white min-h-screen">
            {/* Navigation */}
            <nav className="bg-white shadow-sm">
                <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
                    <div className="flex justify-between h-16 items-center">
                        <Link to="/" className="text-2xl font-bold text-indigo-600">
                            PreView
                        </Link>
                        <div className="flex items-center gap-4">
                            {isAuthenticated ? (
                                <button
                                    onClick={logout}
                                    className="px-4 py-2 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 transition-colors font-medium"
                                >
                                    로그아웃
                                </button>
                            ) : (
                                <>
                                    <Link
                                        to="/login"
                                        className="text-gray-600 hover:text-gray-900 font-medium"
                                    >
                                        로그인
                                    </Link>
                                    <Link
                                        to="/signup"
                                        className="px-4 py-2 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 transition-colors font-medium"
                                    >
                                        회원가입
                                    </Link>
                                </>
                            )}
                        </div>
                    </div>
                </div>
            </nav>

            {/* Hero Section */}
            <div className="relative overflow-hidden">
                <div className="max-w-7xl mx-auto">
                    <div className="relative z-10 pb-8 bg-white sm:pb-16 md:pb-20 lg:max-w-2xl lg:w-full lg:pb-28 xl:pb-32">
                        {/* 데스크탑에서 이미지와 텍스트 사이 대각선 구분선 */}
                        <svg
                            className="hidden lg:block absolute right-0 inset-y-0 h-full w-48 text-white transform translate-x-1/2"
                            fill="currentColor"
                            viewBox="0 0 100 100"
                            preserveAspectRatio="none"
                            aria-hidden="true"
                        >
                            <polygon points="50,0 100,0 50,100 0,100" />
                        </svg>

                        <main className="pt-10 mx-auto max-w-7xl px-4 sm:pt-12 sm:px-6 md:pt-16 lg:pt-20 lg:px-8 xl:pt-28">
                            <div className="text-center lg:text-left">
                                <h1 className="text-3xl tracking-tight font-extrabold text-gray-900 sm:text-4xl md:text-5xl lg:text-6xl">
                                    <span className="block">AI 면접의 새로운 기준</span>
                                    <span className="block text-indigo-600 mt-1 sm:mt-2">PreView</span>
                                </h1>
                                <p className="mt-3 text-base text-gray-500 sm:mt-5 sm:text-lg sm:max-w-xl sm:mx-auto md:mt-5 md:text-xl lg:mx-0">
                                    실제 면접관 같은 AI와 함께 실전처럼 연습하세요.
                                    <br className="hidden sm:inline" />
                                    <span className="sm:hidden"> </span>
                                    이력서 기반 맞춤 질문부터 실시간 피드백까지,
                                    <br className="hidden sm:inline" />
                                    <span className="sm:hidden"> </span>
                                    당신의 취업 성공을 위한 완벽한 파트너입니다.
                                </p>
                                <div className="mt-6 sm:mt-8 flex flex-col sm:flex-row sm:justify-center lg:justify-start gap-3 sm:gap-4">
                                    <Link
                                        to={isAuthenticated ? "/create" : "/signup"}
                                        className="w-full sm:w-auto flex items-center justify-center px-8 py-3 border border-transparent text-base font-medium rounded-lg text-white bg-indigo-600 hover:bg-indigo-700 shadow-lg hover:shadow-xl transition-all duration-200 md:py-4 md:text-lg md:px-10"
                                    >
                                        {isAuthenticated ? "면접 시작하기" : "지금 시작하기"}
                                    </Link>
                                    {isAuthenticated && (
                                        <Link
                                            to="/dashboard"
                                            className="w-full sm:w-auto flex items-center justify-center px-8 py-3 border border-transparent text-base font-medium rounded-lg text-indigo-700 bg-indigo-100 hover:bg-indigo-200 transition-all duration-200 md:py-4 md:text-lg md:px-10"
                                        >
                                            내 면접 보기
                                        </Link>
                                    )}
                                </div>
                            </div>
                        </main>
                    </div>
                </div>

                {/* Hero Image - 모바일에서는 상단, 데스크탑에서는 우측 */}
                <div className="lg:absolute lg:inset-y-0 lg:right-0 lg:w-1/2">
                    <div className="h-48 w-full bg-gradient-to-br from-indigo-400 to-purple-500 sm:h-64 md:h-80 lg:w-full lg:h-full flex items-center justify-center">
                        {/* 이미지가 없을 경우 플레이스홀더 */}
                        <div className="text-white text-center p-8">
                            <svg className="mx-auto h-16 w-16 sm:h-24 sm:w-24 opacity-75" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.5" d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z" />
                            </svg>
                            <p className="mt-4 text-lg sm:text-xl font-medium opacity-90">AI 모의 면접</p>
                        </div>
                    </div>
                </div>
            </div>

            {/* Feature Section */}
            <div className="py-12 sm:py-16 lg:py-20 bg-gray-50">
                <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
                    <div className="text-center">
                        <h2 className="text-sm sm:text-base text-indigo-600 font-semibold tracking-wide uppercase">Features</h2>
                        <p className="mt-2 text-2xl sm:text-3xl lg:text-4xl leading-8 font-extrabold tracking-tight text-gray-900">
                            완벽한 면접 준비를 위한 기능
                        </p>
                        <p className="mt-4 max-w-2xl text-base sm:text-lg lg:text-xl text-gray-500 mx-auto">
                            직무별 전문 질문과 AI의 정교한 평가로 면접 실력을 향상시키세요.
                        </p>
                    </div>

                    <div className="mt-10 sm:mt-12 lg:mt-16">
                        <dl className="space-y-8 sm:space-y-10 md:space-y-0 md:grid md:grid-cols-2 md:gap-x-8 md:gap-y-10 lg:gap-x-12">
                            {FEATURES.map((feature) => (
                                <div key={feature.name} className="relative flex flex-col sm:flex-row">
                                    <dt className="flex items-start">
                                        <div className="flex-shrink-0 flex items-center justify-center h-12 w-12 rounded-lg bg-indigo-500 text-white shadow-md">
                                            {FEATURE_ICONS[feature.icon]}
                                        </div>
                                        <div className="ml-4 sm:ml-5">
                                            <p className="text-lg leading-6 font-semibold text-gray-900">{feature.name}</p>
                                            <dd className="mt-1 sm:mt-2 text-base text-gray-500">
                                                {feature.description}
                                            </dd>
                                        </div>
                                    </dt>
                                </div>
                            ))}
                        </dl>
                    </div>

                </div>
            </div>
        </div>
    );
};

export default Home;
