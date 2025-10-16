package com.flow.api.service;

import com.flow.api.domain.UploadedFile;
import com.woo.core.service.BaseService;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

public interface UploadedFileService extends BaseService<UploadedFile> {
  
  List<UploadedFile> getFilesBySpace(Long spaceId);
  
  List<UploadedFile> getFilesByUploader(Long memberId);
  
  Long countFilesBySpace(Long spaceId);
  
  UploadedFile uploadFile(Long spaceId, MultipartFile file);
  
  /**
   * 파일 확장자가 해당 공간에서 차단되어 있는지 확인
   * 
   * <p>DB의 BlockedExtension 테이블에서 isDeleted=false인 차단 확장자 목록을 조회하여
   * 대소문자 구분 없이 비교한다.
   * 
   * @param extension 확장자 (점 제외, 예: "bat", "exe")
   * @return true면 차단된 확장자, false면 허용된 확장자
   */
  boolean isExtensionBlocked(Long spaceId, String extension);
}

