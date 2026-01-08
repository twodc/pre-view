import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { createInterview, uploadResume, uploadPortfolio, startInterview } from '../api/interviewApi';
import Layout from '../components/Layout';
import { Button } from '../components/ui/button';
import { FileText, Upload, Briefcase, Code, ArrowRight, AlertCircle, Loader2 } from 'lucide-react';

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

const TYPES = [
    { value: 'FULL', label: '전체 면접', desc: '기술 + 인성 면접' },
    { value: 'TECHNICAL', label: '기술 면접', desc: '기술 질문만' },
    { value: 'PERSONALITY', label: '인성 면접', desc: '인성 질문만' },
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

        const payload = {
            ...formData,
            techStacks: formData.techStacks.split(',').map(s => s.trim()).filter(s => s.length > 0)
        };

        try {
            const response = await createInterview(payload);
            const interviewId = response.data.id;

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
            <div className="max-w-2xl mx-auto">
                {/* Header */}
                <div className="mb-8">
                    <h1 className="text-3xl font-bold text-gray-900 mb-2">새 면접 생성</h1>
                    <p className="text-gray-500">면접 정보를 입력하고 AI와 함께 연습을 시작하세요.</p>
                </div>

                {/* Error Message */}
                {error && (
                    <div className="mb-6 p-4 rounded-xl flex items-start gap-3 bg-red-50 border border-red-200">
                        <AlertCircle className="w-5 h-5 flex-shrink-0 mt-0.5 text-red-500" />
                        <p className="text-sm font-medium text-red-600">{error}</p>
                    </div>
                )}

                <form onSubmit={handleSubmit} className="space-y-6">
                    {/* Title */}
                    <div className="bg-white rounded-2xl border border-gray-100 shadow-lg shadow-gray-100/50 p-6">
                        <label className="block text-sm font-medium text-gray-700 mb-2">
                            <FileText className="w-4 h-4 inline mr-2" />
                            면접 제목
                        </label>
                        <input
                            type="text"
                            name="title"
                            value={formData.title}
                            onChange={handleChange}
                            required
                            className="block w-full px-4 py-3 border border-gray-200 rounded-xl bg-gray-50 focus:bg-white focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-all"
                            placeholder="예: 네이버 백엔드 신입 면접 준비"
                        />
                    </div>

                    {/* Position & Level */}
                    <div className="bg-white rounded-2xl border border-gray-100 shadow-lg shadow-gray-100/50 p-6">
                        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                            <div>
                                <label className="block text-sm font-medium text-gray-700 mb-2">
                                    <Briefcase className="w-4 h-4 inline mr-2" />
                                    포지션
                                </label>
                                <select
                                    name="position"
                                    value={formData.position}
                                    onChange={handleChange}
                                    className="block w-full px-4 py-3 border border-gray-200 rounded-xl bg-gray-50 focus:bg-white focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-all"
                                >
                                    {POSITIONS.map(opt => (
                                        <option key={opt.value} value={opt.value}>{opt.label}</option>
                                    ))}
                                </select>
                            </div>

                            <div>
                                <label className="block text-sm font-medium text-gray-700 mb-2">
                                    경력 레벨
                                </label>
                                <select
                                    name="level"
                                    value={formData.level}
                                    onChange={handleChange}
                                    className="block w-full px-4 py-3 border border-gray-200 rounded-xl bg-gray-50 focus:bg-white focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-all"
                                >
                                    {LEVELS.map(opt => (
                                        <option key={opt.value} value={opt.value}>{opt.label}</option>
                                    ))}
                                </select>
                            </div>
                        </div>
                    </div>

                    {/* Interview Type */}
                    <div className="bg-white rounded-2xl border border-gray-100 shadow-lg shadow-gray-100/50 p-6">
                        <label className="block text-sm font-medium text-gray-700 mb-3">
                            면접 유형
                        </label>
                        <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
                            {TYPES.map(type => (
                                <label
                                    key={type.value}
                                    className={`relative flex flex-col p-4 rounded-xl border-2 cursor-pointer transition-all ${
                                        formData.type === type.value
                                            ? 'border-blue-500 bg-blue-50'
                                            : 'border-gray-200 hover:border-gray-300 bg-gray-50'
                                    }`}
                                >
                                    <input
                                        type="radio"
                                        name="type"
                                        value={type.value}
                                        checked={formData.type === type.value}
                                        onChange={handleChange}
                                        className="sr-only"
                                    />
                                    <span className={`font-medium ${formData.type === type.value ? 'text-blue-700' : 'text-gray-900'}`}>
                                        {type.label}
                                    </span>
                                    <span className={`text-xs mt-1 ${formData.type === type.value ? 'text-blue-600' : 'text-gray-500'}`}>
                                        {type.desc}
                                    </span>
                                </label>
                            ))}
                        </div>
                    </div>

                    {/* Tech Stacks */}
                    <div className="bg-white rounded-2xl border border-gray-100 shadow-lg shadow-gray-100/50 p-6">
                        <label className="block text-sm font-medium text-gray-700 mb-2">
                            <Code className="w-4 h-4 inline mr-2" />
                            기술 스택 (쉼표로 구분)
                        </label>
                        <input
                            type="text"
                            name="techStacks"
                            value={formData.techStacks}
                            onChange={handleChange}
                            className="block w-full px-4 py-3 border border-gray-200 rounded-xl bg-gray-50 focus:bg-white focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-all"
                            placeholder="Java, Spring Boot, MySQL, Redis"
                        />
                    </div>

                    {/* File Uploads */}
                    <div className="bg-white rounded-2xl border border-gray-100 shadow-lg shadow-gray-100/50 p-6">
                        <label className="block text-sm font-medium text-gray-700 mb-4">
                            <Upload className="w-4 h-4 inline mr-2" />
                            서류 업로드 (선택)
                        </label>
                        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                            <div className="relative">
                                <input
                                    type="file"
                                    accept=".pdf"
                                    onChange={(e) => handleFileChange(e, 'resume')}
                                    className="absolute inset-0 w-full h-full opacity-0 cursor-pointer"
                                />
                                <div className={`flex flex-col items-center justify-center p-6 border-2 border-dashed rounded-xl transition-all ${
                                    resumeFile ? 'border-blue-400 bg-blue-50' : 'border-gray-200 hover:border-gray-300'
                                }`}>
                                    <FileText className={`w-8 h-8 mb-2 ${resumeFile ? 'text-blue-500' : 'text-gray-400'}`} />
                                    <span className={`text-sm font-medium ${resumeFile ? 'text-blue-600' : 'text-gray-600'}`}>
                                        {resumeFile ? resumeFile.name : '이력서 (PDF)'}
                                    </span>
                                    <span className="text-xs text-gray-400 mt-1">클릭하여 업로드</span>
                                </div>
                            </div>
                            <div className="relative">
                                <input
                                    type="file"
                                    accept=".pdf"
                                    onChange={(e) => handleFileChange(e, 'portfolio')}
                                    className="absolute inset-0 w-full h-full opacity-0 cursor-pointer"
                                />
                                <div className={`flex flex-col items-center justify-center p-6 border-2 border-dashed rounded-xl transition-all ${
                                    portfolioFile ? 'border-blue-400 bg-blue-50' : 'border-gray-200 hover:border-gray-300'
                                }`}>
                                    <FileText className={`w-8 h-8 mb-2 ${portfolioFile ? 'text-blue-500' : 'text-gray-400'}`} />
                                    <span className={`text-sm font-medium ${portfolioFile ? 'text-blue-600' : 'text-gray-600'}`}>
                                        {portfolioFile ? portfolioFile.name : '포트폴리오 (PDF)'}
                                    </span>
                                    <span className="text-xs text-gray-400 mt-1">클릭하여 업로드</span>
                                </div>
                            </div>
                        </div>
                        <p className="text-xs text-gray-400 mt-3">
                            AI가 서류 내용을 분석하여 맞춤형 질문을 생성합니다.
                        </p>
                    </div>

                    {/* Actions */}
                    <div className="flex flex-col-reverse sm:flex-row sm:justify-end gap-3 pt-4">
                        <Button
                            type="button"
                            variant="outline"
                            onClick={() => navigate('/')}
                            disabled={isSubmitting}
                            className="border-gray-200 text-gray-700 hover:bg-gray-50 rounded-xl h-12"
                        >
                            취소
                        </Button>
                        <Button
                            type="submit"
                            disabled={isSubmitting}
                            className="bg-gradient-to-r from-blue-500 to-cyan-400 hover:from-blue-600 hover:to-cyan-500 text-white font-semibold rounded-xl h-12 px-8 shadow-lg shadow-blue-500/25 transition-all disabled:opacity-50"
                        >
                            {isSubmitting ? (
                                <>
                                    <Loader2 className="w-5 h-5 mr-2 animate-spin" />
                                    면접 준비 중...
                                </>
                            ) : (
                                <>
                                    면접 시작하기
                                    <ArrowRight className="w-5 h-5 ml-2" />
                                </>
                            )}
                        </Button>
                    </div>
                </form>
            </div>
        </Layout>
    );
};

export default InterviewCreate;
