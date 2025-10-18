package com.flow.api.repository;

import com.flow.api.domain.Member;
import com.woo.core.repository.BaseRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface MemberRepository extends BaseRepository<Member, Long> {
  
  // ══════════════════════════════════════
  // ========== 조회 메서드 ==========
  // 1. 사용자명으로 조회 - findByUsernameAndIsDeletedFalse
  // 2. 사용자명 중복 확인 - existsByUsernameAndIsDeletedFalse
  // 3. Space별 멤버 목록 조회 - findBySpaceIdAndIsDeletedFalse
  // ══════════════════════════════════════
  
  // 사용자명으로 조회 (삭제되지 않은 것만)
  // SELECT * FROM member WHERE username = ? AND is_deleted = false LIMIT 1
  Optional<Member> findByUsernameAndIsDeletedFalse(String username);

  // 사용자명 중복 확인 (삭제되지 않은 것만)
  // SELECT COUNT(*) > 0 FROM member WHERE username = ? AND is_deleted = false
  boolean existsByUsernameAndIsDeletedFalse(String username);

  // 공간의 모든 회원 조회 (삭제되지 않은 것만)
  // SELECT * FROM member WHERE space_id = ? AND is_deleted = false
  List<Member> findBySpaceIdAndIsDeletedFalse(Long spaceId);
}

