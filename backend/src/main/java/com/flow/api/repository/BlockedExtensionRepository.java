package com.flow.api.repository;

import com.flow.api.domain.BlockedExtension;
import com.woo.core.repository.BaseRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface BlockedExtensionRepository extends BaseRepository<BlockedExtension, Long> {
  
  // 공간의 모든 차단 확장자 조회 (삭제되지 않은 것만)
  List<BlockedExtension> findBySpaceIdAndIsDeletedFalse(Long spaceId);
  
  // 공간의 고정/커스텀 확장자 조회 (isFixed: true=고정, false=커스텀)
  List<BlockedExtension> findBySpaceIdAndIsFixedAndIsDeletedFalse(Long spaceId, Boolean isFixed);
  
  // 공간의 특정 확장자 조회
  Optional<BlockedExtension> findBySpaceIdAndExtensionAndIsDeletedFalse(Long spaceId, String extension);
  
  // 공간의 커스텀 확장자 개수 (최대 200개 제한용)
  Long countBySpaceIdAndIsFixedAndIsDeletedFalse(Long spaceId, Boolean isFixed);
}

