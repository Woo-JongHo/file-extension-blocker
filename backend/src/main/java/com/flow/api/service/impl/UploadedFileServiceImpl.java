package com.flow.api.service.impl;

import com.flow.api.domain.UploadedFile;
import com.flow.api.repository.UploadedFileRepository;
import com.flow.api.service.UploadedFileService;
import com.woo.core.service.BaseServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Slf4j
@Service
@Transactional
public class UploadedFileServiceImpl extends BaseServiceImpl<UploadedFile> implements UploadedFileService {

  private final UploadedFileRepository uploadedFileRepository;

  public UploadedFileServiceImpl(UploadedFileRepository uploadedFileRepository) {
    super(uploadedFileRepository);
    this.uploadedFileRepository = uploadedFileRepository;
  }

  @Override
  @Transactional(readOnly = true)
  public List<UploadedFile> getFilesBySpace(Long spaceId) {
    return uploadedFileRepository.findBySpaceIdAndIsDeletedFalse(spaceId);
  }

  @Override
  @Transactional(readOnly = true)
  public List<UploadedFile> getFilesByUploader(Long memberId) {
    return uploadedFileRepository.findByCreatedByAndIsDeletedFalse(memberId);
  }

  @Override
  @Transactional(readOnly = true)
  public Long countFilesBySpace(Long spaceId) {
    return uploadedFileRepository.countBySpaceIdAndIsDeletedFalse(spaceId);
  }
}

