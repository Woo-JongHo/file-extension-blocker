import axios from 'axios';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'https://orange-bottles-smoke.loca.lt';

const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

// 요청 인터셉터
api.interceptors.request.use(
  (config) => {
    // 필요시 토큰 추가
    // const token = localStorage.getItem('token');
    // if (token) {
    //   config.headers.Authorization = `Bearer ${token}`;
    // }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// 응답 인터셉터
api.interceptors.response.use(
  (response) => {
    // BaseResponse 구조에서 data 자동 추출
    // response.data = { timestamp, status, message, data, errorCode, errorDetail }
    // → response.data 자체를 반환 (BaseResponse 전체)
    return response.data;
  },
  (error) => {
    // 에러 처리
    if (error.response?.status === 401) {
      // 인증 실패 처리
      console.error('인증이 필요합니다.');
    }
    
    // BaseResponse 형식의 에러도 추출
    if (error.response?.data) {
      return Promise.reject(error.response.data);
    }
    
    return Promise.reject(error);
  }
);

export default api;

