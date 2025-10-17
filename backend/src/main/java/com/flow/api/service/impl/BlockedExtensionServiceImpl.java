package com.flow.api.service.impl;

import com.flow.api.domain.BlockedExtension;
import com.flow.api.repository.BlockedExtensionRepository;
import com.flow.api.service.BlockedExtensionService;
import com.woo.core.service.BaseServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Slf4j
@Service
@Transactional
public class BlockedExtensionServiceImpl extends BaseServiceImpl<BlockedExtension> implements BlockedExtensionService {

  private final BlockedExtensionRepository blockedExtensionRepository;

  public BlockedExtensionServiceImpl(BlockedExtensionRepository blockedExtensionRepository) {
    super(blockedExtensionRepository);
    this.blockedExtensionRepository = blockedExtensionRepository;
  }

  @Override
  @Transactional(readOnly = true)
  public List<BlockedExtension> getBlockedExtensions(Long spaceId) {
    return blockedExtensionRepository.findBySpaceIdAndIsDeletedFalse(spaceId);
  }

  @Override
  @Transactional(readOnly = true)
  public List<BlockedExtension> getFixedExtensions(Long spaceId) {
    return blockedExtensionRepository.findBySpaceIdAndIsFixedAndIsDeletedFalse(spaceId, true);
  }

  @Override
  @Transactional(readOnly = true)
  public List<BlockedExtension> getCustomExtensions(Long spaceId) {
    return blockedExtensionRepository.findBySpaceIdAndIsFixedAndIsDeletedFalse(spaceId, false);
  }

  @Override
  public void toggleFixedExtension(Long spaceId, String extension) {
    BlockedExtension blockedExtension = blockedExtensionRepository
        .findBySpaceIdAndExtension(spaceId, extension.toLowerCase())
        .orElseThrow(() -> new IllegalArgumentException("고정 확장자를 찾을 수 없습니다: " + extension));
    
    // 현재 상태 반전 (isDeleted 토글)
    blockedExtension.setIsDeleted(!blockedExtension.getIsDeleted());
    blockedExtensionRepository.save(blockedExtension);
  }

  @Override
  @Transactional(readOnly = true)
  public Long countCustomExtensions(Long spaceId) {
    return blockedExtensionRepository.countBySpaceIdAndIsFixedAndIsDeletedFalse(spaceId, false);
  }
}

