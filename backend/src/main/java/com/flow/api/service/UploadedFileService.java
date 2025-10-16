package com.flow.api.service;

import com.flow.api.domain.UploadedFile;
import com.woo.core.service.BaseService;
import java.util.List;

public interface UploadedFileService extends BaseService<UploadedFile> {
  
  // 공간의 모든 파일 조회
  List<UploadedFile> getFilesBySpace(Long spaceId);
  
  // 특정 사용자가 업로드한 파일 조회
  List<UploadedFile> getFilesByUploader(Long memberId);
  
  // 공간의 파일 개수
  Long countFilesBySpace(Long spaceId);
}

