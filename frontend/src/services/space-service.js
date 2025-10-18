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

  // Space + Admin Member + 고정 확장자 동시 생성 (권장)
  createSpaceWithAdmin: async ({ spaceName, description, adminUsername, adminPassword }) => {
    const params = new URLSearchParams();
    params.append('spaceName', spaceName);
    params.append('description', description);
    params.append('adminUsername', adminUsername);
    params.append('adminPassword', adminPassword);
    
    const baseResponse = await api.post('/api/spaces/create-with-admin', params, {
      headers: {
        'Content-Type': 'application/x-www-form-urlencoded',
      },
    });
    return baseResponse;
  },

  // Top-6 고정 확장자 자동 삽입 (레거시)
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

