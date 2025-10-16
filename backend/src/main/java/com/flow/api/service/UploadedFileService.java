package com.flow.api.service;

import com.flow.api.domain.UploadedFile;
import com.woo.core.service.BaseService;
import java.util.List;

public interface UploadedFileService extends BaseService<UploadedFile> {
  
  List<UploadedFile> getFilesBySpace(Long spaceId);
  
  List<UploadedFile> getFilesByUploader(Long memberId);
  
  Long countFilesBySpace(Long spaceId);
}

