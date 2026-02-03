import React from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { Button } from '../components/ui/button';
import {
    MessageSquare,
    CheckCircle,
    MessagesSquare,
    FileText,
    ArrowRight,
    Users,
    Award,
    Clock
} from 'lucide-react';

const FEATURES = [
    {
        title: '맞춤형 질문 생성',
        description: '이력서와 지원 포지션을 분석하여 실제 면접에서 나올 수 있는 질문을 AI가 생성합니다.',
        icon: MessageSquare,
        color: 'from-blue-500 to-cyan-400',
    },
    {
        title: '실시간 AI 피드백',
        description: '답변 직후 개선점과 모범 답안을 제시하여 즉각적인 학습이 가능합니다.',
        icon: CheckCircle,
        color: 'from-violet-500 to-blue-400',
    },
    {
        title: '심층 꼬리 질문',
        description: '실제 면접처럼 꼬리를 무는 질문으로 대응력과 논리력을 향상시킵니다.',
        icon: MessagesSquare,
        color: 'from-blue-600 to-indigo-400',
    },
    {
        title: '종합 역량 리포트',
        description: '면접 종료 후 강점과 보완점을 분석한 상세 리포트를 제공합니다.',
        icon: FileText,
        color: 'from-cyan-500 to-blue-400',
    },
];

const STATS = [
    { value: '10,000+', label: '면접 세션 완료', icon: Users },
    { value: '96%', label: '사용자 만족도', icon: Award },
    { value: '24/7', label: '언제든 이용 가능', icon: Clock },
];

const TESTIMONIALS = [
    {
        name: '김OO',
        company: '네이버 합격',
        comment: '실제 면접에서 나온 질문이 PreView에서 연습한 것과 거의 똑같았어요!',
        gradient: 'from-cyan-400 to-blue-500',
    },
    {
        name: '이OO',
        company: '카카오 합격',
        comment: 'AI 피드백 덕분에 제 답변의 문제점을 정확히 파악할 수 있었습니다.',
        gradient: 'from-violet-400 to-blue-500',
    },
    {
        name: '박OO',
        company: '라인 합격',
        comment: '꼬리질문 연습이 정말 도움됐어요. 실전에서 당황하지 않았습니다.',
        gradient: 'from-emerald-400 to-cyan-500',
    },
    {
        name: '최OO',
        company: '쿠팡 합격',
        comment: '체계적인 피드백으로 부족한 부분을 보완할 수 있었어요.',
        gradient: 'from-orange-400 to-pink-500',
    },
    {
        name: '정OO',
        company: '토스 합격',
        comment: '반복 연습으로 자신감이 생겼고, 면접에서 좋은 결과를 얻었습니다.',
        gradient: 'from-blue-400 to-indigo-500',
    },
    {
        name: '강OO',
        company: '배민 합격',
        comment: '종합 리포트가 정말 유용했어요. 객관적으로 제 실력을 파악할 수 있었습니다.',
        gradient: 'from-pink-400 to-rose-500',
    },
];

