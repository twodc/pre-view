import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { getInterviews, deleteInterview } from '../api/interviewApi';
import Layout from '../components/Layout';
import { Button } from '../components/ui/button';
import { Plus, Trash2, ChevronRight, AlertCircle, FileText, RefreshCw, ChevronLeft } from 'lucide-react';

const InterviewList = () => {
    const [interviews, setInterviews] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');
    const [page, setPage] = useState(0);
    const [totalPages, setTotalPages] = useState(0);

    const fetchInterviews = async () => {
        setError('');
        try {
            const response = await getInterviews(page);
            if (response.success) {
                setInterviews(response.data.content);
                setTotalPages(response.data.totalPages);
            } else {
                setError(response.message || '목록을 불러오지 못했습니다.');
            }
        } catch (error) {
            console.error('Failed to fetch interviews', error);
            setError('서버 통신 오류가 발생했습니다.');
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchInterviews();
    }, [page]);

    const handleDelete = async (id) => {
        if (window.confirm('정말 삭제하시겠습니까?')) {
            try {
                await deleteInterview(id);
                fetchInterviews();
            } catch (error) {
                console.error('Failed to delete interview', error);
            }
        }
    };

    const getStatusStyle = (status) => {
        switch (status) {
            case 'DONE':
                return 'bg-green-100 text-green-700';
            case 'IN_PROGRESS':
                return 'bg-blue-100 text-blue-700';
            default:
                return 'bg-gray-100 text-gray-700';
        }
    };

    const getStatusText = (status) => {
        switch (status) {
            case 'DONE':
                return '완료';
            case 'IN_PROGRESS':
                return '진행중';
            case 'READY':
                return '대기';
            default:
                return status;
        }
    };

    return (
        <Layout>
            {/* Header */}
            <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 mb-8">
                <div>
                    <h1 className="text-3xl font-bold text-gray-900 mb-1">내 면접 목록</h1>
                    <p className="text-gray-500">진행했던 면접을 확인하고 관리하세요.</p>
                </div>
                <Link to="/create">
                    <Button className="bg-gradient-to-r from-blue-500 to-cyan-400 hover:from-blue-600 hover:to-cyan-500 text-white font-semibold rounded-xl shadow-lg shadow-blue-500/25 transition-all">
                        <Plus className="w-5 h-5 mr-2" />
                        새 면접 생성
                    </Button>
                </Link>
            </div>

            {/* Error Message */}
            {error && (
                <div className="mb-6 p-4 rounded-xl flex items-center justify-between bg-red-50 border border-red-200">
                    <div className="flex items-center gap-3">
                        <AlertCircle className="w-5 h-5 text-red-500" />
                        <p className="text-sm font-medium text-red-600">{error}</p>
                    </div>
                    <button
                        onClick={fetchInterviews}
                        className="flex items-center gap-2 text-sm text-red-600 hover:text-red-700 font-medium"
                    >
                        <RefreshCw className="w-4 h-4" />
                        다시 시도
                    </button>
                </div>
            )}

            {loading ? (
                <div className="flex flex-col items-center justify-center py-20">
                    <div className="w-12 h-12 border-4 border-blue-200 border-t-blue-500 rounded-full animate-spin mb-4" />
                    <p className="text-gray-500">면접 목록을 불러오는 중...</p>
                </div>
            ) : error && interviews.length === 0 ? (
                <div className="bg-white rounded-2xl border border-gray-100 shadow-lg shadow-gray-100/50 p-12 text-center">
                    <AlertCircle className="w-12 h-12 text-gray-300 mx-auto mb-4" />
                    <p className="text-gray-500">데이터를 불러올 수 없습니다.</p>
                </div>
            ) : interviews.length === 0 ? (
                <div className="bg-white rounded-2xl border border-gray-100 shadow-lg shadow-gray-100/50 p-12 text-center">
                    <FileText className="w-12 h-12 text-gray-300 mx-auto mb-4" />
                    <h3 className="text-lg font-medium text-gray-900 mb-2">아직 면접이 없습니다</h3>
                    <p className="text-gray-500 mb-6">첫 번째 AI 면접을 시작해보세요!</p>
                    <Link to="/create">
                        <Button className="bg-gradient-to-r from-blue-500 to-cyan-400 hover:from-blue-600 hover:to-cyan-500 text-white font-semibold rounded-xl shadow-lg shadow-blue-500/25">
                            <Plus className="w-5 h-5 mr-2" />
                            면접 생성하기
                        </Button>
                    </Link>
                </div>
            ) : (
                <>
                    <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3 mb-8">
                        {interviews.map((interview) => (
                            <div
                                key={interview.id}
                                className="group bg-white rounded-2xl border border-gray-100 shadow-lg shadow-gray-100/50 hover:shadow-xl hover:shadow-blue-100/50 transition-all duration-300 overflow-hidden"
                            >
                                <div className="p-6">
                                    <div className="flex items-start justify-between mb-4">
                                        <h3 className="text-lg font-semibold text-gray-900 truncate pr-2">
                                            {interview.title}
                                        </h3>
                                        <span className={`px-2.5 py-1 text-xs font-medium rounded-full whitespace-nowrap ${getStatusStyle(interview.status)}`}>
                                            {getStatusText(interview.status)}
                                        </span>
                                    </div>

                                    <div className="space-y-2 text-sm text-gray-500 mb-4">
                                        <p>포지션: {interview.positionDescription}</p>
                                        <p>레벨: {interview.levelDescription}</p>
                                        <p>유형: {interview.typeDescription}</p>
                                    </div>

                                    <div className="flex items-center justify-between pt-4 border-t border-gray-100">
                                        <button
                                            onClick={() => handleDelete(interview.id)}
                                            className="flex items-center gap-1.5 text-sm text-red-500 hover:text-red-600 transition-colors"
                                        >
                                            <Trash2 className="w-4 h-4" />
                                            삭제
                                        </button>
                                        <Link
                                            to={`/interviews/${interview.id}`}
                                            className="flex items-center gap-1.5 text-sm font-medium text-blue-500 hover:text-blue-600 transition-colors"
                                        >
                                            상세보기
                                            <ChevronRight className="w-4 h-4" />
                                        </Link>
                                    </div>
                                </div>
                            </div>
                        ))}
                    </div>

                    {/* Pagination */}
                    {totalPages > 1 && (
                        <div className="flex justify-center items-center gap-4">
                            <Button
                                variant="outline"
                                onClick={() => setPage(p => Math.max(0, p - 1))}
                                disabled={page === 0}
                                className="rounded-xl border-gray-200 disabled:opacity-50"
                            >
                                <ChevronLeft className="w-4 h-4 mr-1" />
                                이전
                            </Button>
                            <span className="text-sm text-gray-600">
                                {page + 1} / {totalPages}
                            </span>
                            <Button
                                variant="outline"
                                onClick={() => setPage(p => Math.min(totalPages - 1, p + 1))}
                                disabled={page >= totalPages - 1}
                                className="rounded-xl border-gray-200 disabled:opacity-50"
                            >
                                다음
                                <ChevronRight className="w-4 h-4 ml-1" />
                            </Button>
                        </div>
                    )}
                </>
            )}
        </Layout>
    );
};

export default InterviewList;
