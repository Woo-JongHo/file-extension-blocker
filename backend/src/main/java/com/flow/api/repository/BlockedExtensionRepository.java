package com.flow.api.repository;

import com.flow.api.domain.BlockedExtension;
import com.woo.core.repository.BaseRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface BlockedExtensionRepository extends BaseRepository<BlockedExtension, Long> {
  
  List<BlockedExtension> findBySpaceIdAndIsDeletedFalse(Long spaceId);
  
  List<BlockedExtension> findBySpaceIdAndIsFixedAndIsDeletedFalse(Long spaceId, Boolean isFixed);
  
  Optional<BlockedExtension> findBySpaceIdAndExtensionAndIsDeletedFalse(Long spaceId, String extension);
  
  Long countBySpaceIdAndIsFixedAndIsDeletedFalse(Long spaceId, Boolean isFixed);
}

