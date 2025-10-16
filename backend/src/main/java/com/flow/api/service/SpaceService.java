package com.flow.api.service;

import com.flow.api.domain.Space;
import com.woo.core.service.BaseService;
import java.util.List;

public interface SpaceService extends BaseService<Space> {
  
  // 모든 공간 조회 (삭제되지 않은 것만)
  List<Space> getAllSpaces();
  
  // 공간명 중복 확인
  boolean existsBySpaceName(String spaceName);
}

