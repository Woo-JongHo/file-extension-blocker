import api from '@/services/api';

const blockedExtensionService = {
  // 전체 차단 확장자 목록 조회
  getBlockList: async (spaceId) => {
    const baseResponse = await api.get('/api/blocked-extensions/block-list', {
      params: { spaceId }
    });
    return baseResponse;
  },

  // 고정 확장자 목록 조회
  getFixedBlockList: async (spaceId) => {
    const baseResponse = await api.get('/api/blocked-extensions/fixed-block-list', {
      params: { spaceId }
    });
    return baseResponse;
  },

  // 커스텀 확장자 목록 조회
  getCustomBlockList: async (spaceId) => {
    const baseResponse = await api.get('/api/blocked-extensions/custom-block-list', {
      params: { spaceId }
    });
    return baseResponse;
  },

  // 고정 확장자 상태 변경 (체크/언체크)
  toggleFixedExtension: async (spaceId, extension) => {
    const baseResponse = await api.patch('/api/blocked-extensions/fixed-change-status', null, {
      params: { spaceId, extension }
    });
    return baseResponse;
  },

  // 커스텀 확장자 추가
  addCustomExtension: async (extensionData) => {
    const baseResponse = await api.post('/api/blocked-extensions', extensionData);
    return baseResponse;
  },

  // 커스텀 확장자 삭제
  deleteCustomExtension: async (blockedId) => {
    const baseResponse = await api.delete(`/api/blocked-extensions/${blockedId}`);
    return baseResponse;
  },

  // 커스텀 확장자 개수 조회
  countCustomExtensions: async (spaceId) => {
    const baseResponse = await api.get('/api/blocked-extensions/count-custom-block-list', {
      params: { spaceId }
    });
    return baseResponse;
  },
};

export default blockedExtensionService;

