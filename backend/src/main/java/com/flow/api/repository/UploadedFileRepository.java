package com.flow.api.repository;

import com.flow.api.domain.UploadedFile;
import com.woo.core.repository.BaseRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface UploadedFileRepository extends BaseRepository<UploadedFile, Long> {
  
  // 공간의 모든 파일 조회 (삭제되지 않은 것만)
  List<UploadedFile> findBySpaceIdAndIsDeletedFalse(Long spaceId);
  
  // 특정 사용자가 업로드한 파일 조회
  List<UploadedFile> findByCreatedByAndIsDeletedFalse(Long createdBy);
  
  // 공간의 파일 개수
  Long countBySpaceIdAndIsDeletedFalse(Long spaceId);
}

