import api from '@/services/api';

const memberService = {
  // Space별 멤버 목록 조회
  getMembersBySpace: async (spaceId) => {
    const baseResponse = await api.get('/api/members/member-list', {
      params: { spaceId }
    });
    return baseResponse;
  },

  // Member 단건 조회
  getMemberById: async (memberId) => {
    const baseResponse = await api.get(`/api/members/${memberId}`);
    return baseResponse;
  },

  // Member 생성
  createMember: async (memberData) => {
    const baseResponse = await api.post('/api/members', memberData);
    return baseResponse;
  },

  // Member 수정
  updateMember: async (memberId, memberData) => {
    const baseResponse = await api.put(`/api/members/${memberId}`, memberData);
    return baseResponse;
  },

  // Member 삭제
  deleteMember: async (memberId) => {
    const baseResponse = await api.delete(`/api/members/${memberId}`);
    return baseResponse;
  },
};

export default memberService;

