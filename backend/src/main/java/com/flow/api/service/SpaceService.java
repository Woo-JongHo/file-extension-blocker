package com.flow.api.service;

import com.flow.api.domain.Space;
import com.woo.core.service.BaseService;

import java.util.List;

/**
 * Space Service Interface
 */
public interface SpaceService extends BaseService<Space> {
  
  List<Space> getAllSpaces();
  
  boolean existsBySpaceName(String spaceName);
}

