package com.flow.api.service;

import com.flow.api.domain.Space;
import com.woo.core.service.BaseService;

import java.util.List;
import java.util.Map;

public interface SpaceService extends BaseService<Space> {
  
  List<Space> getAllSpaces();
  
  boolean existsBySpaceName(String spaceName);

  void insertTop6Extensions(Long spaceId, Long memberId);
  
  /**
   * Space와 Admin Member를 동시에 생성하고 고정 확장자 자동 삽입
   * @return 생성된 Space, Member, 확장자 개수
   */
  Map<String, Object> createSpaceWithAdmin(
      String spaceName, 
      String description, 
      String adminUsername, 
      String adminPassword);
}

