package com.flow.api.repository;

import com.flow.api.domain.UploadedFile;
import com.woo.core.repository.BaseRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface UploadedFileRepository extends BaseRepository<UploadedFile, Long> {
  
  // ══════════════════════════════════════
  // ========== 조회 메서드 ==========
  // 1. Space별 파일 목록 조회 - findBySpaceIdAndIsDeletedFalse
  // 2. 사용자별 업로드 파일 조회 - findByCreatedByAndIsDeletedFalse
  // 3. Space별 파일 개수 - countBySpaceIdAndIsDeletedFalse
  // ══════════════════════════════════════
  
  // 공간의 모든 파일 조회 (삭제되지 않은 것만)
  // SELECT * FROM uploaded_file WHERE space_id = ? AND is_deleted = false
  List<UploadedFile> findBySpaceIdAndIsDeletedFalse(Long spaceId);

  // 특정 사용자가 업로드한 파일 조회
  // SELECT * FROM uploaded_file WHERE created_by = ? AND is_deleted = false
  List<UploadedFile> findByCreatedByAndIsDeletedFalse(Long createdBy);

  // 공간의 파일 개수
  // SELECT COUNT(*) FROM uploaded_file WHERE space_id = ? AND is_deleted = false
  Long countBySpaceIdAndIsDeletedFalse(Long spaceId);
}

