import api from '@/services/api';

const spaceService = {
  // Space 전체 목록 조회
  getAllSpaces: async () => {
    const baseResponse = await api.get('/api/spaces/space-list');
    return baseResponse; // BaseResponse { timestamp, status, message, data }
  },

  // Space 단건 조회
  getSpaceById: async (spaceId) => {
    const baseResponse = await api.get(`/api/spaces/${spaceId}`);
    return baseResponse;
  },

  // Space 생성
  createSpace: async (spaceData) => {
    const baseResponse = await api.post('/api/spaces', spaceData);
    return baseResponse;
  },

  // Top-6 고정 확장자 자동 삽입
  insertTop6Extensions: async (spaceId, memberId) => {
    const baseResponse = await api.post(`/api/spaces/${spaceId}/top6?memberId=${memberId}`);
    return baseResponse;
  },

  // Space 수정
  updateSpace: async (spaceId, spaceData) => {
    const baseResponse = await api.put(`/api/spaces/${spaceId}`, spaceData);
    return baseResponse;
  },

  // Space 삭제
  deleteSpace: async (spaceId) => {
    const baseResponse = await api.delete(`/api/spaces/${spaceId}`);
    return baseResponse;
  },
};

export default spaceService;

