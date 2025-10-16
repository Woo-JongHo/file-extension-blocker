package com.flow.api.repository;

import com.flow.api.domain.Space;
import com.woo.core.repository.BaseRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SpaceRepository extends BaseRepository<Space, Long> {
  
  // 모든 공간 조회 (삭제되지 않은 것만)
  // SELECT * FROM space WHERE is_deleted = false
  List<Space> findByIsDeletedFalse();

  // 공간명 중복 확인 (삭제되지 않은 것만)
  // SELECT EXISTS(SELECT 1 FROM space WHERE space_name = ? AND is_deleted = false)
  boolean existsBySpaceNameAndIsDeletedFalse(String spaceName);
}

