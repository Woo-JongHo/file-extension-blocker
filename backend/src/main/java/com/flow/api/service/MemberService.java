package com.flow.api.service;

import com.flow.api.domain.Member;
import com.woo.core.service.BaseService;
import java.util.List;

public interface MemberService extends BaseService<Member> {
  
  // 사용자명으로 조회 (삭제되지 않은 것만)
  Member findByUsername(String username);
  
  // 공간의 모든 회원 조회
  List<Member> getMembersBySpace(Long spaceId);
}

