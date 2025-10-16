package com.flow.api.repository;

import com.flow.api.domain.Space;
import com.woo.core.repository.BaseRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SpaceRepository extends BaseRepository<Space, Long> {
  
  List<Space> findByIsDeletedFalse();
  
  boolean existsBySpaceNameAndIsDeletedFalse(String spaceName);
}

