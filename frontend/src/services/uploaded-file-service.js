import api from '@/services/api';

const uploadedFileService = {
  // 파일 업로드
  uploadFile: async (spaceId, file) => {
    const formData = new FormData();
    formData.append('spaceId', spaceId);
    formData.append('file', file);

    const baseResponse = await api.post('/api/uploaded-files/upload', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    });
    return baseResponse;
  },

  // Space별 파일 목록 조회
  getFilesBySpace: async (spaceId) => {
    const baseResponse = await api.get('/api/uploaded-files/list', {
      params: { spaceId }
    });
    return baseResponse;
  },

  // 파일 단건 조회
  getFileById: async (fileId) => {
    const baseResponse = await api.get(`/api/uploaded-files/${fileId}`);
    return baseResponse;
  },

  // 파일 삭제
  deleteFile: async (fileId) => {
    const baseResponse = await api.delete(`/api/uploaded-files/${fileId}`);
    return baseResponse;
  },

  // Space별 파일 개수 조회
  countFilesBySpace: async (spaceId) => {
    const baseResponse = await api.get('/api/uploaded-files/count', {
      params: { spaceId }
    });
    return baseResponse;
  },

  // 파일 다운로드
  downloadFile: async (fileId, fileName) => {
    const isLocal = window.location.hostname === 'localhost' || window.location.hostname === '127.0.0.1';
    const API_URL = import.meta.env.VITE_API_URL || 
      (isLocal ? 'http://localhost:8800' : 'https://hilton-roseolar-pauselessly.ngrok-free.dev');
    const response = await fetch(`${API_URL}/api/uploaded-files/download/${fileId}`, {
      method: 'GET'
    });
    
    if (!response.ok) {
      throw new Error('파일 다운로드 실패');
    }
    
    const blob = await response.blob();
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = fileName;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    window.URL.revokeObjectURL(url);
  },
};

export default uploadedFileService;

