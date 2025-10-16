package com.flow.api.service;

import com.flow.api.domain.BlockedExtension;
import com.woo.core.service.BaseService;
import java.util.List;

public interface BlockedExtensionService extends BaseService<BlockedExtension> {
  
  List<BlockedExtension> getBlockedExtensions(Long spaceId);
  
  List<BlockedExtension> getFixedExtensions(Long spaceId);
  
  List<BlockedExtension> getCustomExtensions(Long spaceId);
  
  void toggleFixedExtension(Long spaceId, String extension, Boolean isBlocked);
  
  Long countCustomExtensions(Long spaceId);
}

