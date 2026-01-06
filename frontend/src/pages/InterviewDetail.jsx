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
        try {
            await startInterview(id);
            navigate(`/interviews/${id}/session`);
        } catch (error) {
            console.error('Failed to start interview', error);
            setStartError('면접 시작에 실패했습니다. 다시 시도해주세요.');
        }
    };

    if (loading) return <Layout><div className="flex justify-center py-12">Loading...</div></Layout>;
    if (!interview) return <Layout><div className="flex justify-center py-12">Interview not found</div></Layout>;

    return (
        <Layout>
            <div className="max-w-4xl mx-auto">
                {/* 에러 메시지 */}
                {startError && (
                    <div className="mb-4 p-3 bg-red-50 border border-red-200 rounded-lg">
                        <p className="text-sm text-red-600">{startError}</p>
                    </div>
                )}

                <div className="bg-white shadow overflow-hidden sm:rounded-lg">
                    {/* Header */}
                    <div className="px-4 py-5 sm:px-6 border-b border-gray-200 flex justify-between items-center">
                        <div>
                            <h3 className="text-xl leading-6 font-bold text-gray-900">{interview.title}</h3>
                            <p className="mt-1 max-w-2xl text-sm text-gray-500">
                                면접 상세 정보
                            </p>
                        </div>
                        <span className={`px-3 py-1 inline-flex text-xs leading-5 font-semibold rounded-full 
                            ${interview.status === 'DONE' ? 'bg-blue-100 text-blue-800' :
                                interview.status === 'IN_PROGRESS' ? 'bg-green-100 text-green-800' :
                                    'bg-gray-100 text-gray-800'}`}>
                            {interview.status}
                        </span>
                    </div>

                    {/* Content Grid */}
                    <div className="px-4 py-5 sm:p-6">
                        <div className="grid grid-cols-1 gap-y-6 gap-x-4 sm:grid-cols-2">

                            {/* Basic Info Card */}
                            <div className="sm:col-span-1 bg-gray-50 p-4 rounded-lg">
                                <h4 className="text-sm font-medium text-gray-500 uppercase tracking-wider mb-3">기본 정보</h4>
                                <dl className="space-y-2">
                                    <div className="flex justify-between">
                                        <dt className="text-sm font-medium text-gray-600">포지션</dt>
                                        <dd className="text-sm text-gray-900">{interview.positionDescription}</dd>
                                    </div>
                                    <div className="flex justify-between">
                                        <dt className="text-sm font-medium text-gray-600">경력 레벨</dt>
                                        <dd className="text-sm text-gray-900">{interview.levelDescription}</dd>
                                    </div>
                                    <div className="flex justify-between">
                                        <dt className="text-sm font-medium text-gray-600">면접 유형</dt>
                                        <dd className="text-sm text-gray-900">{interview.typeDescription}</dd>
                                    </div>
                                </dl>
                            </div>

                            {/* Tech Stack Card */}
                            <div className="sm:col-span-1 bg-gray-50 p-4 rounded-lg">
                                <h4 className="text-sm font-medium text-gray-500 uppercase tracking-wider mb-3">기술 스택</h4>
                                <div className="flex flex-wrap gap-2">
                                    {interview.techStacks.length > 0 ? (
                                        interview.techStacks.map((stack, idx) => (
                                            <span key={idx} className="inline-flex items-center px-2.5 py-0.5 rounded-md text-sm font-medium bg-indigo-100 text-indigo-800">
                                                {stack}
                                            </span>
                                        ))
                                    ) : (
                                        <span className="text-sm text-gray-400">지정된 기술 스택 없음</span>
                                    )}
                                </div>
                            </div>

                            {/* Upload Status Card */}
                            <div className="sm:col-span-2 bg-gray-50 p-4 rounded-lg">
                                <h4 className="text-sm font-medium text-gray-500 uppercase tracking-wider mb-3">제출 서류 상태</h4>
                                <div className="flex space-x-6">
                                    <div className="flex items-center">
                                        {interview.hasResume ? (
                                            <span className="flex items-center text-green-600 text-sm font-medium">
                                                <svg className="w-5 h-5 mr-1.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
                                                </svg>
                                                이력서 등록됨
                                            </span>
                                        ) : (
                                            <span className="flex items-center text-gray-400 text-sm">
                                                <svg className="w-5 h-5 mr-1.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M10 14l2-2m0 0l2-2m-2 2l-2-2m2 2l2 2m7-2a9 9 0 11-18 0 9 9 0 0118 0z" />
                                                </svg>
                                                이력서 미등록
                                            </span>
                                        )}
                                    </div>
                                    <div className="flex items-center">
                                        {interview.hasPortfolio ? (
                                            <span className="flex items-center text-green-600 text-sm font-medium">
                                                <svg className="w-5 h-5 mr-1.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
                                                </svg>
                                                포트폴리오 등록됨
                                            </span>
                                        ) : (
                                            <span className="flex items-center text-gray-400 text-sm">
                                                <svg className="w-5 h-5 mr-1.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M10 14l2-2m0 0l2-2m-2 2l-2-2m2 2l2 2m7-2a9 9 0 11-18 0 9 9 0 0118 0z" />
                                                </svg>
                                                포트폴리오 미등록
                                            </span>
                                        )}
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>

                    {/* Footer Actions */}
                    <div className="px-4 py-4 sm:px-6 bg-gray-50 border-t border-gray-200 flex justify-end space-x-3">
                        <button
                            onClick={() => navigate('/dashboard')}
                            className="inline-flex justify-center py-2 px-4 border border-gray-300 shadow-sm text-sm font-medium rounded-md text-gray-700 bg-white hover:bg-gray-50 focus:outline-none"
                        >
                            목록으로
                        </button>

                        {interview.status === 'READY' && (
                            <button
                                onClick={handleStart}
                                className="inline-flex justify-center py-2 px-4 border border-transparent shadow-sm text-sm font-medium rounded-md text-white bg-indigo-600 hover:bg-indigo-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500"
                            >
                                면접 시작하기
                            </button>
                        )}
                        {interview.status === 'IN_PROGRESS' && (
                            <button
                                onClick={() => navigate(`/interviews/${id}/session`)}
                                className="inline-flex justify-center py-2 px-4 border border-transparent shadow-sm text-sm font-medium rounded-md text-white bg-green-600 hover:bg-green-700 focus:outline-none"
                            >
                                면접 이어하기
                            </button>
                        )}
                        {interview.status === 'DONE' && (
                            <button
                                onClick={() => navigate(`/interviews/${id}/result`)}
                                className="inline-flex justify-center py-2 px-4 border border-transparent shadow-sm text-sm font-medium rounded-md text-white bg-blue-600 hover:bg-blue-700 focus:outline-none"
                            >
                                결과 리포트 보기
                            </button>
                        )}
                    </div>
                </div>
            </div>
        </Layout>
    );
};

export default InterviewDetail;
