package com.woo.core.util.audit;

import java.util.Optional;
import org.springframework.data.domain.AuditorAware;
import org.springframework.stereotype.Component;

/**
 * JPA Auditing을 위한 현재 사용자 정보 제공
 * created_by, updated_by 자동 설정
 */
@Component
public class AuditorAwareImpl implements AuditorAware<Long> {

  @Override
  public Optional<Long> getCurrentAuditor() {
    // TODO: Spring Security 연동 시 실제 로그인 사용자 ID 반환
    // SecurityContext에서 현재 로그인한 사용자의 member_id 가져오기
    // return Optional.ofNullable(SecurityContextHolder.getContext()
    //   .getAuthentication())
    //   .map(auth -> ((UserDetails) auth.getPrincipal()).getId());
    
    // 임시: 개발 중에는 1L (admin) 반환
    return Optional.of(1L);
  }
}

