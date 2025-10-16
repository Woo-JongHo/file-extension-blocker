package com.flow.api.repository;

import com.flow.api.domain.Member;
import com.woo.core.repository.BaseRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface MemberRepository extends BaseRepository<Member, Long> {
  
  // 사용자명으로 조회 (삭제되지 않은 것만)
  // SELECT * FROM member WHERE username = ? AND is_deleted = false LIMIT 1
  Optional<Member> findByUsernameAndIsDeletedFalse(String username);

  // 공간의 모든 회원 조회 (삭제되지 않은 것만)
  // SELECT * FROM member WHERE space_id = ? AND is_deleted = false
  List<Member> findBySpaceIdAndIsDeletedFalse(Long spaceId);
}

