import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { getQuestions, createAnswer, getInterview } from '../api/interviewApi';
import Layout from '../components/Layout';
import clsx from 'clsx';

// 백엔드 InterviewPhase enum의 order 순서와 일치
// OPENING(1) → TECHNICAL(2) → PERSONALITY(3) → CLOSING(4)
const PHASE_ORDER = [
    'OPENING',     // 인사/자기소개 (order: 1)
    'TECHNICAL',   // 기술 (order: 2)
    'PERSONALITY', // 인성/태도 (order: 3)
    'CLOSING'      // 마무리 (order: 4)
];

const InterviewSession = () => {
    const { id } = useParams();
    const navigate = useNavigate();

    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');
    const [questions, setQuestions] = useState([]);
    const [currentIndex, setCurrentIndex] = useState(0);
    const [answerContent, setAnswerContent] = useState('');
    const [submitting, setSubmitting] = useState(false);
    const [submitError, setSubmitError] = useState('');
    const [feedback, setFeedback] = useState(null); // AnswerResponse | null
    const [submittedAnswer, setSubmittedAnswer] = useState(''); // 제출한 답변 저장
    const [interviewTitle, setInterviewTitle] = useState('');

    useEffect(() => {
        const init = async () => {
            try {
                // Fetch Interview Info
                const interviewRes = await getInterview(id);
                if (interviewRes.success) {
                    setInterviewTitle(interviewRes.data.title);
                }

                // Fetch Questions
                const qRes = await getQuestions(id);
                if (qRes.success) {
                    const grouped = qRes.data.questionsByPhase;
                    let flatList = [];

                    // Flatten based on logical order
                    PHASE_ORDER.forEach(phase => {
                        if (grouped[phase]) {
                            // Sort by sequence just in case
                            const phaseQuestions = grouped[phase].sort((a, b) => a.sequence - b.sequence);
                            flatList = [...flatList, ...phaseQuestions];
                        }
                    });

                    // If API returns phases we didn't expect, append them at the end
                    Object.keys(grouped).forEach(key => {
                        if (!PHASE_ORDER.includes(key)) {
                            flatList = [...flatList, ...grouped[key]];
                        }
                    });

                    // Find first unanswered question
                    console.log('Flat Questions:', flatList);
                    const firstUnansweredIndex = flatList.findIndex(q => !q.isAnswered);
                    console.log('First Unanswered Index:', firstUnansweredIndex);
                    if (firstUnansweredIndex !== -1) {
                        setCurrentIndex(firstUnansweredIndex);
                    } else if (flatList.length > 0 && flatList.every(q => q.isAnswered)) {
                        // All answered, redirect to result immediately
                        navigate(`/interviews/${id}/result`);
                        return;
                    }

                    setQuestions(flatList);
                }
            } catch (error) {
                console.error("Failed to load session", error);
                setError("면접 데이터를 불러오는데 실패했습니다.");
            } finally {
                setLoading(false);
            }
        };
        init();
    }, [id, navigate]);

    // 질문 목록을 다시 조회하는 헬퍼 함수
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
        setSubmittedAnswer(answerContent); // 제출 전 답변 저장
        try {
            const currentQ = questions[currentIndex];
            const response = await createAnswer(id, currentQ.id, { content: answerContent });

            if (response.success) {
                const answerRes = response.data;
                setFeedback(answerRes);

                // followUpQuestion이 있으면 로컬에서 바로 추가
                if (answerRes.followUpQuestion) {
                    const updatedQuestions = [...questions];
                    updatedQuestions[currentIndex] = { ...updatedQuestions[currentIndex], isAnswered: true };
                    updatedQuestions.splice(currentIndex + 1, 0, answerRes.followUpQuestion);
                    setQuestions(updatedQuestions);
                } else {
                    // followUpQuestion이 없으면 페이즈 전환이 발생했을 수 있음
                    // 질문 목록을 다시 조회하여 새로 생성된 질문 반영
                    const newQuestions = await refetchQuestions();
                    if (newQuestions) {
                        // 현재 질문의 다음 미답변 질문 찾기
                        const nextUnansweredIndex = newQuestions.findIndex(
                            (q, idx) => !q.isAnswered && idx > currentIndex
                        );

                        setQuestions(newQuestions);

                        // 모든 질문이 답변됨 → 결과 페이지로 이동
                        if (newQuestions.every(q => q.isAnswered)) {
                            navigate(`/interviews/${id}/result`);
                            return;
                        }
                    } else {
                        // refetch 실패 시 로컬 상태만 업데이트
                        const updatedQuestions = [...questions];
                        updatedQuestions[currentIndex] = { ...updatedQuestions[currentIndex], isAnswered: true };
                        setQuestions(updatedQuestions);
                    }
                }
            }
        } catch (error) {
            console.error("Answer submission failed", error);
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
            // Finished
            if (window.confirm("모든 질문이 완료되었습니다. 결과 페이지로 이동하시겠습니까?")) {
                navigate(`/interviews/${id}/result`); // Adjust if needed
            }
        }
    };

    if (loading) return <Layout><div className="flex justify-center p-10">로딩 중...</div></Layout>;

    if (error) {
        return (
            <Layout>
                <div className="max-w-3xl mx-auto py-8 px-4 text-center">
                    <div className="bg-white rounded-lg shadow p-8">
                        <div className="text-red-500 mb-4">
                            <svg className="w-12 h-12 mx-auto" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
                            </svg>
                        </div>
                        <p className="text-gray-600 mb-4">{error}</p>
                        <button
                            onClick={() => navigate('/dashboard')}
                            className="px-4 py-2 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700"
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
                <div className="text-center p-10">
                    <p>질문이 생성되지 않았습니다. 관리자에게 문의하세요.</p>
                    <button onClick={() => navigate(-1)} className="mt-4 text-indigo-600">뒤로 가기</button>
                </div>
            </Layout>
        );
    }

    const currentQuestion = questions[currentIndex];
    const isLast = currentIndex === questions.length - 1;

    return (
        <Layout>
            <div className="max-w-3xl mx-auto py-8 px-4">
                <div className="mb-6 flex justify-between items-end border-b pb-4">
                    <div>
                        <h2 className="text-lg text-gray-500">{interviewTitle}</h2>
                        <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-indigo-100 text-indigo-800 mt-2">
                            {currentQuestion.phaseDescription}
                        </span>
                    </div>
                </div>

                <div className="bg-white shadow rounded-lg p-6 mb-6">
                    <h3 className="text-xl font-medium text-gray-900">
                        Q. {currentQuestion.content}
                    </h3>
                </div>

                {/* Feedback Section (if submitted) */}
                {feedback ? (
                    <div className="space-y-4 mb-6 animate-fade-in-up">
                        {/* 내 답변 표시 */}
                        <div className="bg-gray-50 border border-gray-200 rounded-lg p-5">
                            <h4 className="text-sm font-medium text-gray-500 mb-2">내 답변</h4>
                            <p className="text-gray-800 whitespace-pre-wrap">{submittedAnswer}</p>
                        </div>

                        {/* AI 피드백 */}
                        <div className="bg-green-50 border border-green-200 rounded-lg p-5">
                            <div className="flex items-center justify-between mb-3">
                                <h4 className="text-lg font-medium text-green-800">AI 피드백</h4>
                                <span className={`px-3 py-1 rounded-full text-sm font-bold ${feedback.score >= 7 ? 'bg-green-100 text-green-800' :
                                        feedback.score >= 5 ? 'bg-yellow-100 text-yellow-800' :
                                            'bg-red-100 text-red-800'
                                    }`}>
                                    {feedback.score}점
                                </span>
                            </div>
                            <p className="text-green-700 whitespace-pre-wrap">{feedback.feedback}</p>
                        </div>

                        <div className="flex justify-end">
                            <button
                                onClick={handleNext}
                                className="bg-green-600 text-white px-4 py-2 rounded-md hover:bg-green-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-green-500 transition-colors"
                            >
                                {isLast ? "결과 보기" : "다음 질문으로"}
                            </button>
                        </div>
                    </div>
                ) : (
                    /* Answer Form */
                    <form onSubmit={handleSubmit} className="space-y-4">
                        {/* 제출 에러 메시지 */}
                        {submitError && (
                            <div className="p-3 bg-red-50 border border-red-200 rounded-lg">
                                <p className="text-sm text-red-600">{submitError}</p>
                            </div>
                        )}
                        <div>
                            <label htmlFor="answer" className="block text-sm font-medium text-gray-700 mb-2">
                                답변 작성
                            </label>
                            <textarea
                                id="answer"
                                rows={6}
                                className="shadow-sm focus:ring-indigo-500 focus:border-indigo-500 block w-full sm:text-sm border-gray-300 rounded-md border p-3"
                                placeholder="답변을 입력해주세요..."
                                value={answerContent}
                                onChange={(e) => setAnswerContent(e.target.value)}
                                disabled={submitting}
                            />
                        </div>
                        <div className="flex justify-end">
                            <button
                                type="submit"
                                disabled={submitting || !answerContent.trim()}
                                className={clsx(
                                    "inline-flex justify-center py-2 px-4 border border-transparent shadow-sm text-sm font-medium rounded-md text-white bg-indigo-600 hover:bg-indigo-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500 disabled:opacity-50 disabled:cursor-not-allowed",
                                    submitting && "cursor-wait"
                                )}
                            >
                                {submitting ? "제출 중..." : "답변 제출"}
                            </button>
                        </div>
                    </form>
                )}
            </div>
        </Layout>
    );
};

export default InterviewSession;
