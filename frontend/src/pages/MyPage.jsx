import React, { useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { getMyProfile, updateProfile } from '../api/memberApi';
import { getInterviews, deleteInterview } from '../api/interviewApi';
import Layout from '../components/Layout';
import { Plus, Trash2, ChevronRight, FileText, ChevronLeft } from 'lucide-react';
import { Button } from '../components/ui/button';

const MyPage = () => {
    const navigate = useNavigate();
    const { user, logout } = useAuth();

    // 프로필 상태
    const [profile, setProfile] = useState(null);
    const [profileLoading, setProfileLoading] = useState(true);
    const [editing, setEditing] = useState(false);
    const [formData, setFormData] = useState({ name: '', profileImage: '' });
    const [saving, setSaving] = useState(false);
    const [error, setError] = useState('');
    const [success, setSuccess] = useState('');

    // 면접 목록 상태
    const [interviews, setInterviews] = useState([]);
    const [interviewsLoading, setInterviewsLoading] = useState(true);
    const [page, setPage] = useState(0);
    const [totalPages, setTotalPages] = useState(0);

    useEffect(() => {
        if (!user) {
            navigate('/login');
            return;
        }
        fetchProfile();
        fetchInterviews();
    }, [user, navigate]);

    useEffect(() => {
        fetchInterviews();
    }, [page]);

    const fetchProfile = async () => {
        try {
            const response = await getMyProfile();
            setProfile(response.data);
            setFormData({
                name: response.data.name || '',
                profileImage: response.data.profileImage || '',
            });
        } catch (err) {
            setError('프로필을 불러오는데 실패했습니다.');
        } finally {
            setProfileLoading(false);
        }
    };

    const fetchInterviews = async () => {
        try {
            const response = await getInterviews(page);
            if (response.success) {
                setInterviews(response.data.content);
                setTotalPages(response.data.totalPages);
            }
        } catch (err) {
            console.error('Failed to fetch interviews', err);
        } finally {
            setInterviewsLoading(false);
        }
    };

    const handleChange = (e) => {
        const { name, value } = e.target;
        setFormData(prev => ({ ...prev, [name]: value }));
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        setSaving(true);
        setError('');
        setSuccess('');

        try {
            const response = await updateProfile(formData.name, formData.profileImage);
            setProfile(response.data);
            setEditing(false);
            setSuccess('프로필이 수정되었습니다.');
        } catch (err) {
            setError(err.response?.data?.message || '프로필 수정에 실패했습니다.');
        } finally {
            setSaving(false);
        }
    };

    const handleDelete = async (id) => {
        if (window.confirm('정말 삭제하시겠습니까?')) {
            try {
                await deleteInterview(id);
                fetchInterviews();
            } catch (err) {
                console.error('Failed to delete interview', err);
            }
        }
    };

    const handleLogout = async () => {
        await logout();
        navigate('/');
    };

    const getStatusStyle = (status) => {
        switch (status) {
            case 'DONE':
                return 'bg-emerald-50 text-emerald-700 border border-emerald-200';
            case 'IN_PROGRESS':
                return 'bg-blue-50 text-blue-700 border border-blue-200';
            default:
                return 'bg-gray-100 text-gray-700 border border-gray-200';
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

    if (profileLoading) {
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

    return (
        <Layout>
            <div className="max-w-4xl mx-auto">
                {/* 알림 메시지 */}
                {error && (
                    <div className="mb-6 p-4 bg-red-50 border border-red-100 rounded-xl flex items-start gap-3">
                        <svg className="w-5 h-5 text-red-500 mt-0.5 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                        </svg>
                        <p className="text-red-700">{error}</p>
                    </div>
                )}
                {success && (
                    <div className="mb-6 p-4 bg-emerald-50 border border-emerald-100 rounded-xl flex items-start gap-3">
                        <svg className="w-5 h-5 text-emerald-500 mt-0.5 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
                        </svg>
                        <p className="text-emerald-700">{success}</p>
                    </div>
                )}

                {/* 프로필 카드 */}
                {profile && (
                    <div className="bg-white rounded-2xl shadow-sm border border-gray-100 p-6 mb-8">
                        <div className="flex flex-col sm:flex-row sm:items-center gap-4">
                            {/* 프로필 이미지 */}
                            <div className="w-20 h-20 bg-gradient-to-r from-blue-100 to-cyan-100 rounded-2xl flex items-center justify-center overflow-hidden flex-shrink-0">
                                {profile?.profileImage ? (
                                    <img
                                        src={profile.profileImage}
                                        alt="프로필"
                                        className="w-full h-full object-cover"
                                    />
                                ) : (
                                    <span className="text-3xl font-bold bg-gradient-to-r from-blue-500 to-cyan-500 bg-clip-text text-transparent">
                                        {profile?.name?.charAt(0) || '?'}
                                    </span>
                                )}
                            </div>

                            {/* 프로필 정보 */}
                            <div className="flex-1 min-w-0">
                                <h2 className="text-xl font-bold text-gray-900 truncate">{profile?.name}</h2>
                                <p className="text-gray-500 truncate">{profile?.email}</p>
                            </div>

                            {/* 액션 버튼 */}
                            <div className="flex items-center gap-2 sm:flex-shrink-0">
                                {!editing && (
                                    <button
                                        onClick={() => setEditing(true)}
                                        className="px-4 py-2 border border-blue-500 text-blue-500 font-medium rounded-xl hover:bg-blue-50 transition-all duration-200 flex items-center gap-2"
                                    >
                                        <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15.232 5.232l3.536 3.536m-2.036-5.036a2.5 2.5 0 113.536 3.536L6.5 21.036H3v-3.572L16.732 3.732z" />
                                        </svg>
                                        수정
                                    </button>
                                )}
                                <button
                                    onClick={handleLogout}
                                    className="px-4 py-2 text-gray-500 hover:text-gray-700 font-medium transition-colors"
                                >
                                    로그아웃
                                </button>
                            </div>
                        </div>

                        {/* 수정 폼 */}
                        {editing && (
                            <div className="mt-6 pt-6 border-t border-gray-100">
                                <form onSubmit={handleSubmit} className="space-y-4">
                                    <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                                        <div>
                                            <label className="block text-sm font-medium text-gray-700 mb-2">
                                                이름
                                            </label>
                                            <input
                                                type="text"
                                                name="name"
                                                value={formData.name}
                                                onChange={handleChange}
                                                className="w-full px-4 py-3 border border-gray-200 rounded-xl focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500 transition-all duration-200"
                                                required
                                            />
                                        </div>
                                        <div>
                                            <label className="block text-sm font-medium text-gray-700 mb-2">
                                                프로필 이미지 URL
                                            </label>
                                            <input
                                                type="url"
                                                name="profileImage"
                                                value={formData.profileImage}
                                                onChange={handleChange}
                                                placeholder="https://example.com/image.jpg"
                                                className="w-full px-4 py-3 border border-gray-200 rounded-xl focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500 transition-all duration-200 placeholder-gray-400"
                                            />
                                        </div>
                                    </div>
                                    <div className="flex gap-3">
                                        <button
                                            type="submit"
                                            disabled={saving}
                                            className="px-5 py-2.5 bg-gradient-to-r from-blue-500 to-cyan-500 text-white font-medium rounded-xl hover:shadow-lg hover:shadow-blue-500/25 transition-all duration-200 disabled:opacity-50 disabled:cursor-not-allowed flex items-center gap-2"
                                        >
                                            {saving ? (
                                                <>
                                                    <svg className="w-4 h-4 animate-spin" fill="none" viewBox="0 0 24 24">
                                                        <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                                                        <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                                                    </svg>
                                                    저장 중...
                                                </>
                                            ) : (
                                                '저장'
                                            )}
                                        </button>
                                        <button
                                            type="button"
                                            onClick={() => {
                                                setEditing(false);
                                                setFormData({
                                                    name: profile?.name || '',
                                                    profileImage: profile?.profileImage || '',
                                                });
                                                setError('');
                                            }}
                                            className="px-5 py-2.5 border border-gray-200 text-gray-700 font-medium rounded-xl hover:bg-gray-50 transition-all duration-200"
                                        >
                                            취소
                                        </button>
                                    </div>
                                </form>
                            </div>
                        )}
                    </div>
                )}

                {/* 면접 목록 섹션 */}
                <div className="mb-6">
                    <div className="flex items-center justify-between mb-4">
                        <h2 className="text-xl font-bold text-gray-900">내 면접</h2>
                        <Link to="/create">
                            <Button className="bg-gradient-to-r from-blue-500 to-cyan-400 hover:from-blue-600 hover:to-cyan-500 text-white font-semibold rounded-xl shadow-lg shadow-blue-500/25 transition-all">
                                <Plus className="w-4 h-4 mr-1.5" />
                                새 면접
                            </Button>
                        </Link>
                    </div>

                    {interviewsLoading ? (
                        <div className="flex flex-col items-center justify-center py-12">
                            <div className="w-10 h-10 border-4 border-blue-200 border-t-blue-500 rounded-full animate-spin mb-3" />
                            <p className="text-gray-500 text-sm">면접 목록을 불러오는 중...</p>
                        </div>
                    ) : interviews.length === 0 ? (
                        <div className="bg-white rounded-2xl border border-gray-100 shadow-sm p-10 text-center">
                            <FileText className="w-10 h-10 text-gray-300 mx-auto mb-3" />
                            <h3 className="text-lg font-medium text-gray-900 mb-1">아직 면접이 없습니다</h3>
                            <p className="text-gray-500 mb-5 text-sm">첫 번째 AI 면접을 시작해보세요!</p>
                            <Link to="/create">
                                <Button className="bg-gradient-to-r from-blue-500 to-cyan-400 hover:from-blue-600 hover:to-cyan-500 text-white font-semibold rounded-xl shadow-lg shadow-blue-500/25">
                                    <Plus className="w-4 h-4 mr-1.5" />
                                    면접 생성하기
                                </Button>
                            </Link>
                        </div>
                    ) : (
                        <>
                            <div className="space-y-3">
                                {interviews.map((interview) => (
                                    <div
                                        key={interview.id}
                                        className="bg-white rounded-xl border border-gray-100 shadow-sm hover:shadow-md hover:border-gray-200 transition-all duration-200 p-4"
                                    >
                                        <div className="flex items-center gap-4">
                                            <div className="flex-1 min-w-0">
                                                <div className="flex items-center gap-2 mb-1">
                                                    <h3 className="font-semibold text-gray-900 truncate">
                                                        {interview.title}
                                                    </h3>
                                                    <span className={`px-2 py-0.5 text-xs font-medium rounded-full whitespace-nowrap ${getStatusStyle(interview.status)}`}>
                                                        {getStatusText(interview.status)}
                                                    </span>
                                                </div>
                                                <p className="text-sm text-gray-500 truncate">
                                                    {interview.positionDescription} · {interview.levelDescription} · {interview.typeDescription}
                                                </p>
                                            </div>
                                            <div className="flex items-center gap-2 flex-shrink-0">
                                                <button
                                                    onClick={() => handleDelete(interview.id)}
                                                    className="p-2 text-gray-400 hover:text-red-500 hover:bg-red-50 rounded-lg transition-colors"
                                                    title="삭제"
                                                >
                                                    <Trash2 className="w-4 h-4" />
                                                </button>
                                                <Link
                                                    to={`/interviews/${interview.id}`}
                                                    className="flex items-center gap-1 px-3 py-1.5 text-sm font-medium text-blue-500 hover:text-blue-600 hover:bg-blue-50 rounded-lg transition-colors"
                                                >
                                                    상세
                                                    <ChevronRight className="w-4 h-4" />
                                                </Link>
                                            </div>
                                        </div>
                                    </div>
                                ))}
                            </div>

                            {/* Pagination */}
                            {totalPages > 1 && (
                                <div className="flex justify-center items-center gap-4 mt-6">
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
                </div>
            </div>
        </Layout>
    );
};

export default MyPage;
