import api from '@/services/api';

const statsService = {
  // Space별 통계 조회 (파일 수 + 활성화된 전체 확장자 수)
  getSpaceStats: async (spaceId) => {
    try {
      const [filesResponse, extensionsResponse] = await Promise.all([
        api.get('/api/uploaded-files/count', { params: { spaceId } }),
        api.get('/api/blocked-extensions/count-active', { params: { spaceId } })
      ]);

      return {
        fileCount: filesResponse.data || 0,
        extensionCount: extensionsResponse.data || 0
      };
    } catch (err) {
      console.error('통계 조회 실패:', err);
      return {
        fileCount: 0,
        extensionCount: 0
      };
    }
  },
};

export default statsService;

