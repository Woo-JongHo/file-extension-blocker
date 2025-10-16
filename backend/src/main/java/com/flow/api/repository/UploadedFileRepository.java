package com.flow.api.repository;

import com.flow.api.domain.UploadedFile;
import com.woo.core.repository.BaseRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface UploadedFileRepository extends BaseRepository<UploadedFile, Long> {
  
  List<UploadedFile> findBySpaceIdAndIsDeletedFalse(Long spaceId);
  
  List<UploadedFile> findByCreatedByAndIsDeletedFalse(Long createdBy);
  
  Long countBySpaceIdAndIsDeletedFalse(Long spaceId);
}

