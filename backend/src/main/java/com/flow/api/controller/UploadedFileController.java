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
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

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
  
  // ══════════비즈니스 로직═══════════════════
  // 1. POST /upload - 파일 업로드 (차단 확장자 검증 포함)
  // 2. GET /list - 공간의 모든 파일 조회
  // 3. GET /count - 공간의 파일 개수
  // 4. GET /check-extension - 확장자 차단 여부 확인
  // ══════════════════════════════════════
  
  @PostMapping("/upload")
  public ResponseEntity<BaseResponse<UploadedFileDto>> uploadFile(
      @PathVariable Long spaceId,
      @RequestParam("file") MultipartFile file) {
    try {
      UploadedFile uploadedFile = uploadedFileService.uploadFile(spaceId, file);
      return successResponse(toDto(uploadedFile));
      
    } catch (IllegalArgumentException e) {
      return errorResponse("FILE_UPLOAD_FAILED", e.getMessage());
    }
  }
  
  @GetMapping("/check-extension")
  public ResponseEntity<BaseResponse<Boolean>> checkExtension(
      @PathVariable Long spaceId,
      @RequestParam String extension) {
    boolean isBlocked = uploadedFileService.isExtensionBlocked(spaceId, extension);
    return successResponse(isBlocked);
  }

  @GetMapping("/list")
  public ResponseEntity<BaseResponse<List<UploadedFileDto>>> getFiles(@PathVariable Long spaceId) {
    List<UploadedFile> files = uploadedFileService.getFilesBySpace(spaceId);
    return successResponse(toDtoList(files));
  }

  @GetMapping("/count")
  public ResponseEntity<BaseResponse<Long>> countFiles(@PathVariable Long spaceId) {
    Long count = uploadedFileService.countFilesBySpace(spaceId);
    return successResponse(count);
  }
}

