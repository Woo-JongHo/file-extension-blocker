package com.flow.api.repository;

import com.flow.api.domain.Member;
import com.woo.core.repository.BaseRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MemberRepository extends BaseRepository<Member, Long> {
  
  Optional<Member> findByUsername(String username);
  
  Optional<Member> findByUsernameAndIsDeletedFalse(String username);
  
  boolean existsByUsername(String username);
}

