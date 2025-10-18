package com.flow.api.repository;

import com.flow.api.domain.BlockedExtension;
import com.woo.core.repository.BaseRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;


@Repository
public interface BlockedExtensionRepository extends BaseRepository<BlockedExtension, Long> {
  
  // ══════════════════════════════════════
  // ========== 조회 메서드 ==========
  // 1. 활성화된 모든 차단 확장자 조회 - findBySpaceIdAndIsDeletedFalse
  // 2. 고정/커스텀 확장자 조회 (활성화, 알파벳순) - findBySpaceIdAndIsFixedAndIsDeletedFalseOrderByExtensionAsc
  // 3. 고정 확장자 전체 조회 (삭제 여부 무관, 알파벳순) - findBySpaceIdAndIsFixedOrderByExtensionAsc
  // 4. 특정 확장자 조회 (중복 확인용) - findBySpaceIdAndExtension
  // 5. 특정 확장자 조회 (활성화만) - findBySpaceIdAndExtensionAndIsDeletedFalse
  // 6. 커스텀 확장자 개수 - countBySpaceIdAndIsFixedAndIsDeletedFalse
  // 7. Top-6 인기 확장자 조회 - findTop6CustomExtensions
  // ══════════════════════════════════════
  
  // 공간의 모든 차단 확장자 조회 (삭제되지 않은 것만)
  // SELECT * FROM blocked_extension WHERE space_id = ? AND is_deleted = false
  List<BlockedExtension> findBySpaceIdAndIsDeletedFalse(Long spaceId);

  // 공간의 고정/커스텀 확장자 조회 (isFixed: true=고정, false=커스텀, 확장자명 알파벳 순)
  // SELECT * FROM blocked_extension WHERE space_id = ? AND is_fixed = ? AND is_deleted = false ORDER BY extension ASC
  List<BlockedExtension> findBySpaceIdAndIsFixedAndIsDeletedFalseOrderByExtensionAsc(Long spaceId, Boolean isFixed);

  // 공간의 고정 확장자 전체 조회 (삭제 여부 무관, 확장자명 알파벳 순)
  // SELECT * FROM blocked_extension WHERE space_id = ? AND is_fixed = ? ORDER BY extension ASC
  List<BlockedExtension> findBySpaceIdAndIsFixedOrderByExtensionAsc(Long spaceId, Boolean isFixed);

  // 공간의 특정 확장자 조회 (삭제 여부 무관)
  // SELECT * FROM blocked_extension WHERE space_id = ? AND extension = ? LIMIT 1
  Optional<BlockedExtension> findBySpaceIdAndExtension(Long spaceId, String extension);

  // 공간의 특정 확장자 조회 (삭제되지 않은 것만)
  // SELECT * FROM blocked_extension WHERE space_id = ? AND extension = ? AND is_deleted = false LIMIT 1
  Optional<BlockedExtension> findBySpaceIdAndExtensionAndIsDeletedFalse(Long spaceId, String extension);

  // 공간의 커스텀 확장자 개수 (최대 200개 제한용)
  // SELECT COUNT(*) FROM blocked_extension WHERE space_id = ? AND is_fixed = ? AND is_deleted = false
  Long countBySpaceIdAndIsFixedAndIsDeletedFalse(Long spaceId, Boolean isFixed);

  // Top-6 확장자 조회 (전역, 고정/커스텀 구분 없이 사용 빈도 기준)
  // SELECT extension, COUNT(*) FROM blocked_extension WHERE is_deleted=false GROUP BY extension ORDER BY COUNT(*) DESC LIMIT 6
  @Query(value = "SELECT extension, COUNT(*) as cnt FROM blocked_extension WHERE is_deleted = false GROUP BY extension ORDER BY cnt DESC LIMIT 6", nativeQuery = true)
  List<Object[]> findTop6CustomExtensions();
}

