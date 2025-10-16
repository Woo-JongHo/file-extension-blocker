package com.flow.api.service.impl;

import com.flow.api.domain.Member;
import com.flow.api.repository.MemberRepository;
import com.flow.api.service.MemberService;
import com.woo.core.service.BaseServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Member Service Implementation
 */
@Slf4j
@Service
@Transactional
public class MemberServiceImpl extends BaseServiceImpl<Member> implements MemberService {

  private final MemberRepository memberRepository;

  public MemberServiceImpl(MemberRepository memberRepository) {
    super(memberRepository);
    this.memberRepository = memberRepository;
  }

  @Override
  @Transactional(readOnly = true)
  public Member findByUsername(String username) {
    return memberRepository.findByUsernameAndIsDeletedFalse(username)
        .orElse(null);
  }

  @Override
  @Transactional(readOnly = true)
  public boolean existsByUsername(String username) {
    return memberRepository.existsByUsername(username);
  }
}

