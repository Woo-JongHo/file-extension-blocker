package com.flow.api.repository;

import com.flow.api.domain.BlockedExtension;
import com.woo.core.repository.BaseRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface BlockedExtensionRepository extends BaseRepository<BlockedExtension, Long> {
  
  // 공간의 모든 차단 확장자 조회 (삭제되지 않은 것만)
  // SELECT * FROM blocked_extension WHERE space_id = ? AND is_deleted = false
  List<BlockedExtension> findBySpaceIdAndIsDeletedFalse(Long spaceId);

  // 공간의 고정/커스텀 확장자 조회 (isFixed: true=고정, false=커스텀)
  // SELECT * FROM blocked_extension WHERE space_id = ? AND is_fixed = ? AND is_deleted = false
  List<BlockedExtension> findBySpaceIdAndIsFixedAndIsDeletedFalse(Long spaceId, Boolean isFixed);

  // 공간의 특정 확장자 조회 (삭제 여부 무관)
  // SELECT * FROM blocked_extension WHERE space_id = ? AND extension = ? LIMIT 1
  Optional<BlockedExtension> findBySpaceIdAndExtension(Long spaceId, String extension);

  // 공간의 특정 확장자 조회 (삭제되지 않은 것만)
  // SELECT * FROM blocked_extension WHERE space_id = ? AND extension = ? AND is_deleted = false LIMIT 1
  Optional<BlockedExtension> findBySpaceIdAndExtensionAndIsDeletedFalse(Long spaceId, String extension);

  // 공간의 커스텀 확장자 개수 (최대 200개 제한용)
  // SELECT COUNT(*) FROM blocked_extension WHERE space_id = ? AND is_fixed = ? AND is_deleted = false
  Long countBySpaceIdAndIsFixedAndIsDeletedFalse(Long spaceId, Boolean isFixed);

  // Top-6 커스텀 확장자 조회 (전역)
  // SELECT extension, COUNT(*) FROM blocked_extension WHERE is_fixed=false AND is_deleted=false GROUP BY extension ORDER BY COUNT(*) DESC LIMIT 6
  @Query(value = "SELECT extension, COUNT(*) as cnt FROM blocked_extension WHERE is_fixed = false AND is_deleted = false GROUP BY extension ORDER BY cnt DESC LIMIT 6", nativeQuery = true)
  List<Object[]> findTop6CustomExtensions();
}

