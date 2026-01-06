import React, { useEffect, useState } from 'react';
import Layout from '../components/Layout';
import { useParams, Link } from 'react-router-dom';
import { getInterviewResult } from '../api/interviewApi';

// 로딩 메시지 배열
const LOADING_MESSAGES = [
    { title: "면접 내용을 수집하고 있습니다...", subtitle: "잠시만 기다려주세요" },
    { title: "AI가 답변을 분석하고 있습니다...", subtitle: "각 질문에 대한 피드백을 준비 중입니다" },
    { title: "강점과 개선점을 파악하고 있습니다...", subtitle: "거의 다 됐어요!" },
    { title: "맞춤형 추천 답변을 생성하고 있습니다...", subtitle: "최종 리포트를 정리 중입니다" },
];

const InterviewResult = () => {
    const { id } = useParams();
    const [result, setResult] = useState(null);
    const [loading, setLoading] = useState(true);
    const [loadingMessageIndex, setLoadingMessageIndex] = useState(0);
    const [dataFetched, setDataFetched] = useState(false);
    const [minLoadingComplete, setMinLoadingComplete] = useState(false);

    // API 호출 (최초 1회만)
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

    // 최소 로딩 시간 보장 (2초)
    useEffect(() => {
        const minLoadingTimer = setTimeout(() => {
            setMinLoadingComplete(true);
        }, 2000);

        return () => clearTimeout(minLoadingTimer);
    }, []);

    // 로딩 메시지 순환 (2초마다)
    useEffect(() => {
        if (!loading) return;

        const messageTimer = setInterval(() => {
            setLoadingMessageIndex(prev =>
                prev < LOADING_MESSAGES.length - 1 ? prev + 1 : prev
            );
        }, 2000);

        return () => clearInterval(messageTimer);
    }, [loading]);

    // 데이터 로딩 완료 + 최소 로딩 시간 경과 시 로딩 종료
    useEffect(() => {
        if (dataFetched && minLoadingComplete) {
            setLoading(false);
        }
    }, [dataFetched, minLoadingComplete]);

    if (loading) {
        const currentMessage = LOADING_MESSAGES[loadingMessageIndex];
        return (
            <Layout>
                <div className="flex flex-col items-center justify-center min-h-[60vh]">
                    {/* 로딩 애니메이션 */}
                    <div className="relative mb-8">
                        <div className="animate-spin rounded-full h-16 w-16 border-4 border-indigo-200 border-t-indigo-600"></div>
                        <div className="absolute inset-0 flex items-center justify-center">
                            <span className="text-2xl">📊</span>
                        </div>
                    </div>

                    {/* 메인 메시지 */}
                    <h2 className="text-xl font-semibold text-gray-800 mb-2 text-center transition-opacity duration-300">
                        {currentMessage.title}
                    </h2>
                    <p className="text-gray-500 text-center mb-6 transition-opacity duration-300">
                        {currentMessage.subtitle}
                    </p>

                    {/* 프로그레스 바 */}
                    <div className="w-64 bg-gray-200 rounded-full h-2 mb-4">
                        <div
                            className="bg-indigo-600 h-2 rounded-full transition-all duration-500"
                            style={{ width: `${((loadingMessageIndex + 1) / LOADING_MESSAGES.length) * 100}%` }}
                        ></div>
                    </div>

                    {/* 안내 문구 */}
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
                <div className="text-center py-10">
                    <h1 className="text-2xl font-bold text-red-600">결과 조회 실패</h1>
                    <p className="mt-4">결과를 불러오지 못했습니다. 다시 시도해주세요.</p>
                    <Link to="/" className="mt-6 inline-block text-indigo-600 hover:text-indigo-800">홈으로</Link>
                </div>
            </Layout>
        );
    }

    const { title, aiReport: report } = result;

    // Debug logging
    console.log("Interview Result Data:", result);

    if (!report) {
        return (
            <Layout>
                <div className="text-center py-10">
                    <h1 className="text-2xl font-bold text-red-600">리포트 데이터 없음</h1>
                    <p className="mt-4">AI 리포트 객체가 비어있습니다.</p>
                    <Link to="/dashboard" className="mt-6 inline-block text-indigo-600 hover:text-indigo-800">대시보드로 돌아가기</Link>
                </div>
            </Layout>
        );
    }

    return (
        <Layout>
            <div className="max-w-4xl mx-auto py-8 px-4">
                <div className="text-center mb-10">
                    <h1 className="text-3xl font-bold text-gray-900 mb-2">면접 분석 리포트</h1>
                    <p className="text-gray-500">{title}</p>
                </div>

                {/* Score Summary */}
                <div className="bg-white rounded-lg shadow-lg p-6 mb-8 text-center">
                    <h2 className="text-lg font-medium text-gray-700 mb-4">종합 역량 점수</h2>
                    <div className="flex justify-center items-end gap-2">
                        <span className="text-5xl font-bold text-indigo-600">{report.overallScore || 0}</span>
                        <span className="text-xl text-gray-400 mb-1">/ 10</span>
                    </div>
                </div>

                {/* Strengths & Weaknesses */}
                <div className="grid grid-cols-1 md:grid-cols-2 gap-6 mb-8">
                    <div className="bg-blue-50 rounded-lg p-6 border border-blue-100">
                        <h3 className="text-lg font-bold text-blue-800 mb-4 flex items-center">
                            <span className="mr-2">👍</span> 강점 (Strengths)
                        </h3>
                        <ul className="space-y-2">
                            {Array.isArray(report.strengths) && report.strengths.length > 0 ? (
                                report.strengths.map((str, idx) => (
                                    <li key={idx} className="flex items-start">
                                        <span className="text-blue-500 mr-2">•</span>
                                        <span className="text-blue-900 text-sm">{str}</span>
                                    </li>
                                ))
                            ) : (
                                <li className="text-gray-500 text-sm">분석된 강점이 없습니다.</li>
                            )}
                        </ul>
                    </div>
                    <div className="bg-orange-50 rounded-lg p-6 border border-orange-100">
                        <h3 className="text-lg font-bold text-orange-800 mb-4 flex items-center">
                            <span className="mr-2">💪</span> 보완점 (Improvements)
                        </h3>
                        <ul className="space-y-2">
                            {Array.isArray(report.improvements) && report.improvements.length > 0 ? (
                                report.improvements.map((wk, idx) => (
                                    <li key={idx} className="flex items-start">
                                        <span className="text-orange-500 mr-2">•</span>
                                        <span className="text-orange-900 text-sm">{wk}</span>
                                    </li>
                                ))
                            ) : (
                                <li className="text-gray-500 text-sm">분석된 보완점이 없습니다.</li>
                            )}
                        </ul>
                    </div>
                </div>

                {/* Recommended Topics - 백엔드 AiReportResponse의 recommendedTopics 필드 */}
                {Array.isArray(report.recommendedTopics) && report.recommendedTopics.length > 0 && (
                    <div className="bg-purple-50 rounded-lg p-6 border border-purple-100 mb-8">
                        <h3 className="text-lg font-bold text-purple-800 mb-4 flex items-center">
                            <span className="mr-2">📚</span> 추천 학습 주제
                        </h3>
                        <ul className="space-y-2">
                            {report.recommendedTopics.map((topic, idx) => (
                                <li key={idx} className="flex items-start">
                                    <span className="text-purple-500 mr-2">•</span>
                                    <span className="text-purple-900 text-sm">{topic}</span>
                                </li>
                            ))}
                        </ul>
                    </div>
                )}

                {/* Overall Feedback */}
                <div className="bg-white rounded-lg shadow p-6 mb-8">
                    <h3 className="text-lg font-bold text-gray-900 mb-4">총평</h3>
                    <p className="text-gray-700 whitespace-pre-wrap leading-relaxed">{report.summary || "총평을 생성하지 못했습니다."}</p>
                </div>

                {/* Question-by-Question Feedback */}
                {Array.isArray(report.questionFeedbacks) && report.questionFeedbacks.length > 0 && (
                    <div className="bg-white rounded-lg shadow p-6 mb-8">
                        <h3 className="text-lg font-bold text-gray-900 mb-6 flex items-center">
                            <span className="mr-2">📝</span> 질문별 상세 피드백
                        </h3>
                        <div className="space-y-6">
                            {report.questionFeedbacks.map((qf, idx) => (
                                <div key={idx} className="border border-gray-200 rounded-lg p-5">
                                    {/* Question Header */}
                                    <div className="flex justify-between items-start mb-3">
                                        <h4 className="font-semibold text-gray-800">Q{idx + 1}. {qf.question}</h4>
                                        <span className={`px-2 py-1 rounded text-sm font-medium ${
                                            qf.score >= 7 ? 'bg-green-100 text-green-800' :
                                            qf.score >= 5 ? 'bg-yellow-100 text-yellow-800' :
                                            'bg-red-100 text-red-800'
                                        }`}>
                                            {qf.score}/10
                                        </span>
                                    </div>

                                    {/* User Answer */}
                                    <div className="bg-gray-50 rounded p-3 mb-3">
                                        <p className="text-xs text-gray-500 mb-1">내 답변</p>
                                        <p className="text-sm text-gray-700">{qf.userAnswer}</p>
                                    </div>

                                    {/* Detailed Feedback */}
                                    <div className="bg-blue-50 rounded p-3 mb-3">
                                        <p className="text-xs text-blue-600 mb-1 font-medium">상세 피드백</p>
                                        <p className="text-sm text-blue-900">{qf.detailedFeedback}</p>
                                    </div>

                                    {/* Recommended Answer */}
                                    <div className="bg-green-50 rounded p-3">
                                        <p className="text-xs text-green-600 mb-1 font-medium">💡 추천 답변 예시</p>
                                        <p className="text-sm text-green-900">{qf.recommendedAnswer}</p>
                                    </div>
                                </div>
                            ))}
                        </div>
                    </div>
                )}

                <div className="text-center">
                    <Link
                        to="/dashboard"
                        className="inline-flex justify-center py-2 px-6 border border-transparent shadow-sm text-base font-medium rounded-md text-white bg-indigo-600 hover:bg-indigo-700 focus:outline-none"
                    >
                        목록으로 돌아가기
                    </Link>
                </div>
            </div>
        </Layout>
    );
};

export default InterviewResult;
