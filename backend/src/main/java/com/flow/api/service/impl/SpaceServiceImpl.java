package com.flow.api.service.impl;

import com.flow.api.domain.BlockedExtension;
import com.flow.api.domain.Space;
import com.flow.api.repository.BlockedExtensionRepository;
import com.flow.api.repository.SpaceRepository;
import com.flow.api.service.SpaceService;
import com.woo.core.service.BaseServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
public class SpaceServiceImpl extends BaseServiceImpl<Space> implements SpaceService {

  private final SpaceRepository spaceRepository;
  private final BlockedExtensionRepository blockedExtensionRepository;

  public SpaceServiceImpl(SpaceRepository spaceRepository, BlockedExtensionRepository blockedExtensionRepository) {
    super(spaceRepository);
    this.spaceRepository = spaceRepository;
    this.blockedExtensionRepository = blockedExtensionRepository;
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

  @Override
  public void insertTop6Extensions(Long spaceId, Long memberId) {
    // 전역 커스텀 확장자에서 Top-6 조회
    List<Object[]> top6 = blockedExtensionRepository.findTop6CustomExtensions();
    
    // Top-6 고정 확장자 삽입 (기본 unCheck = is_deleted true)
    List<BlockedExtension> extensions = top6.stream()
        .map(row -> {
          String extension = (String) row[0];
          BlockedExtension be = new BlockedExtension();
          be.setSpaceId(spaceId);
          be.setExtension(extension);
          be.setIsFixed(true);
          be.setCreatedBy(memberId);
          be.setUpdatedBy(memberId);
          be.setIsDeleted(true); // 기본 unCheck
          return be;
        })
        .collect(Collectors.toList());
    
    blockedExtensionRepository.saveAll(extensions);
  }
}

