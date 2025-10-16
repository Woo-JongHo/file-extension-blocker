package com.flow.api.service.impl;

import com.flow.api.domain.Space;
import com.flow.api.repository.SpaceRepository;
import com.flow.api.service.SpaceService;
import com.woo.core.service.BaseServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Space Service Implementation
 */
@Slf4j
@Service
@Transactional
public class SpaceServiceImpl extends BaseServiceImpl<Space> implements SpaceService {

  private final SpaceRepository spaceRepository;

  public SpaceServiceImpl(SpaceRepository spaceRepository) {
    super(spaceRepository);
    this.spaceRepository = spaceRepository;
  }

  @Override
  @Transactional(readOnly = true)
  public List<Space> getAllSpaces() {
    return spaceRepository.findByIsDeletedFalse();
  }

  @Override
  @Transactional(readOnly = true)
  public boolean existsBySpaceName(String spaceName) {
    return spaceRepository.existsBySpaceNameAndIsDeletedFalse(spaceName);
  }
}

