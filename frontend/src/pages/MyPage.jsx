import React, { useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { getMyProfile, updateProfile } from '../api/memberApi';

const MyPage = () => {
    const navigate = useNavigate();
    const { user, logout } = useAuth();

    const [profile, setProfile] = useState(null);
    const [loading, setLoading] = useState(true);
    const [editing, setEditing] = useState(false);
    const [formData, setFormData] = useState({ name: '', profileImage: '' });
    const [saving, setSaving] = useState(false);
    const [error, setError] = useState('');
    const [success, setSuccess] = useState('');

    useEffect(() => {
        if (!user) {
            navigate('/login');
            return;
        }
        fetchProfile();
    }, [user, navigate]);

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
            setLoading(false);
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

    const handleLogout = async () => {
        await logout();
        navigate('/');
    };

    if (loading) {
        return (
            <div className="min-h-screen bg-gray-50 flex items-center justify-center">
                <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-indigo-600"></div>
            </div>
        );
    }

    return (
        <div className="min-h-screen bg-gray-50">
            {/* 헤더 */}
            <header className="bg-white shadow-sm">
                <div className="max-w-4xl mx-auto px-4 py-4 flex justify-between items-center">
                    <Link to="/" className="text-2xl font-bold text-indigo-600">
                        PreView
                    </Link>
                    <nav className="flex items-center gap-4">
                        <Link to="/dashboard" className="text-gray-600 hover:text-gray-900">
                            대시보드
                        </Link>
                        <button
                            onClick={handleLogout}
                            className="text-gray-600 hover:text-gray-900"
                        >
                            로그아웃
                        </button>
                    </nav>
                </div>
            </header>

            {/* 본문 */}
            <main className="max-w-2xl mx-auto px-4 py-8">
                <h1 className="text-2xl font-bold text-gray-900 mb-6">내 정보</h1>

                {/* 에러 메시지 (프로필 로드 실패 시) */}
                {error && !profile && (
                    <div className="bg-white shadow-lg rounded-xl p-6 text-center">
                        <div className="text-red-500 mb-4">
                            <svg className="w-12 h-12 mx-auto" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
                            </svg>
                        </div>
                        <p className="text-gray-600 mb-4">{error}</p>
                        <button
                            onClick={fetchProfile}
                            className="px-4 py-2 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 transition-colors"
                        >
                            다시 시도
                        </button>
                    </div>
                )}

                {/* 프로필 카드 (프로필 로드 성공 시) */}
                {profile && (
                <>
                {/* 알림 메시지 */}
                {error && (
                    <div className="mb-4 p-3 bg-red-50 border border-red-200 rounded-lg">
                        <p className="text-sm text-red-600">{error}</p>
                    </div>
                )}
                {success && (
                    <div className="mb-4 p-3 bg-green-50 border border-green-200 rounded-lg">
                        <p className="text-sm text-green-600">{success}</p>
                    </div>
                )}

                <div className="bg-white shadow-lg rounded-xl p-6">
                    {!editing ? (
                        /* 보기 모드 */
                        <div className="space-y-4">
                            <div className="flex items-center gap-4">
                                <div className="w-16 h-16 bg-indigo-100 rounded-full flex items-center justify-center">
                                    {profile?.profileImage ? (
                                        <img
                                            src={profile.profileImage}
                                            alt="프로필"
                                            className="w-16 h-16 rounded-full object-cover"
                                        />
                                    ) : (
                                        <span className="text-2xl text-indigo-600">
                                            {profile?.name?.charAt(0) || '?'}
                                        </span>
                                    )}
                                </div>
                                <div>
                                    <h2 className="text-xl font-semibold text-gray-900">
                                        {profile?.name}
                                    </h2>
                                    <p className="text-gray-500">{profile?.email}</p>
                                </div>
                            </div>

                            <div className="pt-4 border-t">
                                <button
                                    onClick={() => setEditing(true)}
                                    className="px-4 py-2 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 transition-colors"
                                >
                                    프로필 수정
                                </button>
                            </div>
                        </div>
                    ) : (
                        /* 수정 모드 */
                        <form onSubmit={handleSubmit} className="space-y-4">
                            <div>
                                <label className="block text-sm font-medium text-gray-700 mb-1">
                                    이메일
                                </label>
                                <input
                                    type="email"
                                    value={profile?.email || ''}
                                    disabled
                                    className="w-full px-3 py-2 border border-gray-300 rounded-lg bg-gray-100 text-gray-500"
                                />
                                <p className="text-xs text-gray-400 mt-1">이메일은 변경할 수 없습니다.</p>
                            </div>

                            <div>
                                <label className="block text-sm font-medium text-gray-700 mb-1">
                                    이름
                                </label>
                                <input
                                    type="text"
                                    name="name"
                                    value={formData.name}
                                    onChange={handleChange}
                                    className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500"
                                    required
                                />
                            </div>

                            <div>
                                <label className="block text-sm font-medium text-gray-700 mb-1">
                                    프로필 이미지 URL
                                </label>
                                <input
                                    type="url"
                                    name="profileImage"
                                    value={formData.profileImage}
                                    onChange={handleChange}
                                    placeholder="https://example.com/image.jpg"
                                    className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500"
                                />
                            </div>

                            <div className="flex gap-3 pt-4">
                                <button
                                    type="submit"
                                    disabled={saving}
                                    className="px-4 py-2 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 transition-colors disabled:opacity-50"
                                >
                                    {saving ? '저장 중...' : '저장'}
                                </button>
                                <button
                                    type="button"
                                    onClick={() => {
                                        setEditing(false);
                                        setFormData({
                                            name: profile?.name || '',
                                            profileImage: profile?.profileImage || '',
                                        });
                                    }}
                                    className="px-4 py-2 border border-gray-300 text-gray-700 rounded-lg hover:bg-gray-50 transition-colors"
                                >
                                    취소
                                </button>
                            </div>
                        </form>
                    )}
                </div>
                </>
                )}
            </main>
        </div>
    );
};

export default MyPage;
