import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { getInterviews, deleteInterview } from '../api/interviewApi';
import Layout from '../components/Layout';

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
            console.log('API Response:', response);
            // ApiResponse: { success, message, data: { content: [], totalPages: 0, ... } }
            if (response.success) {
                setInterviews(response.data.content);
                setTotalPages(response.data.totalPages);
            } else {
                console.error('API Error:', response);
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

    return (
        <Layout>
            <div className="flex justify-between items-center mb-6">
                <h1 className="text-2xl font-bold text-gray-900">내 면접 목록</h1>
                <Link
                    to="/create"
                    className="inline-flex items-center px-4 py-2 border border-transparent text-sm font-medium rounded-md shadow-sm text-white bg-indigo-600 hover:bg-indigo-700 focus:outline-none"
                >
                    새 면접 생성
                </Link>
            </div>

            {/* 에러 메시지 */}
            {error && (
                <div className="mb-6 p-4 bg-red-50 border border-red-200 rounded-lg">
                    <div className="flex items-center justify-between">
                        <p className="text-sm text-red-600">{error}</p>
                        <button
                            onClick={fetchInterviews}
                            className="text-sm text-red-600 hover:text-red-800 font-medium"
                        >
                            다시 시도
                        </button>
                    </div>
                </div>
            )}

            {loading ? (
                <div className="text-center py-10">로딩 중...</div>
            ) : error ? (
                <div className="text-center py-10 bg-white rounded-lg shadow">
                    <p className="text-gray-500">데이터를 불러올 수 없습니다.</p>
                </div>
            ) : interviews.length === 0 ? (
                <div className="text-center py-10 bg-white rounded-lg shadow">
                    <p className="text-gray-500">생성된 면접이 없습니다.</p>
                </div>
            ) : (
                <>
                    <div className="grid gap-6 sm:grid-cols-2 lg:grid-cols-3 mb-6">
                        {interviews.map((interview) => (
                            <div key={interview.id} className="bg-white overflow-hidden shadow rounded-lg border border-gray-100 hover:shadow-md transition-shadow">
                                <div className="px-4 py-5 sm:p-6">
                                    <h3 className="text-lg leading-6 font-medium text-gray-900 truncate">
                                        {interview.title}
                                    </h3>
                                    <div className="mt-2 max-w-xl text-sm text-gray-500">
                                        <p>포지션: {interview.positionDescription}</p>
                                        <p>레벨: {interview.levelDescription}</p>
                                        <p>유형: {interview.typeDescription}</p>
                                    </div>
                                    <div className="mt-4 flex items-center justify-between">
                                        <span className={`px-2 inline-flex text-xs leading-5 font-semibold rounded-full ${interview.status === 'DONE' ? 'bg-green-100 text-green-800' : 'bg-yellow-100 text-yellow-800'
                                            }`}>
                                            {interview.status}
                                        </span>
                                        <div className="flex space-x-2">
                                            <button
                                                onClick={() => handleDelete(interview.id)}
                                                className="text-red-600 hover:text-red-900 text-sm font-medium"
                                            >
                                                삭제
                                            </button>
                                            <Link
                                                to={`/interviews/${interview.id}`} // Or logic to start
                                                className="text-indigo-600 hover:text-indigo-900 text-sm font-medium"
                                            >
                                                상세보기
                                            </Link>
                                        </div>
                                    </div>
                                </div>
                            </div>
                        ))}
                    </div>

                    {/* Pagination */}
                    {totalPages > 0 && (
                        <div className="flex justify-center items-center space-x-4 py-4">
                            <button
                                onClick={() => setPage(p => Math.max(0, p - 1))}
                                disabled={page === 0}
                                className={`px-4 py-2 border rounded-md text-sm font-medium ${page === 0
                                    ? 'bg-gray-100 text-gray-400 cursor-not-allowed'
                                    : 'bg-white text-gray-700 hover:bg-gray-50 cursor-pointer'
                                    }`}
                            >
                                이전
                            </button>
                            <span className="text-gray-700 text-sm">
                                {page + 1} / {totalPages}
                            </span>
                            <button
                                onClick={() => setPage(p => Math.min(totalPages - 1, p + 1))}
                                disabled={page >= totalPages - 1}
                                className={`px-4 py-2 border rounded-md text-sm font-medium ${page >= totalPages - 1
                                    ? 'bg-gray-100 text-gray-400 cursor-not-allowed'
                                    : 'bg-white text-gray-700 hover:bg-gray-50 cursor-pointer'
                                    }`}
                            >
                                다음
                            </button>
                        </div>
                    )}
                </>
            )}
        </Layout>
    );
};

export default InterviewList;
