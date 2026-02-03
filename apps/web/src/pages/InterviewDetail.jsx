import React, { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { getInterview, startInterview } from '../api/interviewApi';
import Layout from '../components/Layout';

const InterviewDetail = () => {
    const { id } = useParams();
    const navigate = useNavigate();
    const [interview, setInterview] = useState(null);
    const [loading, setLoading] = useState(true);
    const [startError, setStartError] = useState('');
    const [starting, setStarting] = useState(false);

    useEffect(() => {
        const fetchInterview = async () => {
            try {
                const response = await getInterview(id);
                if (response.success) {
                    setInterview(response.data);
                }
            } catch (error) {
                console.error('Failed to fetch interview', error);
            } finally {
                setLoading(false);
            }
        };
        fetchInterview();
    }, [id]);

    const handleStart = async () => {
        setStartError('');
        setStarting(true);
        try {
            await startInterview(id);
            navigate(`/interviews/${id}/session`);
        } catch (error) {
            console.error('Failed to start interview', error);
            setStartError('면접 시작에 실패했습니다. 다시 시도해주세요.');
        } finally {
            setStarting(false);
        }
    };

    const getStatusBadge = (status) => {
        const styles = {
            READY: 'bg-gray-100 text-gray-700 border border-gray-200',
            IN_PROGRESS: 'bg-emerald-50 text-emerald-700 border border-emerald-200',
            DONE: 'bg-blue-50 text-blue-700 border border-blue-200'
        };
        const labels = {
            READY: '대기중',
            IN_PROGRESS: '진행중',
            DONE: '완료'
        };
        return (
            <span className={`px-3 py-1.5 text-sm font-medium rounded-full ${styles[status] || styles.READY}`}>
                {labels[status] || status}
            </span>
        );
    };

    if (loading) {
        return (
            <Layout>
                <div className="flex flex-col items-center justify-center py-20">
                    <div className="relative">
                        <div className="w-12 h-12 border-4 border-blue-100 border-t-blue-500 rounded-full animate-spin"></div>
                    </div>
                    <p className="mt-4 text-gray-500">로딩 중...</p>
                </div>
            </Layout>
        );
    }

    if (!interview) {
        return (
            <Layout>
                <div className="max-w-2xl mx-auto py-16 text-center">
                    <div className="bg-white rounded-2xl shadow-sm border border-gray-100 p-12">
                        <div className="w-16 h-16 bg-gray-100 rounded-full flex items-center justify-center mx-auto mb-6">
                            <svg className="w-8 h-8 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9.172 16.172a4 4 0 015.656 0M9 10h.01M15 10h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                            </svg>
                        </div>
                        <h2 className="text-xl font-bold text-gray-900 mb-2">면접을 찾을 수 없습니다</h2>
                        <p className="text-gray-500 mb-6">요청하신 면접 정보가 존재하지 않습니다.</p>
                        <button
                            onClick={() => navigate('/dashboard')}
                            className="px-6 py-3 bg-gradient-to-r from-blue-500 to-cyan-500 text-white font-medium rounded-xl hover:shadow-lg hover:shadow-blue-500/25 transition-all duration-200"
                        >
                            목록으로 돌아가기
                        </button>
                    </div>
                </div>
            </Layout>
        );
    }

    return (
        <Layout>
            <div className="max-w-4xl mx-auto">
                {/* 에러 메시지 */}
                {startError && (
                    <div className="mb-6 p-4 bg-red-50 border border-red-100 rounded-xl flex items-start gap-3">
                        <svg className="w-5 h-5 text-red-500 mt-0.5 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                        </svg>
                        <p className="text-red-700">{startError}</p>
                    </div>
                )}

                {/* 헤더 카드 */}
                <div className="bg-white rounded-2xl shadow-sm border border-gray-100 overflow-hidden mb-6">
                    <div className="bg-gradient-to-r from-blue-500 to-cyan-500 px-8 py-6">
                        <div className="flex items-start justify-between">
                            <div>
                                <h1 className="text-2xl font-bold text-white mb-2">{interview.title}</h1>
                                <p className="text-blue-100">면접 상세 정보</p>
                            </div>
                            {getStatusBadge(interview.status)}
                        </div>
                    </div>
                </div>

                {/* 정보 그리드 */}
                <div className="grid grid-cols-1 md:grid-cols-2 gap-6 mb-6">
                    {/* 기본 정보 카드 */}
                    <div className="bg-white rounded-2xl shadow-sm border border-gray-100 p-6 hover:shadow-md hover:-translate-y-0.5 transition-all duration-200">
                        <div className="flex items-center gap-3 mb-5">
                            <div className="w-10 h-10 bg-blue-50 rounded-xl flex items-center justify-center">
                                <svg className="w-5 h-5 text-blue-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                                </svg>
                            </div>
                            <h3 className="text-lg font-bold text-gray-900">기본 정보</h3>
                        </div>
                        <dl className="space-y-4">
                            <div className="flex items-center justify-between py-2 border-b border-gray-50">
                                <dt className="text-gray-500">포지션</dt>
                                <dd className="font-medium text-gray-900">{interview.positionDescription}</dd>
                            </div>
                            <div className="flex items-center justify-between py-2 border-b border-gray-50">
                                <dt className="text-gray-500">경력 레벨</dt>
                                <dd className="font-medium text-gray-900">{interview.levelDescription}</dd>
                            </div>
                            <div className="flex items-center justify-between py-2">
                                <dt className="text-gray-500">면접 유형</dt>
                                <dd className="font-medium text-gray-900">{interview.typeDescription}</dd>
                            </div>
                        </dl>
                    </div>

                    {/* 기술 스택 카드 */}
                    <div className="bg-white rounded-2xl shadow-sm border border-gray-100 p-6 hover:shadow-md hover:-translate-y-0.5 transition-all duration-200">
                        <div className="flex items-center gap-3 mb-5">
                            <div className="w-10 h-10 bg-cyan-50 rounded-xl flex items-center justify-center">
                                <svg className="w-5 h-5 text-cyan-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M10 20l4-16m4 4l4 4-4 4M6 16l-4-4 4-4" />
                                </svg>
                            </div>
                            <h3 className="text-lg font-bold text-gray-900">기술 스택</h3>
                        </div>
                        <div className="flex flex-wrap gap-2">
                            {interview.techStacks && interview.techStacks.length > 0 ? (
                                interview.techStacks.map((stack, idx) => (
                                    <span
                                        key={idx}
                                        className="px-3 py-1.5 bg-gradient-to-r from-blue-50 to-cyan-50 text-blue-700 text-sm font-medium rounded-lg border border-blue-100"
                                    >
                                        {stack}
                                    </span>
                                ))
                            ) : (
                                <p className="text-gray-400 text-sm">지정된 기술 스택이 없습니다</p>
                            )}
                        </div>
                    </div>
                </div>

                {/* 제출 서류 카드 */}
                <div className="bg-white rounded-2xl shadow-sm border border-gray-100 p-6 mb-6 hover:shadow-md hover:-translate-y-0.5 transition-all duration-200">
                    <div className="flex items-center gap-3 mb-5">
                        <div className="w-10 h-10 bg-purple-50 rounded-xl flex items-center justify-center">
                            <svg className="w-5 h-5 text-purple-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                            </svg>
                        </div>
                        <h3 className="text-lg font-bold text-gray-900">제출 서류</h3>
                    </div>
                    <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                        <div className={`flex items-center gap-3 p-4 rounded-xl border ${interview.hasResume ? 'bg-emerald-50 border-emerald-100' : 'bg-gray-50 border-gray-100'}`}>
                            {interview.hasResume ? (
                                <div className="w-8 h-8 bg-emerald-100 rounded-full flex items-center justify-center">
                                    <svg className="w-4 h-4 text-emerald-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
                                    </svg>
                                </div>
                            ) : (
                                <div className="w-8 h-8 bg-gray-200 rounded-full flex items-center justify-center">
                                    <svg className="w-4 h-4 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                                    </svg>
                                </div>
                            )}
                            <div>
                                <p className={`font-medium ${interview.hasResume ? 'text-emerald-700' : 'text-gray-500'}`}>
                                    이력서
                                </p>
                                <p className={`text-sm ${interview.hasResume ? 'text-emerald-600' : 'text-gray-400'}`}>
                                    {interview.hasResume ? '등록 완료' : '미등록'}
                                </p>
                            </div>
                        </div>
                        <div className={`flex items-center gap-3 p-4 rounded-xl border ${interview.hasPortfolio ? 'bg-emerald-50 border-emerald-100' : 'bg-gray-50 border-gray-100'}`}>
                            {interview.hasPortfolio ? (
                                <div className="w-8 h-8 bg-emerald-100 rounded-full flex items-center justify-center">
                                    <svg className="w-4 h-4 text-emerald-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
                                    </svg>
                                </div>
                            ) : (
                                <div className="w-8 h-8 bg-gray-200 rounded-full flex items-center justify-center">
                                    <svg className="w-4 h-4 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                                    </svg>
                                </div>
                            )}
                            <div>
                                <p className={`font-medium ${interview.hasPortfolio ? 'text-emerald-700' : 'text-gray-500'}`}>
                                    포트폴리오
                                </p>
                                <p className={`text-sm ${interview.hasPortfolio ? 'text-emerald-600' : 'text-gray-400'}`}>
                                    {interview.hasPortfolio ? '등록 완료' : '미등록'}
                                </p>
                            </div>
                        </div>
                    </div>
                </div>

                {/* 액션 버튼 */}
                <div className="flex justify-end gap-3">
                    <button
                        onClick={() => navigate('/dashboard')}
                        className="px-6 py-3 border border-gray-200 text-gray-700 font-medium rounded-xl hover:bg-gray-50 transition-all duration-200"
                    >
                        목록으로
                    </button>

                    {interview.status === 'READY' && (
                        <button
                            onClick={handleStart}
                            disabled={starting}
                            className="px-6 py-3 bg-gradient-to-r from-blue-500 to-cyan-500 text-white font-medium rounded-xl hover:shadow-lg hover:shadow-blue-500/25 hover:-translate-y-0.5 transition-all duration-200 disabled:opacity-50 disabled:cursor-not-allowed flex items-center gap-2"
                        >
                            {starting ? (
                                <>
                                    <svg className="w-5 h-5 animate-spin" fill="none" viewBox="0 0 24 24">
                                        <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                                        <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                                    </svg>
                                    시작 중...
                                </>
                            ) : (
                                <>
                                    <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M14.752 11.168l-3.197-2.132A1 1 0 0010 9.87v4.263a1 1 0 001.555.832l3.197-2.132a1 1 0 000-1.664z" />
                                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                                    </svg>
                                    면접 시작하기
                                </>
                            )}
                        </button>
                    )}

                    {interview.status === 'IN_PROGRESS' && (
                        <button
                            onClick={() => navigate(`/interviews/${id}/session`)}
                            className="px-6 py-3 bg-gradient-to-r from-emerald-500 to-teal-500 text-white font-medium rounded-xl hover:shadow-lg hover:shadow-emerald-500/25 hover:-translate-y-0.5 transition-all duration-200 flex items-center gap-2"
                        >
                            <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 5l7 7-7 7M5 5l7 7-7 7" />
                            </svg>
                            면접 이어하기
                        </button>
                    )}

                    {interview.status === 'DONE' && (
                        <button
                            onClick={() => navigate(`/interviews/${id}/result`)}
                            className="px-6 py-3 bg-gradient-to-r from-blue-500 to-cyan-500 text-white font-medium rounded-xl hover:shadow-lg hover:shadow-blue-500/25 hover:-translate-y-0.5 transition-all duration-200 flex items-center gap-2"
                        >
                            <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 17v-2m3 2v-4m3 4v-6m2 10H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                            </svg>
                            결과 리포트 보기
                        </button>
                    )}
                </div>
            </div>
        </Layout>
    );
};

export default InterviewDetail;
