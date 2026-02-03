import api from './axiosConfig';

/**
 * 내 정보 조회
 * axiosConfig 인터셉터가 response.data를 반환하므로 추가 .data 접근 불필요
 */
export const getMyProfile = async () => {
    const response = await api.get('/members/me');
    return response;  // { success, message, data: MemberResponse }
};

/**
 * 프로필 수정
 */
export const updateProfile = async (name, profileImage) => {
    const response = await api.put('/members/me', { name, profileImage });
    return response;  // { success, message, data: MemberResponse }
};
