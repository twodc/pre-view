import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { createInterview, uploadResume, uploadPortfolio, startInterview } from '../api/interviewApi';
import Layout from '../components/Layout';

const POSITIONS = [
    { value: 'BACKEND', label: '백엔드' },
    { value: 'FRONTEND', label: '프론트엔드' },
    { value: 'FULLSTACK', label: '풀스택' },
    { value: 'DEVOPS', label: '데브옵스' },
    { value: 'DATA_ENGINEER', label: '데이터 엔지니어' },
    { value: 'AI_ML', label: 'AI/ML' },
    { value: 'IOS', label: 'iOS' },
    { value: 'ANDROID', label: 'Android' },
    { value: 'GAME', label: '게임' },
];

const LEVELS = [
    { value: 'NEWCOMER', label: '신입' },
    { value: 'JUNIOR', label: '주니어' },
    { value: 'MID', label: '미들' },
    { value: 'SENIOR', label: '시니어' },
];

// 백엔드 InterviewType enum과 일치 (FULL, TECHNICAL, PERSONALITY)
const TYPES = [
    { value: 'FULL', label: '전체 면접' },       // OPENING → TECHNICAL → PERSONALITY → CLOSING
    { value: 'TECHNICAL', label: '기술 면접' },  // TECHNICAL만 진행
    { value: 'PERSONALITY', label: '인성 면접' }, // PERSONALITY만 진행
];

