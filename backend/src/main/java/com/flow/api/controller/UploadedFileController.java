package com.flow.api.controller;

import com.flow.api.domain.UploadedFile;
import com.flow.api.domain.data.UploadedFileDto;
import com.flow.api.service.UploadedFileService;
import com.woo.core.controller.BaseController;
import com.woo.core.response.BaseResponse;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/spaces/{spaceId}/files")
public class UploadedFileController extends BaseController<UploadedFile, UploadedFileDto> {

  private final UploadedFileService uploadedFileService;

  public UploadedFileController(UploadedFileService uploadedFileService, ModelMapper modelMapper) {
    super(uploadedFileService, modelMapper);
    this.uploadedFileService = uploadedFileService;
  }
  
  @Override
  protected Class<UploadedFileDto> getDtoClass() { return UploadedFileDto.class; }

  @Override
  protected Class<UploadedFile> getEntityClass() { return UploadedFile.class; }

  // ========== 비즈니스 로직 ==========
  
  @GetMapping
  public ResponseEntity<BaseResponse<List<UploadedFileDto>>> getFiles(@PathVariable Long spaceId) {
    List<UploadedFile> files = uploadedFileService.getFilesBySpace(spaceId);
    List<UploadedFileDto> dtos = files.stream().map(this::toDto).collect(Collectors.toList());
    return successResponse(dtos);
  }

  @GetMapping("/count")
  public ResponseEntity<BaseResponse<Long>> countFiles(@PathVariable Long spaceId) {
    Long count = uploadedFileService.countFilesBySpace(spaceId);
    return successResponse(count);
  }
}

