import api from './axiosConfig';

export const createInterview = (data) => api.post('/interviews', data);

export const getInterviews = (page = 0, size = 10) => api.get('/interviews', { params: { page, size } });

export const getInterview = (id) => api.get(`/interviews/${id}`);

export const startInterview = (id) => api.post(`/interviews/${id}/start`);

export const getQuestions = (id) => api.get(`/interviews/${id}/questions`);

export const createAnswer = (interviewId, questionId, data) =>
    api.post(`/interviews/${interviewId}/questions/${questionId}/answers`, data);

export const getInterviewResult = (id) => api.get(`/interviews/${id}/result`);

export const deleteInterview = (id) => api.delete(`/interviews/${id}`);

export const uploadResume = (id, file) => {
    const formData = new FormData();
    formData.append('file', file);
    return api.post(`/interviews/${id}/resume`, formData, {
        headers: {
            'Content-Type': 'multipart/form-data',
        },
    });
};

export const uploadPortfolio = (id, file) => {
    const formData = new FormData();
    formData.append('file', file);
    return api.post(`/interviews/${id}/portfolio`, formData, {
        headers: {
            'Content-Type': 'multipart/form-data',
        },
    });
};
