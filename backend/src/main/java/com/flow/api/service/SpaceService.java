package com.flow.api.service;

import com.flow.api.domain.Space;
import com.flow.api.domain.data.SpaceCreationRequest;
import com.flow.api.domain.data.SpaceCreationResponse;
import com.woo.core.service.BaseService;

import java.util.List;

public interface SpaceService extends BaseService<Space> {
  
  List<Space> getAllSpaces();
  
  boolean existsBySpaceName(String spaceName);

  void insertTop6Extensions(Long spaceId, Long memberId);
  
  /**
   * Space와 Admin Member를 동시에 생성하고 고정 확장자 자동 삽입
   * @param request Space 및 Admin 정보
   * @return 생성된 Space, Member, 확장자 개수
   */
  SpaceCreationResponse createSpaceWithAdmin(SpaceCreationRequest request);
}

