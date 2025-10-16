package com.flow.api.service;

import com.flow.api.domain.Member;
import com.woo.core.service.BaseService;

/**
 * Member Service Interface
 */
public interface MemberService extends BaseService<Member> {
  
  Member findByUsername(String username);
  
  boolean existsByUsername(String username);
}

