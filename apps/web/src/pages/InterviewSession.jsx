import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { getQuestions, createAnswer, getInterview } from '../api/interviewApi';
import { useFeatures } from '../context/FeatureContext';
import Layout from '../components/Layout';
import VoiceRecorder from '../components/VoiceRecorder';
import TextToSpeech from '../components/TextToSpeech';

// 백엔드 InterviewPhase enum의 order 순서와 일치
const PHASE_ORDER = ['OPENING', 'TECHNICAL', 'PERSONALITY', 'CLOSING'];

const PHASE_INFO = {
    OPENING: { label: '인사/자기소개', color: 'blue', icon: '👋' },
    TECHNICAL: { label: '기술 면접', color: 'cyan', icon: '💻' },
    PERSONALITY: { label: '인성 면접', color: 'purple', icon: '💭' },
    CLOSING: { label: '마무리', color: 'emerald', icon: '🎯' }
};

const InterviewSession = () => {
    const { id } = useParams();
    const navigate = useNavigate();
    const { voiceEnabled } = useFeatures();

    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');
    const [questions, setQuestions] = useState([]);
    const [currentIndex, setCurrentIndex] = useState(0);
    const [answerContent, setAnswerContent] = useState('');
    const [submitting, setSubmitting] = useState(false);
    const [submitError, setSubmitError] = useState('');
    const [feedback, setFeedback] = useState(null);
    const [submittedAnswer, setSubmittedAnswer] = useState('');
    const [interviewTitle, setInterviewTitle] = useState('');

    useEffect(() => {
        const init = async () => {
            try {
                const interviewRes = await getInterview(id);
                if (interviewRes.success) {
                    setInterviewTitle(interviewRes.data.title);
                }

                const qRes = await getQuestions(id);
                if (qRes.success) {
                    const grouped = qRes.data.questionsByPhase;
                    let flatList = [];

                    PHASE_ORDER.forEach(phase => {
                        if (grouped[phase]) {
                            const phaseQuestions = grouped[phase].sort((a, b) => a.sequence - b.sequence);
                            flatList = [...flatList, ...phaseQuestions];
                        }
                    });

                    Object.keys(grouped).forEach(key => {
                        if (!PHASE_ORDER.includes(key)) {
                            flatList = [...flatList, ...grouped[key]];
                        }
                    });

                    const firstUnansweredIndex = flatList.findIndex(q => !q.isAnswered);
                    if (firstUnansweredIndex !== -1) {
                        setCurrentIndex(firstUnansweredIndex);
                    } else if (flatList.length > 0 && flatList.every(q => q.isAnswered)) {
                        navigate(`/interviews/${id}/result`);
                        return;
                    }

                    setQuestions(flatList);
                }
            } catch (err) {
                console.error("Failed to load session", err);
                setError("면접 데이터를 불러오는데 실패했습니다.");
            } finally {
                setLoading(false);
            }
        };
        init();
    }, [id, navigate]);

    const refetchQuestions = async () => {
        const qRes = await getQuestions(id);
        if (qRes.success) {
            const grouped = qRes.data.questionsByPhase;
            let flatList = [];

            PHASE_ORDER.forEach(phase => {
                if (grouped[phase]) {
                    const phaseQuestions = grouped[phase].sort((a, b) => a.sequence - b.sequence);
                    flatList = [...flatList, ...phaseQuestions];
                }
            });

            Object.keys(grouped).forEach(key => {
                if (!PHASE_ORDER.includes(key)) {
                    flatList = [...flatList, ...grouped[key]];
                }
            });

            return flatList;
        }
        return null;
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        if (!answerContent.trim()) return;

        setSubmitting(true);
        setSubmitError('');
        setSubmittedAnswer(answerContent);
        try {
            const currentQ = questions[currentIndex];
            const response = await createAnswer(id, currentQ.id, { content: answerContent });

            if (response.success) {
                const answerRes = response.data;
                setFeedback(answerRes);

                if (answerRes.followUpQuestion) {
                    const updatedQuestions = [...questions];
                    updatedQuestions[currentIndex] = { ...updatedQuestions[currentIndex], isAnswered: true };
                    updatedQuestions.splice(currentIndex + 1, 0, answerRes.followUpQuestion);
                    setQuestions(updatedQuestions);
                } else {
                    const newQuestions = await refetchQuestions();
                    if (newQuestions) {
                        setQuestions(newQuestions);
                        if (newQuestions.every(q => q.isAnswered)) {
                            navigate(`/interviews/${id}/result`);
                            return;
                        }
                    } else {
                        const updatedQuestions = [...questions];
                        updatedQuestions[currentIndex] = { ...updatedQuestions[currentIndex], isAnswered: true };
                        setQuestions(updatedQuestions);
                    }
                }
            }
        } catch (err) {
            console.error("Answer submission failed", err);
            setSubmitError("답변 제출에 실패했습니다. 다시 시도해주세요.");
        } finally {
            setSubmitting(false);
        }
    };

    const handleNext = () => {
        setFeedback(null);
        setAnswerContent('');
        setSubmittedAnswer('');
        if (currentIndex < questions.length - 1) {
            setCurrentIndex(currentIndex + 1);
        } else {
            navigate(`/interviews/${id}/result`);
        }
    };

    const getScoreColor = (score) => {
        if (score >= 8) return 'from-emerald-500 to-teal-500';
        if (score >= 6) return 'from-blue-500 to-cyan-500';
        if (score >= 4) return 'from-yellow-500 to-orange-500';
        return 'from-red-500 to-pink-500';
    };

    const getScoreBg = (score) => {
        if (score >= 8) return 'bg-emerald-50 border-emerald-100';
        if (score >= 6) return 'bg-blue-50 border-blue-100';
        if (score >= 4) return 'bg-yellow-50 border-yellow-100';
        return 'bg-red-50 border-red-100';
    };

    if (loading) {
        return (
            <Layout>
                <div className="flex flex-col items-center justify-center py-20">
                    <div className="relative">
                        <div className="w-16 h-16 border-4 border-blue-100 border-t-blue-500 rounded-full animate-spin"></div>
                        <div className="absolute inset-0 flex items-center justify-center">
                            <span className="text-xl">💬</span>
                        </div>
                    </div>
                    <p className="mt-6 text-gray-600 font-medium">면접 세션을 준비하고 있습니다...</p>
                    <p className="mt-2 text-gray-400 text-sm">잠시만 기다려주세요</p>
                </div>
            </Layout>
        );
    }

    if (error) {
        return (
            <Layout>
                <div className="max-w-2xl mx-auto py-16 text-center">
                    <div className="bg-white rounded-2xl shadow-sm border border-gray-100 p-12">
                        <div className="w-16 h-16 bg-red-50 rounded-full flex items-center justify-center mx-auto mb-6">
                            <svg className="w-8 h-8 text-red-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
                            </svg>
                        </div>
                        <h2 className="text-xl font-bold text-gray-900 mb-2">오류가 발생했습니다</h2>
                        <p className="text-gray-500 mb-6">{error}</p>
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

    if (questions.length === 0) {
        return (
            <Layout>
                <div className="max-w-2xl mx-auto py-16 text-center">
                    <div className="bg-white rounded-2xl shadow-sm border border-gray-100 p-12">
                        <div className="w-16 h-16 bg-gray-100 rounded-full flex items-center justify-center mx-auto mb-6">
                            <svg className="w-8 h-8 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8.228 9c.549-1.165 2.03-2 3.772-2 2.21 0 4 1.343 4 3 0 1.4-1.278 2.575-3.006 2.907-.542.104-.994.54-.994 1.093m0 3h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                            </svg>
                        </div>
                        <h2 className="text-xl font-bold text-gray-900 mb-2">질문이 없습니다</h2>
                        <p className="text-gray-500 mb-6">면접 질문이 생성되지 않았습니다.</p>
                        <button
                            onClick={() => navigate(-1)}
                            className="px-6 py-3 border border-gray-200 text-gray-700 font-medium rounded-xl hover:bg-gray-50 transition-all duration-200"
                        >
                            뒤로 가기
                        </button>
                    </div>
                </div>
            </Layout>
        );
    }

    const currentQuestion = questions[currentIndex];
    const isLast = currentIndex === questions.length - 1;
    const phaseInfo = PHASE_INFO[currentQuestion.phase] || { label: currentQuestion.phaseDescription, color: 'gray', icon: '📝' };
    const progress = ((currentIndex + 1) / questions.length) * 100;

    return (
        <Layout>
            <div className="max-w-4xl mx-auto">
                {/* 상단 헤더 */}
                <div className="bg-white rounded-2xl shadow-sm border border-gray-100 p-6 mb-6">
                    <div className="flex items-center justify-between mb-4">
                        <div>
                            <h1 className="text-xl font-bold text-gray-900">{interviewTitle}</h1>
                            <div className="flex items-center gap-2 mt-2">
                                <span className={`inline-flex items-center gap-1.5 px-3 py-1.5 rounded-full text-sm font-medium bg-${phaseInfo.color}-50 text-${phaseInfo.color}-700 border border-${phaseInfo.color}-100`}>
                                    <span>{phaseInfo.icon}</span>
                                    {phaseInfo.label}
                                </span>
                            </div>
                        </div>
                        <div className="text-right">
                            <p className="text-sm text-gray-500">진행률</p>
                            <p className="text-2xl font-bold bg-gradient-to-r from-blue-500 to-cyan-500 bg-clip-text text-transparent">
                                {currentIndex + 1} / {questions.length}
                            </p>
                        </div>
                    </div>
                    {/* 프로그레스 바 */}
                    <div className="w-full bg-gray-100 rounded-full h-2">
                        <div
                            className="bg-gradient-to-r from-blue-500 to-cyan-500 h-2 rounded-full transition-all duration-500"
                            style={{ width: `${progress}%` }}
                        ></div>
                    </div>
                </div>

                {/* 질문 카드 */}
                <div className="bg-white rounded-2xl shadow-sm border border-gray-100 p-8 mb-6">
                    <div className="flex items-center gap-4">
                        <div className="w-12 h-12 bg-gradient-to-r from-blue-500 to-cyan-500 rounded-xl flex items-center justify-center flex-shrink-0">
                            <span className="text-white font-bold text-lg">Q</span>
                        </div>
                        <div className="flex-1">
                            <h2 className="text-xl font-semibold text-gray-900 leading-relaxed">
                                {currentQuestion.content}
                            </h2>
                        </div>
                        {/* TTS 버튼 - 질문 듣기 (음성 기능 활성화 시에만 표시) */}
                        {voiceEnabled && <TextToSpeech text={currentQuestion.content} />}
                    </div>
                </div>

                {/* 피드백 섹션 (제출 후) */}
                {feedback ? (
                    <div className="space-y-6 animate-fade-in">
                        {/* 내 답변 */}
                        <div className="bg-white rounded-2xl shadow-sm border border-gray-100 p-6">
                            <div className="flex items-center gap-3 mb-4">
                                <div className="w-10 h-10 bg-gray-100 rounded-xl flex items-center justify-center">
                                    <svg className="w-5 h-5 text-gray-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
                                    </svg>
                                </div>
                                <h3 className="text-lg font-bold text-gray-900">내 답변</h3>
                            </div>
                            <p className="text-gray-700 leading-relaxed whitespace-pre-wrap">{submittedAnswer}</p>
                        </div>

                        {/* AI 피드백 */}
                        <div className={`bg-white rounded-2xl shadow-sm border p-6 ${getScoreBg(feedback.score)}`}>
                            <div className="flex items-center justify-between mb-4">
                                <div className="flex items-center gap-3">
                                    <div className={`w-10 h-10 bg-gradient-to-r ${getScoreColor(feedback.score)} rounded-xl flex items-center justify-center`}>
                                        <svg className="w-5 h-5 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9.663 17h4.673M12 3v1m6.364 1.636l-.707.707M21 12h-1M4 12H3m3.343-5.657l-.707-.707m2.828 9.9a5 5 0 117.072 0l-.548.547A3.374 3.374 0 0014 18.469V19a2 2 0 11-4 0v-.531c0-.895-.356-1.754-.988-2.386l-.548-.547z" />
                                        </svg>
                                    </div>
                                    <h3 className="text-lg font-bold text-gray-900">AI 피드백</h3>
                                </div>
                                <div className={`px-4 py-2 bg-gradient-to-r ${getScoreColor(feedback.score)} rounded-xl`}>
                                    <span className="text-white font-bold text-lg">{feedback.score}점</span>
                                </div>
                            </div>
                            <p className="text-gray-700 leading-relaxed whitespace-pre-wrap">{feedback.feedback}</p>
                        </div>

                        {/* 다음 질문 버튼 */}
                        <div className="flex justify-end">
                            <button
                                onClick={handleNext}
                                className="px-8 py-3 bg-gradient-to-r from-blue-500 to-cyan-500 text-white font-medium rounded-xl hover:shadow-lg hover:shadow-blue-500/25 hover:-translate-y-0.5 transition-all duration-200 flex items-center gap-2"
                            >
                                {isLast ? (
                                    <>
                                        결과 보기
                                        <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 17v-2m3 2v-4m3 4v-6m2 10H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                                        </svg>
                                    </>
                                ) : (
                                    <>
                                        다음 질문
                                        <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 7l5 5m0 0l-5 5m5-5H6" />
                                        </svg>
                                    </>
                                )}
                            </button>
                        </div>
                    </div>
                ) : (
                    /* 답변 입력 폼 */
                    <form onSubmit={handleSubmit} className="space-y-6">
                        {submitError && (
                            <div className="p-4 bg-red-50 border border-red-100 rounded-xl flex items-start gap-3">
                                <svg className="w-5 h-5 text-red-500 mt-0.5 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                                </svg>
                                <p className="text-red-700">{submitError}</p>
                            </div>
                        )}

                        <div className="bg-white rounded-2xl shadow-sm border border-gray-100 p-6">
                            <div className="flex items-center justify-between mb-3">
                                <label htmlFor="answer" className="flex items-center gap-2 text-sm font-medium text-gray-700">
                                    <svg className="w-4 h-4 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15.232 5.232l3.536 3.536m-2.036-5.036a2.5 2.5 0 113.536 3.536L6.5 21.036H3v-3.572L16.732 3.732z" />
                                    </svg>
                                    답변 작성
                                </label>
                                {/* 음성 녹음 버튼 (음성 기능 활성화 시에만 표시) */}
                                {voiceEnabled && (
                                    <VoiceRecorder
                                        onTranscript={(text) => setAnswerContent(prev => prev ? `${prev}\n${text}` : text)}
                                        disabled={submitting}
                                    />
                                )}
                            </div>
                            <textarea
                                id="answer"
                                rows={8}
                                className="w-full px-4 py-3 border border-gray-200 rounded-xl focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500 transition-all duration-200 resize-none text-gray-900 placeholder-gray-400"
                                placeholder="질문에 대한 답변을 자세히 작성해주세요."
                                value={answerContent}
                                onChange={(e) => setAnswerContent(e.target.value)}
                                disabled={submitting}
                            />
                            <p className="mt-2 text-sm text-gray-400">
                                구체적인 예시와 함께 답변하면 더 좋은 피드백을 받을 수 있습니다.{voiceEnabled && ' 🎤 음성으로도 답변할 수 있습니다.'}
                            </p>
                        </div>

                        <div className="flex justify-between items-center">
                            <button
                                type="button"
                                onClick={() => navigate('/dashboard')}
                                className="px-6 py-3 text-gray-500 font-medium hover:text-gray-700 transition-colors"
                            >
                                나중에 계속하기
                            </button>
                            <button
                                type="submit"
                                disabled={submitting || !answerContent.trim()}
                                className="px-8 py-3 bg-gradient-to-r from-blue-500 to-cyan-500 text-white font-medium rounded-xl hover:shadow-lg hover:shadow-blue-500/25 hover:-translate-y-0.5 transition-all duration-200 disabled:opacity-50 disabled:cursor-not-allowed disabled:hover:shadow-none disabled:hover:translate-y-0 flex items-center gap-2"
                            >
                                {submitting ? (
                                    <>
                                        <svg className="w-5 h-5 animate-spin" fill="none" viewBox="0 0 24 24">
                                            <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                                            <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                                        </svg>
                                        AI가 분석 중...
                                    </>
                                ) : (
                                    <>
                                        답변 제출
                                        <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 19l9 2-9-18-9 18 9-2zm0 0v-8" />
                                        </svg>
                                    </>
                                )}
                            </button>
                        </div>
                    </form>
                )}
            </div>
        </Layout>
    );
};

export default InterviewSession;
