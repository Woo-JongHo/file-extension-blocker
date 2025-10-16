package com.flow.api.service;

import com.flow.api.domain.BlockedExtension;
import com.woo.core.service.BaseService;
import java.util.List;

public interface BlockedExtensionService extends BaseService<BlockedExtension> {
  
  // 공간의 모든 차단 확장자 조회
  List<BlockedExtension> getBlockedExtensions(Long spaceId);
  
  // 공간의 고정 확장자 조회 (Top-6)
  List<BlockedExtension> getFixedExtensions(Long spaceId);
  
  // 공간의 커스텀 확장자 조회 (최대 200개)
  List<BlockedExtension> getCustomExtensions(Long spaceId);
  
  // 고정 확장자 체크/언체크 (isBlocked: true=차단, false=해제)
  void toggleFixedExtension(Long spaceId, String extension, Boolean isBlocked);
  
  // 커스텀 확장자 개수 (200개 제한 검증용)
  Long countCustomExtensions(Long spaceId);
}

