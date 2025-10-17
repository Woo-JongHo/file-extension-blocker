package com.flow.api.service;

import com.flow.api.domain.Space;
import com.woo.core.service.BaseService;
import java.util.List;

public interface SpaceService extends BaseService<Space> {
  
  List<Space> getAllSpaces();
  
  boolean existsBySpaceName(String spaceName);

  void insertTop6Extensions(Long spaceId, Long memberId);
}

