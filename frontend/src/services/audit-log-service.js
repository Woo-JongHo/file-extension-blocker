import api from './api';

export const auditLogService = {
  /**
   * 최근 감사 로그 조회
   */
  getRecentLogs: async (limit = 100) => {
    const response = await api.get(`/audit-logs/recent?limit=${limit}`);
    return response.data;
  },

  /**
   * 공간별 감사 로그 조회
   */
  getLogsBySpace: async (spaceId, limit = 100) => {
    const response = await api.get(`/audit-logs/space/${spaceId}?limit=${limit}`);
    return response.data;
  },

  /**
   * 시간 기준 감사 로그 조회
   */
  getLogsSince: async (since, limit = 100) => {
    const response = await api.get(`/audit-logs/since?since=${since}&limit=${limit}`);
    return response.data;
  },

  /**
   * SSE 스트림 연결
   */
  connectToLogStream: () => {
    const baseURL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8800';
    return new EventSource(`${baseURL}/api/logs/stream`);
  },

  /**
   * 로그 파일 내용 조회
   */
  getLogFile: async (lines = 100) => {
    const response = await api.get(`/logs/file?lines=${lines}`);
    return response.data;
  },
};

