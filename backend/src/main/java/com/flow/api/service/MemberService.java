package com.flow.api.service;

import com.flow.api.domain.Member;
import com.woo.core.service.BaseService;
import java.util.List;

public interface MemberService extends BaseService<Member> {
  
  Member findByUsername(String username);
  
  List<Member> getMembersBySpace(Long spaceId);
}