const Home = () => {
    const { isAuthenticated, logout } = useAuth();

    return (
        <div className="min-h-screen bg-white">
            {/* Navigation */}
            <header className="fixed top-0 left-0 right-0 z-50 bg-white/80 backdrop-blur-md border-b border-gray-100">
                <div className="max-w-6xl mx-auto px-5">
                    <div className="flex justify-between h-16 items-center">
                        <Link to="/" className="flex items-center">
                            <img src="/logo.png" alt="PreView" className="h-16 mix-blend-multiply" />
                        </Link>
                        <nav className="flex items-center gap-4">
                            {isAuthenticated ? (
                                <Button
                                    onClick={logout}
                                    className="bg-gradient-to-r from-blue-500 to-cyan-400 hover:from-blue-600 hover:to-cyan-500 text-white font-semibold rounded-lg px-5 h-10 shadow-lg shadow-blue-500/25 transition-all hover:shadow-xl hover:shadow-blue-500/30"
                                >
                                    로그아웃
                                </Button>
                            ) : (
                                <>
                                    <Link
                                        to="/login"
                                        className="text-sm font-medium text-gray-600 hover:text-gray-900 transition-colors px-3 py-2"
                                    >
                                        로그인
                                    </Link>
                                    <Link to="/signup">
                                        <Button className="bg-gradient-to-r from-blue-500 to-cyan-400 hover:from-blue-600 hover:to-cyan-500 text-white font-semibold rounded-lg px-5 h-10 shadow-lg shadow-blue-500/25 transition-all hover:shadow-xl hover:shadow-blue-500/30">
                                            회원가입
                                        </Button>
                                    </Link>
                                </>
                            )}
                        </nav>
                    </div>
                </div>
            </header>

            {/* Hero Section */}
            <section className="relative pt-32 pb-24 px-5 overflow-hidden">
                {/* Background Gradient */}
                <div className="absolute inset-0 bg-gradient-to-b from-blue-50/80 via-white to-white" />
                <div className="absolute top-0 left-1/2 -translate-x-1/2 w-[1000px] h-[600px] bg-gradient-to-br from-blue-400/20 via-cyan-300/10 to-transparent rounded-full blur-3xl" />
                <div className="absolute top-40 right-0 w-[400px] h-[400px] bg-gradient-to-bl from-violet-400/10 to-transparent rounded-full blur-3xl" />

                <div className="max-w-6xl mx-auto relative">
                    <div className="text-center max-w-3xl mx-auto">
                        {/* Badge */}
                        <div className="inline-flex items-center gap-2 px-4 py-2 rounded-full bg-blue-50 border border-blue-100 mb-8">
                            <span className="w-2 h-2 rounded-full bg-gradient-to-r from-blue-500 to-cyan-400" />
                            <span className="text-sm font-medium text-blue-600">AI 면접 코칭 플랫폼</span>
                        </div>

                        {/* Main Title */}
                        <h1 className="text-[44px] sm:text-[56px] lg:text-[65px] font-bold text-gray-900 leading-[1.15] tracking-tight mb-6">
                            면접이 두려우신가요?
                            <br />
                            <span className="text-transparent bg-clip-text bg-gradient-to-r from-blue-500 to-cyan-400">
                                AI와 함께 준비하세요
                            </span>
                        </h1>

                        {/* Subtitle */}
                        <p className="text-lg sm:text-xl text-gray-500 max-w-xl mx-auto mb-10 leading-relaxed">
                            실제 면접처럼 연습하고, 즉각적인 피드백으로
                            <br className="hidden sm:block" />
                            자신있게 면접장에 들어가세요.
                        </p>

                        {/* CTA Button */}
                        <div className="flex justify-center">
                            <Link to={isAuthenticated ? "/create" : "/signup"}>
                                <Button className="bg-gradient-to-r from-blue-500 to-cyan-400 hover:from-blue-600 hover:to-cyan-500 text-white font-semibold rounded-xl px-10 h-14 text-base shadow-lg shadow-blue-500/25 transition-all hover:shadow-xl hover:shadow-blue-500/30 hover:-translate-y-0.5">
                                    {isAuthenticated ? "지금 바로 면접 시작하기" : "지금 바로 면접 준비 시작하기"}
                                    <ArrowRight className="ml-2 h-5 w-5" />
                                </Button>
                            </Link>
                        </div>
                    </div>

                    {/* Stats */}
                    <div className="mt-20 grid grid-cols-1 sm:grid-cols-3 gap-6 max-w-3xl mx-auto">
                        {STATS.map((stat, idx) => (
                            <div
                                key={stat.label}
                                className="text-center p-6 rounded-2xl bg-white border border-gray-100 shadow-lg shadow-gray-100/50"
                            >
                                <div className="w-12 h-12 rounded-xl bg-gradient-to-br from-blue-500/10 to-cyan-400/10 flex items-center justify-center mx-auto mb-4">
                                    <stat.icon className="w-6 h-6 text-blue-500" />
                                </div>
                                <div className="text-3xl font-bold text-gray-900 mb-1">{stat.value}</div>
                                <div className="text-sm text-gray-500">{stat.label}</div>
                            </div>
                        ))}
                    </div>
                </div>
            </section>

            {/* Features Section with Background Effect */}
            <section className="py-24 px-5 relative overflow-hidden">
                {/* Background decorations */}
                <div className="absolute inset-0 bg-gradient-to-b from-white via-gray-50/50 to-gray-50" />
                <div className="absolute top-0 left-0 w-full h-full">
                    {/* Floating shapes */}
                    <div className="absolute top-20 left-[10%] w-72 h-72 bg-blue-100/40 rounded-full blur-3xl animate-pulse" style={{ animationDuration: '4s' }} />
                    <div className="absolute top-40 right-[15%] w-64 h-64 bg-cyan-100/40 rounded-full blur-3xl animate-pulse" style={{ animationDuration: '5s', animationDelay: '1s' }} />
                    <div className="absolute bottom-20 left-[20%] w-80 h-80 bg-violet-100/30 rounded-full blur-3xl animate-pulse" style={{ animationDuration: '6s', animationDelay: '2s' }} />
                    <div className="absolute bottom-40 right-[10%] w-56 h-56 bg-blue-100/30 rounded-full blur-3xl animate-pulse" style={{ animationDuration: '4.5s', animationDelay: '0.5s' }} />
                    {/* Grid pattern overlay */}
                    <div className="absolute inset-0 opacity-[0.015]" style={{
                        backgroundImage: `linear-gradient(#3b82f6 1px, transparent 1px), linear-gradient(90deg, #3b82f6 1px, transparent 1px)`,
                        backgroundSize: '60px 60px'
                    }} />
                </div>

                <div className="max-w-6xl mx-auto relative">
                    <div className="text-center mb-16">
                        <div className="inline-flex items-center gap-2 px-4 py-2 rounded-full bg-blue-50 border border-blue-100 mb-6">
                            <span className="text-sm font-medium text-blue-600">주요 기능</span>
                        </div>
                        <h2 className="text-3xl sm:text-4xl lg:text-[44px] font-bold text-gray-900 tracking-tight mb-4">
                            왜 PreView인가요?
                        </h2>
                        <p className="text-lg text-gray-500 max-w-xl mx-auto">
                            AI 기술로 실제 면접 환경을 재현하고,
                            <br className="hidden sm:block" />
                            맞춤형 피드백으로 효과적인 준비를 돕습니다.
                        </p>
                    </div>

                    <div className="grid sm:grid-cols-2 gap-6">
                        {FEATURES.map((feature, index) => (
                            <div
                                key={feature.title}
                                className="group p-8 rounded-3xl bg-white/80 backdrop-blur-sm border border-gray-100 shadow-lg shadow-gray-100/50 hover:shadow-xl hover:shadow-blue-100/50 transition-all duration-300 hover:-translate-y-1"
                            >
                                <div className={`w-14 h-14 rounded-2xl bg-gradient-to-br ${feature.color} flex items-center justify-center mb-6 shadow-lg shadow-blue-500/20`}>
                                    <feature.icon className="h-7 w-7 text-white" />
                                </div>
                                <h3 className="text-xl font-bold text-gray-900 mb-3">
                                    {feature.title}
                                </h3>
                                <p className="text-gray-500 leading-relaxed">
                                    {feature.description}
                                </p>
                            </div>
                        ))}
                    </div>
                </div>
            </section>

            {/* Social Proof Section - groupby style */}
            <section className="py-24 px-5 bg-gray-50">
                <div className="max-w-6xl mx-auto">
                    <div className="grid sm:grid-cols-2 gap-12 items-center">
                        {/* Left - Auto-scrolling testimonial cards */}
                        <div className="relative h-[480px] overflow-hidden order-2 sm:order-1">
                            {/* Gradient fade overlays */}
                            <div className="absolute top-0 left-0 right-0 h-20 bg-gradient-to-b from-gray-50 via-gray-50/80 to-transparent z-10 pointer-events-none" />
                            <div className="absolute bottom-0 left-0 right-0 h-20 bg-gradient-to-t from-gray-50 via-gray-50/80 to-transparent z-10 pointer-events-none" />

                            {/* Scrolling container */}
                            <div className="animate-scroll-up space-y-4">
                                {/* First set of testimonials */}
                                {TESTIMONIALS.map((testimonial, idx) => (
                                    <div
                                        key={`first-${idx}`}
                                        className="bg-white rounded-2xl p-5 border border-gray-100 shadow-sm hover:shadow-md hover:border-gray-200 transition-all duration-300"
                                    >
                                        <div className="flex items-center gap-3 mb-3">
                                            <div className={`w-11 h-11 rounded-xl bg-gradient-to-br ${testimonial.gradient} shadow-md flex items-center justify-center`}>
                                                <span className="text-white font-bold text-sm">{testimonial.name.charAt(0)}</span>
                                            </div>
                                            <div>
                                                <div className="font-semibold text-gray-900">{testimonial.name}</div>
                                                <div className="text-sm text-blue-500 font-medium">{testimonial.company}</div>
                                            </div>
                                        </div>
                                        <p className="text-gray-600 leading-relaxed">
                                            "{testimonial.comment}"
                                        </p>
                                    </div>
                                ))}
                                {/* Duplicated set for seamless loop */}
                                {TESTIMONIALS.map((testimonial, idx) => (
                                    <div
                                        key={`second-${idx}`}
                                        className="bg-white rounded-2xl p-5 border border-gray-100 shadow-sm hover:shadow-md hover:border-gray-200 transition-all duration-300"
                                    >
                                        <div className="flex items-center gap-3 mb-3">
                                            <div className={`w-11 h-11 rounded-xl bg-gradient-to-br ${testimonial.gradient} shadow-md flex items-center justify-center`}>
                                                <span className="text-white font-bold text-sm">{testimonial.name.charAt(0)}</span>
                                            </div>
                                            <div>
                                                <div className="font-semibold text-gray-900">{testimonial.name}</div>
                                                <div className="text-sm text-blue-500 font-medium">{testimonial.company}</div>
                                            </div>
                                        </div>
                                        <p className="text-gray-600 leading-relaxed">
                                            "{testimonial.comment}"
                                        </p>
                                    </div>
                                ))}
                            </div>
                        </div>

                        {/* Right - Text content */}
                        <div className="order-1 sm:order-2">
                            <h2 className="text-3xl sm:text-4xl lg:text-[44px] font-bold text-gray-900 tracking-tight mb-4 leading-tight">
                                실제 합격자들이 검증한
                                <span className="block mt-2 text-transparent bg-clip-text bg-gradient-to-r from-blue-500 to-cyan-500">
                                    AI 면접 코칭
                                </span>
                            </h2>
                            <p className="text-lg text-gray-500 leading-relaxed mb-8">
                                네이버, 카카오, 토스 등 대기업 합격자들의
                                생생한 후기를 확인하세요.
                            </p>
                            <Link to={isAuthenticated ? "/create" : "/signup"}>
                                <Button className="bg-gradient-to-r from-blue-500 to-cyan-400 hover:from-blue-600 hover:to-cyan-500 text-white font-semibold rounded-xl px-8 h-12 shadow-lg shadow-blue-500/25 transition-all hover:-translate-y-0.5">
                                    나도 시작하기
                                    <ArrowRight className="ml-2 h-5 w-5" />
                                </Button>
                            </Link>
                        </div>
                    </div>
                </div>
            </section>

            {/* CTA Section with Background Image */}
            <section className="py-24 px-5 bg-gray-50 relative overflow-hidden">
                {/* Background Image - 면접 이미지 자리 */}
                <div className="absolute inset-0 z-0">
                    {/* TODO: 나노 이미지로 교체 - /assets/interview-bg.png */}
                    <img
                        src="/interview-bg.png"
                        alt=""
                        className="w-full h-full object-cover opacity-10"
                        onError={(e) => e.target.style.display = 'none'}
                    />
                    {/* Overlay gradient for readability */}
                    <div className="absolute inset-0 bg-gradient-to-b from-gray-50 via-gray-50/95 to-gray-50" />
                </div>

                <div className="max-w-3xl mx-auto text-center relative z-10">
                    <h2 className="text-3xl sm:text-4xl font-bold text-gray-900 mb-4">
                        지금 바로 시작하세요
                    </h2>
                    <p className="text-lg text-gray-500 mb-10">
                        무료로 AI 면접 연습을 시작하고,
                        <br className="hidden sm:block" />
                        원하는 회사에 한 걸음 더 가까워지세요.
                    </p>
                    <Link to={isAuthenticated ? "/create" : "/signup"}>
                        <Button className="bg-gradient-to-r from-blue-500 to-cyan-400 hover:from-blue-600 hover:to-cyan-500 text-white font-semibold rounded-xl px-10 h-14 text-lg shadow-lg shadow-blue-500/25 transition-all hover:shadow-xl hover:shadow-blue-500/30 hover:-translate-y-0.5">
                            {isAuthenticated ? "새 면접 생성" : "무료 회원가입"}
                            <ArrowRight className="ml-2 h-5 w-5" />
                        </Button>
                    </Link>
                </div>
            </section>

            {/* Footer */}
            <footer className="py-12 px-5 border-t border-gray-100">
                <div className="max-w-6xl mx-auto flex flex-col sm:flex-row items-center justify-between gap-4">
                    <div className="flex items-center">
                        <img src="/logo.png" alt="PreView" className="h-10 mix-blend-multiply" />
                    </div>
                    <p className="text-sm text-gray-500">
                        © 2026 PreView. AI 기반 면접 연습 플랫폼
                    </p>
                </div>
            </footer>
        </div>
    );
};

export default Home;