const InterviewCreate = () => {
    const navigate = useNavigate();
    const [formData, setFormData] = useState({
        title: '',
        type: 'FULL',
        position: 'BACKEND',
        level: 'NEWCOMER',
        techStacks: '',
    });

    const [resumeFile, setResumeFile] = useState(null);
    const [portfolioFile, setPortfolioFile] = useState(null);
    const [isSubmitting, setIsSubmitting] = useState(false);
    const [error, setError] = useState('');

    const handleChange = (e) => {
        const { name, value } = e.target;
        setFormData(prev => ({ ...prev, [name]: value }));
    };

    const handleFileChange = (e, type) => {
        if (e.target.files && e.target.files[0]) {
            if (type === 'resume') {
                setResumeFile(e.target.files[0]);
            } else if (type === 'portfolio') {
                setPortfolioFile(e.target.files[0]);
            }
        }
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        setIsSubmitting(true);

        // Convert comma separated tech stacks to array
        const payload = {
            ...formData,
            techStacks: formData.techStacks.split(',').map(s => s.trim()).filter(s => s.length > 0)
        };

        try {
            // 1. 면접 생성
            const response = await createInterview(payload);
            const interviewId = response.data.id;

            // 2. 파일 업로드 (병렬 처리)
            const uploadPromises = [];
            if (resumeFile) {
                uploadPromises.push(uploadResume(interviewId, resumeFile));
            }
            if (portfolioFile) {
                uploadPromises.push(uploadPortfolio(interviewId, portfolioFile));
            }

            if (uploadPromises.length > 0) {
                await Promise.all(uploadPromises);
            }

            // 3. 면접 시작 API 호출 후 세션으로 바로 이동
            await startInterview(interviewId);
            navigate(`/interviews/${interviewId}/session`);
        } catch (error) {
            console.error('Failed to create interview', error);
            setError('면접 생성에 실패했습니다. 다시 시도해주세요.');
            setIsSubmitting(false);
        }
    };

    return (
        <Layout>
            <div className="max-w-2xl mx-auto bg-white p-8 rounded-lg shadow border border-gray-100">
                <h1 className="text-2xl font-bold mb-6 text-gray-900">새 면접 생성</h1>

                {/* 에러 메시지 */}
                {error && (
                    <div className="mb-4 p-3 bg-red-50 border border-red-200 rounded-lg">
                        <p className="text-sm text-red-600">{error}</p>
                    </div>
                )}

                <form onSubmit={handleSubmit} className="space-y-6">
                    <div>
                        <label className="block text-sm font-medium text-gray-700">제목</label>
                        <input
                            type="text"
                            name="title"
                            value={formData.title}
                            onChange={handleChange}
                            required
                            className="mt-1 block w-full rounded-md border-gray-300 bg-gray-50 p-2 shadow-sm focus:border-indigo-500 focus:ring-indigo-500 sm:text-sm border"
                            placeholder="예: 백엔드 신입 모의면접"
                        />
                    </div>

                    <div className="grid grid-cols-1 gap-6 sm:grid-cols-2">
                        <div>
                            <label className="block text-sm font-medium text-gray-700">포지션</label>
                            <select
                                name="position"
                                value={formData.position}
                                onChange={handleChange}
                                className="mt-1 block w-full rounded-md border-gray-300 bg-gray-50 p-2 shadow-sm focus:border-indigo-500 focus:ring-indigo-500 sm:text-sm border"
                            >
                                {POSITIONS.map(opt => (
                                    <option key={opt.value} value={opt.value}>{opt.label}</option>
                                ))}
                            </select>
                        </div>

                        <div>
                            <label className="block text-sm font-medium text-gray-700">경력 레벨</label>
                            <select
                                name="level"
                                value={formData.level}
                                onChange={handleChange}
                                className="mt-1 block w-full rounded-md border-gray-300 bg-gray-50 p-2 shadow-sm focus:border-indigo-500 focus:ring-indigo-500 sm:text-sm border"
                            >
                                {LEVELS.map(opt => (
                                    <option key={opt.value} value={opt.value}>{opt.label}</option>
                                ))}
                            </select>
                        </div>
                    </div>

                    <div>
                        <label className="block text-sm font-medium text-gray-700">면접 유형</label>
                        <select
                            name="type"
                            value={formData.type}
                            onChange={handleChange}
                            className="mt-1 block w-full rounded-md border-gray-300 bg-gray-50 p-2 shadow-sm focus:border-indigo-500 focus:ring-indigo-500 sm:text-sm border"
                        >
                            {TYPES.map(opt => (
                                <option key={opt.value} value={opt.value}>{opt.label}</option>
                            ))}
                        </select>
                    </div>

                    <div>
                        <label className="block text-sm font-medium text-gray-700">기술 스택 (쉼표로 구분)</label>
                        <input
                            type="text"
                            name="techStacks"
                            value={formData.techStacks}
                            onChange={handleChange}
                            className="mt-1 block w-full rounded-md border-gray-300 bg-gray-50 p-2 shadow-sm focus:border-indigo-500 focus:ring-indigo-500 sm:text-sm border"
                            placeholder="Java, Spring, MySQL"
                        />
                    </div>

                    <div className="grid grid-cols-1 gap-6 sm:grid-cols-2">
                        <div>
                            <label className="block text-sm font-medium text-gray-700">이력서 업로드 (PDF)</label>
                            <input
                                type="file"
                                accept=".pdf"
                                onChange={(e) => handleFileChange(e, 'resume')}
                                className="mt-1 block w-full text-sm text-gray-500 file:mr-4 file:py-2 file:px-4 file:rounded-md file:border-0 file:text-sm file:font-semibold file:bg-indigo-50 file:text-indigo-700 hover:file:bg-indigo-100"
                            />
                            <p className="mt-1 text-xs text-gray-500">
                                AI가 내용을 분석하여 질문을 생성합니다. (선택)
                            </p>
                        </div>
                        <div>
                            <label className="block text-sm font-medium text-gray-700">포트폴리오 업로드 (PDF)</label>
                            <input
                                type="file"
                                accept=".pdf"
                                onChange={(e) => handleFileChange(e, 'portfolio')}
                                className="mt-1 block w-full text-sm text-gray-500 file:mr-4 file:py-2 file:px-4 file:rounded-md file:border-0 file:text-sm file:font-semibold file:bg-indigo-50 file:text-indigo-700 hover:file:bg-indigo-100"
                            />
                            <p className="mt-1 text-xs text-gray-500">
                                프로젝트 경험 기반 질문 생성에 활용됩니다. (선택)
                            </p>
                        </div>
                    </div>

                    <div className="flex flex-col-reverse sm:flex-row sm:justify-end gap-3 pt-4">
                        <button
                            type="button"
                            onClick={() => navigate('/')}
                            disabled={isSubmitting}
                            className="w-full sm:w-auto inline-flex justify-center rounded-md border border-gray-300 bg-white py-2.5 px-4 text-sm font-medium text-gray-700 shadow-sm hover:bg-gray-50 focus:outline-none disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
                        >
                            취소
                        </button>
                        <button
                            type="submit"
                            disabled={isSubmitting}
                            className="w-full sm:w-auto inline-flex justify-center items-center rounded-md border border-transparent bg-indigo-600 py-2.5 px-6 text-sm font-medium text-white shadow-sm hover:bg-indigo-700 focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:ring-offset-2 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
                        >
                            {isSubmitting ? (
                                <>
                                    <svg className="animate-spin -ml-1 mr-2 h-4 w-4 text-white" fill="none" viewBox="0 0 24 24">
                                        <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                                        <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                                    </svg>
                                    면접 준비 중...
                                </>
                            ) : (
                                '면접 시작하기'
                            )}
                        </button>
                    </div>
                </form>
            </div>
        </Layout>
    );
};

export default InterviewCreate;
