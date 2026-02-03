import React, { useEffect, useState } from 'react';
import Layout from '../components/Layout';
import { useParams, Link, useNavigate } from 'react-router-dom';
import { getInterviewResult } from '../api/interviewApi';

const LOADING_MESSAGES = [
    { title: "면접 내용을 수집하고 있습니다...", subtitle: "잠시만 기다려주세요" },
    { title: "AI가 답변을 분석하고 있습니다...", subtitle: "각 질문에 대한 피드백을 준비 중입니다" },
    { title: "강점과 개선점을 파악하고 있습니다...", subtitle: "거의 다 됐어요!" },
    { title: "맞춤형 추천 답변을 생성하고 있습니다...", subtitle: "최종 리포트를 정리 중입니다" },
];

const InterviewResult = () => {
    const { id } = useParams();
    const navigate = useNavigate();
    const [result, setResult] = useState(null);
    const [loading, setLoading] = useState(true);
    const [loadingMessageIndex, setLoadingMessageIndex] = useState(0);
    const [dataFetched, setDataFetched] = useState(false);
    const [minLoadingComplete, setMinLoadingComplete] = useState(false);

    useEffect(() => {
        const fetchResult = async () => {
            try {
                const response = await getInterviewResult(id);
                if (response.success) {
                    setResult(response.data);
                }
            } catch (error) {
                console.error("Failed to fetch result", error);
            } finally {
                setDataFetched(true);
            }
        };
        fetchResult();
    }, [id]);

    useEffect(() => {
        const minLoadingTimer = setTimeout(() => {
            setMinLoadingComplete(true);
        }, 2000);
        return () => clearTimeout(minLoadingTimer);
    }, []);

    useEffect(() => {
        if (!loading) return;
        const messageTimer = setInterval(() => {
            setLoadingMessageIndex(prev =>
                prev < LOADING_MESSAGES.length - 1 ? prev + 1 : prev
            );
        }, 2000);
        return () => clearInterval(messageTimer);
    }, [loading]);

    useEffect(() => {
        if (dataFetched && minLoadingComplete) {
            setLoading(false);
        }
    }, [dataFetched, minLoadingComplete]);

    const getScoreColor = (score) => {
        if (score >= 8) return 'from-emerald-500 to-teal-500';
        if (score >= 6) return 'from-blue-500 to-cyan-500';
        if (score >= 4) return 'from-yellow-500 to-orange-500';
        return 'from-red-500 to-pink-500';
    };

    const getScoreLabel = (score) => {
        if (score >= 9) return '탁월함';
        if (score >= 8) return '우수함';
        if (score >= 7) return '양호함';
        if (score >= 6) return '보통';
        if (score >= 5) return '개선 필요';
        return '노력 필요';
    };

    if (loading) {
        const currentMessage = LOADING_MESSAGES[loadingMessageIndex];
        return (
            <Layout>
                <div className="flex flex-col items-center justify-center min-h-[60vh]">
                    <div className="relative mb-8">
                        <div className="w-20 h-20 border-4 border-blue-100 border-t-blue-500 rounded-full animate-spin"></div>
                        <div className="absolute inset-0 flex items-center justify-center">
                            <span className="text-2xl">📊</span>
                        </div>
                    </div>
                    <h2 className="text-xl font-bold text-gray-900 mb-2 text-center">
                        {currentMessage.title}
                    </h2>
                    <p className="text-gray-500 text-center mb-8">
                        {currentMessage.subtitle}
                    </p>
                    <div className="w-72 bg-gray-100 rounded-full h-2 mb-4">
                        <div
                            className="bg-gradient-to-r from-blue-500 to-cyan-500 h-2 rounded-full transition-all duration-500"
                            style={{ width: `${((loadingMessageIndex + 1) / LOADING_MESSAGES.length) * 100}%` }}
                        ></div>
                    </div>
                    <div className="mt-6 text-center text-sm text-gray-400">
                        <p>면접 결과를 상세하게 분석하고 있습니다</p>
                        <p className="mt-1">질문별 맞춤 피드백과 추천 답변이 곧 제공됩니다</p>
                    </div>
                </div>
            </Layout>
        );
    }

    if (!result) {
        return (
            <Layout>
                <div className="max-w-2xl mx-auto py-16 text-center">
                    <div className="bg-white rounded-2xl shadow-sm border border-gray-100 p-12">
                        <div className="w-16 h-16 bg-red-50 rounded-full flex items-center justify-center mx-auto mb-6">
                            <svg className="w-8 h-8 text-red-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
                            </svg>
                        </div>
                        <h2 className="text-xl font-bold text-gray-900 mb-2">결과 조회 실패</h2>
                        <p className="text-gray-500 mb-6">결과를 불러오지 못했습니다. 다시 시도해주세요.</p>
                        <Link
                            to="/"
                            className="px-6 py-3 bg-gradient-to-r from-blue-500 to-cyan-500 text-white font-medium rounded-xl hover:shadow-lg hover:shadow-blue-500/25 transition-all duration-200 inline-block"
                        >
                            홈으로 돌아가기
                        </Link>
                    </div>
                </div>
            </Layout>
        );
    }

    const { title, aiReport: report } = result;

    if (!report) {
        return (
            <Layout>
                <div className="max-w-2xl mx-auto py-16 text-center">
                    <div className="bg-white rounded-2xl shadow-sm border border-gray-100 p-12">
                        <div className="w-16 h-16 bg-yellow-50 rounded-full flex items-center justify-center mx-auto mb-6">
                            <svg className="w-8 h-8 text-yellow-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                            </svg>
                        </div>
                        <h2 className="text-xl font-bold text-gray-900 mb-2">리포트 준비 중</h2>
                        <p className="text-gray-500 mb-6">AI 리포트가 아직 생성되지 않았습니다.</p>
                        <Link
                            to="/dashboard"
                            className="px-6 py-3 bg-gradient-to-r from-blue-500 to-cyan-500 text-white font-medium rounded-xl hover:shadow-lg hover:shadow-blue-500/25 transition-all duration-200 inline-block"
                        >
                            대시보드로 돌아가기
                        </Link>
                    </div>
                </div>
            </Layout>
        );
    }

    return (
        <Layout>
            <div className="max-w-4xl mx-auto">
                {/* 헤더 */}
                <div className="text-center mb-10">
                    <div className="inline-flex items-center gap-2 px-4 py-2 bg-blue-50 text-blue-700 rounded-full text-sm font-medium mb-4">
                        <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
                        </svg>
                        분석 완료
                    </div>
                    <h1 className="text-3xl font-bold text-gray-900 mb-2">면접 분석 리포트</h1>
                    <p className="text-gray-500">{title}</p>
                </div>

                {/* 종합 점수 카드 */}
                <div className="bg-white rounded-2xl shadow-sm border border-gray-100 p-8 mb-8 text-center">
                    <h2 className="text-lg font-medium text-gray-500 mb-6">종합 역량 점수</h2>
                    <div className="relative inline-flex items-center justify-center mb-4">
                        <div className={`w-32 h-32 rounded-full bg-gradient-to-r ${getScoreColor(report.overallScore)} flex items-center justify-center`}>
                            <div className="w-28 h-28 rounded-full bg-white flex items-center justify-center">
                                <span className={`text-4xl font-bold bg-gradient-to-r ${getScoreColor(report.overallScore)} bg-clip-text text-transparent`}>
                                    {report.overallScore || 0}
                                </span>
                            </div>
                        </div>
                    </div>
                    <p className={`text-lg font-medium bg-gradient-to-r ${getScoreColor(report.overallScore)} bg-clip-text text-transparent`}>
                        {getScoreLabel(report.overallScore)}
                    </p>
                </div>

                {/* 강점 & 보완점 */}
                <div className="grid grid-cols-1 md:grid-cols-2 gap-6 mb-8">
                    {/* 강점 */}
                    <div className="bg-white rounded-2xl shadow-sm border border-gray-100 p-6 hover:shadow-md hover:-translate-y-0.5 transition-all duration-200">
                        <div className="flex items-center gap-3 mb-5">
                            <div className="w-10 h-10 bg-blue-50 rounded-xl flex items-center justify-center">
                                <svg className="w-5 h-5 text-blue-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M14 10h4.764a2 2 0 011.789 2.894l-3.5 7A2 2 0 0115.263 21h-4.017c-.163 0-.326-.02-.485-.06L7 20m7-10V5a2 2 0 00-2-2h-.095c-.5 0-.905.405-.905.905 0 .714-.211 1.412-.608 2.006L7 11v9m7-10h-2M7 20H5a2 2 0 01-2-2v-6a2 2 0 012-2h2.5" />
                                </svg>
                            </div>
                            <h3 className="text-lg font-bold text-gray-900">강점</h3>
                        </div>
                        <ul className="space-y-3">
                            {Array.isArray(report.strengths) && report.strengths.length > 0 ? (
                                report.strengths.map((str, idx) => (
                                    <li key={idx} className="flex items-start gap-3">
                                        <div className="w-5 h-5 bg-blue-100 rounded-full flex items-center justify-center flex-shrink-0 mt-0.5">
                                            <svg className="w-3 h-3 text-blue-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={3} d="M5 13l4 4L19 7" />
                                            </svg>
                                        </div>
                                        <span className="text-gray-700">{str}</span>
                                    </li>
                                ))
                            ) : (
                                <li className="text-gray-400">분석된 강점이 없습니다.</li>
                            )}
                        </ul>
                    </div>

                    {/* 보완점 */}
                    <div className="bg-white rounded-2xl shadow-sm border border-gray-100 p-6 hover:shadow-md hover:-translate-y-0.5 transition-all duration-200">
                        <div className="flex items-center gap-3 mb-5">
                            <div className="w-10 h-10 bg-orange-50 rounded-xl flex items-center justify-center">
                                <svg className="w-5 h-5 text-orange-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 10V3L4 14h7v7l9-11h-7z" />
                                </svg>
                            </div>
                            <h3 className="text-lg font-bold text-gray-900">보완점</h3>
                        </div>
                        <ul className="space-y-3">
                            {Array.isArray(report.improvements) && report.improvements.length > 0 ? (
                                report.improvements.map((item, idx) => (
                                    <li key={idx} className="flex items-start gap-3">
                                        <div className="w-5 h-5 bg-orange-100 rounded-full flex items-center justify-center flex-shrink-0 mt-0.5">
                                            <svg className="w-3 h-3 text-orange-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={3} d="M12 4v16m8-8H4" />
                                            </svg>
                                        </div>
                                        <span className="text-gray-700">{item}</span>
                                    </li>
                                ))
                            ) : (
                                <li className="text-gray-400">분석된 보완점이 없습니다.</li>
                            )}
                        </ul>
                    </div>
                </div>

                {/* 추천 학습 주제 */}
                {Array.isArray(report.recommendedTopics) && report.recommendedTopics.length > 0 && (
                    <div className="bg-white rounded-2xl shadow-sm border border-gray-100 p-6 mb-8 hover:shadow-md hover:-translate-y-0.5 transition-all duration-200">
                        <div className="flex items-center gap-3 mb-5">
                            <div className="w-10 h-10 bg-purple-50 rounded-xl flex items-center justify-center">
                                <svg className="w-5 h-5 text-purple-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.747 0 3.332.477 4.5 1.253v13C19.832 18.477 18.247 18 16.5 18c-1.746 0-3.332.477-4.5 1.253" />
                                </svg>
                            </div>
                            <h3 className="text-lg font-bold text-gray-900">추천 학습 주제</h3>
                        </div>
                        <div className="flex flex-wrap gap-2">
                            {report.recommendedTopics.map((topic, idx) => (
                                <span
                                    key={idx}
                                    className="px-4 py-2 bg-gradient-to-r from-purple-50 to-pink-50 text-purple-700 rounded-xl border border-purple-100 text-sm font-medium"
                                >
                                    {topic}
                                </span>
                            ))}
                        </div>
                    </div>
                )}

                {/* 총평 */}
                <div className="bg-white rounded-2xl shadow-sm border border-gray-100 p-6 mb-8">
                    <div className="flex items-center gap-3 mb-5">
                        <div className="w-10 h-10 bg-gradient-to-r from-blue-500 to-cyan-500 rounded-xl flex items-center justify-center">
                            <svg className="w-5 h-5 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                            </svg>
                        </div>
                        <h3 className="text-lg font-bold text-gray-900">총평</h3>
                    </div>
                    <p className="text-gray-700 leading-relaxed whitespace-pre-wrap">
                        {report.summary || "총평을 생성하지 못했습니다."}
                    </p>
                </div>

                {/* 질문별 상세 피드백 */}
                {Array.isArray(report.questionFeedbacks) && report.questionFeedbacks.length > 0 && (
                    <div className="bg-white rounded-2xl shadow-sm border border-gray-100 p-6 mb-8">
                        <div className="flex items-center gap-3 mb-6">
                            <div className="w-10 h-10 bg-cyan-50 rounded-xl flex items-center justify-center">
                                <svg className="w-5 h-5 text-cyan-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8.228 9c.549-1.165 2.03-2 3.772-2 2.21 0 4 1.343 4 3 0 1.4-1.278 2.575-3.006 2.907-.542.104-.994.54-.994 1.093m0 3h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                                </svg>
                            </div>
                            <h3 className="text-lg font-bold text-gray-900">질문별 상세 피드백</h3>
                        </div>
                        <div className="space-y-6">
                            {report.questionFeedbacks.map((qf, idx) => (
                                <div key={idx} className="border border-gray-100 rounded-xl p-5 hover:border-gray-200 transition-colors">
                                    {/* 질문 헤더 */}
                                    <div className="flex items-start justify-between gap-4 mb-4">
                                        <div className="flex items-start gap-3">
                                            <span className="flex-shrink-0 w-8 h-8 bg-gradient-to-r from-blue-500 to-cyan-500 rounded-lg flex items-center justify-center text-white font-bold text-sm">
                                                {idx + 1}
                                            </span>
                                            <h4 className="font-semibold text-gray-900 leading-relaxed">{qf.question}</h4>
                                        </div>
                                        <div className={`flex-shrink-0 px-3 py-1.5 rounded-lg font-bold text-sm bg-gradient-to-r ${getScoreColor(qf.score)} text-white`}>
                                            {qf.score}/10
                                        </div>
                                    </div>

                                    {/* 내 답변 */}
                                    <div className="bg-gray-50 rounded-xl p-4 mb-3">
                                        <p className="text-xs text-gray-500 mb-2 font-medium">내 답변</p>
                                        <p className="text-gray-700 text-sm leading-relaxed">{qf.userAnswer}</p>
                                    </div>

                                    {/* 상세 피드백 */}
                                    <div className="bg-blue-50 rounded-xl p-4 mb-3 border border-blue-100">
                                        <p className="text-xs text-blue-600 mb-2 font-medium flex items-center gap-1">
                                            <svg className="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9.663 17h4.673M12 3v1m6.364 1.636l-.707.707M21 12h-1M4 12H3m3.343-5.657l-.707-.707m2.828 9.9a5 5 0 117.072 0l-.548.547A3.374 3.374 0 0014 18.469V19a2 2 0 11-4 0v-.531c0-.895-.356-1.754-.988-2.386l-.548-.547z" />
                                            </svg>
                                            상세 피드백
                                        </p>
                                        <p className="text-blue-900 text-sm leading-relaxed">{qf.detailedFeedback}</p>
                                    </div>

                                    {/* 추천 답변 */}
                                    <div className="bg-emerald-50 rounded-xl p-4 border border-emerald-100">
                                        <p className="text-xs text-emerald-600 mb-2 font-medium flex items-center gap-1">
                                            <svg className="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9.663 17h4.673M12 3v1m6.364 1.636l-.707.707M21 12h-1M4 12H3m3.343-5.657l-.707-.707m2.828 9.9a5 5 0 117.072 0l-.548.547A3.374 3.374 0 0014 18.469V19a2 2 0 11-4 0v-.531c0-.895-.356-1.754-.988-2.386l-.548-.547z" />
                                            </svg>
                                            추천 답변 예시
                                        </p>
                                        <p className="text-emerald-900 text-sm leading-relaxed">{qf.recommendedAnswer}</p>
                                    </div>
                                </div>
                            ))}
                        </div>
                    </div>
                )}

                {/* 액션 버튼 */}
                <div className="flex flex-col sm:flex-row justify-center gap-4">
                    <button
                        onClick={() => navigate('/interviews/create')}
                        className="px-8 py-3 bg-gradient-to-r from-blue-500 to-cyan-500 text-white font-medium rounded-xl hover:shadow-lg hover:shadow-blue-500/25 hover:-translate-y-0.5 transition-all duration-200 flex items-center justify-center gap-2"
                    >
                        <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v16m8-8H4" />
                        </svg>
                        새 면접 시작하기
                    </button>
                    <Link
                        to="/dashboard"
                        className="px-8 py-3 border border-gray-200 text-gray-700 font-medium rounded-xl hover:bg-gray-50 transition-all duration-200 flex items-center justify-center gap-2"
                    >
                        <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 6h16M4 10h16M4 14h16M4 18h16" />
                        </svg>
                        목록으로 돌아가기
                    </Link>
                </div>
            </div>
        </Layout>
    );
};

export default InterviewResult;
